plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android") // ini yang kita ganti tadi
    id("com.google.gms.google-services")
    id("kotlin-parcelize") // plugin buat ngirim data antar activity
}

android {
    namespace = "com.example.weatherjournalapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.weatherjournalapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // dependensi firebase auth & gps
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // dependensi baru buat database
    implementation("com.google.firebase:firebase-database-ktx")

    // dependensi buat ngambil data api (retrofit)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // dependensi buat nampilin daftar (recyclerview)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // dependensi buat 'kotak' cardview
    implementation("androidx.cardview:cardview:1.0.0")
}