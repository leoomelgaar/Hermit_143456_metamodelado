package org.semanticweb.hermit.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.semanticweb.hermit.ui.MedicalAnswer
import org.semanticweb.hermit.ui.MedicalModel
import org.semanticweb.hermit.ui.MedicalQuestion
import org.semanticweb.hermit.ui.QuestionnaireUiState
import org.semanticweb.hermit.ui.QuestionnaireViewModel

@Composable
fun MedicalQuestionnaireApp(viewModel: QuestionnaireViewModel) {
    val state = viewModel.uiState
    
    LaunchedEffect(Unit) {
        if (state is QuestionnaireUiState.Loading) {
            viewModel.loadData()
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (state) {
                is QuestionnaireUiState.Loading -> LoadingScreen()
                is QuestionnaireUiState.Error -> ErrorScreen(state.message, onRetry = { viewModel.loadData() })
                is QuestionnaireUiState.ModelSelection -> ModelSelectionScreen(
                    models = state.models,
                    onModelSelected = { model, name -> viewModel.selectModel(model, name) },
                    patientHistory = state.patientHistory,
                    patientDisplayName = state.patientDisplayName
                )
                is QuestionnaireUiState.Questionnaire -> QuestionnaireScreen(
                    state = state,
                    onAnswerSelected = { q, a, h -> viewModel.answerQuestion(q, a, h) },
                    onNext = { viewModel.nextQuestion() },
                    onPrevious = { viewModel.previousQuestion() },
                    onFinish = { viewModel.saveAndCheckConsistency() },
                    onBackToSelection = { viewModel.reset() }
                )
                is QuestionnaireUiState.Saving -> SavingScreen()
                is QuestionnaireUiState.CheckingConsistency -> CheckingConsistencyScreen()
                is QuestionnaireUiState.Result -> ResultScreen(
                    result = state,
                    onReset = { viewModel.reset() }
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Cargando datos...", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun SavingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Guardando sesión...", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun CheckingConsistencyScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Verificando consistencia (HermiT)...", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onRetry) {
                    Text("Reintentar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionScreen(
    models: List<MedicalModel>, 
    onModelSelected: (MedicalModel, String) -> Unit,
    patientHistory: List<Pair<String, String>>,
    patientDisplayName: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Selección de Modelo Médico") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        )
        
        Row(modifier = Modifier.fillMaxSize()) {
            // Left side: History
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Historia Médica ($patientDisplayName)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Divider()
                // Group history by key to handle multiple values (like hasHistory)
                val groupedHistory = patientHistory.groupBy { it.first }
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(groupedHistory.toList()) { (key, values) ->
                        // Skip HasType/hasType as requested
                        if (key.equals("HasType", ignoreCase = true) || key.equals("hasType", ignoreCase = true)) {
                            return@items
                        }

                        val displayKey = when(key) {
                            "hasRace" -> "Etnia"
                            "hasAge" -> "Edad"
                            "hasHistory" -> "Historial"
                            else -> key
                        }
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("$displayKey:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            
                            if (key == "hasHistory") {
                                // Render list for history
                                values.forEach { (_, value) ->
                                    Row(modifier = Modifier.padding(start = 8.dp)) {
                                        Text("• ", style = MaterialTheme.typography.bodySmall)
                                        Text(value, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            } else {
                                // Render single line (or comma separated if multiple) for others
                                val valueText = values.joinToString(", ") { it.second }
                                Text(valueText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
            
            // Right side: Models
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Seleccione un modelo para comenzar:",
                    style = MaterialTheme.typography.titleMedium
                )
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(models) { model ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onModelSelected(model, "Woman2") },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ID: ${model.name}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireScreen(
    state: QuestionnaireUiState.Questionnaire,
    onAnswerSelected: (String, String, String?) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    onBackToSelection: () -> Unit
) {
    val currentQuestion = state.questions.getOrNull(state.currentQuestionIndex)
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Cuestionario: ${state.model.displayName}") },
            navigationIcon = {
                IconButton(onClick = onBackToSelection) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver a selección")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Progress
            LinearProgressIndicator(
                progress = (state.currentQuestionIndex + 1).toFloat() / state.questions.size.toFloat(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            
            Text(
                text = "Pregunta ${state.currentQuestionIndex + 1} de ${state.questions.size}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (currentQuestion != null) {
                val currentResponse = state.responses[currentQuestion.iri]
                val currentAnswerId = currentResponse?.answerId
                val currentHistoryId = currentResponse?.historyInstanceId

                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = currentQuestion.text,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (currentQuestion.riskFactorName != null) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Factor: ${currentQuestion.riskFactorName}") }
                            )
                        }
                        
                        Divider()
                        
                        // Local state for history selection to allow selection before answering
                        // This must be defined here to be accessible in both the dropdown and the answer/text blocks
                        var localHistoryId by remember(currentQuestion.iri) { mutableStateOf(currentHistoryId) }

                        // Sync local state if global state updates (e.g. clear/reset/nav)
                        LaunchedEffect(currentHistoryId) {
                            if (currentHistoryId != null) localHistoryId = currentHistoryId
                        }

                        // History Dropdown
                        // Always show dropdown if there are available history instances
                        if (state.availableHistoryInstances.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(false) }
                            val selectedHistory = state.availableHistoryInstances.find { it.iri == localHistoryId }
                            
                            Column {
                                Text(
                                    text = "Asignar Historial (Seleccione uno):",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expanded = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(selectedHistory?.text ?: "Seleccionar historial...")
                                    }
                                    
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 300.dp) // Limit height for many items
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Ninguna") },
                                            onClick = { 
                                                expanded = false
                                                localHistoryId = null
                                                if (currentAnswerId != null) {
                                                    onAnswerSelected(currentQuestion.iri, currentAnswerId, null)
                                                }
                                            }
                                        )
                                        Divider()
                                        state.availableHistoryInstances.forEach { history ->
                                            DropdownMenuItem(
                                                text = { Text(history.text) },
                                                onClick = { 
                                                    expanded = false
                                                    localHistoryId = history.iri
                                                    if (currentAnswerId != null) {
                                                        onAnswerSelected(currentQuestion.iri, currentAnswerId, history.iri)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        if (currentQuestion.allowTextInput) {
                            var textState by remember(currentQuestion.iri) { mutableStateOf(currentAnswerId ?: "") }
                            // Use localHistoryId here too
                            val historyIdToUse = localHistoryId ?: currentHistoryId

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = textState,
                                    onValueChange = { 
                                        textState = it
                                        onAnswerSelected(currentQuestion.iri, it, historyIdToUse)
                                    },
                                    label = { Text("Ingrese su respuesta") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Text(
                                    text = "Esta pregunta permite respuesta libre (e.g., número o texto).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Use localHistoryId here too
                            val historyIdToUse = localHistoryId ?: currentHistoryId
                            
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentQuestion.answers) { answer ->
                                    val isSelected = currentAnswerId == answer.iri
                                    AnswerCard(
                                        answer = answer,
                                        isSelected = isSelected,
                                        onClick = { onAnswerSelected(currentQuestion.iri, answer.iri, historyIdToUse) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = state.currentQuestionIndex > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Anterior")
                }
                
                Button(
                    onClick = {
                        if (state.currentQuestionIndex < state.questions.size - 1) {
                            onNext()
                        } else {
                            onFinish()
                        }
                    },
                    enabled = state.responses.containsKey(currentQuestion?.iri),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.currentQuestionIndex < state.questions.size - 1) "Siguiente" else "Finalizar")
                }
            }
        }
    }
}

@Composable
fun AnswerCard(
    answer: MedicalAnswer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = answer.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ResultScreen(
    result: QuestionnaireUiState.Result, 
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp).fillMaxWidth(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = if (result.isConsistent) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (result.isConsistent) "¡CONSISTENTE!" else "¡INCONSISTENTE!",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (result.isConsistent) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = "Paciente: ${result.patientDisplayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = if (result.isConsistent) 
                        "El modelo no ha detectado contradicciones en las respuestas." 
                    else 
                        "El modelo ha detectado inconsistencias lógicas en las respuestas.",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Tiempo de razonamiento: ${result.timeTaken} ms")
                Text("Archivo guardado en:", style = MaterialTheme.typography.labelSmall)
                Text(result.sessionFile, style = MaterialTheme.typography.labelSmall)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onReset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Volver al Inicio")
                    }
                }
            }
        }
    }
}
