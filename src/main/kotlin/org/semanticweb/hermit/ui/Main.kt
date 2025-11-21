package org.semanticweb.hermit.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.semanticweb.hermit.ui.compose.UnifiedOntologyInterface
import org.semanticweb.hermit.ui.compose.MedicalQuestionnaireApp
import org.semanticweb.hermit.ui.OntologyViewModel
import org.semanticweb.hermit.ui.QuestionnaireViewModel
import org.semanticweb.hermit.ui.theme.HermitTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val ontologyViewModel = remember { OntologyViewModel() }
    val questionnaireViewModel = remember { QuestionnaireViewModel() }
    var selectedTab by remember { mutableStateOf(0) }

    HermitTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
        Column {
            TopAppBar(
                title = { Text("HermiT Ontology Editor & Reasoning") },
                actions = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            // Actions cleared as requested
                        }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ejecución de HermiT") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Cuestionario Médico") }
                )
            }
            
            when (selectedTab) {
                0 -> UnifiedOntologyInterface(ontologyViewModel)
                1 -> MedicalQuestionnaireApp(questionnaireViewModel)
            }
        }
        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(width = 1400.dp, height = 900.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "HermiT Ontology Editor - Compose Desktop",
        state = windowState
    ) {
        App()
    }
}