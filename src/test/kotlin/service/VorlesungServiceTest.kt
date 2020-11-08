@file:Suppress("PackageDirectoryMismatch")

package de.hska.kunde.service

import com.mongodb.client.result.DeleteResult
import de.hska.kunde.config.security.CustomUser
import de.hska.kunde.config.security.CustomUserDetailsService
import de.hska.kunde.entity.Dozent
import de.hska.kunde.entity.Raum
import de.hska.kunde.entity.Vorlesung
import de.hska.kunde.mail.Mailer
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import java.util.UUID
import java.util.UUID.randomUUID
import javax.validation.Validation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE.JAVA_12
import org.junit.jupiter.api.condition.JRE.JAVA_13
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.aggregator.get
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.mongodb.core.ReactiveFindOperation.ReactiveFind
import org.springframework.data.mongodb.core.ReactiveFindOperation.TerminatingFind
import org.springframework.data.mongodb.core.ReactiveFluentMongoOperations
import org.springframework.data.mongodb.core.ReactiveInsertOperation.ReactiveInsert
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.ReactiveRemoveOperation.ReactiveRemove
import org.springframework.data.mongodb.core.ReactiveRemoveOperation.TerminatingRemove
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.div
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.regex
import org.springframework.data.mongodb.core.query.where
import org.springframework.data.mongodb.core.remove
import org.springframework.util.LinkedMultiValueMap
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

@Suppress("ReactorUnusedPublisher")
@Tag("service")
@DisplayName("Anwendungskern fuer Vorlesungne testen")
@ExtendWith(MockKExtension::class, SoftAssertionsExtension::class)
@EnabledOnJre(JAVA_12, JAVA_13)
@ExperimentalCoroutinesApi
class VorlesungServiceTest {
    private var mongo: ReactiveFluentMongoOperations = mockk()
    private var mongoTemplate: ReactiveMongoTemplate = mockk()
    private var userDetailsService: CustomUserDetailsService = mockk()
    private val validatorFactory = Validation.buildDefaultValidatorFactory()
    private val mailer: Mailer = mockk()
    private val service = VorlesungService(mongo, userDetailsService, validatorFactory, mailer)

    private var reactiveFind: ReactiveFind<Vorlesung> = mockk()
    private var terminatingFind: TerminatingFind<Vorlesung> = mockk()
    private var reactiveInsert: ReactiveInsert<Vorlesung> = mockk()
    private var reactiveRemove: ReactiveRemove<Vorlesung> = mockk()
    private var terminatingRemove: TerminatingRemove<Vorlesung> = mockk()

    @BeforeEach
    fun beforeEach() {
        clearMocks(
            mongo,
            mongoTemplate,
            userDetailsService,
            mailer,
            reactiveFind,
            terminatingFind,
            reactiveInsert,
            reactiveRemove
        )
    }

    @Test
    @Order(100)
    fun `Immer erfolgreich`() {
        @Suppress("UserPropertyAccessSyntax")
        assertThat(true).isTrue()
    }

    @Test
    @Order(200)
    @Disabled
    fun `Noch nicht fertig`() {
        @Suppress("UsePropertyAccessSyntax")
        assertThat(false).isFalse()
    }

    // --------------------------------------------------------------------
    // L E S E N
    // --------------------------------------------------------------------
    @Nested
    inner class Lesen {
        @Suppress("ClassName")
        @Nested
        inner class `Suche anhand der ID` {
            @ParameterizedTest
            @CsvSource("$ID_VORHANDEN, $NAME")
            @Order(1000)
            fun `Suche mit vorhandener ID`(idStr: String, name: String) = runBlockingTest {
                // arange
                every { mongo.query<Vorlesung>() } returns reactiveFind
                val id = UUID.fromString(idStr)
                val query = Query(where(Vorlesung::id).isEqualTo(id))
                every { reactiveFind.matching(query) } returns terminatingFind
                val vorlesungMock = createVorlesungMock(id, name)
                every { terminatingFind.one() } returns vorlesungMock.toMono()

                // act
                val vorlesung = service.findById(id)

                // assert
                assertThat(vorlesung?.id).isEqualTo(id)
                // assertThat(true).isTrue()
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_NICHT_VORHANDEN])
            @Order(1100)
            fun `Suche mit nicht vorhandener ID`(idStr: String) = runBlockingTest {
                // arrange
                every { mongo.query<Vorlesung>() } returns reactiveFind
                val id = UUID.fromString(idStr)
                val query = Query(where(Vorlesung::id).isEqualTo(id))
                every { reactiveFind.matching(query) } returns terminatingFind
                every { terminatingFind.one() } returns Mono.empty()

                // act
                val result = service.findById(id, USERNAME)

                // assert
                assertThat(result).isNull()
            }
        }

