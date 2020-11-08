/*
 * Copyright (C) 2016 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.hska.kunde.config.security

import de.hska.kunde.config.Settings.DEV
import de.hska.kunde.config.logger
import de.hska.kunde.config.security.Daten.users
import java.util.UUID.randomUUID
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.asType
import org.springframework.data.mongodb.core.dropCollection
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.oneAndAwait
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Service-Klasse, um Benutzerkennungen zu suchen und neu anzulegen.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class CustomUserDetailsService(
    private val mongo: ReactiveMongoOperations,
    private val passwordEncoder: PasswordEncoder,
    private val ctx: ApplicationContext
) : ReactiveUserDetailsService, InitializingBean {
    /**
     * Im Profil _dev_ werden vorhandene Benutzerkennungen gelöscht und neu initialisiert.
     * Dazu wird vom Interface InitializingBean abgeleitet.
     *
     * Alternativen sind:
     * * Die Annotation `@PostConstruct` aus dem Artifakt `javax.annotation:javax.annotation-api`.
     * * SmartInitializingSingleton.
     */
    override fun afterPropertiesSet() {
        // Spring Security verwaltet einen Cache von UserDetails
        if (ctx.environment.activeProfiles.contains(DEV)) {
            mongo.dropCollection<CustomUser>()
                .thenMany(users)
                .flatMap(mongo::insert)
                .subscribe { logger.warn("{}", it) }

            // runBlocking {
            //     mongo.dropCollection<CustomUser>().awaitFirstOrNull()
            //     users.collect { user ->
            //        val userDb = mongo.insert<CustomUser>().oneAndAwait(user)
            //         logger.debug("{}", userDb)
            //     }
            // }
        }
    }

    /**
     * Zu einem gegebenen Username wird der zugehörige User gesucht.
     * @param username Username des gesuchten Users
     * @return Der gesuchte User
     */

    override fun findByUsername(username: String?): Mono<UserDetails?> {
        val query = Query(where("username").isEqualTo(username?.toLowerCase()))
        return mongo.query<CustomUser>()
            .matching(query)
            .one()
            .cast(UserDetails::class.java)
    }

    suspend fun findByUsernameAndAwait(username: String): UserDetails? = findByUsername(username).awaitFirstOrNull()

    /**
     * Einen neuen User anlegen
     * * @param user Der neue User
     * @return Der neu angelegte User einschließlich ID.
     * @throws UsernameExistsException Falls der Username bereits existiert.
     */
    @Suppress("LongMethod")
    suspend fun create(user: CustomUser): CustomUser {
        val userExists = mongo.query<CustomUser>()
            .asType<UsernameProj>()
            // Username ist eine Attribut der Java-Klasse User
            .matching(Query(where("username").isEqualTo(user.username)))
            .exists()
            .awaitFirst()
        if (userExists) {
            throw UsernameExistsException(user.username)
        }

        // Die Account-Informationen des Kunden transformieren
        //  in Account-Informationen fuer die Security-Komp.
        val password = passwordEncoder.encode(user.password)
        val authorities = user.authorities
            ?.map { grantedAuthority -> SimpleGrantedAuthority(grantedAuthority.authority) }
            ?: emptyList()
        val neuerUser = CustomUser(
            id = randomUUID(),
            username = user.username.toLowerCase(),
            password = password,
            authorities = authorities
        )
        logger.trace("neuerUser = {}", neuerUser)

        return mongo.insert<CustomUser>().oneAndAwait(neuerUser)
    }

    private companion object {
        val logger = logger()
    }
}

/**
 * Klasse für eine DB-Query mit der Projektion auf die Property "username".
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Eine Projektion mit dem Benutzernamen erstellen.
 */
data class UsernameProj(val username: String)

/**
 * Exception, falls es eine gleichnamige Benutzerkennung bereits gibt.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Eine Exception mit dem fehlerhaften Benutzernamen erstellen.
 */
open class UsernameExistsException(username: String) :
    RuntimeException("Der Username $username existiert bereits")
