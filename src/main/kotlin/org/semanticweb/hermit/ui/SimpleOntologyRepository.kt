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
import java.util.stream.Collectors
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.TimeoutCancellationException

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
        // First check if the ontology is already loaded to avoid OWLOntologyAlreadyExistsException
        val existingOntology = manager.ontologies.firstOrNull { 
             val documentIRI = manager.getOntologyDocumentIRI(it)
             documentIRI.toURI().path.endsWith(file.name)
        }
        
        if (existingOntology != null) {
            // If we want to reload, we should remove it first.
            // However, simply returning the existing one is often safer if we assume it hasn't changed on disk.
            // But to ensure "freshness" or if the file changed, we might want to reload.
            // For now, let's try to remove and reload to ensure a clean state.
            manager.removeOntology(existingOntology)
        }

        // Also catch the exception just in case there's a conflict we missed
        try {
            ontology = manager.loadOntologyFromOntologyDocument(file)
        } catch (e: OWLOntologyAlreadyExistsException) {
            // If it still exists (maybe under a different document IRI but same OntologyID), 
            // try to find it and reuse it or force remove.
            val id = e.ontologyID
            val existing = manager.ontologies.find { it.ontologyID == id }
            if (existing != null) {
                ontology = existing
            } else {
                throw e
            }
        }
        return ontology
    }

    /**
     * Creates a new session ontology file by copying the base ontology
     */
    fun createSessionOntology(baseFile: File, sessionDir: File): File {
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        val timestamp = System.currentTimeMillis()
        val sessionFile = File(sessionDir, "Session_${timestamp}.owl")
        // Using Files.copy to copy the file content
        Files.copy(baseFile.toPath(), sessionFile.toPath())
        return sessionFile
    }

    /**
     * Loads a session ontology
     */
    fun loadSessionOntology(file: File) {
        // Remove existing ontology from manager to avoid conflicts if we reload same IRI
        manager.ontologies.forEach { manager.removeOntology(it) }
        ontology = manager.loadOntologyFromOntologyDocument(file)
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
        val classIRIs = getClasses().map { it.iri }.toSet()
        val objPropIRIs = getObjectProperties().map { it.iri }.toSet()
        val dataPropIRIs = getDataProperties().map { it.iri }.toSet()
        val individualIRIs = getIndividuals().map { it.iri }.toSet()

        val classesAsIndividuals = classIRIs.intersect(individualIRIs).size
        val objPropsAsIndividuals = objPropIRIs.intersect(individualIRIs).size
        val dataPropsAsIndividuals = dataPropIRIs.intersect(individualIRIs).size
        
        val metamodelingInfo = StringBuilder()
        if (classesAsIndividuals > 0) {
            metamodelingInfo.append("\n            Clases usadas como individuos (Punning): $classesAsIndividuals")
        }
        if (objPropsAsIndividuals > 0) {
            metamodelingInfo.append("\n            Propiedades de objeto usadas como individuos (Punning): $objPropsAsIndividuals")
        }
        if (dataPropsAsIndividuals > 0) {
            metamodelingInfo.append("\n            Propiedades de datos usadas como individuos (Punning): $dataPropsAsIndividuals")
        }

        return """
            IRI: ${if (ontology.ontologyID.ontologyIRI.isPresent) ontology.ontologyID.ontologyIRI.get() else IRI.create("No IRI")}
            Clases: ${getClasses().size}
            Propiedades de objeto: ${getObjectProperties().size}
            Propiedades de datos: ${getDataProperties().size}
            Individuos: ${getIndividuals().size}
            Total axiomas: ${getAxiomCount()}$metamodelingInfo
        """.trimIndent()
    }

    /**
     * Obtiene las relaciones de subclases
     */
    fun getSubClassRelations(): List<SubClassRelation> {
        val relations = mutableListOf<SubClassRelation>()

        ontology.getAxioms(AxiomType.SUBCLASS_OF).forEach { axiom ->
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
            var classCount = 0
            var axiomCount = 0
            var classesAsIndividuals = 0
            var objPropsAsIndividuals = 0
            var dataPropsAsIndividuals = 0
            
            try {
                val tempManager = OWLManager.createOWLOntologyManager()
                tempManager.ontologyLoaderConfiguration = tempManager.ontologyLoaderConfiguration
                    .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)
                    
                val tempOntology = tempManager.loadOntology(IRI.create(ontologyInfo.file.toURI()))
                
                classCount = tempOntology.classesInSignature.size
                axiomCount = tempOntology.axiomCount
                
                // Calculate metamodeling info (Punning)
                val classIRIs = tempOntology.classesInSignature.map { it.iri }.toSet()
                val objPropIRIs = tempOntology.objectPropertiesInSignature.map { it.iri }.toSet()
                val dataPropIRIs = tempOntology.dataPropertiesInSignature.map { it.iri }.toSet()
                val individualIRIs = tempOntology.individualsInSignature.map { it.iri }.toSet()

                classesAsIndividuals = classIRIs.intersect(individualIRIs).size
                objPropsAsIndividuals = objPropIRIs.intersect(individualIRIs).size
                dataPropsAsIndividuals = dataPropIRIs.intersect(individualIRIs).size
                
            } catch (e: Exception) {
                // Si falla al cargar para obtener estadísticas, continuamos con la verificación
                // pero con estadísticas en 0. Esto maneja casos como AccountingInconsistente2
                // que fallan en el parser de OWLAPI pero queremos que HermiT intente verificarlo (y posiblemente falle por inconsistencia)
                System.err.println("Advertencia: No se pudo cargar estadísticas para ${ontologyInfo.name}: ${e.message ?: e.javaClass.simpleName}")
            }
            
            val startTime = System.currentTimeMillis()
            
            // Usar el patrón de los tests con CommandLine
            val consistent = checkConsistencyWithCommandLine(ontologyInfo.file.absolutePath)
            
            val duration = System.currentTimeMillis() - startTime
            
            OntologyResult(
                ontologyInfo = ontologyInfo,
                isConsistent = consistent,
                classCount = classCount,
                axiomCount = axiomCount,
                classesAsIndividuals = classesAsIndividuals,
                objPropsAsIndividuals = objPropsAsIndividuals,
                dataPropsAsIndividuals = dataPropsAsIndividuals,
                duration = duration,
                error = null
            )
        } catch (e: Exception) {
            // Check if the exception indicates inconsistency
            val isInconsistent = e is InconsistentOntologyException || 
                                 e.cause is InconsistentOntologyException ||
                                 e.message?.contains("InconsistentOntologyException") == true

            OntologyResult(
                ontologyInfo = ontologyInfo,
                isConsistent = if (isInconsistent) false else null,
                classCount = 0,
                axiomCount = 0,
                classesAsIndividuals = 0,
                objPropsAsIndividuals = 0,
                dataPropsAsIndividuals = 0,
                duration = 0,
                error = if (isInconsistent) null else (e.message ?: e.javaClass.simpleName)
            )
        }
    }

    /**
     * Verifica consistencia usando CommandLine como en MetamodellingTests
     * Flag -c verifica la consistencia y infiere conocimiento (necesario para detectar inconsistencias con metamodelado)
     * Flag -k solo verifica la consistencia sin inferencias (más rápido pero no detecta inconsistencias con metamodelado)
     */
    private fun checkConsistencyWithCommandLine(filePath: String): Boolean {
            val flags = arrayOf("-c", filePath)
            println("DEBUG: Running HermiT with flags: ${flags.joinToString(" ")}")
            println("DEBUG: Checking ontology file: $filePath")
            val originalOut = System.out
            val originalErr = System.err
            val baos = ByteArrayOutputStream()
            val ps = PrintStream(baos)

        return try {
            System.setOut(ps)
            System.setErr(ps)

            var inconsistentException: InconsistentOntologyException? = null
            var otherException: Exception? = null
            
            val thread = Thread {
                try {
                    CommandLine.main(flags)
                } catch (e: InconsistentOntologyException) {
                    inconsistentException = e
                } catch (e: Exception) {
                    if (e.cause is InconsistentOntologyException) {
                        inconsistentException = e.cause as InconsistentOntologyException
                    } else {
                        otherException = e
                    }
                }
            }
            
            thread.start()
            thread.join(60_000L)
            
            if (thread.isAlive) {
                thread.interrupt()
                thread.join(1000)
                return false
            }
            
            if (inconsistentException != null) {
                println("DEBUG: InconsistentOntologyException caught - ontology is INCONSISTENT")
                return false
            }
            
            if (otherException != null) {
                val output = baos.toString()
                if (output.contains("Inconsistent", ignoreCase = true) || 
                    output.contains("unsatisfiable", ignoreCase = true) ||
                    output.contains("InconsistentOntologyException", ignoreCase = true)) {
                    return false
                }
                return false
            }

            val output = baos.toString()
            println("DEBUG: CommandLine output (first 500 chars): ${output.take(500)}")
            if (output.contains("Inconsistent", ignoreCase = true) || 
                output.contains("unsatisfiable", ignoreCase = true) ||
                output.contains("InconsistentOntologyException", ignoreCase = true)) {
                println("DEBUG: Output indicates inconsistency - returning false")
                return false
            }

            println("DEBUG: No inconsistency detected in output - returning true")
            true
        } catch (e: Exception) {
            val output = baos.toString()
            if (output.contains("Inconsistent", ignoreCase = true) || 
                output.contains("unsatisfiable", ignoreCase = true) ||
                output.contains("InconsistentOntologyException", ignoreCase = true)) {
                return false
            }
            false
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }

    /**
     * Verifica la consistencia de múltiples ontologías con callback de progreso y resultado
     */
    fun checkMultipleOntologies(
        ontologies: List<OntologyInfo>,
        onProgress: (current: Int, total: Int, currentOntology: OntologyInfo) -> Unit = { _, _, _ -> },
        onResult: (OntologyResult) -> Unit = { _ -> }
    ): List<OntologyResult> {
        val results = mutableListOf<OntologyResult>()

        ontologies.forEachIndexed { index, ontology ->
            onProgress(index + 1, ontologies.size, ontology)
            val result = checkOntologyConsistency(ontology)
            results.add(result)
            onResult(result)
        }

        return results
    }

    fun getMedicalModels(): List<MedicalModel> {
        val models = mutableListOf<MedicalModel>()
        val modelClass = ontology.classesInSignature.find { it.iri.shortForm == "Model" }

        if (modelClass != null) {
            ontology.individualsInSignature.forEach { individual ->
                var isModel = false
                ontology.getAxioms(AxiomType.CLASS_ASSERTION).forEach { axiom ->
                    if (axiom.individual == individual) {
                        val classExpr = axiom.classExpression
                        if (!classExpr.isAnonymous && classExpr.asOWLClass() == modelClass) {
                            isModel = true
                        }
                    }
                }

                if (isModel) {
                    val displayName = getAnnotationValue(individual, "rdfs:label") ?: individual.iri.shortForm
                    models.add(
                        MedicalModel(
                            iri = individual.iri.toString(),
                            name = individual.iri.shortForm,
                            displayName = displayName
                        )
                    )
                }
            }
        }

        return models.sortedBy { it.displayName }
    }

    fun getQuestionsForModel(modelIri: String): List<MedicalQuestion> {
        val questions = mutableListOf<MedicalQuestion>()
        val modelIndividual = dataFactory.getOWLNamedIndividual(IRI.create(modelIri))
        val hasModelQuestionProp = ontology.objectPropertiesInSignature.find {
            it.iri.shortForm == "hasModelQuestion"
        }

        if (hasModelQuestionProp == null) return emptyList()

        ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION).forEach { axiom ->
            if (axiom.subject == modelIndividual && axiom.property == hasModelQuestionProp) {
                val questionIndividual = axiom.`object`
                if (questionIndividual.isNamed) {
                    val question = extractQuestionData(questionIndividual.asOWLNamedIndividual())
                    questions.add(question)
                }
            }
        }

        return questions
    }

    private fun extractQuestionData(questionIndividual: OWLNamedIndividual): MedicalQuestion {
        val questionText = getAnnotationValue(questionIndividual, "rdfs:label")
            ?: getAnnotationValue(questionIndividual, "rdfs:comment")
            ?: questionIndividual.iri.shortForm.replace("_", " ")

        val aboutRiskFactorProp = ontology.objectPropertiesInSignature.find {
            it.iri.shortForm == "aboutRiskFactor"
        }

        var riskFactorIri: String? = null
        var riskFactorName: String? = null

        if (aboutRiskFactorProp != null) {
            ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION).forEach { axiom ->
                if (axiom.subject == questionIndividual && axiom.property == aboutRiskFactorProp) {
                    val riskFactor = axiom.`object`
                    if (riskFactor.isNamed) {
                        riskFactorIri = riskFactor.asOWLNamedIndividual().iri.toString()
                        riskFactorName = riskFactor.asOWLNamedIndividual().iri.shortForm
                    }
                }
            }
        }

        val answers = extractAnswersForQuestion(questionIndividual)
        
        // Allow text input if no answers are defined
        val allowTextInput = answers.isEmpty()

        return MedicalQuestion(
            iri = questionIndividual.iri.toString(),
            text = questionText,
            riskFactorIri = riskFactorIri,
            riskFactorName = riskFactorName,
            answers = answers,
            allowTextInput = allowTextInput
        )
    }

    private fun extractAnswersForQuestion(questionIndividual: OWLNamedIndividual): List<MedicalAnswer> {
        val answers = mutableListOf<MedicalAnswer>()
        val hasAnswerProp = ontology.objectPropertiesInSignature.find {
            it.iri.shortForm == "hasAnswer"
        }

        if (hasAnswerProp == null) return emptyList()

        ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION).forEach { axiom ->
            if (axiom.subject == questionIndividual && axiom.property == hasAnswerProp) {
                val answerIndividual = axiom.`object`
                if (answerIndividual.isNamed) {
                    val answerText = getAnnotationValue(answerIndividual.asOWLNamedIndividual(), "rdfs:label")
                        ?: answerIndividual.asOWLNamedIndividual().iri.shortForm.replace("_", " ")

                    answers.add(
                        MedicalAnswer(
                            iri = answerIndividual.asOWLNamedIndividual().iri.toString(),
                            text = answerText
                        )
                    )
                }
            }
        }

        return answers.sortedBy { it.text }
    }

    private fun getAnnotationValue(entity: OWLNamedIndividual, annotationProperty: String): String? {
        val propIri = when (annotationProperty) {
            "rdfs:label" -> IRI.create("http://www.w3.org/2000/01/rdf-schema#label")
            "rdfs:comment" -> IRI.create("http://www.w3.org/2000/01/rdf-schema#comment")
            else -> IRI.create(annotationProperty)
        }

        val annotationProp = dataFactory.getOWLAnnotationProperty(propIri)

        var result: String? = null
        ontology.getAxioms(AxiomType.ANNOTATION_ASSERTION).forEach { axiom ->
            if (result == null && axiom.subject == entity.iri && axiom.property == annotationProp && axiom.value.isLiteral) {
                result = axiom.value.asLiteral().get().literal
            }
        }

        return result
    }

    fun addPatientAnswer(questionIri: String, answerIri: String, patientIndividualName: String = "CurrentPatient") {
        val patientIRI = IRI.create("$baseIRI#$patientIndividualName")
        var patientIndividual = ontology.individualsInSignature.find { it.iri == patientIRI }

        if (patientIndividual == null) {
            patientIndividual = dataFactory.getOWLNamedIndividual(patientIRI)
            val declaration = dataFactory.getOWLDeclarationAxiom(patientIndividual)
            manager.addAxiom(ontology, declaration)
        }

        val hasAnswerProp = ontology.objectPropertiesInSignature.find {
            it.iri.shortForm == "hasAnswerValue"
        } ?: dataFactory.getOWLObjectProperty(IRI.create("$baseIRI#hasAnswerValue"))

        val answerIndividual = dataFactory.getOWLNamedIndividual(IRI.create(answerIri))

        val existingAxioms = ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)
            .filter { axiom -> axiom.subject == patientIndividual && axiom.property == hasAnswerProp }
        existingAxioms.forEach { axiom -> manager.removeAxiom(ontology, axiom) }

        val axiom = dataFactory.getOWLObjectPropertyAssertionAxiom(
            hasAnswerProp,
            patientIndividual,
            answerIndividual
        )
        manager.addAxiom(ontology, axiom)
    }

    fun addObjectPropertyAssertion(
        subjectName: String,
        propertyName: String,
        objectName: String
    ) {
        val subjectIRI = IRI.create("$baseIRI#$subjectName")
        val propertyIRI = IRI.create("$baseIRI#$propertyName")
        val objectIRI = IRI.create("$baseIRI#$objectName")

        val subject = dataFactory.getOWLNamedIndividual(subjectIRI)
        val property = dataFactory.getOWLObjectProperty(propertyIRI)
        val obj = dataFactory.getOWLNamedIndividual(objectIRI)

        val subjectDecl = dataFactory.getOWLDeclarationAxiom(subject)
        val objDecl = dataFactory.getOWLDeclarationAxiom(obj)
        val propDecl = dataFactory.getOWLDeclarationAxiom(property)

        manager.addAxiom(ontology, subjectDecl)
        manager.addAxiom(ontology, objDecl)
        manager.addAxiom(ontology, propDecl)

        val axiom = dataFactory.getOWLObjectPropertyAssertionAxiom(property, subject, obj)
        manager.addAxiom(ontology, axiom)
    }

    fun addClassAssertion(individualName: String, className: String) {
        val individualIRI = IRI.create("$baseIRI#$individualName")
        val classIRI = IRI.create("$baseIRI#$className")

        val individual = dataFactory.getOWLNamedIndividual(individualIRI)
        val owlClass = dataFactory.getOWLClass(classIRI)

        val individualDecl = dataFactory.getOWLDeclarationAxiom(individual)
        manager.addAxiom(ontology, individualDecl)

        val axiom = dataFactory.getOWLClassAssertionAxiom(owlClass, individual)
        manager.addAxiom(ontology, axiom)
    }

    fun addDataPropertyAssertion(
        individualName: String,
        propertyName: String,
        value: Any
    ) {
        val individualIRI = IRI.create("$baseIRI#$individualName")
        val propertyIRI = IRI.create("$baseIRI#$propertyName")
        
        val individual = dataFactory.getOWLNamedIndividual(individualIRI)
        val property = dataFactory.getOWLDataProperty(propertyIRI)
        
        // Ensure declarations
        val individualDecl = dataFactory.getOWLDeclarationAxiom(individual)
        val propertyDecl = dataFactory.getOWLDeclarationAxiom(property)
        manager.addAxiom(ontology, individualDecl)
        manager.addAxiom(ontology, propertyDecl)
        
        val literal = when(value) {
            is Int -> dataFactory.getOWLLiteral(value)
            is Double -> dataFactory.getOWLLiteral(value)
            is Float -> dataFactory.getOWLLiteral(value.toDouble())
            is Boolean -> dataFactory.getOWLLiteral(value)
            else -> dataFactory.getOWLLiteral(value.toString())
        }
        
        val axiom = dataFactory.getOWLDataPropertyAssertionAxiom(property, individual, literal)
        manager.addAxiom(ontology, axiom)
    }

    /**
     * Agrega axiomas de metamodelado: ofRiskFactor(X, X) para cada X que es Clase e Individuo
     */
    fun addMetamodelingAxioms() {
        val classIRIs = getClasses().map { it.iri }.toSet()
        val individualIRIs = getIndividuals().map { it.iri }.toSet()
        
        // Encontrar entidades que son tanto clases como individuos (Punning)
        val punnedIRIs = classIRIs.intersect(individualIRIs)
        
        if (punnedIRIs.isEmpty()) {
            println("DEBUG: No se encontraron entidades con Punning para agregar axiomas de metamodelado.")
            return
        }
        
        println("DEBUG: Agregando axiomas de metamodelado para ${punnedIRIs.size} entidades...")
        
        // Obtener o crear la propiedad ofRiskFactor
        // Asumimos que está en el mismo namespace que la ontología base o usamos uno genérico si no
        val ontologyIRI = if (ontology.ontologyID.ontologyIRI.isPresent) 
            ontology.ontologyID.ontologyIRI.get() 
        else 
            IRI.create(baseIRI)
            
        val ofRiskFactorIRI = IRI.create("$ontologyIRI#ofRiskFactor")
        val ofRiskFactorProp = dataFactory.getOWLObjectProperty(ofRiskFactorIRI)
        
        // Asegurar que la propiedad esté declarada
        val propDecl = dataFactory.getOWLDeclarationAxiom(ofRiskFactorProp)
        manager.addAxiom(ontology, propDecl)
        
        var count = 0
        punnedIRIs.forEach { iri ->
            val individual = dataFactory.getOWLNamedIndividual(iri)
            
            // Crear axioma: individual ofRiskFactor individual
            // NOTA: En OWL 2 DL, esto es válido si usamos Punning. 
            // El "individual" aquí actúa como el sujeto y objeto de la propiedad.
            // La "clase" con el mismo IRI es conceptualmente lo que estamos vinculando, 
            // pero en la aserción de propiedad de objeto, usamos el individuo.
            
            val axiom = dataFactory.getOWLObjectPropertyAssertionAxiom(
                ofRiskFactorProp,
                individual,
                individual
            )
            
            manager.addAxiom(ontology, axiom)
            count++
        }
        
        println("DEBUG: Se agregaron $count axiomas 'ofRiskFactor'.")
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
    val classesAsIndividuals: Int = 0,
    val objPropsAsIndividuals: Int = 0,
    val dataPropsAsIndividuals: Int = 0,
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