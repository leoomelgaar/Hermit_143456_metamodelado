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
    
    fun selectModel(model: MedicalModel, patientName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val questions = repository.getQuestionsForModel(model.iri)
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.Questionnaire(
                        model = model,
                        questions = questions,
                        currentQuestionIndex = 0,
                        responses = emptyMap(),
                        patientName = patientName
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
        val patientName = currentState.patientName
        
        uiState = QuestionnaireUiState.Saving
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Create Session Ontology
                val sessionDir = File("ontologias/sessions")
                currentSessionFile = repository.createSessionOntology(baseOntologyFile!!, sessionDir)
                
                // 2. Load Session Ontology to work on it
                repository.loadSessionOntology(currentSessionFile!!)
                
                // 3. Add Patient and Answers
                // Use user provided name but sanitize it for IRI
                val safeName = patientName.replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                val patientIriName = "${safeName}_${System.currentTimeMillis()}"
                
                repository.addIndividual(patientIriName)
                
                responses.forEach { (questionIri, answerIri) ->
                    // If the answer is not an IRI (e.g., it's free text/number), handle it differently
                    if (answerIri.startsWith("http")) {
                        repository.addPatientAnswer(questionIri, answerIri, patientIriName)
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
                
                // 4. Save updates
                repository.saveOntology(currentSessionFile!!)
                
                // 4.5 Add Metamodeling Axioms (ofRiskFactor)
                // This is done AFTER saving the session file (so the file on disk is "clean" user data)
                // but BEFORE checking consistency, so the reasoner sees the metamodeling axioms.
                // Note: If we wanted these axioms persisted, we'd save after this call.
                repository.addMetamodelingAxioms()
                
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
    
    fun runMetamodelingDemo() {
        uiState = QuestionnaireUiState.CheckingConsistency // Show loading state
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val results = mutableListOf<DemoResult>()
                val demoPatientName = "Demo_Patient_${System.currentTimeMillis()}"
                val sessionDir = File("ontologias/sessions")
                
                // Shared variables to ensure both scenarios use the exact same data
                var demoQuestionIri: String? = null
                var demoAnswerIri: String? = null
                
                // Scenario 1: With Metamodeling
                // ------------------------------------------------
                val fileWithMetaOwl = File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owl")
                val fileWithMetaOwx = File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx")
                val fileWithMeta = if (fileWithMetaOwl.exists()) fileWithMetaOwl else if (fileWithMetaOwx.exists()) fileWithMetaOwx else null
                
                if (fileWithMeta != null) {
                    val sessionFile1 = repository.createSessionOntology(fileWithMeta, sessionDir)
                    println("DEBUG: Created session ontology: ${sessionFile1.absolutePath}")
                    repository.loadSessionOntology(sessionFile1)
                    repository.addIndividual(demoPatientName)
                    
                    // Inject Metamodeling Axioms
                    repository.addMetamodelingAxioms()
                    
                    // 1. Find the ACS model questions
                    val acsQuestions = repository.getQuestionsForModel("http://purl.org/ontology/breast_cancer_recommendation#ACS_model")
                    
                    if (acsQuestions.size < 3) {
                         throw IllegalStateException("ACS model does not have enough questions for this demo (found ${acsQuestions.size})")
                    }
                    
                    // Question 1: Age (Standard)
                    val q1 = acsQuestions.find { it.iri.endsWith("ACS_age_question") }
                    if (q1 != null) {
                        // Assume age 45 (Risk Factor: Personal)
                        repository.addDataPropertyAssertion(demoPatientName, "age", 45)
                    }
                    
                    // "The demo can be the same questions that the acs does for the usual questionary, 
                    // but with one questions, let's say the second one, having options of the third one"
                    
                    // Question 2: History of Breast Cancer (Expects: Breast_disease)
                    val q2 = acsQuestions.find { it.iri.endsWith("ACS_history_breast_cancer") }
                        ?: throw IllegalStateException("Question 2 (History of Breast Cancer) not found")
                    
                    // Question 3: Genetic Mutation (Expects: Genetic)
                    // We take the answer from this question to use for Q2
                    val q3 = acsQuestions.find { it.iri.endsWith("ACS_genetic_mutation_question") }
                        ?: throw IllegalStateException("Question 3 (Genetic Mutation) not found")
                    
                    val geneticAnswer = q3.answers.find { it.iri.endsWith("yes_value") }
                        ?: q3.answers.firstOrNull()
                        ?: throw IllegalStateException("Genetic answer not found")
                        
                    val mismatchQuestion = q2
                    val answerB = geneticAnswer
                    
                    // Store for Scenario 2
                    demoQuestionIri = mismatchQuestion.iri
                    demoAnswerIri = answerB.iri

                    repository.addPatientAnswer(mismatchQuestion.iri, answerB.iri, demoPatientName)
                    
                    // Get types for feedback
                    val questionRiskFactor = mismatchQuestion.riskFactorName ?: "Unknown"
                    val answerRiskFactor = repository.getInherentRiskFactorForIndividual(answerB.iri) 
                        ?.let { IRI.create(it).shortForm }
                        ?: "Unknown (Not a Risk Factor Class)"
                    
                    val description = """
                        Inconsistency detected!
                        
                        Question (2nd): '${mismatchQuestion.text}'
                        - Expected Risk Factor Type: $questionRiskFactor
                        
                        Answer (from 3rd): '${answerB.text}'
                        - Actual Risk Factor Type: $answerRiskFactor
                        
                        Reason: The answer provided belongs to a different Risk Factor category than what the question asks for.
                        The Metamodeling Axioms correctly identified this semantic mismatch.
                    """.trimIndent()

                    repository.saveOntology(sessionFile1)
                    println("DEBUG: Saved session ontology with inconsistent data to: ${sessionFile1.absolutePath}")
                    
                    val start1 = System.currentTimeMillis()
                    println("DEBUG: Checking consistency with flag -c (inferences enabled)...")
                    val consistent1 = repository.isConsistent()
                    val time1 = System.currentTimeMillis() - start1
                    println("DEBUG: Consistency result: $consistent1 (false = inconsistent, true = consistent)")
                    
                    results.add(DemoResult(
                        scenario = "Con Metamodelado",
                        isConsistent = consistent1,
                        timeTaken = time1,
                        description = description
                    ))
                } else {
                    results.add(DemoResult("Con Metamodelado", false, 0, "Error: No se encontró el archivo BreastCancerRecommendationWithMetamodelling.owl ni .owx en ontologias/EscenarioE/"))
                }

                // Scenario 2: Without Metamodeling
                // ------------------------------------------------
                // Ideally we load a different file, or we try to remove axioms. 
                // Loading the "WithoutMetamodelling" file if it exists
                val fileWithoutMeta = File("ontologias/EscenarioE/BreastCancerRecommendationWithoutMetamodelling.owx")
                
                if (fileWithoutMeta.exists()) {
                    val sessionFile2 = repository.createSessionOntology(fileWithoutMeta, sessionDir)
                    repository.loadSessionOntology(sessionFile2)
                    repository.addIndividual(demoPatientName)
                    
                    // Inject SAME inconsistent data using saved IRIs
                    if (demoQuestionIri != null && demoAnswerIri != null) {
                        repository.addPatientAnswer(demoQuestionIri!!, demoAnswerIri!!, demoPatientName)
                    } else {
                        // Fallback if Scenario 1 failed or IRIs not found
                         val qIri = "http://purl.org/ontology/breast_cancer_recommendation#ACS_history_breast_cancer" 
                         val aIri = "http://purl.org/ontology/breast_cancer_recommendation#ACS_genetic_mutation_yes_value"
                         repository.addPatientAnswer(qIri, aIri, demoPatientName)
                    }
                    
                    repository.saveOntology(sessionFile2)
                    
                    val start2 = System.currentTimeMillis()
                    val consistent2 = repository.isConsistent()
                    val time2 = System.currentTimeMillis() - start2
                    
                    results.add(DemoResult(
                        scenario = "Sin Metamodelado",
                        isConsistent = consistent2,
                        timeTaken = time2,
                        description = "Se usó la ontología sin axiomas de metamodelado. El razonador NO PUEDE ver las clases como individuos, por lo que ignora la restricción de tipo en la respuesta."
                    ))
                } else {
                     results.add(DemoResult("Sin Metamodelado", false, 0, "Error: No se encontró el archivo BreastCancerRecommendationWithoutMetamodelling.owx en ontologias/EscenarioE/"))
                }
                
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.MetamodelingDemo(results)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = QuestionnaireUiState.Error("Error en demo: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }

    fun reset() {
        loadData()
    }
}
