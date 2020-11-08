@file:Suppress("StringLiteralDuplication")

package de.hska.kunde.config.dev

import com.mongodb.reactivestreams.client.MongoCollection
import de.hska.kunde.entity.Vorlesung
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Description
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.mongodb.core.CollectionOptions
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.createCollection
import org.springframework.data.mongodb.core.dropCollection
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.oneAndAwait
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.`object`
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.date
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.int32
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.string
import org.springframework.data.mongodb.core.schema.MongoJsonSchema

/**
 * Interface, um im Profil _dev_ die (Test-) DB neu zu laden.
 *
 * @author Jan Grunicke
 */
interface DbPopulate {
    /**
     * Bean-Definition, um einen CommandLineRunner für das Profil "dev" bereitzustellen
     * damit die (Test-) DB neu geladen wird.
     * @param mongo Template für MongoDB
     * @return CommandLineRunner
     */
    @Bean
    @Description("Test-DB neu laden")
    fun dbPopulate(mongo: ReactiveMongoOperations) = CommandLineRunner {
        val logger = getLogger(DbPopulate::class.java)
        logger.warn("Neuladen der Collection 'Vorlesung'")

        runBlocking {
            mongo.dropCollection<Vorlesung>().awaitFirstOrNull()
            createSchema(mongo, logger)
            createIndexName(mongo, logger)
            createIndexDozent(mongo, logger)
            createIndexRaum(mongo, logger)

            vorlesungen.collect { vorlesung ->
                val vorlesungDb = mongo.insert<Vorlesung>().oneAndAwait(vorlesung)
                logger.debug("{}", vorlesungDb)
            }
        }
    }

    private suspend fun createSchema(mongo: ReactiveMongoOperations, logger: Logger): MongoCollection<Document>? {
        val schema = MongoJsonSchema.builder()
            .required("name", "dozent", "raum")
            .properties(
                int32("version"),
                string("name"),
                `object`("dozent")
                    .properties(
                        string("vorname"),
                        string("nachname")
                    ),
                `object`("raum")
                    .properties(string("gebaeude"), string("Raumnummer")),
                string("username"),
                date("erzeugt"),
                date("aktualisiert")
            )
            .build()
        logger.info("JSON Schema fuer Vorlesung: {}", schema.toDocument().toJson())
        return mongo.createCollection<Vorlesung>(CollectionOptions.empty().schema(schema)).awaitFirst()
    }

    private suspend fun createIndexName(mongo: ReactiveMongoOperations, logger: Logger): String {
        logger.warn("Index fuer 'name'")
        val idx = Index("name", ASC).named("name")
        return mongo.indexOps<Vorlesung>().ensureIndex(idx).awaitFirst()
    }

    private suspend fun createIndexDozent(mongo: ReactiveMongoOperations, logger: Logger): String {
        logger.warn("Index fuer 'dozent'")
        val dozentIdx = Index("dozent", ASC).sparse().named("dozent")
        return mongo.indexOps<Vorlesung>().ensureIndex(dozentIdx).awaitFirst()
    }

    private suspend fun createIndexRaum(mongo: ReactiveMongoOperations, logger: Logger): String {
        logger.warn("Index fuer 'raum'")
        val raumIdx = Index("raum", ASC).sparse().named("raum")
        return mongo.indexOps<Vorlesung>().ensureIndex(raumIdx).awaitFirst()
    }
}
