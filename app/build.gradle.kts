import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.google.services)
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

        // Lista explicita de configuracoes Android e valores quando ausentes.
        // Assim o CI compila sem copiar credenciais privadas para o APK.
        //
        // "PREENCHER" e a sentinela que o GoogleSignInHelper e o ApiBaseUrl
        // reconhecem: com ela o app abre e avisa que falta configurar, em vez
        // de fechar sozinho.
        //
        // Nenhum endereco de API entra aqui como reserva de verdade. Eles
        // mudam de um dia para o outro, e deixar o antigo no codigo faz o app
        // falar com um host morto em silencio - foi o que aconteceu com a
        // ceris-core.vercel.app, que responde DEPLOYMENT_NOT_FOUND.
        val camposObrigatorios =
            mapOf(
                "GOOGLE_WEB_CLIENT_ID" to "PREENCHER",
                "CORE_API_BASE_URL" to "PREENCHER",
                "AUTH_API_BASE_URL" to "PREENCHER",
                "IA_API_BASE_URL" to "PREENCHER",
                "NOTIFY_API_BASE_URL" to "PREENCHER",
                "API_KEY" to "PREENCHER",
                "CERISIA_API_KEY" to "PREENCHER",
                "ACCOUNT_TYPE" to "HOUSEHOLD",
                "VIA_CEP_URL" to "https://viacep.com.br/",
            )

        camposObrigatorios.forEach { (nome, reserva) ->
            // Apenas configuracoes do Android entram no APK. Outras chaves
            // locais, como credenciais privadas de servidor, ficam de fora.
            val valor = localProperties.getProperty(nome)?.takeIf { it.isNotBlank() } ?: reserva
            val literal =
                valor.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
            buildConfigField("String", nome, "\"$literal\"")
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

    // Mesmo projeto Firebase do Notify; as versoes ficam no catalogo.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

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
