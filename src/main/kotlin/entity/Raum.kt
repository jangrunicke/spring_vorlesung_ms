package de.hska.kunde.entity

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Pattern

/**
 * Klasse für einen Raum
 *
 * @author [Jan Grunicke]
 *
 * @property gebaeude Gebäude name als Buchstaben.
 * @property raumnummer Raumnummer als String.
 */
data class Raum(
    @get:NotEmpty(message = "{raum.gebäude.notEmpty}")
    @get:Pattern(
        regexp = "A-ZÄÖÜ",
        message = "{raum.gebäude.pattern}"
    )
    val gebaeude: String,

    @get:NotEmpty(message = "{raum.raumnummer.notEmpty}")
    @get:Pattern(
        regexp = "\\d{3}",
        message = "{raum.raumnummer.pattern}"
    )
    val raumnummer: String
)
