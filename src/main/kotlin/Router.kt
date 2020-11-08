/*
 * Copyright (C) 2018 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package de.hska.kunde

import de.hska.kunde.config.logger
import de.hska.kunde.config.security.AuthHandler
import de.hska.kunde.entity.Vorlesung
import de.hska.kunde.rest.VorlesungHandler
import de.hska.kunde.rest.VorlesungMultimediaHandler
import de.hska.kunde.rest.VorlesungStreamHandler
import de.hska.kunde.rest.VorlesungValuesHandler
import kotlinx.coroutines.FlowPreview
import org.springframework.context.annotation.Bean
import org.springframework.hateoas.MediaTypes.HAL_JSON
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.web.reactive.function.server.coRouter

/**
 * Spring-Konfiguration mit der Router-Function für die REST-Schnittstelle.
 *
 * @author Jan Grunicke
 */
interface Router {
    /**
     * Bean-Function, um das Routing mit _Spring WebFlux_ funktional zu
     * konfigurieren.
     *
     * @param handler Objekt der Handler-Klasse [VorlesungHandler] zur Behandlung
     *      von Requests.
     * @param streamHandler Objekt der Handler-Klasse [VorlesungStreamHandler]
     *      zur Behandlung von Requests mit Streaming.
     * @param multimediaHandler Objekt der Handler-Klasse [VorlesungMultimediaHandler]
     *      zur Behandlung von Requests mit multimedialen Daten.
     * @param valuesHandler Objekt der Handler-Klasse [VorlesungValuesHandler]
     *      zur Behandlung von Requests bzgl. einfachen Werten.
     * @param authHandler Objekt der Handler-Klasse [AuthHandler]
     *      zur Behandlung von Requests bzgl. Authentifizierung und Autorisierung.
     * @return Die konfigurierte Router-Function.
     */
    @Bean
    @FlowPreview
    @Suppress("LongMethod")
    fun router(
        handler: VorlesungHandler,
        streamHandler: VorlesungStreamHandler,
        multimediaHandler: VorlesungMultimediaHandler,
        valuesHandler: VorlesungValuesHandler,
        authHandler: AuthHandler
    ) = coRouter {
        // https://github.com/spring-projects/spring-framework/blob/master/...
        //       ..spring-webflux/src/main/kotlin/org/springframework/web/...
        //       ...reactive/function/server/RouterFunctionDsl.kt
        accept(HAL_JSON).nest {
            GET("/", handler::find)
            GET("/$idPathPattern", handler::findById)
        }

        accept(TEXT_EVENT_STREAM).nest {
            GET("/", streamHandler::findAll)
        }

        accept(TEXT_PLAIN).nest {
            // fuer "Software Engineering" und Android
            GET("$namePath/{$prefixPathVar}", valuesHandler::findNamenByPrefix)
            GET("$versionPath/$idPathPattern", valuesHandler::findVersionById)
        }

        POST("/", handler::create)
        PUT("/$idPathPattern", handler::update)

        DELETE("/$idPathPattern", handler::deleteById)
        DELETE("/", handler::deleteByName)

        multimediaPath.nest {
            GET("/$idPathPattern", multimediaHandler::download)
            PUT("/$idPathPattern", multimediaHandler::upload)
        }

        "/auth".nest {
            GET("/rollen", authHandler::findEigeneRollen)
        }

        // ggf. weitere Routen: z.B. HTML mit ThymeLeaf, Mustache, FreeMarker
    }
        .filter { request, next ->
            logger.trace(
                "Filter vor dem Aufruf eines Handlers: {}",
                request.uri()
            )
            next.handle(request)
        }

    companion object {
        /**
         * Name der Pfadvariablen für IDs.
         */
        const val idPathVar = "id"

        private const val idPathPattern = "{$idPathVar:${Vorlesung.ID_PATTERN}}"

        /**
         * Pfad für multimediale Dateien
         */
        const val multimediaPath = "/multimedia"

        /**
         * Pfad für Authentifizierung und Autorisierung
         */
        const val authPath = "/auth"

        /**
         * Pfad, um Namen abzufragen
         */
        const val namePath = "/name"

        /**
         * Pfad, um Versionsnummern abzufragen
         */
        const val versionPath = "/version"

        /**
         * Name der Pfadvariablen, wenn anhand eines Präfix gesucht wird.
         */
        const val prefixPathVar = "prefix"

        private val logger = logger()
    }
}
