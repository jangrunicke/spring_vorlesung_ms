package de.hska.kunde.rest

import de.hska.kunde.Router.Companion.idPathVar
import de.hska.kunde.Router.Companion.prefixPathVar
import de.hska.kunde.service.VorlesungValuesService
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait

/**
 * Handler für die Abfrage von Werten (für "Software Engineering").
 *
 * @author Jan Grunicke
 */
@Component
class VorlesungValuesHandler(private val service: VorlesungValuesService) {
    /**
     * Abfrage welche Vorlesungsnamen es zu einem Präfix gibt.
     * @param requerst Der eingehende Request mit dem Präfix asl Pfadvariable.
     * @return Die passenden Namen oder Statuscode 404, falls es keine gibt.
     */
    suspend fun findNamenByPrefix(request: ServerRequest): ServerResponse {
        val prefix = request.pathVariable(prefixPathVar)

        val namen = mutableListOf<String>()
        service.findNamenByPrefix(prefix).toList(namen)
        if (namen.isEmpty()) {
            notFound().buildAndAwait()
        }

        return ok().bodyValueAndAwait(namen)
    }

    /**
     * Abfrage, welche Version es zu einer Vorlesung-ID gibt.
     * @param request Der eingehende Request mit der ID als Pfadvariable.
     * @return Die Versionsnummer.
     */
    suspend fun findVersionById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable(idPathVar)
        val version = service.findVersionById(id)
        return ok().bodyValueAndAwait(version.toString())
    }
}
