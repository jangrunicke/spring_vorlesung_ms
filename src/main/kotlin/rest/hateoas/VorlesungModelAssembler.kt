package de.hska.kunde.rest.hateoas

import de.hska.kunde.entity.Vorlesung
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.SimpleRepresentationModelAssembler
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * Mit der Klasse [VorlesungModelAssembler] können Entity-Objekte der Klasse [de.hska.entity.Vorlesung].
 * in eine HATEOAS-Repräsentation transformiert werden.
 *
 * @author Jan Grunicke
 *
 * @constructor Ein VorlesungModelAssembler erzeugen.
 */
@Component
class VorlesungModelAssembler : SimpleRepresentationModelAssembler<Vorlesung> {
    lateinit var request: ServerRequest

    /**
     * EntityModel eines Vorlesung-Objektes (gemäß Spring HATEOAS) um Atom-Links ergänzen.
     * @param vorlesungModel Gefundenes Vorlesung-Objekt als EntityModel gemäß Sprin HATEOAS
     * @return Model für die Vorlesung mit Atom-Links für HATEOAS.
     */
    override fun addLinks(vorlesungModel: EntityModel<Vorlesung>) {
        val uri = request.uri().toString()
        val id = vorlesungModel.content?.id

        val baseUri = uri.substringBefore('?')
            .removeSuffix("/")
            .removeSuffix("/$id")
        val idUri = "$baseUri/$id"

        val selfLink = Link(idUri)
        val listLink = Link(baseUri, "list")
        val addLink = Link(idUri, "add")
        val updateLink = Link(idUri, "update")
        val removeLink = Link(idUri, "remove")
        vorlesungModel.add(selfLink, listLink, addLink, updateLink, removeLink)
    }

    fun toModel(vorlesung: Vorlesung, request: ServerRequest): EntityModel<Vorlesung> {
        this.request = request
        return toModel(vorlesung)
    }

    override fun addLinks(resources: CollectionModel<EntityModel<Vorlesung>>) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
