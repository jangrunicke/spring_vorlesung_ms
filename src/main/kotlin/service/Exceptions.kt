package de.hska.kunde.service

/**
 * Basisklasse für die Exceptions, da Multi-Catch nicht verfügbar ist
 *
 * @author [Jan Grunicke]
 *
 * @constructor Fehlermeldung.
 */
abstract class VorlesungServiceException(msg: String) : RuntimeException(msg)

/**
 * Exception, falls es bereits eine Vorlesung mit dem jeweiligen Namen gibt.
 *
 * @author [Jan Grunicke]
 *
 * @constructor Fehlermeldung mit dem Namen
 */

class NameExistsException(name: String) : VorlesungServiceException("Der Name $name existier bereits")

/**
 * Exception, falls die Zugriffsrechte nicht ausreichen.
 *
 * @author [Jan Grunicke]
 *
 * @constructor Unzureichende Rollen.
 */
class AccessForbiddenException(roles: Collection<String>) : VorlesungServiceException("Unzureichende Rollen: $roles")

/**
 * Excepton, falls de Zugriffsrechte nicht ausreichen.
 *
 * @author [Jan Grunicke]
 */
class InvalidAccountException : VorlesungServiceException("Ungueltiger Account")
/**
 * Exception, falls die Versionsnummer bei z.B. PUT oder PATCH ungültig ist.
 *
 * @author [Jan Grunicke]
 *
 * @constructor Fehlermeldung mit der falschen Versionsnummer.
 */
class InvalidVersionException(version: String) : VorlesungServiceException("Falsche Versionsnummer: $version")
