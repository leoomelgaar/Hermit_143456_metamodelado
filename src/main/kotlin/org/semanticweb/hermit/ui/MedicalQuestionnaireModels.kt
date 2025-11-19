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
    val answers: List<MedicalAnswer>
)

data class MedicalAnswer(
    val iri: String,
    val text: String,
    val isSelected: Boolean = false
)

data class QuestionnaireState(
    val model: MedicalModel?,
    val questions: List<MedicalQuestion>,
    val currentQuestionIndex: Int = 0,
    val responses: Map<String, String> = emptyMap()
)





