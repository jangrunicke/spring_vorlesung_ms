package de.hska.kunde.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import de.hska.kunde.config.security.CustomUser
import java.time.LocalDateTime
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Pattern
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version

/**
 * Unveränderliche Daten einer Vorlesung. In DDD ist Vorlesung ein _Aggregate Root.
 *
 * @author [Jan Grunicke]
 *
 * @property id ID einer Vorlesung als UUID [ID_PATTERN].
 * @property version Versionsnummer in der DB.
 * @property name Name der Vorlesung.
 * @property dozent Dozent der Vorlesung.
 * @property raum Raum in dem eine Vorlesung stattfindet.
 * @property username Der Username bzw. Loginname einer Vorlesung
 * @property user Das Objekt mit allen User-Daten (wird nicht in der DB gespiechert.)
 */

data class Vorlesung(
    @JsonIgnore
    val id: UUID?,

    @Version
    @JsonIgnore
    val version: Int? = null,

    @get:NotEmpty(message = "{vorlesung.name.notEmpty}")
    @get:Pattern(
        regexp = "[\\w\\s]+",
        message = "{vorlesung.name.pattern}"
    )
    val name: String,

    @get:Valid
    val dozent: Dozent,

    val raum: Raum,

    val username: String? = null,

    @CreatedDate
    @JsonIgnore
    private val erzeugt: LocalDateTime? = null,

    @LastModifiedDate
    @JsonIgnore
    private val aktualiserit: LocalDateTime? = null
) {
    @Transient
    var user: CustomUser? = null

    /**
     * Vergleich mit einem anderen Objekt oder null.
     * @param other Das zu vergleichende Objekt oder null.
     * @return True, falls das zu vergleichende Vorlesungs-Objekt den gleichen Namen hat.
     */
    @Suppress("ReturnCount")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vorlesung
        return name == other.name
    }

    /**
     * Hashwert aufgrund des Namens
     * @return Der Hashwert.
     */
    override fun hashCode() = name.hashCode()

    /**
     * Ein Vorlesungs-Objekt als String, z.B. für Logging.
     * @return String mit den Properties.
     */
    override fun toString() = "Vorlesung(id=$id, name=$name, dozent=$dozent, raum = $raum)"

    companion object {
        private const val HEX_PATTERN = "[\\dA-Fa-f]"
        /**
         * Muster für eine UUID.
         */
        const val ID_PATTERN = "$HEX_PATTERN{8}-$HEX_PATTERN{4}-$HEX_PATTERN{4}-$HEX_PATTERN{4}-$HEX_PATTERN{12}"

        const val VORLESUNGNAME_PATTERN = "[\\w\\s]+"
    }
}
