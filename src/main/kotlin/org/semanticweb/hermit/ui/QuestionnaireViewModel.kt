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
                // Target ontology: Base
                val protoFile = File("ontologias/sessions/ontologia_base.owl")
                
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
                
                val woman2DisplayName = repository.getIndividualDisplayName("Woman2") ?: "Woman2"
                
                withContext(Dispatchers.Main) {
                    if (models.isEmpty()) {
                        uiState = QuestionnaireUiState.Error("No se encontraron modelos médicos en la ontología ${baseOntologyFile?.name}.")
                    } else {
                        uiState = QuestionnaireUiState.ModelSelection(
                            models = models,
                            patientName = "Woman2",
                            patientDisplayName = woman2DisplayName,
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

        val actualPatientDisplayName = if (uiState is QuestionnaireUiState.ModelSelection) {
             (uiState as QuestionnaireUiState.ModelSelection).patientDisplayName
        } else actualPatientName

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
                        patientDisplayName = actualPatientDisplayName,
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
        println("DEBUG: answerQuestion called - Q: $questionId, A: $answerId, H: $historyInstanceId")
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
        val patientDisplayName = currentState.patientDisplayName

        println("DEBUG: Starting saveAndCheckConsistency with ${responses.size} responses")
        responses.forEach { (q, r) ->
            println("DEBUG: Response - Q: $q, A: ${r.answerId}, History: ${r.historyInstanceId}")
        }
        
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
                
                // Create a TEMPORARY ontology to hold the NEW axioms
                // This avoids modifying the repository's main loaded ontology which we use for querying
                // and allows us to serialize JUST the new stuff for appending.
                val tempManager = org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager()
                val changesOntology = tempManager.createOntology(org.semanticweb.owlapi.model.IRI.create("http://temp.org/changes"))

                responses.forEach { (questionIri, response) ->
                    val answerIri = response.answerId
                    
                    // If the answer is not an IRI (e.g., it's free text/number), handle it differently
                    if (answerIri.startsWith("http")) {
                        // Solo deja entre el history seleccionado y la respuesta
                        // Descarta las preguntas para las que no se elija historial
                        if (response.historyInstanceId != null) {
                            repository.addHistoryAnswer(
                                answerIri = answerIri, 
                                historyInstanceIri = response.historyInstanceId,
                                targetOntology = changesOntology
                            )
                        }
                    } else {
                        // Literal values (age) are skipped for the append-only session file requirement
                        // unless we also want to append data properties. 
                        // Assuming user requirement focuses on the history answers and control axioms.
                    }
                }
                
                // Add Control Axioms to the CHANGES ontology
                repository.addControlAxioms(changesOntology)
                
                // Save using the APPEND strategy
                // baseOntologyFile is the prototype
                // currentSessionFile is where we write the result
                repository.saveSessionOntologyWithAppend(
                    baseFile = baseOntologyFile!!,
                    targetFile = currentSessionFile!!,
                    newAxiomsOntology = changesOntology
                )
                
                // Now reload the session file into the repository to run consistency check on the FULL result
                repository.loadSessionOntology(currentSessionFile!!)
                
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
                        patientName = patientName,
                        patientDisplayName = patientDisplayName
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
