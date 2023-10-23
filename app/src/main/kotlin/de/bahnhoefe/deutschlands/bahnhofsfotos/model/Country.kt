package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import java.io.Serializable
import java.util.Objects
import java.util.Optional

data class Country @JvmOverloads constructor(
    val code: String,
    val name: String,
    val email: String? = null,
    val timetableUrlTemplate: String? = null,
    val overrideLicense: String? = null,
    val providerApps: List<ProviderApp> = ArrayList()
) : Serializable, Comparable<Country> {

    fun hasTimetableUrlTemplate(): Boolean {
        return !timetableUrlTemplate.isNullOrEmpty()
    }

    val compatibleProviderApps: List<ProviderApp>
        get() = ProviderApp.getCompatibleProviderApps(providerApps)

    override fun toString(): String {
        return name
    }

    override fun compareTo(other: Country): Int {
        return name.compareTo(other.name)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Country) return false
        return Objects.equals(code, other.code)
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    companion object {
        @JvmStatic
        fun getCountryByCode(
            countries: Collection<Country>,
            countryCode: String?
        ): Optional<Country> {
            return countries.stream()
                .filter { country: Country -> country.code == countryCode }
                .findAny()
        }
    }
}