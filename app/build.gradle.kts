plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Plugin do Room: exporta o schema do banco em JSON (uma versão por arquivo) e disponibiliza esses
// arquivos para os testes de migração. Commitamos a pasta: é o "histórico" do formato do banco.
room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "br.dia23.nodo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.dia23.nodo"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Permite usar java.time (LocalDate, ZoneId...) no minSdk 24; nativamente só existe a partir da API 26.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // ksp() = processador que GERA o código dos DAOs a partir das anotações, em tempo de compilação.
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    // Rotas tipadas do Navigation usam @Serializable (Kotlin serialization).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // hiltViewModel() para obter ViewModels do Hilt dentro de composables.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    // DataStore: armazenamento chave-valor assíncrono (substituto moderno do SharedPreferences).
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // MigrationTestHelper: cria o banco numa versão antiga e confere a migração para a nova.
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}