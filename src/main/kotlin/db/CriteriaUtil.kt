/*
 * Copyright (C) 2017 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package de.hska.kunde.db

import de.hska.kunde.config.logger
import de.hska.kunde.entity.Dozent
import de.hska.kunde.entity.Raum
import de.hska.kunde.entity.Vorlesung
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.CriteriaDefinition
import org.springframework.data.mongodb.core.query.div
import org.springframework.data.mongodb.core.query.regex
import org.springframework.util.MultiValueMap

/**
 * Singleton-Klasse, um _Criteria Queries_ für _MongoDB_ zu bauen.
 *
 * @author [Jan Grunicke]
 */
object CriteriaUtil {
    private const val name = "name"
    private const val dozent = "dozent"
    private const val raumnummer = "raum"
    private const val gebaeude = "gebauede"
    private val logger = logger()

    /**
     * Eine `MultiValueMap` von _Spring_ wird in eine Liste von
     * `CriteriaDefinition` für _MongoDB_ konvertiert, um flexibel nach Vorlesungen
     * suchen zu können.
     * @param queryParams Die Query-Parameter in einer `MultiValueMap`.
     * @return Eine Liste von `CriteriaDefinition`.
     */
    @Suppress("ComplexMethod", "LongMethod")
    fun getCriteria(queryParams: MultiValueMap<String, String>): List<CriteriaDefinition?> {
        val criteria = queryParams.map { (key, value) ->
            if (value?.size != 1) {
                null
            } else {
                val critVal = value[0]
                when (key) {
                    name -> getCriteriaName(critVal)
                    dozent -> getCriteriaDozent(critVal)
                    raumnummer -> getCriteriaRaumnummer(critVal)
                    gebaeude -> getCriteriaGebaude(critVal)
                    else -> null
                }
            }
        }

        logger.debug("#Criteria: {}", criteria.size)
        criteria.forEach { logger.debug("Criteria: {}", it?.criteriaObject) }
        return criteria
    }

    // Name: Suche nach Teilstrings ohne Gross-/Kleinschreibung
    private fun getCriteriaName(name: String) = Vorlesung::name.regex(name, "i")

    private fun getCriteriaDozent(dozentStr: String): Criteria? {
        return (Vorlesung::dozent / Dozent::nachname).regex(dozentStr, "i")
    }

    private fun getCriteriaRaumnummer(raumnummerStr: String): Criteria? {
        return (Vorlesung::raum / Raum::raumnummer).regex(raumnummerStr, "i")
    }

    private fun getCriteriaGebaude(gebaeudeStr: String): Criteria? {
        return (Vorlesung::raum / Raum::gebaeude).regex(gebaeudeStr, "i")
    }
}
