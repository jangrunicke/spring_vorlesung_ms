package de.hska.kunde.service

import de.hska.kunde.entity.Vorlesung
import org.springframework.data.mongodb.core.ReactiveFindOperation
import org.springframework.data.mongodb.core.flow
import org.springframework.data.mongodb.core.query
import org.springframework.stereotype.Service

/**
 * Anwendungslogik für Streaming von Vorlesungen.
 *
 * @author [Jan Grunicke]
 */
@Service
class VorlesungStreamService(private val mongo: ReactiveFindOperation) {
    /**
     * Alle Vorlesungen als Flux ermitteln, wie sie später auch von der DB kommen.
     * @return Alle Vorlesungen
     */
    fun findAll() = mongo.query<Vorlesung>().flow()
}
