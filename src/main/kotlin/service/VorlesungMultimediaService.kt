package de.hska.kunde.service

import de.hska.kunde.config.logger
import de.hska.kunde.entity.Vorlesung
import java.time.Duration
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.exists
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import reactor.kotlin.core.publisher.toMono

/**
 * Anwendungslogik für multimediale Daten zu Vorlesungen.
 *
 * @author [Jan Grunicke]
 */
@Service
class VorlesungMultimediaService(
    private val mongoOps: ReactiveMongoOperations,
    private val gridFsTemplate: ReactiveGridFsTemplate
) {
    /**
     * Multimediale Datei (Bild oder Video) zu einer Vorlesung mit gegebener ID ermitteln.
     * @param vorlesungId Vorlesung-ID
     * @return Multimediale Datei, falls sie existiert. Sonst empty()
     */
    suspend fun findMedia(vorlesungId: String): ReactiveGridFsResource? {
        val vorlesungExists = mongoOps.exists<Vorlesung>(Query(Vorlesung::id isEqualTo vorlesungId))
            .timeout(timeout)
            .awaitFirst()

        if (!vorlesungExists) {
            logger.debug("findMedia(): Keine Vorlesung mit der ID {}", vorlesungId)
            return null
        }

        val gridFsResource = gridFsTemplate.getResource(vorlesungId).awaitFirstOrNull()
        if (gridFsResource != null) {
            logger.debug(
                "findMedia(): Binaerdatei {} der Laenge {} mit contentType {}",
                gridFsResource.filename,
                gridFsResource.contentLength(),
                gridFsResource.gridFSFile
                    ?.metadata
                    ?.getString("toStringValue")
                    ?: ""
            )
        }
        return gridFsResource
    }

    /**
     * Multimediale Daten aus einem Inputstream werden persistent mit der gegeenen
     * Vorlesung-Id als Dateiname abgespeichert. Der Inputstream wird am Ende geshclossen.
     *
     * @param dataBuffer DataBuffer mit multimedialen Daten.
     * @param vorlesungId Vorlesung-ID
     * @param contentType MIME-Typen, z.B. image/png
     * @return ID der neuangelegten multimedialen Datei oder null
     */
    // FIXME @Transactional
    suspend fun save(dataBuffer: DataBuffer, vorlesungId: String, contentType: MediaType): ObjectId? {
        val vorlesungExists = mongoOps.exists<Vorlesung>(Query(Vorlesung::id isEqualTo vorlesungId))
            .timeout(timeout)
            .awaitFirst()

        if (!vorlesungExists) {
            logger.debug("save(): Keine Vorleusng mit dre ID {}", vorlesungId)
            return null
        }

        // TODO MIME-Type uerpruefen
        logger.warn("TODO: MIME-Type ueberpruefen")

        // ggf. multimediale Datei loeschen
        val criteria = Criteria.where("filename").isEqualTo(vorlesungId)
        val query = Query(criteria)
        gridFsTemplate.delete(query).awaitFirstOrNull()

        // store() schliesst auch den Inputstream
        val objectId = gridFsTemplate
            .store(dataBuffer.toMono(), vorlesungId, contentType)
            .awaitFirst()

        logger.debug("save(): Binaerdatei wurde angelegt: ObjectId={}, contentType={]", objectId, contentType)
        return objectId
    }

    private companion object {
        val logger = logger()
        @Suppress("MagicNumber", "HasPlatformType")
        val timeout = Duration.ofMillis(500)
    }
}
