package org.semanticweb.HermiT.tableau.rules;

import org.semanticweb.HermiT.tableau.MetamodellingRule;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.Tableau;
import org.semanticweb.HermiT.tableau.MetamodellingAxiomHelper;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.HermiT.model.Atom;
import java.util.List;
import java.util.Map;

/**
 * Regla de metamodelado para manejar desigualdades entre individuos metamodelados.
 * Aplica la lógica de desigualdad entre clases cuando sus individuos metamodelados son diferentes.
 */
public class InequalityMetamodellingRule implements MetamodellingRule {

    @Override
    public boolean apply(Tableau tableau) {
        boolean ruleApplied = false;

        for (Map.Entry<Integer, List<Integer>> entry : tableau.m_metamodellingManager.differentIndividualsMap.entrySet()) {
            Node node1 = tableau.getNode(entry.getKey());
            for (Integer nodeId2 : entry.getValue()) {
                Node node2 = tableau.getNode(nodeId2);
                if (node1 != null && node2 != null && tableau.m_metamodellingManager.areDifferentIndividual(node1, node2) && checkInequalityMetamodellingRuleIteration(tableau, node1, node2))
                    ruleApplied = true;
            }
        }

        return ruleApplied;
    }

    @Override
    public String getRuleName() {
        return "Inequality Metamodelling Rule";
    }

    @Override
    public boolean isApplicable(Tableau tableau) {
        return tableau.isMetamodellingEnabled() && !tableau.getMetamodellingNodes().isEmpty();
    }

    private boolean checkInequalityMetamodellingRuleIteration(Tableau tableau, Node node0, Node node1) {
        List<OWLClassExpression> node0Classes = MetamodellingAxiomHelper.getMetamodellingClassesByIndividual(
            tableau.getNodeToMetaIndividual().get(node0.getNodeID()),
            tableau.getPermanentDLOntology()
        );
        List<OWLClassExpression> node1Classes = MetamodellingAxiomHelper.getMetamodellingClassesByIndividual(
            tableau.getNodeToMetaIndividual().get(node1.getNodeID()),
            tableau.getPermanentDLOntology()
        );
        boolean ruleApplied = false;

        if (!node0Classes.isEmpty() && !node1Classes.isEmpty()) {
            for (OWLClassExpression node0Class : node0Classes) {
                for (OWLClassExpression node1Class : node1Classes) {
                    if (node1Class != node0Class) {
                        Atom def0 = null;
                        if (tableau.m_metamodellingManager.inequalityMetamodellingPairs.containsKey(node1Class) &&
                            tableau.m_metamodellingManager.inequalityMetamodellingPairs.get(node1Class).containsKey(node0Class)) {
                            def0 = tableau.m_metamodellingManager.inequalityMetamodellingPairs.get(node1Class).get(node0Class);
                        }
                        if (tableau.m_metamodellingManager.inequalityMetamodellingPairs.containsKey(node0Class) &&
                            tableau.m_metamodellingManager.inequalityMetamodellingPairs.get(node0Class).containsKey(node1Class)) {
                            def0 = tableau.m_metamodellingManager.inequalityMetamodellingPairs.get(node0Class).get(node1Class);
                        }
                        if (def0 == null || (def0 != null && !tableau.containsClassAssertion(def0.getDLPredicate().toString()))) {
                            MetamodellingAxiomHelper.addInequalityMetamodellingRuleAxiom(
                                node0Class, node1Class,
                                tableau.getPermanentDLOntology(), tableau,
                                def0, tableau.m_metamodellingManager.inequalityMetamodellingPairs
                            );
                            ruleApplied = true;
                        }
                    }
                }
            }
        }
        return ruleApplied;
    }
}
