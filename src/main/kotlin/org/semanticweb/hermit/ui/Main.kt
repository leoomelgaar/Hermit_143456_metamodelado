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
import org.semanticweb.hermit.ui.theme.HermitTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val viewModel = remember { OntologyViewModel() }

    HermitTheme {
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
                            Button(
                                onClick = { viewModel.createNewOntology() }
                            ) {
                                Text("Nueva Ontología")
                            }
                        }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
            UnifiedOntologyInterface(viewModel)
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