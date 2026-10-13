package org.semanticweb.hermit.ui;

import junit.framework.TestCase;
import org.semanticweb.owlapi.model.*;
import java.io.File;
import java.util.*;

public class InspectRiskFactorsTest extends TestCase {

    public void testInspectRiskFactors() {
        SimpleOntologyRepository repository = new SimpleOntologyRepository();
        File ontologyFile = new File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx");
        repository.loadOntology(ontologyFile);
        OWLOntology ontology = repository.getOntology();
        OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();

        String baseIRI = "http://purl.org/ontology/breast_cancer_recommendation";
        OWLObjectProperty aboutRiskFactor = df.getOWLObjectProperty(IRI.create(baseIRI + "#aboutRiskFactor"));

        System.out.println("Inspecting aboutRiskFactor targets:");

        Set<OWLNamedIndividual> targets = new HashSet<>();
        for (OWLAxiom ax : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
            OWLObjectPropertyAssertionAxiom opAx = (OWLObjectPropertyAssertionAxiom) ax;
            if (opAx.getProperty().equals(aboutRiskFactor)) {
                OWLIndividual object = opAx.getObject();
                System.out.println(" - Subject: " + opAx.getSubject() + " -> Object: " + object);
                if (object.isNamed()) {
                    targets.add(object.asOWLNamedIndividual());
                } else {
                    System.out.println("   (Anonymous Individual)");
                }
            }
        }

        System.out.println("\nAnalyzing " + targets.size() + " unique targets:");
        for (OWLNamedIndividual target : targets) {
            boolean isClass = ontology.containsClassInSignature(target.getIRI());
            boolean isIndividual = ontology.containsIndividualInSignature(target.getIRI());
            System.out.println(" - " + target.getIRI().getShortForm() +
                    " | Is Class: " + isClass +
                    " | Is Individual: " + isIndividual);
        }
    }
}
