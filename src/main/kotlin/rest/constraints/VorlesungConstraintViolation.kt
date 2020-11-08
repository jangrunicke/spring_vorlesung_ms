package de.hska.kunde.rest.constraints

/**
 * Datensatz für eine Verletzung gemäß Hibernate Validator.
 *
 * @author Jan Grunicke
 *
 * @property property Name der Vorlesung-Property bei der es eine Verletztung gibt.
 * @property message Die zugehörige Fehlermeldung.
 */
data class VorlesungConstraintViolation(val property: String, val message: String?)
