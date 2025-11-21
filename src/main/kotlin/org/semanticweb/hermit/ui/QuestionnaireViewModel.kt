package org.semanticweb.hermit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class QuestionnaireUiState {
    object Loading : QuestionnaireUiState()
    data class Error(val message: String) : QuestionnaireUiState()
    data class ModelSelection(val models: List<MedicalModel>) : QuestionnaireUiState()
    data class Questionnaire(
        val model: MedicalModel,
        val questions: List<MedicalQuestion>,
        val currentQuestionIndex: Int,
        val responses: Map<String, String>
    ) : QuestionnaireUiState()
    object Saving : QuestionnaireUiState()
    object CheckingConsistency : QuestionnaireUiState()
    data class Result(
        val isConsistent: Boolean,
        val sessionFile: String,
        val timeTaken: Long,
        val error: String? = null
    ) : QuestionnaireUiState()
}

class QuestionnaireViewModel {
    var uiState by mutableStateOf<QuestionnaireUiState>(QuestionnaireUiState.Loading)
        private set
        
    private val repository = SimpleOntologyRepository()
    private var baseOntologyFile: File? = null
    private var currentSessionFile: File? = null
    
    fun loadData() {
        uiState = QuestionnaireUiState.Loading
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val owxFile = File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx")
                // Check if there's an OWL version (often more reliable for parsing)
                val owlFile = File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owl")
                val altFile = File("ontologias/EscenarioE/BreastCancerRecommendationWithoutMetamodelling.owx")
                
                // Try files in order of preference
                val filesToTry = listOfNotNull(
                    if (owlFile.exists()) owlFile else null,
                    if (owxFile.exists()) owxFile else null,
                    if (altFile.exists()) altFile else null
                )
                
                var loaded = false
                var lastError: Exception? = null
                
                for (file in filesToTry) {
                    try {
                        println("Intentando cargar: ${file.name}")
                        repository.loadOntology(file)
                        baseOntologyFile = file
                        loaded = true
                        println("Exito cargando ${file.name}")
                        break
                    } catch (e: Exception) {
                        println("Fallo al cargar ${file.name}: ${e.message}")
                        e.printStackTrace()
                        lastError = e
                    }
                }
                
                if (!loaded) {
                    withContext(Dispatchers.Main) {
                        val msg = lastError?.let { "${it.javaClass.simpleName}: ${it.message}" } ?: "No se encontraron archivos válidos"
                        uiState = QuestionnaireUiState.Error("Error al cargar ontologías. Último error: $msg")
                    }
                    return@launch
                }
                
                val models = repository.getMedicalModels()
                
                withContext(Dispatchers.Main) {
                    if (models.isEmpty()) {
                        uiState = QuestionnaireUiState.Error("No se encontraron modelos médicos en la ontología ${baseOntologyFile?.name}.")
                    } else {
                        uiState = QuestionnaireUiState.ModelSelection(models)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.Error("Error inesperado: $e")
                }
                e.printStackTrace()
            }
        }
    }
    
    fun selectModel(model: MedicalModel) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val questions = repository.getQuestionsForModel(model.iri)
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.Questionnaire(
                        model = model,
                        questions = questions,
                        currentQuestionIndex = 0,
                        responses = emptyMap()
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.Error("Error al cargar preguntas: ${e.message}")
                }
            }
        }
    }
    
    fun answerQuestion(questionId: String, answerId: String) {
        val currentState = uiState
        if (currentState is QuestionnaireUiState.Questionnaire) {
            val newResponses = currentState.responses + (questionId to answerId)
            uiState = currentState.copy(responses = newResponses)
        }
    }
    
    fun nextQuestion() {
        val currentState = uiState
        if (currentState is QuestionnaireUiState.Questionnaire) {
            if (currentState.currentQuestionIndex < currentState.questions.size - 1) {
                uiState = currentState.copy(currentQuestionIndex = currentState.currentQuestionIndex + 1)
            }
        }
    }
    
    fun previousQuestion() {
        val currentState = uiState
        if (currentState is QuestionnaireUiState.Questionnaire) {
            if (currentState.currentQuestionIndex > 0) {
                uiState = currentState.copy(currentQuestionIndex = currentState.currentQuestionIndex - 1)
            }
        }
    }
    
    fun saveAndCheckConsistency() {
        val currentState = uiState
        if (currentState !is QuestionnaireUiState.Questionnaire) return
        
        val responses = currentState.responses
        
        uiState = QuestionnaireUiState.Saving
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Create Session Ontology
                val sessionDir = File("ontologias/sessions")
                currentSessionFile = repository.createSessionOntology(baseOntologyFile!!, sessionDir)
                
                // 2. Load Session Ontology to work on it
                repository.loadSessionOntology(currentSessionFile!!)
                
                // 3. Add Patient and Answers
                val patientName = "Patient_${System.currentTimeMillis()}"
                repository.addIndividual(patientName)
                
                responses.forEach { (questionIri, answerIri) ->
                    // If the answer is not an IRI (e.g., it's free text/number), handle it differently
                    if (answerIri.startsWith("http")) {
                        repository.addPatientAnswer(questionIri, answerIri, patientName)
                    } else {
                        // It's a literal value (number or string)
                        // We need to determine if it's an age, weight, etc. based on the question IRI
                        // For now, we'll try to add it as a data property assertion if possible, 
                        // or just log it if we can't map it dynamically without more ontology info.
                        // Ideally, MedicalQuestion should have metadata about the data property to use.
                        
                        // Simple heuristic based on IRI suffix
                        if (questionIri.endsWith("age_question") || questionIri.contains("age")) {
                             try {
                                 val ageValue = answerIri.toInt()
                                 repository.addDataPropertyAssertion(patientName, "age", ageValue)
                             } catch (e: NumberFormatException) {
                                 println("Could not parse age: $answerIri")
                             }
                        }
                    }
                }
                
                // 4. Save updates
                repository.saveOntology(currentSessionFile!!)
                
                // 5. Run Reasoning
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.CheckingConsistency
                }
                
                val startTime = System.currentTimeMillis()
                // Using the repository's checkConsistency which uses external process or internal hermit
                val isConsistent = repository.isConsistent()
                val endTime = System.currentTimeMillis()
                
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.Result(
                        isConsistent = isConsistent,
                        sessionFile = currentSessionFile!!.absolutePath,
                        timeTaken = endTime - startTime
                    )
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.Error("Error durante el proceso: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }
    
    fun reset() {
        loadData()
    }
}

