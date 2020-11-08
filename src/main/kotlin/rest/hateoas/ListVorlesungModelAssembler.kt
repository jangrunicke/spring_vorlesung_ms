package de.hska.kunde.rest.hateoas

import de.hska.kunde.entity.Vorlesung
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.SimpleRepresentationModelAssembler
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * Mt der Klasse [ListVorlesungModelAssembler] können Entity-Objekte der Klasse [de.hska.kunde.entity.Vorlesung]
 * in eine HATEOAS-Repräsentation innerhalb einer Liste bzw. eines JSON-Arrays transformiert werden.
 *
 * @author Jan Grunicke
 */
@Component
class ListVorlesungModelAssembler : SimpleRepresentationModelAssembler<Vorlesung> {
    /**
     * Konvertierung eines (gefunden) Vorlesung-Objektes in ein Model gemäß Spring-HATEOAS.
     * @param vorlesung Gefundenes Vorlesung-Objekt
     * @param request Der eingegangene Request mit insbesondere der aufgerufenen URI
     * @return Modle für eine Vorlesun mit Atom-Links für HATEOAS
     */
    fun toModel(vorlesung: Vorlesung, request: ServerRequest): EntityModel<Vorlesung> {
        val uri = request.uri().toString()
        val baseUri = uri.substringBefore('?')
            .removeSuffix("/")
        val idUri = "$baseUri/${vorlesung.id}"

        val selfLink = Link(idUri)
        return toModel(vorlesung).add(selfLink)
    }

    /**
     * Konvertierung eines (gefundenen) Vorlesung-Objektes in ein Modle gemäß Spring HATEOAS.
     * @param model Gefundenes Vorlesung-Objekt als EntityModel gemäß Spring HATEOAS.
     */
    override fun addLinks(model: EntityModel<Vorlesung>) = Unit

    override fun addLinks(resources: CollectionModel<EntityModel<Vorlesung>>) = Unit
}
