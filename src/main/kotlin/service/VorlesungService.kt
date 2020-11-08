package de.hska.kunde.service

import de.hska.kunde.config.logger
import de.hska.kunde.config.security.CustomUser
import de.hska.kunde.config.security.CustomUserDetailsService
import de.hska.kunde.config.security.Daten.roleAdminStr
import de.hska.kunde.db.CriteriaUtil.getCriteria
import de.hska.kunde.entity.Vorlesung
import de.hska.kunde.mail.Mailer
import java.util.UUID
import javax.validation.ConstraintViolationException
import javax.validation.ValidatorFactory
import kotlin.reflect.full.isSubclassOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.context.annotation.Lazy
import org.springframework.data.mongodb.core.ReactiveFluentMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.allAndAwait
import org.springframework.data.mongodb.core.awaitExists
import org.springframework.data.mongodb.core.awaitOneOrNull
import org.springframework.data.mongodb.core.flow
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.oneAndAwait
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.MultiValueMap

/**
 * Anwendungslogik für Vorlesungen.
 *
 * @author [Jan Grunicke]
 */
@Service
@Suppress("TooManyFunctions")
class VorlesungService(
    private val mongo: ReactiveFluentMongoOperations,
    @Lazy private val userService: CustomUserDetailsService,
    @Lazy val validatorFactory: ValidatorFactory,
    @Lazy private val mailer: Mailer
) {
    private val validator by lazy { validatorFactory.validator }

    /**
     * Eine Vorlesung anhand ihrer ID suchen.
     * @param id die Id der gesuchten Vorlesung.
     * @param username Der username biem Login.
     * @reutrn Die gefundene Vorlesung oder ein leeres Mono-Objekt.
     */
    @Suppress("ReturnCount")
    suspend fun findById(id: UUID, username: String): Vorlesung? {
        val vorlesung = findById(id) ?: return null

        if (vorlesung.username == username) {
            return vorlesung
        }

        val userDetails = userService.findByUsernameAndAwait(username) ?: return null
        val rollen = userDetails.authorities.map { it.authority }
        if (!rollen.contains(roleAdminStr)) {
            throw AccessForbiddenException(rollen)
        }

        return vorlesung
    }

    suspend fun findById(id: UUID): Vorlesung? {
        val vorlesung = mongo.query<Vorlesung>()
            .matching(query(Vorlesung::id isEqualTo id))
            .awaitOneOrNull()
        logger.debug("findById: {}", vorlesung)
        return vorlesung
    }

    /**
     * Alle Vorlesungen ermiiteln.
     * @return Alle Vorlesungen.
     */
    fun findAll() = mongo.query<Vorlesung>().flow()

    /**
     * Vorlesung anhand von Suchkriterien ermitteln
     * @param queryParams Suchkriterien.
     * @return Gefundene Vorlesungen.
     */
    @Suppress("ReturnCount")
    suspend fun find(queryParams: MultiValueMap<String, String>): Flow<Vorlesung> {
        if (queryParams.isEmpty()) {
            return findAll()
        }

        val criteria = getCriteria(queryParams)
        if (criteria.contains(null)) {
            return emptyFlow()
        }

        val query = Query()
        criteria.filterNotNull()
            .forEach { query.addCriteria(it) }
        logger.debug("{}", query)

        return mongo.query<Vorlesung>()
            .matching(query)
            .flow()
            .onEach { vorlesung -> logger.debug("find: {}", vorlesung) }
    }

    /**
     * Eine neue Vorlesung anlegen.
     * @param vorlesung Das Objekt der neu anzulegenden Vorlesung.
     * @return Die neu angelegte Vorlesung mit generierter ID.
     * @throws InvalidAccountException fass die Benutzererkennung nicht korrekt ist.
     * @throws NameExistsException falls der Name bereits existiert.
     */
    @Transactional
    suspend fun create(vorlesung: Vorlesung): Vorlesung {
        validate(vorlesung)

        vorlesung.user ?: throw InvalidAccountException()

        val name = vorlesung.name
        if (nameExists(name)) {
            throw NameExistsException(name)
        }

        val customUser = createUser(vorlesung)
        val vorlesungDb = create(vorlesung, customUser)
        mailer.send(vorlesungDb)
        return vorlesungDb
    }

    private suspend fun nameExists(name: String) = mongo.query<Vorlesung>()
        .matching(Query(Vorlesung::name isEqualTo name))
        .awaitExists()

    private fun validate(vorlesung: Vorlesung) {
        val violations = validator.validate(vorlesung)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }
    }

    private suspend fun createUser(vorlesung: Vorlesung): CustomUser {
        val userVorlesung = vorlesung.user ?: throw InvalidAccountException()

        // CustomUser is keine "data class", deshalb kein copy()
        val user = with(userVorlesung) {
            CustomUser(
                id = null,
                username = username,
                password = password,
                authorities = listOf(SimpleGrantedAuthority("ROLE VORLESUNG"))
            )
        }

        logger.trace("User wird angelegt: {}", user)
        return userService.create(user)
    }

    private suspend fun create(vorlesung: Vorlesung, user: CustomUser): Vorlesung {
        val neueVorlesung = vorlesung.copy(username = user.username)
        neueVorlesung.user = user
        logger.trace("Vorlesung mit user: {}", vorlesung)
        return mongo.insert<Vorlesung>().oneAndAwait(neueVorlesung)
    }

    /**
     * Eine vorhandene Vorlesung aktualisieren.
     * @param vorlesung Das Objekt mit den neuen Daten.
     * @param id ID der Vorlesung.
     * @param versionStr Versionsnummer.
     * @return Die aktualisierte Vorlesung oder ein leeres Mono-Objekt, falls es keine Vorlesung mit der angegebenen ID gibt.
     * @throws ConstraintViolationException falls es verletze Constraints gibt.
     * @throws InvalidVersionException falls die Versionsnummer nicht korrekt ist.
     * @throws NameExistsException falls der Name bereits existiert.
     */
    @Suppress("MaxLineLength")
    suspend fun update(vorlesung: Vorlesung, id: UUID, versionStr: String): Vorlesung? {
        validate(vorlesung)

        val vorlesungDb = findById(id) ?: return null

        logger.trace("update: version={}, kundeDb={}", versionStr, vorlesungDb)
        val version = versionStr.toIntOrNull() ?: throw InvalidVersionException(versionStr)
        checkName(vorlesungDb, vorlesung.name)
        return update(vorlesung, vorlesungDb, version)
    }

    private suspend fun checkName(vorlesungDb: Vorlesung, neuerName: String) {
        // wurde der Name ueberhaupt geaednert
        if (vorlesungDb.name == neuerName) {
            logger.trace("Email nicht geaendert: {}", neuerName)
            return
        }

        logger.trace("Name geaendert: {} -> {}", vorlesungDb.name, neuerName)
        // Gibt es die neue Emailadresse bei einer existierenden Vorlesung?
        if (nameExists(neuerName)) {
            logger.trace("Neuer Name existiert bereist: {}", neuerName)
            throw NameExistsException(neuerName)
        }
    }

    private suspend fun update(vorlesung: Vorlesung, vorlesungDb: Vorlesung, version: Int): Vorlesung {
        check(mongo::class.isSubclassOf(ReactiveMongoTemplate::class)) {
            "MongoOperations ist nicht MongoTemplate oder davon abgeleitet $mongo::class.java.name"
        }
        mongo as ReactiveMongoTemplate

        val vorlesungCache: MutableCollection<*> = mongo.converter.mappingContext.persistentEntities
        // das DB-Objekt aus dem Cache von Spring Data MongoDB entfernen: sonst doppelte IDs
        // Typecast: sonst gibt es bei remove Probleme mit "Type Interference" infolge von "Type Erasure"
        vorlesungCache.remove(vorlesungDb)

        val neueVorlesung = vorlesung.copy(id = vorlesungDb.id, version = version)
        logger.trace("update: neueVorlesung= {}", neueVorlesung)
        // ggf. OptimisticLockingFailureException
//        return mongo.update<Kunde>()
//            .replaceWith(neuerKunde)
//            .asType<Kunde>()
//            .findReplaceAndAwait()

        return mongo.save(neueVorlesung).awaitFirst()
    }

    /**
     * Eine vorhandene Vorlesung in der DB löschen.
     * @param id Die ID der zu löschenden Vorlesung.
     * @return DeleteResult falls es zur ID ein Vorlesungsobjekt gab, das gelöscht wurde; null sonst.
     */
    // erfordert zusaetzliche Konfiguration in SecurityConfig
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    suspend fun deleteById(id: UUID) = mongo.remove<Vorlesung>()
        .matching(Query(Vorlesung::id isEqualTo id))
        .allAndAwait()

    /**
     * Eine vorhandene Vorlesung löschen.
     * @param name Der Name der zu löschenden Vorlesung.
     * @return DeleteResult falls es zur Email ein Vorlesungsobjekt gab, das gelöscht wurde; null sonst.
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    suspend fun deleteByName(name: String) = mongo.remove<Vorlesung>()
        .matching(Query(Vorlesung::name isEqualTo name))
        .allAndAwait()

    private companion object {
        val logger = logger()
    }
}
