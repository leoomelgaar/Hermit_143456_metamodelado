package org.semanticweb.hermit.ui;

import junit.framework.TestCase;
import org.semanticweb.owlapi.model.*;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class CheckOntologyStructureTest extends TestCase {

    public void testCheckForMultipleRiskFactors() {
        SimpleOntologyRepository repository = new SimpleOntologyRepository();
        File ontologyFile = new File("ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx");
        repository.loadOntology(ontologyFile);
        OWLOntology ontology = repository.getOntology();
        OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();

        String baseIRI = "http://purl.org/ontology/breast_cancer_recommendation";
        OWLObjectProperty hasAnswer = df.getOWLObjectProperty(IRI.create(baseIRI + "#hasAnswer"));
        OWLObjectProperty aboutRiskFactor = df.getOWLObjectProperty(IRI.create(baseIRI + "#aboutRiskFactor"));

        // Map Answer -> Set<RiskFactor>
        Map<OWLNamedIndividual, Set<OWLNamedIndividual>> answerToRisks = new HashMap<>();

        // Iterate ALL assertions, not just named individuals
        for (OWLAxiom ax : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
            OWLObjectPropertyAssertionAxiom opAx = (OWLObjectPropertyAssertionAxiom) ax;

            if (opAx.getProperty().equals(hasAnswer) && opAx.getObject().isNamed()) {
                OWLIndividual question = opAx.getSubject();
                OWLNamedIndividual answer = opAx.getObject().asOWLNamedIndividual();

                // Find Risk Factors for this Question (Named or Anonymous)
                Set<OWLNamedIndividual> risks = new HashSet<>();

                for (OWLAxiom ax2 : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
                    OWLObjectPropertyAssertionAxiom opAx2 = (OWLObjectPropertyAssertionAxiom) ax2;
                    if (opAx2.getSubject().equals(question) && opAx2.getProperty().equals(aboutRiskFactor)
                            && opAx2.getObject().isNamed()) {
                        risks.add(opAx2.getObject().asOWLNamedIndividual());
                    }
                }

                if (!risks.isEmpty()) {
                    answerToRisks.computeIfAbsent(answer, k -> new HashSet<>()).addAll(risks);
                }
            }
        }

        // Check for conflicts
        boolean foundConflict = false;
        for (Map.Entry<OWLNamedIndividual, Set<OWLNamedIndividual>> entry : answerToRisks.entrySet()) {
            if (entry.getValue().size() > 1) {
                System.out.println("CONFLICT: Answer " + entry.getKey().getIRI().getShortForm() +
                        " is associated with multiple Risk Factors: " +
                        entry.getValue().stream().map(i -> i.getIRI().getShortForm())
                                .collect(Collectors.joining(", ")));
                foundConflict = true;
            }
        }

        if (!foundConflict) {
            System.out.println("No answers with multiple risk factors found.");
        }
    }
}
