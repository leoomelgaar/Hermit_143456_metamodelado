package org.semanticweb.hermit.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.semanticweb.hermit.ui.theme.HermitColors

data class GailQuestion(
    val id: String,
    val text: String,
    val riskFactor: String,
    val answers: List<GailAnswer>
)

data class GailAnswer(
    val id: String,
    val text: String
)

data class GailQuestionnaireState(
    val currentQuestionIndex: Int = 0,
    val responses: Map<String, String> = emptyMap()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GailQuestionnaireApp(
    onSaveResponses: (Map<String, String>) -> Unit = {}
) {
    val questions = remember { getGailQuestions() }
    var state by remember { mutableStateOf(GailQuestionnaireState()) }
    
    val currentQuestion = questions.getOrNull(state.currentQuestionIndex)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Cuestionario Médico - Modelo de Gail") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Evaluación de Riesgo de Cáncer de Mama",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pregunta ${state.currentQuestionIndex + 1} de ${questions.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            LinearProgressIndicator(
                progress = (state.currentQuestionIndex + 1).toFloat() / questions.size.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            
            if (currentQuestion != null) {
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = currentQuestion.text,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Factor de riesgo:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currentQuestion.riskFactor,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Divider()
                            
                            Text(
                                text = "Selecciona una respuesta:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 24.dp)
                        ) {
                            items(currentQuestion.answers) { answer ->
                                val isSelected = state.responses[currentQuestion.id] == answer.id
                                GailAnswerCard(
                                    answer = answer,
                                    isSelected = isSelected,
                                    onClick = {
                                        state = state.copy(
                                            responses = state.responses + (currentQuestion.id to answer.id)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (state.currentQuestionIndex > 0) {
                            state = state.copy(currentQuestionIndex = state.currentQuestionIndex - 1)
                        }
                    },
                    enabled = state.currentQuestionIndex > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("← Anterior")
                }
                
                Button(
                    onClick = {
                        if (state.currentQuestionIndex < questions.size - 1) {
                            state = state.copy(currentQuestionIndex = state.currentQuestionIndex + 1)
                        } else {
                            onSaveResponses(state.responses)
                        }
                    },
                    enabled = state.responses.containsKey(currentQuestion?.id),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.currentQuestionIndex < questions.size - 1) "Siguiente →" else "Finalizar")
                }
            }
        }
    }
}

@Composable
fun GailAnswerCard(
    answer: GailAnswer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = answer.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun getGailQuestions(): List<GailQuestion> {
    return listOf(
        GailQuestion(
            id = "age",
            text = "¿Cuál es tu edad actual?",
            riskFactor = "Edad",
            answers = listOf(
                GailAnswer("age_under_35", "Menor de 35 años"),
                GailAnswer("age_35_39", "35 a 39 años"),
                GailAnswer("age_40_44", "40 a 44 años"),
                GailAnswer("age_45_49", "45 a 49 años"),
                GailAnswer("age_50_54", "50 a 54 años"),
                GailAnswer("age_55_59", "55 a 59 años"),
                GailAnswer("age_60_64", "60 a 64 años"),
                GailAnswer("age_65_69", "65 a 69 años"),
                GailAnswer("age_70_plus", "70 años o más")
            )
        ),
        GailQuestion(
            id = "age_first_period",
            text = "¿A qué edad tuviste tu primera menstruación?",
            riskFactor = "Historia hormonal",
            answers = listOf(
                GailAnswer("period_under_11", "Menor de 11 años"),
                GailAnswer("period_11", "11 años"),
                GailAnswer("period_12", "12 años"),
                GailAnswer("period_13", "13 años"),
                GailAnswer("period_14_plus", "14 años o más"),
                GailAnswer("period_unknown", "No recuerdo")
            )
        ),
        GailQuestion(
            id = "age_first_birth",
            text = "¿A qué edad tuviste tu primer hijo nacido vivo?",
            riskFactor = "Historia reproductiva",
            answers = listOf(
                GailAnswer("birth_no_children", "No he tenido hijos"),
                GailAnswer("birth_under_20", "Menor de 20 años"),
                GailAnswer("birth_20_24", "20 a 24 años"),
                GailAnswer("birth_25_29", "25 a 29 años"),
                GailAnswer("birth_30_plus", "30 años o más")
            )
        ),
        GailQuestion(
            id = "family_history",
            text = "¿Cuántos familiares de primer grado (madre, hermanas, hijas) han tenido cáncer de mama?",
            riskFactor = "Historia familiar",
            answers = listOf(
                GailAnswer("family_0", "Ninguno"),
                GailAnswer("family_1", "Uno"),
                GailAnswer("family_2", "Dos"),
                GailAnswer("family_3_plus", "Tres o más"),
                GailAnswer("family_unknown", "No estoy segura")
            )
        ),
        GailQuestion(
            id = "breast_biopsies",
            text = "¿Cuántas biopsias de mama te han realizado (positivas o negativas)?",
            riskFactor = "Historia médica",
            answers = listOf(
                GailAnswer("biopsy_0", "Ninguna"),
                GailAnswer("biopsy_1", "Una"),
                GailAnswer("biopsy_2_plus", "Dos o más"),
                GailAnswer("biopsy_unknown", "No estoy segura")
            )
        ),
        GailQuestion(
            id = "hyperplasia",
            text = "¿Alguna biopsia mostró hiperplasia atípica?",
            riskFactor = "Historia médica",
            answers = listOf(
                GailAnswer("hyperplasia_no", "No"),
                GailAnswer("hyperplasia_yes", "Sí"),
                GailAnswer("hyperplasia_unknown", "No lo sé"),
                GailAnswer("hyperplasia_no_biopsy", "No me han hecho biopsias")
            )
        ),
        GailQuestion(
            id = "race",
            text = "¿Cuál es tu raza/etnicidad?",
            riskFactor = "Demografía",
            answers = listOf(
                GailAnswer("race_white", "Blanca"),
                GailAnswer("race_black", "Afroamericana"),
                GailAnswer("race_hispanic", "Hispana"),
                GailAnswer("race_asian", "Asiática"),
                GailAnswer("race_other", "Otra")
            )
        )
    )
}

