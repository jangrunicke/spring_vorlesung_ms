package de.hska.kunde.rest

import de.hska.kunde.rest.hateoas.ListVorlesungModelAssembler
import de.hska.kunde.service.VorlesungStreamService
import kotlinx.coroutines.flow.map
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyAndAwait

/**
 * Eine Handler-Funktion wird von der Router-Function [de.hska.kunde.Router.router]
 * aufgerufen, nimmt einen Request entgegen und erstellt den Response.
 *
 * @author Jan Grunicke
 *
 * @constructor Einen VorlesungStreamHandler mit einem injizierten
 *      [de.hska.service.VorlesungStreamService] erzeugen.
 */
@Component
class VorlesungStreamHandler(
    private val service: VorlesungStreamService,
    private val modelAssembler: ListVorlesungModelAssembler
) {
    /**
     * Alle Vorlesungen als Event-Stream zurückliefern.
     * @param request Das eingehende Request-Objekt.
     * @return Response mit dem MIME-Typ 'text/event-stream
     */
    suspend fun findAll(request: ServerRequest): ServerResponse {
        val vorlesungen = service.findAll()
            .map { modelAssembler.toModel(it, request) }

        return ok().contentType(TEXT_EVENT_STREAM).bodyAndAwait(vorlesungen)
    }
}
