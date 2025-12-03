package org.semanticweb.hermit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.semanticweb.owlapi.model.IRI
import java.io.File

class QuestionnaireViewModel {
    var uiState by mutableStateOf<QuestionnaireUiState>(QuestionnaireUiState.Loading)
        private set
        
    private val repository = SimpleOntologyRepository()
    private var baseOntologyFile: File? = null
    private var currentSessionFile: File? = null
    private var cachedHistoryInstances: List<MedicalAnswer> = emptyList()
    
    fun loadData() {
        uiState = QuestionnaireUiState.Loading
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Target ontology: Prototipo
                val protoFile = File("ontologias/EscenarioE/BreastCancerRecommendationMetamodellingPROTOTIPO.owx")
                
                // Fallback files
                val filesToTry = listOfNotNull(
                    if (protoFile.exists()) protoFile else null,
                    File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx").takeIf { it.exists() },
                    File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owl").takeIf { it.exists() }
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
                
                // Immediate Session Creation
                val sessionDir = File("ontologias/sessions")
                currentSessionFile = repository.createSessionOntology(baseOntologyFile!!, sessionDir)
                repository.loadSessionOntology(currentSessionFile!!)
                
                // Load data for Woman2 from ontology
                val models = repository.getMedicalModels()
                val patientHistory = repository.getIndividualHistory("Woman2")
                cachedHistoryInstances = repository.getPatientHistoryInstances("Woman2")
                
                withContext(Dispatchers.Main) {
                    if (models.isEmpty()) {
                        uiState = QuestionnaireUiState.Error("No se encontraron modelos médicos en la ontología ${baseOntologyFile?.name}.")
                    } else {
                        uiState = QuestionnaireUiState.ModelSelection(
                            models = models,
                            patientName = "Woman2",
                            patientHistory = patientHistory
                        )
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
    
    fun selectModel(model: MedicalModel, patientName: String) {
        // Ignore input patientName, use Woman2 or whatever was loaded
        val actualPatientName = if (uiState is QuestionnaireUiState.ModelSelection) {
             (uiState as QuestionnaireUiState.ModelSelection).patientName
        } else "Woman2"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val questions = repository.getQuestionsForModel(model.iri)
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.Questionnaire(
                        model = model,
                        questions = questions,
                        currentQuestionIndex = 0,
                        responses = emptyMap(),
                        patientName = actualPatientName,
                        availableHistoryInstances = cachedHistoryInstances
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.Error("Error al cargar preguntas: ${e.message}")
                }
            }
        }
    }
    
    fun answerQuestion(questionId: String, answerId: String, historyInstanceId: String? = null) {
        val currentState = uiState
        if (currentState is QuestionnaireUiState.Questionnaire) {
            val response = QuestionnaireResponse(answerId, historyInstanceId)
            val newResponses = currentState.responses + (questionId to response)
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
        val patientName = currentState.patientName
        
        uiState = QuestionnaireUiState.Saving
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Session and Patient creation is already done in loadData
                // We just need to add answers and run checks on the existing session
                
                // Use the patient name we initialized (e.g. woman2)
                // Note: In loadData we didn't explicitly add "woman2" individual because we assumed it exists in the ontology.
                // However, addPatientAnswer uses the name to find or create the individual.
                // So we should use the same name.
                val patientIriName = patientName
                
                // Ensure patient exists (redundant if in ontology, but safe)
                repository.addIndividual(patientIriName)
                
                responses.forEach { (questionIri, response) ->
                    val answerIri = response.answerId
                    
                    // If the answer is not an IRI (e.g., it's free text/number), handle it differently
                    if (answerIri.startsWith("http")) {
                        repository.addPatientAnswer(
                            questionIri = questionIri, 
                            answerIri = answerIri, 
                            patientIndividualName = patientIriName,
                            historyInstanceIri = response.historyInstanceId
                        )
                    } else {
                        // It's a literal value (number or string)
                        if (questionIri.endsWith("age_question") || questionIri.contains("age")) {
                             try {
                                 val ageValue = answerIri.toInt()
                                 repository.addDataPropertyAssertion(patientIriName, "age", ageValue)
                             } catch (e: NumberFormatException) {
                                 println("Could not parse age: $answerIri")
                             }
                        }
                    }
                }
                
                // Save updates
                repository.saveOntology(currentSessionFile!!)
                
                // Add Metamodeling Axioms
                repository.addMetamodelingAxioms()
                
                // Run Reasoning
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.CheckingConsistency
                }
                
                val startTime = System.currentTimeMillis()
                val isConsistent = repository.isConsistent()
                val endTime = System.currentTimeMillis()
                
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.Result(
                        isConsistent = isConsistent,
                        sessionFile = currentSessionFile!!.absolutePath,
                        timeTaken = endTime - startTime,
                        patientName = patientName
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
