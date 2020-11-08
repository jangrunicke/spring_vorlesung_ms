/*
 * Copyright (C) 2019 - present Juergen Zimmermann, Hochschule Karlsruhe
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

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import java.time.Duration.ofSeconds
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.Customizer
import org.springframework.context.annotation.Bean

/**
 * Autokonfiguration für Resilience4J.
 *
 * @author Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface Resilience4jConfig {
    /**
     * Autokonfiguration für Resilience4JCircuitBreakerFactory.
     * https://github.com/spring-cloud/spring-cloud-circuitbreaker/blob/master/docs/src/main/asciidoc/spring-cloud-circuitbreaker-resilience4j.adoc
     * @return Instanziiertes Objekt der Klasse Customizer<Resilience4JCircuitBreakerFactory>.
     */
    @Bean
    fun defaultCustomizer() =
        Customizer<Resilience4JCircuitBreakerFactory> { factory ->
            factory.configureDefault { id ->
                Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                    // Defaultwerte von Resilience4j
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                            .timeoutDuration(ofSeconds(1))
                            .build()
                    )
                    .build()
            }
        }
}
