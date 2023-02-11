package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HighScoreItem {
    String name;
    int photos;
    int position;
}
