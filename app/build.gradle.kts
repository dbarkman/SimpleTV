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

// Text made safe inside a Java string literal in BuildConfig.
fun String.javaString(): String = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

// The household this build is for (-Phousehold=mom): its location and sync key come from
// ~/.config/simpletv/<household>/. Without it, the build is for our own home and reads
// ~/.config/simpletv/ itself.
val household = providers.gradleProperty("household").orNull.orEmpty()
val householdDir = ".config/simpletv" + if (household.isEmpty()) "" else "/$household"

// Each build is newer than the last, so sticks know to update (Updater.kt); scripts/release.sh
// passes one version for every household in a release.
val buildVersion = providers.gradleProperty("versionCode").orNull?.toInt()
    ?: (System.currentTimeMillis() / 60_000).toInt()

android {
    namespace = "com.redmesastudios.simpletv"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.redmesastudios.simpletv"
        minSdk = 34
        targetSdk = 37
        versionCode = buildVersion
        versionName = "1.0"

        buildConfigField("String", "TMDB_TOKEN", "\"${secret(".config/tmdb/read_token")}\"")
        buildConfigField("String", "WATCHMODE_KEY", "\"${secret(".config/watchmode/api_key")}\"")
        // Rotten Tomatoes, IMDb and Metacritic scores for the Movies page.
        buildConfigField("String", "OMDB_KEY", "\"${credential(".config/omdb/api_key", "API_KEY").ifEmpty { secret(".config/omdb/api_key").takeIf { '=' !in it }.orEmpty() }}\"")
        buildConfigField("String", "SIGNALPOST_KEY", "\"${secret(".config/signalpost/api_key")}\"")
        // Sticks built with the same key share Continue Watching and photos (Sync.kt).
        buildConfigField("String", "SYNC_KEY", "\"${secret("$householdDir/sync_key")}\"")
        // Where the TV is, for weather and local news; see README.
        buildConfigField("String", "WEATHER_POINT", "\"${credential("$householdDir/home.txt", "weather_point")}\"")
        buildConfigField("String", "LOCAL_NEWS_MARKET", "\"${credential("$householdDir/home.txt", "local_news_market")}\"")
        buildConfigField("String", "LOCAL_NEWS_NAME", "\"${credential("$householdDir/home.txt", "local_news_name")}\"")
        // Which holiday packs to decorate for (HolidayCalendar.kt); blank means the defaults.
        buildConfigField("String", "HOLIDAY_PACKS", "\"${credential("$householdDir/home.txt", "holiday_packs").javaString()}\"")
        // Birthdays and anniversaries (FamilyDays.kt), kept out of the repo.
        buildConfigField("String", "FAMILY_DAYS", "\"${secret("$householdDir/birthdays.txt").javaString()}\"")
        buildConfigField("String", "YOUTUBE_CLIENT_ID", "\"${credential(".config/youtube/credentials.txt", "client_id")}\"")
        buildConfigField("String", "YOUTUBE_CLIENT_SECRET", "\"${credential(".config/youtube/credentials.txt", "client_secret")}\"")
        // Updates: which household's build to fetch, and a read-only token for the private
        // builds repo. Without the token, SimpleTV doesn't update itself.
        buildConfigField("String", "HOUSEHOLD", "\"${household.ifEmpty { "home" }}\"")
        buildConfigField("String", "UPDATE_TOKEN", "\"${secret(".config/simpletv/update_token")}\"")
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
    // Android's org.json is a stub off the device; the real one lets tests read records.
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}