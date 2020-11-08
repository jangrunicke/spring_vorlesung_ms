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
package de.hska.kunde.config.dev

import org.springframework.context.annotation.Bean
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

/**
 * Interface, um im Profil _dev_ CORS (= Cross-Origin Resource Sharing) zu konfigurieren.
 *
 * - http://www.w3.org/TR/cors
 * - https://github.com/spring-projects/spring-framework/blob/master/src/docs/asciidoc/web/webflux-cors.adoc
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface CorsConfig {
    /**
     * Bean-Definition, um CORS für das Profil "dev" bereitzustellen.
     * @return CorsWebFilter
     */
    @Bean
    @Suppress("LongMethod")
    fun corsFilter(): CorsWebFilter {
        val source = UrlBasedCorsConfigurationSource()
            .apply {
                val config = CorsConfiguration().apply {
                    allowedOrigins = listOf("https://localhost", "https://127.0.0.1")
                    allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    allowedHeaders = listOf(
                        "Origin",
                        "Content-Type",
                        "Accept",
                        "Authorization",
                        "Access-Control-Allow-Origin",
                        "Access-Control-Allow-Methods",
                        "Access-Control-Allow-Headers",
                        "Access-Control-Expose-Headers",
                        "Allow",
                        "Content-Length",
                        "Date",
                        "If-None-Match",
                        "If-Match",
                        "Last-Modified",
                        "If-Modified-Since"
                    )
                    exposedHeaders = listOf(
                        "Location",
                        "ETag",
                        "Access-Control-Allow-Origin",
                        "Access-Control-Allow-Headers"
                    )
                }
                registerCorsConfiguration("/**", config)
            }

        return CorsWebFilter(source)
    }
}
