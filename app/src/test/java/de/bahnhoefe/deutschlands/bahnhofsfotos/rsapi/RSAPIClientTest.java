package de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScoreItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Photo;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoLicense;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStation;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStations;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Photographer;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

class RSAPIClientTest {

    private MockWebServer server;
    private RSAPIClient client;

    @BeforeEach
    void setup() throws Exception {
        server = new MockWebServer();
        server.start();
        var baseUrl = server.url("/");
        client = new RSAPIClient(baseUrl.toString(), "clientId", null, "rsapiRedirectScheme://rsapiRedirectHost");
    }

    @AfterEach
    void teardown() throws Exception {
        server.shutdown();
    }

    @Test
    void getCountries() throws Exception {
        server.enqueue(new MockResponse().setBody(fromFile("countries.json")));

        var response = client.getCountries().execute();

        assertThat(server.takeRequest().getPath()).isEqualTo("/countries");
        assertThat(response.body()).isNotNull();
        assertThat(response.body()).containsExactly(
                new Country("in", "India", null, "@Bahnhofsoma, #dbHackathon, #dbOpendata, #Bahnhofsfoto, @khgdrn", "https://enquiry.indianrail.gov.in/ntes/"),
                new Country("ch", "Schweiz", "fotos@schweizer-bahnhoefe.ch", "@BahnhoefeCH, @Bahnhofsoma, #BahnhofsfotoCH",
                        "http://fahrplan.sbb.ch/bin/stboard.exe/dn?input={title}&REQTrain_name=&boardType=dep&time=now&maxJourneys=20&selectDate=today&productsFilter=1111111111&start=yes",
                        null,
                        List.of(
                                new ProviderApp(
                                        "android",
                                        "SBB Mobile",
                                        "https://play.google.com/store/apps/details?id=ch.sbb.mobile.android.b2c"),
                                new ProviderApp(
                                        "ios",
                                        "SBB Mobile",
                                        "https://apps.apple.com/app/sbb-mobile/id294855237"))));
    }

    @Test
    void getHighScore() throws Exception {
        server.enqueue(new MockResponse().setBody(fromFile("highscore.json")));

        var response = client.getHighScore().execute();

        assertThat(server.takeRequest().getPath()).isEqualTo("/photographers");
        assertThat(response.body()).isNotNull();
        assertThat(response.body().getItems()).containsExactly(
                new HighScoreItem(
                        "User1",
                        2581,
                        1),
                new HighScoreItem(
                        "User2",
                        1109,
                        2),
                new HighScoreItem(
                        "User3",
                        812,
                        3)
        );
    }

    @Test
    void getHighScoreWithCountry() throws Exception {
        server.enqueue(new MockResponse().setBody(fromFile("highscore.json")));

        var response = client.getHighScore("de").execute();

        assertThat(server.takeRequest().getPath()).isEqualTo("/photographers?country=de");
        assertThat(response.body()).isNotNull();
        assertThat(response.body().getItems()).containsExactly(
                new HighScoreItem(
                        "User1",
                        2581,
                        1),
                new HighScoreItem(
                        "User2",
                        1109,
                        2),
                new HighScoreItem(
                        "User3",
                        812,
                        3)
        );
    }

    @Test
    void getStatsWithCountry() throws Exception {
        server.enqueue(new MockResponse().setBody(fromFile("stats.json")));

        var response = client.getStatistic("de").execute();

        assertThat(server.takeRequest().getPath()).isEqualTo("/stats?country=de");
        assertThat(response.body()).isNotNull();
        assertThat(response.body()).usingRecursiveComparison().isEqualTo(new Statistic(7863, 7861, 2, 249));
    }

    @Test
    void getPhotoStationById() throws Exception {
        server.enqueue(new MockResponse().setBody(fromFile("photoStationById.json")));

        var response = client.getPhotoStationById("de", "1973").execute();

        assertThat(server.takeRequest().getPath()).isEqualTo("/photoStationById/de/1973");
        assertThat(response.body()).isNotNull();
        assertThat(response.body()).isEqualTo(new PhotoStations(
                "https://api.railway-stations.org/photos",
                List.of(new PhotoLicense(
                        "CC0_10",
                        "CC0 1.0 Universell (CC0 1.0)",
                        new URL("https://creativecommons.org/publicdomain/zero/1.0/"))),
                List.of(new Photographer(
                        "@User1",
                        new URL("https://example.com/@User1"))),
                List.of(new PhotoStation(
                        "de",
                        "1973",
                        "Fulda",
                        50.5547372607544,
                        9.6843855869764,
                        "FFU",
                        false,
                        List.of(
                                new Photo(
                                        4430L,
                                        "@User1",
                                        "/de/1973.jpg",
                                        1451846273000L,
                                        "CC0_10"),
                                new Photo(
                                        4431L,
                                        "@User1",
                                        "/de/1973_2.jpg",
                                        1451846279000L,
                                        "CC0_10")
                        )))
        ));
    }

