package org.semanticweb.HermiT;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.blocking.AncestorBlocking;
import org.semanticweb.HermiT.blocking.AnywhereBlocking;
import org.semanticweb.HermiT.blocking.AnywhereValidatedBlocking;
import org.semanticweb.HermiT.blocking.BlockingSignatureCache;
import org.semanticweb.HermiT.blocking.BlockingStrategy;
import org.semanticweb.HermiT.blocking.DirectBlockingChecker;
import org.semanticweb.HermiT.blocking.PairWiseDirectBlockingChecker;
import org.semanticweb.HermiT.blocking.SingleDirectBlockingChecker;
import org.semanticweb.HermiT.blocking.ValidatedPairwiseDirectBlockingChecker;
import org.semanticweb.HermiT.blocking.ValidatedSingleDirectBlockingChecker;
import org.semanticweb.HermiT.debugger.Debugger;
import org.semanticweb.HermiT.existentials.AbstractExpansionStrategy;
import org.semanticweb.HermiT.existentials.CreationOrderStrategy;
import org.semanticweb.HermiT.existentials.IndividualReuseStrategy;
import org.semanticweb.HermiT.hierarchy.ClassificationProgressMonitor;
import org.semanticweb.HermiT.hierarchy.DeterministicClassification;
import org.semanticweb.HermiT.hierarchy.Hierarchy;
import org.semanticweb.HermiT.hierarchy.HierarchyDumperFSS;
import org.semanticweb.HermiT.hierarchy.HierarchyNode;
import org.semanticweb.HermiT.hierarchy.HierarchyPrinterFSS;
import org.semanticweb.HermiT.hierarchy.HierarchySearch;
import org.semanticweb.HermiT.hierarchy.InstanceManager;
import org.semanticweb.HermiT.hierarchy.QuasiOrderClassification;
import org.semanticweb.HermiT.hierarchy.QuasiOrderClassificationForRoles;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Constant;
import org.semanticweb.HermiT.model.DLOntology;
import org.semanticweb.HermiT.model.DescriptionGraph;
import org.semanticweb.HermiT.model.Equality;
import org.semanticweb.HermiT.model.Individual;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.model.InverseRole;
import org.semanticweb.HermiT.model.Role;
import org.semanticweb.HermiT.monitor.TableauMonitor;
import org.semanticweb.HermiT.monitor.TableauMonitorFork;
import org.semanticweb.HermiT.monitor.Timer;
import org.semanticweb.HermiT.monitor.TimerWithPause;
import org.semanticweb.HermiT.structural.BuiltInPropertyManager;
import org.semanticweb.HermiT.structural.OWLAxioms;
import org.semanticweb.HermiT.structural.OWLAxiomsExpressivity;
import org.semanticweb.HermiT.structural.OWLClausification;
import org.semanticweb.HermiT.structural.OWLNormalization;
import org.semanticweb.HermiT.structural.ObjectPropertyInclusionManager;
import org.semanticweb.HermiT.structural.ReducedABoxOnlyClausification;
import org.semanticweb.HermiT.tableau.InterruptFlag;
import org.semanticweb.HermiT.tableau.ReasoningTaskDescription;
import org.semanticweb.HermiT.tableau.Tableau;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.FreshEntitiesException;
import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNodeSet;
import org.semanticweb.owlapi.reasoner.impl.OWLDataPropertyNode;
import org.semanticweb.owlapi.reasoner.impl.OWLDataPropertyNodeSet;
import org.semanticweb.owlapi.reasoner.impl.OWLNamedIndividualNode;
import org.semanticweb.owlapi.reasoner.impl.OWLNamedIndividualNodeSet;
import org.semanticweb.owlapi.reasoner.impl.OWLObjectPropertyNode;
import org.semanticweb.owlapi.reasoner.impl.OWLObjectPropertyNodeSet;
import org.semanticweb.owlapi.util.Version;

