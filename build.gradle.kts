import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.11"
}

group = "net.sourceforge.owlapi"
version = "1.4.3.456"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    mavenLocal()  // Load metamodelling dependencies from Maven local repo
    // Repositorio local para dependencias específicas
    flatDir {
        dirs("repo")
    }
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.uiTooling)
    implementation(compose.foundation)
    implementation(compose.runtime)
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    
    // OWLAPI metamodelling distribution (from mavenLocal)
    implementation("net.sourceforge.owlapi:owlapi-distribution-metamodelling:4.5.7")
    
    // HermiT reasoner metamodelling (built from this project)
    implementation("net.sourceforge.owlapi:org.semanticweb.hermit-metamodelling:1.4.3.456")
    
    // SLF4J - required by OWLAPI
    implementation("org.slf4j:slf4j-simple:1.7.36")
    
    // Commons and utilities
    implementation("commons-logging:commons-logging:1.2")
    implementation("com.google.guava:guava:32.1.3-jre")
    
    // GNU Getopt
    implementation("gnu.getopt:java-getopt:1.0.13")
    
    // Testing
    testImplementation("junit:junit:4.12")
    testImplementation("junit:junit:3.8.2")
}

compose.desktop {
    application {
        mainClass = "org.semanticweb.hermit.ui.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "HermiT-Ontology-Editor"
            packageVersion = "1.4.3"
            
            description = "HermiT Ontology Editor - Visual OWL Ontology Creator"
            copyright = "© 2024 HermiT Team"
            vendor = "Oxford University & OWLAPI Team"
            
            macOS {
                bundleID = "org.semanticweb.hermit.editor"
                dockName = "HermiT Editor"
            }
            
            windows {
                menuGroup = "HermiT"
                upgradeUuid = "18159995-d967-4CD2-8885-77BFA97CFA9F"
            }
            
            linux {
                packageName = "hermit-ontology-editor"
            }
        }
    }
}

kotlin {
    jvmToolchain(11)
}

// Excluir archivos Java problemáticos para compilar solo la UI
sourceSets {
    main {
        java {
            // Incluir código fuente Java (Core de HermiT)
            srcDir("src/main/java")
            // Excluir vieja UI si causa conflictos con Compose/Kotlin
            exclude("org/semanticweb/hermit/ui/**") 
            // Excluir integración con Protege (no necesaria para standalone)
            exclude("**/Protege*")
        }
        kotlin {
            srcDir("src/main/kotlin")
        }
    }
    test {
        java {
            srcDir("src/test/java")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }
}

tasks.register("runApp", JavaExec::class) {
    group = "application"
    description = "Run the HermiT Ontology Editor"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.semanticweb.hermit.ui.MainKt")
}

tasks.register("runGailQuestionnaire", JavaExec::class) {
    group = "application"
    description = "Run the Gail Questionnaire App"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.semanticweb.hermit.ui.GailQuestionnaireMainKt")
}