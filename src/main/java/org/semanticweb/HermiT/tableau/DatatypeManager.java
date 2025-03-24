package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.datatypes.DatatypeRegistry;
import org.semanticweb.HermiT.datatypes.ValueSpaceSubset;
import org.semanticweb.HermiT.model.AtomicDataRange;
import org.semanticweb.HermiT.model.AtomicNegationDataRange;
import org.semanticweb.HermiT.model.ConstantEnumeration;
import org.semanticweb.HermiT.model.DLOntology;
import org.semanticweb.HermiT.model.DataRange;
import org.semanticweb.HermiT.model.DatatypeRestriction;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.model.InternalDatatype;
import org.semanticweb.HermiT.model.LiteralDataRange;
import org.semanticweb.HermiT.monitor.TableauMonitor;

public final class DatatypeManager
implements Serializable {
    private static final long serialVersionUID = -5304869484553471737L;
    private final InterruptFlag m_interruptFlag;
    private final TableauMonitor m_tableauMonitor;
    private final ExtensionManager m_extensionManager;
    private final ExtensionTable.Retrieval m_assertionsDeltaOldRetrieval;
    private final ExtensionTable.Retrieval m_inequalityDeltaOldRetrieval;
    private final ExtensionTable.Retrieval m_inequality01Retrieval;
    private final ExtensionTable.Retrieval m_inequality02Retrieval;
    private final ExtensionTable.Retrieval m_assertions0Retrieval;
    private final ExtensionTable.Retrieval m_assertions1Retrieval;
    private final DConjunction m_conjunction;
    private final List<DVariable> m_auxiliaryVariableList;
    private final UnionDependencySet m_unionDependencySet;
    private final boolean[] m_newVariableAdded;
    private final Set<DatatypeRestriction> m_unknownDatatypeRestrictionsPermanent;
    private Set<DatatypeRestriction> m_unknownDatatypeRestrictionsAdditional;

    public DatatypeManager(Tableau tableau) {
        this.m_interruptFlag = tableau.m_interruptFlag;
        this.m_tableauMonitor = tableau.m_tableauMonitor;
        this.m_extensionManager = tableau.m_extensionManager;
        this.m_assertionsDeltaOldRetrieval = this.m_extensionManager.getBinaryExtensionTable().createRetrieval(new boolean[]{false, false}, ExtensionTable.View.DELTA_OLD);
        this.m_inequalityDeltaOldRetrieval = this.m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{false, false, false}, ExtensionTable.View.DELTA_OLD);
        this.m_inequality01Retrieval = this.m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{true, true, false}, ExtensionTable.View.EXTENSION_THIS);
        this.m_inequality02Retrieval = this.m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{true, false, true}, ExtensionTable.View.EXTENSION_THIS);
        this.m_assertions0Retrieval = this.m_extensionManager.getBinaryExtensionTable().createRetrieval(new boolean[]{true, false}, ExtensionTable.View.EXTENSION_THIS);
        this.m_assertions1Retrieval = this.m_extensionManager.getBinaryExtensionTable().createRetrieval(new boolean[]{false, true}, ExtensionTable.View.EXTENSION_THIS);
        this.m_conjunction = new DConjunction();
        this.m_auxiliaryVariableList = new ArrayList<DVariable>();
        this.m_unionDependencySet = new UnionDependencySet(16);
        this.m_newVariableAdded = new boolean[1];
        this.m_unknownDatatypeRestrictionsPermanent = tableau.m_permanentDLOntology.getAllUnknownDatatypeRestrictions();
        if (tableau.m_additionalDLOntology != null) {
            this.additionalDLOntologySet(tableau.m_additionalDLOntology);
        }
    }

    public void additionalDLOntologySet(DLOntology additionalDLOntology) {
        this.m_unknownDatatypeRestrictionsAdditional = additionalDLOntology.getAllUnknownDatatypeRestrictions();
    }

    public void additionalDLOntologyCleared() {
        this.m_unknownDatatypeRestrictionsAdditional = null;
    }

    public void clear() {
        this.m_assertionsDeltaOldRetrieval.clear();
        this.m_inequalityDeltaOldRetrieval.clear();
        this.m_inequality01Retrieval.clear();
        this.m_inequality02Retrieval.clear();
        this.m_assertions0Retrieval.clear();
        this.m_assertions1Retrieval.clear();
        this.m_conjunction.clear();
        this.m_auxiliaryVariableList.clear();
        this.m_unionDependencySet.clearConstituents();
    }

    public void applyUnknownDatatypeRestrictionSemantics() {
        Object[] tupleBuffer = this.m_assertionsDeltaOldRetrieval.getTupleBuffer();
        this.m_assertionsDeltaOldRetrieval.open();
        while (!this.m_extensionManager.containsClash() && !this.m_assertionsDeltaOldRetrieval.afterLast()) {
            AtomicNegationDataRange negationDataRange;
            AtomicDataRange negatedDataRange;
            DatatypeRestriction datatypeRestriction;
            Object dataRangeObject = tupleBuffer[0];
            if (dataRangeObject instanceof DatatypeRestriction) {
                DatatypeRestriction datatypeRestriction2 = (DatatypeRestriction)dataRangeObject;
                if (this.m_unknownDatatypeRestrictionsPermanent.contains(datatypeRestriction2) || this.m_unknownDatatypeRestrictionsAdditional != null && this.m_unknownDatatypeRestrictionsAdditional.contains(datatypeRestriction2)) {
                    this.generateInequalitiesFor(datatypeRestriction2, (Node)tupleBuffer[1], this.m_assertionsDeltaOldRetrieval.getDependencySet(), AtomicNegationDataRange.create(datatypeRestriction2));
                }
            } else if (dataRangeObject instanceof AtomicNegationDataRange && (negatedDataRange = (negationDataRange = (AtomicNegationDataRange)dataRangeObject).getNegatedDataRange()) instanceof DatatypeRestriction && (this.m_unknownDatatypeRestrictionsPermanent.contains(datatypeRestriction = (DatatypeRestriction)negatedDataRange) || this.m_unknownDatatypeRestrictionsAdditional != null && this.m_unknownDatatypeRestrictionsAdditional.contains(datatypeRestriction))) {
                this.generateInequalitiesFor(negationDataRange, (Node)tupleBuffer[1], this.m_assertionsDeltaOldRetrieval.getDependencySet(), datatypeRestriction);
            }
            this.m_assertionsDeltaOldRetrieval.next();
        }
    }

    private void generateInequalitiesFor(DataRange dataRange1, Node node1, DependencySet dependencySet1, DataRange dataRange2) {
        this.m_unionDependencySet.clearConstituents();
        this.m_unionDependencySet.addConstituent(dependencySet1);
        this.m_unionDependencySet.addConstituent(null);
        this.m_assertions0Retrieval.getBindingsBuffer()[0] = dataRange2;
        Object[] tupleBuffer = this.m_assertions0Retrieval.getTupleBuffer();
        this.m_assertions0Retrieval.open();
        while (!this.m_assertions0Retrieval.afterLast()) {
            Node node2 = (Node)tupleBuffer[1];
            this.m_unionDependencySet.m_dependencySets[1] = this.m_assertions0Retrieval.getDependencySet();
            if (this.m_tableauMonitor != null) {
                this.m_tableauMonitor.unknownDatatypeRestrictionDetectionStarted(dataRange1, node1, dataRange2, node2);
            }
            this.m_extensionManager.addAssertion(Inequality.INSTANCE, node1, node2, this.m_unionDependencySet, false);
            if (this.m_tableauMonitor != null) {
                this.m_tableauMonitor.unknownDatatypeRestrictionDetectionFinished(dataRange1, node1, dataRange2, node2);
            }
            this.m_assertions0Retrieval.next();
        }
    }

    public void checkDatatypeConstraints() {
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.datatypeCheckingStarted();
        }
        this.m_conjunction.clear();
        Object[] tupleBuffer = this.m_assertionsDeltaOldRetrieval.getTupleBuffer();
        this.m_assertionsDeltaOldRetrieval.open();
        while (!this.m_extensionManager.containsClash() && !this.m_assertionsDeltaOldRetrieval.afterLast()) {
            if (tupleBuffer[0] instanceof DataRange) {
                Node node = (Node)tupleBuffer[1];
                DVariable variable = this.getAndInitializeVariableFor(node, this.m_newVariableAdded);
                if (this.m_newVariableAdded[0]) {
                    this.m_conjunction.clearActiveVariables();
                    this.loadConjunctionFrom(variable);
                    this.checkConjunctionSatisfiability();
                }
            }
            this.m_assertionsDeltaOldRetrieval.next();
        }
        tupleBuffer = this.m_inequalityDeltaOldRetrieval.getTupleBuffer();
        this.m_inequalityDeltaOldRetrieval.open();
        while (!this.m_extensionManager.containsClash() && !this.m_inequalityDeltaOldRetrieval.afterLast()) {
            if (Inequality.INSTANCE.equals(tupleBuffer[0])) {
                Node node1 = (Node)tupleBuffer[1];
                Node node2 = (Node)tupleBuffer[2];
                if (!node1.getNodeType().isAbstract() && !node2.getNodeType().isAbstract()) {
                    this.m_conjunction.clearActiveVariables();
                    DVariable variable1 = this.getAndInitializeVariableFor(node1, this.m_newVariableAdded);
                    if (this.m_newVariableAdded[0]) {
                        this.loadConjunctionFrom(variable1);
                    }
                    DVariable variable2 = this.getAndInitializeVariableFor(node2, this.m_newVariableAdded);
                    if (this.m_newVariableAdded[0]) {
                        this.loadConjunctionFrom(variable2);
                    }
                    this.m_conjunction.addInequality(variable1, variable2);
                    this.checkConjunctionSatisfiability();
                }
            }
            this.m_inequalityDeltaOldRetrieval.next();
        }
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.datatypeCheckingFinished(!this.m_extensionManager.containsClash());
        }
        this.m_unionDependencySet.clearConstituents();
        this.m_conjunction.clear();
        this.m_auxiliaryVariableList.clear();
    }

    private void loadConjunctionFrom(DVariable startVariable) {
        this.m_auxiliaryVariableList.clear();
        this.m_auxiliaryVariableList.add(startVariable);
        while (!this.m_extensionManager.containsClash() && !this.m_auxiliaryVariableList.isEmpty()) {
            Node newNode;
            DVariable newVariable;
            DVariable reachedVariable = this.m_auxiliaryVariableList.remove(this.m_auxiliaryVariableList.size() - 1);
            if (this.m_conjunction.m_activeVariables.contains(reachedVariable)) continue;
            this.m_conjunction.m_activeVariables.add(reachedVariable);
            if (reachedVariable.m_node.getNodeType() == NodeType.ROOT_CONSTANT_NODE) continue;
            this.m_inequality01Retrieval.getBindingsBuffer()[0] = Inequality.INSTANCE;
            this.m_inequality01Retrieval.getBindingsBuffer()[1] = reachedVariable.m_node;
            this.m_inequality01Retrieval.open();
            Object[] tupleBuffer = this.m_inequality01Retrieval.getTupleBuffer();
            while (!this.m_extensionManager.containsClash() && !this.m_inequality01Retrieval.afterLast()) {
                newNode = (Node)tupleBuffer[2];
                newVariable = this.getAndInitializeVariableFor(newNode, this.m_newVariableAdded);
                this.m_auxiliaryVariableList.add(newVariable);
                this.m_conjunction.addInequality(reachedVariable, newVariable);
                this.m_inequality01Retrieval.next();
                this.m_interruptFlag.checkInterrupt();
            }
            this.m_inequality02Retrieval.getBindingsBuffer()[0] = Inequality.INSTANCE;
            this.m_inequality02Retrieval.getBindingsBuffer()[2] = reachedVariable.m_node;
            this.m_inequality02Retrieval.open();
            tupleBuffer = this.m_inequality02Retrieval.getTupleBuffer();
            while (!this.m_extensionManager.containsClash() && !this.m_inequality02Retrieval.afterLast()) {
                newNode = (Node)tupleBuffer[1];
                newVariable = this.getAndInitializeVariableFor(newNode, this.m_newVariableAdded);
                this.m_auxiliaryVariableList.add(newVariable);
                this.m_conjunction.addInequality(newVariable, reachedVariable);
                this.m_inequality02Retrieval.next();
                this.m_interruptFlag.checkInterrupt();
            }
        }
    }

    private DVariable getAndInitializeVariableFor(Node node, boolean[] newVariableAdded) {
        DVariable variable = this.m_conjunction.getVariableForEx(node, newVariableAdded);
        if (this.m_newVariableAdded[0]) {
            this.m_assertions1Retrieval.getBindingsBuffer()[1] = variable.m_node;
            this.m_assertions1Retrieval.open();
            Object[] tupleBuffer = this.m_assertions1Retrieval.getTupleBuffer();
            while (!this.m_extensionManager.containsClash() && !this.m_assertions1Retrieval.afterLast()) {
                Object potentialDataRange = tupleBuffer[0];
                if (potentialDataRange instanceof DataRange) {
                    this.addDataRange(variable, (DataRange)potentialDataRange);
                }
                this.m_assertions1Retrieval.next();
                this.m_interruptFlag.checkInterrupt();
            }
            if (!this.m_extensionManager.containsClash()) {
                this.normalize(variable);
            }
        }
        return variable;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private void addDataRange(DVariable variable, DataRange dataRange) {
        if (dataRange instanceof InternalDatatype) return;
        if (dataRange instanceof DatatypeRestriction) {
            DatatypeRestriction datatypeRestriction = (DatatypeRestriction)dataRange;
            if (this.m_unknownDatatypeRestrictionsPermanent.contains(datatypeRestriction) || this.m_unknownDatatypeRestrictionsAdditional != null && this.m_unknownDatatypeRestrictionsAdditional.contains(datatypeRestriction)) return;
            variable.m_positiveDatatypeRestrictions.add(datatypeRestriction);
            if (variable.m_mostSpecificRestriction == null) {
                variable.m_mostSpecificRestriction = datatypeRestriction;
                return;
            } else if (DatatypeRegistry.isDisjointWith(variable.m_mostSpecificRestriction.getDatatypeURI(), datatypeRestriction.getDatatypeURI())) {
                Object[] tuple1;
                Object[] tuple2;
                this.m_unionDependencySet.clearConstituents();
                this.m_unionDependencySet.addConstituent(this.m_extensionManager.getAssertionDependencySet(variable.m_mostSpecificRestriction, variable.m_node));
                this.m_unionDependencySet.addConstituent(this.m_extensionManager.getAssertionDependencySet(datatypeRestriction, variable.m_node));
                if (this.m_tableauMonitor != null) {
                    tuple1 = new Object[]{variable.m_mostSpecificRestriction, variable.m_node};
                    tuple2 = new Object[]{datatypeRestriction, variable.m_node};
                    this.m_tableauMonitor.clashDetectionStarted(tuple1, tuple2);
                }
                this.m_extensionManager.setClash(this.m_unionDependencySet);
                if (this.m_tableauMonitor == null) return;
                tuple1 = new Object[]{variable.m_mostSpecificRestriction, variable.m_node};
                tuple2 = new Object[]{datatypeRestriction, variable.m_node};
                this.m_tableauMonitor.clashDetectionFinished(tuple1, tuple2);
                return;
            } else {
                if (!DatatypeRegistry.isSubsetOf(datatypeRestriction.getDatatypeURI(), variable.m_mostSpecificRestriction.getDatatypeURI())) return;
                variable.m_mostSpecificRestriction = datatypeRestriction;
            }
        } else if (dataRange instanceof ConstantEnumeration) {
            variable.m_positiveConstantEnumerations.add((ConstantEnumeration)dataRange);
        } else {
            if (!(dataRange instanceof AtomicNegationDataRange)) throw new IllegalStateException("Internal error: invalid data range.");
            AtomicDataRange negatedDataRange = ((AtomicNegationDataRange)dataRange).getNegatedDataRange();
            if (negatedDataRange instanceof InternalDatatype) return;
            if (negatedDataRange instanceof DatatypeRestriction) {
                DatatypeRestriction datatypeRestriction = (DatatypeRestriction)negatedDataRange;
                if (this.m_unknownDatatypeRestrictionsPermanent.contains(datatypeRestriction) || this.m_unknownDatatypeRestrictionsAdditional != null && this.m_unknownDatatypeRestrictionsAdditional.contains(datatypeRestriction)) return;
                variable.m_negativeDatatypeRestrictions.add(datatypeRestriction);
            } else {
                if (!(negatedDataRange instanceof ConstantEnumeration)) throw new IllegalStateException("Internal error: invalid data range.");
                ConstantEnumeration negatedConstantEnumeration = (ConstantEnumeration)negatedDataRange;
                variable.m_negativeConstantEnumerations.add(negatedConstantEnumeration);
                for (int index = negatedConstantEnumeration.getNumberOfConstants() - 1; index >= 0; --index) {
                    variable.addForbiddenDataValue(negatedConstantEnumeration.getConstant(index).getDataValue());
                }
            }
        }
    }

    private void checkConjunctionSatisfiability() {
        if (!this.m_extensionManager.containsClash() && !this.m_conjunction.m_activeVariables.isEmpty()) {
            if (this.m_tableauMonitor != null) {
                this.m_tableauMonitor.datatypeConjunctionCheckingStarted(this.m_conjunction);
            }
            if (this.m_conjunction.isSymmetricClique()) {
                DVariable representative = this.m_conjunction.m_activeVariables.get(0);
                if (!this.m_extensionManager.containsClash() && !representative.hasCardinalityAtLeast(this.m_conjunction.m_activeVariables.size())) {
                    this.setClashFor(this.m_conjunction.m_activeVariables);
                }
            } else if (!this.m_extensionManager.containsClash()) {
                this.eliminateTrivialInequalities();
                this.eliminateTriviallySatisfiableNodes();
                this.enumerateValueSpaceSubsets();
                if (!this.m_extensionManager.containsClash()) {
                    this.eliminateTriviallySatisfiableNodes();
                    this.checkAssignments();
                }
            }
            if (this.m_tableauMonitor != null) {
                this.m_tableauMonitor.datatypeConjunctionCheckingFinished(this.m_conjunction, !this.m_extensionManager.containsClash());
            }
        }
    }

    private void normalize(DVariable variable) {
        if (!variable.m_positiveConstantEnumerations.isEmpty()) {
            this.normalizeAsEnumeration(variable);
        } else if (!variable.m_positiveDatatypeRestrictions.isEmpty()) {
            this.normalizeAsValueSpaceSubset(variable);
        }
    }

    private void normalizeAsEnumeration(DVariable variable) {
        variable.m_hasExplicitDataValues = true;
        List<Object> explicitDataValues = variable.m_explicitDataValues;
        List<ConstantEnumeration> positiveConstantEnumerations = variable.m_positiveConstantEnumerations;
        ConstantEnumeration firstDataValueEnumeration = positiveConstantEnumerations.get(0);
        block0 : for (int index = firstDataValueEnumeration.getNumberOfConstants() - 1; index >= 0; --index) {
            Object dataValue = firstDataValueEnumeration.getConstant(index).getDataValue();
            if (explicitDataValues.contains(dataValue) || variable.m_forbiddenDataValues.contains(dataValue)) continue;
            for (int enumerationIndex = positiveConstantEnumerations.size() - 1; enumerationIndex >= 1; --enumerationIndex) {
                if (!DatatypeManager.containsDataValue(positiveConstantEnumerations.get(enumerationIndex), dataValue)) continue block0;
            }
            explicitDataValues.add(dataValue);
        }
        variable.m_forbiddenDataValues.clear();
        List<DatatypeRestriction> positiveDatatypeRestrictions = variable.m_positiveDatatypeRestrictions;
        for (int index = positiveDatatypeRestrictions.size() - 1; !explicitDataValues.isEmpty() && index >= 0; --index) {
            DatatypeRestriction positiveDatatypeRestriction = positiveDatatypeRestrictions.get(index);
            ValueSpaceSubset valueSpaceSubset = DatatypeRegistry.createValueSpaceSubset(positiveDatatypeRestriction);
            DatatypeManager.eliminateDataValuesUsingValueSpaceSubset(valueSpaceSubset, explicitDataValues, false);
        }
        List<DatatypeRestriction> negativeDatatypeRestrictions = variable.m_negativeDatatypeRestrictions;
        for (int index = negativeDatatypeRestrictions.size() - 1; !explicitDataValues.isEmpty() && index >= 0; --index) {
            DatatypeRestriction negativeDatatypeRestriction = negativeDatatypeRestrictions.get(index);
            ValueSpaceSubset valueSpaceSubset = DatatypeRegistry.createValueSpaceSubset(negativeDatatypeRestriction);
            DatatypeManager.eliminateDataValuesUsingValueSpaceSubset(valueSpaceSubset, explicitDataValues, true);
        }
        if (explicitDataValues.isEmpty()) {
            this.setClashFor(variable);
        }
    }

    private static boolean containsDataValue(ConstantEnumeration constantEnumeration, Object dataValue) {
        for (int index = constantEnumeration.getNumberOfConstants() - 1; index >= 0; --index) {
            if (!constantEnumeration.getConstant(index).getDataValue().equals(dataValue)) continue;
            return true;
        }
        return false;
    }

    private static void eliminateDataValuesUsingValueSpaceSubset(ValueSpaceSubset valueSpaceSubset, List<Object> explicitDataValues, boolean eliminateWhenValue) {
        for (int valueIndex = explicitDataValues.size() - 1; valueIndex >= 0; --valueIndex) {
            Object dataValue = explicitDataValues.get(valueIndex);
            if (valueSpaceSubset.containsDataValue(dataValue) != eliminateWhenValue) continue;
            explicitDataValues.remove(valueIndex);
        }
    }

    private void normalizeAsValueSpaceSubset(DVariable variable) {
        String mostSpecificDatatypeURI = variable.m_mostSpecificRestriction.getDatatypeURI();
        variable.m_valueSpaceSubset = DatatypeRegistry.createValueSpaceSubset(variable.m_mostSpecificRestriction);
        List<DatatypeRestriction> positiveDatatypeRestrictions = variable.m_positiveDatatypeRestrictions;
        for (int index = positiveDatatypeRestrictions.size() - 1; index >= 0; --index) {
            DatatypeRestriction datatypeRestriction = positiveDatatypeRestrictions.get(index);
            if (datatypeRestriction == variable.m_mostSpecificRestriction) continue;
            variable.m_valueSpaceSubset = DatatypeRegistry.conjoinWithDR(variable.m_valueSpaceSubset, datatypeRestriction);
        }
        List<DatatypeRestriction> negativeDatatypeRestrictions = variable.m_negativeDatatypeRestrictions;
        for (int index = negativeDatatypeRestrictions.size() - 1; index >= 0; --index) {
            DatatypeRestriction datatypeRestriction = negativeDatatypeRestrictions.get(index);
            String datatypeRestrictionDatatypeURI = datatypeRestriction.getDatatypeURI();
            if (DatatypeRegistry.isDisjointWith(mostSpecificDatatypeURI, datatypeRestrictionDatatypeURI)) continue;
            variable.m_valueSpaceSubset = DatatypeRegistry.conjoinWithDRNegation(variable.m_valueSpaceSubset, datatypeRestriction);
        }
        if (!variable.m_valueSpaceSubset.hasCardinalityAtLeast(1)) {
            variable.m_forbiddenDataValues.clear();
            this.setClashFor(variable);
        } else {
            for (int valueIndex = variable.m_forbiddenDataValues.size() - 1; valueIndex >= 0; --valueIndex) {
                Object forbiddenValue = variable.m_forbiddenDataValues.get(valueIndex);
                if (variable.m_valueSpaceSubset.containsDataValue(forbiddenValue)) continue;
                variable.m_forbiddenDataValues.remove(valueIndex);
            }
        }
    }

    private void eliminateTrivialInequalities() {
        for (int index1 = this.m_conjunction.m_activeVariables.size() - 1; index1 >= 0; --index1) {
            DVariable variable1 = this.m_conjunction.m_activeVariables.get(index1);
            if (variable1.m_mostSpecificRestriction == null) continue;
            String datatypeURI1 = variable1.m_mostSpecificRestriction.getDatatypeURI();
            for (int index2 = variable1.m_unequalToDirect.size() - 1; index2 >= 0; --index2) {
                DVariable variable2 = variable1.m_unequalToDirect.get(index2);
                if (variable2.m_mostSpecificRestriction == null || !DatatypeRegistry.isDisjointWith(datatypeURI1, variable2.m_mostSpecificRestriction.getDatatypeURI())) continue;
                variable1.m_unequalTo.remove(variable2);
                variable1.m_unequalToDirect.remove(variable2);
                variable2.m_unequalTo.remove(variable1);
                variable2.m_unequalToDirect.remove(variable1);
            }
        }
    }

    private void eliminateTriviallySatisfiableNodes() {
        this.m_auxiliaryVariableList.clear();
        for (int index = this.m_conjunction.m_activeVariables.size() - 1; index >= 0; --index) {
            this.m_auxiliaryVariableList.add(this.m_conjunction.m_activeVariables.get(index));
        }
        while (!this.m_auxiliaryVariableList.isEmpty()) {
            DVariable variable = this.m_auxiliaryVariableList.remove(this.m_auxiliaryVariableList.size() - 1);
            if (!variable.hasCardinalityAtLeast(variable.m_unequalTo.size() + 1)) continue;
            for (int index = variable.m_unequalTo.size() - 1; index >= 0; --index) {
                DVariable neighborVariable = variable.m_unequalTo.get(index);
                neighborVariable.m_unequalTo.remove(variable);
                neighborVariable.m_unequalToDirect.remove(variable);
                if (this.m_auxiliaryVariableList.contains(neighborVariable)) continue;
                this.m_auxiliaryVariableList.add(neighborVariable);
            }
            variable.clearEqualities();
            this.m_conjunction.m_activeVariables.remove(variable);
        }
    }

    private void enumerateValueSpaceSubsets() {
        for (int index = this.m_conjunction.m_activeVariables.size() - 1; !this.m_extensionManager.containsClash() && index >= 0; --index) {
            DVariable variable = this.m_conjunction.m_activeVariables.get(index);
            if (variable.m_valueSpaceSubset == null) continue;
            variable.m_hasExplicitDataValues = true;
            variable.m_valueSpaceSubset.enumerateDataValues(variable.m_explicitDataValues);
            if (!variable.m_forbiddenDataValues.isEmpty()) {
                for (int valueIndex = variable.m_explicitDataValues.size() - 1; valueIndex >= 0; --valueIndex) {
                    Object dataValue = variable.m_explicitDataValues.get(valueIndex);
                    if (!variable.m_forbiddenDataValues.contains(dataValue)) continue;
                    variable.m_explicitDataValues.remove(valueIndex);
                }
            }
            variable.m_valueSpaceSubset = null;
            variable.m_forbiddenDataValues.clear();
            if (!variable.m_explicitDataValues.isEmpty()) continue;
            this.setClashFor(variable);
        }
    }

    private void checkAssignments() {
        Collections.sort(this.m_conjunction.m_activeVariables, SmallestEnumerationFirst.INSTANCE);
        if (!this.findAssignment(0)) {
            this.setClashFor(this.m_conjunction.m_activeVariables);
        }
    }

    private boolean findAssignment(int nodeIndex) {
        if (nodeIndex == this.m_conjunction.m_activeVariables.size()) {
            return true;
        }
        DVariable variable = this.m_conjunction.m_activeVariables.get(nodeIndex);
        for (int valueIndex = variable.m_explicitDataValues.size() - 1; valueIndex >= 0; --valueIndex) {
            Object dataValue = variable.m_explicitDataValues.get(valueIndex);
            if (DatatypeManager.satisfiesNeighbors(variable, dataValue)) {
                variable.m_dataValue = dataValue;
                if (this.findAssignment(nodeIndex + 1)) {
                    return true;
                }
            }
            this.m_interruptFlag.checkInterrupt();
        }
        variable.m_dataValue = null;
        return false;
    }

    private static boolean satisfiesNeighbors(DVariable variable, Object dataValue) {
        for (int neighborIndex = variable.m_unequalTo.size() - 1; neighborIndex >= 0; --neighborIndex) {
            Object neighborDataValue = variable.m_unequalTo.get(neighborIndex).m_dataValue;
            if (neighborDataValue == null || !neighborDataValue.equals(dataValue)) continue;
            return false;
        }
        return true;
    }

    private void setClashFor(DVariable variable) {
        this.m_unionDependencySet.clearConstituents();
        this.loadAssertionDependencySets(variable);
        this.m_extensionManager.setClash(this.m_unionDependencySet);
    }

    private void setClashFor(List<DVariable> variables) {
        this.m_unionDependencySet.clearConstituents();
        for (int nodeIndex = variables.size() - 1; nodeIndex >= 0; --nodeIndex) {
            DVariable variable = variables.get(nodeIndex);
            this.loadAssertionDependencySets(variable);
            for (int neighborIndex = variable.m_unequalToDirect.size() - 1; neighborIndex >= 0; --neighborIndex) {
                DVariable neighborVariable = variable.m_unequalToDirect.get(neighborIndex);
                DependencySet dependencySet = this.m_extensionManager.getAssertionDependencySet(Inequality.INSTANCE, variable.m_node, neighborVariable.m_node);
                this.m_unionDependencySet.addConstituent(dependencySet);
            }
        }
        this.m_extensionManager.setClash(this.m_unionDependencySet);
    }

    private void loadAssertionDependencySets(DVariable variable) {
        LiteralDataRange dataRange;
        int index;
        DependencySet dependencySet;
        Node node = variable.m_node;
        for (index = variable.m_positiveDatatypeRestrictions.size() - 1; index >= 0; --index) {
            dataRange = variable.m_positiveDatatypeRestrictions.get(index);
            dependencySet = this.m_extensionManager.getAssertionDependencySet(dataRange, node);
            this.m_unionDependencySet.addConstituent(dependencySet);
        }
        for (index = variable.m_negativeDatatypeRestrictions.size() - 1; index >= 0; --index) {
            dataRange = variable.m_negativeDatatypeRestrictions.get(index).getNegation();
            dependencySet = this.m_extensionManager.getAssertionDependencySet(dataRange, node);
            this.m_unionDependencySet.addConstituent(dependencySet);
        }
        for (index = variable.m_positiveConstantEnumerations.size() - 1; index >= 0; --index) {
            dataRange = variable.m_positiveConstantEnumerations.get(index);
            dependencySet = this.m_extensionManager.getAssertionDependencySet(dataRange, node);
            this.m_unionDependencySet.addConstituent(dependencySet);
        }
        for (index = variable.m_negativeConstantEnumerations.size() - 1; index >= 0; --index) {
            dataRange = variable.m_negativeConstantEnumerations.get(index).getNegation();
            dependencySet = this.m_extensionManager.getAssertionDependencySet(dataRange, node);
            this.m_unionDependencySet.addConstituent(dependencySet);
        }
    }

    private static int getIndexFor(int _hashCode, int tableLength) {
        int hashCode = _hashCode;
        hashCode += ~ (hashCode << 9);
        hashCode ^= hashCode >>> 14;
        hashCode += hashCode << 4;
        hashCode ^= hashCode >>> 10;
        return hashCode & tableLength - 1;
    }

    protected static class SmallestEnumerationFirst
    implements Comparator<DVariable>,
    Serializable {
        private static final long serialVersionUID = 8838838641444833249L;
        public static final Comparator<DVariable> INSTANCE = new SmallestEnumerationFirst();

        protected SmallestEnumerationFirst() {
        }

        @Override
        public int compare(DVariable o1, DVariable o2) {
            return o1.m_explicitDataValues.size() - o2.m_explicitDataValues.size();
        }
    }

    public static class DVariable
    implements Serializable {
        private static final long serialVersionUID = -2490195841140286089L;
        protected final List<ConstantEnumeration> m_positiveConstantEnumerations = new ArrayList<ConstantEnumeration>();
        protected final List<ConstantEnumeration> m_negativeConstantEnumerations = new ArrayList<ConstantEnumeration>();
        protected final List<DatatypeRestriction> m_positiveDatatypeRestrictions = new ArrayList<DatatypeRestriction>();
        protected final List<DatatypeRestriction> m_negativeDatatypeRestrictions = new ArrayList<DatatypeRestriction>();
        protected final List<DVariable> m_unequalTo = new ArrayList<DVariable>();
        protected final List<DVariable> m_unequalToDirect = new ArrayList<DVariable>();
        protected final List<Object> m_forbiddenDataValues = new ArrayList<Object>();
        protected final List<Object> m_explicitDataValues = new ArrayList<Object>();
        protected boolean m_hasExplicitDataValues;
        protected DatatypeRestriction m_mostSpecificRestriction;
        protected Node m_node;
        protected DVariable m_nextEntry;
        protected ValueSpaceSubset m_valueSpaceSubset;
        protected Object m_dataValue;

        protected void dispose() {
            this.m_positiveConstantEnumerations.clear();
            this.m_negativeConstantEnumerations.clear();
            this.m_positiveDatatypeRestrictions.clear();
            this.m_negativeDatatypeRestrictions.clear();
            this.m_unequalTo.clear();
            this.m_unequalToDirect.clear();
            this.m_forbiddenDataValues.clear();
            this.m_explicitDataValues.clear();
            this.m_hasExplicitDataValues = false;
            this.m_mostSpecificRestriction = null;
            this.m_node = null;
            this.m_nextEntry = null;
            this.m_valueSpaceSubset = null;
            this.m_dataValue = null;
        }

        protected void clearEqualities() {
            this.m_unequalTo.clear();
            this.m_unequalToDirect.clear();
        }

        protected void addForbiddenDataValue(Object forbiddenDataValue) {
            if (!this.m_forbiddenDataValues.contains(forbiddenDataValue)) {
                this.m_forbiddenDataValues.add(forbiddenDataValue);
            }
        }

        boolean hasCardinalityAtLeast(int number) {
            if (this.m_hasExplicitDataValues) {
                return this.m_explicitDataValues.size() >= number;
            }
            if (this.m_valueSpaceSubset != null) {
                return this.m_valueSpaceSubset.hasCardinalityAtLeast(number + this.m_forbiddenDataValues.size());
            }
            return true;
        }

        public Node getNode() {
            return this.m_node;
        }

        public List<ConstantEnumeration> getPositiveDataValueEnumerations() {
            return Collections.unmodifiableList(this.m_positiveConstantEnumerations);
        }

        public List<ConstantEnumeration> getNegativeDataValueEnumerations() {
            return Collections.unmodifiableList(this.m_negativeConstantEnumerations);
        }

        public List<DatatypeRestriction> getPositiveDatatypeRestrictions() {
            return Collections.unmodifiableList(this.m_positiveDatatypeRestrictions);
        }

        public List<DatatypeRestriction> getNegativeDatatypeRestrictions() {
            return Collections.unmodifiableList(this.m_negativeDatatypeRestrictions);
        }

        public List<DVariable> getUnequalToDirect() {
            return Collections.unmodifiableList(this.m_unequalToDirect);
        }

        public boolean hasSameRestrictions(DVariable that) {
            return this == that || DVariable.equals(this.m_positiveConstantEnumerations, that.m_positiveConstantEnumerations) && DVariable.equals(this.m_negativeConstantEnumerations, that.m_negativeConstantEnumerations) && DVariable.equals(this.m_positiveDatatypeRestrictions, that.m_positiveDatatypeRestrictions) && DVariable.equals(this.m_negativeDatatypeRestrictions, that.m_negativeDatatypeRestrictions);
        }

        protected static <T> boolean equals(List<T> first, List<T> second) {
            if (first.size() != second.size()) {
                return false;
            }
            for (int index = first.size() - 1; index >= 0; --index) {
                T object = first.get(index);
                if (second.contains(object)) continue;
                return false;
            }
            return true;
        }

        public String toString() {
            return this.toString(Prefixes.STANDARD_PREFIXES);
        }

        public String toString(Prefixes prefixes) {
            int index;
            StringBuffer buffer = new StringBuffer();
            boolean first = true;
            buffer.append('[');
            for (index = 0; index < this.m_positiveConstantEnumerations.size(); ++index) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(", ");
                }
                buffer.append(this.m_positiveConstantEnumerations.get(index).toString(prefixes));
            }
            for (index = 0; index < this.m_negativeConstantEnumerations.size(); ++index) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(", ");
                }
                buffer.append(this.m_negativeConstantEnumerations.get(index).getNegation().toString(prefixes));
            }
            for (index = 0; index < this.m_positiveDatatypeRestrictions.size(); ++index) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(", ");
                }
                buffer.append(this.m_positiveDatatypeRestrictions.get(index).toString(prefixes));
            }
            for (index = 0; index < this.m_negativeDatatypeRestrictions.size(); ++index) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(", ");
                }
                buffer.append(this.m_negativeDatatypeRestrictions.get(index).getNegation().toString(prefixes));
            }
            buffer.append(']');
            return buffer.toString();
        }
    }

    public static class DConjunction
    implements Serializable {
        private static final long serialVersionUID = 3597740301361593691L;
        protected final List<DVariable> m_unusedVariables = new ArrayList<DVariable>();
        protected final List<DVariable> m_usedVariables = new ArrayList<DVariable>();
        protected final List<DVariable> m_activeVariables = new ArrayList<DVariable>();
        protected DVariable[] m_buckets = new DVariable[16];
        protected int m_numberOfEntries = 0;
        protected int m_resizeThreshold = 12;

        protected void clear() {
            for (int index = this.m_usedVariables.size() - 1; index >= 0; --index) {
                DVariable variable = this.m_usedVariables.get(index);
                variable.dispose();
                this.m_unusedVariables.add(variable);
            }
            this.m_usedVariables.clear();
            this.m_activeVariables.clear();
            Arrays.fill(this.m_buckets, null);
            this.m_numberOfEntries = 0;
        }

        protected void clearActiveVariables() {
            for (int index = this.m_activeVariables.size() - 1; index >= 0; --index) {
                this.m_activeVariables.get(index).clearEqualities();
            }
            this.m_activeVariables.clear();
        }

        public List<DVariable> getActiveVariables() {
            return Collections.unmodifiableList(this.m_activeVariables);
        }

        public DVariable getVariableFor(Node node) {
            int index = DatatypeManager.getIndexFor(node.hashCode(), this.m_buckets.length);
            DVariable entry = this.m_buckets[index];
            while (entry != null) {
                if (entry.m_node == node) {
                    return entry;
                }
                entry = entry.m_nextEntry;
            }
            return null;
        }

        protected DVariable getVariableForEx(Node node, boolean[] newVariableAdded) {
            int index = DatatypeManager.getIndexFor(node.hashCode(), this.m_buckets.length);
            DVariable entry = this.m_buckets[index];
            while (entry != null) {
                if (entry.m_node == node) {
                    newVariableAdded[0] = false;
                    return entry;
                }
                entry = entry.m_nextEntry;
            }
            DVariable newVariable = this.m_unusedVariables.isEmpty() ? new DVariable() : this.m_unusedVariables.remove(this.m_unusedVariables.size() - 1);
            newVariable.m_node = node;
            newVariable.m_nextEntry = this.m_buckets[index];
            this.m_buckets[index] = newVariable;
            ++this.m_numberOfEntries;
            if (this.m_numberOfEntries >= this.m_resizeThreshold) {
                this.resize(this.m_buckets.length * 2);
            }
            newVariableAdded[0] = true;
            this.m_usedVariables.add(newVariable);
            return newVariable;
        }

        protected void resize(int newCapacity) {
            DVariable[] newBuckets = new DVariable[newCapacity];
            for (int i = 0; i < this.m_buckets.length; ++i) {
                DVariable entry = this.m_buckets[i];
                while (entry != null) {
                    DVariable nextEntry = entry.m_nextEntry;
                    int newIndex = DatatypeManager.getIndexFor(entry.m_node.hashCode(), newCapacity);
                    entry.m_nextEntry = newBuckets[newIndex];
                    newBuckets[newIndex] = entry;
                    entry = nextEntry;
                }
            }
            this.m_buckets = newBuckets;
            this.m_resizeThreshold = (int)((double)newCapacity * 0.75);
        }

        protected void addInequality(DVariable node1, DVariable node2) {
            assert (node1 != node2);
            if (!node1.m_unequalTo.contains(node2)) {
                node1.m_unequalTo.add(node2);
                node2.m_unequalTo.add(node1);
                node1.m_unequalToDirect.add(node2);
            }
        }

        boolean isSymmetricClique() {
            int numberOfVariables = this.m_activeVariables.size();
            if (numberOfVariables > 0) {
                DVariable first = this.m_activeVariables.get(0);
                for (int variableIndex = numberOfVariables - 1; variableIndex >= 0; --variableIndex) {
                    DVariable variable = this.m_activeVariables.get(variableIndex);
                    if (variable.m_unequalTo.size() + 1 == numberOfVariables && first.hasSameRestrictions(variable)) continue;
                    return false;
                }
            }
            return true;
        }

        public String toString() {
            return this.toString(Prefixes.STANDARD_PREFIXES);
        }

        public String toString(Prefixes prefixes) {
            StringBuffer buffer = new StringBuffer();
            boolean first = true;
            for (int variableIndex = 0; variableIndex < this.m_activeVariables.size(); ++variableIndex) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(" & ");
                }
                DVariable variable = this.m_activeVariables.get(variableIndex);
                buffer.append(variable.toString(prefixes));
                buffer.append('(');
                buffer.append(variableIndex);
                buffer.append(')');
                for (int neighborIndex = 0; neighborIndex < variable.m_unequalToDirect.size(); ++neighborIndex) {
                    buffer.append(" & ");
                    buffer.append(variableIndex);
                    buffer.append(" != ");
                    buffer.append(this.m_activeVariables.indexOf(variable.m_unequalToDirect.get(neighborIndex)));
                }
            }
            return buffer.toString();
        }
    }

}

