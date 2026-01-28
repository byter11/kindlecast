plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.chaquopy.python)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.byter11.kindlecast"
    compileSdk = 36
    ndkVersion = "27"

    defaultConfig {
        applicationId = "io.github.byter11.kindlecast"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        packaging {
            jniLibs {
                // This stops Android from trying to map the .so directly from the APK
                // It forces an extraction to the filesystem, which avoids the alignment requirement
                useLegacyPackaging = true
            }
        }

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    chaquopy {

        defaultConfig {

            version = "3.13"
            pip {
                install("lxml==5.3.0")
                install("regex==2024.9.11")
                install("pillow==11.0.0")
                install("msgpack==1.1.2")
                install("css-parser==1.0.10")
                install("mechanize==0.4.10")
                install("beautifulsoup4==4.14.3")
                install("https://github.com/byter11/calibre-chaquopy/releases/download/1.0.0/calibre_chaquopy-1.0.0-cp313-cp313-android_21_arm64_v8a.whl")
            }
            pyc {
                src = false
                pip = false
            }
            extractPackages("calibre", "calibre_extensions", "html5_parser", "resources")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true

    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.nanohttpd)
    implementation(libs.material3)
    implementation(libs.androidx.documentfile)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}