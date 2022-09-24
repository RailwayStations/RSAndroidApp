package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Country implements Serializable {
    private String name;
    private String code;
    private String email;
    private String twitterTags;
    private String timetableUrlTemplate;
    private String overrideLicense;
    private List<ProviderApp> providerApps = new ArrayList<>();

    public Country() {

    }

    public Country(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public Country(String name, String code, String email, String twitterTags, String timetableUrlTemplate, String overrideLicense, List<ProviderApp> providerApps) {
        this(name, code);
        this.email = email;
        this.twitterTags = twitterTags;
        this.timetableUrlTemplate = timetableUrlTemplate;
        this.overrideLicense = overrideLicense;
        this.providerApps = providerApps;
    }

    public static Country getCountryByCode(Set<Country> countries, String countryCode) {
        return countries.stream()
                .filter(country -> country.getCode().equals(countryCode))
                .findFirst()
                .orElse(countries.iterator().next()); // get first country as fallback
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTwitterTags() {
        return twitterTags;
    }

    public void setTwitterTags(String twitterTags) {
        this.twitterTags = twitterTags;
    }

    public String getTimetableUrlTemplate() {
        return timetableUrlTemplate;
    }

    public boolean hasTimetableUrlTemplate() {
        return timetableUrlTemplate != null && !timetableUrlTemplate.isEmpty();
    }

    public void setTimetableUrlTemplate(String timetableUrlTemplate) {
        this.timetableUrlTemplate = timetableUrlTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var country = (Country) o;
        return code.equals(country.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    @NonNull
    public String toString() {
        return name;
    }

    public List<ProviderApp> getProviderApps() {
        providerApps.forEach(a -> a.setCountryCode(code));
        return providerApps;
    }

    public void setProviderApps(List<ProviderApp> providerApps) {
        this.providerApps = providerApps;
    }

    public boolean hasCompatibleProviderApps() {
        return ProviderApp.hasCompatibleProviderApps(providerApps);
    }

    public List<ProviderApp> getCompatibleProviderApps() {
        return ProviderApp.getCompatibleProviderApps(providerApps);
    }

    public String getOverrideLicense() {
        return overrideLicense;
    }

    public void setOverrideLicense(String overrideLicense) {
        this.overrideLicense = overrideLicense;
    }
}
