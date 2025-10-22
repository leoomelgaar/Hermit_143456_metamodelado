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
import org.semanticweb.hermit.ui.theme.HermitColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OntologyVerifier(viewModel: OntologyViewModel) {
    val availableOntologies by viewModel.availableOntologies.collectAsState()
    val selectedOntology by viewModel.selectedOntology.collectAsState()
    val verificationResults by viewModel.verificationResults.collectAsState()
    val isVerifying by viewModel.isVerifying.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val currentProgress by viewModel.currentProgress.collectAsState()
    val totalProgress by viewModel.totalProgress.collectAsState()
    val currentOntology by viewModel.currentOntology.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Verificador de Ontologías HermiT",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Selecciona ontologías para verificar su consistencia",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Status and controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.loadAvailableOntologies() },
                    enabled = !isVerifying
                ) {
                    Text("Recargar")
                }
                
                Button(
                    onClick = { viewModel.verifySelectedOntology() },
                    enabled = !isVerifying && selectedOntology != null
                ) {
                    Text("Verificar Seleccionada")
                }
                
                Button(
                    onClick = { viewModel.verifyAllOntologies() },
                    enabled = !isVerifying && availableOntologies.isNotEmpty()
                ) {
                    Text("Verificar Todas")
                }
                
                if (verificationResults.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearVerificationResults() },
                        enabled = !isVerifying
                    ) {
                        Text("Limpiar")
                    }
                }
            }
        }
        
        // Progress indicator
        if (isVerifying) {
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
                            text = "Progreso: $currentProgress / $totalProgress",
                            style = MaterialTheme.typography.bodySmall
                        )
                        currentOntology?.let { ontology ->
                            Text(
                                text = "Actual: ${ontology.scenario}/${ontology.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Main content
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - Available ontologies
            Card(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Ontologías Disponibles (${availableOntologies.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val groupedOntologies = availableOntologies.groupBy { it.scenario }
                        
                        groupedOntologies.forEach { (scenario, ontologies) ->
                            item {
                                ScenarioHeader(scenario, ontologies.size)
                            }
                            items(ontologies) { ontology ->
                                OntologyItem(
                                    ontology = ontology,
                                    isSelected = ontology == selectedOntology,
                                    onClick = { viewModel.selectOntology(ontology) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Right panel - Results
            Card(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Resultados de Verificación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (verificationResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Selecciona una ontología y haz clic en 'Verificar' para ver los resultados",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(verificationResults) { result ->
                                VerificationResultItem(result)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScenarioHeader(scenario: String, count: Int) {
    Text(
        text = "$scenario ($count)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun OntologyItem(
    ontology: OntologyInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            CardDefaults.outlinedCardBorder() 
        else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
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
    }
}

@Composable
fun VerificationResultItem(result: OntologyResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                result.error != null -> HermitColors.Error.copy(alpha = 0.1f)
                result.isConsistent == true -> HermitColors.Success.copy(alpha = 0.1f)
                result.isConsistent == false -> HermitColors.Error.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
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
                        text = "${result.ontologyInfo.scenario}/${result.ontologyInfo.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${result.classCount} clases, ${result.axiomCount} axiomas, ${result.duration}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                result.error != null -> HermitColors.Error
                                result.isConsistent == true -> HermitColors.Success
                                result.isConsistent == false -> HermitColors.Error
                                else -> HermitColors.OnSurfaceVariant
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            result.error != null -> "ERROR"
                            result.isConsistent == true -> "✓ CONSISTENTE"
                            result.isConsistent == false -> "✗ INCONSISTENTE"
                            else -> "? DESCONOCIDO"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Error message if present
            result.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Error: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = HermitColors.Error
                )
            }
        }
    }
}