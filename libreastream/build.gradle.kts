plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

group = "com.github.niusounds"
version = "0.2.0"

android {
    compileSdk = 31
    defaultConfig {
        minSdk = 21
        targetSdk = 31
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            //useProguard = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.ktor)
    implementation(libs.annotation)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.findByName("release"))
                artifactId = "libReaStream"
            }
        }
    }
}