        @ParameterizedTest
        @ValueSource(strings = [NAME])
        @Order(2000)
        fun `Suche alle Vorlesungen`(name: String) = runBlockingTest {
            // arange
            every { mongo.query<Vorlesung>() } returns reactiveFind
            val vorlesungMock = createVorlesungMock(name)
            every { reactiveFind.all() } returns listOf(vorlesungMock).toFlux()
            val emptyQueryParams = LinkedMultiValueMap<String, String>()

            // act
            val vorlesungen = service.find(emptyQueryParams)

            // assert
            val vorlesungenList = mutableListOf<Vorlesung>()
            vorlesungen.toList(vorlesungenList)
            assertThat(vorlesungenList).isNotEmpty
        }

        @ParameterizedTest
        @ValueSource(strings = [NAME])
        @Order(2100)
        fun `Suche mit vorhandenem Namen`(name: String) = runBlockingTest {
            // arange
            every { mongo.query<Vorlesung>() } returns reactiveFind
            val query = Query(Vorlesung::name.regex(name, "i"))
            every { reactiveFind.matching(query) } returns terminatingFind
            val vorlesungMock = createVorlesungMock(name)
            every { terminatingFind.all() } returns listOf(vorlesungMock).toFlux()
            val queryParams = LinkedMultiValueMap(mapOf("name" to listOf(name)))

            // act
            val vorlesungen = service.find(queryParams)

            // asert
            val vorlesungenList = mutableListOf<Vorlesung>()
            vorlesungen.toList(vorlesungenList)
            assertThat(vorlesungenList)
                .isNotEmpty
                .allMatch { vorlesung -> vorlesung.name == name }
        }

        @ParameterizedTest
        @ValueSource(strings = [NAME])
        @Order(2200)
        fun `Suche mit nicht-vorhandenem Name`(name: String) = runBlockingTest {
            // arange
            every { mongo.query<Vorlesung>() } returns reactiveFind
            val query = Query(Vorlesung::name.regex(name, "i"))
            every { reactiveFind.matching(query) } returns terminatingFind
            every { terminatingFind.all() } returns Flux.empty()
            val queryParams = LinkedMultiValueMap(mapOf("name" to listOf(name)))

            // act
            val vorlesungen = service.find(queryParams)

            // assert
            val vorlesungenList = mutableListOf<Vorlesung>()
            vorlesungen.toList(vorlesungenList)
            assertThat(vorlesungenList).isEmpty()
        }

        @ParameterizedTest
        @CsvSource("$ID_VORHANDEN, $NAME, $NACHNAME")
        @Order(2400)
        fun `Suche mit vorhandenem Dozent-Nachnamen`(idStr: String, name: String, nachname: String) =
            runBlockingTest {
            // arange
                every { mongo.query<Vorlesung>() } returns reactiveFind
                val query = Query((Vorlesung::dozent / Dozent::nachname).regex("^$nachname"))
                every { reactiveFind.matching(query) } returns terminatingFind
                val id = UUID.fromString(idStr)
                val vorlesungMock = createVorlesungMock(id, name, nachname)
                every { terminatingFind.all() } returns listOf(vorlesungMock).toFlux()
                val queryParams = LinkedMultiValueMap<String, String>(mapOf("nachname" to listOf(nachname)))

                // act
                val vorlesungen = service.find(queryParams)

                // assert
                val vorlesungenList = mutableListOf<Vorlesung>()
                vorlesungen.toList(vorlesungenList)
                val nachnamenList = vorlesungenList.map { it.dozent.nachname }
                assertThat(nachnamenList)
                    .allMatch { n -> n == nachname }
        }

