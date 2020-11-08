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

import de.hska.kunde.Router.Companion.authPath
import de.hska.kunde.Router.Companion.multimediaPath
import de.hska.kunde.Router.Companion.namePath
import de.hska.kunde.Router.Companion.versionPath
import de.hska.kunde.config.Settings.DEV
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

/**
 * Security-Konfiguration.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
// https://github.com/spring-projects/spring-security/tree/master/samples
interface SecurityConfig {
    /**
     * Bean-Definition, um den Zugriffsschutz an der REST-Schnittstelle zu
     * konfigurieren.
     *
     * @param http Injiziertes Objekt von `ServerHttpSecurity` als
     *      Ausgangspunkt für die Konfiguration.
     * @return Objekt von `SecurityWebFilterChain`
     */
    @Bean
    @Suppress("LongMethod")
    // FIXME DSL https://github.com/spring-projects/spring-security/issues/5557
    fun securityWebFilterChain(http: ServerHttpSecurity, ctx: ApplicationContext): SecurityWebFilterChain =
        http.authorizeExchange { exchanges ->
            exchanges
                .pathMatchers(POST, kundePath).permitAll()
                .pathMatchers(GET, kundePath).hasAnyRole(adminRolle)
                .pathMatchers(GET, kundeIdPath).hasAnyRole(adminRolle, kundeRolle)
                .pathMatchers(GET, multimediaIdPath).hasRole(kundeRolle)
                .pathMatchers(GET, rollenPath).hasRole(kundeRolle)
                .pathMatchers(GET, "$namePath/*").hasRole(kundeRolle)
                .pathMatchers(GET, "$versionPath/*").hasRole(kundeRolle)

                .pathMatchers(PUT, kundeIdPath, multimediaIdPath)
                .hasRole(kundeRolle)
                .pathMatchers(PATCH, kundeIdPath).hasRole(adminRolle)
                .pathMatchers(DELETE, kundeIdPath).hasRole(adminRolle)
                .pathMatchers(OPTIONS).permitAll()

                .matchers(EndpointRequest.to("health")).permitAll()
                .matchers(EndpointRequest.toAnyEndpoint()).hasRole(endpointAdminRolle)
        }

            .httpBasic {}

            // keine generierte HTML-Seite fuer Login
            .formLogin { form -> form.disable() }

            // als Default sind durch ServerHttpSecurity aktiviert:
            // * Keine  XSS (= Cross-site scripting) Angriffe: Header "X-XSS-Protection: 1; mode=block"
            //   https://www.owasp.org/index.php/Cross-site_scripting
            // * Kein CSRF (= Cross-Site Request Forgery) durch CSRF-Token
            //   https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)_Prevention_Cheat_Sheet
            // * Kein Clickjacking: im Header "X-Frame-Options: DENY"
            //   https://www.owasp.org/index.php/Clickjacking
            //   http://tools.ietf.org/html/rfc7034
            // * HSTS (= HTTP Strict Transport Security) für HTTPS: im Header
            //      "Strict-Transport-Security: max-age=31536000 ; includeSubDomains"
            //   https://www.owasp.org/index.php/HTTP_Strict_Transport_Security
            //   https://tools.ietf.org/html/rfc6797
            // * Kein MIME-sniffing: im Header "X-Content-Type-Options: nosniff"
            //   https://blogs.msdn.microsoft.com/ie/2008/09/02/ie8-security-part-vi-beta-2-update
            //   http://msdn.microsoft.com/en-us/library/gg622941%28v=vs.85%29.aspx
            //   https://tools.ietf.org/html/rfc7034
            // * im Header: "Cache-Control: no-cache, no-store, max-age=0, must-revalidate"

            // CSP = Content Security Policy
            //  https://www.owasp.org/index.php/HTTP_Strict_Transport_Security
            //  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy
            //  https://tools.ietf.org/html/rfc7762
            .headers { headers -> headers.contentSecurityPolicy("default-src 'self'") }

            .csrf { csrf ->
                if (ctx.environment.activeProfiles.contains(DEV)) {
                    // CSRF wird im Profil "dev" deaktiviert, damit der interaktive REST-Client von IntelliJ benutzt
                    // werden kann und damit man in den Tests auch keinen "CSRF Token" generieren muss.
                    csrf.disable()
                }
            }.build()

    companion object {
        private const val adminRolle = "ADMIN"
        private const val kundeRolle = "KUNDE"
        private const val endpointAdminRolle = "ACTUATOR"

        private const val kundePath = "/"
        private const val kundeIdPath = "/*"
        private const val multimediaIdPath = "$multimediaPath/*"
        private const val rollenPath = "$authPath/rollen"
    }
}
