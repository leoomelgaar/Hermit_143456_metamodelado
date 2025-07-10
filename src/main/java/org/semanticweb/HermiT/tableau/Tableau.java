package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.existentials.ExistentialExpansionStrategy;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Constant;
import org.semanticweb.HermiT.model.ConstantEnumeration;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLOntology;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.DescriptionGraph;
import org.semanticweb.HermiT.model.Equality;
import org.semanticweb.HermiT.model.ExistentialConcept;
import org.semanticweb.HermiT.model.Individual;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.model.InternalDatatype;
import org.semanticweb.HermiT.model.LiteralConcept;
import org.semanticweb.HermiT.model.NegatedAtomicRole;
import org.semanticweb.HermiT.model.Term;
import org.semanticweb.HermiT.model.Variable;
import org.semanticweb.HermiT.monitor.TableauMonitor;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLMetamodellingAxiom;

public final class Tableau
implements Serializable {
    private static final long serialVersionUID = -28982363158925221L;
    final InterruptFlag m_interruptFlag;
    private final Map<String, Object> m_parameters;
    final TableauMonitor m_tableauMonitor;
    final ExistentialExpansionStrategy m_existentialExpansionStrategy;
    final DLOntology m_permanentDLOntology;
    DLOntology m_additionalDLOntology;
    final DependencySetFactory m_dependencySetFactory;
    final ExtensionManager m_extensionManager;
    final ClashManager m_clashManager;
    private HyperresolutionManager m_permanentHyperresolutionManager;
    private ArrayList<BranchedHyperresolutionManager> branchedHyperresolutionManagers;
    private HyperresolutionManager m_additionalHyperresolutionManager;
    final MergingManager m_mergingManager;
    private final ExistentialExpansionManager m_existentialExpasionManager;
    final NominalIntroductionManager m_nominalIntroductionManager;
    final DescriptionGraphManager m_descriptionGraphManager;
    private final DatatypeManager m_datatypeManager;
    private final List<List<ExistentialConcept>> m_existentialConceptsBuffers;
    final boolean m_useDisjunctionLearning;
    private final boolean m_hasDescriptionGraphs;
    private BranchingPoint[] m_branchingPoints;
    int m_currentBranchingPoint;
    private int m_nonbacktrackableBranchingPoint;
    private boolean m_isCurrentModelDeterministic;
    boolean m_needsThingExtension;
    private boolean m_needsNamedExtension;
    boolean m_needsRDFSLiteralExtension;
    private boolean m_checkDatatypes;
    private boolean m_checkUnknownDatatypeRestrictions;
    private int m_allocatedNodes;
    private int m_numberOfNodesInTableau;
    private int m_numberOfMergedOrPrunedNodes;
    private int m_numberOfNodeCreations;
    private Node m_firstFreeNode;
    private Node m_firstTableauNode;
    Node m_lastTableauNode;
    Node m_lastMergedOrPrunedNode;
    GroundDisjunction m_firstGroundDisjunction;
    GroundDisjunction m_firstUnprocessedGroundDisjunction;

    // Metamodelling attributes
    MetamodellingManager m_metamodellingManager;
    boolean metamodellingFlag;
    private ArrayList<BranchedMetamodellingManager> branchedMetamodellingManagers;

    public Tableau(InterruptFlag interruptFlag, TableauMonitor tableauMonitor, ExistentialExpansionStrategy existentialsExpansionStrategy, boolean useDisjunctionLearning, DLOntology permanentDLOntology, DLOntology additionalDLOntology, Map<String, Object> parameters) {
        if (additionalDLOntology != null && !additionalDLOntology.getAllDescriptionGraphs().isEmpty()) {
            throw new IllegalArgumentException("Additional ontology cannot contain description graphs.");
        }
        this.m_interruptFlag = interruptFlag;
        this.m_interruptFlag.startTask();
        try {
            this.m_parameters = parameters;
            this.m_tableauMonitor = tableauMonitor;
            this.m_existentialExpansionStrategy = existentialsExpansionStrategy;
            this.m_permanentDLOntology = permanentDLOntology;
            this.m_additionalDLOntology = additionalDLOntology;
            this.m_dependencySetFactory = new DependencySetFactory();
            this.m_extensionManager = new ExtensionManager(this);
            this.m_metamodellingManager = new MetamodellingManager(this);
            this.m_clashManager = new ClashManager(this);
            this.m_permanentHyperresolutionManager = new HyperresolutionManager(this, this.m_permanentDLOntology.getDLClauses());
            this.m_additionalHyperresolutionManager = this.m_additionalDLOntology != null ? new HyperresolutionManager(this, this.m_additionalDLOntology.getDLClauses()) : null;
            this.m_mergingManager = new MergingManager(this);
            this.m_existentialExpasionManager = new ExistentialExpansionManager(this);
            this.m_nominalIntroductionManager = new NominalIntroductionManager(this);
            this.m_descriptionGraphManager = new DescriptionGraphManager(this);
            this.m_datatypeManager = new DatatypeManager(this);
            this.m_existentialExpansionStrategy.initialize(this);
            this.m_existentialConceptsBuffers = new ArrayList<List<ExistentialConcept>>();
            this.m_useDisjunctionLearning = useDisjunctionLearning;
            this.m_hasDescriptionGraphs = !this.m_permanentDLOntology.getAllDescriptionGraphs().isEmpty();
            this.m_branchingPoints = new BranchingPoint[2];
            this.m_currentBranchingPoint = -1;
            this.m_nonbacktrackableBranchingPoint = -1;
            this.branchedHyperresolutionManagers = new ArrayList<BranchedHyperresolutionManager>();
            this.metamodellingFlag = true;

            for (int j=0; j<this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages.length; j++) {
            	if (this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages[j] != null) {
            		for (int i=0; i < this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages[j].m_objects.length-2 ;i++) {
                		Object object = this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages[j].m_objects[i];
                		if (object != null && object.toString().equals("!=")) {
                			Node obj1 = (Node) this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages[j].m_objects[i+1];
                			Node obj2 = (Node) this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages[j].m_objects[i+2];
                			this.m_metamodellingManager.differentIndividualsMap.putIfAbsent(obj1.m_nodeID, new ArrayList<Integer>());
                			this.m_metamodellingManager.differentIndividualsMap.get(obj1.m_nodeID).add(obj2.m_nodeID);
                		}
                    }
            	}
            }

            this.m_metamodellingManager.nodeProperties = new HashMap<Integer,Map<Integer, List<String>>>();
            for (int j=0; j<this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages.length; j++) {
            	if (this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages[j] != null) {
            		for (int i=0; i < this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages[j].m_objects.length-2 ;i++) {
            			Object property = this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages[j].m_objects[i];
    	    			if (property instanceof AtomicRole && (i + 2) <= this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages[j].m_objects.length) {
    	    				Node node1 = (Node) this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages[j].m_objects[i+1];
    	    				Node node2 = (Node) this.m_extensionManager.m_ternaryExtensionTable.m_tupleTable.m_pages[j].m_objects[i+2];
    	    				this.m_metamodellingManager.nodeProperties.putIfAbsent(node1.m_nodeID, new HashMap<Integer, List<String>>());
    	    				this.m_metamodellingManager.nodeProperties.get(node1.m_nodeID).putIfAbsent(node2.m_nodeID, new ArrayList<String>());
    	    				this.m_metamodellingManager.nodeProperties.get(node1.m_nodeID).get(node2.m_nodeID).add(property.toString());
    	    			}
                    }
            	}
            }

            BranchedHyperresolutionManager branchedHypM = new BranchedHyperresolutionManager();
            branchedHypM.setHyperresolutionManager(this.m_permanentHyperresolutionManager);
            branchedHypM.setBranchingIndex(this.getCurrentBranchingPointLevel());
            branchedHypM.setBranchingPoint(this.m_currentBranchingPoint);
            this.branchedHyperresolutionManagers.add(branchedHypM);

            this.branchedMetamodellingManagers = new ArrayList<BranchedMetamodellingManager>();
            BranchedMetamodellingManager branchedMetamodellingManager = new BranchedMetamodellingManager(this.m_metamodellingManager, this.m_currentBranchingPoint);
            this.branchedMetamodellingManagers.add(branchedMetamodellingManager);

            this.updateFlagsDependentOnAdditionalOntology();
            if (this.m_tableauMonitor != null) {
                this.m_tableauMonitor.setTableau(this);
            }
        }
        finally {
            this.m_interruptFlag.endTask();
        }
    }

    public Map<Integer, Individual> getMapNodeIndividual(){
    	return this.m_metamodellingManager.mapNodeIndividual;
    }

    public Map<Integer, Individual> getNodeToMetaIndividual(){
    	return this.m_metamodellingManager.nodeToMetaIndividual;
    }

    public List<Node> getMetamodellingNodes() {
		return this.m_metamodellingManager.metamodellingNodes;
	}

	public void setMetamodellingNodes(List<Node> metamodellingNodes) {
		this.m_metamodellingManager.metamodellingNodes = metamodellingNodes;
	}

    public int getM_currentBranchingPoint() {
		return m_currentBranchingPoint;
	}

    public ArrayList<BranchedHyperresolutionManager> getBranchedHyperresolutionManagers() {
		return branchedHyperresolutionManagers;
	}

	public void setBranchedHyperresolutionManagers(
			ArrayList<BranchedHyperresolutionManager> branchedHyperresolutionManagers) {
		this.branchedHyperresolutionManagers = branchedHyperresolutionManagers;
	}

	public InterruptFlag getInterruptFlag() {
        return this.m_interruptFlag;
    }

    public DLOntology getPermanentDLOntology() {
        return this.m_permanentDLOntology;
    }

    public DLOntology getAdditionalDLOntology() {
        return this.m_additionalDLOntology;
    }

    public Map<String, Object> getParameters() {
        return this.m_parameters;
    }

    public TableauMonitor getTableauMonitor() {
        return this.m_tableauMonitor;
    }

    public ExistentialExpansionStrategy getExistentialsExpansionStrategy() {
        return this.m_existentialExpansionStrategy;
    }

    public boolean isDeterministic() {
        return this.m_permanentDLOntology.isHorn() && (this.m_additionalDLOntology == null || this.m_additionalDLOntology.isHorn()) && this.m_existentialExpansionStrategy.isDeterministic();
    }

    public DependencySetFactory getDependencySetFactory() {
        return this.m_dependencySetFactory;
    }

    public ExtensionManager getExtensionManager() {
        return this.m_extensionManager;
    }

    public HyperresolutionManager getPermanentHyperresolutionManager() {
        return this.m_permanentHyperresolutionManager;
    }

    public void setPermanentHyperresolutionManager(HyperresolutionManager hypM) {
    	this.m_permanentHyperresolutionManager = hypM;
    }

    public HyperresolutionManager getAdditionalHyperresolutionManager() {
        return this.m_additionalHyperresolutionManager;
    }

    public MergingManager getMergingManager() {
        return this.m_mergingManager;
    }

    public ExistentialExpansionManager getExistentialExpansionManager() {
        return this.m_existentialExpasionManager;
    }

    public NominalIntroductionManager getNominalIntroductionManager() {
        return this.m_nominalIntroductionManager;
    }

    public DescriptionGraphManager getDescriptionGraphManager() {
        return this.m_descriptionGraphManager;
    }

    public void clear() {
        this.m_allocatedNodes = 0;
        this.m_numberOfNodesInTableau = 0;
        this.m_numberOfMergedOrPrunedNodes = 0;
        this.m_numberOfNodeCreations = 0;
        this.m_firstFreeNode = null;
        this.m_firstTableauNode = null;
        this.m_lastTableauNode = null;
        this.m_lastMergedOrPrunedNode = null;
        this.m_firstGroundDisjunction = null;
        this.m_firstUnprocessedGroundDisjunction = null;
        this.m_branchingPoints = new BranchingPoint[2];
        this.m_currentBranchingPoint = -1;
        this.m_nonbacktrackableBranchingPoint = -1;
        this.m_dependencySetFactory.clear();
        this.m_extensionManager.clear();
        this.m_clashManager.clear();
        this.m_permanentHyperresolutionManager.clear();
        if (this.m_additionalHyperresolutionManager != null) {
            this.m_additionalHyperresolutionManager.clear();
        }
        this.m_mergingManager.clear();
        this.m_existentialExpasionManager.clear();
        this.m_nominalIntroductionManager.clear();
        this.m_descriptionGraphManager.clear();
        this.m_isCurrentModelDeterministic = true;
        this.m_existentialExpansionStrategy.clear();
        this.m_datatypeManager.clear();
        this.m_existentialConceptsBuffers.clear();
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.tableauCleared();
        }
    }

    public boolean supportsAdditionalDLOntology(DLOntology additionalDLOntology) {
        boolean hasInverseRoles = this.m_permanentDLOntology.hasInverseRoles() || this.m_additionalDLOntology != null && this.m_additionalDLOntology.hasInverseRoles();
        boolean hasNominals = this.m_permanentDLOntology.hasNominals() || this.m_additionalDLOntology != null && this.m_additionalDLOntology.hasNominals();
        boolean isHorn = this.m_permanentDLOntology.isHorn() || this.m_additionalDLOntology != null && this.m_additionalDLOntology.isHorn();
        boolean permanentHasBottomObjectProperty = this.m_permanentDLOntology.containsObjectRole(AtomicRole.BOTTOM_OBJECT_ROLE);
        boolean hasBottomObjectProperty = permanentHasBottomObjectProperty || this.m_additionalDLOntology != null && this.m_additionalDLOntology.containsObjectRole(AtomicRole.BOTTOM_OBJECT_ROLE);
        if (!additionalDLOntology.getAllDescriptionGraphs().isEmpty() || additionalDLOntology.hasInverseRoles() && !hasInverseRoles || additionalDLOntology.hasNominals() && !hasNominals || !additionalDLOntology.isHorn() && isHorn || hasBottomObjectProperty && !permanentHasBottomObjectProperty) {
            return false;
        }
        for (DLClause dlClause : additionalDLOntology.getDLClauses()) {
            if (!dlClause.isAtomicRoleInclusion() && !dlClause.isAtomicRoleInverseInclusion() && !dlClause.isFunctionalityAxiom() && !dlClause.isInverseFunctionalityAxiom()) continue;
            return false;
        }
        return true;
    }

    public void setAdditionalDLOntology(DLOntology additionalDLOntology) {
        if (!this.supportsAdditionalDLOntology(additionalDLOntology)) {
            throw new IllegalArgumentException("Additional DL-ontology contains features that are incompatible with this tableau.");
        }
        this.m_additionalDLOntology = additionalDLOntology;
        this.m_additionalHyperresolutionManager = new HyperresolutionManager(this, this.m_additionalDLOntology.getDLClauses());
        this.m_existentialExpansionStrategy.additionalDLOntologySet(this.m_additionalDLOntology);
        this.m_datatypeManager.additionalDLOntologySet(this.m_additionalDLOntology);
        this.updateFlagsDependentOnAdditionalOntology();
    }

    public void clearAdditionalDLOntology() {
        this.m_additionalDLOntology = null;
        this.m_additionalHyperresolutionManager = null;
        this.m_existentialExpansionStrategy.additionalDLOntologyCleared();
        this.m_datatypeManager.additionalDLOntologyCleared();
        this.updateFlagsDependentOnAdditionalOntology();
    }

    private void updateFlagsDependentOnAdditionalOntology() {
        this.m_needsThingExtension = this.m_permanentHyperresolutionManager.m_tupleConsumersByDeltaPredicate.containsKey(AtomicConcept.THING);
        this.m_needsNamedExtension = this.m_permanentHyperresolutionManager.m_tupleConsumersByDeltaPredicate.containsKey(AtomicConcept.INTERNAL_NAMED);
        this.m_needsRDFSLiteralExtension = this.m_permanentHyperresolutionManager.m_tupleConsumersByDeltaPredicate.containsKey(InternalDatatype.RDFS_LITERAL);
        this.m_checkDatatypes = this.m_permanentDLOntology.hasDatatypes();
        this.m_checkUnknownDatatypeRestrictions = this.m_permanentDLOntology.hasUnknownDatatypeRestrictions();
        if (this.m_additionalHyperresolutionManager != null) {
            this.m_needsThingExtension |= this.m_additionalHyperresolutionManager.m_tupleConsumersByDeltaPredicate.containsKey(AtomicConcept.THING);
            this.m_needsNamedExtension |= this.m_additionalHyperresolutionManager.m_tupleConsumersByDeltaPredicate.containsKey(AtomicConcept.INTERNAL_NAMED);
            this.m_needsRDFSLiteralExtension |= this.m_additionalHyperresolutionManager.m_tupleConsumersByDeltaPredicate.containsKey(InternalDatatype.RDFS_LITERAL);
        }
        if (this.m_additionalDLOntology != null) {
            this.m_checkDatatypes |= this.m_additionalDLOntology.hasDatatypes();
            this.m_checkUnknownDatatypeRestrictions |= this.m_additionalDLOntology.hasUnknownDatatypeRestrictions();
        }
    }

    public boolean isSatisfiable(boolean loadPermanentABox, boolean loadAdditionalABox, Set<Atom> perTestPositiveFactsNoDependency, Set<Atom> perTestNegativeFactsNoDependency, Set<Atom> perTestPositiveFactsDummyDependency, Set<Atom> perTestNegativeFactsDummyDependency, Map<Term, Node> termsToNodes, Map<Individual, Node> nodesForIndividuals, ReasoningTaskDescription reasoningTaskDescription) {
        System.out.println("=== isSatisfiable START ===");
        System.out.println("loadPermanentABox: " + loadPermanentABox);
        System.out.println("loadAdditionalABox: " + loadAdditionalABox);
        System.out.println("perTestPositiveFactsNoDependency: " + (perTestPositiveFactsNoDependency != null ? perTestPositiveFactsNoDependency.size() + " facts" : "null"));
        System.out.println("perTestNegativeFactsNoDependency: " + (perTestNegativeFactsNoDependency != null ? perTestNegativeFactsNoDependency.size() + " facts" : "null"));

        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.isSatisfiableStarted(reasoningTaskDescription);
        }
        this.clear();
        // Obs: Aca se agregan nodos para cada axioma de metamodelado
        for (OWLMetamodellingAxiom metamodellingAxiom : this.m_permanentDLOntology.getMetamodellingAxioms()) {
        	System.out.println("Processing metamodelling axiom: " + metamodellingAxiom);
        	Individual ind = Individual.create(metamodellingAxiom.getMetamodelIndividual().toStringID());
        	System.out.println("Created individual: " + ind);

        	if (!termsToNodes.containsKey(ind)) {
        		Node node = this.createNewNamedNode(this.m_dependencySetFactory.emptySet());
        		System.out.println("Created new node: " + node.m_nodeID + " for individual: " + ind);
            	termsToNodes.put(ind, node);
        	}
        	m_metamodellingManager.nodeToMetaIndividual.put(termsToNodes.get(ind).m_nodeID, ind);
        	m_metamodellingManager.mapNodeIndividual.put(termsToNodes.get(ind).m_nodeID, ind);
        	m_metamodellingManager.mapNodeIdtoNodes.put(termsToNodes.get(ind).m_nodeID, termsToNodes.get(ind));
        	m_metamodellingManager.metamodellingNodes.add(termsToNodes.get(ind));
        	System.out.println("Added node " + termsToNodes.get(ind).m_nodeID + " to metamodelling manager");
        }
        if (loadPermanentABox) {
            System.out.println("Loading permanent ABox - positive facts: " + this.m_permanentDLOntology.getPositiveFacts().size() + ", negative facts: " + this.m_permanentDLOntology.getNegativeFacts().size());
            for (Atom atom : this.m_permanentDLOntology.getPositiveFacts()) {
                this.loadPositiveFact(termsToNodes, atom, this.m_dependencySetFactory.emptySet());
            }
            for (Atom atom : this.m_permanentDLOntology.getNegativeFacts()) {
                this.loadNegativeFact(termsToNodes, atom, this.m_dependencySetFactory.emptySet());
            }
        }
        if (loadAdditionalABox && this.m_additionalDLOntology != null) {
            for (Atom atom : this.m_additionalDLOntology.getPositiveFacts()) {
                this.loadPositiveFact(termsToNodes, atom, this.m_dependencySetFactory.emptySet());
            }
            for (Atom atom : this.m_additionalDLOntology.getNegativeFacts()) {
                this.loadNegativeFact(termsToNodes, atom, this.m_dependencySetFactory.emptySet());
            }
        }
        if (perTestPositiveFactsNoDependency != null && !perTestPositiveFactsNoDependency.isEmpty()) {
            System.out.println("Loading per-test positive facts: " + perTestPositiveFactsNoDependency.size());
            for (Atom atom : perTestPositiveFactsNoDependency) {
                System.out.println("Loading positive fact: " + atom);
                this.loadPositiveFact(termsToNodes, atom, this.m_dependencySetFactory.emptySet());
            }
        }
        if (perTestNegativeFactsNoDependency != null && !perTestNegativeFactsNoDependency.isEmpty()) {
            System.out.println("Loading per-test negative facts: " + perTestNegativeFactsNoDependency.size());
            for (Atom atom : perTestNegativeFactsNoDependency) {
                System.out.println("Loading negative fact: " + atom);
                this.loadNegativeFact(termsToNodes, atom, this.m_dependencySetFactory.emptySet());
            }
        }
        if (perTestPositiveFactsDummyDependency != null && !perTestPositiveFactsDummyDependency.isEmpty() || perTestNegativeFactsDummyDependency != null && !perTestNegativeFactsDummyDependency.isEmpty()) {
            this.m_branchingPoints[0] = new BranchingPoint(this);
            ++this.m_currentBranchingPoint;
            this.m_nonbacktrackableBranchingPoint = this.m_currentBranchingPoint;
            PermanentDependencySet dependencySet = this.m_dependencySetFactory.addBranchingPoint(this.m_dependencySetFactory.emptySet(), this.m_currentBranchingPoint);
            if (perTestPositiveFactsDummyDependency != null && !perTestPositiveFactsDummyDependency.isEmpty()) {
                for (Atom atom : perTestPositiveFactsDummyDependency) {
                    this.loadPositiveFact(termsToNodes, atom, dependencySet);
                }
            }
            if (perTestNegativeFactsDummyDependency != null && !perTestNegativeFactsDummyDependency.isEmpty()) {
                for (Atom atom : perTestNegativeFactsDummyDependency) {
                    this.loadNegativeFact(termsToNodes, atom, dependencySet);
                }
            }
        }
        if (nodesForIndividuals != null) {
            for (Map.Entry<Individual, Node> entry : nodesForIndividuals.entrySet()) {
                if (termsToNodes.get(entry.getKey()) == null) {
                    Atom topAssertion = Atom.create(AtomicConcept.THING, entry.getKey());
                    this.loadPositiveFact(termsToNodes, topAssertion, this.m_dependencySetFactory.emptySet());
                }
                entry.setValue(termsToNodes.get(entry.getKey()));
            }
        }
        if (this.m_firstTableauNode == null) {
            System.out.println("Creating initial NI node");
            this.createNewNINode(this.m_dependencySetFactory.emptySet());
        }

        boolean result = this.runCalculus();

        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.isSatisfiableFinished(reasoningTaskDescription, result);
        }
        return result;
    }

    private void loadPositiveFact(Map<Term, Node> termsToNodes, Atom atom, DependencySet dependencySet) {
        DLPredicate dlPredicate = atom.getDLPredicate();
        if (dlPredicate instanceof LiteralConcept) {
            this.m_extensionManager.addConceptAssertion((LiteralConcept) dlPredicate, this.getNodeForTerm(termsToNodes, atom.getArgument(0), dependencySet), dependencySet, true);
        } else if (dlPredicate instanceof AtomicRole || Equality.INSTANCE.equals(dlPredicate) || Inequality.INSTANCE.equals(dlPredicate)) {
        	this.m_extensionManager.addAssertion(dlPredicate, this.getNodeForTerm(termsToNodes, atom.getArgument(0), dependencySet), this.getNodeForTerm(termsToNodes, atom.getArgument(1), dependencySet), dependencySet, true);
        } else if (dlPredicate instanceof DescriptionGraph) {
            DescriptionGraph descriptionGraph = (DescriptionGraph)dlPredicate;
            Object[] tuple = new Object[descriptionGraph.getArity() + 1];
            tuple[0] = descriptionGraph;
            for (int argumentIndex = 0; argumentIndex < descriptionGraph.getArity(); ++argumentIndex) {
                tuple[argumentIndex + 1] = this.getNodeForTerm(termsToNodes, atom.getArgument(argumentIndex), dependencySet);
            }
            this.m_extensionManager.addTuple(tuple, dependencySet, true);
        } else {
            throw new IllegalArgumentException("Unsupported type of positive ground atom.");
        }
    }

    private void loadNegativeFact(Map<Term, Node> termsToNodes, Atom atom, DependencySet dependencySet) {
        DLPredicate dlPredicate = atom.getDLPredicate();
        if (dlPredicate instanceof LiteralConcept) {
            this.m_extensionManager.addConceptAssertion(((LiteralConcept) dlPredicate).getNegation(), this.getNodeForTerm(termsToNodes, atom.getArgument(0), dependencySet), dependencySet, true);
        } else if (dlPredicate instanceof AtomicRole) {
            Object[] ternaryTuple = this.m_extensionManager.m_ternaryAuxiliaryTupleAdd;
            ternaryTuple[0] = NegatedAtomicRole.create((AtomicRole)dlPredicate);
            ternaryTuple[1] = this.getNodeForTerm(termsToNodes, atom.getArgument(0), dependencySet);
            ternaryTuple[2] = this.getNodeForTerm(termsToNodes, atom.getArgument(1), dependencySet);
            this.m_extensionManager.addTuple(ternaryTuple, dependencySet, true);
        } else if (Equality.INSTANCE.equals(dlPredicate)) {
            this.m_extensionManager.addAssertion(Inequality.INSTANCE, this.getNodeForTerm(termsToNodes, atom.getArgument(0), dependencySet), this.getNodeForTerm(termsToNodes, atom.getArgument(1), dependencySet), dependencySet, true);
        } else if (Inequality.INSTANCE.equals(dlPredicate)) {
            this.m_extensionManager.addAssertion(Equality.INSTANCE, this.getNodeForTerm(termsToNodes, atom.getArgument(0), dependencySet), this.getNodeForTerm(termsToNodes, atom.getArgument(1), dependencySet), dependencySet, true);
        } else {
            throw new IllegalArgumentException("Unsupported type of negative ground atom.");
        }
    }

    private Node getNodeForTerm(Map<Term, Node> termsToNodes, Term term, DependencySet dependencySet) {
        Node node = termsToNodes.get(term);
        if (node == null) {
            if (term instanceof Individual) {
                Individual individual = (Individual)term;
                node = individual.isAnonymous() ? this.createNewNINode(dependencySet) : this.createNewNamedNode(dependencySet);
                m_metamodellingManager.mapNodeIndividual.put(node.m_nodeID, (Individual) term);
                m_metamodellingManager.mapNodeIdtoNodes.put(node.m_nodeID, node);
            } else {
                Constant constant = (Constant)term;
                node = this.createNewRootConstantNode(dependencySet);
                if (!constant.isAnonymous()) {
                    this.m_extensionManager.addAssertion(ConstantEnumeration.create(new Constant[]{constant}), node, dependencySet, true);
                }
            }
            termsToNodes.put(term, node);
        }
        return node.getCanonicalNode();
    }

    boolean runCalculus() {
    	System.out.println("=== runCalculus START ===");
    	int iterations = 0;
        this.m_interruptFlag.startTask();
        try {
            boolean existentialsAreExact = this.m_existentialExpansionStrategy.isExact();
            if (this.m_tableauMonitor != null) {
                this.m_tableauMonitor.saturateStarted();
            }
            boolean hasMoreWork = true;
            while (hasMoreWork) {
                iterations++;
                if (this.m_tableauMonitor != null) {
                    this.m_tableauMonitor.iterationStarted();
                }
                hasMoreWork = this.doIteration();
                if (this.m_tableauMonitor != null) {
                    this.m_tableauMonitor.iterationFinished();
                }
                if (existentialsAreExact || hasMoreWork || this.m_extensionManager.containsClash()) continue;
                if (this.m_tableauMonitor != null) {
                    this.m_tableauMonitor.iterationStarted();
                }
                hasMoreWork = this.m_existentialExpansionStrategy.expandExistentials(true);
                System.out.println("Final existential expansion result: " + hasMoreWork);
                if (this.m_tableauMonitor == null) continue;
                this.m_tableauMonitor.iterationFinished();
            }
            if (this.m_tableauMonitor != null) {
                this.m_tableauMonitor.saturateFinished(!this.m_extensionManager.containsClash());
            }
            if (!this.m_extensionManager.containsClash()) {
                System.out.println("No clash found - model found - SATISFIABLE");
                this.m_existentialExpansionStrategy.modelFound();
                return true;
            }
            return false;
        }
        finally {
            this.m_interruptFlag.endTask();
        }
    }

    boolean doIteration() {
        System.out.println("=== doIteration START ===");
        System.out.println("Contains clash: " + this.m_extensionManager.containsClash());

//        Hace esto si no hay ninguna contradiccion
//        Si hay alguna contradicción, se fija si hay otra rama para cambiar
        if (!this.m_extensionManager.containsClash()) {
            System.out.println("No clash detected, proceeding with iteration");
            this.m_nominalIntroductionManager.processAnnotatedEqualities();
            boolean hasChange = false;
            while (this.m_extensionManager.propagateDeltaNew() && !this.m_extensionManager.containsClash()) {
                System.out.println("Delta propagated, checking constraints...");
                if (this.m_hasDescriptionGraphs && !this.m_extensionManager.containsClash()) {
                    this.m_descriptionGraphManager.checkGraphConstraints();
                }
                if (!this.m_extensionManager.containsClash()) {
                	this.m_permanentHyperresolutionManager.applyDLClauses();
                	System.out.println("Applied DL clauses, clash status: " + this.m_extensionManager.containsClash());
                }
                if (this.m_additionalHyperresolutionManager != null && !this.m_extensionManager.containsClash()) {
                    this.m_additionalHyperresolutionManager.applyDLClauses();
                }
                if (this.m_checkUnknownDatatypeRestrictions && !this.m_extensionManager.containsClash()) {
                    this.m_datatypeManager.applyUnknownDatatypeRestrictionSemantics();
                }
                if (this.m_checkDatatypes && !this.m_extensionManager.containsClash()) {
                    this.m_datatypeManager.checkDatatypeConstraints();
                }
                if (!this.m_extensionManager.containsClash()) {
                    this.m_nominalIntroductionManager.processAnnotatedEqualities();
                    if (this.metamodellingFlag) {
                    	System.out.println("Checking metamodeling rules...");
                    	boolean equalMetamodellingRuleApplied = this.m_metamodellingManager.checkEqualMetamodellingRule();
                    	boolean inequalityMetamodellingRuleApplied = this.m_metamodellingManager.checkInequalityMetamodellingRule();
                    	this.metamodellingFlag = false;
                    }
                    if(this.m_metamodellingManager.checkPropertyNegation()) {
                    	System.out.println("Property negation check returned true, returning from iteration");
                    	return true;
                    }
                    if (MetamodellingAxiomHelper.findCyclesInM(this)) {
                    	System.out.println("Cycles found in metamodeling, setting clash");
                    	DependencySet clashDependencySet = this.m_dependencySetFactory.getActualDependencySet();
                    	this.m_extensionManager.setClash(clashDependencySet);
                    	return true;
                    }
                }
                hasChange = true;
            }
            System.out.println("Delta propagation loop ended, hasChange: " + hasChange + ", clash: " + this.m_extensionManager.containsClash());
            if (hasChange) {
                return true;
            }
        }
        if (!this.m_extensionManager.containsClash() && this.m_existentialExpansionStrategy.expandExistentials(false)) {
            System.out.println("Existentials expanded, returning true");
            return true;
        }
        if (!this.m_extensionManager.containsClash()) {
        	System.out.println("Checking close metamodeling rule...");
        	this.m_metamodellingManager.checkCloseMetamodellingRule();
        	while (this.m_firstUnprocessedGroundDisjunction != null) {
        		System.out.println("Processing ground disjunction...");
        		GroundDisjunction groundDisjunction = this.m_firstUnprocessedGroundDisjunction;
        		if (this.m_tableauMonitor != null) {
        			this.m_tableauMonitor.processGroundDisjunctionStarted(groundDisjunction);
        		}
        		this.m_firstUnprocessedGroundDisjunction = groundDisjunction.m_previousGroundDisjunction;
        		if (!groundDisjunction.isPruned() && !groundDisjunction.isSatisfied(this)) {
        			int[] sortedDisjunctIndexes = groundDisjunction.getGroundDisjunctionHeader().getSortedDisjunctIndexes();
        			DependencySet dependencySet = groundDisjunction.getDependencySet();
        			if (groundDisjunction.getNumberOfDisjuncts() > 1) {
        				DisjunctionBranchingPoint branchingPoint = new DisjunctionBranchingPoint(this, groundDisjunction, sortedDisjunctIndexes);
        				this.pushBranchingPoint(branchingPoint);
        				dependencySet = this.m_dependencySetFactory.addBranchingPoint(dependencySet, branchingPoint.getLevel());
        			}
        			if (this.m_tableauMonitor != null) {
        				this.m_tableauMonitor.disjunctProcessingStarted(groundDisjunction, sortedDisjunctIndexes[0]);
        			}
        			groundDisjunction.addDisjunctToTableau(this, sortedDisjunctIndexes[0], dependencySet);
        			if (this.m_tableauMonitor != null) {
        				this.m_tableauMonitor.disjunctProcessingFinished(groundDisjunction, sortedDisjunctIndexes[0]);
        				this.m_tableauMonitor.processGroundDisjunctionFinished(groundDisjunction);
        			}
        			return true;
        		}
        		if (this.m_tableauMonitor != null) {
        			this.m_tableauMonitor.groundDisjunctionSatisfied(groundDisjunction);
        		}
        		this.m_interruptFlag.checkInterrupt();
        	}
        }
        if (this.m_extensionManager.containsClash()) {
        	System.out.println("=== CLASH DETECTED ===");
        	DependencySet clashDependencySet = this.m_extensionManager.getClashDependencySet();
    		int newCurrentBranchingPoint = clashDependencySet.getMaximumBranchingPoint();
<<<<<<< HEAD

            if (newCurrentBranchingPoint <= this.m_nonbacktrackableBranchingPoint || this.m_branchingPoints[newCurrentBranchingPoint] == null) {
                boolean backtrackedMetamodelling = false;

                if (shouldBacktrackHyperresolutionManager()) {
                	System.out.println("Backtracking hyperresolution manager...");
    	    		backtrackHyperresolutionManager();
                    backtrackedMetamodelling = backtrackMetamodellingClash();
                }

=======

            if (newCurrentBranchingPoint <= this.m_nonbacktrackableBranchingPoint || this.m_branchingPoints[newCurrentBranchingPoint] == null) {
                boolean backtrackedMetamodelling = false;

                if (shouldBacktrackHyperresolutionManager()) {
    	    		backtrackHyperresolutionManager();
                    backtrackedMetamodelling = backtrackMetamodellingClash();
                }

>>>>>>> 41d188e (Add possibility of backtracking more than one branching point)
                if (backtrackedMetamodelling) return true;

                if (this.m_currentBranchingPoint < 0) {
                    return false;
                }

                if (this.m_branchingPoints[this.m_currentBranchingPoint].canStartNextChoice()) {
                    newCurrentBranchingPoint = this.m_currentBranchingPoint;
                } else {
                    newCurrentBranchingPoint = findPreviousBranchingPointWithOptions();
                    if (newCurrentBranchingPoint == -1) {
                        return false;
                    }
                }
    		}
    		System.out.println("Backtracking to level: " + newCurrentBranchingPoint);
    		this.backtrackTo(newCurrentBranchingPoint);
    		BranchingPoint branchingPoint = this.getCurrentBranchingPoint();
    		if (this.m_tableauMonitor != null) {
    		    this.m_tableauMonitor.startNextBranchingPointStarted(branchingPoint);
    		}
    		branchingPoint.startNextChoice(this, clashDependencySet);
    		if (this.m_tableauMonitor != null) {
    		    this.m_tableauMonitor.startNextBranchingPointFinished(branchingPoint);
    		}
    		this.m_dependencySetFactory.removeUnusedSets();
    		System.out.println("Completed backtracking, returning true");
    		return true;
        }
        return false;
    }

    public Set<Node> getClassInstances(String className) {
    	Set<Node> instances = new HashSet<Node>();
    	Atom classAtom = Atom.create(AtomicConcept.create(className.substring(1, className.length()-1)), Variable.create("X"));
    	DLPredicate dlPredicate = classAtom.getDLPredicate();
    	for (int nodeId : this.m_metamodellingManager.mapNodeIdtoNodes.keySet()) {
    		if (this.getExtensionManager().containsAssertion(dlPredicate, m_metamodellingManager.mapNodeIdtoNodes.get(nodeId))) {
    			instances.add(m_metamodellingManager.mapNodeIdtoNodes.get(nodeId));
    		}
    	}
    	return instances;
    }

    List<Node> getRelatedNodes(Node node, String property) {
    	Set<Node> relatedNodes = new HashSet<Node>();
    	if (this.m_metamodellingManager.nodeProperties.containsKey(node.m_nodeID)) {
    		for (Integer node2 : this.m_metamodellingManager.nodeProperties.get(node.m_nodeID).keySet()) {
    			for (String propertyIter : this.m_metamodellingManager.nodeProperties.get(node.m_nodeID).get(node2)) {
    				if (propertyIter.equals(property)) {
    					for (Node metamodellingNode : this.m_metamodellingManager.metamodellingNodes) {
    						if (metamodellingNode.m_nodeID == node2 || metamodellingNode.getCanonicalNode().m_nodeID == node2) {
    							relatedNodes.add(metamodellingNode);
    						}
    					}
    				}
    			}
    		}
    	}
    	if (this.m_metamodellingManager.nodeProperties.containsKey(node.getCanonicalNode().m_nodeID)) {
    		for (Integer node2 : this.m_metamodellingManager.nodeProperties.get(node.getCanonicalNode().m_nodeID).keySet()) {
    			for (String propertyIter : this.m_metamodellingManager.nodeProperties.get(node.getCanonicalNode().m_nodeID).get(node2)) {
    				if (propertyIter.equals(property)) {
    					for (Node metamodellingNode : this.m_metamodellingManager.metamodellingNodes) {
    						if (metamodellingNode.m_nodeID == node2 || metamodellingNode.getCanonicalNode().m_nodeID == node2) {
    							relatedNodes.add(metamodellingNode);
    						}
    					}
    				}
    			}
    		}
    	}

    	return new ArrayList<Node>(relatedNodes);
    }

	public boolean startBacktracking(GroundDisjunction groundDisjunction) {
		if (this.m_tableauMonitor != null) {
		    this.m_tableauMonitor.processGroundDisjunctionStarted(groundDisjunction);
		}
		this.m_firstUnprocessedGroundDisjunction = groundDisjunction.m_previousGroundDisjunction;
		if (!groundDisjunction.isPruned() && !groundDisjunction.isSatisfied(this)) {
		    int[] sortedDisjunctIndexes = groundDisjunction.getGroundDisjunctionHeader().getSortedDisjunctIndexes();
		    DependencySet dependencySet = groundDisjunction.getDependencySet();
		    if (groundDisjunction.getNumberOfDisjuncts() > 1) {
		        DisjunctionBranchingPoint branchingPoint = new DisjunctionBranchingPoint(this, groundDisjunction, sortedDisjunctIndexes);
		        this.pushBranchingPoint(branchingPoint);
		        dependencySet = this.m_dependencySetFactory.addBranchingPoint(dependencySet, branchingPoint.getLevel());
		    }
		    if (this.m_tableauMonitor != null) {
		        this.m_tableauMonitor.disjunctProcessingStarted(groundDisjunction, sortedDisjunctIndexes[0]);
		    }
		    groundDisjunction.addDisjunctToTableau(this, sortedDisjunctIndexes[0], dependencySet);
		    if (this.m_tableauMonitor != null) {
		        this.m_tableauMonitor.disjunctProcessingFinished(groundDisjunction, sortedDisjunctIndexes[0]);
		        this.m_tableauMonitor.processGroundDisjunctionFinished(groundDisjunction);
		    }
		    return true;
		}
		if (this.m_tableauMonitor != null) {
		    this.m_tableauMonitor.groundDisjunctionSatisfied(groundDisjunction);
		}
		this.m_interruptFlag.checkInterrupt();
		return false;
	}

    private boolean shouldBacktrackHyperresolutionManager() {
        if (this.m_extensionManager.containsClash() && this.branchedHyperresolutionManagers.size() > 1 && this.m_branchingPoints[0] != null) {
            return this.branchedHyperresolutionManagers.get(this.branchedHyperresolutionManagers.size() - 1).getBranchingPoint() <= this.m_currentBranchingPoint;
        }
    	return false;
    }

	private void backtrackHyperresolutionManager() {
		for (int i=1; i<this.branchedHyperresolutionManagers.size(); i++) {
			if (this.branchedHyperresolutionManagers.get(this.branchedHyperresolutionManagers.size()-i).getBranchingPoint() == this.m_currentBranchingPoint) {
				for (int j=0; j<this.branchedHyperresolutionManagers.get(this.branchedHyperresolutionManagers.size()-i).getDlClausesAdded().size(); j++) {
					DLClause dlClauseAdded = this.branchedHyperresolutionManagers.get(this.branchedHyperresolutionManagers.size()-i).getDlClausesAdded().get(j);
					removeFromInequalityMetamodellingPairs(i, j, dlClauseAdded);
					this.getPermanentDLOntology().getDLClauses().remove(dlClauseAdded);
					this.setPermanentHyperresolutionManager(this.branchedHyperresolutionManagers.get(this.branchedHyperresolutionManagers.size()-i).getHyperresolutionManager());
				}
			}
		}
	}

	private void removeFromInequalityMetamodellingPairs(int i, int j, DLClause dlClauseAdded) {
		if (dlClauseAdded.isGeneralConceptInclusion() && dlClauseAdded.getHeadLength() == 2 && dlClauseAdded.getBodyLength() == 1) {
			if (dlClauseAdded.getBodyAtom(0).toString().contains(MetamodellingAxiomHelper.DEF_STRING) &&
					j+2 < this.branchedHyperresolutionManagers.get(this.branchedHyperresolutionManagers.size()-i).getDlClausesAdded().size()) {
				DLClause dlClause1 = this.branchedHyperresolutionManagers.get(this.branchedHyperresolutionManagers.size()-i).getDlClausesAdded().get(j+1);
				DLClause dlClause2 = this.branchedHyperresolutionManagers.get(this.branchedHyperresolutionManagers.size()-i).getDlClausesAdded().get(j+2);
				String class1 = null;
				String class2 = null;
				if (dlClause1.getHeadAtom(0).toString().contains(MetamodellingAxiomHelper.DEF_STRING)) {
					class1 = dlClause1.getBodyAtom(0).toString();
				} else {
					class1 = dlClause1.getHeadAtom(0).toString();
				}
				if (dlClause2.getHeadAtom(0).toString().contains(MetamodellingAxiomHelper.DEF_STRING)) {
					class2 = dlClause2.getBodyAtom(0).toString();
				} else {
					class2 = dlClause2.getHeadAtom(0).toString();
				}
				OWLClassExpression owlClassExpressionToRemove = null;
				for (OWLClassExpression classExpression1 : this.m_metamodellingManager.inequalityMetamodellingPairs.keySet()) {
					if (classExpression1.toString().equals(class1)) {
						for (OWLClassExpression classExpression2 : this.m_metamodellingManager.inequalityMetamodellingPairs.get(classExpression1).keySet()) {
							if (classExpression2.toString().equals(class2)) {
								owlClassExpressionToRemove = classExpression2;
							}
						}
						if (owlClassExpressionToRemove != null) {
							this.m_metamodellingManager.inequalityMetamodellingPairs.get(classExpression1).remove(owlClassExpressionToRemove);
							owlClassExpressionToRemove = null;
						}
					}
					if (classExpression1.toString().equals(class2)) {
						for (OWLClassExpression classExpression2 : this.m_metamodellingManager.inequalityMetamodellingPairs.get(classExpression1).keySet()) {
							if (classExpression2.toString().equals(class1)) {
								owlClassExpressionToRemove = classExpression2;
							}
						}
						if (owlClassExpressionToRemove != null) {
							this.m_metamodellingManager.defAssertions.remove(this.m_metamodellingManager.inequalityMetamodellingPairs.get(classExpression1).get(owlClassExpressionToRemove).getDLPredicate().toString());
							this.m_metamodellingManager.inequalityMetamodellingPairs.get(classExpression1).remove(owlClassExpressionToRemove);
							owlClassExpressionToRemove = null;
						}
					}
				}
			}
		}
	}

	private boolean backtrackMetamodellingClash() {
		this.m_existentialExpansionStrategy.backtrack();
		this.m_existentialExpasionManager.backtrack();
		this.m_nominalIntroductionManager.backtrack();
		this.m_extensionManager.backtrack();

		Node lastMergedOrPrunedNodeShouldBe = this.m_branchingPoints[this.m_currentBranchingPoint].m_lastMergedOrPrunedNode;
		while (this.m_lastMergedOrPrunedNode != lastMergedOrPrunedNodeShouldBe) {
		    this.backtrackLastMergedOrPrunedNode();
		}
		Node lastTableauNodeShouldBe = this.m_branchingPoints[this.m_currentBranchingPoint].m_lastTableauNode;
		while (lastTableauNodeShouldBe != this.m_lastTableauNode) {
		    this.destroyLastTableauNode();
		}

		if (this.m_branchingPoints[this.m_currentBranchingPoint].canStartNextChoice()) {
			this.m_branchingPoints[this.m_currentBranchingPoint].startNextChoice(this, this.m_extensionManager.getClashDependencySet());
		} else {
			return false;
		}

		this.m_extensionManager.clearClash();
		this.m_dependencySetFactory.removeUnusedSets();
		return true;
	}

    public boolean containsClassAssertion(String def) {
    	return this.m_metamodellingManager.defAssertions.contains(def);
    }

    boolean areDifferentIndividual(Node node1, Node node2) {
    	if (m_metamodellingManager.differentIndividualsMap.containsKey(node1.m_nodeID)) {
    		if (m_metamodellingManager.differentIndividualsMap.get(node1.m_nodeID).contains(node2.m_nodeID) || m_metamodellingManager.differentIndividualsMap.get(node1.m_nodeID).contains(node2.getCanonicalNode().m_nodeID)) {
    			return true;
    		}
    	}
    	if (m_metamodellingManager.differentIndividualsMap.containsKey(node2.m_nodeID)) {
    		if (m_metamodellingManager.differentIndividualsMap.get(node2.m_nodeID).contains(node1.m_nodeID) || m_metamodellingManager.differentIndividualsMap.get(node2.m_nodeID).contains(node1.getCanonicalNode().m_nodeID)) {
    			return true;
    		}
    	}
    	if (m_metamodellingManager.differentIndividualsMap.containsKey(node1.getCanonicalNode().m_nodeID)) {
    		if (m_metamodellingManager.differentIndividualsMap.get(node1.getCanonicalNode().m_nodeID).contains(node2.m_nodeID) || m_metamodellingManager.differentIndividualsMap.get(node1.getCanonicalNode().m_nodeID).contains(node2.getCanonicalNode().m_nodeID)) {
    			return true;
    		}
    	}
    	if (m_metamodellingManager.differentIndividualsMap.containsKey(node2.getCanonicalNode().m_nodeID)) {
            return m_metamodellingManager.differentIndividualsMap.get(node2.getCanonicalNode().m_nodeID).contains(node1.m_nodeID) || m_metamodellingManager.differentIndividualsMap.get(node2.getCanonicalNode().m_nodeID).contains(node1.getCanonicalNode().m_nodeID);
    	}
    	return false;
    }

    boolean areSameIndividual(Node node1, Node node2) {
    	if ((node1.m_nodeID == node2.m_nodeID) || (node1.getCanonicalNode() == node2.getCanonicalNode())) return true;
        return (node1.isMerged() && node1.m_mergedInto == node2) || (node2.isMerged() && node2.m_mergedInto == node1);
    }

    boolean alreadyCreateDisjunction(Node node0, Node node1) {
    	if (m_metamodellingManager.createdDisjunction.containsKey(node0.m_nodeID)) {
    		for (int nodeIter : m_metamodellingManager.createdDisjunction.get(node0.m_nodeID)) {
    			if (nodeIter == node1.m_nodeID) return true;
    		}
    	}
    	if (m_metamodellingManager.createdDisjunction.containsKey(node1.m_nodeID)) {
    		for (int nodeIter : m_metamodellingManager.createdDisjunction.get(node1.m_nodeID)) {
    			if (nodeIter == node0.m_nodeID) return true;
    		}
    	}
		return false;
	}

    void addCreatedDisjuntcion(Node node0, Node node1) {
    	if (!this.m_metamodellingManager.createdDisjunction.containsKey(node0.m_nodeID)) {
    		this.m_metamodellingManager.createdDisjunction.put(node0.m_nodeID, new ArrayList<Integer>());
    	}
    	this.m_metamodellingManager.createdDisjunction.get(node0.m_nodeID).add(node1.m_nodeID);
    }

    public boolean isCurrentModelDeterministic() {
        return this.m_isCurrentModelDeterministic;
    }

    public int getCurrentBranchingPointLevel() {
        return this.m_currentBranchingPoint;
    }

    public BranchingPoint getCurrentBranchingPoint() {
        return this.m_branchingPoints[this.m_currentBranchingPoint];
    }

    /**
     * Busca hacia atrás en los branching points hasta encontrar uno que tenga opciones disponibles
     * @return el índice del branching point con opciones disponibles, o -1 si no hay ninguno
     */
    private int findPreviousBranchingPointWithOptions() {
        int highestLevelChecked = this.m_branchingPoints[this.m_currentBranchingPoint].getLevel();
        for (int i = this.m_currentBranchingPoint - 1; i >= 0; i--) {
            if (this.m_branchingPoints[i] != null &&
                this.m_branchingPoints[i].getLevel() < highestLevelChecked &&
                this.m_branchingPoints[i].canStartNextChoice()) {
                return i;
            }

            if (this.m_branchingPoints[i] != null) {
                highestLevelChecked = Math.min(highestLevelChecked, this.m_branchingPoints[i].getLevel());
            }
        }
        return -1;
    }

    public void addGroundDisjunction(GroundDisjunction groundDisjunction) {
        groundDisjunction.m_nextGroundDisjunction = this.m_firstGroundDisjunction;
        groundDisjunction.m_previousGroundDisjunction = null;
        if (this.m_firstGroundDisjunction != null) {
            this.m_firstGroundDisjunction.m_previousGroundDisjunction = groundDisjunction;
        }
        this.m_firstGroundDisjunction = groundDisjunction;
        if (this.m_firstUnprocessedGroundDisjunction == null) {
            this.m_firstUnprocessedGroundDisjunction = groundDisjunction;
        }
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.groundDisjunctionDerived(groundDisjunction);
        }
    }

    public GroundDisjunction getFirstUnprocessedGroundDisjunction() {
        return this.m_firstUnprocessedGroundDisjunction;
    }

    public void pushBranchingPoint(BranchingPoint branchingPoint) {
        assert (this.m_currentBranchingPoint + 1 == branchingPoint.m_level);
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.pushBranchingPointStarted(branchingPoint);
        }
        ++this.m_currentBranchingPoint;
        if (this.m_currentBranchingPoint >= this.m_branchingPoints.length) {
            BranchingPoint[] newBranchingPoints = new BranchingPoint[this.m_currentBranchingPoint * 3 / 2];
            System.arraycopy(this.m_branchingPoints, 0, newBranchingPoints, 0, this.m_branchingPoints.length);
            this.m_branchingPoints = newBranchingPoints;
        }
        this.m_branchingPoints[this.m_currentBranchingPoint] = branchingPoint;
        this.m_extensionManager.branchingPointPushed();
        this.m_existentialExpasionManager.branchingPointPushed();
        this.m_existentialExpansionStrategy.branchingPointPushed();
        this.m_nominalIntroductionManager.branchingPointPushed();
        this.m_isCurrentModelDeterministic = false;
        if (this.shouldUseMetamodellingManager()) {
            this.addBranchedMetamodellingManager(branchingPoint);
        }
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.pushBranchingPointFinished(branchingPoint);
        }
    }

    void backtrackTo(int newCurrentBranchingPoint) {
        BranchingPoint branchingPoint = this.m_branchingPoints[newCurrentBranchingPoint];
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.backtrackToStarted(branchingPoint);
        }
        for (int index = newCurrentBranchingPoint + 1; index <= this.m_currentBranchingPoint; ++index) {
            this.m_branchingPoints[index] = null;
        }
        this.m_currentBranchingPoint = newCurrentBranchingPoint;


        this.m_firstUnprocessedGroundDisjunction = branchingPoint.m_firstUnprocessedGroundDisjunction;
        GroundDisjunction firstGroundDisjunctionShouldBe = branchingPoint.m_firstGroundDisjunction;
        while (this.m_firstGroundDisjunction != firstGroundDisjunctionShouldBe) {
            this.m_firstGroundDisjunction.destroy(this);
            this.m_firstGroundDisjunction = this.m_firstGroundDisjunction.m_nextGroundDisjunction;
        }
        if (this.m_firstGroundDisjunction != null) {
            this.m_firstGroundDisjunction.m_previousGroundDisjunction = null;
        }


        this.m_existentialExpansionStrategy.backtrack();
        this.m_existentialExpasionManager.backtrack();
        this.m_nominalIntroductionManager.backtrack();
        this.m_extensionManager.backtrack();
        Node lastMergedOrPrunedNodeShouldBe = branchingPoint.m_lastMergedOrPrunedNode;
        while (this.m_lastMergedOrPrunedNode != lastMergedOrPrunedNodeShouldBe) {
            this.backtrackLastMergedOrPrunedNode();
        }
        Node lastTableauNodeShouldBe = branchingPoint.m_lastTableauNode;
        while (lastTableauNodeShouldBe != this.m_lastTableauNode) {
            this.destroyLastTableauNode();
        }
        this.m_extensionManager.clearClash();
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.backtrackToFinished(branchingPoint);
        }
        if (this.shouldUseMetamodellingManager()) {
            this.backtrackMetamodellingManager(newCurrentBranchingPoint);
        }
    }

    public Node createNewNamedNode(DependencySet dependencySet) {
        return this.createNewNodeRaw(dependencySet, null, NodeType.NAMED_NODE, 0);
    }

    public Node createNewNINode(DependencySet dependencySet) {
        return this.createNewNodeRaw(dependencySet, null, NodeType.NI_NODE, 0);
    }

    public Node createNewTreeNode(DependencySet dependencySet, Node parent) {
        return this.createNewNodeRaw(dependencySet, parent, NodeType.TREE_NODE, parent.getTreeDepth() + 1);
    }

    public Node createNewConcreteNode(DependencySet dependencySet, Node parent) {
        return this.createNewNodeRaw(dependencySet, parent, NodeType.CONCRETE_NODE, parent.getTreeDepth() + 1);
    }

    public Node createNewRootConstantNode(DependencySet dependencySet) {
        return this.createNewNodeRaw(dependencySet, null, NodeType.ROOT_CONSTANT_NODE, 0);
    }

    public Node createNewGraphNode(Node parent, DependencySet dependencySet) {
        return this.createNewNodeRaw(dependencySet, parent, NodeType.GRAPH_NODE, parent == null ? 0 : parent.getTreeDepth());
    }

    private Node createNewNodeRaw(DependencySet dependencySet, Node parent, NodeType nodeType, int treeDepth) {
        Node node;
        if (this.m_firstFreeNode == null) {
            node = new Node(this);
            ++this.m_allocatedNodes;
        } else {
            node = this.m_firstFreeNode;
            this.m_firstFreeNode = this.m_firstFreeNode.m_nextTableauNode;
        }
        assert (node.m_nodeID == -1);
        assert (node.m_nodeState == null);
        node.initialize(++this.m_numberOfNodesInTableau, parent, nodeType, treeDepth);
        this.m_existentialExpansionStrategy.nodeInitialized(node);
        node.m_previousTableauNode = this.m_lastTableauNode;
        if (this.m_lastTableauNode == null) {
            this.m_firstTableauNode = node;
        } else {
            this.m_lastTableauNode.m_nextTableauNode = node;
        }
        this.m_lastTableauNode = node;
        this.m_existentialExpansionStrategy.nodeStatusChanged(node);
        ++this.m_numberOfNodeCreations;
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.nodeCreated(node);
        }
        if (nodeType.m_isAbstract) {
            this.m_extensionManager.addConceptAssertion(AtomicConcept.THING, node, dependencySet, true);
            if (nodeType == NodeType.NAMED_NODE && this.m_needsNamedExtension) {
                this.m_extensionManager.addConceptAssertion(AtomicConcept.INTERNAL_NAMED, node, dependencySet, true);
            }
        } else {
            this.m_extensionManager.addDataRangeAssertion(InternalDatatype.RDFS_LITERAL, node, dependencySet, true);
        }
        return node;
    }

    public void mergeNode(Node node, Node mergeInto, DependencySet dependencySet) {
        assert (node.m_nodeState == Node.NodeState.ACTIVE);
        assert (node.m_mergedInto == null);
        assert (node.m_mergedIntoDependencySet == null);
        assert (node.m_previousMergedOrPrunedNode == null);
        node.m_mergedInto = mergeInto;
        node.m_mergedIntoDependencySet = this.m_dependencySetFactory.getPermanent(dependencySet);
        this.m_dependencySetFactory.addUsage(node.m_mergedIntoDependencySet);
        node.m_nodeState = Node.NodeState.MERGED;
        node.m_previousMergedOrPrunedNode = this.m_lastMergedOrPrunedNode;
        this.m_lastMergedOrPrunedNode = node;
        ++this.m_numberOfMergedOrPrunedNodes;
        this.m_existentialExpansionStrategy.nodeStatusChanged(node);
        this.m_existentialExpansionStrategy.nodesMerged(node, mergeInto);
    }

    public void pruneNode(Node node) {
        assert (node.m_nodeState == Node.NodeState.ACTIVE);
        assert (node.m_mergedInto == null);
        assert (node.m_mergedIntoDependencySet == null);
        assert (node.m_previousMergedOrPrunedNode == null);
        node.m_nodeState = Node.NodeState.PRUNED;
        node.m_previousMergedOrPrunedNode = this.m_lastMergedOrPrunedNode;
        this.m_lastMergedOrPrunedNode = node;
        ++this.m_numberOfMergedOrPrunedNodes;
        this.m_existentialExpansionStrategy.nodeStatusChanged(node);
    }

    private void backtrackLastMergedOrPrunedNode() {
        Node node = this.m_lastMergedOrPrunedNode;
        assert (node.m_nodeState == Node.NodeState.MERGED && node.m_mergedInto != null || node.m_nodeState == Node.NodeState.PRUNED && node.m_mergedInto == null);
        Node savedMergedInfo = null;
        if (node.m_nodeState == Node.NodeState.MERGED) {
            this.m_dependencySetFactory.removeUsage(node.m_mergedIntoDependencySet);
            savedMergedInfo = node.m_mergedInto;
            node.m_mergedInto = null;
            node.m_mergedIntoDependencySet = null;
        }
        node.m_nodeState = Node.NodeState.ACTIVE;
        this.m_lastMergedOrPrunedNode = node.m_previousMergedOrPrunedNode;
        node.m_previousMergedOrPrunedNode = null;
        --this.m_numberOfMergedOrPrunedNodes;
        this.m_existentialExpansionStrategy.nodeStatusChanged(node);
        if (savedMergedInfo != null) {
            this.m_existentialExpansionStrategy.nodesUnmerged(node, savedMergedInfo);
        }
    }

    private void destroyLastTableauNode() {
        Node node = this.m_lastTableauNode;
        assert (node.m_nodeState == Node.NodeState.ACTIVE);
        assert (node.m_mergedInto == null);
        assert (node.m_mergedIntoDependencySet == null);
        assert (node.m_previousMergedOrPrunedNode == null);
        this.m_existentialExpansionStrategy.nodeDestroyed(node);
        if (node.m_previousTableauNode == null) {
            this.m_firstTableauNode = null;
        } else {
            node.m_previousTableauNode.m_nextTableauNode = null;
        }
        this.m_lastTableauNode = node.m_previousTableauNode;
        node.destroy();
        node.m_nextTableauNode = this.m_firstFreeNode;
        this.m_firstFreeNode = node;
        --this.m_numberOfNodesInTableau;
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.nodeDestroyed(node);
        }
    }

    public int getNumberOfNodeCreations() {
        return this.m_numberOfNodeCreations;
    }

    public Node getFirstTableauNode() {
        return this.m_firstTableauNode;
    }

    public Node getLastTableauNode() {
        return this.m_lastTableauNode;
    }

    public int getNumberOfAllocatedNodes() {
        return this.m_allocatedNodes;
    }

    public int getNumberOfNodesInTableau() {
        return this.m_numberOfNodesInTableau;
    }

    public int getNumberOfMergedOrPrunedNodes() {
        return this.m_numberOfMergedOrPrunedNodes;
    }

    public Node getNode(int nodeID) {
        for (Node node = this.m_firstTableauNode; node != null; node = node.getNextTableauNode()) {
            if (node.getNodeID() != nodeID) continue;
            return node;
        }
        return null;
    }

    List<ExistentialConcept> getExistentialConceptsBuffer() {
        if (this.m_existentialConceptsBuffers.isEmpty()) {
            return new ArrayList<ExistentialConcept>();
        }
        return this.m_existentialConceptsBuffers.remove(this.m_existentialConceptsBuffers.size() - 1);
    }

    public void putExistentialConceptsBuffer(List<ExistentialConcept> buffer) {
        assert (buffer.isEmpty());
        this.m_existentialConceptsBuffers.add(buffer);
    }

    public void checkTableauList() {
        Node node = this.m_firstTableauNode;
        int numberOfNodesInTableau = 0;
        while (node != null) {
            if (node.m_previousTableauNode == null) {
                if (this.m_firstTableauNode != node) {
                    throw new IllegalStateException("First tableau node is pointing wrongly.");
                }
            } else if (node.m_previousTableauNode.m_nextTableauNode != node) {
                throw new IllegalStateException("Previous tableau node is pointing wrongly.");
            }
            if (node.m_nextTableauNode == null) {
                if (this.m_lastTableauNode != node) {
                    throw new IllegalStateException("Last tableau node is pointing wrongly.");
                }
            } else if (node.m_nextTableauNode.m_previousTableauNode != node) {
                throw new IllegalStateException("Next tableau node is pointing wrongly.");
            }
            ++numberOfNodesInTableau;
            node = node.m_nextTableauNode;
        }
        if (numberOfNodesInTableau != this.m_numberOfNodesInTableau) {
            throw new IllegalStateException("Invalid number of nodes in the tableau.");
        }
    }

    private boolean shouldUseMetamodellingManager() {
        return this.metamodellingFlag;
    }

    private void addBranchedMetamodellingManager(BranchingPoint branchingPoint) {
        MetamodellingManager metamodellingManager = new MetamodellingManager(this.m_metamodellingManager);
        BranchedMetamodellingManager branchedMetamodellingManager = new BranchedMetamodellingManager(
            metamodellingManager, branchingPoint.m_level
        );
        this.branchedMetamodellingManagers.add(branchedMetamodellingManager);
    }

    private void backtrackMetamodellingManager(int newCurrentBranchingPoint) {
        for (BranchedMetamodellingManager branchedMetamodellingManager : branchedMetamodellingManagers) {
            if (branchedMetamodellingManager.getBranchingPoint() == newCurrentBranchingPoint) {
                this.m_metamodellingManager = branchedMetamodellingManager.getMetamodellingManager();
                break;
            }
        }
    }

    public boolean isSatisfiable(boolean loadPermanentABox, boolean loadAdditionalABox, Set<Atom> perTestPositiveFactsNoDependency, Set<Atom> perTestNegativeFactsNoDependency, Set<Atom> perTestPositiveFactsDummyDependency, Set<Atom> perTestNegativeFactsDummyDependency, Map<Individual, Node> nodesForIndividuals, ReasoningTaskDescription reasoningTaskDescription) {
        return this.isSatisfiable(loadPermanentABox, loadAdditionalABox, perTestPositiveFactsNoDependency, perTestNegativeFactsNoDependency, perTestPositiveFactsDummyDependency, perTestNegativeFactsDummyDependency, new HashMap<Term, Node>(), nodesForIndividuals, reasoningTaskDescription);
    }

    public boolean isSatisfiable(boolean loadAdditionalABox, Set<Atom> perTestPositiveFactsNoDependency, Set<Atom> perTestNegativeFactsNoDependency, Set<Atom> perTestPositiveFactsDummyDependency, Set<Atom> perTestNegativeFactsDummyDependency, Map<Individual, Node> nodesForIndividuals, ReasoningTaskDescription reasoningTaskDescription) {
        boolean loadPermanentABox = this.m_permanentDLOntology.hasNominals() || this.m_additionalDLOntology != null && this.m_additionalDLOntology.hasNominals();
        return this.isSatisfiable(loadPermanentABox, loadAdditionalABox, perTestPositiveFactsNoDependency, perTestNegativeFactsNoDependency, perTestPositiveFactsDummyDependency, perTestNegativeFactsDummyDependency, new HashMap<Term, Node>(), nodesForIndividuals, reasoningTaskDescription);
    }
}

