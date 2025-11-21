package org.semanticweb.hermit.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * ViewModel para manejar el estado de la UI de ontologías
 */
class OntologyViewModel {
    private val repository = SimpleOntologyRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val expectedResults: Map<String, String> = loadExpectedResults()
    
    private fun loadExpectedResults(): Map<String, String> {
        val results = mutableMapOf<String, String>()
        try {
            val file = File("expected_consistency_results.txt")
            if (file.exists()) {
                file.readLines().forEach { line ->
                    if (line.isNotBlank() && !line.startsWith("#")) {
                        val parts = line.split("->").map { it.trim() }
                        if (parts.size == 2) {
                            results[parts[0]] = parts[1]
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Advertencia: No se pudo cargar expected_consistency_results.txt: ${e.message}")
        }
        return results
    }
    
    // Estados de la UI
    private val _classes = MutableStateFlow<List<String>>(emptyList())
    val classes: StateFlow<List<String>> = _classes.asStateFlow()
    
    private val _objectProperties = MutableStateFlow<List<String>>(emptyList())
    val objectProperties: StateFlow<List<String>> = _objectProperties.asStateFlow()
    
    private val _dataProperties = MutableStateFlow<List<String>>(emptyList())
    val dataProperties: StateFlow<List<String>> = _dataProperties.asStateFlow()
    
    private val _individuals = MutableStateFlow<List<String>>(emptyList())
    val individuals: StateFlow<List<String>> = _individuals.asStateFlow()
    
    private val _isConsistent = MutableStateFlow<Boolean?>(null)
    val isConsistent: StateFlow<Boolean?> = _isConsistent.asStateFlow()
    
    private val _ontologyInfo = MutableStateFlow("")
    val ontologyInfo: StateFlow<String> = _ontologyInfo.asStateFlow()
    
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    
    // Estados para ontologías disponibles
    private val _availableOntologies = MutableStateFlow<List<OntologyInfo>>(emptyList())
    val availableOntologies: StateFlow<List<OntologyInfo>> = _availableOntologies.asStateFlow()
    
    private val _selectedOntology = MutableStateFlow<OntologyInfo?>(null)
    val selectedOntology: StateFlow<OntologyInfo?> = _selectedOntology.asStateFlow()
    
    private val _verificationResults = MutableStateFlow<List<OntologyResult>>(emptyList())
    val verificationResults: StateFlow<List<OntologyResult>> = _verificationResults.asStateFlow()
    
    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentProgress = MutableStateFlow(0)
    val currentProgress: StateFlow<Int> = _currentProgress.asStateFlow()
    
    private val _totalProgress = MutableStateFlow(0)
    val totalProgress: StateFlow<Int> = _totalProgress.asStateFlow()
    
    private val _currentOntology = MutableStateFlow<OntologyInfo?>(null)
    val currentOntology: StateFlow<OntologyInfo?> = _currentOntology.asStateFlow()
    
    // Estados para visualización de jerarquías
    private val _classHierarchy = MutableStateFlow<List<ClassNode>>(emptyList())
    val classHierarchy: StateFlow<List<ClassNode>> = _classHierarchy.asStateFlow()
    
    private val _subClassRelations = MutableStateFlow<List<SubClassRelation>>(emptyList())
    val subClassRelations: StateFlow<List<SubClassRelation>> = _subClassRelations.asStateFlow()
    
    private val _medicalModels = MutableStateFlow<List<MedicalModel>>(emptyList())
    val medicalModels: StateFlow<List<MedicalModel>> = _medicalModels.asStateFlow()
    
    private val _selectedModel = MutableStateFlow<MedicalModel?>(null)
    val selectedModel: StateFlow<MedicalModel?> = _selectedModel.asStateFlow()
    
    private val _patientResponses = MutableStateFlow<Map<String, String>>(emptyMap())
    val patientResponses: StateFlow<Map<String, String>> = _patientResponses.asStateFlow()
    
    init {
        updateUI()
        loadAvailableOntologies()
    }
    
    /**
     * Actualiza todos los estados de la UI
     */
    private fun updateUI() {
        _classes.value = repository.getClasses().map { it.iri.shortForm }
        _objectProperties.value = repository.getObjectProperties().map { it.iri.shortForm }
        _dataProperties.value = repository.getDataProperties().map { it.iri.shortForm }
        _individuals.value = repository.getIndividuals().map { it.iri.shortForm }
        _ontologyInfo.value = repository.getOntologyInfo()
        _classHierarchy.value = repository.getClassHierarchy()
        _subClassRelations.value = repository.getSubClassRelations()
    }
    
    /**
     * Crea una nueva ontología
     */
    fun createNewOntology(iri: String = "http://example.org/ontology") {
        _isLoading.value = true
        scope.launch {
            try {
                repository.createNewOntology(iri)
                updateUI()
                _statusMessage.value = "Nueva ontología creada"
                _isConsistent.value = null
            } catch (e: Exception) {
                _statusMessage.value = "Error al crear ontología: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Carga una ontología desde archivo
     */
    fun loadOntology(file: File) {
        _isLoading.value = true
        scope.launch {
            try {
                repository.loadOntology(file)
                updateUI()
                _statusMessage.value = "Ontología cargada: ${file.name}"
                _isConsistent.value = null
            } catch (e: Exception) {
                _statusMessage.value = "Error al cargar ontología: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Guarda la ontología actual
     */
    fun saveOntology(file: File) {
        try {
            repository.saveOntology(file)
            _statusMessage.value = "Ontología guardada: ${file.name}"
        } catch (e: Exception) {
            _statusMessage.value = "Error al guardar ontología: ${e.message}"
        }
    }
    
    /**
     * Agrega una nueva clase
     */
    fun addClass(className: String) {
        if (className.isBlank()) {
            _statusMessage.value = "El nombre de la clase no puede estar vacío"
            return
        }
        
        try {
            repository.addClass(className)
            updateUI()
            _statusMessage.value = "Clase '$className' agregada"
            _isConsistent.value = null
        } catch (e: Exception) {
            _statusMessage.value = "Error al agregar clase: ${e.message}"
        }
    }
    
    /**
     * Agrega una nueva propiedad de objeto
     */
    fun addObjectProperty(propertyName: String) {
        if (propertyName.isBlank()) {
            _statusMessage.value = "El nombre de la propiedad no puede estar vacío"
            return
        }
        
        try {
            repository.addObjectProperty(propertyName)
            updateUI()
            _statusMessage.value = "Propiedad de objeto '$propertyName' agregada"
            _isConsistent.value = null
        } catch (e: Exception) {
            _statusMessage.value = "Error al agregar propiedad: ${e.message}"
        }
    }
    
    /**
     * Agrega una nueva propiedad de datos
     */
    fun addDataProperty(propertyName: String) {
        if (propertyName.isBlank()) {
            _statusMessage.value = "El nombre de la propiedad no puede estar vacío"
            return
        }
        
        try {
            repository.addDataProperty(propertyName)
            updateUI()
            _statusMessage.value = "Propiedad de datos '$propertyName' agregada"
            _isConsistent.value = null
        } catch (e: Exception) {
            _statusMessage.value = "Error al agregar propiedad: ${e.message}"
        }
    }
    
    /**
     * Agrega un nuevo individuo
     */
    fun addIndividual(individualName: String) {
        if (individualName.isBlank()) {
            _statusMessage.value = "El nombre del individuo no puede estar vacío"
            return
        }
        
        try {
            repository.addIndividual(individualName)
            updateUI()
            _statusMessage.value = "Individuo '$individualName' agregado"
            _isConsistent.value = null
        } catch (e: Exception) {
            _statusMessage.value = "Error al agregar individuo: ${e.message}"
        }
    }
    
    /**
     * Establece una relación de subclase
     */
    fun addSubClassOf(subClass: String, superClass: String) {
        if (subClass.isBlank() || superClass.isBlank()) {
            _statusMessage.value = "Los nombres de las clases no pueden estar vacíos"
            return
        }
        
        try {
            repository.addSubClassOf(subClass, superClass)
            updateUI()
            _statusMessage.value = "'$subClass' es ahora subclase de '$superClass'"
            _isConsistent.value = null
        } catch (e: Exception) {
            _statusMessage.value = "Error al agregar relación: ${e.message}"
        }
    }
    
    /**
     * Verifica la consistencia de la ontología
     */
    fun checkConsistency() {
        _isVerifying.value = true
        scope.launch {
            try {
                _statusMessage.value = "Verificando consistencia..."
                val consistent = repository.isConsistent()
                _isConsistent.value = consistent
                _statusMessage.value = if (consistent) {
                    "La ontología es consistente"
                } else {
                    "La ontología es inconsistente"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error al verificar consistencia: ${e.message}"
                _isConsistent.value = null
            } finally {
                _isVerifying.value = false
            }
        }
    }
    
    /**
     * Limpia el mensaje de estado
     */
    fun clearStatusMessage() {
        _statusMessage.value = ""
    }
    
    /**
     * Carga las ontologías disponibles del directorio
     */
    fun loadAvailableOntologies() {
        _isLoading.value = true
        scope.launch {
            try {
                val ontologies = repository.getAvailableOntologies()
                _availableOntologies.value = ontologies
                val scenarioCount = ontologies.groupBy { it.scenario }.size
                _statusMessage.value = "Cargadas ${ontologies.size} ontologías de $scenarioCount escenarios"
            } catch (e: Exception) {
                _statusMessage.value = "Error al cargar ontologías: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Selecciona una ontología específica y la carga automáticamente
     */
    fun selectOntology(ontologyInfo: OntologyInfo) {
        _selectedOntology.value = ontologyInfo
        _statusMessage.value = "Seleccionada: ${ontologyInfo.scenario}/${ontologyInfo.name}"
        _isConsistent.value = null
        
        // Cargar automáticamente para edición
        loadOntologyForEditing(ontologyInfo)
    }
    
    /**
     * Verifica la consistencia de la ontología seleccionada
     */
    fun verifySelectedOntology() {
        val selected = _selectedOntology.value
        if (selected == null) {
            _statusMessage.value = "No hay ontología seleccionada"
            return
        }
        _isVerifying.value = true
        _statusMessage.value = "Verificando ${selected.name}..."
        scope.launch {
            try {
                val result = repository.checkOntologyConsistency(selected)
                _verificationResults.value = listOf(result)
                
                val actualStatus = when {
                    result.error != null -> "ERROR"
                    result.isConsistent == true -> "CONSISTENTE"
                    result.isConsistent == false -> "INCONSISTENTE"
                    else -> "DESCONOCIDO"
                }
                
                val key = "${result.ontologyInfo.scenario}/${result.ontologyInfo.name}"
                val expectedStatus = expectedResults[key]
                
                val validationResult = when {
                    expectedStatus == null -> "⚠️  NO ESTÁ EN ARCHIVO DE REFERENCIA"
                    expectedStatus == actualStatus -> "✓ CORRECTO"
                    result.error != null -> "✗ ERROR (Esperado: $expectedStatus)"
                    else -> "✗ INCORRECTO (Esperado: $expectedStatus)"
                }
                
                println("[UI-CHECK] $key -> $actualStatus $validationResult")
                
                _statusMessage.value = when {
                    result.error != null -> "Error: ${result.error}"
                    result.isConsistent == true -> "✓ ${selected.name} es consistente"
                    result.isConsistent == false -> "✗ ${selected.name} es inconsistente"
                    else -> "? No se pudo determinar consistencia de ${selected.name}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error al verificar: ${e.message}"
            } finally {
                _isVerifying.value = false
            }
        }
    }
    
    /**
     * Verifica la consistencia de todas las ontologías con progreso individual
     */
    fun verifyAllOntologies() {
        val ontologies = _availableOntologies.value
        if (ontologies.isEmpty()) {
            _statusMessage.value = "No hay ontologías disponibles"
            return
        }
        _isVerifying.value = true
        _totalProgress.value = ontologies.size
        _currentProgress.value = 0
        _currentOntology.value = null
        _verificationResults.value = emptyList()
        _statusMessage.value = "Iniciando verificación de ${ontologies.size} ontologías..."
        scope.launch {
            try {
                // Clear previous results first or keep them? Let's clear to avoid confusion
                _verificationResults.value = emptyList()
                val currentResults = mutableListOf<OntologyResult>()
                
                val results = repository.checkMultipleOntologies(
                    ontologies = ontologies, 
                    onProgress = { current, total, currentOntology ->
                        _currentProgress.value = current
                        _totalProgress.value = total
                        _currentOntology.value = currentOntology
                        _statusMessage.value = "Verificando ($current/$total): ${currentOntology.scenario}/${currentOntology.name}"
                    },
                    onResult = { result ->
                        currentResults.add(result)
                        _verificationResults.value = currentResults.toList()
                        
                        val actualStatus = when {
                            result.error != null -> "ERROR"
                            result.isConsistent == true -> "CONSISTENTE"
                            result.isConsistent == false -> "INCONSISTENTE"
                            else -> "DESCONOCIDO"
                        }
                        
                        val key = "${result.ontologyInfo.scenario}/${result.ontologyInfo.name}"
                        val expectedStatus = expectedResults[key]
                        
                        val validationResult = when {
                            expectedStatus == null -> "⚠️  NO ESTÁ EN ARCHIVO DE REFERENCIA"
                            expectedStatus == actualStatus -> "✓ CORRECTO"
                            result.error != null -> "✗ ERROR (Esperado: $expectedStatus)"
                            else -> "✗ INCORRECTO (Esperado: $expectedStatus)"
                        }
                        
                        println("[UI-CHECK] $key -> $actualStatus $validationResult")
                    }
                )
                // Final update just in case
                _verificationResults.value = results
                val consistent = results.count { it.isConsistent == true }
                val inconsistent = results.count { it.isConsistent == false }
                val errors = results.count { it.error != null }
                val totalTime = results.sumOf { it.duration }
                _statusMessage.value = "Verificación completa: $consistent consistentes, $inconsistent inconsistentes, $errors errores (${totalTime}ms total)"
                _currentOntology.value = null
            } catch (e: Exception) {
                _statusMessage.value = "Error al verificar ontologías: ${e.message}"
            } finally {
                _isVerifying.value = false
                _currentProgress.value = 0
                _totalProgress.value = 0
            }
        }
    }
    
    /**
     * Limpia los resultados de verificación
     */
    fun clearVerificationResults() {
        _verificationResults.value = emptyList()
        _selectedOntology.value = null
        _statusMessage.value = "Resultados limpiados"
    }
    
    /**
     * Guarda la ontología actual en un archivo
     */
    fun saveOntology(fileName: String) {
        try {
            val file = File("ontologias/custom/$fileName.owl")
            file.parentFile.mkdirs()
            repository.saveOntology(file)
            _statusMessage.value = "Ontología guardada como: ${file.absolutePath}"
        } catch (e: Exception) {
            _statusMessage.value = "Error al guardar ontología: ${e.message}"
        }
    }
    
    /**
     * Carga una ontología existente para edición
     */
    fun loadOntologyForEditing(ontologyInfo: OntologyInfo) {
        _isLoading.value = true
        scope.launch {
            try {
                repository.loadOntology(ontologyInfo.file)
                updateUI()
                _statusMessage.value = "Ontología cargada para edición: ${ontologyInfo.name}"
            } catch (e: Exception) {
                _statusMessage.value = "Error al cargar ontología: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadMedicalModels() {
        scope.launch {
            try {
                val models = repository.getMedicalModels()
                _medicalModels.value = models
                _statusMessage.value = "Se encontraron ${models.size} modelos médicos"
            } catch (e: Exception) {
                _statusMessage.value = "Error al cargar modelos: ${e.message}"
            }
        }
    }
}
