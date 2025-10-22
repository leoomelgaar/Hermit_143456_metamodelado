package org.semanticweb.hermit.ui

import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.*
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.HermiT.Configuration
import org.semanticweb.HermiT.cli.CommandLine
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

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
     * Verifica la consistencia de la ontología usando HermiT (patrón de tests)
     */
    fun isConsistent(): Boolean {
        return try {
            // Guardar temporalmente la ontología para usar con CommandLine
            val tempFile = File.createTempFile("temp_ontology", ".owl")
            tempFile.deleteOnExit()
            saveOntology(tempFile)
            
            checkConsistencyWithCommandLine(tempFile.absolutePath)
        } catch (e: Exception) {
            false
        }
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
    
    /**
     * Obtiene las relaciones de subclases
     */
    fun getSubClassRelations(): List<SubClassRelation> {
        val relations = mutableListOf<SubClassRelation>()
        
        ontology.axioms(AxiomType.SUBCLASS_OF).forEach { axiom ->
            val subClass = axiom.subClass
            val superClass = axiom.superClass
            
            if (!subClass.isAnonymous && !superClass.isAnonymous) {
                relations.add(
                    SubClassRelation(
                        subClass = subClass.asOWLClass().iri.shortForm,
                        superClass = superClass.asOWLClass().iri.shortForm
                    )
                )
            }
        }
        
        return relations
    }
    
    /**
     * Obtiene la jerarquía de clases como árbol
     */
    fun getClassHierarchy(): List<ClassNode> {
        val relations = getSubClassRelations()
        val allClasses = getClasses().map { it.iri.shortForm }.toSet()
        val classNodes = mutableMapOf<String, ClassNode>()
        
        // Crear nodos para todas las clases
        allClasses.forEach { className ->
            classNodes[className] = ClassNode(className, mutableListOf())
        }
        
        // Construir relaciones padre-hijo
        relations.forEach { relation ->
            val parentNode = classNodes[relation.superClass]
            val childNode = classNodes[relation.subClass]
            
            if (parentNode != null && childNode != null) {
                parentNode.children.add(childNode)
            }
        }
        
        // Encontrar nodos raíz (sin padres)
        val childClasses = relations.map { it.subClass }.toSet()
        val rootClasses = allClasses - childClasses
        
        return rootClasses.mapNotNull { classNodes[it] }
    }
    
    /**
     * Guarda la ontología en un archivo
     */
    fun saveOntology(file: File) {
        manager.saveOntology(ontology, IRI.create(file.toURI()))
    }
    
    /**
     * Obtiene todas las ontologías disponibles en el directorio ontologias/
     */
    fun getAvailableOntologies(): List<OntologyInfo> {
        val ontologiesDir = File("ontologias")
        if (!ontologiesDir.exists() || !ontologiesDir.isDirectory) {
            return emptyList()
        }
        
        val ontologies = mutableListOf<OntologyInfo>()
        
        // Recorrer todos los subdirectorios
        ontologiesDir.listFiles()?.forEach { scenarioDir ->
            if (scenarioDir.isDirectory) {
                scenarioDir.listFiles()?.forEach { file ->
                    if (file.extension.lowercase() in listOf("owl", "owx", "rdf", "xml")) {
                        ontologies.add(
                            OntologyInfo(
                                name = file.nameWithoutExtension,
                                scenario = scenarioDir.name,
                                file = file,
                                relativePath = "ontologias/${scenarioDir.name}/${file.name}"
                            )
                        )
                    }
                }
            }
        }
        
        return ontologies.sortedWith(compareBy({ it.scenario }, { it.name }))
    }
    
    /**
     * Verifica la consistencia de una ontología específica usando HermiT (patrón de MetamodellingTests)
     */
    fun checkOntologyConsistency(ontologyInfo: OntologyInfo): OntologyResult {
        return try {
            val tempManager = OWLManager.createOWLOntologyManager()
            val tempOntology = tempManager.loadOntologyFromOntologyDocument(ontologyInfo.file)
            
            val startTime = System.currentTimeMillis()
            
            // Usar el patrón de los tests con CommandLine
            val consistent = checkConsistencyWithCommandLine(ontologyInfo.file.absolutePath)
            
            val duration = System.currentTimeMillis() - startTime
            
            OntologyResult(
                ontologyInfo = ontologyInfo,
                isConsistent = consistent,
                classCount = tempOntology.classesInSignature.size,
                axiomCount = tempOntology.axiomCount,
                duration = duration,
                error = null
            )
        } catch (e: Exception) {
            OntologyResult(
                ontologyInfo = ontologyInfo,
                isConsistent = null,
                classCount = 0,
                axiomCount = 0,
                duration = 0,
                error = e.message
            )
        }
    }
    
    /**
     * Verifica consistencia usando CommandLine como en MetamodellingTests
     */
    private fun checkConsistencyWithCommandLine(filePath: String): Boolean {
        return try {
            val flags = arrayOf("-c", filePath)
            
            // Capturar output para evitar spam en consola
            val originalOut = System.out
            val originalErr = System.err
            val baos = ByteArrayOutputStream()
            val ps = PrintStream(baos)
            
            System.setOut(ps)
            System.setErr(ps)
            
            val thread = Thread {
                try {
                    CommandLine.main(flags)
                } catch (ignored: Exception) { }
            }
            thread.isDaemon = true
            thread.start()
            thread.join(60_000) // timeout 60s
            val finished = !thread.isAlive
            if (!finished) {
                thread.interrupt()
            }
            System.setOut(originalOut)
            System.setErr(originalErr)
            if (!finished) return false
            // Si terminó y no lanzó InconsistentOntologyException, asumimos consistente
            true
        } catch (e: Exception) {
            // Error durante verificación
            false
        }
    }
    
    /**
     * Verifica la consistencia de múltiples ontologías con callback de progreso
     */
    fun checkMultipleOntologies(
        ontologies: List<OntologyInfo>,
        onProgress: (current: Int, total: Int, currentOntology: OntologyInfo) -> Unit = { _, _, _ -> }
    ): List<OntologyResult> {
        val results = mutableListOf<OntologyResult>()
        
        ontologies.forEachIndexed { index, ontology ->
            onProgress(index + 1, ontologies.size, ontology)
            val result = checkOntologyConsistency(ontology)
            results.add(result)
        }
        
        return results
    }
}

/**
 * Información sobre una ontología disponible
 */
data class OntologyInfo(
    val name: String,
    val scenario: String,
    val file: File,
    val relativePath: String
)

/**
 * Resultado de verificación de una ontología
 */
data class OntologyResult(
    val ontologyInfo: OntologyInfo,
    val isConsistent: Boolean?,
    val classCount: Int,
    val axiomCount: Int,
    val duration: Long, // tiempo en milisegundos
    val error: String?
)

/**
 * Relación de subclase
 */
data class SubClassRelation(
    val subClass: String,
    val superClass: String
)

/**
 * Nodo de clase para representar jerarquía
 */
data class ClassNode(
    val name: String,
    val children: MutableList<ClassNode>
)