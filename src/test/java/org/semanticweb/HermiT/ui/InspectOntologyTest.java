package org.semanticweb.hermit.ui;

import junit.framework.TestCase;
import org.semanticweb.owlapi.model.*;
import java.io.File;

public class InspectOntologyTest extends TestCase {

    public void testInspectHierarchy() {
        SimpleOntologyRepository repository = new SimpleOntologyRepository();
        File ontologyFile = new File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx");
        repository.loadOntology(ontologyFile);
        OWLOntology ontology = repository.getOntology();

        String[] individuals = {
                "http://purl.org/ontology/breast_cancer_recommendation#Magnetic_resonance"
        };

        for (String iriStr : individuals) {
            System.out.println("\nInspecting: " + iriStr);
            IRI iri = IRI.create(iriStr);
            OWLNamedIndividual ind = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(iri);

            // Check if it is an answer (hasAnswerValue inverse or similar, but here we
            // check if it IS an answer individual)
            // Actually, answers are usually objects of hasAnswerValue from Patient, or
            // objects of hasAnswer from Question.
            // Let's check if it is used as an object of hasAnswer

            System.out.println(" - Usage as Object in ObjectPropertyAssertions:");
            for (OWLObjectPropertyAssertionAxiom ax : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
                if (ax.getObject().equals(ind)) {
                    System.out.println(
                            "   * Property: " + ax.getProperty().asOWLObjectProperty().getIRI().getShortForm() +
                                    " | Subject: " + ax.getSubject());
                }
            }

            if (ontology.containsClassInSignature(iri)) {
                System.out.println(" - Is a Class (Punning)");
                OWLClass cls = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(iri);
                printSuperClasses(ontology, cls, "  ");
            } else {
                System.out.println(" - Is NOT a Class");
            }
        }
    }

    private void printSuperClasses(OWLOntology ontology, OWLClass cls, String indent) {
        for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSubClass(cls)) {
            OWLClassExpression superCls = ax.getSuperClass();
            System.out.println(indent + "SubClassOf: " + superCls);
            if (!superCls.isAnonymous()) {
                printSuperClasses(ontology, superCls.asOWLClass(), indent + "  ");
            }
        }
    }
}
