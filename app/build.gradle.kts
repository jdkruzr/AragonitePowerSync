plugins { id("com.android.application") }
android {
 namespace = "dev.aragonite.powersync"
 compileSdk = 35
 defaultConfig { applicationId = "dev.aragonite.powersync"; minSdk = 30; targetSdk = 35; versionCode = 1; versionName = "0.1.0-probe" }
 buildFeatures { aidl = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
