package fr.kosmik;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {
    String sha;
    @JsonProperty
    String name;

    @JsonProperty
    Release release;

    public static class Release {

        @JsonProperty("current-version")
        String currentVersion;
        @JsonProperty("next-version")
        String nextVersion;
    }
}
