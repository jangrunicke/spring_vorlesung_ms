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
package de.hska.kunde.config.dev

import de.hska.kunde.entity.Dozent
import de.hska.kunde.entity.Raum
import de.hska.kunde.entity.Vorlesung
import java.util.UUID
import kotlinx.coroutines.flow.flowOf

/**
 * Testdaten für Vorlesungen
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Suppress("MagicNumber", "UnderscoresInNumericLiterals")
val vorlesungen = flowOf(
    Vorlesung(
        id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
        name = "Rechnungswesen",
        dozent = Dozent(
            "Michael", "Reichardt"
        ),
        raum = Raum(
            "M", "310"
        ),
        username = "admin"
    ),
    Vorlesung(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        name = "Mathematik",
        dozent = Dozent(
            "Thomas", "Morgenstern"
        ),
        raum = Raum(
            "M", "304"
        ),
        username = "admin"
    ),
    Vorlesung(
        id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
        name = "allgemeine BWL",
        dozent = Dozent(
            "Karl", "Dübon"
        ),
        raum = Raum(
            "M", "306"
        ),
        username = "admin"
    ),
    Vorlesung(
        id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
        name = "Programmieren",
        dozent = Dozent(
            "Udo", "Müller"
        ),
        raum = Raum(
            "M", "301"
        ),
        username = "admin"
    ),
    Vorlesung(
        id = UUID.fromString("00000000-0000-0000-0000-000000000004"),
        name = "Datenbanken",
        dozent = Dozent(
            "Andreas", "Schmidt"
        ),
        raum = Raum(
            "M", "210"
        ),
        username = "admin"
    ),
    Vorlesung(
        id = UUID.fromString("00000000-0000-0000-0000-000000000005"),
        name = "Volkswirtschaftslehre",
        dozent = Dozent(
            "Michael", "Reichardt"
        ),
        raum = Raum(
            "M", "206"
        ),
        username = "admin"
    )
)
