import java.util.Properties
plugins { id("com.android.application") }
val signingFile = File(System.getProperty("user.home"), ".local/share/aragonite-powersync/signing/client.properties")
val signingProps = Properties().apply { if(signingFile.exists()) signingFile.inputStream().use { load(it) } }
android {
 namespace = "dev.aragonite.powersync.client"
 compileSdk = 35
 defaultConfig { applicationId = "dev.aragonite.powersync.client"; minSdk = 35; targetSdk = 35; versionCode = 11; versionName = "0.3.0" }
 // Per-app language (Android Settings → App languages) lists every values-<locale> folder.
 androidResources { generateLocaleConfig = true }
 // New user-facing text belongs in strings.xml, and every language must cover every string.
 lint { error += listOf("HardcodedText", "SetTextI18n", "MissingTranslation", "StringFormatInvalid", "StringFormatMatches", "MissingQuantity") }
 buildFeatures { buildConfig = true }
 signingConfigs { if(signingFile.exists()) create("persistent") { storeFile = File(signingProps.getProperty("storeFile")); storePassword = signingProps.getProperty("storePassword"); keyAlias = signingProps.getProperty("keyAlias"); keyPassword = signingProps.getProperty("keyPassword") } }
 buildTypes { getByName("release") { if(signingFile.exists()) signingConfig = signingConfigs.getByName("persistent") } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { testImplementation("junit:junit:4.13.2"); testImplementation("org.json:json:20240303") }
