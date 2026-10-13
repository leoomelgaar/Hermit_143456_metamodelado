package org.semanticweb.hermit.ui;

import junit.framework.TestCase;
import org.semanticweb.owlapi.model.*;
import java.io.File;
import java.util.*;

public class FindPunnedAnswersTest extends TestCase {

    public void testFindPunnedAnswers() {
        SimpleOntologyRepository repository = new SimpleOntologyRepository();
        File ontologyFile = new File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx");
        repository.loadOntology(ontologyFile);
        OWLOntology ontology = repository.getOntology();
        OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();

        String baseIRI = "http://purl.org/ontology/breast_cancer_recommendation";
        OWLObjectProperty hasAnswer = df.getOWLObjectProperty(IRI.create(baseIRI + "#hasAnswer"));

        // 1. Find Punned Entities
        Set<OWLNamedIndividual> punned = new HashSet<>();
        for (OWLNamedIndividual ind : ontology.getIndividualsInSignature()) {
            if (ontology.containsClassInSignature(ind.getIRI())) {
                punned.add(ind);
            }
        }
        System.out.println("Found " + punned.size() + " punned entities.");

        // 2. Find Answers (Objects of hasAnswer)
        Set<OWLNamedIndividual> answers = new HashSet<>();
        for (OWLAxiom ax : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
            OWLObjectPropertyAssertionAxiom opAx = (OWLObjectPropertyAssertionAxiom) ax;
            if (opAx.getProperty().equals(hasAnswer) && opAx.getObject().isNamed()) {
                answers.add(opAx.getObject().asOWLNamedIndividual());
            }
        }
        System.out.println("Found " + answers.size() + " answers.");

        // 3. Intersect
        Set<OWLNamedIndividual> punnedAnswers = new HashSet<>(punned);
        punnedAnswers.retainAll(answers);

        if (!punnedAnswers.isEmpty()) {
            System.out.println("CONFLICT FOUND! The following individuals are both Punned Classes and Answers:");
            for (OWLNamedIndividual ind : punnedAnswers) {
                System.out.println(" - " + ind.getIRI().getShortForm());
            }
        } else {
            System.out.println("No Punned Answers found.");
        }

        // 4. Check for pre-existing ofRiskFactor axioms
        OWLObjectProperty ofRiskFactor = df.getOWLObjectProperty(IRI.create(baseIRI + "#ofRiskFactor"));
        int existingCount = 0;
        for (OWLAxiom ax : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
            OWLObjectPropertyAssertionAxiom opAx = (OWLObjectPropertyAssertionAxiom) ax;
            if (opAx.getProperty().equals(ofRiskFactor)) {
                System.out.println("Pre-existing ofRiskFactor: " + ax);
                existingCount++;
            }
        }
        System.out.println("Found " + existingCount + " pre-existing ofRiskFactor axioms.");
    }
}
