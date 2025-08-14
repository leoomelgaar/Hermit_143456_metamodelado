package org.semanticweb.hermit.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * ViewModel para manejar el estado de la UI de ontologías
 */
class OntologyViewModel {
    private val repository = SimpleOntologyRepository()
    
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
    
    init {
        updateUI()
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
    }
    
    /**
     * Crea una nueva ontología
     */
    fun createNewOntology(iri: String = "http://example.org/ontology") {
        try {
            repository.createNewOntology(iri)
            updateUI()
            _statusMessage.value = "Nueva ontología creada"
            _isConsistent.value = null
        } catch (e: Exception) {
            _statusMessage.value = "Error al crear ontología: ${e.message}"
        }
    }
    
    /**
     * Carga una ontología desde archivo
     */
    fun loadOntology(file: File) {
        try {
            repository.loadOntology(file)
            updateUI()
            _statusMessage.value = "Ontología cargada: ${file.name}"
            _isConsistent.value = null
        } catch (e: Exception) {
            _statusMessage.value = "Error al cargar ontología: ${e.message}"
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
        }
    }
    
    /**
     * Limpia el mensaje de estado
     */
    fun clearStatusMessage() {
        _statusMessage.value = ""
    }
}