plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
 //   id("com.chaquo.python")
}

android {
    namespace = "com.belaku.homey"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.belaku.homey"
        minSdk = 28
        targetSdk = 35
        versionCode = 108
        versionName = "108.0"

        renderscriptTargetApi = 18
        renderscriptSupportModeEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // On Apple silicon, you can omit x86_64.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }


      /*  chaquopy {
            defaultConfig {
                pip {
                    version = "3.8" // Specify your desired Python version
                    pip {
                        install("apify-client")
                    }
                }
            }
        }*/

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

  //  implementation(libs.androidx.activity.ktx)


    implementation(libs.zxing.android.embedded)
    implementation(libs.androidx.viewpager2)
    implementation(libs.material.v1130) // Or the latest version
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.firebase.analytics)

    implementation(libs.firebase.ai)

    implementation(libs.android.maps.utils)
    implementation(libs.android.gif.drawable)
    implementation(libs.material.v120alpha01) // Replace X.Y.Z with the latest stable version
    implementation(libs.picasso)
    implementation(libs.gson.v288)
  //  implementation(libs.androidx.multidex)
    implementation(libs.gms.play.services.location)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.picasso.v28)
    implementation(libs.gson)
    implementation(libs.glide)

    annotationProcessor(libs.compiler) // Use the same version
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.volley)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.material3.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.play.services.ads)
}