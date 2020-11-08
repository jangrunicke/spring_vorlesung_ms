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
package de.hska.kunde.config.dev

import de.hska.kunde.config.Settings.DEV
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.CommandLineRunner
import org.springframework.cloud.consul.discovery.reactive.ConsulReactiveDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

/**
 * Registrierte Service-Instanzen fuer den Microservice "kunde" protokollieren.
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface LogDiscoveryClient {
    /**
     * Bean-Definition, um einen CommandLineRunner für das Profil "dev" bereitzustellen,
     * damit _Service Discovery_ für den Service _kunde_ abgefragt wird.
     * @param discoveryClient ReactiveDiscoveryClient für Service Discovery.
     * @return CommandLineRunner
     */
    @InternalCoroutinesApi
    @Bean
    @Profile(DEV)
    // FIXME https://github.com/spring-cloud/spring-cloud-consul/issues/593
    fun logDiscoveryClient(discoveryClient: ConsulReactiveDiscoveryClient) = CommandLineRunner {
        val logger = getLogger(LogDiscoveryClient::class.java)

        runBlocking {
            with(discoveryClient) {
                services.asFlow()
                    .collect { service ->
                        logger.warn("Service: $service")
                        getInstances(service).asFlow()
                            .collect { instance -> logger.warn("Instanz: ${instance.uri}") }
                    }
            }
        }
    }
}
