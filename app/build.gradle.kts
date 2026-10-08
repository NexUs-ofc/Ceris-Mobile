import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
}

// Carrega o local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

android {
    namespace = "com.example.ceris"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ceris"

        minSdk = 24
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Adiciona as propriedades do local.properties ao BuildConfig
        localProperties.forEach { (key, value) ->
            val propertyName = key.toString()

            if (propertyName != "sdk.dir") {
                buildConfigField(
                    "String",
                    propertyName,
                    "\"${value.toString().replace("\"", "\\\"")}\"",
                )
            }
        }

        // Campos que o codigo referencia e por isso precisam existir sempre.
        // O bloco acima so cria o que estiver no local.properties, e o CI
        // escreve apenas um subconjunto das chaves - sem isto, a compilacao
        // quebra la com "Unresolved reference".
        //
        // O valor de reserva e a sentinela que o GoogleSignInHelper reconhece:
        // com ela o app avisa que falta configurar, em vez de falhar sozinho.
        val camposObrigatorios =
            mapOf(
                "GOOGLE_WEB_CLIENT_ID" to "PREENCHER",
                "CORE_API_BASE_URL" to "https://ceris-core.vercel.app/",
            )

        camposObrigatorios.forEach { (nome, reserva) ->
            if (!localProperties.containsKey(nome)) {
                buildConfigField("String", nome, "\"$reserva\"")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.coordinatorlayout)

    // Material
    implementation(libs.material)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp
    implementation("com.squareup.okhttp3:logging-interceptor:3.14.9")

    // Google Sign-In via Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Coroutines (necessarias para o Credential Manager)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