public class Reasoner
implements OWLReasoner {
    private static final Version version = new Version(1, 4, 1, 432);
    protected final OntologyChangeListener m_ontologyChangeListener = new OntologyChangeListener();
    protected final Configuration m_configuration;
    protected final OWLOntology m_rootOntology;
    protected final OWLDataFactory df;
    protected final List<OWLOntologyChange> m_pendingChanges;
    protected final Collection<DescriptionGraph> m_descriptionGraphs;
    protected final InterruptFlag m_interruptFlag;
    protected ObjectPropertyInclusionManager m_objectPropertyInclusionManager;
    protected DLOntology m_dlOntology;
    protected Prefixes m_prefixes;
    protected Tableau m_tableau;
    protected Boolean m_isConsistent;
    protected Hierarchy<AtomicConcept> m_atomicConceptHierarchy;
    protected Hierarchy<Role> m_objectRoleHierarchy;
    protected Hierarchy<AtomicRole> m_dataRoleHierarchy;
    protected Map<Role, Set<HierarchyNode<AtomicConcept>>> m_directObjectRoleDomains;
    protected Map<Role, Set<HierarchyNode<AtomicConcept>>> m_directObjectRoleRanges;
    protected Map<AtomicRole, Set<HierarchyNode<AtomicConcept>>> m_directDataRoleDomains;
    protected Map<HierarchyNode<AtomicConcept>, Set<HierarchyNode<AtomicConcept>>> m_directDisjointClasses;
    protected InstanceManager m_instanceManager;

    public Reasoner(Configuration configuration, OWLOntology rootOntology) {
        this(configuration, rootOntology, null);
    }

    public Reasoner(Configuration configuration, OWLOntology rootOntology, Collection<DescriptionGraph> descriptionGraphs) {
        this.m_configuration = configuration;
        this.m_rootOntology = rootOntology;
        this.df = this.m_rootOntology.getOWLOntologyManager().getOWLDataFactory();
        this.m_pendingChanges = new ArrayList<OWLOntologyChange>();
        this.m_rootOntology.getOWLOntologyManager().addOntologyChangeListener(this.m_ontologyChangeListener);
        this.m_descriptionGraphs = descriptionGraphs == null ? Collections.emptySet() : descriptionGraphs;
        this.m_interruptFlag = new InterruptFlag(configuration.individualTaskTimeout);
        this.m_directDisjointClasses = new HashMap<HierarchyNode<AtomicConcept>, Set<HierarchyNode<AtomicConcept>>>();
        this.loadOntology();
    }

    protected void loadOntology() {
        this.clearState();
        OWLClausification clausifier = new OWLClausification(this.m_configuration);
        Object[] result = clausifier.preprocessAndClausify(this.m_rootOntology, this.m_descriptionGraphs);
        this.m_objectPropertyInclusionManager = (ObjectPropertyInclusionManager)result[0];
        this.m_dlOntology = (DLOntology)result[1];
        this.createPrefixes();
        this.m_tableau = Reasoner.createTableau(this.m_interruptFlag, this.m_configuration, this.m_dlOntology, null, this.m_prefixes);
        this.m_instanceManager = null;
    }

    protected void createPrefixes() {
        this.m_prefixes = new Prefixes();
        this.m_prefixes.declareSemanticWebPrefixes();
        HashSet<String> individualIRIs = new HashSet<String>();
        HashSet<String> anonIndividualIRIs = new HashSet<String>();
        for (Individual individual : this.m_dlOntology.getAllIndividuals()) {
            if (individual.isAnonymous()) {
                this.addIRI(individual.getIRI(), anonIndividualIRIs);
                continue;
            }
            this.addIRI(individual.getIRI(), individualIRIs);
        }
        this.m_prefixes.declareInternalPrefixes(individualIRIs, anonIndividualIRIs);
        this.m_prefixes.declareDefaultPrefix(this.m_dlOntology.getOntologyIRI() + "#");
        OWLDocumentFormat format = this.m_rootOntology.getOWLOntologyManager().getOntologyFormat(this.m_rootOntology);
        if (format instanceof PrefixDocumentFormat) {
            PrefixDocumentFormat prefixFormat = (PrefixDocumentFormat)format;
            for (String prefixName : prefixFormat.getPrefixName2PrefixMap().keySet()) {
                String prefix = prefixFormat.getPrefixName2PrefixMap().get(prefixName);
                if (this.m_prefixes.getPrefixName(prefix) != null) continue;
                try {
                    this.m_prefixes.declarePrefix(prefixName, prefix);
                }
                catch (IllegalArgumentException illegalArgumentException) {}
            }
        }
    }

    protected void addIRI(String uri, Set<String> prefixIRIs) {
        int lastHash;
        if (!Prefixes.isInternalIRI(uri) && (lastHash = uri.lastIndexOf(35)) != -1) {
            String prefixIRI = uri.substring(0, lastHash + 1);
            prefixIRIs.add(prefixIRI);
        }
    }

    protected void finalize() {
        this.dispose();
    }

    public void dispose() {
        this.m_rootOntology.getOWLOntologyManager().removeOntologyChangeListener(this.m_ontologyChangeListener);
        this.clearState();
        this.m_interruptFlag.dispose();
    }

    protected void clearState() {
        this.m_pendingChanges.clear();
        this.m_dlOntology = null;
        this.m_prefixes = null;
        this.m_tableau = null;
        this.m_isConsistent = null;
        this.m_atomicConceptHierarchy = null;
        this.m_objectRoleHierarchy = null;
        this.m_dataRoleHierarchy = null;
        this.m_directObjectRoleDomains = new HashMap<Role, Set<HierarchyNode<AtomicConcept>>>();
        this.m_directObjectRoleRanges = new HashMap<Role, Set<HierarchyNode<AtomicConcept>>>();
        this.m_directDataRoleDomains = new HashMap<AtomicRole, Set<HierarchyNode<AtomicConcept>>>();
        this.m_directDisjointClasses = new HashMap<HierarchyNode<AtomicConcept>, Set<HierarchyNode<AtomicConcept>>>();
        this.m_instanceManager = null;
    }

    public void interrupt() {
        this.m_interruptFlag.interrupt();
    }

    public OWLDataFactory getDataFactory() {
        return this.df;
    }

    public String getReasonerName() {
        return "HermiT";
    }

    public Version getReasonerVersion() {
        return version;
    }

    public OWLOntology getRootOntology() {
        return this.m_rootOntology;
    }

    public long getTimeOut() {
        return this.m_configuration.individualTaskTimeout;
    }

    public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
        return this.m_configuration.getIndividualNodeSetPolicy();
    }

    public FreshEntityPolicy getFreshEntityPolicy() {
        return this.m_configuration.getFreshEntityPolicy();
    }

    public Prefixes getPrefixes() {
        return this.m_prefixes;
    }

    public DLOntology getDLOntology() {
        return this.m_dlOntology;
    }

    public Configuration getConfiguration() {
        return this.m_configuration.clone();
    }

    public BufferingMode getBufferingMode() {
        return this.m_configuration.bufferChanges ? BufferingMode.BUFFERING : BufferingMode.NON_BUFFERING;
    }

    public Set<OWLAxiom> getPendingAxiomAdditions() {
        HashSet<OWLAxiom> added = new HashSet<OWLAxiom>();
        for (OWLOntologyChange change : this.m_pendingChanges) {
            if (!(change instanceof AddAxiom)) continue;
            added.add(change.getAxiom());
        }
        return added;
    }

    public Set<OWLAxiom> getPendingAxiomRemovals() {
        HashSet<OWLAxiom> removed = new HashSet<OWLAxiom>();
        for (OWLOntologyChange change : this.m_pendingChanges) {
            if (!(change instanceof RemoveAxiom)) continue;
            removed.add(change.getAxiom());
        }
        return removed;
    }

    public List<OWLOntologyChange> getPendingChanges() {
        return this.m_pendingChanges;
    }

    public void flush() {
        if (!this.m_pendingChanges.isEmpty()) {
            if (this.canProcessPendingChangesIncrementally()) {
                Set rootOntologyImportsClosure = this.m_rootOntology.getImportsClosure();
                Set<Atom> positiveFacts = this.m_dlOntology.getPositiveFacts();
                Set<Atom> negativeFacts = this.m_dlOntology.getNegativeFacts();
                HashSet<Individual> allIndividuals = new HashSet<Individual>();
                Set<AtomicConcept> allAtomicConcepts = this.m_dlOntology.getAllAtomicConcepts();
                Set<AtomicRole> allAtomicObjectRoles = this.m_dlOntology.getAllAtomicObjectRoles();
                Set<AtomicRole> allAtomicDataRoles = this.m_dlOntology.getAllAtomicDataRoles();
                ReducedABoxOnlyClausification aboxFactClausifier = new ReducedABoxOnlyClausification(this.m_configuration, allAtomicConcepts, allAtomicObjectRoles, allAtomicDataRoles);
                for (OWLOntologyChange change : this.m_pendingChanges) {
                    OWLAxiom axiom;
                    if (!rootOntologyImportsClosure.contains(change.getOntology()) || !(axiom = change.getAxiom()).isLogicalAxiom()) continue;
                    aboxFactClausifier.clausify((OWLIndividualAxiom)axiom);
                    if (change instanceof AddAxiom) {
                        positiveFacts.addAll(aboxFactClausifier.getPositiveFacts());
                        negativeFacts.addAll(aboxFactClausifier.getNegativeFacts());
                        continue;
                    }
                    positiveFacts.removeAll(aboxFactClausifier.getPositiveFacts());
                    negativeFacts.removeAll(aboxFactClausifier.getNegativeFacts());
                }
                for (Atom atom : positiveFacts) {
                    atom.getIndividuals(allIndividuals);
                }
                for (Atom atom : negativeFacts) {
                    atom.getIndividuals(allIndividuals);
                }
                this.m_dlOntology = new DLOntology(this.m_dlOntology.getOntologyIRI(), this.m_dlOntology.getDLClauses(), positiveFacts, negativeFacts, allAtomicConcepts, allAtomicObjectRoles, this.m_dlOntology.getAllComplexObjectRoles(), allAtomicDataRoles, this.m_dlOntology.getAllUnknownDatatypeRestrictions(), this.m_dlOntology.getDefinedDatatypeIRIs(), allIndividuals, this.m_dlOntology.hasInverseRoles(), this.m_dlOntology.hasAtMostRestrictions(), this.m_dlOntology.hasNominals(), this.m_dlOntology.hasDatatypes(), null);
                this.m_tableau = new Tableau(this.m_interruptFlag, this.m_tableau.getTableauMonitor(), this.m_tableau.getExistentialsExpansionStrategy(), this.m_configuration.useDisjunctionLearning, this.m_dlOntology, null, this.m_configuration.parameters);
                this.m_instanceManager = null;
                this.m_isConsistent = null;
            } else {
                this.loadOntology();
            }
            this.m_pendingChanges.clear();
        }
    }

    public boolean canProcessPendingChangesIncrementally() {
        Set rootOntologyImportsClosure = this.m_rootOntology.getImportsClosure();
        for (OWLOntologyChange change : this.m_pendingChanges) {
            if (!rootOntologyImportsClosure.contains(change.getOntology())) continue;
            if (this.m_dlOntology.hasNominals() || !this.m_dlOntology.getAllDescriptionGraphs().isEmpty()) {
                return false;
            }
            if (!change.isAxiomChange()) {
                return false;
            }
            OWLAxiom axiom = change.getAxiom();
            if (axiom.isLogicalAxiom()) {
                if (axiom instanceof OWLClassAssertionAxiom) {
                    OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom)axiom;
                    OWLIndividual individual = classAssertion.getIndividual();
                    if (!this.isDefined(individual)) {
                        return false;
                    }
                    OWLClassExpression classExpression = classAssertion.getClassExpression();
                    if (classExpression instanceof OWLClass) {
                        if (this.isDefined((OWLClass)classExpression) || Prefixes.isInternalIRI(((OWLClass)classExpression).getIRI().toString())) continue;
                        return false;
                    }
                    if (classExpression instanceof OWLObjectHasSelf) {
                        OWLObjectProperty namedOP = ((OWLObjectHasSelf)classExpression).getProperty().getNamedProperty();
                        if (this.isDefined(namedOP) || Prefixes.isInternalIRI(namedOP.getIRI().toString())) continue;
                        return false;
                    }
                    if (classExpression instanceof OWLObjectHasValue) {
                        OWLObjectHasValue hasValue = (OWLObjectHasValue)classExpression;
                        OWLObjectProperty namedOP = hasValue.getProperty().getNamedProperty();
                        OWLIndividual filler = hasValue.getFiller();
                        if ((this.isDefined(namedOP) || Prefixes.isInternalIRI(namedOP.getIRI().toString())) && this.isDefined(filler)) continue;
                        return false;
                    }
                    if (classExpression instanceof OWLObjectComplementOf) {
                        OWLClassExpression negated = ((OWLObjectComplementOf)classExpression).getOperand();
                        if (negated instanceof OWLClass) {
                            OWLClass cls = (OWLClass)negated;
                            if (this.isDefined(cls) || Prefixes.isInternalIRI(cls.getIRI().toString())) continue;
                            return false;
                        }
                        if (negated instanceof OWLObjectHasSelf) {
                        	OWLObjectHasSelf hasSelf = (OWLObjectHasSelf)negated;
                        	OWLObjectProperty namedOP = hasSelf.getProperty().getNamedProperty();
                            if (this.isDefined(namedOP) || Prefixes.isInternalIRI(namedOP.getIRI().toString())) continue;
                            return false;
                        }
                        if (negated instanceof OWLObjectHasValue) {
                        	OWLObjectHasValue hasSelf = (OWLObjectHasValue)negated;
                        	OWLObjectProperty namedOP = hasSelf.getProperty().getNamedProperty();
                            OWLIndividual filler = hasSelf.getFiller();
                            if ((this.isDefined(namedOP) || Prefixes.isInternalIRI(namedOP.getIRI().toString())) && this.isDefined(filler)) continue;
                            return false;
                        }
                        return false;
                    }
                    return false;
                }
                if (axiom instanceof OWLIndividualAxiom) continue;
                return false;
            }
            if (!(axiom instanceof OWLDeclarationAxiom)) continue;
            OWLEntity entity = ((OWLDeclarationAxiom)axiom).getEntity();
            if (entity.isOWLClass() && !this.isDefined((OWLClass)entity) && !Prefixes.isInternalIRI(entity.getIRI().toString())) {
                return false;
            }
            if (entity.isOWLObjectProperty() && !this.isDefined((OWLObjectProperty)entity) && !Prefixes.isInternalIRI(entity.getIRI().toString())) {
                return false;
            }
            if (!entity.isOWLDataProperty() || this.isDefined((OWLDataProperty)entity) || Prefixes.isInternalIRI(entity.getIRI().toString())) continue;
            return false;
        }
        return true;
    }

    public boolean isDefined(OWLClass owlClass) {
        AtomicConcept atomicConcept = AtomicConcept.create(owlClass.getIRI().toString());
        return this.m_dlOntology.containsAtomicConcept(atomicConcept) || AtomicConcept.THING.equals(atomicConcept) || AtomicConcept.NOTHING.equals(atomicConcept);
    }

    public boolean isDefined(OWLIndividual owlIndividual) {
        Individual individual = owlIndividual.isAnonymous() ? Individual.createAnonymous(owlIndividual.asOWLAnonymousIndividual().getID().toString()) : Individual.create(owlIndividual.asOWLNamedIndividual().getIRI().toString());
        return this.m_dlOntology.containsIndividual(individual);
    }

    public boolean isDefined(OWLObjectProperty owlObjectProperty) {
        AtomicRole atomicRole = AtomicRole.create(owlObjectProperty.getIRI().toString());
        return this.m_dlOntology.containsObjectRole(atomicRole) || AtomicRole.TOP_OBJECT_ROLE.equals(atomicRole) || AtomicRole.BOTTOM_OBJECT_ROLE.equals(atomicRole);
    }

    public boolean isDefined(OWLDataProperty owlDataProperty) {
        AtomicRole atomicRole = AtomicRole.create(owlDataProperty.getIRI().toString());
        return this.m_dlOntology.containsDataRole(atomicRole) || AtomicRole.TOP_DATA_ROLE.equals(atomicRole) || AtomicRole.BOTTOM_DATA_ROLE.equals(atomicRole);
    }

    public Set<InferenceType> getPrecomputableInferenceTypes() {
        HashSet<InferenceType> supportedInferenceTypes = new HashSet<InferenceType>();
        supportedInferenceTypes.add(InferenceType.CLASS_HIERARCHY);
        supportedInferenceTypes.add(InferenceType.OBJECT_PROPERTY_HIERARCHY);
        supportedInferenceTypes.add(InferenceType.DATA_PROPERTY_HIERARCHY);
        supportedInferenceTypes.add(InferenceType.CLASS_ASSERTIONS);
        supportedInferenceTypes.add(InferenceType.OBJECT_PROPERTY_ASSERTIONS);
        supportedInferenceTypes.add(InferenceType.SAME_INDIVIDUAL);
        return supportedInferenceTypes;
    }

    public boolean isPrecomputed(InferenceType inferenceType) {
        switch (inferenceType) {
            case CLASS_HIERARCHY: {
                return this.m_atomicConceptHierarchy != null;
            }
            case OBJECT_PROPERTY_HIERARCHY: {
                return this.m_objectRoleHierarchy != null;
            }
            case DATA_PROPERTY_HIERARCHY: {
                return this.m_dataRoleHierarchy != null;
            }
            case CLASS_ASSERTIONS: {
                return this.m_instanceManager != null && this.m_instanceManager.realizationCompleted();
            }
            case OBJECT_PROPERTY_ASSERTIONS: {
                return this.m_instanceManager != null && this.m_instanceManager.objectPropertyRealizationCompleted();
            }
            case SAME_INDIVIDUAL: {
                return this.m_instanceManager != null && this.m_instanceManager.sameAsIndividualsComputed();
            }
        }
        return false;
    }

    public /* varargs */ void precomputeInferences(InferenceType ... inferenceTypes) throws ReasonerInterruptedException, TimeOutException, InconsistentOntologyException {
        this.checkPreConditions();
        boolean doAll = this.m_configuration.prepareReasonerInferences == null;
        HashSet<InferenceType> requiredInferences = new HashSet<InferenceType>(Arrays.asList(inferenceTypes));
        if (requiredInferences.contains(InferenceType.CLASS_HIERARCHY) && (doAll || this.m_configuration.prepareReasonerInferences.classClassificationRequired)) {
            this.classifyClasses();
        }
        if (requiredInferences.contains(InferenceType.OBJECT_PROPERTY_HIERARCHY) && (doAll || this.m_configuration.prepareReasonerInferences.objectPropertyClassificationRequired)) {
            this.classifyObjectProperties();
        }
        if (requiredInferences.contains(InferenceType.DATA_PROPERTY_HIERARCHY) && (doAll || this.m_configuration.prepareReasonerInferences.dataPropertyClassificationRequired)) {
            this.classifyDataProperties();
        }
        if (requiredInferences.contains(InferenceType.CLASS_ASSERTIONS) && (doAll || this.m_configuration.prepareReasonerInferences.realisationRequired)) {
            this.realise();
            if (this.m_configuration.individualNodeSetPolicy == IndividualNodeSetPolicy.BY_SAME_AS || this.m_configuration.prepareReasonerInferences != null && this.m_configuration.prepareReasonerInferences.sameAs) {
                this.precomputeSameAsEquivalenceClasses();
            }
        }
        if (requiredInferences.contains(InferenceType.OBJECT_PROPERTY_ASSERTIONS) && (doAll || this.m_configuration.prepareReasonerInferences.objectPropertyRealisationRequired)) {
            this.realiseObjectProperties();
        }
        if (requiredInferences.contains(InferenceType.SAME_INDIVIDUAL) && (doAll || this.m_configuration.prepareReasonerInferences.sameAs)) {
            this.precomputeSameAsEquivalenceClasses();
        }
    }

    protected void initialisePropertiesInstanceManager() {
        if (this.m_instanceManager == null || !this.m_instanceManager.arePropertiesInitialised()) {
            if (this.m_configuration.reasonerProgressMonitor != null) {
                this.m_configuration.reasonerProgressMonitor.reasonerTaskStarted("Initializing property instance data structures");
            }
            if (this.m_instanceManager == null) {
                this.m_instanceManager = new InstanceManager(this.m_interruptFlag, this, this.m_atomicConceptHierarchy, this.m_objectRoleHierarchy);
            }
            boolean isConsistent = true;
            if (this.m_isConsistent != null && !this.m_isConsistent.booleanValue()) {
                this.m_instanceManager.setInconsistent();
            } else {
                int noAxioms = this.m_dlOntology.getDLClauses().size();
                int noComplexRoles = this.m_dlOntology.getAllComplexObjectRoles().size();
                if (this.m_dlOntology.hasInverseRoles()) {
                    noComplexRoles /= 2;
                }
                int noIndividuals = this.m_dlOntology.getAllIndividuals().size();
                int chunks = 2 * noComplexRoles * noIndividuals / 10000 + 1;
                int stepsAdditionalAxioms = noComplexRoles * noIndividuals;
                int stepsRewritingAdditionalAxioms = 5 * noComplexRoles * noIndividuals / chunks;
                int stepsTableauExpansion = stepsAdditionalAxioms / chunks + noAxioms + noIndividuals;
                int stepsInitialiseKnownPossible = noIndividuals + noComplexRoles * noIndividuals;
                int steps = stepsAdditionalAxioms + chunks * stepsRewritingAdditionalAxioms + chunks * stepsTableauExpansion + stepsInitialiseKnownPossible;
                int startIndividualIndex = 0;
                int completedSteps = 0;
                OWLAxiom[] additionalAxioms = this.m_instanceManager.getAxiomsForReadingOffCompexProperties(this.getDataFactory(), this.m_configuration.reasonerProgressMonitor, completedSteps, steps);
                completedSteps += stepsAdditionalAxioms / chunks;
                boolean moreWork = true;
                while (moreWork) {
                    Tableau tableau = this.getTableau(additionalAxioms);
                    completedSteps += stepsRewritingAdditionalAxioms;
                    if (this.m_configuration.reasonerProgressMonitor != null) {
                        this.m_configuration.reasonerProgressMonitor.reasonerTaskProgressChanged(completedSteps, steps);
                    }
                    isConsistent = tableau.isSatisfiable(true, true, null, null, null, null, this.m_instanceManager.getNodesForIndividuals(), new ReasoningTaskDescription(false, "Initial consistency check plus reading-off known and possible class and property instances (individual " + startIndividualIndex + " to " + this.m_instanceManager.getCurrentIndividualIndex() + ")."));
                    completedSteps += stepsTableauExpansion;
                    if (this.m_configuration.reasonerProgressMonitor != null) {
                        this.m_configuration.reasonerProgressMonitor.reasonerTaskProgressChanged(completedSteps, steps);
                    }
                    if (!isConsistent) {
                        this.m_instanceManager.setInconsistent();
                        break;
                    }
                    completedSteps = this.m_instanceManager.initializeKnowAndPossiblePropertyInstances(this.m_configuration.reasonerProgressMonitor, startIndividualIndex, completedSteps, steps);
                    tableau.clearAdditionalDLOntology();
                    startIndividualIndex = this.m_instanceManager.getCurrentIndividualIndex();
                    additionalAxioms = this.m_instanceManager.getAxiomsForReadingOffCompexProperties(this.getDataFactory(), this.m_configuration.reasonerProgressMonitor, completedSteps, steps);
                    completedSteps += stepsAdditionalAxioms / chunks;
                    moreWork = additionalAxioms.length > 0;
                }
                if (this.m_isConsistent == null) {
                    this.m_isConsistent = isConsistent;
                }
            }
            if (this.m_configuration.reasonerProgressMonitor != null) {
                this.m_configuration.reasonerProgressMonitor.reasonerTaskStopped();
            }
        }
    }

    protected void initialiseClassInstanceManager() {
        if (this.m_instanceManager == null || !this.m_instanceManager.areClassesInitialised()) {
            if (this.m_configuration.reasonerProgressMonitor != null) {
                this.m_configuration.reasonerProgressMonitor.reasonerTaskStarted("Initializing class instance data structures");
            }
            if (this.m_instanceManager == null) {
                this.m_instanceManager = new InstanceManager(this.m_interruptFlag, this, this.m_atomicConceptHierarchy, this.m_objectRoleHierarchy);
            }
            boolean isConsistent = true;
            if (this.m_isConsistent != null && !this.m_isConsistent.booleanValue()) {
                this.m_instanceManager.setInconsistent();
            } else {
                int noAxioms = this.m_dlOntology.getDLClauses().size();
                int noIndividuals = this.m_dlOntology.getAllIndividuals().size();
                int stepsTableauExpansion = noAxioms + noIndividuals;
                int stepsInitialiseKnownPossible = noIndividuals;
                int steps = stepsTableauExpansion + stepsInitialiseKnownPossible;
                int completedSteps = 0;
                Tableau tableau = this.getTableau();
                isConsistent = tableau.isSatisfiable(true, true, null, null, null, null, this.m_instanceManager.getNodesForIndividuals(), new ReasoningTaskDescription(false, "Initial tableau for reading-off known and possible class instances."));
                completedSteps += stepsTableauExpansion;
                if (this.m_configuration.reasonerProgressMonitor != null) {
                    this.m_configuration.reasonerProgressMonitor.reasonerTaskProgressChanged(completedSteps, steps);
                }
                if (!isConsistent) {
                    this.m_instanceManager.setInconsistent();
                } else {
                    this.m_instanceManager.initializeKnowAndPossibleClassInstances(this.m_configuration.reasonerProgressMonitor, completedSteps, steps);
                }
                if (this.m_isConsistent == null) {
                    this.m_isConsistent = isConsistent;
                }
                tableau.clearAdditionalDLOntology();
            }
            if (this.m_configuration.reasonerProgressMonitor != null) {
                this.m_configuration.reasonerProgressMonitor.reasonerTaskStopped();
            }
        }
    }

    public boolean isConsistent() {
        this.flushChangesIfRequired();
        if (this.m_isConsistent == null) {
            this.m_isConsistent = this.getTableau().isSatisfiable(true, true, null, null, null, null, null, ReasoningTaskDescription.isABoxSatisfiable());
        }
        return this.m_isConsistent;
    }

    public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
        return true;
    }

    public boolean isEntailed(OWLAxiom axiom) {
        this.checkPreConditions(axiom);
        if (!this.isConsistent()) {
            return true;
        }
        EntailmentChecker checker = new EntailmentChecker(this, this.getDataFactory());
        return checker.entails(axiom);
    }

    public boolean isEntailed(Set<? extends OWLAxiom> axioms) {
        this.checkPreConditions(axioms.toArray(new OWLObject[0]));
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        EntailmentChecker checker = new EntailmentChecker(this, this.getDataFactory());
        return checker.entails(axioms);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void classifyClasses() {
        this.checkPreConditions();
        if (this.m_atomicConceptHierarchy == null) {
            HashSet<AtomicConcept> relevantAtomicConcepts = new HashSet<AtomicConcept>();
            relevantAtomicConcepts.add(AtomicConcept.THING);
            relevantAtomicConcepts.add(AtomicConcept.NOTHING);
            for (AtomicConcept atomicConcept : this.m_dlOntology.getAllAtomicConcepts()) {
                if (Prefixes.isInternalIRI(atomicConcept.getIRI())) continue;
                relevantAtomicConcepts.add(atomicConcept);
            }
            if (!this.m_isConsistent.booleanValue()) {
                this.m_atomicConceptHierarchy = Hierarchy.emptyHierarchy(relevantAtomicConcepts, AtomicConcept.THING, AtomicConcept.NOTHING);
            } else {
                try {
                    final int numRelevantConcepts = relevantAtomicConcepts.size();
                    if (this.m_configuration.reasonerProgressMonitor != null) {
                        this.m_configuration.reasonerProgressMonitor.reasonerTaskStarted("Building the class hierarchy...");
                    }
                    ClassificationProgressMonitor progressMonitor = new ClassificationProgressMonitor(){
                        private int m_processedConcepts = 0;

                        @Override
                        public void elementClassified(AtomicConcept element) {
                            ++this.m_processedConcepts;
                            if (Reasoner.this.m_configuration.reasonerProgressMonitor != null) {
                                Reasoner.this.m_configuration.reasonerProgressMonitor.reasonerTaskProgressChanged(this.m_processedConcepts, numRelevantConcepts);
                            }
                        }
                    };
                    this.m_atomicConceptHierarchy = this.classifyAtomicConcepts(this.getTableau(), progressMonitor, AtomicConcept.THING, AtomicConcept.NOTHING, relevantAtomicConcepts, this.m_configuration.forceQuasiOrderClassification);
                    if (this.m_instanceManager != null) {
                        this.m_instanceManager.setToClassifiedConceptHierarchy(this.m_atomicConceptHierarchy);
                    }

                    this.applyMetamodellingInferencesFromHierarchy();
                }
                finally {
                    if (this.m_configuration.reasonerProgressMonitor != null) {
                        this.m_configuration.reasonerProgressMonitor.reasonerTaskStopped();
                    }
                }
            }
        }
    }

    /**
     * Aplica inferencias de metamodelado basadas en la jerarquía clasificada.
     * Si existe un axioma de metamodelado (C, a) y C es equivalente a D en la jerarquía
     * inferida, entonces se agrega (D, a) si aún no está presente.
     */
    private void applyMetamodellingInferencesFromHierarchy() {
        if (this.m_atomicConceptHierarchy == null || !this.isConsistent()) {
            return;
        }
        java.util.Set<org.semanticweb.owlapi.model.OWLMetamodellingAxiom> metaAxioms = this.m_dlOntology.getMetamodellingAxioms();
        if (metaAxioms == null || metaAxioms.isEmpty()) {
            return;
        }
        org.semanticweb.owlapi.model.OWLDataFactory factory = this.getDataFactory();
        // Para cada axioma de metamodelado existente (C, a), propagar a clases equivalentes a C
        java.util.List<org.semanticweb.owlapi.model.OWLMetamodellingAxiom> toAdd = new java.util.ArrayList<org.semanticweb.owlapi.model.OWLMetamodellingAxiom>();
        for (org.semanticweb.owlapi.model.OWLMetamodellingAxiom ax : metaAxioms) {
            org.semanticweb.owlapi.model.OWLClassExpression clsExpr = ax.getModelClass();
            if (!(clsExpr instanceof org.semanticweb.owlapi.model.OWLClass)) {
                continue; // solo soportamos clases nominales aquí
            }
            org.semanticweb.owlapi.model.OWLClass cls = (org.semanticweb.owlapi.model.OWLClass) clsExpr;
            AtomicConcept concept = Reasoner.H(cls);
            org.semanticweb.owlapi.model.OWLIndividual ind = ax.getMetamodelIndividual();
            HierarchyNode<AtomicConcept> node = this.m_atomicConceptHierarchy.getNodeForElement(concept);
            if (node == null) {
                continue;
            }
            for (AtomicConcept eq : node.getEquivalentElements()) {
                if (eq.equals(concept)) {
                    continue;
                }
                org.semanticweb.owlapi.model.OWLClass eqClass = factory.getOWLClass(org.semanticweb.owlapi.model.IRI.create(eq.getIRI()));
                org.semanticweb.owlapi.model.OWLMetamodellingAxiom newAx = factory.getOWLMetamodellingAxiom(eqClass, ind);
                if (!metaAxioms.contains(newAx)) {
                    toAdd.add(newAx);
                }
            }
        }
        if (!toAdd.isEmpty()) {
            metaAxioms.addAll(toAdd);
        }
        // Print inferred metamodelling axioms
        if (!toAdd.isEmpty()) {
            System.out.println("Inferred metamodelling axioms:");
            for (org.semanticweb.owlapi.model.OWLMetamodellingAxiom ax : toAdd) {
                System.out.println("  " + ax);
            }
        }
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLClass> getTopClassNode() {
        this.classifyClasses();
        return this.atomicConceptHierarchyNodeToNode(this.m_atomicConceptHierarchy.getTopNode());
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLClass> getBottomClassNode() {
        this.classifyClasses();
        return this.atomicConceptHierarchyNodeToNode(this.m_atomicConceptHierarchy.getBottomNode());
    }

    public boolean isSatisfiable(OWLClassExpression classExpression) {
        this.checkPreConditions(classExpression);
        if (!this.isConsistent()) {
            return false;
        }
        if (classExpression instanceof OWLClass && this.m_atomicConceptHierarchy != null) {
            AtomicConcept concept = Reasoner.H((OWLClass)classExpression);
            HierarchyNode<AtomicConcept> node = this.m_atomicConceptHierarchy.getNodeForElement(concept);
            return node != this.m_atomicConceptHierarchy.getBottomNode();
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLAnonymousIndividual freshIndividual = factory.getOWLAnonymousIndividual("fresh-individual");
        OWLClassAssertionAxiom assertClassExpression = factory.getOWLClassAssertionAxiom(classExpression, freshIndividual);
        Tableau tableau = this.getTableau(assertClassExpression);
        return tableau.isSatisfiable(true, null, null, null, null, null, ReasoningTaskDescription.isConceptSatisfiable(classExpression));
    }

    protected boolean isSubClassOf(OWLClassExpression subClassExpression, OWLClassExpression superClassExpression) {
        this.checkPreConditions(subClassExpression, superClassExpression);
        if (!this.isConsistent() || subClassExpression.isOWLNothing() || superClassExpression.isOWLThing()) {
            return true;
        }
        if (subClassExpression instanceof OWLClass && superClassExpression instanceof OWLClass) {
            AtomicConcept subconcept = Reasoner.H((OWLClass)subClassExpression);
            AtomicConcept superconcept = Reasoner.H((OWLClass)superClassExpression);
            if (this.m_atomicConceptHierarchy != null) {
                if (!this.containsFreshEntities(subClassExpression, superClassExpression)) {
                    HierarchyNode<AtomicConcept> subconceptNode = this.m_atomicConceptHierarchy.getNodeForElement(subconcept);
                    return subconceptNode.isEquivalentElement(superconcept) || subconceptNode.isAncestorElement(superconcept);
                }
            }
            Tableau tableau = this.getTableau();
            Individual freshIndividual = Individual.createAnonymous("fresh-individual");
            Atom subconceptAssertion = Atom.create(subconcept, freshIndividual);
            Atom superconceptAssertion = Atom.create(superconcept, freshIndividual);
            return !tableau.isSatisfiable(true, Collections.singleton(subconceptAssertion), Collections.singleton(superconceptAssertion), null, null, null, ReasoningTaskDescription.isConceptSubsumedBy(subconcept, superconcept));
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLAnonymousIndividual freshIndividual = factory.getOWLAnonymousIndividual("fresh-individual");
        OWLClassAssertionAxiom assertSubClassExpression = factory.getOWLClassAssertionAxiom(subClassExpression, freshIndividual);
        OWLClassAssertionAxiom assertNotSuperClassExpression = factory.getOWLClassAssertionAxiom(superClassExpression.getObjectComplementOf(), freshIndividual);
        Tableau tableau = this.getTableau(assertSubClassExpression, assertNotSuperClassExpression);
        boolean result = tableau.isSatisfiable(true, null, null, null, null, null, ReasoningTaskDescription.isConceptSubsumedBy(subClassExpression, superClassExpression));
        tableau.clearAdditionalDLOntology();
        return !result;
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLClass> getEquivalentClasses(OWLClassExpression classExpression) {
        HierarchyNode<AtomicConcept> node = this.getHierarchyNode(classExpression);
        return this.atomicConceptHierarchyNodeToNode(node);
    }

    public NodeSet<OWLClass> getSuperClasses(OWLClassExpression classExpression, boolean direct) {
        Set<HierarchyNode<AtomicConcept>> result;
        HierarchyNode<AtomicConcept> node = this.getHierarchyNode(classExpression);
        if (direct) {
            result = node.getParentNodes();
        } else {
            result = new HashSet<HierarchyNode<AtomicConcept>>(node.getAncestorNodes());
            result.remove(node);
        }
        return this.atomicConceptHierarchyNodesToNodeSet(result);
    }

    public NodeSet<OWLClass> getSubClasses(OWLClassExpression classExpression, boolean direct) {
        Set<HierarchyNode<AtomicConcept>> result;
        HierarchyNode<AtomicConcept> node = this.getHierarchyNode(classExpression);
        if (direct) {
            result = node.getChildNodes();
        } else {
            result = new HashSet<HierarchyNode<AtomicConcept>>(node.getDescendantNodes());
            result.remove(node);
        }
        return this.atomicConceptHierarchyNodesToNodeSet(result);
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLClass> getUnsatisfiableClasses() {
        this.classifyClasses();
        HierarchyNode<AtomicConcept> node = this.m_atomicConceptHierarchy.getBottomNode();
        return this.atomicConceptHierarchyNodeToNode(node);
    }

    public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression classExpression) {
        this.checkPreConditions(classExpression);
        this.classifyClasses();
        if (classExpression.isOWLNothing() || !this.m_isConsistent.booleanValue()) {
            HierarchyNode<AtomicConcept> node = this.m_atomicConceptHierarchy.getBottomNode();
            return this.atomicConceptHierarchyNodesToNodeSet(node.getAncestorNodes());
        }
        if (classExpression.isOWLThing()) {
            HierarchyNode<AtomicConcept> node = this.m_atomicConceptHierarchy.getBottomNode();
            return this.atomicConceptHierarchyNodesToNodeSet(Collections.singleton(node));
        }
        if (classExpression instanceof OWLClass) {
            HierarchyNode<AtomicConcept> node = this.getHierarchyNode(classExpression);
            if (node == null || node == this.m_atomicConceptHierarchy.getTopNode()) {
                return new OWLClassNodeSet(this.getDataFactory().getOWLNothing());
            }
            if (node == this.m_atomicConceptHierarchy.getBottomNode()) {
                return this.atomicConceptHierarchyNodesToNodeSet(node.getAncestorNodes());
            }
            Set<HierarchyNode<AtomicConcept>> directDisjoints = this.getDisjointConceptNodes(node);
            HashSet<HierarchyNode<AtomicConcept>> result = new HashSet<HierarchyNode<AtomicConcept>>();
            for (HierarchyNode<AtomicConcept> directDisjoint : directDisjoints) {
                result.addAll(directDisjoint.getDescendantNodes());
            }
            return this.atomicConceptHierarchyNodesToNodeSet(result);
        }
        org.semanticweb.owlapi.reasoner.Node<OWLClass> equivalentToComplement = this.getEquivalentClasses(classExpression.getObjectComplementOf());
        NodeSet<OWLClass> subsDisjoint = this.getSubClasses(classExpression.getObjectComplementOf(), false);
        HashSet<org.semanticweb.owlapi.reasoner.Node<OWLClass>> result = new HashSet<org.semanticweb.owlapi.reasoner.Node<OWLClass>>();
        if (equivalentToComplement.getSize() > 0) {
            result.add(equivalentToComplement);
        }
        result.addAll(subsDisjoint.getNodes());
        return new OWLClassNodeSet(result);
    }

    protected Set<HierarchyNode<AtomicConcept>> getDisjointConceptNodes(HierarchyNode<AtomicConcept> node) {
        if (this.m_directDisjointClasses.containsKey(node)) {
            return this.m_directDisjointClasses.get(node);
        }
        Set<HierarchyNode<AtomicConcept>> result = new HashSet();
        OWLDataFactory factory = this.getDataFactory();
        OWLObjectComplementOf negated = factory.getOWLObjectComplementOf(factory.getOWLClass(IRI.create(node.getRepresentative().getIRI())));
        HierarchyNode<AtomicConcept> equivalentToComplement = this.getHierarchyNode(negated);
        for (AtomicConcept equiv : equivalentToComplement.getEquivalentElements()) {
            if (Prefixes.isInternalIRI(equiv.getIRI())) continue;
            HierarchyNode<AtomicConcept> rootDisjoint = this.m_atomicConceptHierarchy.getNodeForElement(equiv);
            result = Collections.singleton(rootDisjoint);
            this.m_directDisjointClasses.put(node, result);
            return result;
        }
        result = equivalentToComplement.getChildNodes();
        this.m_directDisjointClasses.put(node, result);
        return result;
    }

    public void precomputeDisjointClasses() {
        this.checkPreConditions();
        if (!this.m_isConsistent.booleanValue()) {
            return;
        }
        if (this.m_atomicConceptHierarchy == null || this.m_directDisjointClasses.keySet().size() < this.m_atomicConceptHierarchy.getAllNodesSet().size() - 2) {
            this.classifyClasses();
            HashSet<HierarchyNode<AtomicConcept>> nodes = new HashSet<HierarchyNode<AtomicConcept>>(this.m_atomicConceptHierarchy.getAllNodes());
            nodes.remove(this.m_atomicConceptHierarchy.getTopNode());
            nodes.remove(this.m_atomicConceptHierarchy.getBottomNode());
            nodes.removeAll(this.m_directDisjointClasses.keySet());
            int steps = nodes.size();
            int step = 0;
            if (this.m_configuration.reasonerProgressMonitor != null) {
                this.m_configuration.reasonerProgressMonitor.reasonerTaskStarted("Compute disjoint classes");
            }
            for (HierarchyNode node : nodes) {
                this.getDisjointConceptNodes(node);
                if (this.m_configuration.reasonerProgressMonitor == null) continue;
                this.m_configuration.reasonerProgressMonitor.reasonerTaskProgressChanged(++step, steps);
            }
            if (this.m_configuration.reasonerProgressMonitor != null) {
                this.m_configuration.reasonerProgressMonitor.reasonerTaskStopped();
            }
        }
    }

    protected HierarchyNode<AtomicConcept> getHierarchyNode(OWLClassExpression classExpression) {
        this.checkPreConditions(classExpression);
        this.classifyClasses();
        if (!this.isConsistent()) {
            return this.m_atomicConceptHierarchy.getBottomNode();
        }
        if (classExpression instanceof OWLClass) {
            AtomicConcept atomicConcept = Reasoner.H((OWLClass)classExpression);
            HierarchyNode<AtomicConcept> node = this.m_atomicConceptHierarchy.getNodeForElement(atomicConcept);
            if (node == null) {
                node = new HierarchyNode<AtomicConcept>(atomicConcept, Collections.singleton(atomicConcept), Collections.singleton(this.m_atomicConceptHierarchy.getTopNode()), Collections.singleton(this.m_atomicConceptHierarchy.getBottomNode()));
            }
            return node;
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLClass queryConcept = factory.getOWLClass(IRI.create("internal:query-concept"));
        OWLEquivalentClassesAxiom classDefinitionAxiom = factory.getOWLEquivalentClassesAxiom(queryConcept, classExpression);
        final Tableau tableau = this.getTableau(classDefinitionAxiom);
        HierarchySearch.Relation<AtomicConcept> hierarchyRelation = new HierarchySearch.Relation<AtomicConcept>(){

            @Override
            public boolean doesSubsume(AtomicConcept parent, AtomicConcept child) {
                Individual freshIndividual = Individual.createAnonymous("fresh-individual");
                return !tableau.isSatisfiable(true, Collections.singleton(Atom.create(child, freshIndividual)), null, null, Collections.singleton(Atom.create(parent, freshIndividual)), null, ReasoningTaskDescription.isConceptSubsumedBy(child, parent));
            }
        };
        HierarchyNode<AtomicConcept> extendedHierarchy = HierarchySearch.findPosition(hierarchyRelation, AtomicConcept.create("internal:query-concept"), this.m_atomicConceptHierarchy.getTopNode(), this.m_atomicConceptHierarchy.getBottomNode());
        tableau.clearAdditionalDLOntology();
        return extendedHierarchy;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void classifyObjectProperties() {
        this.checkPreConditions();
        if (this.m_objectRoleHierarchy == null) {
            HashSet<Role> relevantObjectRoles = new HashSet<Role>();
            for (AtomicRole atomicRole : this.m_dlOntology.getAllAtomicObjectRoles()) {
                if (atomicRole == AtomicRole.TOP_OBJECT_ROLE || atomicRole == AtomicRole.BOTTOM_OBJECT_ROLE) continue;
                relevantObjectRoles.add(atomicRole);
                if (!this.m_dlOntology.hasInverseRoles()) continue;
                relevantObjectRoles.add(atomicRole.getInverse());
            }
            if (!this.m_isConsistent.booleanValue()) {
                relevantObjectRoles.add(AtomicRole.TOP_OBJECT_ROLE);
                relevantObjectRoles.add(AtomicRole.BOTTOM_OBJECT_ROLE);
                this.m_objectRoleHierarchy = Hierarchy.emptyHierarchy(relevantObjectRoles, AtomicRole.TOP_OBJECT_ROLE, AtomicRole.BOTTOM_OBJECT_ROLE);
            } else {
                HashMap<Role, AtomicConcept> conceptsForRoles = new HashMap<Role, AtomicConcept>();
                final HashMap<AtomicConcept, Role> rolesForConcepts = new HashMap<AtomicConcept, Role>();
                ArrayList<Object> additionalAxioms = new ArrayList<Object>();
                OWLDataFactory factory = this.getDataFactory();
                OWLClass freshConcept = factory.getOWLClass(IRI.create("internal:fresh-concept"));
                for (Role objectRole : relevantObjectRoles) {
                	OWLObjectPropertyExpression objectPropertyExpression;
                    AtomicConcept conceptForRole;
                    if (objectRole instanceof AtomicRole) {
                        conceptForRole = AtomicConcept.create("internal:prop#" + ((AtomicRole)objectRole).getIRI());
                        objectPropertyExpression = factory.getOWLObjectProperty(IRI.create(((AtomicRole)objectRole).getIRI()));
                    } else {
                        conceptForRole = AtomicConcept.create("internal:prop#inv#" + ((InverseRole)objectRole).getInverseOf().getIRI());
                        objectPropertyExpression = factory.getOWLObjectInverseOf(factory.getOWLObjectProperty(IRI.create(((InverseRole)objectRole).getInverseOf().getIRI())));
                    }
                    OWLClass classForRole = factory.getOWLClass(IRI.create(conceptForRole.getIRI()));
                    OWLEquivalentClassesAxiom axiom = factory.getOWLEquivalentClassesAxiom(classForRole, factory.getOWLObjectSomeValuesFrom(objectPropertyExpression, freshConcept));
                    additionalAxioms.add(axiom);
                    conceptsForRoles.put(objectRole, conceptForRole);
                    rolesForConcepts.put(conceptForRole, objectRole);
                }
                conceptsForRoles.put(AtomicRole.TOP_OBJECT_ROLE, AtomicConcept.THING);
                rolesForConcepts.put(AtomicConcept.THING, AtomicRole.TOP_OBJECT_ROLE);
                conceptsForRoles.put(AtomicRole.BOTTOM_OBJECT_ROLE, AtomicConcept.NOTHING);
                rolesForConcepts.put(AtomicConcept.NOTHING, AtomicRole.BOTTOM_OBJECT_ROLE);
                OWLAnonymousIndividual freshIndividual = factory.getOWLAnonymousIndividual();
                OWLClassAssertionAxiom axiom = factory.getOWLClassAssertionAxiom(freshConcept, freshIndividual);
                additionalAxioms.add(axiom);
                OWLAxiom[] additionalAxiomsArray = new OWLAxiom[additionalAxioms.size()];
                additionalAxioms.toArray(additionalAxiomsArray);
                Tableau tableau = this.getTableau(additionalAxiomsArray);
                try {
                    final int numberOfRoles = relevantObjectRoles.size();
                    if (this.m_configuration.reasonerProgressMonitor != null) {
                        this.m_configuration.reasonerProgressMonitor.reasonerTaskStarted("Classifying object properties...");
                    }
                    ClassificationProgressMonitor progressMonitor = new ClassificationProgressMonitor(){
                        private int m_processedRoles = 0;

                        @Override
                        public void elementClassified(AtomicConcept element) {
                            ++this.m_processedRoles;
                            if (Reasoner.this.m_configuration.reasonerProgressMonitor != null) {
                                Reasoner.this.m_configuration.reasonerProgressMonitor.reasonerTaskProgressChanged(this.m_processedRoles, numberOfRoles);
                            }
                        }
                    };
                    Hierarchy<AtomicConcept> atomicConceptHierarchyForRoles = this.classifyAtomicConceptsForRoles(tableau, progressMonitor, conceptsForRoles.get(AtomicRole.TOP_OBJECT_ROLE), conceptsForRoles.get(AtomicRole.BOTTOM_OBJECT_ROLE), rolesForConcepts.keySet(), this.m_dlOntology.hasInverseRoles(), conceptsForRoles, rolesForConcepts, this.m_configuration.forceQuasiOrderClassification);
                    Hierarchy.Transformer<AtomicConcept, Role> transformer = new Hierarchy.Transformer<AtomicConcept, Role>(){

                        @Override
                        public Role transform(AtomicConcept atomicConcept) {
                            return rolesForConcepts.get(atomicConcept);
                        }

                        @Override
                        public Role determineRepresentative(AtomicConcept oldRepresentative, Set<Role> newEquivalentElements) {
                            return this.transform(oldRepresentative);
                        }
                    };
                    this.m_objectRoleHierarchy = atomicConceptHierarchyForRoles.transform(transformer, null);
                    if (this.m_instanceManager != null) {
                        this.m_instanceManager.setToClassifiedRoleHierarchy(this.m_objectRoleHierarchy);
                    }
                }
                finally {
                    tableau.clearAdditionalDLOntology();
                    if (this.m_configuration.reasonerProgressMonitor != null) {
                        this.m_configuration.reasonerProgressMonitor.reasonerTaskStopped();
                    }
                }
            }
        }
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
        this.classifyObjectProperties();
        return this.objectPropertyHierarchyNodeToNode(this.m_objectRoleHierarchy.getTopNode());
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
        this.classifyObjectProperties();
        return this.objectPropertyHierarchyNodeToNode(this.m_objectRoleHierarchy.getBottomNode());
    }

    protected boolean isSubObjectPropertyExpressionOf(OWLObjectPropertyExpression subObjectPropertyExpression, OWLObjectPropertyExpression superObjectPropertyExpression) {
        this.checkPreConditions(subObjectPropertyExpression, superObjectPropertyExpression);
        if (!this.m_isConsistent.booleanValue() || subObjectPropertyExpression.getNamedProperty().isOWLBottomObjectProperty() || superObjectPropertyExpression.getNamedProperty().isOWLTopObjectProperty()) {
            return true;
        }
        Role subrole = Reasoner.H(subObjectPropertyExpression);
        Role superrole = Reasoner.H(superObjectPropertyExpression);
        if (this.m_objectRoleHierarchy != null) {
            if (!this.containsFreshEntities(subObjectPropertyExpression, superObjectPropertyExpression)) {
                HierarchyNode<Role> subroleNode = this.m_objectRoleHierarchy.getNodeForElement(subrole);
                return subroleNode.isEquivalentElement(superrole) || subroleNode.isAncestorElement(superrole);
            }
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLClass pseudoNominal = factory.getOWLClass(IRI.create("internal:pseudo-nominal"));
        OWLObjectAllValuesFrom allSuperNotPseudoNominal = factory.getOWLObjectAllValuesFrom(superObjectPropertyExpression, pseudoNominal.getObjectComplementOf());
        OWLAnonymousIndividual freshIndividualA = factory.getOWLAnonymousIndividual("fresh-individual-A");
        OWLAnonymousIndividual freshIndividualB = factory.getOWLAnonymousIndividual("fresh-individual-B");
        OWLObjectPropertyAssertionAxiom subObjectPropertyAssertion = factory.getOWLObjectPropertyAssertionAxiom(subObjectPropertyExpression, freshIndividualA, freshIndividualB);
        OWLClassAssertionAxiom pseudoNominalAssertion = factory.getOWLClassAssertionAxiom(pseudoNominal, freshIndividualB);
        OWLClassAssertionAxiom allSuperNotPseudoNominalAssertion = factory.getOWLClassAssertionAxiom(allSuperNotPseudoNominal, freshIndividualA);
        Tableau tableau = this.getTableau(subObjectPropertyAssertion, pseudoNominalAssertion, allSuperNotPseudoNominalAssertion);
        boolean result = tableau.isSatisfiable(true, null, null, null, null, null, ReasoningTaskDescription.isRoleSubsumedBy(subrole, superrole, true));
        tableau.clearAdditionalDLOntology();
        return !result;
    }

    protected boolean isSubObjectPropertyExpressionOf(List<OWLObjectPropertyExpression> subPropertyChain, OWLObjectPropertyExpression superObjectPropertyExpression) {
        OWLObject[] objects = new OWLObject[subPropertyChain.size() + 1];
        for (int i = 0; i < subPropertyChain.size(); ++i) {
            objects[i] = subPropertyChain.get(i);
        }
        objects[subPropertyChain.size()] = superObjectPropertyExpression;
        this.checkPreConditions(objects);
        if (!this.m_isConsistent.booleanValue() || superObjectPropertyExpression.getNamedProperty().isOWLTopObjectProperty()) {
            return true;
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLClass pseudoNominal = factory.getOWLClass(IRI.create("internal:pseudo-nominal"));
        OWLObjectAllValuesFrom allSuperNotPseudoNominal = factory.getOWLObjectAllValuesFrom(superObjectPropertyExpression, pseudoNominal.getObjectComplementOf());
        OWLAxiom[] additionalAxioms = new OWLAxiom[subPropertyChain.size() + 2];
        int axiomIndex = 0;
        for (OWLObjectPropertyExpression subObjectPropertyExpression : subPropertyChain) {
            OWLAnonymousIndividual first = factory.getOWLAnonymousIndividual("fresh-individual-" + axiomIndex);
            OWLAnonymousIndividual second = factory.getOWLAnonymousIndividual("fresh-individual-" + (axiomIndex + 1));
            additionalAxioms[axiomIndex++] = factory.getOWLObjectPropertyAssertionAxiom(subObjectPropertyExpression, first, second);
        }
        OWLAnonymousIndividual freshIndividual0 = factory.getOWLAnonymousIndividual("fresh-individual-0");
        OWLAnonymousIndividual freshIndividualN = factory.getOWLAnonymousIndividual("fresh-individual-" + subPropertyChain.size());
        additionalAxioms[axiomIndex++] = factory.getOWLClassAssertionAxiom(pseudoNominal, freshIndividualN);
        additionalAxioms[axiomIndex++] = factory.getOWLClassAssertionAxiom(allSuperNotPseudoNominal, freshIndividual0);
        Tableau tableau = this.getTableau(additionalAxioms);
        return !tableau.isSatisfiable(true, null, null, null, null, null, new ReasoningTaskDescription(true, "subproperty chain subsumption"));
    }

    public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(OWLObjectPropertyExpression propertyExpression, boolean direct) {
        HierarchyNode<Role> node = this.getHierarchyNode(propertyExpression);
        Set<HierarchyNode<Role>> result = new HashSet<HierarchyNode<Role>>();
        if (direct) {
            for (HierarchyNode<Role> n : node.getParentNodes()) {
                result.add(n);
            }
        } else {
            result = node.getAncestorNodes();
            result.remove(node);
        }
        return this.objectPropertyHierarchyNodesToNodeSet(result);
    }

    public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(OWLObjectPropertyExpression propertyExpression, boolean direct) {
        HierarchyNode<Role> node = this.getHierarchyNode(propertyExpression);
        Set<HierarchyNode<Role>> result = new HashSet<HierarchyNode<Role>>();
        if (direct) {
            for (HierarchyNode<Role> n : node.getChildNodes()) {
                result.add(n);
            }
        } else {
            result = node.getDescendantNodes();
            result.remove(node);
        }
        return this.objectPropertyHierarchyNodesToNodeSet(result);
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(OWLObjectPropertyExpression propertyExpression) {
        return this.objectPropertyHierarchyNodeToNode(this.getHierarchyNode(propertyExpression));
    }

    public NodeSet<OWLClass> getObjectPropertyDomains(OWLObjectPropertyExpression propertyExpression, boolean direct) {
        this.checkPreConditions(propertyExpression);
        this.classifyClasses();
        if (!this.isConsistent()) {
            return new OWLClassNodeSet(this.getBottomClassNode());
        }
        final Role role = Reasoner.H(propertyExpression);
        Set<HierarchyNode<AtomicConcept>> nodes = this.m_directObjectRoleDomains.get(role);
        if (nodes == null) {
            final Individual freshIndividualA = Individual.createAnonymous("fresh-individual-A");
            Individual freshIndividualB = Individual.createAnonymous("fresh-individual-B");
            final Set<Atom> roleAssertion = Collections.singleton(role.getRoleAssertion(freshIndividualA, freshIndividualB));
            final Tableau tableau = this.getTableau();
            HierarchySearch.SearchPredicate<HierarchyNode<AtomicConcept>> searchPredicate = new HierarchySearch.SearchPredicate<HierarchyNode<AtomicConcept>>(){

                @Override
                public Set<HierarchyNode<AtomicConcept>> getSuccessorElements(HierarchyNode<AtomicConcept> u) {
                    return u.getChildNodes();
                }

                @Override
                public Set<HierarchyNode<AtomicConcept>> getPredecessorElements(HierarchyNode<AtomicConcept> u) {
                    return u.getParentNodes();
                }

                @Override
                public boolean trueOf(HierarchyNode<AtomicConcept> u) {
                    AtomicConcept potentialDomainConcept = u.getRepresentative();
                    return !tableau.isSatisfiable(false, roleAssertion, Collections.singleton(Atom.create(potentialDomainConcept, freshIndividualA)), null, null, null, ReasoningTaskDescription.isDomainOf(potentialDomainConcept, role));
                }
            };
            nodes = HierarchySearch.search(searchPredicate, Collections.singleton(this.m_atomicConceptHierarchy.getTopNode()), null);
            this.m_directObjectRoleDomains.put(role, nodes);
        }
        if (!direct) {
            nodes = HierarchyNode.getAncestorNodes(nodes);
        }
        return this.atomicConceptHierarchyNodesToNodeSet(nodes);
    }

    public NodeSet<OWLClass> getObjectPropertyRanges(OWLObjectPropertyExpression propertyExpression, boolean direct) {
        this.checkPreConditions(propertyExpression);
        this.classifyClasses();
        if (!this.isConsistent()) {
            return new OWLClassNodeSet(this.getBottomClassNode());
        }
        final Role role = Reasoner.H(propertyExpression);
        Set<HierarchyNode<AtomicConcept>> nodes = this.m_directObjectRoleRanges.get(role);
        if (nodes == null) {
            Individual freshIndividualA = Individual.createAnonymous("fresh-individual-A");
            final Individual freshIndividualB = Individual.createAnonymous("fresh-individual-B");
            final Set<Atom> roleAssertion = Collections.singleton(role.getRoleAssertion(freshIndividualA, freshIndividualB));
            final Tableau tableau = this.getTableau();
            HierarchySearch.SearchPredicate<HierarchyNode<AtomicConcept>> searchPredicate = new HierarchySearch.SearchPredicate<HierarchyNode<AtomicConcept>>(){

                @Override
                public Set<HierarchyNode<AtomicConcept>> getSuccessorElements(HierarchyNode<AtomicConcept> u) {
                    return u.getChildNodes();
                }

                @Override
                public Set<HierarchyNode<AtomicConcept>> getPredecessorElements(HierarchyNode<AtomicConcept> u) {
                    return u.getParentNodes();
                }

                @Override
                public boolean trueOf(HierarchyNode<AtomicConcept> u) {
                    AtomicConcept potentialRangeConcept = u.getRepresentative();
                    return !tableau.isSatisfiable(false, roleAssertion, Collections.singleton(Atom.create(potentialRangeConcept, freshIndividualB)), null, null, null, ReasoningTaskDescription.isRangeOf(potentialRangeConcept, role));
                }
            };
            nodes = HierarchySearch.search(searchPredicate, Collections.singleton(this.m_atomicConceptHierarchy.getTopNode()), null);
            this.m_directObjectRoleRanges.put(role, nodes);
        }
        if (!direct) {
            nodes = HierarchyNode.getAncestorNodes(nodes);
        }
        return this.atomicConceptHierarchyNodesToNodeSet(nodes);
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLObjectPropertyExpression> getInverseObjectProperties(OWLObjectPropertyExpression propertyExpression) {
        return this.getEquivalentObjectProperties(propertyExpression.getInverseProperty());
    }

    public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(OWLObjectPropertyExpression propertyExpression) {
        this.checkPreConditions(propertyExpression);
        if (!this.m_isConsistent.booleanValue()) {
            return new OWLObjectPropertyNodeSet();
        }
        this.classifyObjectProperties();
        HashSet<HierarchyNode<Role>> result = new HashSet<HierarchyNode<Role>>();
        if (propertyExpression.getNamedProperty().isOWLTopObjectProperty()) {
            result.add(this.m_objectRoleHierarchy.getBottomNode());
            return this.objectPropertyHierarchyNodesToNodeSet(result);
        }
        if (propertyExpression.isOWLBottomObjectProperty()) {
            HierarchyNode<Role> node = this.m_objectRoleHierarchy.getTopNode();
            result.add(node);
            result.addAll(node.getDescendantNodes());
            return this.objectPropertyHierarchyNodesToNodeSet(result);
        }
        Role role = Reasoner.H(propertyExpression);
        Individual freshIndividualA = Individual.createAnonymous("fresh-individual-A");
        Individual freshIndividualB = Individual.createAnonymous("fresh-individual-B");
        Atom roleAssertion = role.getRoleAssertion(freshIndividualA, freshIndividualB);
        Tableau tableau = this.getTableau();
        HashSet nodesToTest = new HashSet();
        nodesToTest.addAll(this.m_objectRoleHierarchy.getTopNode().getChildNodes());
        while (!nodesToTest.isEmpty()) {
            HierarchyNode nodeToTest = (HierarchyNode)nodesToTest.iterator().next();
            nodesToTest.remove(nodeToTest);
            Role roleToTest = (Role)nodeToTest.getRepresentative();
            Atom roleToTestAssertion = roleToTest.getRoleAssertion(freshIndividualA, freshIndividualB);
            HashSet<Atom> perTestAtoms = new HashSet<Atom>(2);
            perTestAtoms.add(roleAssertion);
            perTestAtoms.add(roleToTestAssertion);
            if (!tableau.isSatisfiable(false, perTestAtoms, null, null, null, null, new ReasoningTaskDescription(true, "disjointness of {0} and {1}", role, roleToTest))) {
                result.addAll(nodeToTest.getDescendantNodes());
                continue;
            }
            nodesToTest.addAll(nodeToTest.getChildNodes());
        }
        if (result.isEmpty()) {
            result.add(this.m_objectRoleHierarchy.getBottomNode());
        }
        return this.objectPropertyHierarchyNodesToNodeSet(result);
    }

    protected boolean isDisjointObjectProperty(OWLObjectPropertyExpression propertyExpression1, OWLObjectPropertyExpression propertyExpression2) {
        this.checkPreConditions(propertyExpression1, propertyExpression2);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        Role role1 = Reasoner.H(propertyExpression1);
        Role role2 = Reasoner.H(propertyExpression2);
        Individual freshIndividualA = Individual.createAnonymous("fresh-individual-A");
        Individual freshIndividualB = Individual.createAnonymous("fresh-individual-B");
        Atom roleAssertion1 = role1.getRoleAssertion(freshIndividualA, freshIndividualB);
        Atom roleAssertion2 = role2.getRoleAssertion(freshIndividualA, freshIndividualB);
        HashSet<Atom> perTestAtoms = new HashSet<Atom>(2);
        perTestAtoms.add(roleAssertion1);
        perTestAtoms.add(roleAssertion2);
        return !this.getTableau().isSatisfiable(false, perTestAtoms, null, null, null, null, new ReasoningTaskDescription(true, "disjointness of {0} and {1}", role1, role2));
    }

    protected boolean isFunctional(OWLObjectPropertyExpression propertyExpression) {
        this.checkPreConditions(propertyExpression);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        Role role = Reasoner.H(propertyExpression);
        Individual freshIndividual = Individual.createAnonymous("fresh-individual");
        Individual freshIndividualA = Individual.createAnonymous("fresh-individual-A");
        Individual freshIndividualB = Individual.createAnonymous("fresh-individual-B");
        HashSet<Atom> assertions = new HashSet<Atom>();
        assertions.add(role.getRoleAssertion(freshIndividual, freshIndividualA));
        assertions.add(role.getRoleAssertion(freshIndividual, freshIndividualB));
        assertions.add(Atom.create(Inequality.INSTANCE, freshIndividualA, freshIndividualB));
        return !this.getTableau().isSatisfiable(false, assertions, null, null, null, null, new ReasoningTaskDescription(true, "functionality of {0}", role));
    }

    protected boolean isInverseFunctional(OWLObjectPropertyExpression propertyExpression) {
        this.checkPreConditions(propertyExpression);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        Role role = Reasoner.H(propertyExpression);
        Individual freshIndividual = Individual.createAnonymous("fresh-individual");
        Individual freshIndividualA = Individual.createAnonymous("fresh-individual-A");
        Individual freshIndividualB = Individual.createAnonymous("fresh-individual-B");
        HashSet<Atom> assertions = new HashSet<Atom>();
        assertions.add(role.getRoleAssertion(freshIndividualA, freshIndividual));
        assertions.add(role.getRoleAssertion(freshIndividualB, freshIndividual));
        assertions.add(Atom.create(Inequality.INSTANCE, freshIndividualA, freshIndividualB));
        return !this.getTableau().isSatisfiable(false, assertions, null, null, null, null, new ReasoningTaskDescription(true, "inverse-functionality of {0}", role));
    }

    protected boolean isIrreflexive(OWLObjectPropertyExpression propertyExpression) {
        this.checkPreConditions(propertyExpression);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        Role role = Reasoner.H(propertyExpression);
        Individual freshIndividual = Individual.createAnonymous("fresh-individual");
        return !this.getTableau().isSatisfiable(false, Collections.singleton(role.getRoleAssertion(freshIndividual, freshIndividual)), null, null, null, null, new ReasoningTaskDescription(true, "irreflexivity of {0}", role));
    }

    protected boolean isReflexive(OWLObjectPropertyExpression propertyExpression) {
        this.checkPreConditions(propertyExpression);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLClass pseudoNominal = factory.getOWLClass(IRI.create("internal:pseudo-nominal"));
        OWLObjectAllValuesFrom allNotPseudoNominal = factory.getOWLObjectAllValuesFrom(propertyExpression, pseudoNominal.getObjectComplementOf());
        OWLAnonymousIndividual freshIndividual = factory.getOWLAnonymousIndividual("fresh-individual");
        OWLClassAssertionAxiom pseudoNominalAssertion = factory.getOWLClassAssertionAxiom(pseudoNominal, freshIndividual);
        OWLClassAssertionAxiom allNotPseudoNominalAssertion = factory.getOWLClassAssertionAxiom(allNotPseudoNominal, freshIndividual);
        Tableau tableau = this.getTableau(pseudoNominalAssertion, allNotPseudoNominalAssertion);
        boolean result = tableau.isSatisfiable(true, null, null, null, null, null, new ReasoningTaskDescription(true, "symmetry of {0}", Reasoner.H(propertyExpression)));
        tableau.clearAdditionalDLOntology();
        return !result;
    }

    protected boolean isAsymmetric(OWLObjectPropertyExpression propertyExpression) {
        this.checkPreConditions(propertyExpression);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLAnonymousIndividual freshIndividualA = factory.getOWLAnonymousIndividual("fresh-individual-A");
        OWLAnonymousIndividual freshIndividualB = factory.getOWLAnonymousIndividual("fresh-individual-B");
        OWLObjectPropertyAssertionAxiom assertion1 = factory.getOWLObjectPropertyAssertionAxiom(propertyExpression, freshIndividualA, freshIndividualB);
        OWLObjectPropertyAssertionAxiom assertion2 = factory.getOWLObjectPropertyAssertionAxiom(propertyExpression.getInverseProperty(), freshIndividualA, freshIndividualB);
        Tableau tableau = this.getTableau(assertion1, assertion2);
        boolean result = tableau.isSatisfiable(true, null, null, null, null, null, new ReasoningTaskDescription(true, "asymmetry of {0}", Reasoner.H(propertyExpression)));
        tableau.clearAdditionalDLOntology();
        return !result;
    }

    protected boolean isSymmetric(OWLObjectPropertyExpression propertyExpression) {
        this.checkPreConditions(propertyExpression);
        if (!this.m_isConsistent.booleanValue() || propertyExpression.getNamedProperty().isOWLTopObjectProperty()) {
            return true;
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLClass pseudoNominal = factory.getOWLClass(IRI.create("internal:pseudo-nominal"));
        OWLObjectAllValuesFrom allNotPseudoNominal = factory.getOWLObjectAllValuesFrom(propertyExpression, pseudoNominal.getObjectComplementOf());
        OWLAnonymousIndividual freshIndividualA = factory.getOWLAnonymousIndividual("fresh-individual-A");
        OWLAnonymousIndividual freshIndividualB = factory.getOWLAnonymousIndividual("fresh-individual-B");
        OWLObjectPropertyAssertionAxiom assertion1 = factory.getOWLObjectPropertyAssertionAxiom(propertyExpression, freshIndividualA, freshIndividualB);
        OWLClassAssertionAxiom assertion2 = factory.getOWLClassAssertionAxiom(allNotPseudoNominal, freshIndividualB);
        OWLClassAssertionAxiom assertion3 = factory.getOWLClassAssertionAxiom(pseudoNominal, freshIndividualA);
        Tableau tableau = this.getTableau(assertion1, assertion2, assertion3);
        boolean result = tableau.isSatisfiable(true, null, null, null, null, null, new ReasoningTaskDescription(true, "symmetry of {0}", propertyExpression));
        tableau.clearAdditionalDLOntology();
        return !result;
    }

    protected boolean isTransitive(OWLObjectPropertyExpression propertyExpression) {
        this.checkPreConditions(propertyExpression);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLClass pseudoNominal = factory.getOWLClass(IRI.create("internal:pseudo-nominal"));
        OWLObjectAllValuesFrom allNotPseudoNominal = factory.getOWLObjectAllValuesFrom(propertyExpression, pseudoNominal.getObjectComplementOf());
        OWLAnonymousIndividual freshIndividualA = factory.getOWLAnonymousIndividual("fresh-individual-A");
        OWLAnonymousIndividual freshIndividualB = factory.getOWLAnonymousIndividual("fresh-individual-B");
        OWLAnonymousIndividual freshIndividualC = factory.getOWLAnonymousIndividual("fresh-individual-C");
        OWLObjectPropertyAssertionAxiom assertion1 = factory.getOWLObjectPropertyAssertionAxiom(propertyExpression, freshIndividualA, freshIndividualB);
        OWLObjectPropertyAssertionAxiom assertion2 = factory.getOWLObjectPropertyAssertionAxiom(propertyExpression, freshIndividualB, freshIndividualC);
        OWLClassAssertionAxiom assertion3 = factory.getOWLClassAssertionAxiom(allNotPseudoNominal, freshIndividualA);
        OWLClassAssertionAxiom assertion4 = factory.getOWLClassAssertionAxiom(pseudoNominal, freshIndividualC);
        Tableau tableau = this.getTableau(assertion1, assertion2, assertion3, assertion4);
        boolean result = tableau.isSatisfiable(true, null, null, null, null, null, new ReasoningTaskDescription(true, "transitivity of {0}", Reasoner.H(propertyExpression)));
        tableau.clearAdditionalDLOntology();
        return !result;
    }

    protected HierarchyNode<Role> getHierarchyNode(OWLObjectPropertyExpression propertyExpression) {
        this.checkPreConditions(propertyExpression);
        this.classifyObjectProperties();
        if (!this.m_isConsistent.booleanValue()) {
            return this.m_objectRoleHierarchy.getBottomNode();
        }
        Role role = Reasoner.H(propertyExpression);
        HierarchyNode<Role> node = this.m_objectRoleHierarchy.getNodeForElement(role);
        if (node == null) {
            node = new HierarchyNode<Role>(role, Collections.singleton(role), Collections.singleton(this.m_objectRoleHierarchy.getTopNode()), Collections.singleton(this.m_objectRoleHierarchy.getBottomNode()));
        }
        return node;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void classifyDataProperties() {
        this.checkPreConditions();
        if (this.m_dataRoleHierarchy == null) {
            HashSet<AtomicRole> relevantDataRoles = new HashSet<AtomicRole>();
            relevantDataRoles.add(AtomicRole.TOP_DATA_ROLE);
            relevantDataRoles.add(AtomicRole.BOTTOM_DATA_ROLE);
            relevantDataRoles.addAll(this.m_dlOntology.getAllAtomicDataRoles());
            if (!this.m_isConsistent.booleanValue()) {
                this.m_dataRoleHierarchy = Hierarchy.emptyHierarchy(relevantDataRoles, AtomicRole.TOP_DATA_ROLE, AtomicRole.BOTTOM_DATA_ROLE);
            } else if (this.m_dlOntology.hasDatatypes()) {
                HashMap<AtomicRole, AtomicConcept> conceptsForRoles = new HashMap<AtomicRole, AtomicConcept>();
                final HashMap<AtomicConcept, AtomicRole> rolesForConcepts = new HashMap<AtomicConcept, AtomicRole>();
                ArrayList<OWLEquivalentClassesAxiom> additionalAxioms = new ArrayList<OWLEquivalentClassesAxiom>();
                OWLDataFactory factory = this.getDataFactory();
                OWLDatatype unknownDatatypeA = factory.getOWLDatatype(IRI.create("internal:unknown-datatype#A"));
                for (AtomicRole dataRole : relevantDataRoles) {
                    AtomicConcept conceptForRole;
                    if (AtomicRole.TOP_DATA_ROLE.equals(dataRole)) {
                        conceptForRole = AtomicConcept.THING;
                    } else if (AtomicRole.BOTTOM_DATA_ROLE.equals(dataRole)) {
                        conceptForRole = AtomicConcept.NOTHING;
                    } else {
                        conceptForRole = AtomicConcept.create("internal:prop#" + dataRole.getIRI());
                        OWLClass classForRole = factory.getOWLClass(IRI.create(conceptForRole.getIRI()));
                        OWLDataProperty dataProperty = factory.getOWLDataProperty(IRI.create(dataRole.getIRI()));
                        OWLEquivalentClassesAxiom axiom = factory.getOWLEquivalentClassesAxiom(classForRole, factory.getOWLDataSomeValuesFrom(dataProperty, unknownDatatypeA));
                        additionalAxioms.add(axiom);
                    }
                    conceptsForRoles.put(dataRole, conceptForRole);
                    rolesForConcepts.put(conceptForRole, dataRole);
                }
                OWLAxiom[] additionalAxiomsArray = new OWLAxiom[additionalAxioms.size()];
                additionalAxioms.toArray(additionalAxiomsArray);
                Tableau tableau = this.getTableau(additionalAxiomsArray);
                try {
                    final int numberOfRoles = relevantDataRoles.size();
                    if (this.m_configuration.reasonerProgressMonitor != null) {
                        this.m_configuration.reasonerProgressMonitor.reasonerTaskStarted("Classifying data properties...");
                    }
                    ClassificationProgressMonitor progressMonitor = new ClassificationProgressMonitor(){
                        private int m_processedRoles = 0;

                        @Override
                        public void elementClassified(AtomicConcept element) {
                            ++this.m_processedRoles;
                            if (Reasoner.this.m_configuration.reasonerProgressMonitor != null) {
                                Reasoner.this.m_configuration.reasonerProgressMonitor.reasonerTaskProgressChanged(this.m_processedRoles, numberOfRoles);
                            }
                        }
                    };
                    Hierarchy<AtomicConcept> atomicConceptHierarchyForRoles = this.classifyAtomicConcepts(tableau, progressMonitor, conceptsForRoles.get(AtomicRole.TOP_DATA_ROLE), conceptsForRoles.get(AtomicRole.BOTTOM_DATA_ROLE), rolesForConcepts.keySet(), this.m_configuration.forceQuasiOrderClassification);
                    Hierarchy.Transformer<AtomicConcept, AtomicRole> transformer = new Hierarchy.Transformer<AtomicConcept, AtomicRole>(){

                        @Override
                        public AtomicRole transform(AtomicConcept atomicConcept) {
                            return rolesForConcepts.get(atomicConcept);
                        }

                        @Override
                        public AtomicRole determineRepresentative(AtomicConcept oldRepresentative, Set<AtomicRole> newEquivalentElements) {
                            return this.transform(oldRepresentative);
                        }
                    };
                    this.m_dataRoleHierarchy = atomicConceptHierarchyForRoles.transform(transformer, null);
                }
                finally {
                    tableau.clearAdditionalDLOntology();
                    if (this.m_configuration.reasonerProgressMonitor != null) {
                        this.m_configuration.reasonerProgressMonitor.reasonerTaskStopped();
                    }
                }
            } else {
                this.m_dataRoleHierarchy = Hierarchy.trivialHierarchy(AtomicRole.TOP_DATA_ROLE, AtomicRole.BOTTOM_DATA_ROLE);
            }
        }
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLDataProperty> getTopDataPropertyNode() {
        this.classifyDataProperties();
        return this.dataPropertyHierarchyNodeToNode(this.m_dataRoleHierarchy.getTopNode());
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLDataProperty> getBottomDataPropertyNode() {
        this.classifyDataProperties();
        return this.dataPropertyHierarchyNodeToNode(this.m_dataRoleHierarchy.getBottomNode());
    }

    protected boolean isSubDataPropertyOf(OWLDataProperty subDataProperty, OWLDataProperty superDataProperty) {
        this.checkPreConditions(subDataProperty, superDataProperty);
        if (!this.m_isConsistent.booleanValue() || subDataProperty.isOWLBottomDataProperty() || superDataProperty.isOWLTopDataProperty()) {
            return true;
        }
        AtomicRole subrole = Reasoner.H(subDataProperty);
        AtomicRole superrole = Reasoner.H(superDataProperty);
        if (this.m_dataRoleHierarchy != null) {
            if (!this.containsFreshEntities(subDataProperty, superDataProperty)) {
                HierarchyNode<AtomicRole> subroleNode = this.m_dataRoleHierarchy.getNodeForElement(subrole);
                return subroleNode.isEquivalentElement(superrole) || subroleNode.isAncestorElement(superrole);
            }
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLAnonymousIndividual individual = factory.getOWLAnonymousIndividual("fresh-individual");
        OWLLiteral freshConstant = factory.getOWLLiteral("internal:fresh-constant", factory.getOWLDatatype(IRI.create("internal:anonymous-constants")));
        OWLDataProperty negatedSuperDataProperty = factory.getOWLDataProperty(IRI.create("internal:negated-superproperty"));
        OWLDataPropertyAssertionAxiom subpropertyAssertion = factory.getOWLDataPropertyAssertionAxiom(subDataProperty, individual, freshConstant);
        OWLDataPropertyAssertionAxiom negatedSuperpropertyAssertion = factory.getOWLDataPropertyAssertionAxiom(negatedSuperDataProperty, individual, freshConstant);
        OWLDisjointDataPropertiesAxiom superpropertyAxiomatization = factory.getOWLDisjointDataPropertiesAxiom(superDataProperty, negatedSuperDataProperty);
        Tableau tableau = this.getTableau(subpropertyAssertion, negatedSuperpropertyAssertion, superpropertyAxiomatization);
        boolean result = tableau.isSatisfiable(true, null, null, null, null, null, ReasoningTaskDescription.isRoleSubsumedBy(subrole, superrole, false));
        tableau.clearAdditionalDLOntology();
        return !result;
    }

    public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty property, boolean direct) {
        Set<HierarchyNode<AtomicRole>> result;
        HierarchyNode<AtomicRole> node = this.getHierarchyNode(property);
        if (direct) {
            result = node.getParentNodes();
        } else {
            result = new HashSet<HierarchyNode<AtomicRole>>(node.getAncestorNodes());
            result.remove(node);
        }
        return this.dataPropertyHierarchyNodesToNodeSet(result);
    }

    public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty property, boolean direct) {
        Set<HierarchyNode<AtomicRole>> result;
        HierarchyNode<AtomicRole> node = this.getHierarchyNode(property);
        if (direct) {
            result = node.getChildNodes();
        } else {
            result = new HashSet<HierarchyNode<AtomicRole>>(node.getDescendantNodes());
            result.remove(node);
        }
        return this.dataPropertyHierarchyNodesToNodeSet(result);
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty property) {
        return this.dataPropertyHierarchyNodeToNode(this.getHierarchyNode(property));
    }

    public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty property, boolean direct) {
        this.checkPreConditions(property);
        this.classifyClasses();
        if (!this.m_isConsistent.booleanValue()) {
            return new OWLClassNodeSet(this.getBottomClassNode());
        }
        final AtomicRole atomicRole = Reasoner.H(property);
        Set<HierarchyNode<AtomicConcept>> nodes = this.m_directDataRoleDomains.get(atomicRole);
        if (nodes == null) {
            final Individual freshIndividual = Individual.createAnonymous("fresh-individual");
            Constant freshConstant = Constant.createAnonymous("fresh-constant");
            final Set<Atom> roleAssertion = Collections.singleton(atomicRole.getRoleAssertion(freshIndividual, freshConstant));
            final Tableau tableau = this.getTableau();
            HierarchySearch.SearchPredicate<HierarchyNode<AtomicConcept>> searchPredicate = new HierarchySearch.SearchPredicate<HierarchyNode<AtomicConcept>>(){

                @Override
                public Set<HierarchyNode<AtomicConcept>> getSuccessorElements(HierarchyNode<AtomicConcept> u) {
                    return u.getChildNodes();
                }

                @Override
                public Set<HierarchyNode<AtomicConcept>> getPredecessorElements(HierarchyNode<AtomicConcept> u) {
                    return u.getParentNodes();
                }

                @Override
                public boolean trueOf(HierarchyNode<AtomicConcept> u) {
                    AtomicConcept potentialDomainConcept = u.getRepresentative();
                    return !tableau.isSatisfiable(false, roleAssertion, Collections.singleton(Atom.create(potentialDomainConcept, freshIndividual)), null, null, null, ReasoningTaskDescription.isDomainOf(potentialDomainConcept, atomicRole));
                }
            };
            nodes = HierarchySearch.search(searchPredicate, Collections.singleton(this.m_atomicConceptHierarchy.getTopNode()), null);
            this.m_directDataRoleDomains.put(atomicRole, nodes);
        }
        if (!direct) {
            nodes = HierarchyNode.getAncestorNodes(nodes);
        }
        return this.atomicConceptHierarchyNodesToNodeSet(nodes);
    }

    public NodeSet<OWLDataProperty> getDisjointDataProperties(OWLDataPropertyExpression propertyExpression) {
        this.checkPreConditions(propertyExpression);
        if (this.m_dlOntology.hasDatatypes()) {
            this.classifyDataProperties();
            if (!this.m_isConsistent.booleanValue()) {
                return new OWLDataPropertyNodeSet();
            }
            HashSet<HierarchyNode<AtomicRole>> result = new HashSet<HierarchyNode<AtomicRole>>();
            if (propertyExpression.isOWLTopDataProperty()) {
                result.add(this.m_dataRoleHierarchy.getBottomNode());
                return this.dataPropertyHierarchyNodesToNodeSet(result);
            }
            if (propertyExpression.isOWLBottomDataProperty()) {
                HierarchyNode<AtomicRole> node = this.m_dataRoleHierarchy.getTopNode();
                result.add(node);
                result.addAll(node.getDescendantNodes());
                return this.dataPropertyHierarchyNodesToNodeSet(result);
            }
            AtomicRole atomicRole = Reasoner.H(propertyExpression.asOWLDataProperty());
            Individual freshIndividual = Individual.create("fresh-individual");
            Constant freshConstant = Constant.createAnonymous("fresh-constant");
            Atom atomicRoleAssertion = atomicRole.getRoleAssertion(freshIndividual, freshConstant);
            Tableau tableau = this.getTableau();
            HashSet nodesToTest = new HashSet();
            nodesToTest.addAll(this.m_dataRoleHierarchy.getTopNode().getChildNodes());
            while (!nodesToTest.isEmpty()) {
                HierarchyNode nodeToTest = (HierarchyNode)nodesToTest.iterator().next();
                nodesToTest.remove(nodeToTest);
                AtomicRole atomicRoleToTest = (AtomicRole)nodeToTest.getRepresentative();
                Atom atomicRoleToTestAssertion = atomicRoleToTest.getRoleAssertion(freshIndividual, freshConstant);
                HashSet<Atom> perTestAtoms = new HashSet<Atom>(2);
                perTestAtoms.add(atomicRoleAssertion);
                perTestAtoms.add(atomicRoleToTestAssertion);
                if (!tableau.isSatisfiable(false, perTestAtoms, null, null, null, null, new ReasoningTaskDescription(true, "disjointness of {0} and {1}", atomicRole, atomicRoleToTest))) {
                    result.addAll(nodeToTest.getDescendantNodes());
                    continue;
                }
                nodesToTest.addAll(nodeToTest.getChildNodes());
            }
            if (result.isEmpty()) {
                result.add(this.m_dataRoleHierarchy.getBottomNode());
            }
            return this.dataPropertyHierarchyNodesToNodeSet(result);
        }
        OWLDataFactory factory = this.getDataFactory();
        if (propertyExpression.isOWLTopDataProperty() && this.isConsistent()) {
            return new OWLDataPropertyNodeSet(new OWLDataPropertyNode(factory.getOWLBottomDataProperty()));
        }
        if (propertyExpression.isOWLBottomDataProperty() && this.isConsistent()) {
            return new OWLDataPropertyNodeSet(new OWLDataPropertyNode(factory.getOWLTopDataProperty()));
        }
        return new OWLDataPropertyNodeSet();
    }

    protected boolean isFunctional(OWLDataProperty property) {
        this.checkPreConditions(property);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        AtomicRole atomicRole = Reasoner.H(property);
        Individual freshIndividual = Individual.createAnonymous("fresh-individual");
        Constant freshConstantA = Constant.createAnonymous("fresh-constant-A");
        Constant freshConstantB = Constant.createAnonymous("fresh-constant-B");
        HashSet<Atom> assertions = new HashSet<Atom>();
        assertions.add(atomicRole.getRoleAssertion(freshIndividual, freshConstantA));
        assertions.add(atomicRole.getRoleAssertion(freshIndividual, freshConstantB));
        assertions.add(Atom.create(Inequality.INSTANCE, freshConstantA, freshConstantB));
        return !this.getTableau().isSatisfiable(false, assertions, null, null, null, null, new ReasoningTaskDescription(true, "functionality of {0}", atomicRole));
    }

    protected HierarchyNode<AtomicRole> getHierarchyNode(OWLDataProperty property) {
        this.checkPreConditions(property);
        this.classifyDataProperties();
        if (!this.m_isConsistent.booleanValue()) {
            return this.m_dataRoleHierarchy.getBottomNode();
        }
        AtomicRole atomicRole = Reasoner.H(property);
        HierarchyNode<AtomicRole> node = this.m_dataRoleHierarchy.getNodeForElement(atomicRole);
        if (node == null) {
            node = new HierarchyNode<AtomicRole>(atomicRole, Collections.singleton(atomicRole), Collections.singleton(this.m_dataRoleHierarchy.getTopNode()), Collections.singleton(this.m_dataRoleHierarchy.getBottomNode()));
        }
        return node;
    }

    protected void realise() {
        this.checkPreConditions();
        if (this.m_dlOntology.getAllIndividuals().size() > 0) {
            this.classifyClasses();
            this.initialiseClassInstanceManager();
            this.m_instanceManager.realize(this.m_configuration.reasonerProgressMonitor);
        }
    }

    public void realiseObjectProperties() {
        this.checkPreConditions();
        if (this.m_dlOntology.getAllIndividuals().size() > 0) {
            this.classifyObjectProperties();
            this.initialisePropertiesInstanceManager();
            this.m_instanceManager.realizeObjectRoles(this.m_configuration.reasonerProgressMonitor);
        }
    }

    public void precomputeSameAsEquivalenceClasses() {
        this.checkPreConditions();
        if (this.m_dlOntology.getAllIndividuals().size() > 0) {
            this.initialiseClassInstanceManager();
            this.m_instanceManager.computeSameAsEquivalenceClasses(this.m_configuration.reasonerProgressMonitor);
        }
    }

    public NodeSet<OWLClass> getTypes(OWLNamedIndividual namedIndividual, boolean direct) {
        Set<HierarchyNode<AtomicConcept>> result;
        this.checkPreConditions(namedIndividual);
        if (!this.isDefined(namedIndividual)) {
            this.classifyClasses();
            result = new HashSet<HierarchyNode<AtomicConcept>>();
            result.add(this.m_atomicConceptHierarchy.getTopNode());
        } else {
            if (direct) {
                this.classifyClasses();
            }
            this.initialiseClassInstanceManager();
            if (direct) {
                this.m_instanceManager.setToClassifiedConceptHierarchy(this.m_atomicConceptHierarchy);
            }
            result = this.m_instanceManager.getTypes(Reasoner.H(namedIndividual), direct);
        }
        return this.atomicConceptHierarchyNodesToNodeSet(result);
    }

    public boolean hasType(OWLNamedIndividual namedIndividual, OWLClassExpression type, boolean direct) {
        this.checkPreConditions(namedIndividual, type);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        if (!this.isDefined(namedIndividual)) {
            return this.getEquivalentClasses(type).contains(this.df.getOWLThing());
        }
        if (type instanceof OWLClass) {
            if (direct) {
                this.classifyClasses();
            }
            this.initialiseClassInstanceManager();
            if (direct) {
                this.m_instanceManager.setToClassifiedConceptHierarchy(this.m_atomicConceptHierarchy);
            }
            return this.m_instanceManager.hasType(Reasoner.H(namedIndividual), Reasoner.H((OWLClass)type), direct);
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLClassAssertionAxiom negatedAssertionAxiom = factory.getOWLClassAssertionAxiom(type.getObjectComplementOf(), namedIndividual);
        Tableau tableau = this.getTableau(negatedAssertionAxiom);
        boolean result = tableau.isSatisfiable(true, true, null, null, null, null, null, ReasoningTaskDescription.isInstanceOf(namedIndividual, type));
        tableau.clearAdditionalDLOntology();
        return !result;
    }

    public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression classExpression, boolean direct) {
        if (this.m_dlOntology.getAllIndividuals().size() > 0) {
            this.checkPreConditions(classExpression);
            if (!this.m_isConsistent.booleanValue()) {
                OWLNamedIndividualNode node = new OWLNamedIndividualNode(this.getAllNamedIndividuals());
                return new OWLNamedIndividualNodeSet(Collections.singleton(node));
            }
            if (direct || !(classExpression instanceof OWLClass)) {
                this.classifyClasses();
            }
            this.initialiseClassInstanceManager();
            Set<Individual> result = new HashSet();
            if (classExpression instanceof OWLClass) {
                result = this.m_instanceManager.getInstances(Reasoner.H((OWLClass)classExpression), direct);
            } else {
                HierarchyNode<AtomicConcept> hierarchyNode = this.getHierarchyNode(classExpression);
                result = this.m_instanceManager.getInstances(hierarchyNode, direct);
                OWLDataFactory factory = this.getDataFactory();
                OWLClass queryClass = factory.getOWLClass(IRI.create("internal:query-concept"));
                OWLSubClassOfAxiom queryClassDefinition = factory.getOWLSubClassOfAxiom(queryClass, classExpression.getObjectComplementOf());
                AtomicConcept queryConcept = AtomicConcept.create("internal:query-concept");
                HashSet<HierarchyNode<AtomicConcept>> visitedNodes = new HashSet<HierarchyNode<AtomicConcept>>(hierarchyNode.getChildNodes());
                ArrayList<HierarchyNode<AtomicConcept>> toVisit = new ArrayList<HierarchyNode<AtomicConcept>>(hierarchyNode.getParentNodes());
                while (!toVisit.isEmpty()) {
                    HierarchyNode node = toVisit.remove(toVisit.size() - 1);
                    if (!visitedNodes.add(node)) continue;
                    Set<Individual> realizationForNodeConcept = this.m_instanceManager.getInstances(node, true);
                    if (realizationForNodeConcept != null) {
                        Tableau tableau = this.getTableau(queryClassDefinition);
                        for (Individual individual : realizationForNodeConcept) {
                            if (!Reasoner.isResultRelevantIndividual(individual)) continue;
                            if (tableau.isSatisfiable(true, true, Collections.singleton(Atom.create(queryConcept, individual)), null, null, null, null, ReasoningTaskDescription.isInstanceOf(individual, classExpression))) continue;
                            result.add(individual);
                        }
                        tableau.clearAdditionalDLOntology();
                    }
                    toVisit.addAll(node.getChildNodes());
                }
            }
            return this.sortBySameAsIfNecessary(result);
        }
        return new OWLNamedIndividualNodeSet(new HashSet());
    }

    public boolean isSameIndividual(OWLNamedIndividual namedIndividual1, OWLNamedIndividual namedIndividual2) {
        this.checkPreConditions(namedIndividual1, namedIndividual2);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        if (this.m_dlOntology.getAllIndividuals().size() == 0) {
            return false;
        }
        this.initialiseClassInstanceManager();
        this.m_instanceManager.computeSameAsEquivalenceClasses(this.m_configuration.reasonerProgressMonitor);
        return this.m_instanceManager.isSameIndividual(Reasoner.H(namedIndividual1), Reasoner.H(namedIndividual2));
    }

    public org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual namedIndividual) {
        this.checkPreConditions(namedIndividual);
        if (!this.m_isConsistent.booleanValue()) {
            return new OWLNamedIndividualNode(this.getAllNamedIndividuals());
        }
        if (this.m_dlOntology.getAllIndividuals().size() == 0 || !this.m_dlOntology.containsIndividual(Reasoner.H(namedIndividual))) {
            return new OWLNamedIndividualNode(namedIndividual);
        }
        this.initialiseClassInstanceManager();
        Set<Individual> sameIndividuals = this.m_instanceManager.getSameAsIndividuals(Reasoner.H(namedIndividual));
        OWLDataFactory factory = this.getDataFactory();
        HashSet<OWLNamedIndividual> result = new HashSet<OWLNamedIndividual>();
        for (Individual individual : sameIndividuals) {
            result.add(factory.getOWLNamedIndividual(IRI.create(individual.getIRI())));
        }
        return new OWLNamedIndividualNode(result);
    }

    public NodeSet<OWLNamedIndividual> getDifferentIndividuals(OWLNamedIndividual namedIndividual) {
        this.checkPreConditions(namedIndividual);
        if (!this.m_isConsistent.booleanValue()) {
            OWLNamedIndividualNode node = new OWLNamedIndividualNode(this.getAllNamedIndividuals());
            return new OWLNamedIndividualNodeSet(Collections.singleton(node));
        }
        Individual individual = Reasoner.H(namedIndividual);
        Tableau tableau = this.getTableau();
        HashSet<Individual> result = new HashSet<Individual>();
        for (Individual potentiallyDifferentIndividual : this.m_dlOntology.getAllIndividuals()) {
            if (!Reasoner.isResultRelevantIndividual(potentiallyDifferentIndividual) || individual.equals(potentiallyDifferentIndividual)) continue;
            if (tableau.isSatisfiable(true, true, Collections.singleton(Atom.create(Equality.INSTANCE, individual, potentiallyDifferentIndividual)), null, null, null, null, new ReasoningTaskDescription(true, "is {0} different from {1}", individual, potentiallyDifferentIndividual))) continue;
            result.add(potentiallyDifferentIndividual);
        }
        return this.sortBySameAsIfNecessary(result);
    }

    public NodeSet<OWLNamedIndividual> getObjectPropertyValues(OWLNamedIndividual namedIndividual, OWLObjectPropertyExpression propertyExpression) {
        this.checkPreConditions(namedIndividual, propertyExpression);
        if (!this.m_isConsistent.booleanValue()) {
            OWLNamedIndividualNode node = new OWLNamedIndividualNode(this.getAllNamedIndividuals());
            return new OWLNamedIndividualNodeSet(Collections.singleton(node));
        }
        AtomicRole role = Reasoner.H(propertyExpression.getNamedProperty());
        if (!this.m_dlOntology.containsObjectRole(role)) {
            return new OWLNamedIndividualNodeSet();
        }
        this.initialisePropertiesInstanceManager();
        Individual individual = Reasoner.H(namedIndividual);
        Set<Individual> result = propertyExpression.isAnonymous() ? this.m_instanceManager.getObjectPropertySubjects(role, individual) : this.m_instanceManager.getObjectPropertyValues(role, individual);
        return this.sortBySameAsIfNecessary(result);
    }

    public Map<OWLNamedIndividual, Set<OWLNamedIndividual>> getObjectPropertyInstances(OWLObjectProperty property) {
        this.checkPreConditions(property);
        HashMap<OWLNamedIndividual, Set<OWLNamedIndividual>> result = new HashMap<OWLNamedIndividual, Set<OWLNamedIndividual>>();
        if (!this.m_isConsistent.booleanValue()) {
            Set<OWLNamedIndividual> all = this.getAllNamedIndividuals();
            for (OWLNamedIndividual ind : all) {
                result.put(ind, all);
            }
            return result;
        }
        this.initialisePropertiesInstanceManager();
        AtomicRole role = Reasoner.H(property);
        Map<Individual, Set<Individual>> relations = this.m_instanceManager.getObjectPropertyInstances(role);
        OWLDataFactory factory = this.getDataFactory();
        for (Individual individual : relations.keySet()) {
            HashSet<OWLNamedIndividual> successors = new HashSet<OWLNamedIndividual>();
            result.put(factory.getOWLNamedIndividual(IRI.create(individual.getIRI())), successors);
            for (Individual successorIndividual : relations.get(individual)) {
                successors.add(factory.getOWLNamedIndividual(IRI.create(successorIndividual.getIRI())));
            }
        }
        return result;
    }

    public boolean hasObjectPropertyRelationship(OWLNamedIndividual subject, OWLObjectPropertyExpression propertyExpression, OWLNamedIndividual object) {
        this.checkPreConditions(subject, propertyExpression, object);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        this.initialisePropertiesInstanceManager();
        OWLObjectProperty property = propertyExpression.getNamedProperty();
        if (propertyExpression.isAnonymous()) {
            OWLNamedIndividual tmp = subject;
            subject = object;
            object = tmp;
        }
        AtomicRole role = Reasoner.H(property);
        Individual subj = Reasoner.H(subject);
        Individual obj = Reasoner.H(object);
        return this.m_instanceManager.hasObjectRoleRelationship(role, subj, obj);
    }

    public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual namedIndividual, OWLDataProperty property) {
        this.checkPreConditions(namedIndividual, property);
        HashSet<OWLLiteral> result = new HashSet<OWLLiteral>();
        if (this.m_dlOntology.hasDatatypes()) {
            OWLDataFactory factory = this.getDataFactory();
            Set<OWLDataProperty> relevantDataProperties = this.getSubDataProperties(property, false).getFlattened();
            relevantDataProperties.add(property);
            Set<OWLNamedIndividual> relevantIndividuals = this.getSameIndividuals(namedIndividual).getEntities();
            for (OWLDataProperty dataProperty : relevantDataProperties) {
                if (dataProperty.isBottomEntity()) continue;
                AtomicRole atomicRole = Reasoner.H(dataProperty);
                Map<Individual, Set<Constant>> dataPropertyAssertions = this.m_dlOntology.getDataPropertyAssertions().get(atomicRole);
                if (dataPropertyAssertions == null) continue;
                for (OWLNamedIndividual ind : relevantIndividuals) {
                    Individual individual = Reasoner.H(ind);
                    if (!dataPropertyAssertions.containsKey(individual)) continue;
                    for (Constant constant : dataPropertyAssertions.get(individual)) {
                        OWLLiteral literal;
                        String lexicalForm = constant.getLexicalForm();
                        String datatypeURI = constant.getDatatypeURI();
                        if ((Prefixes.s_semanticWebPrefixes.get("rdf:") + "PlainLiteral").equals(datatypeURI)) {
                            int atPosition = lexicalForm.lastIndexOf(64);
                            literal = factory.getOWLLiteral(lexicalForm.substring(0, atPosition), lexicalForm.substring(atPosition + 1));
                        } else {
                            literal = factory.getOWLLiteral(lexicalForm, factory.getOWLDatatype(IRI.create(datatypeURI)));
                        }
                        result.add(literal);
                    }
                }
            }
        }
        return result;
    }

    public boolean hasDataPropertyRelationship(OWLNamedIndividual subject, OWLDataProperty property, OWLLiteral object) {
        this.checkPreConditions(subject, property);
        if (!this.m_isConsistent.booleanValue()) {
            return true;
        }
        OWLDataFactory factory = this.getDataFactory();
        OWLNegativeDataPropertyAssertionAxiom notAssertion = factory.getOWLNegativeDataPropertyAssertionAxiom(property, subject, object);
        Tableau tableau = this.getTableau(notAssertion);
        boolean result = tableau.isSatisfiable(true, true, null, null, null, null, null, new ReasoningTaskDescription(true, "is {0} connected to {1} via {2}", Reasoner.H(subject), object, Reasoner.H(property)));
        tableau.clearAdditionalDLOntology();
        return !result;
    }

    protected Set<HierarchyNode<AtomicConcept>> getDirectSuperConceptNodes(final Individual individual) {
        HierarchySearch.SearchPredicate<HierarchyNode<AtomicConcept>> predicate = new HierarchySearch.SearchPredicate<HierarchyNode<AtomicConcept>>(){

            @Override
            public Set<HierarchyNode<AtomicConcept>> getSuccessorElements(HierarchyNode<AtomicConcept> u) {
                return u.getChildNodes();
            }

            @Override
            public Set<HierarchyNode<AtomicConcept>> getPredecessorElements(HierarchyNode<AtomicConcept> u) {
                return u.getParentNodes();
            }

            @Override
            public boolean trueOf(HierarchyNode<AtomicConcept> u) {
                AtomicConcept atomicConcept = u.getRepresentative();
                if (AtomicConcept.THING.equals(atomicConcept)) {
                    return true;
                }
                return !Reasoner.this.getTableau().isSatisfiable(true, true, null, Collections.singleton(Atom.create(atomicConcept, individual)), null, null, null, ReasoningTaskDescription.isInstanceOf(atomicConcept, individual));
            }
        };
        return HierarchySearch.search(predicate, Collections.singleton(this.m_atomicConceptHierarchy.getTopNode()), null);
    }

    protected NodeSet<OWLNamedIndividual> sortBySameAsIfNecessary(Set<Individual> individuals) {
        Set result;
        OWLDataFactory factory = this.getDataFactory();
        result = new HashSet<OWLNamedIndividualNode>();
        if (this.m_configuration.individualNodeSetPolicy == IndividualNodeSetPolicy.BY_SAME_AS) {
            while (!individuals.isEmpty()) {
                this.initialiseClassInstanceManager();
                Individual individual = individuals.iterator().next();
                Set<Individual> sameIndividuals = this.m_instanceManager.getSameAsIndividuals(individual);
                HashSet<OWLNamedIndividual> sameNamedIndividuals = new HashSet<OWLNamedIndividual>();
                for (Individual sameIndividual : sameIndividuals) {
                    sameNamedIndividuals.add(factory.getOWLNamedIndividual(IRI.create(sameIndividual.getIRI())));
                }
                individuals.removeAll(sameIndividuals);
                result.add(new OWLNamedIndividualNode(sameNamedIndividuals));
            }
        } else {
            for (Individual individual : individuals) {
                result.add(new OWLNamedIndividualNode(factory.getOWLNamedIndividual(IRI.create(individual.getIRI()))));
            }
        }
        return new OWLNamedIndividualNodeSet(result);
    }

    protected Set<OWLNamedIndividual> getAllNamedIndividuals() {
        HashSet<OWLNamedIndividual> result = new HashSet<OWLNamedIndividual>();
        OWLDataFactory factory = this.getDataFactory();
        for (Individual individual : this.m_dlOntology.getAllIndividuals()) {
            if (!Reasoner.isResultRelevantIndividual(individual)) continue;
            result.add(factory.getOWLNamedIndividual(IRI.create(individual.getIRI())));
        }
        return result;
    }

    protected static boolean isResultRelevantIndividual(Individual individual) {
        return !individual.isAnonymous() && !Prefixes.isInternalIRI(individual.getIRI());
    }

    public Tableau getTableau() {
        this.m_tableau.clearAdditionalDLOntology();
        return this.m_tableau;
    }

    public /* varargs */ Tableau getTableau(OWLAxiom ... additionalAxioms) throws IllegalArgumentException {
        if (additionalAxioms == null || additionalAxioms.length == 0) {
            return this.getTableau();
        }
        DLOntology deltaDLOntology = this.createDeltaDLOntology(this.m_configuration, this.m_dlOntology, additionalAxioms);
        if (this.m_tableau.supportsAdditionalDLOntology(deltaDLOntology)) {
            this.m_tableau.setAdditionalDLOntology(deltaDLOntology);
            return this.m_tableau;
        }
        return Reasoner.createTableau(this.m_interruptFlag, this.m_configuration, this.m_dlOntology, deltaDLOntology, this.m_prefixes);
    }

    protected static Tableau createTableau(InterruptFlag interruptFlag, Configuration configuration, DLOntology permanentDLOntology, DLOntology additionalDLOntology, Prefixes prefixes) throws IllegalArgumentException {
        boolean hasInverseRoles = permanentDLOntology.hasInverseRoles() || additionalDLOntology != null && additionalDLOntology.hasInverseRoles();
        boolean hasNominals = permanentDLOntology.hasNominals() || additionalDLOntology != null && additionalDLOntology.hasNominals();
        TableauMonitor wellKnownTableauMonitor = null;
        switch (configuration.tableauMonitorType) {
            case NONE: {
                break;
            }
            case TIMING: {
                wellKnownTableauMonitor = new Timer(System.out);
                break;
            }
            case TIMING_WITH_PAUSE: {
                wellKnownTableauMonitor = new TimerWithPause(System.out);
                break;
            }
            case DEBUGGER_HISTORY_ON: {
                wellKnownTableauMonitor = new Debugger(prefixes, true);
                break;
            }
            case DEBUGGER_NO_HISTORY: {
                wellKnownTableauMonitor = new Debugger(prefixes, false);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown monitor type");
            }
        }
        TableauMonitor tableauMonitor = null;
        tableauMonitor = configuration.monitor == null ? wellKnownTableauMonitor : (wellKnownTableauMonitor == null ? configuration.monitor : new TableauMonitorFork(wellKnownTableauMonitor, configuration.monitor));
        DirectBlockingChecker directBlockingChecker = null;
        switch (configuration.directBlockingType) {
            case OPTIMAL: {
                if (configuration.blockingStrategyType == Configuration.BlockingStrategyType.SIMPLE_CORE || configuration.blockingStrategyType == Configuration.BlockingStrategyType.COMPLEX_CORE) {
                    directBlockingChecker = new ValidatedSingleDirectBlockingChecker(hasInverseRoles);
                    break;
                }
                if (hasInverseRoles) {
                    directBlockingChecker = new PairWiseDirectBlockingChecker();
                    break;
                }
                directBlockingChecker = new SingleDirectBlockingChecker();
                break;
            }
            case SINGLE: {
                if (configuration.blockingStrategyType == Configuration.BlockingStrategyType.SIMPLE_CORE || configuration.blockingStrategyType == Configuration.BlockingStrategyType.COMPLEX_CORE) {
                    directBlockingChecker = new ValidatedSingleDirectBlockingChecker(hasInverseRoles);
                    break;
                }
                directBlockingChecker = new SingleDirectBlockingChecker();
                break;
            }
            case PAIR_WISE: {
                if (configuration.blockingStrategyType == Configuration.BlockingStrategyType.SIMPLE_CORE || configuration.blockingStrategyType == Configuration.BlockingStrategyType.COMPLEX_CORE) {
                    directBlockingChecker = new ValidatedPairwiseDirectBlockingChecker(hasInverseRoles);
                    break;
                }
                directBlockingChecker = new PairWiseDirectBlockingChecker();
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown direct blocking type.");
            }
        }
        BlockingSignatureCache blockingSignatureCache = null;
        if (!hasNominals && configuration.blockingStrategyType != Configuration.BlockingStrategyType.SIMPLE_CORE && configuration.blockingStrategyType != Configuration.BlockingStrategyType.COMPLEX_CORE) {
            switch (configuration.blockingSignatureCacheType) {
                case CACHED: {
                    blockingSignatureCache = new BlockingSignatureCache(directBlockingChecker);
                    break;
                }
                case NOT_CACHED: {
                    blockingSignatureCache = null;
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unknown blocking cache type.");
                }
            }
        }
        BlockingStrategy blockingStrategy = null;
        switch (configuration.blockingStrategyType) {
            case ANCESTOR: {
                blockingStrategy = new AncestorBlocking(directBlockingChecker, blockingSignatureCache);
                break;
            }
            case ANYWHERE: {
                blockingStrategy = new AnywhereBlocking(directBlockingChecker, blockingSignatureCache);
                break;
            }
            case SIMPLE_CORE: {
                blockingStrategy = new AnywhereValidatedBlocking(directBlockingChecker, true);
                break;
            }
            case COMPLEX_CORE: {
                blockingStrategy = new AnywhereValidatedBlocking(directBlockingChecker, false);
                break;
            }
            case OPTIMAL: {
                blockingStrategy = new AnywhereBlocking(directBlockingChecker, blockingSignatureCache);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown blocking strategy type.");
            }
        }
        AbstractExpansionStrategy existentialsExpansionStrategy = null;
        switch (configuration.existentialStrategyType) {
            case CREATION_ORDER: {
                existentialsExpansionStrategy = new CreationOrderStrategy(blockingStrategy);
                break;
            }
            case EL: {
                existentialsExpansionStrategy = new IndividualReuseStrategy(blockingStrategy, true);
                break;
            }
            case INDIVIDUAL_REUSE: {
                existentialsExpansionStrategy = new IndividualReuseStrategy(blockingStrategy, false);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown expansion strategy type.");
            }
        }
        return new Tableau(interruptFlag, tableauMonitor, existentialsExpansionStrategy, configuration.useDisjunctionLearning, permanentDLOntology, additionalDLOntology, configuration.parameters);
    }

    protected Hierarchy<AtomicConcept> classifyAtomicConcepts(Tableau tableau, ClassificationProgressMonitor progressMonitor, AtomicConcept topElement, AtomicConcept bottomElement, Set<AtomicConcept> elements, boolean forceQuasiOrder) {
        if (tableau.isDeterministic() && !forceQuasiOrder) {
            return new DeterministicClassification(tableau, progressMonitor, topElement, bottomElement, elements).classify();
        }
        return new QuasiOrderClassification(tableau, progressMonitor, topElement, bottomElement, elements).classify();
    }

    protected Hierarchy<AtomicConcept> classifyAtomicConceptsForRoles(Tableau tableau, ClassificationProgressMonitor progressMonitor, AtomicConcept topElement, AtomicConcept bottomElement, Set<AtomicConcept> elements, boolean hasInverses, Map<Role, AtomicConcept> conceptsForRoles, Map<AtomicConcept, Role> rolesForConcepts, boolean forceQuasiOrder) {
        if (tableau.isDeterministic() && !forceQuasiOrder) {
            return new DeterministicClassification(tableau, progressMonitor, topElement, bottomElement, elements).classify();
        }
        return new QuasiOrderClassificationForRoles(tableau, progressMonitor, topElement, bottomElement, elements, hasInverses, conceptsForRoles, rolesForConcepts).classify();
    }

    protected /* varargs */ DLOntology createDeltaDLOntology(Configuration configuration, DLOntology originalDLOntology, OWLAxiom ... additionalAxioms) throws IllegalArgumentException {
        HashSet<OWLAxiom> additionalAxiomsSet = new HashSet<OWLAxiom>();
        for (OWLAxiom axiom : additionalAxioms) {
            if (Reasoner.isUnsupportedExtensionAxiom(axiom)) {
                throw new IllegalArgumentException("Internal error: unsupported extension axiom type.");
            }
            additionalAxiomsSet.add(axiom);
        }
        OWLDataFactory dataFactory = this.getDataFactory();
        OWLAxioms axioms = new OWLAxioms();
        axioms.m_definedDatatypesIRIs.addAll(originalDLOntology.getDefinedDatatypeIRIs());
        OWLNormalization normalization = new OWLNormalization(dataFactory, axioms, originalDLOntology.getAllAtomicConcepts().size());
        normalization.processAxioms(additionalAxiomsSet);
        BuiltInPropertyManager builtInPropertyManager = new BuiltInPropertyManager(dataFactory);
        builtInPropertyManager.axiomatizeBuiltInPropertiesAsNeeded(axioms, originalDLOntology.getAllAtomicObjectRoles().contains(AtomicRole.TOP_OBJECT_ROLE), originalDLOntology.getAllAtomicObjectRoles().contains(AtomicRole.BOTTOM_OBJECT_ROLE), originalDLOntology.getAllAtomicObjectRoles().contains(AtomicRole.TOP_DATA_ROLE), originalDLOntology.getAllAtomicObjectRoles().contains(AtomicRole.BOTTOM_DATA_ROLE));
        int currentReplacementIndex = this.m_objectPropertyInclusionManager.rewriteNegativeObjectPropertyAssertions(dataFactory, axioms, originalDLOntology.getAllAtomicConcepts().size());
        this.m_objectPropertyInclusionManager.rewriteAxioms(dataFactory, axioms, currentReplacementIndex);
        OWLAxiomsExpressivity axiomsExpressivity = new OWLAxiomsExpressivity(axioms);
        axiomsExpressivity.m_hasAtMostRestrictions |= originalDLOntology.hasAtMostRestrictions();
        axiomsExpressivity.m_hasInverseRoles |= originalDLOntology.hasInverseRoles();
        axiomsExpressivity.m_hasNominals |= originalDLOntology.hasNominals();
        axiomsExpressivity.m_hasDatatypes |= originalDLOntology.hasDatatypes();
        OWLClausification clausifier = new OWLClausification(configuration);
        Set<DescriptionGraph> descriptionGraphs = Collections.emptySet();
        return clausifier.clausify(dataFactory, "uri:urn:internal-kb", axioms, axiomsExpressivity, descriptionGraphs);
    }

    protected static boolean isUnsupportedExtensionAxiom(OWLAxiom axiom) {
        return axiom instanceof OWLSubObjectPropertyOfAxiom || axiom instanceof OWLTransitiveObjectPropertyAxiom || axiom instanceof OWLSubPropertyChainOfAxiom || axiom instanceof OWLFunctionalObjectPropertyAxiom || axiom instanceof OWLInverseFunctionalObjectPropertyAxiom || axiom instanceof SWRLRule;
    }

    public void dumpHierarchies(PrintWriter out, boolean classes, boolean objectProperties, boolean dataProperties) {
        HierarchyDumperFSS printer = new HierarchyDumperFSS(out);
        if (classes) {
            this.classifyClasses();
            printer.printAtomicConceptHierarchy(this.m_atomicConceptHierarchy);
        }
        if (objectProperties) {
            this.classifyObjectProperties();
            printer.printObjectPropertyHierarchy(this.m_objectRoleHierarchy);
        }
        if (dataProperties) {
            this.classifyDataProperties();
            printer.printDataPropertyHierarchy(this.m_dataRoleHierarchy);
        }
    }

    public void printHierarchies(PrintWriter out, boolean classes, boolean objectProperties, boolean dataProperties) {
        HierarchyPrinterFSS printer = new HierarchyPrinterFSS(out, this.m_dlOntology.getOntologyIRI() + "#");
        if (classes) {
            this.classifyClasses();
            printer.loadAtomicConceptPrefixIRIs(this.m_atomicConceptHierarchy.getAllElements());
        }
        if (objectProperties) {
            this.classifyObjectProperties();
            printer.loadAtomicRolePrefixIRIs(this.m_dlOntology.getAllAtomicObjectRoles());
        }
        if (dataProperties) {
            this.classifyDataProperties();
            printer.loadAtomicRolePrefixIRIs(this.m_dlOntology.getAllAtomicDataRoles());
        }
        printer.startPrinting();
        boolean atLF = true;
        if (classes && !this.m_atomicConceptHierarchy.isEmpty()) {
            printer.printAtomicConceptHierarchy(this.m_atomicConceptHierarchy);
            atLF = false;
        }
        if (objectProperties && !this.m_objectRoleHierarchy.isEmpty()) {
            if (!atLF) {
                out.println();
            }
            printer.printRoleHierarchy(this.m_objectRoleHierarchy, true);
            atLF = false;
        }
        if (dataProperties && !this.m_dataRoleHierarchy.isEmpty()) {
            if (!atLF) {
                out.println();
            }
            printer.printRoleHierarchy(this.m_dataRoleHierarchy, false);
            atLF = false;
        }
        printer.endPrinting();
    }

    protected /* varargs */ void checkPreConditions(OWLObject ... objects) {
        this.flushChangesIfRequired();
        if (objects != null && objects.length > 0) {
            this.throwFreshEntityExceptionIfNecessary(objects);
        }
        this.throwInconsistentOntologyExceptionIfNecessary();
    }

    protected void flushChangesIfRequired() {
        if (!this.m_configuration.bufferChanges && !this.m_pendingChanges.isEmpty()) {
            this.flush();
        }
    }

    protected void throwInconsistentOntologyExceptionIfNecessary() {
        if (!this.isConsistent() && this.m_configuration.throwInconsistentOntologyException) {
            throw new InconsistentOntologyException();
        }
    }

    protected /* varargs */ void throwFreshEntityExceptionIfNecessary(OWLObject ... objects) {
        if (this.m_configuration.freshEntityPolicy == FreshEntityPolicy.DISALLOW) {
            HashSet<OWLEntity> undeclaredEntities = new HashSet<OWLEntity>();
            for (OWLObject object : objects) {
                if (object instanceof OWLEntity && ((OWLEntity)object).isBuiltIn()) continue;
                for (OWLDataProperty dp : object.getDataPropertiesInSignature()) {
                    if (this.isDefined(dp) || Prefixes.isInternalIRI(dp.getIRI().toString())) continue;
                    undeclaredEntities.add(dp);
                }
                for (OWLObjectProperty op : object.getObjectPropertiesInSignature()) {
                    if (this.isDefined(op) || Prefixes.isInternalIRI(op.getIRI().toString())) continue;
                    undeclaredEntities.add(op);
                }
                for (OWLNamedIndividual individual : object.getIndividualsInSignature()) {
                    if (this.isDefined(individual) || Prefixes.isInternalIRI(individual.getIRI().toString())) continue;
                    undeclaredEntities.add(individual);
                }
                for (OWLClass owlClass : object.getClassesInSignature()) {
                    if (this.isDefined(owlClass) || Prefixes.isInternalIRI(owlClass.getIRI().toString())) continue;
                    undeclaredEntities.add(owlClass);
                }
            }
            if (!undeclaredEntities.isEmpty()) {
                throw new FreshEntitiesException(undeclaredEntities);
            }
        }
    }

    protected /* varargs */ boolean containsFreshEntities(OWLObject ... objects) {
        for (OWLObject object : objects) {
            if (object instanceof OWLEntity && ((OWLEntity)object).isBuiltIn()) continue;
            for (OWLDataProperty dp : object.getDataPropertiesInSignature()) {
                if (this.isDefined(dp) || Prefixes.isInternalIRI(dp.getIRI().toString())) continue;
                return true;
            }
            for (OWLObjectProperty op : object.getObjectPropertiesInSignature()) {
                if (this.isDefined(op) || Prefixes.isInternalIRI(op.getIRI().toString())) continue;
                return true;
            }
            for (OWLNamedIndividual individual : object.getIndividualsInSignature()) {
                if (this.isDefined(individual) || Prefixes.isInternalIRI(individual.getIRI().toString())) continue;
                return true;
            }
            for (OWLClass owlClass : object.getClassesInSignature()) {
                if (this.isDefined(owlClass) || Prefixes.isInternalIRI(owlClass.getIRI().toString())) continue;
                return true;
            }
        }
        return false;
    }

    protected static AtomicConcept H(OWLClass owlClass) {
        return AtomicConcept.create(owlClass.getIRI().toString());
    }

    protected static AtomicRole H(OWLObjectProperty objectProperty) {
        return AtomicRole.create(objectProperty.getIRI().toString());
    }

    protected static Role H(OWLObjectPropertyExpression objectPropertyExpression) {
        if (objectPropertyExpression instanceof OWLObjectProperty) {
            return Reasoner.H((OWLObjectProperty)objectPropertyExpression);
        }
        return Reasoner.H(objectPropertyExpression.getNamedProperty()).getInverse();
    }

    protected static AtomicRole H(OWLDataProperty dataProperty) {
        return AtomicRole.create(dataProperty.getIRI().toString());
    }

    protected static Role H(OWLDataPropertyExpression dataPropertyExpression) {
        return Reasoner.H((OWLDataProperty)dataPropertyExpression);
    }

    protected static Individual H(OWLNamedIndividual namedIndividual) {
        return Individual.create(namedIndividual.getIRI().toString());
    }

    protected static Individual H(OWLAnonymousIndividual anonymousIndividual) {
        return Individual.createAnonymous(anonymousIndividual.getID().toString());
    }

    protected static Individual H(OWLIndividual individual) {
        if (individual.isAnonymous()) {
            return Reasoner.H((OWLAnonymousIndividual)individual);
        }
        return Reasoner.H((OWLNamedIndividual)individual);
    }

    protected org.semanticweb.owlapi.reasoner.Node<OWLClass> atomicConceptHierarchyNodeToNode(HierarchyNode<AtomicConcept> hierarchyNode) {
        HashSet<OWLClass> result = new HashSet<OWLClass>();
        OWLDataFactory factory = this.getDataFactory();
        for (AtomicConcept concept : hierarchyNode.getEquivalentElements()) {
            if (Prefixes.isInternalIRI(concept.getIRI())) continue;
            result.add(factory.getOWLClass(IRI.create(concept.getIRI())));
        }
        return new OWLClassNode(result);
    }

    protected NodeSet<OWLClass> atomicConceptHierarchyNodesToNodeSet(Collection<HierarchyNode<AtomicConcept>> hierarchyNodes) {
        HashSet<org.semanticweb.owlapi.reasoner.Node<OWLClass>> result = new HashSet<org.semanticweb.owlapi.reasoner.Node<OWLClass>>();
        for (HierarchyNode<AtomicConcept> hierarchyNode : hierarchyNodes) {
            org.semanticweb.owlapi.reasoner.Node<OWLClass> node = this.atomicConceptHierarchyNodeToNode(hierarchyNode);
            if (node.getSize() == 0) continue;
            result.add(node);
        }
        return new OWLClassNodeSet(result);
    }

    protected org.semanticweb.owlapi.reasoner.Node<OWLObjectPropertyExpression> objectPropertyHierarchyNodeToNode(HierarchyNode<Role> hierarchyNode) {
        HashSet<OWLObjectPropertyExpression> result = new HashSet<OWLObjectPropertyExpression>();
        OWLDataFactory factory = this.getDataFactory();
        for (Role role : hierarchyNode.getEquivalentElements()) {
            if (role instanceof AtomicRole) {
                result.add(factory.getOWLObjectProperty(IRI.create(((AtomicRole)role).getIRI())));
                continue;
            }
            OWLObjectProperty ope = factory.getOWLObjectProperty(IRI.create(((InverseRole)role).getInverseOf().getIRI()));
            result.add(factory.getOWLObjectInverseOf(ope));
        }
        return new OWLObjectPropertyNode(result);
    }

    protected NodeSet<OWLObjectPropertyExpression> objectPropertyHierarchyNodesToNodeSet(Collection<HierarchyNode<Role>> hierarchyNodes) {
        HashSet<org.semanticweb.owlapi.reasoner.Node<OWLObjectPropertyExpression>> result = new HashSet<org.semanticweb.owlapi.reasoner.Node<OWLObjectPropertyExpression>>();
        for (HierarchyNode<Role> hierarchyNode : hierarchyNodes) {
            result.add(this.objectPropertyHierarchyNodeToNode(hierarchyNode));
        }
        return new OWLObjectPropertyNodeSet(result);
    }

    protected org.semanticweb.owlapi.reasoner.Node<OWLDataProperty> dataPropertyHierarchyNodeToNode(HierarchyNode<AtomicRole> hierarchyNode) {
        HashSet<OWLDataProperty> result = new HashSet<OWLDataProperty>();
        OWLDataFactory factory = this.getDataFactory();
        for (AtomicRole atomicRole : hierarchyNode.getEquivalentElements()) {
            result.add(factory.getOWLDataProperty(IRI.create(atomicRole.getIRI())));
        }
        return new OWLDataPropertyNode(result);
    }

    protected NodeSet<OWLDataProperty> dataPropertyHierarchyNodesToNodeSet(Collection<HierarchyNode<AtomicRole>> hierarchyNodes) {
        HashSet<org.semanticweb.owlapi.reasoner.Node<OWLDataProperty>> result = new HashSet<org.semanticweb.owlapi.reasoner.Node<OWLDataProperty>>();
        for (HierarchyNode<AtomicRole> hierarchyNode : hierarchyNodes) {
            result.add(this.dataPropertyHierarchyNodeToNode(hierarchyNode));
        }
        return new OWLDataPropertyNodeSet(result);
    }

    @Deprecated
    public static class ReasonerFactory
    extends org.semanticweb.HermiT.ReasonerFactory {
    }

    protected class OntologyChangeListener
    implements OWLOntologyChangeListener {
        protected OntologyChangeListener() {
        }

        public void ontologiesChanged(List<? extends OWLOntologyChange> changes) {
            for (OWLOntologyChange change : changes) {
                if (change instanceof RemoveOntologyAnnotation || change instanceof AddOntologyAnnotation) continue;
                Reasoner.this.m_pendingChanges.add(change);
            }
        }
    }

}

