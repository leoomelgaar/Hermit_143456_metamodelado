package org.semanticweb.hermit.ui;

import junit.framework.TestCase;
import org.semanticweb.owlapi.model.*;
import java.util.Set;

public class MetamodelingAxiomTest extends TestCase {

    public void testAddMetamodelingAxioms() {
        SimpleOntologyRepository repository = new SimpleOntologyRepository();
        repository.createNewOntology("http://example.org/test");

        // Case 1: Punning (Class A and Individual A)
        repository.addClass("A");
        repository.addIndividual("A");

        // Case 2: No Punning (Class B, Individual C)
        repository.addClass("B");
        repository.addIndividual("C");

        // Add metamodeling axioms
        repository.addMetamodelingAxioms();

        OWLOntology ontology = repository.getOntology();

        // Verify ofRiskFactor(A, A) exists
        boolean foundA = false;
        for (OWLAxiom axiom : ontology.getAxioms()) {
            if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
                OWLObjectPropertyAssertionAxiom opa = (OWLObjectPropertyAssertionAxiom) axiom;
                String subject = opa.getSubject().toString();
                String property = opa.getProperty().asOWLObjectProperty().getIRI().getShortForm();
                String object = opa.getObject().toString();

                if (property.equals("ofRiskFactor")) {
                    System.out.println("Found axiom: " + subject + " " + property + " " + object);
                    if (subject.contains("#A") && object.contains("#A")) {
                        foundA = true;
                    }

                    // Verify B or C are not involved
                    if (subject.contains("#B") || object.contains("#B") ||
                            subject.contains("#C") || object.contains("#C")) {
                        fail("Should not have added axiom for non-punned entities: " + axiom);
                    }
                }
            }
        }

        assertTrue("Should have found ofRiskFactor(A, A)", foundA);
    }
}