    @Test
    void getPhotoStationsByCountry() throws Exception {
        server.enqueue(new MockResponse().setBody(fromFile("photoStationsByCountry.json")));

        var response = client.getPhotoStationsByCountry("de").execute();

        assertThat(server.takeRequest().getPath()).isEqualTo("/photoStationsByCountry/de");
        assertThat(response.body()).isNotNull();
        assertThat(response.body()).isEqualTo(new PhotoStations(
                "https://api.railway-stations.org/photos",
                List.of(new PhotoLicense(
                        "CC0_10",
                        "CC0 1.0 Universell (CC0 1.0)",
                        new URL("https://creativecommons.org/publicdomain/zero/1.0/"))),
                List.of(new Photographer(
                        "@User1",
                        new URL("https://example.com/@User1"))),
                List.of(new PhotoStation(
                                "de",
                                "Z11",
                                "Bahnhof Zoo",
                                51.03745553026876,
                                13.757279123256026,
                                "",
                                false,
                                List.of()),
                        new PhotoStation(
                                "de",
                                "1973",
                                "Fulda",
                                50.5547372607544,
                                9.6843855869764,
                                "FFU",
                                false,
                                List.of(
                                        new Photo(
                                                4430L,
                                                "@User1",
                                                "/de/1973.jpg",
                                                1451846273000L,
                                                "CC0_10")
                                )))));
    }

    @Test
    void postPhotoUpload() throws Exception {
        server.enqueue(new MockResponse().setBody(fromFile("photoUpload.json")));

        var requestBody = RequestBody.create("IMAGE".getBytes(), MediaType.parse(URLConnection.guessContentTypeFromName("photo.png")));
        var response = client.photoUpload("4711", "de", "Title", 50d, 9d, "Comment", null, requestBody).execute();

        var recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/photoUpload");
        assertThat(recordedRequest.getHeader("Station-Id")).isEqualTo("4711");
        assertThat(recordedRequest.getHeader("Country")).isEqualTo("de");
        assertThat(recordedRequest.getHeader("Station-Title")).isEqualTo("Title");
        assertThat(recordedRequest.getHeader("Latitude")).isEqualTo("50.0");
        assertThat(recordedRequest.getHeader("Longitude")).isEqualTo("9.0");
        assertThat(recordedRequest.getHeader("Comment")).isEqualTo("Comment");
        assertThat(recordedRequest.getHeader("Active")).isNull();
        assertThat(recordedRequest.getHeader("Content-Type").split(";")[0]).isEqualTo("image/png");
        assertThat(recordedRequest.getBody().readString(Charset.defaultCharset())).isEqualTo("IMAGE");
        assertThat(response.body()).isNotNull();
        assertThat(response.body()).usingRecursiveComparison().isEqualTo(new InboxResponse(InboxResponse.InboxResponseState.REVIEW, "message", 123456L, "123456.png", "https://localhost:8080/inbox/123456.png", 4711L));
    }

    @Test
    void postProblemReport() throws Exception {
        server.enqueue(new MockResponse().setBody(fromFile("reportProblem.json")));

        var problemReport = new ProblemReport("de", "4711", "Comment", ProblemType.OTHER, 123456L, 50.1, 9.2, "New Title");
        var response = client.reportProblem(problemReport).execute();

        var recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/reportProblem");
        assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json");
        assertThat(recordedRequest.getBody().readString(Charset.defaultCharset())).isEqualTo("{\"countryCode\":\"de\",\"stationId\":\"4711\",\"comment\":\"Comment\",\"type\":\"OTHER\",\"photoId\":123456,\"lat\":50.1,\"lon\":9.2,\"title\":\"New Title\"}");
        assertThat(response.body()).isNotNull();
        assertThat(response.body()).usingRecursiveComparison().isEqualTo(new InboxResponse(InboxResponse.InboxResponseState.REVIEW, "message", 123456L));
    }

    Buffer fromFile(final String filename) throws Exception {
        return new Buffer().readFrom(Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream(filename));
    }

}