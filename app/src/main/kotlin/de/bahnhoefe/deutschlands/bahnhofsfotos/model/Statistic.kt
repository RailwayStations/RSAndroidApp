package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Statistic {

    int total;
    int withPhoto;
    int withoutPhoto;
    int photographers;

}
