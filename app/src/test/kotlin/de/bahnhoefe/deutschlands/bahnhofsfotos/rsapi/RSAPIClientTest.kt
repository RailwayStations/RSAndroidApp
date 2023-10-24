package de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScoreItem
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse.InboxResponseState
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Photo
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoLicense
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStation
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStations
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Photographer
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Token
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URL
import java.net.URLConnection
import java.nio.charset.Charset
import java.util.Objects

internal class RSAPIClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: RSAPIClient

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/")
        client = RSAPIClient(
            baseUrl.toString(),
            "clientId",
            null,
            "rsapiRedirectScheme://rsapiRedirectHost"
        )
    }

    @AfterEach
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun countries() {
        server.enqueue(MockResponse().setBody(fromFile("countries.json")))
        val response = client.getCountries().execute()
        assertThat(server.takeRequest().path).isEqualTo("/countries")
        assertThat(response.body()).isNotNull
        assertThat(response.body()).containsExactly(
            Country("in", "India", null, "https://enquiry.indianrail.gov.in/ntes/"),
            Country(
                "ch", "Schweiz", "fotos@schweizer-bahnhoefe.ch",
                "http://fahrplan.sbb.ch/bin/stboard.exe/dn?input={title}&REQTrain_name=&boardType=dep&time=now&maxJourneys=20&selectDate=today&productsFilter=1111111111&start=yes",
                null,
                listOf(
                    ProviderApp(
                        "android",
                        "SBB Mobile",
                        "https://play.google.com/store/apps/details?id=ch.sbb.mobile.android.b2c"
                    ),
                    ProviderApp(
                        "ios",
                        "SBB Mobile",
                        "https://apps.apple.com/app/sbb-mobile/id294855237"
                    )
                )
            )
        )
    }

    @Test
    fun highScore() {
        server.enqueue(MockResponse().setBody(fromFile("highscore.json")))
        val response = client.getHighScore().execute()
        assertThat(server.takeRequest().path).isEqualTo("/photographers")
        assertThat(response.body()).isNotNull
        assertThat(response.body()!!.getItems()).containsExactly(
            HighScoreItem(
                "User1",
                2581,
                1
            ),
            HighScoreItem(
                "User2",
                1109,
                2
            ),
            HighScoreItem(
                "User3",
                812,
                3
            )
        )
    }

    @Test
    fun highScoreWithCountry() {
        server.enqueue(MockResponse().setBody(fromFile("highscore.json")))
        val response = client.getHighScore("de").execute()
        assertThat(server.takeRequest().path)
            .isEqualTo("/photographers?country=de")
        assertThat(response.body()).isNotNull
        assertThat(response.body()!!.getItems()).containsExactly(
            HighScoreItem(
                "User1",
                2581,
                1
            ),
            HighScoreItem(
                "User2",
                1109,
                2
            ),
            HighScoreItem(
                "User3",
                812,
                3
            )
        )
    }

    @Test
    fun statsWithCountry() {
        server.enqueue(MockResponse().setBody(fromFile("stats.json")))
        val response = client.getStatistic("de").execute()
        assertThat(server.takeRequest().path).isEqualTo("/stats?country=de")
        assertThat(response.body()).isNotNull
        assertThat(response.body()).usingRecursiveComparison()
            .isEqualTo(Statistic(7863, 7861, 2, 249))
    }

    @Test
    fun photoStationById() {
        server.enqueue(MockResponse().setBody(fromFile("photoStationById.json")))
        val response = client.getPhotoStationById("de", "1973").execute()
        assertThat(server.takeRequest().path)
            .isEqualTo("/photoStationById/de/1973")
        assertThat(response.body()).isNotNull
        assertThat(response.body()).isEqualTo(
            PhotoStations(
                "https://api.railway-stations.org/photos",
                listOf(
                    PhotoLicense(
                        "CC0_10",
                        "CC0 1.0 Universell (CC0 1.0)",
                        URL("https://creativecommons.org/publicdomain/zero/1.0/")
                    )
                ),
                listOf(
                    Photographer(
                        "@User1",
                        URL("https://example.com/@User1")
                    )
                ),
                listOf(
                    PhotoStation(
                        "de",
                        "1973",
                        "Fulda",
                        50.5547372607544,
                        9.6843855869764,
                        "FFU",
                        false,
                        listOf(
                            Photo(
                                4430L,
                                "@User1",
                                "/de/1973.jpg",
                                1451846273000L,
                                "CC0_10"
                            ),
                            Photo(
                                4431L,
                                "@User1",
                                "/de/1973_2.jpg",
                                1451846279000L,
                                "CC0_10"
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun photoStationsByCountry() {
        server.enqueue(MockResponse().setBody(fromFile("photoStationsByCountry.json")))
        val response = client.getPhotoStationsByCountry("de").execute()
        assertThat(server.takeRequest().path)
            .isEqualTo("/photoStationsByCountry/de")
        assertThat(response.body()).isNotNull
        assertThat(response.body()).isEqualTo(
            PhotoStations(
                "https://api.railway-stations.org/photos",
                listOf(
                    PhotoLicense(
                        "CC0_10",
                        "CC0 1.0 Universell (CC0 1.0)",
                        URL("https://creativecommons.org/publicdomain/zero/1.0/")
                    )
                ),
                listOf(
                    Photographer(
                        "@User1",
                        URL("https://example.com/@User1")
                    )
                ),
                listOf(
                    PhotoStation(
                        "de",
                        "Z11",
                        "Bahnhof Zoo",
                        51.03745553026876,
                        13.757279123256026,
                        "",
                        false, listOf()
                    ),
                    PhotoStation(
                        "de",
                        "1973",
                        "Fulda",
                        50.5547372607544,
                        9.6843855869764,
                        "FFU",
                        false,
                        listOf(
                            Photo(
                                4430L,
                                "@User1",
                                "/de/1973.jpg",
                                1451846273000L,
                                "CC0_10"
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun postPhotoUpload() {
        server.enqueue(MockResponse().setBody(fromFile("photoUpload.json")))
        val requestBody: RequestBody = "IMAGE".toByteArray().toRequestBody(
            URLConnection.guessContentTypeFromName("photo.png").toMediaTypeOrNull(),
        )
        client.setToken(Token("accessToken", "tokenType", "refeshToken"))
        val response =
            client.photoUpload("4711", "de", "Title", 50.0, 9.0, "Comment", null, requestBody)
                .execute()
        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/photoUpload")
        assertThat(recordedRequest.getHeader("Station-Id")).isEqualTo("4711")
        assertThat(recordedRequest.getHeader("Country")).isEqualTo("de")
        assertThat(recordedRequest.getHeader("Station-Title")).isEqualTo("Title")
        assertThat(recordedRequest.getHeader("Latitude")).isEqualTo("50.0")
        assertThat(recordedRequest.getHeader("Longitude")).isEqualTo("9.0")
        assertThat(recordedRequest.getHeader("Comment")).isEqualTo("Comment")
        assertThat(recordedRequest.getHeader("Active")).isNull()
        assertThat(
            recordedRequest.getHeader("Content-Type")!!.split(";".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()[0]).isEqualTo("image/png")
        assertThat(recordedRequest.body.readString(Charset.defaultCharset()))
            .isEqualTo("IMAGE")
        assertThat(response.body()).isNotNull
        assertThat(response.body()).usingRecursiveComparison().isEqualTo(
            InboxResponse(
                InboxResponseState.REVIEW,
                "message",
                123456L,
                "123456.png",
                "https://localhost:8080/inbox/123456.png",
                4711L
            )
        )
    }

    @Test
    fun postProblemReport() {
        server.enqueue(MockResponse().setBody(fromFile("reportProblem.json")))
        val problemReport = ProblemReport(
            "de",
            "4711",
            "Comment",
            ProblemType.OTHER,
            123456L,
            50.1,
            9.2,
            "New Title"
        )
        val response = client.reportProblem(problemReport).execute()
        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/reportProblem")
        assertThat(recordedRequest.getHeader("Content-Type"))
            .isEqualTo("application/json")
        assertThat(recordedRequest.body.readString(Charset.defaultCharset()))
            .isEqualTo("{\"countryCode\":\"de\",\"stationId\":\"4711\",\"comment\":\"Comment\",\"type\":\"OTHER\",\"photoId\":123456,\"lat\":50.1,\"lon\":9.2,\"title\":\"New Title\"}")
        assertThat(response.body()).isNotNull
        assertThat(response.body()).usingRecursiveComparison().isEqualTo(
            InboxResponse(
                InboxResponseState.REVIEW, "message", 123456L
            )
        )
    }

    private fun fromFile(filename: String?): Buffer {
        return Buffer().readFrom(
            Objects.requireNonNull(javaClass.classLoader).getResourceAsStream(filename)
        )
    }
}