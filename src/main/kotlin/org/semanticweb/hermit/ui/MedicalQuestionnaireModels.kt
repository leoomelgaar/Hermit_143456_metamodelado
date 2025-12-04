package org.semanticweb.hermit.ui

data class MedicalModel(
    val iri: String,
    val name: String,
    val displayName: String
)

data class MedicalQuestion(
    val iri: String,
    val text: String,
    val riskFactorIri: String?,
    val riskFactorName: String?,
    val answers: List<MedicalAnswer>,
    val allowTextInput: Boolean = false
)

data class MedicalAnswer(
    val iri: String,
    val text: String,
    val isSelected: Boolean = false
)

data class QuestionnaireResponse(
    val answerId: String,
    val historyInstanceId: String? = null
)

sealed class QuestionnaireUiState {
    object Loading : QuestionnaireUiState()
    data class Error(val message: String) : QuestionnaireUiState()
    data class ModelSelection(
        val models: List<MedicalModel>,
        val patientName: String = "",
        val patientDisplayName: String = "",
        val patientHistory: List<Pair<String, String>> = emptyList()
    ) : QuestionnaireUiState()
    data class Questionnaire(
        val model: MedicalModel,
        val questions: List<MedicalQuestion>,
        val currentQuestionIndex: Int,
        val responses: Map<String, QuestionnaireResponse>,
        val patientName: String,
        val patientDisplayName: String,
        val availableHistoryInstances: List<MedicalAnswer> = emptyList()
    ) : QuestionnaireUiState()
    object Saving : QuestionnaireUiState()
    object CheckingConsistency : QuestionnaireUiState()
    data class Result(
        val isConsistent: Boolean,
        val sessionFile: String,
        val timeTaken: Long,
        val error: String? = null,
        val patientName: String,
        val patientDisplayName: String
    ) : QuestionnaireUiState()
}

data class DemoResult(
    val scenario: String,
    val isConsistent: Boolean,
    val timeTaken: Long,
    val description: String
)





