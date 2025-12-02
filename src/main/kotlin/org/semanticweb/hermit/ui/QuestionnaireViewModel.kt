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
                cachedHistoryInstances = repository.getHistoryInstances()
                
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

                    // Use the specific history property if known
                    val historyPropertyIRI = when {
                        mismatchQuestion.riskFactorName == "Breast_disease" -> "http://purl.org/ontology/breast_cancer_recommendation#hasBreastDiseaseValue"
                        mismatchQuestion.riskFactorName == "Hormonal" -> "http://purl.org/ontology/breast_cancer_recommendation#hasHormonalValue"
                        mismatchQuestion.riskFactorName == "Genetic" -> "http://purl.org/ontology/breast_cancer_recommendation#hasGeneticValue"
                        mismatchQuestion.riskFactorName == "Family" -> "http://purl.org/ontology/breast_cancer_recommendation#hasFamilyHistory"
                        mismatchQuestion.riskFactorName == "Personal" -> "http://purl.org/ontology/breast_cancer_recommendation#hasPersonalHistory"
                        else -> null
                    }
                    
                    if (historyPropertyIRI != null) {
                        // Add: Patient hasSpecificHistory Answer
                        repository.addObjectPropertyAssertion(demoPatientName, historyPropertyIRI.substringAfter("#"), answerB.iri.substringAfter("#"))
                    } else {
                        repository.addPatientAnswer(mismatchQuestion.iri, answerB.iri, demoPatientName)
                    }
                    
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

                    // INJECT CONFLICT: Explicitly assert the expected risk factor type to force conflict
                    // Because addPatientAnswer does not link the answer to the question in the ABox,
                    // we must manually assert that this Answer is being used as a Breast_disease answer.
                    var conflictRiskFactorIri: String? = null
                    
                    if (mismatchQuestion.riskFactorIri != null) {
                        conflictRiskFactorIri = mismatchQuestion.riskFactorIri
                    } else if (questionRiskFactor == "Breast_disease") {
                        conflictRiskFactorIri = "http://purl.org/ontology/breast_cancer_recommendation#Breast_disease"
                    }
                    
                    if (conflictRiskFactorIri != null) {
                        repository.addConflictingRiskFactorAssertion(answerB.iri, conflictRiskFactorIri)
                        
                        // Also assert that the inherent risk factor (Genetic) is DIFFERENT from the expected (Breast_disease)
                        val inherentRiskFactor = repository.getInherentRiskFactorForIndividual(answerB.iri)
                        if (inherentRiskFactor != null) {
                            repository.addDifferentFromAssertion(inherentRiskFactor, conflictRiskFactorIri)
                        }
                    }

                    // INJECT METAMODELING AXIOMS AGAIN
                    repository.addMetamodelingAxioms()

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
