package org.semanticweb.HermiT.ui;

import org.semanticweb.hermit.ui.SimpleOntologyRepository;
import junit.framework.TestCase;
import org.semanticweb.owlapi.model.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DebugInconsistencyTest extends TestCase {

//    public void testNormalFlowConsistency() {
//        // This test attempts to reproduce the user's scenario:
//        // 1. Load the "WithMetamodelling" ontology
//        // 2. Add "normal" patient answers (correct types)
//        // 3. Add metamodeling axioms
//        // 4. Check consistency -> Should be TRUE
//
//        SimpleOntologyRepository repository = new SimpleOntologyRepository();
//        File ontologyFile = new File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx");
//
//        if (!ontologyFile.exists()) {
//            System.out.println("Ontology file not found, skipping test: " + ontologyFile.getAbsolutePath());
//            return;
//        }
//
//        repository.loadOntology(ontologyFile);
//
//        // Create a session ontology to avoid modifying the original
//        File sessionDir = new File("ontologias/sessions");
//        File sessionFile = repository.createSessionOntology(ontologyFile, sessionDir);
//        repository.loadSessionOntology(sessionFile);
//
//        String patientName = "TestPatient_Normal";
//        repository.addIndividual(patientName);
//
//        // Simulate "Normal" answers (matching types)
//        // We need to know the actual IRIs. Based on QuestionnaireViewModel and common
//        // sense:
//        // Question: IBIS_has_menopause_question (Hormonal)
//        // Answer: IBIS_has_menopause_no_value (Hormonal) -> Should be consistent
//
//        String questionIri = "http://purl.org/ontology/breast_cancer_recommendation#IBIS_has_menopause_question";
//        String answerIri = "http://purl.org/ontology/breast_cancer_recommendation#IBIS_has_menopause_no_value";
//
//        // Add the metamodeling axioms FIRST to check static consistency
//        repository.addMetamodelingAxioms();
//
//        boolean isConsistentBefore = repository.isConsistent();
//        System.out.println("Consistency result BEFORE Patient Answer: " + isConsistentBefore);
//        assertTrue("Ontology should be consistent before adding patient answer", isConsistentBefore);
//
//        repository.addPatientAnswer(answerIri, patientName);
//
//        // DEBUG: Inspect the ontology
//        OWLOntology ontology = repository.getOntology();
//        OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
//
//        OWLNamedIndividual answer = df.getOWLNamedIndividual(IRI.create(answerIri));
//        System.out.println("DEBUG: Answer Types for " + answerIri + ":");
//        for (OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(answer)) {
//            System.out.println(" - " + ax.getClassExpression());
//        }
//
//        OWLNamedIndividual question = df.getOWLNamedIndividual(IRI.create(questionIri));
//        System.out.println("DEBUG: Question Types for " + questionIri + ":");
//        for (OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(question)) {
//            System.out.println(" - " + ax.getClassExpression());
//        }
//
//        // Check ofRiskFactor axioms
//        System.out.println("DEBUG: ofRiskFactor axioms:");
//        OWLObjectProperty ofRiskFactor = df
//                .getOWLObjectProperty(IRI.create(ontology.getOntologyID().getOntologyIRI().get() + "#ofRiskFactor"));
//        for (OWLAxiom ax : ontology.getAxioms()) {
//            if (ax.toString().contains("ofRiskFactor")) {
//                System.out.println(" - " + ax);
//            }
//        }
//
//        // List ALL object properties
//        System.out.println("DEBUG: All Object Properties:");
//        for (OWLObjectProperty prop : ontology.getObjectPropertiesInSignature()) {
//            System.out.println(" - " + prop.getIRI().getShortForm());
//        }
//
//        // Check consistency
//        boolean isConsistent = repository.isConsistent();
//        System.out.println("Consistency result for Normal Flow: " + isConsistent);
//
//        // If inconsistent, we can't easily ask HermiT "why" via API without complex
//        // code,
//        // but knowing the types might help.
//
//        assertTrue("Normal flow should be consistent", isConsistent);
//    }
//
//    public void testInconsistentFlow() {
//        // This test attempts to reproduce the "Demo" scenario:
//        // 1. Load the "WithMetamodelling" ontology
//        // 2. Add "wrong" patient answers (mismatched types)
//        // 3. Add metamodeling axioms
//        // 4. Check consistency -> Should be FALSE
//
//        SimpleOntologyRepository repository = new SimpleOntologyRepository();
//        File ontologyFile = new File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx");
//
//        if (!ontologyFile.exists()) {
//            return;
//        }
//
//        repository.loadOntology(ontologyFile);
//
//        File sessionDir = new File("ontologias/sessions");
//        File sessionFile = repository.createSessionOntology(ontologyFile, sessionDir);
//        repository.loadSessionOntology(sessionFile);
//
//        String patientName = "TestPatient_Wrong";
//        repository.addIndividual(patientName);
//
//        // Question: IBIS_has_menopause_question (Hormonal)
//        // Answer: ACS_history_breast_cancer_yes_value (Family/BreastDisease) ->
//        // Mismatch
//
//        String questionIri = "http://purl.org/ontology/breast_cancer_recommendation#IBIS_has_menopause_question";
//        String badAnswerIri = "http://purl.org/ontology/breast_cancer_recommendation#ACS_history_breast_cancer_yes_value";
//
//        repository.addPatientAnswer(badAnswerIri, patientName);
//
//        repository.addMetamodelingAxioms();
//
//        // Check consistency
//        boolean isConsistent = repository.isConsistent();
//        System.out.println("Consistency result for Wrong Flow: " + isConsistent);
//
//        assertFalse("Wrong flow should be INCONSISTENT with metamodeling axioms", isConsistent);
//
//        // Verify the feedback logic helper
//        String answerRiskFactor = repository.getInherentRiskFactorForIndividual(badAnswerIri);
//        System.out.println("DEBUG: Risk Factor for Answer: " + answerRiskFactor);
//
//        if (answerRiskFactor == null) {
//            System.out.println("DEBUG: Inspecting superclasses for " + badAnswerIri);
//            OWLOntology ontology = repository.getOntology();
//            OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
//            OWLClass answerClass = df.getOWLClass(IRI.create(badAnswerIri));
//
//            for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSubClass(answerClass)) {
//                System.out.println(" - SubClassOf: " + ax.getSuperClass());
//            }
//        }
//
//        assertNotNull("Should be able to identify risk factor for answer", answerRiskFactor);
//    }
}
