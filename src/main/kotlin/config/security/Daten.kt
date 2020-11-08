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
package de.hska.kunde.config.security

import java.util.UUID
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import reactor.core.publisher.Flux

/**
 * Testdaten für Benutzernamen, Passwörter und Rollen
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Suppress("UnderscoresInNumericLiterals")
object Daten {
    /**
     * Konstante für Rolle _admin_
     */
    const val roleAdminStr = "ROLE_ADMIN"
    private const val roleKundeStr = "ROLE_KUNDE"
    private const val roleActuatorStr = "ROLE_ACTUATOR"

    private val roleKunde = SimpleGrantedAuthority(roleKundeStr)
    private val roleAdmin = SimpleGrantedAuthority(roleAdminStr)
    private val roleActuator = SimpleGrantedAuthority(roleActuatorStr)

    // Default-Verschluesselung durch bcrypt
    private val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
    private val password = passwordEncoder.encode("p")

    /**
     * Testdaten für Benutzer
     */
    val users = Flux.just(
        CustomUser(
            id = UUID.fromString("10000000-0000-0000-0000-000000000000"),
            username = "admin",
            password = password,
            authorities = listOf(
                roleAdmin,
                roleKunde,
                roleActuator
            )
        ),
        CustomUser(
            id = UUID.fromString("10000000-0000-0000-0000-000000000001"),
            username = "alpha1",
            password = password,
            authorities = listOf(roleKunde)
        ),
        CustomUser(
            id = UUID.fromString("10000000-0000-0000-0000-000000000002"),
            username = "alpha2",
            password = password,
            authorities = listOf(roleKunde)
        ),
        CustomUser(
            id = UUID.fromString("10000000-0000-0000-0000-000000000003"),
            username = "alpha3",
            password = password,
            authorities = listOf(roleKunde)
        ),
        CustomUser(
            id = UUID.fromString("10000000-0000-0000-0000-000000000004"),
            username = "delta",
            password = password,
            authorities = listOf(roleKunde)
        ),
        CustomUser(
            id = UUID.fromString("10000000-0000-0000-0000-000000000005"),
            username = "epsilon",
            password = password,
            authorities = listOf(roleKunde)
        ),
        CustomUser(
            id = UUID.fromString("10000000-0000-0000-0000-000000000006"),
            username = "phi",
            password = password,
            authorities = listOf(roleKunde)
        )
    )
}
