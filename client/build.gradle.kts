import java.util.Properties
plugins { id("com.android.application") }
val signingFile = File(System.getProperty("user.home"), ".local/share/aragonite-powersync/signing/client.properties")
val signingProps = Properties().apply { if(signingFile.exists()) signingFile.inputStream().use { load(it) } }
android {
 namespace = "dev.aragonite.powersync.client"
 compileSdk = 35
 defaultConfig { applicationId = "dev.aragonite.powersync.client"; minSdk = 35; targetSdk = 35; versionCode = 7; versionName = "0.1.6" }
 buildFeatures { buildConfig = true }
 signingConfigs { if(signingFile.exists()) create("persistent") { storeFile = File(signingProps.getProperty("storeFile")); storePassword = signingProps.getProperty("storePassword"); keyAlias = signingProps.getProperty("keyAlias"); keyPassword = signingProps.getProperty("keyPassword") } }
 buildTypes { getByName("release") { if(signingFile.exists()) signingConfig = signingConfigs.getByName("persistent") } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { testImplementation("junit:junit:4.13.2"); testImplementation("org.json:json:20240303") }
