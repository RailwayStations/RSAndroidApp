package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.net.URL;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Photographer {

    String name;

    URL url;

}
