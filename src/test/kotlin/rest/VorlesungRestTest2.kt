@file:Suppress("PackageDirectoryMissmatch")

package de.hska.kunde.rest

import com.jayway.jsonpath.JsonPath
import de.hska.kunde.config.security.CustomUser
import de.hska.kunde.entity.Dozent
import de.hska.kunde.entity.Raum
import de.hska.kunde.entity.Vorlesung
import de.hska.kunde.entity.Vorlesung.Companion.ID_PATTERN
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.aggregator.get
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.MediaTypes.HAL_JSON
import org.springframework.hateoas.mediatype.hal.HalLinkDiscoverer
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.body
import org.springframework.web.reactive.function.client.bodyToFlow
import reactor.kotlin.core.publisher.toMono

@Tag("rest")
@ExtendWith(SpringExtension::class, SoftAssertionsExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("dev")
@TestPropertySource(locations = ["/rest-test.properties"])
@EnabledOnJre(JRE.JAVA_12, JRE.JAVA_13)
@DisplayName("REST-Schnittstelle für Vorlesung testen")
class VorlesungRestTest2(@LocalServerPort private val port: Int, ctx: ReactiveWebApplicationContext) {
    private val baseUrl = "http://localhost:$port"
    private val client = WebClient.builder()
        .filter(basicAuthentication(USERNAME, PASSWORD))
        .baseUrl(baseUrl)
        .build()

    init {
        assertThat(ctx.getBean<VorlesungHandler>()).isNotNull
    }

    @Test
    @Order(100)
    fun `immer erfolgreich`() {
        assertThat(true).isTrue()
    }

    // -------------------------------------------------------------
    // L E S E N
    // --------------------------------------------------------------
    @Nested
    inner class Lesen {
        @Nested
        inner class `Suche anhand der ID` {
            @ParameterizedTest
            @ValueSource(strings = [ID_VORHANDEN, ID_UPDATE])
            @Order(1000)
            fun `Suche mit vorhandener ID`(id: String, softly: SoftAssertions) = runBlocking<Unit> {
                // act
                val response = client.get()
                    .uri(ID_PATH, id)
                    .accept(HAL_JSON)
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(OK)
                val content = response.awaitBody<String>()

                with(softly) {
                    val name: String = JsonPath.read(content, "$.name")
                    assertThat(name).isNotBlank
                    val linkDiscoverer = HalLinkDiscoverer()
                    val selfLink = linkDiscoverer.findLinkWithRel("self", content).get().href
                    assertThat(selfLink).endsWith("/$id")
                }
            }
        }
        @Test
        @Order(2000)
        fun `Suche nach allen Vorlesungen`() = runBlocking<Unit> {
            // act
            val vorlesungen = client.get()
                .retrieve()
                .bodyToFlow<EntityModel<Vorlesung>>()

            // assert
            val vorlesungenList = mutableListOf<EntityModel<Vorlesung>>()
            vorlesungen.toList(vorlesungenList)
            assertThat(vorlesungenList).isNotEmpty
        }
    }

    // ---------------------------------------------------------
    // S C H R E I B E N
    // ---------------------------------------------------------
    @Nested
    inner class Schreiben {
        @Nested
        inner class Erzeugen {
            @ParameterizedTest
            @CsvSource(
                "$NEUER_NAME, $NEUER_DOZENTNACHNAME, $NEUER_DOZENTVORNAME, $NEUES_GEBAEUDE, $NEUE_RAUMNUMMER, " +
                    "$NEUER_USERNAME"
            )
            @Order(5000)
            fun `Abspeichern einer neuen Vorlesung`(args: ArgumentsAccessor, softly: SoftAssertions) =
                runBlocking<Unit> {
                // arange
                val neueVorlesung = Vorlesung(
                    id = null,
                    name = args.get<String>(0),
                    dozent = Dozent(nachname = args.get<String>(1), vorname = args.get<String>(2)),
                    raum = Raum(args.get<String>(3), args.get<String>(4))
                )
                neueVorlesung.user = CustomUser(
                    id = null,
                    username = args.get<String>(5),
                    password = "p",
                    authorities = emptyList()
                )

                // act
                val response = client.post()
                    .singleBody(neueVorlesung)
                    .awaitExchange()

                // assert
                val id: String
                with(response) {
                    with(softly) {
                        assertThat(statusCode()).isEqualTo(CREATED)

                        assertThat(headers()).isNotNull
                        val location = headers().asHttpHeaders().location
                        assertThat(location).isNotNull
                        val locationStr = location.toString()
                        assertThat(locationStr).isNotBlank
                        val indexLastSlash = locationStr.lastIndexOf('/')
                        assertThat(indexLastSlash).isPositive
                        id = locationStr.substring(indexLastSlash + 1)
                        assertThat(id).matches((ID_PATTERN))
                    }
                }
            }
        }
    }

    private companion object {
        const val ID_PATH = "/{id}"

        const val USERNAME = "admin"
        const val PASSWORD = "p"

        const val ID_VORHANDEN = "00000000-0000-0000-0000-000000000001"
        const val ID_UPDATE = "00000000-0000-0000-0000-000000000002"

        const val NEUER_NAME = "Test"
        const val NEUE_RAUMNUMMER = "300"
        const val NEUER_DOZENTNACHNAME = "Tester"
        const val NEUER_DOZENTVORNAME = "Theo"
        const val NEUES_GEBAEUDE = "M"
        const val NEUER_USERNAME = "test"
    }
}

inline fun <reified T : Any> WebClient.RequestBodySpec.singleBody(obj: T): WebClient.RequestHeadersSpec<*> =
    body(obj.toMono())
