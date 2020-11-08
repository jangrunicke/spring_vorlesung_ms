package de.hska.kunde.rest

import de.hska.kunde.Router.Companion.idPathVar
import de.hska.kunde.config.logger
import de.hska.kunde.service.VorlesungMultimediaService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.http.MediaType.parseMediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait

/**
 * Eine Handler-Function wird von der Router-Function [de.hska.kunde.Router.router]
 * aufgerufen, nimmt einen Request entgegen und erstellt den Response.
 *
 * @author Jan Grunicke
 *
 * @constructor Einen VorlesungMultimediaHandler mti einem injizierten
 *      [de.hska.vorlesung.service.VorlesungMultimediaService] erzeugen.
 */
@Component
class VorlesungMultimediaHandler(private val service: VorlesungMultimediaService) {
    /**
     * Eine multimediale (binäre) Datei herunterladen.
     * @param request Das eingehende Request-Objekt mit der Kunde-ID als Pfadvariable.
     * @return Die multimediale Datei oder Statuscode 404, falls es keine Vorlesung
     *      oder keine multimediale Datei gibt.
     */
    @Suppress("LongMethod")
    suspend fun download(request: ServerRequest): ServerResponse {
        val id = request.pathVariable(idPathVar)
        val reactiveGridFsResource = service.findMedia(id) ?: return notFound().buildAndAwait()

        @Suppress("BlockingMethodInNonBlockingContext")
        val length = reactiveGridFsResource.contentLength()
        logger.trace("length = {}", length)

        var contentType = reactiveGridFsResource.gridFSFile
            ?.metadata
            ?.getString("toStringValue")
            ?: ""

        logger.trace("contentType = {}", contentType)
        if (contentType.contains("*")) {
            contentType = "image/png"
        }
        val mediaType = parseMediaType(contentType)
        logger.trace("mediaType = {}", mediaType)

        return ok().contentLength(length)
            .contentType(mediaType)
            .bodyAndAwait(reactiveGridFsResource.downloadStream.asFlow())
    }

    /**
     * Eine multimediale (binäre) Datei hochladen.
     * @param request Der eingehende Request mit der Binärdatei im Rumpf oder als
     *      Teil eines Requests mit dme MIME-Type 'multipart/form-data'.
     * @return Statuscode 204 falls das Hochladne erfolgriech war oder 400 falls
     *      es ein Problem mit der Datei gibt.
     */
    suspend fun upload(request: ServerRequest): ServerResponse {
        val contentType = request.headers()
            .contentType()
            .orElse(null)
            ?: return badRequest().buildAndAwait()
        logger.trace("upload: contentType = {}", contentType)

        val id = request.pathVariable(idPathVar)

        return if (contentType.toString().startsWith("multipart/form-data")) {
            uploadMultipart(request, id)
        } else {
            uploadBinary(request, id, contentType)
        }
    }

    private suspend fun uploadMultipart(request: ServerRequest, id: String): ServerResponse {
        val multiparData = request.body(BodyExtractors.toMultipartData()).awaitFirst()
        val part = multiparData.toSingleValueMap()["file"]
        val contentType = part?.headers()?.contentType ?: return badRequest().buildAndAwait()

        logger.trace("uploadMultipart: contentType part = {}", contentType)
        return save(part.content().asFlow(), id, contentType)
    }

    private suspend fun uploadBinary(request: ServerRequest, id: String, contentType: MediaType): ServerResponse {
        val data = request.body(BodyExtractors.toDataBuffers()).asFlow()
        return save(data, id, contentType)
    }

    @Suppress("ReturnCount")
    private suspend fun save(dataFlow: Flow<DataBuffer?>, id: String, contentType: MediaType): ServerResponse {
        // Flux<DataBuffer> als Mono<List<DataBuffer>>
        val dataList = mutableListOf<DataBuffer?>()
        dataFlow.toList(dataList)
        val dataBuffer = dataList[0] ?: return badRequest().buildAndAwait()

        service.save(dataBuffer, id, contentType) ?: return badRequest().buildAndAwait()
        return noContent().buildAndAwait()
    }

    companion object {
        val logger = logger()
    }
}
