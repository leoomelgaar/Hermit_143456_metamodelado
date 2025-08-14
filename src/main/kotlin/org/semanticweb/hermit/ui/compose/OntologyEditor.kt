package org.semanticweb.hermit.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.semanticweb.hermit.ui.OntologyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OntologyEditor(viewModel: OntologyViewModel) {
    val classes by viewModel.classes.collectAsState()
    val objectProperties by viewModel.objectProperties.collectAsState()
    val dataProperties by viewModel.dataProperties.collectAsState()
    val individuals by viewModel.individuals.collectAsState()
    val isConsistent by viewModel.isConsistent.collectAsState()
    val ontologyInfo by viewModel.ontologyInfo.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    
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
                    text = "Editor de Ontologías HermiT",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ontologyInfo,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Status and consistency check
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
            
            Button(
                onClick = { viewModel.checkConsistency() }
            ) {
                Text("Verificar Consistencia")
            }
        }
        
        // Consistency indicator
        isConsistent?.let { consistent ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (consistent) Color.Green.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
        
        // Main content
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - Input forms
            Card(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Agregar Elementos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    ClassCreator(viewModel)
                    Divider()
                    ObjectPropertyCreator(viewModel)
                    Divider()
                    DataPropertyCreator(viewModel)
                    Divider()
                    IndividualCreator(viewModel)
                    Divider()
                    SubClassCreator(viewModel, classes)
                }
            }
            
            // Right panel - Lists
            Card(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Elementos de la Ontología",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (classes.isNotEmpty()) {
                            item {
                                SectionHeader("Clases (${classes.size})")
                            }
                            items(classes) { className ->
                                ElementItem(className)
                            }
                        }
                        
                        if (objectProperties.isNotEmpty()) {
                            item {
                                SectionHeader("Propiedades de Objeto (${objectProperties.size})")
                            }
                            items(objectProperties) { property ->
                                ElementItem(property)
                            }
                        }
                        
                        if (dataProperties.isNotEmpty()) {
                            item {
                                SectionHeader("Propiedades de Datos (${dataProperties.size})")
                            }
                            items(dataProperties) { property ->
                                ElementItem(property)
                            }
                        }
                        
                        if (individuals.isNotEmpty()) {
                            item {
                                SectionHeader("Individuos (${individuals.size})")
                            }
                            items(individuals) { individual ->
                                ElementItem(individual)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassCreator(viewModel: OntologyViewModel) {
    var className by remember { mutableStateOf("") }
    
    Column {
        Text("Nueva Clase", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = className,
                onValueChange = { className = it },
                label = { Text("Nombre de la clase") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (className.isNotBlank()) {
                            viewModel.addClass(className)
                            className = ""
                        }
                    }
                )
            )
            Button(
                onClick = {
                    viewModel.addClass(className)
                    className = ""
                },
                enabled = className.isNotBlank()
            ) {
                Text("Agregar")
            }
        }
    }
}

@Composable
fun ObjectPropertyCreator(viewModel: OntologyViewModel) {
    var propertyName by remember { mutableStateOf("") }
    
    Column {
        Text("Nueva Propiedad de Objeto", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = propertyName,
                onValueChange = { propertyName = it },
                label = { Text("Nombre de la propiedad") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (propertyName.isNotBlank()) {
                            viewModel.addObjectProperty(propertyName)
                            propertyName = ""
                        }
                    }
                )
            )
            Button(
                onClick = {
                    viewModel.addObjectProperty(propertyName)
                    propertyName = ""
                },
                enabled = propertyName.isNotBlank()
            ) {
                Text("Agregar")
            }
        }
    }
}

@Composable
fun DataPropertyCreator(viewModel: OntologyViewModel) {
    var propertyName by remember { mutableStateOf("") }
    
    Column {
        Text("Nueva Propiedad de Datos", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = propertyName,
                onValueChange = { propertyName = it },
                label = { Text("Nombre de la propiedad") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (propertyName.isNotBlank()) {
                            viewModel.addDataProperty(propertyName)
                            propertyName = ""
                        }
                    }
                )
            )
            Button(
                onClick = {
                    viewModel.addDataProperty(propertyName)
                    propertyName = ""
                },
                enabled = propertyName.isNotBlank()
            ) {
                Text("Agregar")
            }
        }
    }
}

@Composable
fun IndividualCreator(viewModel: OntologyViewModel) {
    var individualName by remember { mutableStateOf("") }
    
    Column {
        Text("Nuevo Individuo", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = individualName,
                onValueChange = { individualName = it },
                label = { Text("Nombre del individuo") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (individualName.isNotBlank()) {
                            viewModel.addIndividual(individualName)
                            individualName = ""
                        }
                    }
                )
            )
            Button(
                onClick = {
                    viewModel.addIndividual(individualName)
                    individualName = ""
                },
                enabled = individualName.isNotBlank()
            ) {
                Text("Agregar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubClassCreator(viewModel: OntologyViewModel, classes: List<String>) {
    var subClass by remember { mutableStateOf("") }
    var superClass by remember { mutableStateOf("") }
    var subClassExpanded by remember { mutableStateOf(false) }
    var superClassExpanded by remember { mutableStateOf(false) }
    
    Column {
        Text("Nueva Relación SubClase", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // SubClass dropdown
            ExposedDropdownMenuBox(
                expanded = subClassExpanded,
                onExpandedChange = { subClassExpanded = !subClassExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = subClass,
                    onValueChange = { subClass = it },
                    label = { Text("Subclase") },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = subClassExpanded,
                    onDismissRequest = { subClassExpanded = false }
                ) {
                    classes.forEach { className ->
                        DropdownMenuItem(
                            text = { Text(className) },
                            onClick = {
                                subClass = className
                                subClassExpanded = false
                            }
                        )
                    }
                }
            }
            
            // SuperClass dropdown
            ExposedDropdownMenuBox(
                expanded = superClassExpanded,
                onExpandedChange = { superClassExpanded = !superClassExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = superClass,
                    onValueChange = { superClass = it },
                    label = { Text("Superclase") },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = superClassExpanded,
                    onDismissRequest = { superClassExpanded = false }
                ) {
                    classes.forEach { className ->
                        DropdownMenuItem(
                            text = { Text(className) },
                            onClick = {
                                superClass = className
                                superClassExpanded = false
                            }
                        )
                    }
                }
            }
            
            Button(
                onClick = {
                    viewModel.addSubClassOf(subClass, superClass)
                    subClass = ""
                    superClass = ""
                },
                enabled = subClass.isNotBlank() && superClass.isNotBlank()
            ) {
                Text("Agregar")
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun ElementItem(name: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}