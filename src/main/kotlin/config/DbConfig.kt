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
package de.hska.kunde.config

import com.mongodb.WriteConcern.ACKNOWLEDGED
import de.hska.kunde.config.security.CustomUser
import de.hska.kunde.entity.Vorlesung
import java.util.UUID
import java.util.UUID.randomUUID
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.ReactiveMongoTransactionManager
import org.springframework.data.mongodb.core.WriteConcernResolver
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.mapping.event.ReactiveBeforeConvertCallback
import reactor.kotlin.core.publisher.toMono

/**
 * Spring-Konfiguration für den Zugriff auf _MongoDB_.
 *
 * @author Jan Grunicke
 */
interface DbConfig {

    @Bean
    fun customConversions() = MongoCustomConversions(
        listOf(
            UUIDReadConverter(),
            UUIDWriteConverter(),

            // Rollen fuer Security
            CustomUser.RoleReadConverter(),
            CustomUser.RoleWriteConverter()
        )
    )

    /**
     * Konvertierungsklasse für MongoDB, um einen String einzulesen und eine UUID zu erzeugen.
     * Wegen @ReadingConverter ist kein Lambda-Ausdruck möglich.
     */
    @ReadingConverter
    class UUIDReadConverter : Converter<String, UUID> {
        /**
         * Konvertierung eines Strings in eine UUID.
         * @param uuid String mit einer UUID.
         * @return Zugehörige UUID
         */
        override fun convert(uuid: String): UUID = UUID.fromString(uuid)
    }

    /**
     * Konvertierungsklasse für MongoDB, um eine UUID in einen String zu konvertieren.
     * Wegen @WritingConverter ist kein Lambda-Ausdruck möglich.
     */
    @WritingConverter
    class UUIDWriteConverter : Converter<UUID, String> {
        /**
         * Konvertierung einer UUID in einen String, z.B. beim Abspeichern.
         * @param uuid Objekt von UUID
         * @return String z.B. zum Abspeichern.
         */
        override fun convert(uuid: UUID): String? = uuid.toString()
    }

    /**
     * Bean zur Generierung der Vorlesung-ID beim Anlegen eines neuen Kunden
     * @return Kunde-Objekt mit einer Kunde-ID
     */
    @Bean
    fun generateVorlesungId() = ReactiveBeforeConvertCallback<Vorlesung> { vorlesung, _ ->
        if (vorlesung.id == null) {
            val vorlesungMitId = vorlesung.copy(id = randomUUID(), name = vorlesung.name.toLowerCase())
            LoggerFactory.getLogger(DbConfig::class.java).debug("generateKundeId: {}", vorlesungMitId)
            vorlesungMitId
        } else {
            vorlesung
        }.toMono()
    }

    /**
     * Bean für Optimistische Synchronisation
     * @return ACKNOWLEDGED als "WriteConcern" für MongoDB
     */
    @Bean
    fun writeConcernResolver() = WriteConcernResolver { ACKNOWLEDGED }

    /**
     * Autokonfiguration für MongoDB-Transaktionen bei "Reactive Programming".
     * @return Instanziiertes Objekt der Klasse ReactiveMongoTransactionManager.
     */
    @Bean
    fun reactiveTransactionManager(factory: ReactiveMongoDatabaseFactory) = ReactiveMongoTransactionManager(factory)
}
