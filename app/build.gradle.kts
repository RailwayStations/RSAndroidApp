import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("de.mannodermaus.android-junit5")
}

fun getVersionCode(): Int {
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
        versionCode = 85
        versionName = "15.0.0"
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
            isMinifyEnabled = true
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
        outputs.all {
            this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            versionCodeOverride = getVersionCode()
            buildType.name
            val apkName = "${applicationId}_${versionCode}.apk"
            outputFileName = apkName
        }
    }

}

val mapsforgeVersion = "0.20.0"
val retrofitVersion = "2.9.0"
val junitVersion = "5.10.0"
val assertJVersion = "3.24.2"

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation(group = "com.squareup.retrofit2", name = "retrofit", version = retrofitVersion)
    implementation(
        group = "com.squareup.retrofit2",
        name = "converter-gson",
        version = retrofitVersion
    )
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(
        group = "org.mapsforge",
        name = "mapsforge-map-reader",
        version = mapsforgeVersion
    )
    implementation(group = "org.mapsforge", name = "mapsforge-core", version = mapsforgeVersion)
    implementation(group = "org.mapsforge", name = "mapsforge-themes", version = mapsforgeVersion)
    implementation(group = "org.mapsforge", name = "mapsforge-map", version = mapsforgeVersion)
    implementation(
        group = "org.mapsforge",
        name = "mapsforge-map-android",
        version = mapsforgeVersion
    )
    implementation(
        group = "org.mapsforge",
        name = "mapsforge-map-android",
        version = mapsforgeVersion
    )
    implementation(
        group = "org.mapsforge",
        name = "mapsforge-map-reader",
        version = mapsforgeVersion
    )
    implementation(group = "org.mapsforge", name = "mapsforge-themes", version = mapsforgeVersion)
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

    testImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter-api",
        version = junitVersion
    )
    testImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter-params",
        version = junitVersion
    )
    testImplementation(group = "org.assertj", name = "assertj-core", version = assertJVersion)
    testImplementation("org.mockito:mockito-core:5.6.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testRuntimeOnly(
        group = "org.junit.jupiter",
        name = "junit-jupiter-engine",
        version = junitVersion
    )

    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("de.mannodermaus.junit5:android-test-core:1.3.0")
    androidTestImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter-api",
        version = junitVersion
    )
    androidTestImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter-params", version = junitVersion
    )
    androidTestImplementation(
        group = "org.assertj",
        name = "assertj-core",
        version = assertJVersion
    )
    androidTestRuntimeOnly("de.mannodermaus.junit5:android-test-runner:1.3.0")

}