        @Disabled
        @ParameterizedTest
        @CsvSource("$ID_VORHANDEN, $NAME, $NACHNAME")
        @Order(2500)
        fun `Suche mit vorhandenem Namen und Dozent-Nachname`(idStr: String, name: String, nachname: String) =
            runBlockingTest {
                // arange
                every { mongo.query<Vorlesung>() } returns reactiveFind
                val query = Query(Vorlesung::name.regex(name, "i"))
                query.addCriteria(Vorlesung::dozent / Dozent::nachname regex "^$nachname")
                every { reactiveFind.matching(query) } returns terminatingFind
                val id = UUID.fromString(idStr)
                val vorlesungMock = createVorlesungMock(id, name, nachname)
                every { terminatingFind.all() } returns listOf(vorlesungMock).toFlux()
                val queryParams = LinkedMultiValueMap(mapOf("name" to listOf(name)))

                // act
                val vorlesungen = service.find(queryParams)

                // assert
                val vorlesungenList = mutableListOf<Vorlesung>()
                vorlesungen.toList(vorlesungenList)
                assertThat(vorlesungenList)
                    .isNotEmpty
                    .allMatch { vorlesung ->
                        vorlesung.name.toLowerCase() == name.toLowerCase() &&
                            vorlesung.dozent.nachname == nachname
                    }
            }
    }

    // ---------------------------------------------------------------------
    // SCHREIBEN
    // ---------------------------------------------------------------------
    @Nested
    inner class Schreiben {
        @Nested
        inner class Erzeugen {
            @ParameterizedTest
            @CsvSource("$NAME, $NACHNAME, $USERNAME, $PASSWORD")
            @Order(5000)
            fun `Neue Vorelsung abspeichern`(args: ArgumentsAccessor, softly: SoftAssertions) = runBlockingTest {
                // arrange
                val name = args.get<String>(0)
                val nachname = args.get<String>(1)
                val username = args.get<String>(2)
                val password = args.get<String>(3)

                every { mongo.query<Vorlesung>() } returns reactiveFind
                every { reactiveFind.matching(Query(Vorlesung::name isEqualTo name)) } returns terminatingFind
                every { terminatingFind.exists() } returns false.toMono()

                val userMock = CustomUser(id = null, username = username, password = password)
                val userMockCreated = CustomUser(id = randomUUID(), username = username, password = password)
                every { runBlocking { userDetailsService.create(userMock) } } returns userMockCreated

                every { mongo.insert<Vorlesung>() } returns reactiveInsert
                val vorlesungMock = createVorlesungMock(
                    id = null,
                    name = name,
                    nachname = nachname,
                    username = username,
                    password = password)
                val vorlesungResultMock = vorlesungMock.copy(id = randomUUID())
                every { reactiveInsert.one(vorlesungMock) } returns vorlesungResultMock.toMono()

                every { mailer.send(vorlesungMock) } just Runs

                // act
                val vorlesung = service.create(vorlesungMock)

                // assert
                with(softly) {
                    assertThat(vorlesung.id).isNotNull()
                    assertThat(vorlesung.name).isEqualTo(name)
                    assertThat(vorlesung.dozent.nachname).isEqualTo(nachname)
                    assertThat(vorlesung.username).isEqualTo(username)
                }
            }

            @ParameterizedTest
            @CsvSource("$NAME, $NACHNAME")
            @Order(5100)
            fun `Neue Vorlesung ohne Benutzerdaten`(name: String, nachname: String) = runBlockingTest {
                // arange
                val vorlesungMock = createVorlesungMock(id = null, name = name, nachname = nachname)

                // act
                val thrown = catchThrowableOfType(
                    { runBlockingTest { service.create(vorlesungMock) } },
                    InvalidAccountException::class.java
                )

                // assert
                assertThat(thrown.cause).isNull()
            }

            @ParameterizedTest
            @CsvSource("$NAME, $NACHNAME, $USERNAME, $PASSWORD")
            @Order(5200)
            fun `Neue Vorlesung mit existierendem Namen`(args: ArgumentsAccessor) = runBlockingTest {
                // arange
                val name = args.get<String>(0)
                val nachname = args.get<String>(1)
                val username = args.get<String>(2)
                val password = args.get<String>(3)

                every { mongo.query<Vorlesung>() } returns reactiveFind
                every { reactiveFind.matching(Query(Vorlesung::name isEqualTo name)) } returns terminatingFind
                every { terminatingFind.exists() } returns true.toMono()
                val vorlesungMock = createVorlesungMock(null, name, nachname, username, password)

                // act
                val thrown = catchThrowableOfType(
                    { runBlockingTest { service.create(vorlesungMock) } },
                    NameExistsException::class.java
                )

                // assert
                assertThat(thrown.cause).isNull()
            }
        }

        @Nested
        inner class Aendern {
            @ParameterizedTest
            @CsvSource("$ID_UPDATE, $NAME, $NACHNAME")
            @Order(6000)
            @Disabled("Mocking des Cache in Spring Data MongoDb...")
            fun `Vorhandene Vorlesung aktualisieren`(
                idStr: String,
                name: String,
                nachname: String
            ) = runBlockingTest {
                // arrange
                every { mongo.query<Vorlesung>() } returns reactiveFind
                val id = UUID.fromString(idStr)
                every { reactiveFind.matching(Query(where(Vorlesung::id).isEqualTo(id))) } returns terminatingFind
                val vorlesungMock = createVorlesungMock(id, name, nachname)
                every { terminatingFind.one() } returns vorlesungMock.toMono()
                every { mongoTemplate.save(vorlesungMock) } returns vorlesungMock.toMono()

                // act
                val vorlesung = service.update(vorlesungMock, id, vorlesungMock.version.toString())

                // assert
                assertThat(vorlesung?.id).isEqualTo(id)
            }

            @ParameterizedTest
            @CsvSource("$ID_NICHT_VORHANDEN, $NAME, $NACHNAME, $VERSION")
            @Order(6100)
            fun `Nicht-existierende Vorlesung aktualisieren`(args: ArgumentsAccessor) = runBlockingTest {
                // arange
                val idStr = args.get<String>(0)
                val id = UUID.fromString(idStr)
                val name = args.get<String>(1)
                val nachname = args.get<String>(2)
                val version = args.get<String>(3)

                every { mongo.query<Vorlesung>() } returns reactiveFind
                every { reactiveFind.matching(Query(where(Vorlesung::id).isEqualTo(id))) } returns terminatingFind
                every { terminatingFind.one() } returns Mono.empty()

                val vorlesungMock = createVorlesungMock(id, name, nachname)

                // act
                val vorlesung = service.update(vorlesungMock, id, version)

                // assert
                assertThat(vorlesung).isNull()
            }

            @ParameterizedTest
            @CsvSource("$ID_UPDATE, $NAME, $NACHNAME, $VERSION_INVALID")
            @Order(6200)
            fun `Vorlesung aktualisieren mit falscher Versionsnummer`(args: ArgumentsAccessor) = runBlockingTest {
                // arange
                val idStr = args.get<String>(0)
                val id = UUID.fromString(idStr)
                val name = args.get<String>(1)
                val nachname = args.get<String>(2)
                val version = args.get<String>(3)

                every { mongo.query<Vorlesung>() } returns reactiveFind
                every { reactiveFind.matching(Query(where(Vorlesung::id).isEqualTo(id))) } returns terminatingFind
                val vorlesungMock = createVorlesungMock(id, name, nachname)
                every { terminatingFind.one() } returns vorlesungMock.toMono()

                // act
                val thrown = catchThrowableOfType(
                    { runBlockingTest { service.update(vorlesungMock, id, version) } },
                    InvalidVersionException::class.java
                )

                // assert
                assertThat(thrown.message).isEqualTo("Falsche Versionsnummer: $version")
            }

            @Disabled
            @ParameterizedTest
            @CsvSource("$ID_UPDATE, $NAME, $NACHNAME, $VERSION_ALT")
            @Order(6300)
            fun `Vorlesung aktualisieren mit alter Versionsnummer`(args: ArgumentsAccessor) = runBlockingTest {
                val idStr = args.get<String>(0)
                val id = UUID.fromString(idStr)
                val name = args.get<String>(1)
                val nachname = args.get<String>(2)
                val version = args.get<String>(3)

                every { mongo.query<Vorlesung>() } returns reactiveFind
                every { reactiveFind.matching(Query(where(Vorlesung::id).isEqualTo(id))) } returns terminatingFind
                val vorlesungMock = createVorlesungMock(id, name, nachname)

                // act
                val thrown = catchThrowableOfType(
                    { runBlockingTest { service.update(vorlesungMock, id, version) } },
                    InvalidVersionException::class.java
                )

                // assert
                assertThat(thrown.cause).isNull()
            }
        }

        @Nested
        inner class Loeschen {
            @ParameterizedTest
            @ValueSource(strings = [ID_LOESCHEN])
            @Order(7000)
            fun `Vorhandene Vorlesung loeschen()`(idStr: String) = runBlockingTest {
                // arange
                every { mongo.remove<Vorlesung>() } returns reactiveRemove
                val id = UUID.fromString(idStr)
                every { reactiveRemove.matching(Query(where(Vorlesung::id).isEqualTo(id))) } returns terminatingRemove
                // DeleteResult ist eien absrakte Klasse
                val deleteResultMock = object : DeleteResult() {
                    override fun wasAcknowledged() = true
                    override fun getDeletedCount() = 1L
                }
                every { terminatingRemove.all() } returns deleteResultMock.toMono()

                // act
                val deleteResult = service.deleteById(id)

                // assert
                assertThat(deleteResult.deletedCount).isOne()
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_LOESCHEN_NICHT_VORHANDEN])
            @Order(7100)
            fun `Nicht-vorhandene Vorlesung loeschen`(idStr: String) = runBlockingTest {
                // arange
                every { mongo.remove<Vorlesung>() } returns reactiveRemove
                val id = UUID.fromString(idStr)
                every { reactiveRemove.matching(Query(where(Vorlesung::id).isEqualTo(id))) } returns terminatingRemove
                // DeleteResult ist eien absrakte Klasse
                val deleteResultMock = object : DeleteResult() {
                    override fun wasAcknowledged() = true
                    override fun getDeletedCount() = 0L
                }
                every { terminatingRemove.all() } returns deleteResultMock.toMono()

                // act
                val deleteResult = service.deleteById(id)

                // assert
                assertThat(deleteResult.deletedCount).isZero()
            }
        }
    }

    // ------------------------------------------------------------------
    // Hilfsmethoden fuer Mocking
    // ------------------------------------------------------------------
    private fun createVorlesungMock(name: String): Vorlesung = createVorlesungMock(randomUUID(), name)

    private fun createVorlesungMock(id: UUID, name: String): Vorlesung = createVorlesungMock(id, name, NACHNAME)

    private fun createVorlesungMock(id: UUID?, name: String, nachname: String) =
        createVorlesungMock(id, name, nachname, null, null)

    @Suppress("LongParameterList", "SameParameterValue")
    private fun createVorlesungMock(
        id: UUID?,
        name: String,
        nachname: String,
        username: String?,
        password: String?
    ): Vorlesung {
        val dozent = Dozent(vorname = VORNAME, nachname = nachname)
        val raum = Raum(GEBAEUDE, RAUMNUMMER)
        val vorlesung = Vorlesung(
            id = id,
            version = 0,
            name = name,
            dozent = dozent,
            raum = raum,
            username = username
        )
        if (username != null && password != null) {
            val customUser = CustomUser(id = null, username = username, password = password)
            vorlesung.user = customUser
        }
        return vorlesung
    }

    private companion object {
        const val ID_VORHANDEN = "00000000-0000-0000-0000-000000000000"
        const val ID_NICHT_VORHANDEN = "99999999-9999-9999-9999-999999999999"
        const val ID_UPDATE = "00000000-0000-0000-0000-000000000002"
        const val ID_LOESCHEN = "00000000-0000-0000-0000-000000000005"
        const val ID_LOESCHEN_NICHT_VORHANDEN = "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"
        const val NAME = "Rechnungswesen"
        const val VORNAME = "Michael"
        const val NACHNAME = "Reichardt"
        const val GEBAEUDE = "M"
        const val RAUMNUMMER = "310"
        const val USERNAME = "admin"
        const val PASSWORD = "p"
        const val VERSION = "0"
        const val VERSION_INVALID = "!?"
        const val VERSION_ALT = "-1"
    }
}
