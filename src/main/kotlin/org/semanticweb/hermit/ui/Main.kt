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
import org.semanticweb.hermit.ui.compose.OntologyEditor
import org.semanticweb.hermit.ui.compose.OntologyVerifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val viewModel = remember { OntologyViewModel() }
    var selectedTab by remember { mutableStateOf(0) }

    MaterialTheme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            // Top App Bar
            TopAppBar(
                title = { Text("HermiT Ontology Editor") },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        if (selectedTab == 0) {
                            Button(
                                onClick = { viewModel.createNewOntology() }
                            ) {
                                Text("Nueva")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Editor de Ontologías") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Verificador de Ontologías") }
                )
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> OntologyEditor(viewModel)
                1 -> OntologyVerifier(viewModel)
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