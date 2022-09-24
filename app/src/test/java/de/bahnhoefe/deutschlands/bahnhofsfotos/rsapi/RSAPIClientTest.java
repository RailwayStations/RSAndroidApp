package de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScoreItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
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
        client = new RSAPIClient(baseUrl.toString(), "", "");
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
                new Country("India", "in", null, "@Bahnhofsoma, #dbHackathon, #dbOpendata, #Bahnhofsfoto, @khgdrn",
                        "https://enquiry.indianrail.gov.in/ntes/", null, Collections.emptyList()),
                new Country("Schweiz", "ch", "fotos@schweizer-bahnhoefe.ch", "@BahnhoefeCH, @Bahnhofsoma, #BahnhofsfotoCH",
                        "http://fahrplan.sbb.ch/bin/stboard.exe/dn?input={title}&REQTrain_name=&boardType=dep&time=now&maxJourneys=20&selectDate=today&productsFilter=1111111111&start=yes",
                        null, List.of(
                        new ProviderApp("android", "SBB Mobile", "https://play.google.com/store/apps/details?id=ch.sbb.mobile.android.b2c"),
                        new ProviderApp("ios", "SBB Mobile", "https://apps.apple.com/app/sbb-mobile/id294855237")
                ))
        );
    }

    @Test
    void getHighScore() throws Exception {
        server.enqueue(new MockResponse().setBody(fromFile("highscore.json")));

        var response = client.getHighScore().execute();

        assertThat(server.takeRequest().getPath()).isEqualTo("/photographers");
        assertThat(response.body()).isNotNull();
        assertThat(response.body().getItems()).containsExactly(
                new HighScoreItem("User1", 2581, 1),
                new HighScoreItem("User2", 1109, 2),
                new HighScoreItem("User3", 812, 3)
        );
    }

    @Test
    void getHighScoreWithCountry() throws Exception {
        server.enqueue(new MockResponse().setBody(fromFile("highscore.json")));

        var response = client.getHighScore("de").execute();

        assertThat(server.takeRequest().getPath()).isEqualTo("/photographers?country=de");
        assertThat(response.body()).isNotNull();
        assertThat(response.body().getItems()).containsExactly(
                new HighScoreItem("User1", 2581, 1),
                new HighScoreItem("User2", 1109, 2),
                new HighScoreItem("User3", 812, 3)
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

    Buffer fromFile(final String filename) throws Exception {
        return new Buffer().readFrom(Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream(filename));
    }

}