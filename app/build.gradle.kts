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
        compileSdk = 34
        minSdk = 26
        targetSdk = 34
        versionCode = 92
        versionName = "15.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
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
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
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

    packaging {
        resources {
            excludes += listOf("META-INF/*")
        }
    }

    lint {
        abortOnError = false
    }

    val gitVersionProvider = providers.of(GitVersionValueSource::class) {}
    val gitVersion = gitVersionProvider.get()

    applicationVariants.all {
        if (name == "nightly") {
            outputs.forEach { output ->
                output as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                output.versionCodeOverride = gitVersion
                output.versionNameOverride = "${applicationId}_${output.versionCode}"
                output.outputFileName = "${applicationId}_${versionCode}.apk"
            }
        }
        outputs.forEach { output ->
            output as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            output.outputFileName = "${applicationId}_${output.versionCode}.apk"
        }
    }

}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.mapsforge.map.reader)
    implementation(libs.mapsforge.core)
    implementation(libs.mapsforge.themes)
    implementation(libs.mapsforge.map)
    implementation(libs.mapsforge.map.android)
    implementation(libs.org.mapsforge.mapsforge.map.android)
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
