package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicNegationConcept;
import org.semanticweb.HermiT.model.AtomicNegationDataRange;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.model.InternalDatatype;
import org.semanticweb.HermiT.model.LiteralConcept;
import org.semanticweb.HermiT.model.LiteralDataRange;
import org.semanticweb.HermiT.model.NegatedAtomicRole;
import org.semanticweb.HermiT.monitor.TableauMonitor;

public final class ClashManager
implements Serializable {
    private static final long serialVersionUID = 3533809151139695892L;
    private static final LiteralDataRange NOT_RDFS_LITERAL = InternalDatatype.RDFS_LITERAL.getNegation();
    private final ExtensionManager m_extensionManager;
    private final ExtensionTable.Retrieval m_ternaryExtensionTableSearch01Bound;
    private final TableauMonitor m_tableauMonitor;
    private final Object[] m_binaryAuxiliaryTuple;
    private final Object[] m_ternaryAuxiliaryTuple;
    private final UnionDependencySet m_binaryUnionDependencySet;

    public ClashManager(Tableau tableau) {
        this.m_extensionManager = tableau.m_extensionManager;
        this.m_ternaryExtensionTableSearch01Bound = this.m_extensionManager.m_ternaryExtensionTable.createRetrieval(new boolean[]{true, true, false}, ExtensionTable.View.TOTAL);
        this.m_tableauMonitor = tableau.m_tableauMonitor;
        this.m_binaryAuxiliaryTuple = new Object[2];
        this.m_ternaryAuxiliaryTuple = new Object[3];
        this.m_binaryUnionDependencySet = new UnionDependencySet(2);
    }

    public void clear() {
        this.m_ternaryExtensionTableSearch01Bound.clear();
        this.m_binaryAuxiliaryTuple[0] = null;
        this.m_binaryAuxiliaryTuple[1] = null;
        this.m_ternaryAuxiliaryTuple[0] = null;
        this.m_ternaryAuxiliaryTuple[1] = null;
        this.m_ternaryAuxiliaryTuple[2] = null;
        this.m_binaryUnionDependencySet.m_dependencySets[0] = null;
        this.m_binaryUnionDependencySet.m_dependencySets[1] = null;
    }

    public void tupleAdded(ExtensionTable extensionTable, Object[] tuple, DependencySet dependencySet) {
        block14 : {
            Node node0;
            Object dlPredicateObject;
            block15 : {
                block13 : {
                    dlPredicateObject = tuple[0];
                    node0 = (Node)tuple[1];
                    if (!AtomicConcept.NOTHING.equals(dlPredicateObject) && !NOT_RDFS_LITERAL.equals(dlPredicateObject) && (!Inequality.INSTANCE.equals(dlPredicateObject) || tuple[1] != tuple[2])) break block13;
                    if (this.m_tableauMonitor != null) {
                        this.m_tableauMonitor.clashDetectionStarted(new Object[][]{tuple});
                    }
                    this.m_extensionManager.setClash(dependencySet);
                    if (this.m_tableauMonitor != null) {
                        this.m_tableauMonitor.clashDetectionFinished(new Object[][]{tuple});
                    }
                    break block14;
                }
                if (!(dlPredicateObject instanceof InternalDatatype || dlPredicateObject instanceof AtomicNegationDataRange && ((AtomicNegationDataRange)dlPredicateObject).getNegatedDataRange() instanceof InternalDatatype || dlPredicateObject instanceof AtomicConcept && node0.m_numberOfNegatedAtomicConcepts > 0) && (!(dlPredicateObject instanceof AtomicNegationConcept) || node0.m_numberOfPositiveAtomicConcepts <= 0)) break block15;
                this.m_binaryAuxiliaryTuple[0] = dlPredicateObject instanceof LiteralDataRange ? ((LiteralDataRange)dlPredicateObject).getNegation() : ((LiteralConcept)dlPredicateObject).getNegation();
                this.m_binaryAuxiliaryTuple[1] = node0;
                if (!extensionTable.containsTuple(this.m_binaryAuxiliaryTuple)) break block14;
                this.m_binaryUnionDependencySet.m_dependencySets[0] = dependencySet;
                this.m_binaryUnionDependencySet.m_dependencySets[1] = extensionTable.getDependencySet(this.m_binaryAuxiliaryTuple);
                if (this.m_tableauMonitor != null) {
                    this.m_tableauMonitor.clashDetectionStarted(tuple, this.m_binaryAuxiliaryTuple);
                }
                this.m_extensionManager.setClash(this.m_binaryUnionDependencySet);
                if (this.m_tableauMonitor == null) break block14;
                this.m_tableauMonitor.clashDetectionFinished(tuple, this.m_binaryAuxiliaryTuple);
                break block14;
            }
            if (dlPredicateObject instanceof AtomicRole && node0.m_numberOfNegatedRoleAssertions > 0 || dlPredicateObject instanceof NegatedAtomicRole) {
                Object searchPredicate = dlPredicateObject instanceof AtomicRole ? NegatedAtomicRole.create((AtomicRole)dlPredicateObject) : ((NegatedAtomicRole)dlPredicateObject).getNegatedAtomicRole();
                this.m_ternaryAuxiliaryTuple[0] = searchPredicate;
                this.m_ternaryAuxiliaryTuple[1] = node0;
                this.m_ternaryAuxiliaryTuple[2] = tuple[2];
                if (extensionTable.containsTuple(this.m_ternaryAuxiliaryTuple)) {
                    this.m_binaryUnionDependencySet.m_dependencySets[0] = dependencySet;
                    this.m_binaryUnionDependencySet.m_dependencySets[1] = extensionTable.getDependencySet(this.m_ternaryAuxiliaryTuple);
                    if (this.m_tableauMonitor != null) {
                        this.m_tableauMonitor.clashDetectionStarted(tuple, this.m_ternaryAuxiliaryTuple);
                    }
                    this.m_extensionManager.setClash(this.m_binaryUnionDependencySet);
                    if (this.m_tableauMonitor != null) {
                        this.m_tableauMonitor.clashDetectionFinished(tuple, this.m_ternaryAuxiliaryTuple);
                    }
                } else if (!((Node)tuple[2]).getNodeType().isAbstract()) {
                    this.m_ternaryAuxiliaryTuple[0] = Inequality.INSTANCE;
                    this.m_ternaryAuxiliaryTuple[1] = tuple[2];
                    this.m_binaryUnionDependencySet.m_dependencySets[0] = dependencySet;
                    this.m_ternaryExtensionTableSearch01Bound.getBindingsBuffer()[0] = searchPredicate;
                    this.m_ternaryExtensionTableSearch01Bound.getBindingsBuffer()[1] = tuple[1];
                    this.m_ternaryExtensionTableSearch01Bound.open();
                    Object[] tupleBuffer = this.m_ternaryExtensionTableSearch01Bound.getTupleBuffer();
                    while (!this.m_ternaryExtensionTableSearch01Bound.afterLast()) {
                        assert (!((Node)tupleBuffer[2]).getNodeType().isAbstract());
                        this.m_ternaryAuxiliaryTuple[2] = tupleBuffer[2];
                        this.m_binaryUnionDependencySet.m_dependencySets[1] = this.m_ternaryExtensionTableSearch01Bound.getDependencySet();
                        if (this.m_tableauMonitor != null) {
                            this.m_tableauMonitor.clashDetectionStarted(tuple, tupleBuffer);
                        }
                        this.m_extensionManager.m_ternaryExtensionTable.addTuple(this.m_ternaryAuxiliaryTuple, this.m_binaryUnionDependencySet, true);
                        if (this.m_tableauMonitor != null) {
                            this.m_tableauMonitor.clashDetectionFinished(tuple, tupleBuffer);
                        }
                        this.m_ternaryExtensionTableSearch01Bound.next();
                    }
                }
            }
        }
    }
}

