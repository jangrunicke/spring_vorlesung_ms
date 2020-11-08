package de.hska.kunde.rest

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.sun.mail.util.DecodingException
import de.hska.kunde.Router.Companion.idPathVar
import de.hska.kunde.config.logger
import de.hska.kunde.config.security.UsernameExistsException
import de.hska.kunde.entity.Vorlesung
import de.hska.kunde.rest.constraints.VorlesungConstraintViolation
import de.hska.kunde.rest.hateoas.ListVorlesungModelAssembler
import de.hska.kunde.rest.hateoas.VorlesungModelAssembler
import de.hska.kunde.service.AccessForbiddenException
import de.hska.kunde.service.InvalidVersionException
import de.hska.kunde.service.NameExistsException
import de.hska.kunde.service.VorlesungService
import de.hska.kunde.service.VorlesungServiceException
import java.lang.IllegalArgumentException
import java.net.URI
import java.util.UUID
import javax.validation.ConstraintViolationException
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import org.bson.json.JsonParseException
import org.springframework.cloud.sleuth.annotation.NewSpan
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_MODIFIED
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.status
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait

/**
 * Eine Handler-Funktion wird ovn der Router-Function [de.hska.kunde.Router.router]
 * aufgerufen, nimmt einen Request entgegen und erstellt den Response.
 *
 * @author Jan Grunicke
 *
 * @constructor Einen VorlesungHandler mit einem injizierten [VorlesungService] erzeugen.
 */
