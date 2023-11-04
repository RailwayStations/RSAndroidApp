import java.io.ByteArrayOutputStream

plugins {
    kotlin("kapt")
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("de.mannodermaus.android-junit5")
    id("com.google.dagger.hilt.android")
}

fun getGitVersionCode(): Int {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-list", "HEAD", "--count")
            standardOutput = stdout
        }
        Integer.valueOf(stdout.toString().trim())
    } catch (ignored: Exception) {
        0
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
        testInstrumentationRunnerArguments +=
            mutableMapOf("runnerBuilder" to "de.mannodermaus.junit5.AndroidJUnit5Builder")
        applicationId = "de.bahnhoefe.deutschlands.bahnhofsfotos"
        compileSdk = 34
        minSdk = 26
        targetSdk = 34
        versionCode = 86
        versionName = "15.0.1"
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
    }

    packaging {
        resources {
            excludes += listOf("META-INF/*")
        }
    }

    lint {
        abortOnError = false
    }

    applicationVariants.all {
        if (name == "nightly") {
            outputs.forEach { output ->
                output as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                output.versionCodeOverride = getGitVersionCode()
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

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.mapsforge:mapsforge-map-reader:0.20.0")
    implementation("org.mapsforge:mapsforge-core:0.20.0")
    implementation("org.mapsforge:mapsforge-themes:0.20.0")
    implementation("org.mapsforge:mapsforge-map:0.20.0")
    implementation("org.mapsforge:mapsforge-map-android:0.20.0")
    implementation("org.mapsforge:mapsforge-map-android:0.20.0")
    implementation("org.mapsforge:mapsforge-map-reader:0.20.0")
    implementation("org.mapsforge:mapsforge-themes:0.20.0")
    implementation("net.sf.kxml:kxml2:2.3.0")
    implementation("com.caverock:androidsvg:1.4")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("commons-io:commons-io:20030203.000550")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.browser:browser:1.6.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.mcxiaoke.volley:library:1.0.19")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.dagger:hilt-android:2.48.1")

    kapt("com.google.dagger:hilt-android-compiler:2.48.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("de.mannodermaus.junit5:android-test-core:1.3.0")
    androidTestImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    androidTestImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    androidTestImplementation("org.assertj:assertj-core:3.24.2")
    androidTestRuntimeOnly("de.mannodermaus.junit5:android-test-runner:1.3.0")

}
