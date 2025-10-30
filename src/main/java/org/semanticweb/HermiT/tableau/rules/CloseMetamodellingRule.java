package org.semanticweb.HermiT.tableau.rules;

import org.semanticweb.HermiT.tableau.MetamodellingRule;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.Tableau;
import org.semanticweb.HermiT.tableau.GroundDisjunction;
import org.semanticweb.HermiT.tableau.GroundDisjunctionHeader;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.Equality;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.tableau.DependencySet;

/**
 * Regla de metamodelado para manejar el cierre de relaciones entre individuos metamodelados.
 * Aplica la lógica de cierre cuando dos individuos metamodelados no tienen una relación definida.
 */
public class CloseMetamodellingRule implements MetamodellingRule {

    @Override
    public boolean apply(Tableau tableau) {
        boolean addedAny = false;
        for (Node node1 : tableau.getMetamodellingNodes()) {
            for (Node node2 : tableau.getMetamodellingNodes()) {
                boolean iterationResult = checkCloseMetamodellingRuleIteration(tableau, node1, node2);
                addedAny = addedAny || iterationResult;
            }
        }
        return addedAny;
    }

    @Override
    public String getRuleName() {
        return "Close Metamodelling Rule";
    }

    @Override
    public boolean isApplicable(Tableau tableau) {
        return tableau.isMetamodellingEnabled() && !tableau.getMetamodellingNodes().isEmpty();
    }

    /**
     * Verifica la iteración de la regla de cierre para un par específico de nodos.
     * Esta lógica fue extraída del método original checkCloseMetamodellingRuleIteration.
     */
    private boolean checkCloseMetamodellingRuleIteration(Tableau tableau, Node node0, Node node1) {
        // Obs: esta es la close-rule, no la close-meta-rule, tiene el nombre confuso

        Node node0Equivalent = node0.getCanonicalNode();
        Node node1Equivalent = node1.getCanonicalNode();

        if (!tableau.m_metamodellingManager.areDifferentIndividual(node0Equivalent, node1Equivalent) &&
            !tableau.m_metamodellingManager.areSameIndividual(node0Equivalent, node1Equivalent) &&
            !tableau.alreadyCreateDisjunction(node0Equivalent, node1Equivalent)) {

            Atom eqAtom = Atom.create(Equality.INSTANCE,
                tableau.getMapNodeIndividual().get(node0Equivalent.getNodeID()),
                tableau.getMapNodeIndividual().get(node1Equivalent.getNodeID()));
            DLPredicate equalityPredicate = eqAtom.getDLPredicate();

            Atom ineqAtom = Atom.create(Inequality.INSTANCE,
                tableau.getMapNodeIndividual().get(node0Equivalent.getNodeID()),
                tableau.getMapNodeIndividual().get(node1Equivalent.getNodeID()));
            DLPredicate inequalityPredicate = ineqAtom.getDLPredicate();

            DLPredicate[] dlPredicates = new DLPredicate[]{equalityPredicate, inequalityPredicate};
            int hashCode = 0;
            for (int disjunctIndex = 0; disjunctIndex < dlPredicates.length; ++disjunctIndex) {
                hashCode = hashCode * 7 + dlPredicates[disjunctIndex].hashCode();
            }

            GroundDisjunctionHeader gdh = new GroundDisjunctionHeader(dlPredicates, hashCode, null);
            DependencySet dependencySet = tableau.getDependencySetFactory().getActualDependencySet();
            GroundDisjunction groundDisjunction = new GroundDisjunction(tableau, gdh,
                new Node[]{node0Equivalent, node1Equivalent, node0Equivalent, node1Equivalent},
                new boolean[]{true, true}, dependencySet);

            if (!tableau.alreadyCreateDisjunction(node0Equivalent, node1Equivalent) &&
                !groundDisjunction.isSatisfied(tableau)) {
                tableau.addGroundDisjunction(groundDisjunction);
                tableau.addCreatedDisjuntcion(node0Equivalent, node1Equivalent);
                return true;
            }
        }
        return false;
    }
}
