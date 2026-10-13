package org.semanticweb.hermit.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.semanticweb.hermit.ui.compose.GailQuestionnaireApp
import org.semanticweb.hermit.ui.theme.HermitTheme
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class GailQuestionnaireRepository {
    private val repository = SimpleOntologyRepository()
    
    fun loadBreastCancerOntology(): Boolean {
        println("Buscando ontología...")
        val owxFile = File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx")
        val owlFile = File("ontologias/EscenarioE/BreastCancerRecommendationWithoutMetamodelling.owx")
        
        val fileToUse = if (owxFile.exists()) {
            println("Intentando cargar: BreastCancerRecommendationWithMetamodelling.owx")
            owxFile
        } else if (owlFile.exists()) {
            println("Intentando cargar: BreastCancerRecommendationWithoutMetamodelling.owx")
            owlFile
        } else {
            println("ERROR: No se encontró ninguna ontología de BreastCancer")
            return false
        }
        
        return try {
            println("Archivo seleccionado: ${fileToUse.name}")
            println("Tamaño: ${fileToUse.length()} bytes")
            
            println("Cargando ontología...")
            repository.loadOntology(fileToUse)
            
            println("✓ Ontología cargada exitosamente!")
            println("  Clases: ${repository.getClasses().size}")
            println("  Individuos: ${repository.getIndividuals().size}")
            println("  Axiomas: ${repository.getAxiomCount()}")
            true
        } catch (e1: Exception) {
            println("ERROR al cargar ${fileToUse.name}:")
            println("  ${e1.message ?: "Sin mensaje"}")
            
            if (fileToUse == owxFile && owlFile.exists()) {
                println("\nIntentando archivo alternativo sin metamodelado...")
                try {
                    repository.loadOntology(owlFile)
                    println("✓ Ontología alternativa cargada!")
                    println("  Clases: ${repository.getClasses().size}")
                    println("  Individuos: ${repository.getIndividuals().size}")
                    return true
                } catch (e2: Exception) {
                    println("ERROR también con archivo alternativo: ${e2.message}")
                    e2.printStackTrace()
                    return false
                }
            }
            
            e1.printStackTrace()
            false
        }
    }
    
    fun savePatientResponses(responses: Map<String, String>) {
        try {
            println("Guardando respuestas del paciente en la ontología...")
            
            val patientName = "Patient_${System.currentTimeMillis()}"
            println("  Creando paciente: $patientName")
            
            val questionToProperty = mapOf(
                "age" to "hasAge",
                "age_first_period" to "hasHormonalValue",
                "age_first_birth" to "hasHormonalValue",
                "family_history" to "hasFamilyHistory",
                "breast_biopsies" to "hasBreastDiseaseValue",
                "hyperplasia" to "hasBreastDiseaseValue",
                "race" to "hasPersonalHistory"
            )
            
            responses.forEach { (questionId, answerId) ->
                println("  Procesando: $questionId -> $answerId")
                
                try {
                    repository.addClassAssertion(answerId, "Answer_value")
                    
                    val propertyName = questionToProperty[questionId] ?: "hasAnswerValue"
                    
                    repository.addObjectPropertyAssertion(
                        patientName,
                        propertyName,
                        answerId
                    )
                    
                    println("    ✓ Axioma creado: $patientName -[$propertyName]-> $answerId")
                } catch (e: Exception) {
                    println("    ✗ Error: ${e.message}")
                }
            }
            
            val outputFile = File("ontologias/custom/BreastCancer_GailResponses_${System.currentTimeMillis()}.owl")
            outputFile.parentFile.mkdirs()
            repository.saveOntology(outputFile)
            
            println("\n✓ Ontología guardada exitosamente!")
            println("  Archivo: ${outputFile.absolutePath}")
            println("  Paciente: $patientName")
            println("  Respuestas: ${responses.size}")
            println("\nFormato seguido: BreastCancerRecommendationWithMetamodelling")
        } catch (e: Exception) {
            println("ERROR al guardar respuestas: ${e.message}")
            e.printStackTrace()
        }
    }
}

@Composable
@Preview
fun GailApp() {
    val repository = remember { GailQuestionnaireRepository() }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val success = repository.loadBreastCancerOntology()
            isLoading = false
            if (!success) {
                loadError = "No se pudo cargar la ontología"
            }
        }
    }
    
    HermitTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Cargando ontología de Cáncer de Mama...",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                loadError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "⚠️ Error al cargar la ontología",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = loadError!!,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Asegúrate de que existe el archivo:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                else -> {
                    GailQuestionnaireApp(
                        onSaveResponses = { responses ->
                            scope.launch(Dispatchers.IO) {
                                repository.savePatientResponses(responses)
                            }
                        }
                    )
                }
            }
        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(width = 1000.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Cuestionario Modelo Gail - Evaluación de Riesgo de Cáncer de Mama",
        state = windowState
    ) {
        GailApp()
    }
}

