plugins { id("com.android.application"); kotlin("android"); kotlin("plugin.compose"); kotlin("plugin.serialization") }
val signingRoot = System.getenv("FIGHTFORGE_SIGNING_DIR") ?: "/workspace/fightforge-signing"
val passwordFile = file("$signingRoot/store-password")
android {
    namespace = "com.fightforge.game"
    compileSdk = 34
    defaultConfig { testInstrumentationRunner = "com.fightforge.game.ForgeInstrumentation"; applicationId = "com.fightforge.game"; minSdk = 29; targetSdk = 34; versionCode = providers.gradleProperty("releaseCode").orElse("1").get().toInt(); versionName = providers.gradleProperty("releaseName").orElse("0.1.0").get() }
    if (passwordFile.exists()) signingConfigs.create("stable") {
        storeFile = file("$signingRoot/release.p12")
        storePassword = passwordFile.readText().trim()
        keyAlias = "fightforge"
        keyPassword = passwordFile.readText().trim()
        storeType = "PKCS12"
    }
    buildTypes { release { isMinifyEnabled = false; if (passwordFile.exists()) signingConfig = signingConfigs.getByName("stable") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":engine"))
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
