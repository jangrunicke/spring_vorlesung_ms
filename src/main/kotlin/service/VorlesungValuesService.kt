package de.hska.kunde.service

import de.hska.kunde.entity.Vorlesung
import kotlinx.coroutines.flow.map
import org.springframework.data.mongodb.core.ReactiveFindOperation
import org.springframework.data.mongodb.core.asType
import org.springframework.data.mongodb.core.awaitOne
import org.springframework.data.mongodb.core.flow
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service

/**
 * Anwendungslogik für Werte von Vorlesungen (für "Software Engineering").
 * @author Jan Grunicke
 */
@Service
class VorlesungValuesService(private val mongo: ReactiveFindOperation) {
    /**
     * Namen anahnd eines Präfix ermittln.
     *
     * @param prefix Präfix für Namen
     * @return Gefundene Namen
     */
    @Suppress("DEPRECATION")
    fun findNamenByPrefix(prefix: String) = mongo.query<Vorlesung>()
        .distinct("name")
        .asType(NameProj::class)
        .matching(Query(where(Vorlesung::name).regex("^$prefix", "i")))
        .flow()
        .map { it.name }

    suspend fun findVersionById(id: String) = mongo.query<Vorlesung>()
        .asType<VersionProj>()
        .matching(Query.query(where(Vorlesung::id).isEqualTo(id)))
        .awaitOne()
        .version
}
data class NameProj(val name: String)
data class VersionProj(val version: Int)
