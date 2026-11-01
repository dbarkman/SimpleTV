plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// API keys stay out of the repo: they're read from ~/.config on the build machine.
fun secret(path: String): String = providers
    .fileContents(layout.projectDirectory.file(System.getProperty("user.home") + "/" + path))
    .asText.orNull?.trim().orEmpty()

// One "name=value" line from a credentials file in ~/.config.
fun credential(path: String, name: String): String = secret(path).lines()
    .firstOrNull { it.startsWith("$name=") }?.substringAfter('=')?.trim().orEmpty()

android {
    namespace = "com.redmesastudios.simpletv"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.redmesastudios.simpletv"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "TMDB_TOKEN", "\"${secret(".config/tmdb/read_token")}\"")
        buildConfigField("String", "WATCHMODE_KEY", "\"${secret(".config/watchmode/api_key")}\"")
        // Rotten Tomatoes, IMDb and Metacritic scores for the Movies page.
        buildConfigField("String", "OMDB_KEY", "\"${credential(".config/omdb/api_key", "API_KEY").ifEmpty { secret(".config/omdb/api_key").takeIf { '=' !in it }.orEmpty() }}\"")
        buildConfigField("String", "SIGNALPOST_KEY", "\"${secret(".config/signalpost/api_key")}\"")
        // Sticks built with the same key share Continue Watching and photos (Sync.kt).
        buildConfigField("String", "SYNC_KEY", "\"${secret(".config/simpletv/sync_key")}\"")
        // Where the TV is, for weather and local news; see README.
        buildConfigField("String", "WEATHER_POINT", "\"${credential(".config/simpletv/home.txt", "weather_point")}\"")
        buildConfigField("String", "LOCAL_NEWS_MARKET", "\"${credential(".config/simpletv/home.txt", "local_news_market")}\"")
        buildConfigField("String", "LOCAL_NEWS_NAME", "\"${credential(".config/simpletv/home.txt", "local_news_name")}\"")
        buildConfigField("String", "YOUTUBE_CLIENT_ID", "\"${credential(".config/youtube/credentials.txt", "client_id")}\"")
        buildConfigField("String", "YOUTUBE_CLIENT_SECRET", "\"${credential(".config/youtube/credentials.txt", "client_secret")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // QR codes for the photo upload page.
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}