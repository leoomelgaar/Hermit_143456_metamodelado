package org.semanticweb.HermiT.ui;

import junit.framework.TestCase;
import org.semanticweb.hermit.ui.SimpleOntologyRepository;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.Set;

public class ReproduceInconsistencyTest extends TestCase {

    public void testStaticInconsistency() {
        SimpleOntologyRepository repository = new SimpleOntologyRepository();
        File ontologyFile = new File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx");

        if (!ontologyFile.exists()) {
            System.out.println("Ontology file not found: " + ontologyFile.getAbsolutePath());
            return;
        }

        repository.loadOntology(ontologyFile);
        
        // Create a session ontology to work safely
        File sessionDir = new File("ontologias/sessions");
        File sessionFile = repository.createSessionOntology(ontologyFile, sessionDir);
        repository.loadSessionOntology(sessionFile);
        
        System.err.println("DEBUG: Checking consistency BEFORE Metamodeling...");
        boolean isConsistentBefore = repository.isConsistent();
        System.err.println("DEBUG: Consistency Result BEFORE: " + isConsistentBefore);
        if (!isConsistentBefore) {
            System.err.println("FAIL: Ontology is ALREADY inconsistent!");
        }

        // Apply metamodeling axioms WITHOUT adding any patient answers yet.
        // If the ontology has structural issues (shared answers across risk factors),
        // this alone might cause inconsistency because ofRiskFactor becomes Functional.
        repository.addMetamodelingAxioms();

        // Check consistency
        boolean isConsistent = repository.isConsistent();
        System.err.println("DEBUG: Consistency Result: " + isConsistent);
        
        if (!isConsistent) {
            System.err.println("FAIL: Ontology became inconsistent just by adding metamodeling axioms!");
            
            OWLOntology ontology = repository.getOntology();
            OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
            OWLObjectProperty ofRiskFactor = df.getOWLObjectProperty(IRI.create(ontology.getOntologyID().getOntologyIRI().get() + "#ofRiskFactor"));
            
            System.err.println("DEBUG: ofRiskFactor characteristics:");
            System.err.println(" - Functional: " + ontology.getFunctionalObjectPropertyAxioms(ofRiskFactor).size());
            // ... other checks ...
            
            System.err.println("DEBUG: Super properties:");
            ontology.getObjectSubPropertyAxiomsForSubProperty(ofRiskFactor).forEach(ax -> 
                System.err.println(" - subPropertyOf: " + ax.getSuperProperty())
            );

            System.err.println("DEBUG: Inspecting ofRiskFactor usage...");
            System.err.println(" - InverseFunctional: " + ontology.getInverseFunctionalObjectPropertyAxioms(ofRiskFactor).size());
            System.err.println(" - Irreflexive: " + ontology.getIrreflexiveObjectPropertyAxioms(ofRiskFactor).size());
            System.err.println(" - Asymmetric: " + ontology.getAsymmetricObjectPropertyAxioms(ofRiskFactor).size());
            System.err.println(" - Transitive: " + ontology.getTransitiveObjectPropertyAxioms(ofRiskFactor).size());
            
            System.err.println("DEBUG: Inspecting ofRiskFactor usage...");
            for (OWLNamedIndividual ind : ontology.getIndividualsInSignature()) {
                int count = 0;
                for (OWLObjectPropertyAssertionAxiom ax : ontology.getObjectPropertyAssertionAxioms(ind)) {
                    if (ax.getProperty().equals(ofRiskFactor)) {
                        count++;
                        System.err.println(" - " + ind.getIRI().getShortForm() + " -> " + ax.getObject().asOWLNamedIndividual().getIRI().getShortForm());
                    }
                }
                if (count > 1) {
                    System.err.println("CONFLICT: Individual " + ind.getIRI().getShortForm() + " has " + count + " risk factors!");
                }
            }
        } else {
            System.err.println("SUCCESS: Ontology is statically consistent.");
        }
        
        assertTrue("Ontology should be consistent before adding patient data", isConsistent);
    }
}

