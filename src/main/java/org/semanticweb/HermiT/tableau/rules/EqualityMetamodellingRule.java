package org.semanticweb.HermiT.tableau.rules;

import org.semanticweb.HermiT.tableau.MetamodellingRule;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.Tableau;
import org.semanticweb.HermiT.tableau.MetamodellingAxiomHelper;
import org.semanticweb.HermiT.tableau.DependencySet;
import org.semanticweb.owlapi.model.OWLClassExpression;
import java.util.List;

/**
 * Regla de metamodelado para manejar igualdades entre individuos metamodelados.
 * Aplica la lógica de equivalencia entre clases cuando sus individuos metamodelados son iguales.
 */
public class EqualityMetamodellingRule implements MetamodellingRule {

    @Override
    public boolean apply(Tableau tableau) {
        boolean ruleApplied = false;

        for (Node node1 : tableau.getMetamodellingNodes()) {
            for (Node node2 : tableau.getMetamodellingNodes()) {

                if (tableau.m_metamodellingManager.areSameIndividual(node1, node2)) {
                  ruleApplied = checkEqualMetamodellingRuleIteration(tableau, node1, node2);
                }
            }
        }

        return ruleApplied;
    }

    @Override
    public String getRuleName() {
        return "Equality Metamodelling Rule";
    }

    @Override
    public boolean isApplicable(Tableau tableau) {
        return tableau.isMetamodellingEnabled() && !tableau.getMetamodellingNodes().isEmpty();
    }


    private boolean checkEqualMetamodellingRuleIteration(Tableau tableau, Node node0, Node node1) {
        List<OWLClassExpression> node0Classes = MetamodellingAxiomHelper.getMetamodellingClassesByIndividual(
            tableau.getNodeToMetaIndividual().get(node0.getNodeID()),
            tableau.getPermanentDLOntology()
        );
        List<OWLClassExpression> node1Classes = MetamodellingAxiomHelper.getMetamodellingClassesByIndividual(
            tableau.getNodeToMetaIndividual().get(node1.getNodeID()),
            tableau.getPermanentDLOntology()
        );

        if (node0Classes.isEmpty() || node1Classes.isEmpty()) {
            return false;
        }

        boolean ruleApplied = false;

        for (OWLClassExpression node0Class : node0Classes) {
            for (OWLClassExpression node1Class : node1Classes) {
                if (node1Class == node0Class) {
                    break;
                }

                boolean isNode1ClassContainedInNode0Class = MetamodellingAxiomHelper
                    .containsSubClassOfAxiom(node0Class, node1Class, tableau.getPermanentDLOntology());
                boolean isNode0ClassContainedInNode1Class = MetamodellingAxiomHelper
                    .containsSubClassOfAxiom(node1Class, node0Class, tableau.getPermanentDLOntology());

                if (!isNode1ClassContainedInNode0Class || !isNode0ClassContainedInNode1Class) {
                    boolean areDisjoint = MetamodellingAxiomHelper
                        .areClassesDisjoint(node0Class, node1Class, tableau.getPermanentDLOntology(), tableau);

                    if (areDisjoint) {
                        DependencySet clashDependencySet = tableau.getDependencySetFactory().getActualDependencySet();
                        tableau.getExtensionManager().setClash(clashDependencySet);
                        ruleApplied = true;
                    } else {
                        MetamodellingAxiomHelper.addSubClassOfAxioms(node0Class, node1Class, tableau.getPermanentDLOntology(), tableau);
                        ruleApplied = true;
                    }
                }
            }
        }

        return ruleApplied;
    }
}
