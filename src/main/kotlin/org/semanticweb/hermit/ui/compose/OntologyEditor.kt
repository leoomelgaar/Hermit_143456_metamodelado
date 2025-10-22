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
import androidx.compose.ui.window.Dialog
import org.semanticweb.hermit.ui.OntologyViewModel
import org.semanticweb.hermit.ui.ClassNode
import org.semanticweb.hermit.ui.SubClassRelation

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
    val classHierarchy by viewModel.classHierarchy.collectAsState()
    val subClassRelations by viewModel.subClassRelations.collectAsState()
    
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
        
        // Status and actions
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
                var saveFileName by remember { mutableStateOf("") }
                var showSaveDialog by remember { mutableStateOf(false) }
                
                OutlinedButton(
                    onClick = { showSaveDialog = true }
                ) {
                    Text("Guardar")
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
            
            // Right panel - Ontology Visualization
            Card(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Tabs for different views
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
                            text = { Text("Estadísticas", style = MaterialTheme.typography.bodySmall) }
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

@Composable
fun OntologyHierarchyView(classHierarchy: List<ClassNode>, subClassRelations: List<SubClassRelation>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (classHierarchy.isNotEmpty()) {
            item {
                Text(
                    text = "Jerarquía de Clases",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(classHierarchy) { rootNode ->
                ClassHierarchyNode(rootNode, 0)
            }
        } else if (subClassRelations.isNotEmpty()) {
            item {
                Text(
                    text = "Relaciones SubClase",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(subClassRelations) { relation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = relation.subClass,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "⊆",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = relation.superClass,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay jerarquía de clases definida",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ClassHierarchyNode(node: ClassNode, depth: Int) {
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp),
            colors = CardDefaults.cardColors(
                containerColor = when (depth) {
                    0 -> MaterialTheme.colorScheme.primaryContainer
                    1 -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (depth > 0) {
                    Text(
                        text = "└─ ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (depth == 0) FontWeight.Bold else FontWeight.Medium
                )
                if (node.children.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${node.children.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        node.children.forEach { child ->
            ClassHierarchyNode(child, depth + 1)
        }
    }
}

@Composable
fun OntologyElementsView(
    classes: List<String>,
    objectProperties: List<String>,
    dataProperties: List<String>,
    individuals: List<String>,
    isLoading: Boolean = false
) {
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
        
        if (!isLoading && classes.isEmpty() && objectProperties.isEmpty() && dataProperties.isEmpty() && individuals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay elementos en la ontología",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun OntologyStatsView(
    classes: List<String>,
    objectProperties: List<String>,
    dataProperties: List<String>,
    individuals: List<String>,
    subClassRelations: List<SubClassRelation>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Estadísticas de la Ontología",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatItem("Clases", classes.size)
                    StatItem("Propiedades de Objeto", objectProperties.size)
                    StatItem("Propiedades de Datos", dataProperties.size)
                    StatItem("Individuos", individuals.size)
                    StatItem("Relaciones SubClase", subClassRelations.size)
                    
                    Divider()
                    
                    val totalElements = classes.size + objectProperties.size + dataProperties.size + individuals.size
                    StatItem("Total de Elementos", totalElements, isTotal = true)
                }
            }
        }
        
        if (classes.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Análisis de Clases",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val rootClasses = classes.filter { className ->
                            subClassRelations.none { it.subClass == className }
                        }
                        val leafClasses = classes.filter { className ->
                            subClassRelations.none { it.superClass == className }
                        }
                        
                        StatItem("Clases Raíz", rootClasses.size)
                        StatItem("Clases Hoja", leafClasses.size)
                        
                        if (subClassRelations.isNotEmpty()) {
                            val maxDepth = calculateMaxDepth(subClassRelations)
                            StatItem("Profundidad Máxima", maxDepth)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: Int, isTotal: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

fun calculateMaxDepth(relations: List<SubClassRelation>): Int {
    if (relations.isEmpty()) return 0
    
    val graph = mutableMapOf<String, MutableList<String>>()
    relations.forEach { relation ->
        graph.getOrPut(relation.superClass) { mutableListOf() }.add(relation.subClass)
    }
    
    fun dfs(node: String, visited: MutableSet<String>): Int {
        if (node in visited) return 0
        visited.add(node)
        
        val children = graph[node] ?: emptyList()
        if (children.isEmpty()) return 1
        
        return 1 + (children.maxOfOrNull { dfs(it, visited.toMutableSet()) } ?: 0)
    }
    
    val roots = relations.map { it.superClass }.toSet() - relations.map { it.subClass }.toSet()
    return if (roots.isEmpty()) 1 else roots.maxOf { dfs(it, mutableSetOf()) }
}