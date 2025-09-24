package org.semanticweb.hermit.ui.compose

import androidx.compose.foundation.background
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
import org.semanticweb.hermit.ui.OntologyViewModel
import org.semanticweb.hermit.ui.OntologyInfo
import org.semanticweb.hermit.ui.OntologyResult
import org.semanticweb.hermit.ui.ClassNode
import org.semanticweb.hermit.ui.SubClassRelation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedOntologyInterface(viewModel: OntologyViewModel) {
    // Estados para ontologías disponibles y verificación
    val availableOntologies by viewModel.availableOntologies.collectAsState()
    val selectedOntology by viewModel.selectedOntology.collectAsState()
    val verificationResults by viewModel.verificationResults.collectAsState()
    val isVerifying by viewModel.isVerifying.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val currentProgress by viewModel.currentProgress.collectAsState()
    val totalProgress by viewModel.totalProgress.collectAsState()
    val currentOntology by viewModel.currentOntology.collectAsState()
    
    // Estados para editor de ontología seleccionada
    val classes by viewModel.classes.collectAsState()
    val objectProperties by viewModel.objectProperties.collectAsState()
    val dataProperties by viewModel.dataProperties.collectAsState()
    val individuals by viewModel.individuals.collectAsState()
    val isConsistent by viewModel.isConsistent.collectAsState()
    val ontologyInfo by viewModel.ontologyInfo.collectAsState()
    val classHierarchy by viewModel.classHierarchy.collectAsState()
    val subClassRelations by viewModel.subClassRelations.collectAsState()
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Panel izquierdo - Lista de ontologías y controles (más estrecho)
        Card(
            modifier = Modifier.weight(0.25f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header con controles
                OntologyListHeader(
                    viewModel = viewModel,
                    availableOntologies = availableOntologies,
                    selectedOntology = selectedOntology,
                    isVerifying = isVerifying
                )
                
                // Progress indicator
                if (isVerifying) {
                    ProgressSection(
                        currentProgress = currentProgress,
                        totalProgress = totalProgress,
                        currentOntology = currentOntology
                    )
                }
                
                // Lista de ontologías
                OntologyList(
                    availableOntologies = availableOntologies,
                    selectedOntology = selectedOntology,
                    verificationResults = verificationResults,
                    onOntologySelected = { viewModel.selectOntology(it) },
                    onLoadOntologyForEditing = { viewModel.loadOntologyForEditing(it) }
                )
                
                // Status message
                if (statusMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
        
        // Panel derecho - Editor/Visualización de ontología (más ancho)
        Card(
            modifier = Modifier.weight(0.75f)
        ) {
            if (selectedOntology != null && classes.isNotEmpty()) {
                // Modo edición - ontología cargada
                OntologyEditorSection(
                    viewModel = viewModel,
                    selectedOntology = selectedOntology!!,
                    classes = classes,
                    objectProperties = objectProperties,
                    dataProperties = dataProperties,
                    individuals = individuals,
                    isConsistent = isConsistent,
                    ontologyInfo = ontologyInfo,
                    classHierarchy = classHierarchy,
                    subClassRelations = subClassRelations
                )
            } else {
                // Estado inicial o ontología nueva
                EmptyEditorState(viewModel)
            }
        }
    }
}

@Composable
fun OntologyListHeader(
    viewModel: OntologyViewModel,
    availableOntologies: List<OntologyInfo>,
    selectedOntology: OntologyInfo?,
    isVerifying: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ontologías del Proyecto (${availableOntologies.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Controles de verificación
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.loadAvailableOntologies() },
                enabled = !isVerifying,
                modifier = Modifier.weight(1f)
            ) {
                Text("Recargar", style = MaterialTheme.typography.bodySmall)
            }
            
            Button(
                onClick = { viewModel.verifySelectedOntology() },
                enabled = !isVerifying && selectedOntology != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Verificar", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        Button(
            onClick = { viewModel.verifyAllOntologies() },
            enabled = !isVerifying && availableOntologies.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verificar Todas")
        }
    }
}

@Composable
fun ProgressSection(
    currentProgress: Int,
    totalProgress: Int,
    currentOntology: OntologyInfo?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (totalProgress > 0) {
            LinearProgressIndicator(
                progress = currentProgress.toFloat() / totalProgress.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$currentProgress / $totalProgress",
                    style = MaterialTheme.typography.bodySmall
                )
                currentOntology?.let { ontology ->
                    Text(
                        text = ontology.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun OntologyList(
    availableOntologies: List<OntologyInfo>,
    selectedOntology: OntologyInfo?,
    verificationResults: List<OntologyResult>,
    onOntologySelected: (OntologyInfo) -> Unit,
    onLoadOntologyForEditing: (OntologyInfo) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val groupedOntologies = availableOntologies.groupBy { it.scenario }
        
        groupedOntologies.forEach { (scenario, ontologies) ->
            item {
                Text(
                    text = "$scenario (${ontologies.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            items(ontologies) { ontology ->
                val result = verificationResults.find { it.ontologyInfo == ontology }
                OntologyListItem(
                    ontology = ontology,
                    isSelected = ontology == selectedOntology,
                    verificationResult = result,
                    onSelected = { onOntologySelected(ontology) },
                    onLoadForEditing = { onLoadOntologyForEditing(ontology) }
                )
            }
        }
    }
}

@Composable
fun OntologyListItem(
    ontology: OntologyInfo,
    isSelected: Boolean,
    verificationResult: OntologyResult?,
    onSelected: () -> Unit,
    onLoadForEditing: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ontology.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = ontology.relativePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Verification status
                verificationResult?.let { result ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = when {
                                    result.error != null -> Color.Red
                                    result.isConsistent == true -> Color.Green
                                    result.isConsistent == false -> Color.Red
                                    else -> Color.Gray
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when {
                                result.error != null -> "ERR"
                                result.isConsistent == true -> "✓"
                                result.isConsistent == false -> "✗"
                                else -> "?"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Action buttons
            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLoadForEditing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Editar", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun OntologyEditorSection(
    viewModel: OntologyViewModel,
    selectedOntology: OntologyInfo,
    classes: List<String>,
    objectProperties: List<String>,
    dataProperties: List<String>,
    individuals: List<String>,
    isConsistent: Boolean?,
    ontologyInfo: String,
    classHierarchy: List<ClassNode>,
    subClassRelations: List<SubClassRelation>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con información de la ontología
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Editando: ${selectedOntology.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedOntology.scenario,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Actions row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var saveFileName by remember { mutableStateOf("") }
                    var showSaveDialog by remember { mutableStateOf(false) }
                    var showGraphPreview by remember { mutableStateOf(false) }
                    
                    OutlinedButton(
                        onClick = { showSaveDialog = true }
                    ) {
                        Text("Guardar Como...")
                    }
                    
                    OutlinedButton(
                        onClick = { showGraphPreview = true }
                    ) {
                        Text("📊 Preview Grafo")
                    }
                    
                    Button(
                        onClick = { viewModel.checkConsistency() }
                    ) {
                        Text("Verificar Consistencia")
                    }
                    
                    // Save dialog
                    if (showSaveDialog) {
                        AlertDialog(
                            onDismissRequest = { showSaveDialog = false },
                            title = { Text("Guardar Ontología") },
                            text = {
                                OutlinedTextField(
                                    value = saveFileName,
                                    onValueChange = { saveFileName = it },
                                    label = { Text("Nombre del archivo") },
                                    placeholder = { Text("mi_ontologia") }
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (saveFileName.isNotBlank()) {
                                            viewModel.saveOntology(saveFileName)
                                            showSaveDialog = false
                                            saveFileName = ""
                                        }
                                    }
                                ) {
                                    Text("Guardar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSaveDialog = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                    
                    // Graph preview dialog
                    if (showGraphPreview) {
                        GraphPreviewDialog(
                            classes = classes,
                            objectProperties = objectProperties,
                            dataProperties = dataProperties,
                            individuals = individuals,
                            subClassRelations = subClassRelations,
                            onDismiss = { showGraphPreview = false }
                        )
                    }
                }
            }
        }
        
        // Consistency indicator
        isConsistent?.let { consistent ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (consistent) Color.Green.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (consistent) "✓ Ontología Consistente" else "✗ Ontología Inconsistente",
                        color = if (consistent) Color.Green else Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Main editor content
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left - Input forms
            Card(
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Agregar Elementos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    item { 
                        ClassCreator(viewModel)
                    }
                    item { Divider() }
                    item { 
                        ObjectPropertyCreator(viewModel)
                    }
                    item { Divider() }
                    item { 
                        DataPropertyCreator(viewModel)
                    }
                    item { Divider() }
                    item { 
                        IndividualCreator(viewModel)
                    }
                    item { Divider() }
                    item { 
                        SubClassCreator(viewModel, classes)
                    }
                }
            }
            
            // Right - Visualization
            Card(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    var selectedTab by remember { mutableStateOf(0) }
                    
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Jerarquía", style = MaterialTheme.typography.bodySmall) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Elementos", style = MaterialTheme.typography.bodySmall) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Stats", style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (selectedTab) {
                        0 -> OntologyHierarchyView(classHierarchy, subClassRelations)
                        1 -> OntologyElementsView(classes, objectProperties, dataProperties, individuals)
                        2 -> OntologyStatsView(classes, objectProperties, dataProperties, individuals, subClassRelations)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyEditorState(viewModel: OntologyViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Editor de Ontologías HermiT",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Selecciona una ontología de la lista y haz clic en 'Editar' para comenzar a modificarla, o crea una nueva ontología.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = { viewModel.createNewOntology() }
            ) {
                Text("Crear Nueva Ontología")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphPreviewDialog(
    classes: List<String>,
    objectProperties: List<String>,
    dataProperties: List<String>,
    individuals: List<String>,
    subClassRelations: List<SubClassRelation>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(0.9f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Preview del Grafo de Ontología",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Text("✕", style = MaterialTheme.typography.headlineSmall)
                }
            }
        },
        text = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                OntologyGraphView(
                    classes = classes,
                    objectProperties = objectProperties,
                    dataProperties = dataProperties,
                    individuals = individuals,
                    subClassRelations = subClassRelations
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