@Component
@Suppress("TooManyFunctions", "LargeClass")
class VorlesungHandler(
    private val service: VorlesungService,
    private val modelAssembler: VorlesungModelAssembler,
    private val listModelAssembler: ListVorlesungModelAssembler
) {
    /**
     * Suche anhand der Kunde-ID
     * @param request Der eingehende Request
     * @return Ein Mono-Objekt mit dme Statuscode 200 und dem gefundenen Vorlesung-Objekt einschließlich HATEOAS-Links, oder
     *      aber Statuscode 204.
     */
    @NewSpan("VorlesungHandler.findById")
    @Suppress("ReturnCount", "MaxLineLength")
    suspend fun findById(request: ServerRequest): ServerResponse {
        val idStr = request.pathVariable(idPathVar)
        val id = UUID.fromString(idStr)

        val username = getUsername(request)
        val vorlesung = try {
            service.findById(id, username)
        } catch (e: AccessForbiddenException) {
            return status(FORBIDDEN).buildAndAwait()
        }
        logger.debug("findById: {}", vorlesung)

        if (vorlesung == null) {
            return notFound().buildAndAwait()
        }

        return toResponse(vorlesung, request)
    }

    suspend fun getUsername(request: ServerRequest): String {
        val principal = request.principal().awaitFirst()
        val username = principal.name
        logger.debug("username = {}", username)
        return username
    }

    private suspend fun toResponse(vorlesung: Vorlesung, request: ServerRequest): ServerResponse {
        val versionHeader = getIfNoneMatch(request)
        val versionStr = "\"${vorlesung.version}\""
        if (versionStr == versionHeader) {
            return status(NOT_MODIFIED).buildAndAwait()
        }

        val vorlesungModel = modelAssembler.toModel(vorlesung, request)
        // Entity Tag, um Aenderungen an der angeforderten
        // Ressource erkennen zu koennen.
        // Client: GET-Requests mit Header "If-None-Match"
        //         ggf. Response mit Statuscode NOT MODIFIED (s.o.)
        return ok().eTag(versionStr).bodyValueAndAwait(vorlesungModel)
    }

    private fun getIfNoneMatch(request: ServerRequest): String? {
        val versionHeaderList = try {
            request.headers()
                .asHttpHeaders()
                .ifNoneMatch
        } catch (e: IllegalArgumentException) {
            // falls das ETag syntaktisch nicht korrekt ist
            emptyList<String>()
        }
        val versionHeader = versionHeaderList.firstOrNull()
        logger.debug("versionheader: {}", versionHeader)
        return versionHeader
    }

    /**
     * Suche mit diversen Suchkriterien als Query-Parameter. Es wird 'List<Vorlesung> statt 'Flow<Vorlesung>' zurückgeliefert,
     * damit auch der Statuscode 204 möglich ist.
     * @param request Der eingehende Request mit den Query-Parametern.
     * @return Ein Mono-Objekt mit dem Statuscode 200 und einer Liste mit den gefundenen Vorlesungen einschließlihc
     *      Atom-Links, oder aer Statuscode 204.
     */
    @Suppress("ReturnCount", "MaxLineLength")
    suspend fun find(request: ServerRequest): ServerResponse {
        val queryParams = request.queryParams()

        val vorlesungen = mutableListOf<Vorlesung>()
        service.find(queryParams)
            .onEach { vorlesung -> logger.debug("find: {}", vorlesung) }
            .toList(vorlesungen)

        if (vorlesungen.isEmpty()) {
            logger.debug("find: Keine Vorlesungen gefunden")
            return notFound().buildAndAwait()
        }

        // genau 1 Treffer bei der Suche anhand des Namens
        if (queryParams.keys.contains("name")) {
            val vorlesungModel = modelAssembler.toModel(vorlesungen[0], request)
            logger.debug("find(): Vorlesung mit name: {}", vorlesungModel)
            return ok().bodyValueAndAwait(vorlesungModel)
        }

        val vorlesungModel = vorlesungen.map { vorlesung -> listModelAssembler.toModel(vorlesung, request) }
        logger.debug("find(): {}", vorlesungModel)
        return ok().bodyValueAndAwait(vorlesungModel)
    }

    /**
     * Einen neuen Vorlesungsdatensatz anlegen.
     * @param request Der eingehende Request mit dem Vorlesungs-Datensatz im Body
     * @return Response mit dem Statuscode 201 einschließlich Location-Header oder Statuscode 400 falls Constraints verletzt
     *      sind oder der JSON-Datensatz syntaktisch nicht korrekt ist.
     */
    @Suppress("LongMehtod", "ReturnCount", "MaxLineLength")
    suspend fun create(request: ServerRequest): ServerResponse {
        val vorlesung = try {
            request.awaitBody<Vorlesung>()
        } catch (e: DecodingException) {
            return handleDecodingException(e)
        }

        val neueVorlesung = try {
            service.create(vorlesung)
        } catch (e: ConstraintViolationException) {
            return handleConstraintViolation(e)
        } catch (e: VorlesungServiceException) {
            val msg = e.message ?: ""
            return badRequest().bodyValueAndAwait(msg)
        } catch (e: UsernameExistsException) {
            val msg = e.message ?: ""
            return badRequest().bodyValueAndAwait(msg)
        }

        logger.trace("Vorlesung abgespeichert: {}", neueVorlesung)
        val location = URI("${request.uri()}${neueVorlesung.id}")
        return created(location).buildAndAwait()
    }

    private suspend fun handleConstraintViolation(exception: ConstraintViolationException):
        ServerResponse {
        val violations = exception.constraintViolations
        if (violations.isEmpty()) {
            return badRequest().buildAndAwait()
        }

        val vorlesungViolations = violations.map { violation ->
            VorlesungConstraintViolation(
                property = violation.propertyPath.toString(),
                message = violation.message
            )
        }
        logger.trace("violations: {}", vorlesungViolations)
        return badRequest().bodyValueAndAwait(vorlesungViolations)
    }

    private suspend fun handleDecodingException(e: DecodingException): ServerResponse {
        logger.debug(e.message)
        return when (val exception = e.cause) {
            is JsonParseException -> {
                val msg = exception.message ?: ""
                logger.debug("JsonParseException: {}", msg)
                badRequest().bodyValueAndAwait(msg)
            }
            is InvalidFormatException -> {
                val msg = exception.message ?: ""
                logger.debug("InvalidFormatException: {}", msg)
                badRequest().bodyValueAndAwait(msg)
            }
            else -> status(INTERNAL_SERVER_ERROR).buildAndAwait()
        }
    }

    @Suppress("LongMethod", "ReturnCount", "DuplicatedCode")
    suspend fun update(request: ServerRequest): ServerResponse {
        var version = getIfMatch(request)
            ?: return status(PRECONDITION_FAILED).bodyValueAndAwait("Versionsnummer: fehlt oder falsche Syntax")
        @Suppress("MagicNumber")
        if (version.length < 3) {
            return status(PRECONDITION_FAILED).bodyValueAndAwait("Falsche Versionsnummer $version")
        }
        logger.trace("Versionsnumer $version")
        version = version.substring(1, version.length - 1)

        val idStr = request.pathVariable(idPathVar)
        val id = UUID.fromString(idStr)

        val vorlesung = try {
            request.awaitBody<Vorlesung>()
        } catch (e: DecodingException) {
            return handleDecodingException(e)
        }

        return update(vorlesung, id, version)
    }

    private fun getIfMatch(request: ServerRequest): String? {
        val versionList = try {
            request.headers().asHttpHeaders().ifMatch
        } catch (e: IllegalArgumentException) {
            null
        }
        return versionList?.firstOrNull()
    }

    @Suppress("ReturnCount")
    private suspend fun update(vorlesung: Vorlesung, id: UUID, version: String): ServerResponse {
        val vorlesungAktualisiert = try {
            service.update(vorlesung, id, version) ?: return notFound().buildAndAwait()
        } catch (e: ConstraintViolationException) {
            return handleConstraintViolation(e)
        } catch (e: InvalidVersionException) {
            val msg = e.message ?: ""
            logger.trace("InvalidVersionException: {}", msg)
            return status(PRECONDITION_FAILED).bodyValueAndAwait(msg)
        } catch (e: NameExistsException) {
            val msg = e.message ?: ""
            logger.trace("NameExistsException: {}", msg)
            return badRequest().bodyValueAndAwait(msg)
        } catch (e: OptimisticLockingFailureException) {
            val msg = e.message ?: ""
            logger.trace("OptimisticLockingFailureException: {}", msg)
            return status(PRECONDITION_FAILED).bodyValueAndAwait("Falsche Versionsnummer $version")
        }

        logger.trace("Vorlesung aktualisiert: {}", vorlesungAktualisiert)
        return noContent().eTag("\"${vorlesungAktualisiert.version}\"").buildAndAwait()
    }

    /**
     * Eine vorhandene Vorlesung anhand ihrer ID löschen.
     * @param request Der eingehende Request mti der ID asl Pfad-Parameter.
     * @return Respose mit Statuscode 204.
     */
    suspend fun deleteById(request: ServerRequest): ServerResponse {
        val idStr = request.pathVariable(idPathVar)
        val id = UUID.fromString(idStr)
        val deleteResult = service.deleteById(id)
        logger.debug("deleteById(): {}", deleteResult)

        return noContent().buildAndAwait()
    }

    /**
     * Eine vorhandene Vorlesung anhand ihres Namens löschen
     * @param request Der eingehende Request mit dem Namen als Query-Paramter.
     * @return Response mit dem Statuscode 204.
     */
    @Suppress("ReturnCount")
    suspend fun deleteByName(request: ServerRequest): ServerResponse {
        val name = request.queryParam("name")
        if (name.isEmpty) {
            return noContent().buildAndAwait()
        }

        val deleteResult = service.deleteByName(name.get())
        logger.debug("deleteById(): {}", deleteResult)
        return noContent().buildAndAwait()
    }

    private companion object {
        val logger = logger()
    }
}
