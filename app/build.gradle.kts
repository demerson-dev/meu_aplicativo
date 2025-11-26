plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ifpr.androidapptemplate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ifpr.androidapptemplate"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))

    // Firebase dependencies - When using the BoM, don't specify versions in Firebase dependencies
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.common.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.crashlytics.buildtools)

    // Add Firebase Analytics
    implementation(libs.firebase.analytics)

    // Google Play Services
    implementation(libs.play.services.auth)

    // Glide for image loading
    implementation(libs.glide)

    implementation(libs.jetbrains.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Task para obter o SHA-1 do certificado de debug
tasks.register("getDebugSha1") {
    doLast {
        val userHome = System.getProperty("user.home")
        val keystorePath = "$userHome\\.android\\debug.keystore"
        val keystore = File(keystorePath)

        println("\n" + "=".repeat(70))
        println("  OBTER SHA-1 DO CERTIFICADO DE DEBUG")
        println("=".repeat(70))
        println("\nVerificando certificado em: $keystorePath\n")

        if (!keystore.exists()) {
            println("❌ ERRO: Certificado de debug não encontrado!")
            println("\nO certificado debug.keystore ainda não foi criado.")
            println("Execute o app no emulador ou dispositivo pelo menos uma vez.")
            println("\n" + "=".repeat(70) + "\n")
            return@doLast
        }

        try {
            // Tenta encontrar o keytool
            val javaHome = System.getProperty("java.home")
            val keytoolPath = "$javaHome\\bin\\keytool.exe"

            val keytoolCmd = if (File(keytoolPath).exists()) {
                keytoolPath
            } else {
                "keytool" // Tenta usar do PATH
            }

            println("Executando keytool...\n")

            val process = ProcessBuilder(
                keytoolCmd, "-list", "-v",
                "-keystore", keystorePath,
                "-alias", "androiddebugkey",
                "-storepass", "android",
                "-keypass", "android"
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            // Extrai SHA-1 e SHA-256 (case insensitive)
            val sha1Regex = "SHA1:\\s*([A-Fa-f0-9:]+)".toRegex()
            val sha256Regex = "SHA256:\\s*([A-Fa-f0-9:]+)".toRegex()

            val sha1 = sha1Regex.find(output)?.groupValues?.get(1)
            val sha256 = sha256Regex.find(output)?.groupValues?.get(1)

            if (sha1 != null) {
                println("✅ SHA-1 (Debug Certificate):")
                println("   $sha1")
                println()
            } else {
                println("⚠️ SHA-1 não encontrado no output do keytool")
                println("\nOutput completo:")
                println(output)
            }

            if (sha256 != null) {
                println("✅ SHA-256 (Debug Certificate):")
                println("   $sha256")
                println()
            }

            if (sha1 != null) {
                println("=".repeat(70))
                println("  📋 PRÓXIMOS PASSOS:")
                println("=".repeat(70))
                println("\n1. 📋 COPIE o SHA-1 acima")
                println("\n2. 🌐 Abra o Firebase Console:")
                println("   https://console.firebase.google.com/project/aplicativotreinos/settings/general")
                println("\n3. 📱 Localize o app Android:")
                println("   - Role até 'Seus apps'")
                println("   - Clique em 'appTreinos' (com.ifpr.androidapptemplate)")
                println("\n4. 🔐 Adicione a impressão digital:")
                println("   - Encontre 'Impressões digitais do certificado SHA'")
                println("   - Clique em '+ Adicionar impressão digital'")
                println("   - Cole o SHA-1")
                println("   - Clique em 'Salvar'")
                println("\n5. 📥 Baixe o novo google-services.json:")
                println("   - Clique no botão 'google-services.json'")
                println("   - Substitua em: app/google-services.json")
                println("\n6. 🔄 Sincronize o projeto:")
                println("   - Clique em 'Sync Now' no Android Studio")
                println("   - Build → Clean Project")
                println("   - Build → Rebuild Project")
                println("\n7. 🎉 Teste o login com Google!")
                println("\n" + "=".repeat(70) + "\n")
            }

        } catch (e: Exception) {
            println("❌ ERRO ao executar keytool: ${e.message}")
            println("\nPossíveis soluções:")
            println("1. Certifique-se que o JDK está instalado")
            println("2. Adicione o JDK/bin ao PATH do sistema")
            println("3. Execute o app pelo menos uma vez para gerar o certificado")
            println("\n" + "=".repeat(70) + "\n")
        }
    }
}
