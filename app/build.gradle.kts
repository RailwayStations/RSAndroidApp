import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

plugins {
    kotlin("kapt")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.junit5)
    alias(libs.plugins.hilt.android)
}

abstract class GitVersionValueSource : ValueSource<Int, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): Int {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "rev-list", "HEAD", "--count")
            standardOutput = output
        }
        return Integer.valueOf(String(output.toByteArray(), Charset.defaultCharset()).trim())
    }
}

android {
    namespace = "de.bahnhoefe.deutschlands.bahnhofsfotos"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        testInstrumentationRunnerArguments += mapOf("runnerBuilder" to "de.mannodermaus.junit5.AndroidJUnit5Builder")
        applicationId = "de.bahnhoefe.deutschlands.bahnhofsfotos"
        compileSdk = 35
        minSdk = 26
        targetSdk = 35
        versionCode = 93
        versionName = "16.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    signingConfigs {
        register("nightly") {
            if (System.getProperty("nightly_store_file") != null) {
                storeFile = file(System.getProperty("nightly_store_file"))
                storePassword = System.getProperty("nightly_store_password")
                keyAlias = System.getProperty("nightly_key_alias")
                keyPassword = System.getProperty("nightly_key_password")
            }
        }
        register("release") {
            if (System.getProperty("release_store_file") != null) {
                storeFile = file(System.getProperty("release_store_file"))
                storePassword = System.getProperty("release_store_password")
                keyAlias = System.getProperty("release_key_alias")
                keyPassword = System.getProperty("release_key_password")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        register("nightly") {
            signingConfig = signingConfigs.getByName("nightly")
            applicationIdSuffix = ".nightly"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        abortOnError = false
    }

    val gitVersionProvider = providers.of(GitVersionValueSource::class) {}
    val gitVersion = gitVersionProvider.get()

    applicationVariants.all {
        if (name == "nightly" || name == "debug") {
            outputs.forEach { output ->
                output as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                output.versionCodeOverride = gitVersion
                output.versionNameOverride = "${applicationId}_${output.versionCode}"
                output.outputFileName = "${applicationId}_${versionCode}.apk"
            }
        } else {
            outputs.forEach { output ->
                output as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                output.outputFileName = "${applicationId}_${versionName}.apk"
            }
        }

    }
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.mapsforge.map.reader)
    implementation(libs.mapsforge.core)
    implementation(libs.mapsforge.themes)
    implementation(libs.mapsforge.map)
    implementation(libs.mapsforge.map.android)
    implementation(libs.mapsforge.map.reader)
    implementation(libs.mapsforge.themes)
    implementation(libs.kxml2)
    implementation(libs.androidsvg)
    implementation(libs.commons.lang3)
    implementation(libs.commons.io)
    implementation(libs.photo.view)
    implementation(libs.legacy.support.v4)
    implementation(libs.cardview)
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.activity.ktx)
    implementation(libs.material)
    implementation(libs.browser)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    implementation(libs.glide)
    implementation(libs.library)
    implementation(libs.gson)
    implementation(libs.multidex)
    implementation(libs.recyclerview)
    implementation(libs.security.crypto)
    implementation(libs.circleimageview)
    implementation(libs.hilt.android)

    kapt(libs.hilt.android.compiler)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testRuntimeOnly(libs.junit.jupiter.engine)

    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.android.test.core)
    androidTestImplementation(libs.junit.jupiter.api)
    androidTestImplementation(libs.junit.jupiter.params)
    androidTestImplementation(libs.assertj.core)
    androidTestRuntimeOnly(libs.android.test.runner)

}
