package de.hska.kunde.entity

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Pattern

/**
 * Klasse für einen Dozenten der eine Vorlesung hält
 *
 * @author [Jan Grunicke]
 *
 * @property vorname Vorname des Dozenten.
 * @property nachname Nachname des Dozenten.
 */
data class Dozent(
    @get:NotEmpty(message = "{dozent.vorname.notEmpty}")
    @get:Pattern(
        regexp = VORNAME_PATTERN,
        message = "{dozent.vorname.pattern]"
    )
    val vorname: String,

    @get:NotEmpty(message = "{dozent.nachname.notEmpty}")
    @get:Pattern(
        regexp = NACHNAME_PATTERN,
        message = "{dozent.nachname.pattern]"
    )
    val nachname: String
) {
    companion object {
        private const val NACHNAME_PREFIX = "o'|von|von der|von und zu|van"

        const val NAME_PATTERN = "[A-ZÄÖÜ][a-zäöüß]+"

        /**
         * Muster für einen Nachnamen
         */
        const val NACHNAME_PATTERN = "($NACHNAME_PREFIX)?$NAME_PATTERN(-$NAME_PATTERN)?"

        const val VORNAME_PATTERN = NAME_PATTERN
    }
}
