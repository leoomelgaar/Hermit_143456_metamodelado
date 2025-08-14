package org.semanticweb.hermit.ui

import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.*
import java.io.File

/**
 * Repository simplificado para manejar ontologías usando solo OWLAPI
 * (sin HermiT por ahora para evitar problemas de dependencias)
 */
class SimpleOntologyRepository {
    private val manager = OWLManager.createOWLOntologyManager()
    private val dataFactory = manager.owlDataFactory
    private val baseIRI = "http://example.org/ontology"
    
    var ontology: OWLOntology = manager.createOntology(IRI.create(baseIRI))
        private set
    
    /**
     * Crea una nueva ontología vacía
     */
    fun createNewOntology(iri: String = baseIRI): OWLOntology {
        ontology = manager.createOntology(IRI.create(iri))
        return ontology
    }
    
    /**
     * Carga una ontología desde archivo
     */
    fun loadOntology(file: File): OWLOntology {
        ontology = manager.loadOntologyFromOntologyDocument(file)
        return ontology
    }
    
    /**
     * Guarda la ontología actual en un archivo
     */
    fun saveOntology(file: File) {
        manager.saveOntology(ontology, IRI.create(file.toURI()))
    }
    
    /**
     * Agrega una nueva clase a la ontología
     */
    fun addClass(className: String): OWLClass {
        val classIRI = IRI.create("$baseIRI#$className")
        val owlClass = dataFactory.getOWLClass(classIRI)
        val declaration = dataFactory.getOWLDeclarationAxiom(owlClass)
        manager.addAxiom(ontology, declaration)
        return owlClass
    }
    
    /**
     * Agrega una nueva propiedad de objeto a la ontología
     */
    fun addObjectProperty(propertyName: String): OWLObjectProperty {
        val propertyIRI = IRI.create("$baseIRI#$propertyName")
        val property = dataFactory.getOWLObjectProperty(propertyIRI)
        val declaration = dataFactory.getOWLDeclarationAxiom(property)
        manager.addAxiom(ontology, declaration)
        return property
    }
    
    /**
     * Agrega una nueva propiedad de datos a la ontología
     */
    fun addDataProperty(propertyName: String): OWLDataProperty {
        val propertyIRI = IRI.create("$baseIRI#$propertyName")
        val property = dataFactory.getOWLDataProperty(propertyIRI)
        val declaration = dataFactory.getOWLDeclarationAxiom(property)
        manager.addAxiom(ontology, declaration)
        return property
    }
    
    /**
     * Agrega un nuevo individuo a la ontología
     */
    fun addIndividual(individualName: String): OWLNamedIndividual {
        val individualIRI = IRI.create("$baseIRI#$individualName")
        val individual = dataFactory.getOWLNamedIndividual(individualIRI)
        val declaration = dataFactory.getOWLDeclarationAxiom(individual)
        manager.addAxiom(ontology, declaration)
        return individual
    }
    
    /**
     * Establece una relación de subclase (A subClassOf B)
     */
    fun addSubClassOf(subClass: String, superClass: String) {
        val subIRI = IRI.create("$baseIRI#$subClass")
        val superIRI = IRI.create("$baseIRI#$superClass")
        val subOwlClass = dataFactory.getOWLClass(subIRI)
        val superOwlClass = dataFactory.getOWLClass(superIRI)
        val axiom = dataFactory.getOWLSubClassOfAxiom(subOwlClass, superOwlClass)
        manager.addAxiom(ontology, axiom)
    }
    
    /**
     * Verifica la consistencia de la ontología (simulado por ahora)
     */
    fun isConsistent(): Boolean {
        // Por ahora siempre devuelve true
        // TODO: Integrar HermiT cuando se resuelvan los problemas de dependencias
        return true
    }
    
    /**
     * Obtiene todas las clases de la ontología
     */
    fun getClasses(): Set<OWLClass> {
        return ontology.classesInSignature
    }
    
    /**
     * Obtiene todas las propiedades de objeto de la ontología
     */
    fun getObjectProperties(): Set<OWLObjectProperty> {
        return ontology.objectPropertiesInSignature
    }
    
    /**
     * Obtiene todas las propiedades de datos de la ontología
     */
    fun getDataProperties(): Set<OWLDataProperty> {
        return ontology.dataPropertiesInSignature
    }
    
    /**
     * Obtiene todos los individuos de la ontología
     */
    fun getIndividuals(): Set<OWLNamedIndividual> {
        return ontology.individualsInSignature
    }
    
    /**
     * Obtiene el número total de axiomas en la ontología
     */
    fun getAxiomCount(): Int {
        return ontology.axiomCount
    }
    
    /**
     * Obtiene información básica sobre la ontología
     */
    fun getOntologyInfo(): String {
        return """
            IRI: ${if (ontology.ontologyID.ontologyIRI.isPresent) ontology.ontologyID.ontologyIRI.get() else IRI.create("No IRI")}
            Clases: ${getClasses().size}
            Propiedades de objeto: ${getObjectProperties().size}
            Propiedades de datos: ${getDataProperties().size}
            Individuos: ${getIndividuals().size}
            Total axiomas: ${getAxiomCount()}
        """.trimIndent()
    }
}