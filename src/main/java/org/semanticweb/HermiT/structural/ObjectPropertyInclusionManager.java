package org.semanticweb.HermiT.structural;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.graph.Graph;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

public class ObjectPropertyInclusionManager {
    protected final Map<OWLObjectPropertyExpression, Automaton> m_automataByProperty = new HashMap<OWLObjectPropertyExpression, Automaton>();

    public ObjectPropertyInclusionManager(OWLAxioms axioms) {
        this.createAutomata(this.m_automataByProperty, axioms.m_complexObjectPropertyExpressions, axioms.m_simpleObjectPropertyInclusions, axioms.m_complexObjectPropertyInclusions, axioms.m_explicitInverses);
    }

    public int rewriteNegativeObjectPropertyAssertions(OWLDataFactory factory, OWLAxioms axioms, int replacementIndex) {
        HashSet<OWLNegativeObjectPropertyAssertionAxiom> redundantFacts = new HashSet<OWLNegativeObjectPropertyAssertionAxiom>();
        HashSet<OWLClassAssertionAxiom> additionalFacts = new HashSet<OWLClassAssertionAxiom>();
        for (OWLIndividualAxiom axiom : axioms.m_facts) {
            OWLObjectPropertyExpression prop;
            OWLNegativeObjectPropertyAssertionAxiom negAssertion;
            if (!(axiom instanceof OWLNegativeObjectPropertyAssertionAxiom) || !axioms.m_complexObjectPropertyExpressions.contains(prop = (negAssertion = (OWLNegativeObjectPropertyAssertionAxiom)axiom).getProperty())) continue;
            OWLIndividual individual = negAssertion.getObject();
            OWLClass individualConcept = factory.getOWLClass(IRI.create("internal:nom#" + individual.asOWLNamedIndividual().getIRI()));
            OWLObjectComplementOf notIndividualConcept = factory.getOWLObjectComplementOf(individualConcept);
            OWLObjectAllValuesFrom allNotIndividualConcept = factory.getOWLObjectAllValuesFrom(prop, notIndividualConcept);
            OWLClass definition = factory.getOWLClass(IRI.create("internal:def#" + replacementIndex++));
            axioms.m_conceptInclusions.add(new OWLClassExpression[]{factory.getOWLObjectComplementOf(definition), allNotIndividualConcept});
            additionalFacts.add(factory.getOWLClassAssertionAxiom(definition, negAssertion.getSubject()));
            additionalFacts.add(factory.getOWLClassAssertionAxiom(individualConcept, individual));
            redundantFacts.add(negAssertion);
        }
        axioms.m_facts.addAll(additionalFacts);
        axioms.m_facts.removeAll(redundantFacts);
        return replacementIndex;
    }

    public void rewriteAxioms(OWLDataFactory dataFactory, OWLAxioms axioms, int firstReplacementIndex) {
        for (OWLObjectPropertyExpression objectPropertyExpression : axioms.m_asymmetricObjectProperties) {
            if (!axioms.m_complexObjectPropertyExpressions.contains(objectPropertyExpression)) continue;
            throw new IllegalArgumentException("Non-simple property '" + objectPropertyExpression + "' or its inverse appears in asymmetric object property axiom.");
        }
        for (OWLObjectPropertyExpression objectPropertyExpression : axioms.m_irreflexiveObjectProperties) {
            if (!axioms.m_complexObjectPropertyExpressions.contains(objectPropertyExpression)) continue;
            throw new IllegalArgumentException("Non-simple property '" + objectPropertyExpression + "' or its inverse appears in irreflexive object property axiom.");
        }
        for (OWLObjectPropertyExpression[] properties : axioms.m_disjointObjectProperties) {
            for (int i = 0; i < properties.length; ++i) {
                if (!axioms.m_complexObjectPropertyExpressions.contains(properties[i])) continue;
                throw new IllegalArgumentException("Non-simple property '" + properties[i] + "' or its inverse appears in disjoint properties axiom.");
            }
        }
        HashMap<OWLObjectAllValuesFrom, OWLClassExpression> replacedDescriptions = new HashMap<OWLObjectAllValuesFrom, OWLClassExpression>();
        for (OWLClassExpression[] inclusion : axioms.m_conceptInclusions) {
            for (int index = 0; index < inclusion.length; ++index) {
                OWLObjectAllValuesFrom objectAll;
                Object objectProperty;
                OWLObjectHasSelf objectSelfRestriction;
                OWLClassExpression replacement2222;
                OWLClassExpression classExpression = inclusion[index];
                if (classExpression instanceof OWLObjectCardinalityRestriction) {
                    OWLObjectCardinalityRestriction objectCardinalityRestriction = (OWLObjectCardinalityRestriction)classExpression;
                    OWLObjectPropertyExpression objectPropertyExpression = objectCardinalityRestriction.getProperty();
                    if (axioms.m_complexObjectPropertyExpressions.contains(objectPropertyExpression)) {
                        throw new IllegalArgumentException("Non-simple property '" + objectPropertyExpression + "' or its inverse appears in the cardinality restriction '" + objectCardinalityRestriction + "'.");
                    }
                } else if (classExpression instanceof OWLObjectHasSelf && axioms.m_complexObjectPropertyExpressions.contains((objectSelfRestriction = (OWLObjectHasSelf)classExpression).getProperty())) {
                    throw new IllegalArgumentException("Non-simple property '" + objectSelfRestriction.getProperty() + "' or its inverse appears in the Self restriction '" + objectSelfRestriction + "'.");
                }
                if (!(classExpression instanceof OWLObjectAllValuesFrom) || (objectAll = (OWLObjectAllValuesFrom)classExpression).getFiller().equals(dataFactory.getOWLThing()) || !this.m_automataByProperty.containsKey(objectProperty = objectAll.getProperty())) continue;
                replacement2222 = replacedDescriptions.get(objectAll);
                if (replacement2222 == null) {
                    replacement2222 = dataFactory.getOWLClass(IRI.create("internal:all#" + firstReplacementIndex++));
                    if (objectAll.getFiller() instanceof OWLObjectComplementOf || objectAll.getFiller().equals(dataFactory.getOWLNothing())) {
                        replacement2222 = replacement2222.getComplementNNF();
                    }
                    replacedDescriptions.put(objectAll, replacement2222);
                }
                inclusion[index] = replacement2222;
            }
        }
        for (Map.Entry replacement : replacedDescriptions.entrySet()) {
            Automaton automaton = this.m_automataByProperty.get(((OWLObjectAllValuesFrom)replacement.getKey()).getProperty());
            boolean isOfNegativePolarity = replacement.getValue() instanceof OWLObjectComplementOf;
            HashMap statesToConcepts = new HashMap();
            for (Object stateObject : automaton.states()) {
                State state = (State)stateObject;
                if (state.isInitial()) {
                    statesToConcepts.put(state, replacement.getValue());
                    continue;
                }
                OWLClassExpression stateConcept = dataFactory.getOWLClass(IRI.create("internal:all#" + firstReplacementIndex++));
                if (isOfNegativePolarity) {
                    stateConcept = stateConcept.getComplementNNF();
                }
                statesToConcepts.put(state, stateConcept);
            }
            for (Object transitionObject2 : automaton.delta()) {
                Transition transition = (Transition)transitionObject2;
                OWLClassExpression fromStateConcept = ((OWLClassExpression)statesToConcepts.get(transition.start())).getComplementNNF();
                OWLClassExpression toStateConcept = (OWLClassExpression)statesToConcepts.get(transition.end());
                if (transition.label() == null) {
                    axioms.m_conceptInclusions.add(new OWLClassExpression[]{fromStateConcept, toStateConcept});
                    continue;
                }
                OWLObjectAllValuesFrom consequentAll = dataFactory.getOWLObjectAllValuesFrom((OWLObjectPropertyExpression)transition.label(), toStateConcept);
                axioms.m_conceptInclusions.add(new OWLClassExpression[]{fromStateConcept, consequentAll});
            }
            OWLClassExpression filler = ((OWLObjectAllValuesFrom)replacement.getKey()).getFiller();
            for (State finalStateObject : automaton.terminals()) {
                OWLClassExpression finalStateConceptComplement = ((OWLClassExpression)statesToConcepts.get(finalStateObject)).getComplementNNF();
                if (filler.isOWLNothing()) {
                    axioms.m_conceptInclusions.add(new OWLClassExpression[]{finalStateConceptComplement});
                    continue;
                }
                axioms.m_conceptInclusions.add(new OWLClassExpression[]{finalStateConceptComplement, filler});
            }
        }
    }

    protected void createAutomata(Map<OWLObjectPropertyExpression, Automaton> automataByProperty, Set<OWLObjectPropertyExpression> complexObjectPropertyExpressions, Collection<OWLObjectPropertyExpression[]> simpleObjectPropertyInclusions, Collection<OWLAxioms.ComplexObjectPropertyInclusion> complexObjectPropertyInclusions, Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> explicitInverses) {
        Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> equivalentPropertiesMap = this.findEquivalentProperties(simpleObjectPropertyInclusions);
        Set<OWLObjectPropertyExpression> symmetricObjectProperties = ObjectPropertyInclusionManager.findSymmetricProperties(simpleObjectPropertyInclusions);
        Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> inversePropertiesMap = this.buildInversePropertiesMap(simpleObjectPropertyInclusions, explicitInverses);
        Graph<OWLObjectPropertyExpression> propertyDependencyGraph = this.buildPropertyOrdering(simpleObjectPropertyInclusions, complexObjectPropertyInclusions, equivalentPropertiesMap);
        this.checkForRegularity(propertyDependencyGraph, equivalentPropertiesMap);
        Graph<OWLObjectPropertyExpression> complexPropertiesDependencyGraph = propertyDependencyGraph.clone();
        HashSet<OWLObjectPropertyExpression> transitiveProperties = new HashSet<OWLObjectPropertyExpression>();
        Map<OWLObjectPropertyExpression, Automaton> individualAutomata = this.buildIndividualAutomata(complexPropertiesDependencyGraph, complexObjectPropertyInclusions, equivalentPropertiesMap, transitiveProperties);
        Set<OWLObjectPropertyExpression> simpleProperties = this.findSimpleProperties(complexPropertiesDependencyGraph, individualAutomata);
        propertyDependencyGraph.removeElements(simpleProperties);
        complexPropertiesDependencyGraph.removeElements(simpleProperties);
        complexObjectPropertyExpressions.addAll(complexPropertiesDependencyGraph.getElements());
        for (OWLObjectPropertyExpression[] inclusion : simpleObjectPropertyInclusions) {
            if (!complexObjectPropertyExpressions.contains(inclusion[0]) || !individualAutomata.containsKey(inclusion[1])) continue;
            Automaton auto = individualAutomata.get(inclusion[1]);
            Transition transition = new Transition(auto.initials().iterator().next(), inclusion[0], auto.terminals().iterator().next());
            auto.addTransition(transition, "Could not create automaton for property at the bottom of hierarchy (simple property).");
        }
        HashSet<OWLObjectPropertyExpression> inverseOfComplexProperties = new HashSet<OWLObjectPropertyExpression>();
        for (OWLObjectPropertyExpression complexProp : complexObjectPropertyExpressions) {
            inverseOfComplexProperties.add(complexProp.getInverseProperty());
        }
        complexObjectPropertyExpressions.addAll(inverseOfComplexProperties);
        this.connectAllAutomata(automataByProperty, propertyDependencyGraph, inversePropertiesMap, individualAutomata, symmetricObjectProperties, transitiveProperties);
        HashMap<OWLObjectPropertyExpression, Automaton> individualAutomataForEquivRoles = new HashMap<OWLObjectPropertyExpression, Automaton>();
        for (OWLObjectPropertyExpression propExprWithAutomaton : automataByProperty.keySet()) {
            if (equivalentPropertiesMap.get(propExprWithAutomaton) == null) continue;
            Automaton autoOfPropExpr = automataByProperty.get(propExprWithAutomaton);
            for (OWLObjectPropertyExpression equivProp : equivalentPropertiesMap.get(propExprWithAutomaton)) {
                OWLObjectPropertyExpression inverseEquivProp;
                if (!equivProp.equals(propExprWithAutomaton) && !automataByProperty.containsKey(equivProp)) {
                    Automaton automatonOfEquivalent = (Automaton)autoOfPropExpr.clone();
                    individualAutomataForEquivRoles.put(equivProp, automatonOfEquivalent);
                    simpleProperties.remove(equivProp);
                    complexObjectPropertyExpressions.add(equivProp);
                }
                if ((inverseEquivProp = equivProp.getInverseProperty()).equals(propExprWithAutomaton) || automataByProperty.containsKey(inverseEquivProp)) continue;
                Automaton automatonOfEquivalent = (Automaton)autoOfPropExpr.clone();
                individualAutomataForEquivRoles.put(inverseEquivProp, this.getMirroredCopy(automatonOfEquivalent));
                simpleProperties.remove(inverseEquivProp);
                complexObjectPropertyExpressions.add(inverseEquivProp);
            }
        }
        automataByProperty.putAll(individualAutomataForEquivRoles);
    }

    private static Set<OWLObjectPropertyExpression> findSymmetricProperties(Collection<OWLObjectPropertyExpression[]> simpleObjectPropertyInclusions) {
        HashSet<OWLObjectPropertyExpression> symmetricProperties = new HashSet<OWLObjectPropertyExpression>();
        for (OWLObjectPropertyExpression[] inclusion : simpleObjectPropertyInclusions) {
            if (!inclusion[1].getInverseProperty().equals(inclusion[0]) && !inclusion[1].equals(inclusion[0].getInverseProperty())) continue;
            symmetricProperties.add(inclusion[0]);
            symmetricProperties.add(inclusion[0].getInverseProperty());
        }
        return symmetricProperties;
    }

    protected Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> buildInversePropertiesMap(Collection<OWLObjectPropertyExpression[]> simpleObjectPropertyInclusions, Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> explicitInverses) {
        HashMap<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> inversePropertiesMap = new HashMap<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>>(explicitInverses);
        ArrayList<OWLObjectPropertyExpression[]> inclusionCandidates = new ArrayList<OWLObjectPropertyExpression[]>();
        for (OWLObjectPropertyExpression[] inclusion : simpleObjectPropertyInclusions) {
            if (ObjectPropertyInclusionManager.isInverseOf(inclusion[0]) == ObjectPropertyInclusionManager.isInverseOf(inclusion[1])) continue;
            inclusionCandidates.add(inclusion);
        }
        for (OWLObjectPropertyExpression[] inclusion : inclusionCandidates) {
            OWLObjectPropertyExpression inverse0 = inclusion[0].getInverseProperty();
            OWLObjectPropertyExpression inverse1 = inclusion[1].getInverseProperty();
            if (inclusion[1] instanceof OWLObjectInverseOf) {
                if (!ObjectPropertyInclusionManager.contains(inclusionCandidates, inverse1, inverse0)) continue;
                inversePropertiesMap.computeIfAbsent(inclusion[0], x -> new HashSet()).add(inverse1);
                inversePropertiesMap.computeIfAbsent(inverse1, x -> new HashSet()).add(inclusion[0]);
                continue;
            }
            if (!(inclusion[0] instanceof OWLObjectInverseOf) || !ObjectPropertyInclusionManager.contains(inclusionCandidates, inverse0, inverse1)) continue;
            inversePropertiesMap.computeIfAbsent(inclusion[1], x -> new HashSet()).add(inverse0);
            inversePropertiesMap.computeIfAbsent(inverse0, x -> new HashSet()).add(inclusion[1]);
        }
        return inversePropertiesMap;
    }

    private static boolean isInverseOf(OWLObjectPropertyExpression e) {
        return e instanceof OWLObjectInverseOf;
    }

    private static boolean contains(List<OWLObjectPropertyExpression[]> list, OWLObjectPropertyExpression o1, OWLObjectPropertyExpression o2) {
        for (OWLObjectPropertyExpression[] l : list) {
            if (!l[0].equals(o1) || !l[1].equals(o2)) continue;
            return true;
        }
        return false;
    }

    protected Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> findEquivalentProperties(Collection<OWLObjectPropertyExpression[]> simpleObjectPropertyInclusions) {
        Graph<OWLObjectPropertyExpression> propertyDependencyGraph = new Graph<OWLObjectPropertyExpression>();
        HashMap<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> equivalentObjectPropertiesMapping = new HashMap<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>>();
        for (OWLObjectPropertyExpression[] inclusion : simpleObjectPropertyInclusions) {
            if (inclusion[0].equals(inclusion[1]) || inclusion[0].equals(inclusion[1].getInverseProperty())) continue;
            propertyDependencyGraph.addEdge(inclusion[0], inclusion[1]);
        }
        propertyDependencyGraph.transitivelyClose();
        for (OWLObjectPropertyExpression objExpr : propertyDependencyGraph.getElements()) {
            if (!propertyDependencyGraph.getSuccessors(objExpr).contains(objExpr) && !propertyDependencyGraph.getSuccessors(objExpr).contains(objExpr.getInverseProperty())) continue;
            HashSet<OWLObjectPropertyExpression> equivPropertiesSet = new HashSet<OWLObjectPropertyExpression>();
            for (OWLObjectPropertyExpression succ : propertyDependencyGraph.getSuccessors(objExpr)) {
                if (succ.equals(objExpr) || !propertyDependencyGraph.getSuccessors(succ).contains(objExpr) && !propertyDependencyGraph.getSuccessors(succ).contains(objExpr.getInverseProperty())) continue;
                equivPropertiesSet.add(succ);
            }
            equivalentObjectPropertiesMapping.put(objExpr, equivPropertiesSet);
        }
        return equivalentObjectPropertiesMapping;
    }

    protected Set<OWLObjectPropertyExpression> findSimpleProperties(Graph<OWLObjectPropertyExpression> complexPropertiesDependencyGraph, Map<OWLObjectPropertyExpression, Automaton> individualAutomata) {
        HashSet<OWLObjectPropertyExpression> simpleProperties = new HashSet<OWLObjectPropertyExpression>();
        Graph<OWLObjectPropertyExpression> complexPropertiesDependencyGraphWithInverses = complexPropertiesDependencyGraph.clone();
        for (OWLObjectPropertyExpression complexProperty1 : complexPropertiesDependencyGraph.getElements()) {
            for (OWLObjectPropertyExpression complexProperty2 : complexPropertiesDependencyGraph.getSuccessors(complexProperty1)) {
                complexPropertiesDependencyGraphWithInverses.addEdge(complexProperty1.getInverseProperty(), complexProperty2.getInverseProperty());
            }
        }
        Graph<OWLObjectPropertyExpression> invertedGraph = complexPropertiesDependencyGraphWithInverses.getInverse();
        invertedGraph.transitivelyClose();
        for (OWLObjectPropertyExpression properties : invertedGraph.getElements()) {
            boolean hasComplexSubproperty = false;
            for (OWLObjectPropertyExpression subDependingProperties : invertedGraph.getSuccessors(properties)) {
                if (!individualAutomata.containsKey(subDependingProperties) && !individualAutomata.containsKey(subDependingProperties.getInverseProperty())) continue;
                hasComplexSubproperty = true;
                break;
            }
            if (hasComplexSubproperty || individualAutomata.containsKey(properties) || individualAutomata.containsKey(properties.getInverseProperty())) continue;
            simpleProperties.add(properties);
        }
        return simpleProperties;
    }

    protected void connectAllAutomata(Map<OWLObjectPropertyExpression, Automaton> completeAutomata, Graph<OWLObjectPropertyExpression> propertyDependencyGraph, Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> inversePropertiesMap, Map<OWLObjectPropertyExpression, Automaton> individualAutomata, Set<OWLObjectPropertyExpression> symmetricObjectProperties, Set<OWLObjectPropertyExpression> transitiveProperties) {
    	Graph<OWLObjectPropertyExpression> transClosedGraph = propertyDependencyGraph.clone();
        transClosedGraph.transitivelyClose();
        HashSet<OWLObjectPropertyExpression> propertiesToStartRecursion = new HashSet<OWLObjectPropertyExpression>();
        for (OWLObjectPropertyExpression owlProp : transClosedGraph.getElements()) {
            if (!transClosedGraph.getSuccessors(owlProp).isEmpty()) continue;
            propertiesToStartRecursion.add(owlProp);
        }
        Graph<OWLObjectPropertyExpression> inversePropertyDependencyGraph = propertyDependencyGraph.getInverse();
        for (OWLObjectPropertyExpression superproperty : propertiesToStartRecursion) {
            this.buildCompleteAutomataForProperties(superproperty, inversePropertiesMap, individualAutomata, completeAutomata, inversePropertyDependencyGraph, symmetricObjectProperties, transitiveProperties);
        }
        for (OWLObjectPropertyExpression property : individualAutomata.keySet()) {
            if (completeAutomata.containsKey(property)) continue;
            Automaton propertyAutomaton = individualAutomata.get(property);
            if (completeAutomata.containsKey(property.getInverseProperty()) && inversePropertyDependencyGraph.getElements().contains(property.getInverseProperty()) || individualAutomata.containsKey(property.getInverseProperty())) {
                Automaton inversePropertyAutomaton = completeAutomata.get(property.getInverseProperty());
                if (inversePropertyAutomaton == null) {
                    inversePropertyAutomaton = individualAutomata.get(property.getInverseProperty());
                }
                this.increaseAutomatonWithInversePropertyAutomaton(propertyAutomaton, inversePropertyAutomaton);
            }
            completeAutomata.put(property, propertyAutomaton);
        }
        HashMap<OWLObjectPropertyExpression, Automaton> extraCompleteAutomataForInverseProperties = new HashMap<OWLObjectPropertyExpression, Automaton>();
        for (OWLObjectPropertyExpression property : completeAutomata.keySet()) {
            if (completeAutomata.containsKey(property.getInverseProperty())) continue;
            extraCompleteAutomataForInverseProperties.put(property.getInverseProperty(), this.getMirroredCopy(completeAutomata.get(property)));
        }
        completeAutomata.putAll(extraCompleteAutomataForInverseProperties);
        extraCompleteAutomataForInverseProperties.clear();
        for (OWLObjectPropertyExpression property : completeAutomata.keySet()) {
            if (!completeAutomata.containsKey(property) || completeAutomata.containsKey(property.getInverseProperty())) continue;
            extraCompleteAutomataForInverseProperties.put(property.getInverseProperty(), this.getMirroredCopy(completeAutomata.get(property)));
        }
        completeAutomata.putAll(extraCompleteAutomataForInverseProperties);
        extraCompleteAutomataForInverseProperties.clear();
        for (OWLObjectPropertyExpression propExprWithAutomaton : completeAutomata.keySet()) {
            if (inversePropertiesMap.get(propExprWithAutomaton) == null) continue;
            Automaton autoOfPropExpr = completeAutomata.get(propExprWithAutomaton);
            for (OWLObjectPropertyExpression inverseProp : inversePropertiesMap.get(propExprWithAutomaton)) {
                Automaton automatonOfInverse = completeAutomata.get(inverseProp);
                if (automatonOfInverse != null) {
                    this.increaseAutomatonWithInversePropertyAutomaton(autoOfPropExpr, automatonOfInverse);
                    extraCompleteAutomataForInverseProperties.put(propExprWithAutomaton, autoOfPropExpr);
                    continue;
                }
                automatonOfInverse = this.getMirroredCopy(autoOfPropExpr);
                extraCompleteAutomataForInverseProperties.put(inverseProp, automatonOfInverse);
            }
        }
        completeAutomata.putAll(extraCompleteAutomataForInverseProperties);
    }

    protected void increaseAutomatonWithInversePropertyAutomaton(Automaton propertyAutomaton, Automaton inversePropertyAutomaton) {
        State initialState = propertyAutomaton.initials().iterator().next();
        State finalState = propertyAutomaton.terminals().iterator().next();
        Transition transition = propertyAutomaton.deltaFrom(initialState, finalState).iterator().next();
        this.automataConnector(propertyAutomaton, this.getMirroredCopy(inversePropertyAutomaton), transition);
    }

    protected Automaton buildCompleteAutomataForProperties(OWLObjectPropertyExpression propertyToBuildAutomatonFor, Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> inversePropertiesMap, Map<OWLObjectPropertyExpression, Automaton> individualAutomata, Map<OWLObjectPropertyExpression, Automaton> completeAutomata, Graph<OWLObjectPropertyExpression> inversedPropertyDependencyGraph, Set<OWLObjectPropertyExpression> symmetricObjectProperties, Set<OWLObjectPropertyExpression> transitiveProperties) {
        if (completeAutomata.containsKey(propertyToBuildAutomatonFor)) {
            return completeAutomata.get(propertyToBuildAutomatonFor);
        }
        if (completeAutomata.containsKey(propertyToBuildAutomatonFor.getInverseProperty()) && !individualAutomata.containsKey(propertyToBuildAutomatonFor)) {
            Automaton mirroredCopy = this.getMirroredCopy(completeAutomata.get(propertyToBuildAutomatonFor.getInverseProperty()));
            completeAutomata.put(propertyToBuildAutomatonFor, mirroredCopy);
            return mirroredCopy;
        }
        if (inversedPropertyDependencyGraph.getSuccessors(propertyToBuildAutomatonFor).isEmpty() && inversedPropertyDependencyGraph.getSuccessors(propertyToBuildAutomatonFor.getInverseProperty()).isEmpty()) {
            Automaton automatonForLeafProperty = individualAutomata.get(propertyToBuildAutomatonFor);
            if (automatonForLeafProperty == null) {
                boolean noInversePropertyWithAutomaton;
                Set<OWLObjectPropertyExpression> inverses = inversePropertiesMap.get(propertyToBuildAutomatonFor);
                noInversePropertyWithAutomaton = true;
                if (inverses != null) {
                    for (OWLObjectPropertyExpression inverse : inverses) {
                        if (!individualAutomata.containsKey(inverse) || inverse.equals(propertyToBuildAutomatonFor)) continue;
                        automatonForLeafProperty = this.getMirroredCopy(this.buildCompleteAutomataForProperties(inverse, inversePropertiesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph, symmetricObjectProperties, transitiveProperties));
                        automatonForLeafProperty = this.minimizeAndNormalizeAutomaton(automatonForLeafProperty);
                        completeAutomata.put(propertyToBuildAutomatonFor, automatonForLeafProperty);
                        noInversePropertyWithAutomaton = false;
                        break;
                    }
                } else if (individualAutomata.containsKey(propertyToBuildAutomatonFor.getInverseProperty())) {
                    automatonForLeafProperty = this.getMirroredCopy(this.buildCompleteAutomataForProperties(propertyToBuildAutomatonFor.getInverseProperty(), inversePropertiesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph, symmetricObjectProperties, transitiveProperties));
                    if (!completeAutomata.containsKey(propertyToBuildAutomatonFor)) {
                        automatonForLeafProperty = this.minimizeAndNormalizeAutomaton(automatonForLeafProperty);
                        completeAutomata.put(propertyToBuildAutomatonFor, automatonForLeafProperty);
                    } else {
                        automatonForLeafProperty = completeAutomata.get(propertyToBuildAutomatonFor);
                    }
                    noInversePropertyWithAutomaton = false;
                }
                if (noInversePropertyWithAutomaton) {
                    automatonForLeafProperty = new Automaton();
                    State initial = automatonForLeafProperty.addState(true, false);
                    State accepting = automatonForLeafProperty.addState(false, true);
                    Transition transition = new Transition(initial, propertyToBuildAutomatonFor, accepting);
                    automatonForLeafProperty.addTransition(transition, "Could not create automaton for property at the bottom of hierarchy (simple property).");
                    this.finalizeConstruction(completeAutomata, propertyToBuildAutomatonFor, automatonForLeafProperty, symmetricObjectProperties, transitiveProperties);
                }
            } else if (propertyToBuildAutomatonFor.getInverseProperty().isAnonymous() && individualAutomata.containsKey(propertyToBuildAutomatonFor.getInverseProperty())) {
                Automaton inversePropertyAutomaton = this.buildCompleteAutomataForProperties(propertyToBuildAutomatonFor.getInverseProperty(), inversePropertiesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph, symmetricObjectProperties, transitiveProperties);
                this.increaseAutomatonWithInversePropertyAutomaton(automatonForLeafProperty, this.getMirroredCopy(inversePropertyAutomaton));
                if (!completeAutomata.containsKey(propertyToBuildAutomatonFor)) {
                    this.finalizeConstruction(completeAutomata, propertyToBuildAutomatonFor, automatonForLeafProperty, symmetricObjectProperties, transitiveProperties);
                } else {
                    automatonForLeafProperty = completeAutomata.get(propertyToBuildAutomatonFor);
                }
            } else {
                this.increaseWithDefinedInverseIfNecessary(propertyToBuildAutomatonFor, automatonForLeafProperty, inversePropertiesMap, individualAutomata);
                this.finalizeConstruction(completeAutomata, propertyToBuildAutomatonFor, automatonForLeafProperty, symmetricObjectProperties, transitiveProperties);
            }
            return automatonForLeafProperty;
        }
        Automaton biggerPropertyAutomaton = individualAutomata.get(propertyToBuildAutomatonFor);
        if (biggerPropertyAutomaton == null) {
            biggerPropertyAutomaton = new Automaton();
            State initialState = biggerPropertyAutomaton.addState(true, false);
            State finalState = biggerPropertyAutomaton.addState(false, true);
            Transition transition = new Transition(initialState, propertyToBuildAutomatonFor, finalState);
            biggerPropertyAutomaton.addTransition(transition, "Could not create automaton");
            for (OWLObjectPropertyExpression smallerProperty : inversedPropertyDependencyGraph.getSuccessors(propertyToBuildAutomatonFor)) {
                Automaton smallerPropertyAutomaton = this.buildCompleteAutomataForProperties(smallerProperty, inversePropertiesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph, symmetricObjectProperties, transitiveProperties);
                this.automataConnector(biggerPropertyAutomaton, smallerPropertyAutomaton, transition);
                Transition t = new Transition(initialState, smallerProperty, finalState);
                biggerPropertyAutomaton.addTransition(t, "Could not create automaton");
            }
            if (propertyToBuildAutomatonFor.getInverseProperty().isAnonymous() && individualAutomata.containsKey(propertyToBuildAutomatonFor.getInverseProperty())) {
                Automaton inversePropertyAutomaton = this.buildCompleteAutomataForProperties(propertyToBuildAutomatonFor.getInverseProperty(), inversePropertiesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph, symmetricObjectProperties, transitiveProperties);
                this.increaseAutomatonWithInversePropertyAutomaton(biggerPropertyAutomaton, this.getMirroredCopy(inversePropertyAutomaton));
                if (!completeAutomata.containsKey(propertyToBuildAutomatonFor)) {
                    this.finalizeConstruction(completeAutomata, propertyToBuildAutomatonFor, biggerPropertyAutomaton, symmetricObjectProperties, transitiveProperties);
                } else {
                    biggerPropertyAutomaton = completeAutomata.get(propertyToBuildAutomatonFor);
                }
            } else {
                this.increaseWithDefinedInverseIfNecessary(propertyToBuildAutomatonFor, biggerPropertyAutomaton, inversePropertiesMap, individualAutomata);
                if (!completeAutomata.containsKey(propertyToBuildAutomatonFor)) {
                    this.finalizeConstruction(completeAutomata, propertyToBuildAutomatonFor, biggerPropertyAutomaton, symmetricObjectProperties, transitiveProperties);
                } else {
                    biggerPropertyAutomaton = completeAutomata.get(propertyToBuildAutomatonFor);
                }
            }
        } else {
            for (OWLObjectPropertyExpression smallerProperty : inversedPropertyDependencyGraph.getSuccessors(propertyToBuildAutomatonFor)) {
                boolean someInternalTransitionMatched = false;
                for (Transition transitionObject : biggerPropertyAutomaton.delta()) {
                    Transition transition = transitionObject;
                    if (transition.label() == null || !transition.label().equals(smallerProperty)) continue;
                    Automaton smallerPropertyAutomaton = this.buildCompleteAutomataForProperties(smallerProperty, inversePropertiesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph, symmetricObjectProperties, transitiveProperties);
                    if (smallerPropertyAutomaton.delta().size() != 1) {
                        this.automataConnector(biggerPropertyAutomaton, smallerPropertyAutomaton, transition);
                    }
                    someInternalTransitionMatched = true;
                }
                if (someInternalTransitionMatched) continue;
                Automaton smallerPropertyAutomaton = this.buildCompleteAutomataForProperties(smallerProperty, inversePropertiesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph, symmetricObjectProperties, transitiveProperties);
                Transition initial2TerminalTransition = biggerPropertyAutomaton.deltaFrom(biggerPropertyAutomaton.initials().iterator().next(), biggerPropertyAutomaton.terminals().iterator().next()).iterator().next();
                this.automataConnector(biggerPropertyAutomaton, smallerPropertyAutomaton, initial2TerminalTransition);
            }
        }
        if (propertyToBuildAutomatonFor.getInverseProperty().isAnonymous() && individualAutomata.containsKey(propertyToBuildAutomatonFor.getInverseProperty())) {
            Automaton inversePropertyAutomaton = this.buildCompleteAutomataForProperties(propertyToBuildAutomatonFor.getInverseProperty(), inversePropertiesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph, symmetricObjectProperties, transitiveProperties);
            this.increaseAutomatonWithInversePropertyAutomaton(biggerPropertyAutomaton, this.getMirroredCopy(inversePropertyAutomaton));
            if (!completeAutomata.containsKey(propertyToBuildAutomatonFor)) {
                this.finalizeConstruction(completeAutomata, propertyToBuildAutomatonFor, biggerPropertyAutomaton, symmetricObjectProperties, transitiveProperties);
            } else {
                biggerPropertyAutomaton = completeAutomata.get(propertyToBuildAutomatonFor);
            }
        } else {
            this.increaseWithDefinedInverseIfNecessary(propertyToBuildAutomatonFor, biggerPropertyAutomaton, inversePropertiesMap, individualAutomata);
            if (!completeAutomata.containsKey(propertyToBuildAutomatonFor)) {
                this.finalizeConstruction(completeAutomata, propertyToBuildAutomatonFor, biggerPropertyAutomaton, symmetricObjectProperties, transitiveProperties);
            } else {
                biggerPropertyAutomaton = completeAutomata.get(propertyToBuildAutomatonFor);
            }
        }
        return biggerPropertyAutomaton;
    }

    private void finalizeConstruction(Map<OWLObjectPropertyExpression, Automaton> completeAutomata, OWLObjectPropertyExpression propertyToBuildAutomatonFor, Automaton biggerPropertyAutomaton, Set<OWLObjectPropertyExpression> symmetricObjectProperties, Set<OWLObjectPropertyExpression> transitiveProperties) {
        if (transitiveProperties.contains(propertyToBuildAutomatonFor.getInverseProperty())) {
            Transition transition = new Transition(biggerPropertyAutomaton.terminals().iterator().next(), null, biggerPropertyAutomaton.initials().iterator().next());
            biggerPropertyAutomaton.addTransition(transition, "Could not create automaton for symmetric property: " + propertyToBuildAutomatonFor);
        }
        if (symmetricObjectProperties.contains(propertyToBuildAutomatonFor)) {
            Transition basicTransition = new Transition(biggerPropertyAutomaton.initials().iterator().next(), propertyToBuildAutomatonFor.getInverseProperty(), biggerPropertyAutomaton.terminals().iterator().next());
            this.automataConnector(biggerPropertyAutomaton, this.getMirroredCopy(biggerPropertyAutomaton), basicTransition);
        }
        biggerPropertyAutomaton = this.minimizeAndNormalizeAutomaton(biggerPropertyAutomaton);
        completeAutomata.put(propertyToBuildAutomatonFor, biggerPropertyAutomaton);
        completeAutomata.put(propertyToBuildAutomatonFor.getInverseProperty(), this.getMirroredCopy(biggerPropertyAutomaton));
    }

    protected void increaseWithDefinedInverseIfNecessary(OWLObjectPropertyExpression propertyToBuildAutomatonFor, Automaton leafPropertyAutomaton, Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> inversePropertiesMap, Map<OWLObjectPropertyExpression, Automaton> individualAutomata) {
        Set<OWLObjectPropertyExpression> inverses = inversePropertiesMap.get(propertyToBuildAutomatonFor);
        if (inverses != null) {
            Automaton inversePropertyAutomaton = null;
            for (OWLObjectPropertyExpression inverse : inverses) {
                if (!individualAutomata.containsKey(inverse) || inverse.equals(propertyToBuildAutomatonFor)) continue;
                inversePropertyAutomaton = individualAutomata.get(inverse);
                this.increaseAutomatonWithInversePropertyAutomaton(leafPropertyAutomaton, inversePropertyAutomaton);
            }
        } else if (individualAutomata.containsKey(propertyToBuildAutomatonFor.getInverseProperty())) {
            Automaton autoOfInv_Role = individualAutomata.get(propertyToBuildAutomatonFor.getInverseProperty());
            this.increaseAutomatonWithInversePropertyAutomaton(leafPropertyAutomaton, autoOfInv_Role);
        }
    }

    protected Automaton minimizeAndNormalizeAutomaton(Automaton automaton) {
        return automaton;
    }

    protected void useStandardAutomataConnector(Automaton biggerPropertyAutomaton, Automaton smallerPropertyAutomaton, Transition transition) {
        Map<State, State> stateMapper = this.getDisjointUnion(biggerPropertyAutomaton, smallerPropertyAutomaton);
        State initialState = transition.start();
        State finalState = transition.end();
        State oldStartOfSmaller = stateMapper.get(smallerPropertyAutomaton.initials().iterator().next());
        State oldFinalOfSmaller = stateMapper.get(smallerPropertyAutomaton.terminals().iterator().next());
        Transition t1 = new Transition(initialState, null, oldStartOfSmaller);
        Transition t2 = new Transition(oldFinalOfSmaller, null, finalState);
        biggerPropertyAutomaton.addTransition(t1, "Could not build the Complete Automata of non-Simple Properties");
        biggerPropertyAutomaton.addTransition(t2, "Could not build the Complete Automata of non-Simple Properties");
    }

    protected void automataConnector(Automaton biggerPropertyAutomaton, Automaton smallerPropertyAutomaton, Transition transition) {
        this.useStandardAutomataConnector(biggerPropertyAutomaton, smallerPropertyAutomaton, transition);
    }

    protected Set<Transition> deltaToState(Automaton smallerPropertyAutomaton, State state) {
        HashSet<Transition> incommingTrans = new HashSet<Transition>();
        for (Transition transitionObject : smallerPropertyAutomaton.delta()) {
            Transition transition = transitionObject;
            if (!transition.end().equals(state)) continue;
            incommingTrans.add(transition);
        }
        return incommingTrans;
    }

    protected Graph<OWLObjectPropertyExpression> buildPropertyOrdering(Collection<OWLObjectPropertyExpression[]> simpleObjectPropertyInclusions, Collection<OWLAxioms.ComplexObjectPropertyInclusion> complexObjectPropertyInclusions, Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> equivalentPropertiesMap) {
        Graph propertyDependencyGraph = new Graph();
        for (OWLObjectPropertyExpression[] inclusion : simpleObjectPropertyInclusions) {
            if (inclusion[0].equals(inclusion[1]) || inclusion[0].equals(inclusion[1].getInverseProperty()) || equivalentPropertiesMap.get(inclusion[0]) != null && equivalentPropertiesMap.get(inclusion[0]).contains(inclusion[1])) continue;
            propertyDependencyGraph.addEdge(inclusion[0], inclusion[1]);
        }
        for (OWLAxioms.ComplexObjectPropertyInclusion inclusion : complexObjectPropertyInclusions) {
            OWLObjectPropertyExpression owlSuperProperty = inclusion.m_superObjectProperty;
            OWLObjectPropertyExpression owlSubPropertyInChain = null;
            OWLObjectPropertyExpression[] owlSubProperties = inclusion.m_subObjectProperties;
            if (owlSubProperties.length != 2 && owlSuperProperty.equals(owlSubProperties[0]) && owlSuperProperty.equals(owlSubProperties[owlSubProperties.length - 1])) {
                throw new IllegalArgumentException("The given property hierarchy is not regular.");
            }
            for (int i = 0; i < owlSubProperties.length; ++i) {
                owlSubPropertyInChain = owlSubProperties[i];
                if (owlSubProperties.length != 2 && i > 0 && i < owlSubProperties.length - 1 && (owlSubPropertyInChain.equals(owlSuperProperty) || equivalentPropertiesMap.containsKey(owlSuperProperty) && equivalentPropertiesMap.get(owlSuperProperty).contains(owlSubPropertyInChain))) {
                    throw new IllegalArgumentException("The given property hierarchy is not regular.");
                }
                if (owlSubPropertyInChain.getInverseProperty().equals(owlSuperProperty)) {
                    throw new IllegalArgumentException("The given property hierarchy is not regular.");
                }
                if (owlSubPropertyInChain.equals(owlSuperProperty)) continue;
                propertyDependencyGraph.addEdge(owlSubPropertyInChain, owlSuperProperty);
            }
        }
        return propertyDependencyGraph;
    }

    protected void checkForRegularity(Graph<OWLObjectPropertyExpression> propertyDependencyGraph, Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> equivalentPropertiesMap) {
    	Graph<OWLObjectPropertyExpression> regularityCheckGraph = propertyDependencyGraph.clone();
        boolean trimmed = false;
        do {
            trimmed = false;
            Graph<OWLObjectPropertyExpression> regularityCheckGraphTemp = regularityCheckGraph.clone();
            for (OWLObjectPropertyExpression prop : regularityCheckGraphTemp.getElements()) {
                for (OWLObjectPropertyExpression succProp : regularityCheckGraphTemp.getSuccessors(prop)) {
                    if (!equivalentPropertiesMap.containsKey(prop) || !equivalentPropertiesMap.get(prop).contains(succProp)) continue;
                    for (OWLObjectPropertyExpression succPropSucc : regularityCheckGraphTemp.getSuccessors(succProp)) {
                        if (prop.equals(succPropSucc)) continue;
                        regularityCheckGraph.addEdge(prop, succPropSucc);
                    }
                    trimmed = true;
                    regularityCheckGraph.getSuccessors(prop).remove(succProp);
                }
            }
        } while (trimmed);
        regularityCheckGraph.transitivelyClose();
        for (OWLObjectPropertyExpression prop : regularityCheckGraph.getElements()) {
            Set<OWLObjectPropertyExpression> successors = regularityCheckGraph.getSuccessors(prop);
            if (!successors.contains(prop) && !successors.contains(prop.getInverseProperty())) continue;
            throw new IllegalArgumentException("The given property hierarchy is not regular.\nThere is a cyclic dependency involving property " + prop);
        }
    }

    protected Map<OWLObjectPropertyExpression, Automaton> buildIndividualAutomata(Graph<OWLObjectPropertyExpression> complexPropertiesDependencyGraph, Collection<OWLAxioms.ComplexObjectPropertyInclusion> complexObjectPropertyInclusions, Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> equivalentPropertiesMap, Set<OWLObjectPropertyExpression> transitiveProperties) {
        HashMap<OWLObjectPropertyExpression, Automaton> automataMap = new HashMap<OWLObjectPropertyExpression, Automaton>();
        for (OWLAxioms.ComplexObjectPropertyInclusion inclusion : complexObjectPropertyInclusions) {
            OWLObjectPropertyExpression transitionLabel;
            int i;
            State fromState;
            OWLObjectPropertyExpression[] subObjectProperties = inclusion.m_subObjectProperties;
            OWLObjectPropertyExpression superObjectProperty = inclusion.m_superObjectProperty;
            Automaton automaton = null;
            State initialState = null;
            State finalState = null;
            if (!automataMap.containsKey(superObjectProperty)) {
                automaton = new Automaton();
                initialState = automaton.addState(true, false);
                finalState = automaton.addState(false, true);
                automaton.addTransition(new Transition(initialState, superObjectProperty, finalState), "Could not create automaton");
            } else {
                automaton = automataMap.get(superObjectProperty);
                initialState = automaton.initials().iterator().next();
                finalState = automaton.terminals().iterator().next();
            }
            if (subObjectProperties.length == 2 && subObjectProperties[0].equals(superObjectProperty) && subObjectProperties[1].equals(superObjectProperty)) {
                automaton.addTransition(new Transition(finalState, null, initialState), "Could not create automaton");
                transitiveProperties.add(superObjectProperty);
            } else if (subObjectProperties[0].equals(superObjectProperty)) {
                fromState = finalState;
                for (i = 1; i < subObjectProperties.length - 1; ++i) {
                    transitionLabel = subObjectProperties[i];
                    if (equivalentPropertiesMap.containsKey(superObjectProperty) && equivalentPropertiesMap.get(superObjectProperty).contains(transitionLabel)) {
                        transitionLabel = superObjectProperty;
                    }
                    fromState = this.addNewTransition(automaton, fromState, transitionLabel);
                }
                transitionLabel = subObjectProperties[subObjectProperties.length - 1];
                if (equivalentPropertiesMap.containsKey(superObjectProperty) && equivalentPropertiesMap.get(superObjectProperty).contains(transitionLabel)) {
                    transitionLabel = superObjectProperty;
                }
                automaton.addTransition(new Transition(fromState, transitionLabel, finalState), "Could not create automaton");
            } else if (subObjectProperties[subObjectProperties.length - 1].equals(superObjectProperty)) {
                fromState = initialState;
                for (i = 0; i < subObjectProperties.length - 2; ++i) {
                    transitionLabel = subObjectProperties[i];
                    if (equivalentPropertiesMap.containsKey(superObjectProperty) && equivalentPropertiesMap.get(superObjectProperty).contains(transitionLabel)) {
                        transitionLabel = superObjectProperty;
                    }
                    fromState = this.addNewTransition(automaton, fromState, transitionLabel);
                }
                transitionLabel = subObjectProperties[subObjectProperties.length - 2];
                if (equivalentPropertiesMap.containsKey(superObjectProperty) && equivalentPropertiesMap.get(superObjectProperty).contains(transitionLabel)) {
                    transitionLabel = superObjectProperty;
                }
                automaton.addTransition(new Transition(fromState, transitionLabel, initialState), "Could not create automaton");
            } else {
                fromState = initialState;
                for (i = 0; i < subObjectProperties.length - 1; ++i) {
                    transitionLabel = subObjectProperties[i];
                    if (equivalentPropertiesMap.containsKey(superObjectProperty) && equivalentPropertiesMap.get(superObjectProperty).contains(transitionLabel)) {
                        transitionLabel = superObjectProperty;
                    }
                    fromState = this.addNewTransition(automaton, fromState, transitionLabel);
                }
                transitionLabel = subObjectProperties[subObjectProperties.length - 1];
                if (equivalentPropertiesMap.containsKey(superObjectProperty) && equivalentPropertiesMap.get(superObjectProperty).contains(transitionLabel)) {
                    transitionLabel = superObjectProperty;
                }
                automaton.addTransition(new Transition(fromState, transitionLabel, finalState), "Could not create automaton");
            }
            automataMap.put(superObjectProperty, automaton);
        }
        for (OWLAxioms.ComplexObjectPropertyInclusion inclusion : complexObjectPropertyInclusions) {
            OWLObjectPropertyExpression superpropertyExpression = inclusion.m_superObjectProperty;
            OWLObjectPropertyExpression[] subpropertyExpression = inclusion.m_subObjectProperties;
            if (subpropertyExpression.length != 2 || !subpropertyExpression[0].equals(superpropertyExpression) || !subpropertyExpression[1].equals(superpropertyExpression) || complexPropertiesDependencyGraph.getElements().contains(superpropertyExpression) || automataMap.containsKey(superpropertyExpression.getInverseProperty())) continue;
            complexPropertiesDependencyGraph.addEdge(superpropertyExpression, superpropertyExpression);
            Automaton propertyAutomaton = automataMap.get(superpropertyExpression);
            automataMap.put(superpropertyExpression.getInverseProperty(), this.getMirroredCopy(propertyAutomaton));
        }
        OWLDataFactory df = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        OWLObjectProperty topOP = df.getOWLTopObjectProperty();
        if (!automataMap.containsKey(topOP)) {
            Automaton automaton = new Automaton();
            State initialState = automaton.addState(true, false);
            State finalState = automaton.addState(false, true);
            automaton.addTransition(new Transition(initialState, topOP, finalState), "Could not create automaton");
            automaton.addTransition(new Transition(finalState, null, initialState), "Could not create automaton");
            automataMap.put(topOP, automaton);
        }
        return automataMap;
    }

    protected Map<State, State> getDisjointUnion(Automaton automaton1, Automaton automaton2) {
        HashMap<State, State> stateMapperUnionInverse = new HashMap<State, State>();
        for (State stateObject : automaton2.states()) {
            stateMapperUnionInverse.put(stateObject, automaton1.addState(false, false));
        }
        for (Object transitionObject : automaton2.delta()) {
            Transition transition = (Transition)transitionObject;
            automaton1.addTransition(new Transition(stateMapperUnionInverse.get(transition.start()), transition.label(), stateMapperUnionInverse.get(transition.end())), "Could not create disjoint union of automata");
        }
        return stateMapperUnionInverse;
    }

    protected Automaton getMirroredCopy(Automaton automaton) {
        Automaton mirroredCopy = new Automaton();
        HashMap<State, State> map = new HashMap<State, State>();
        Iterator<State> iterator = automaton.states().iterator();
        while (iterator.hasNext()) {
            State stateObject;
            State state = stateObject = iterator.next();
            map.put(state, mirroredCopy.addState(state.isTerminal(), state.isInitial()));
        }
        for (Object transitionObject : automaton.delta()) {
            Transition transition = (Transition)transitionObject;
            Object label = transition.label();
            if (label instanceof OWLObjectPropertyExpression) {
                label = ((OWLObjectPropertyExpression)label).getInverseProperty();
            }
            mirroredCopy.addTransition(new Transition(map.get(transition.end()), label, map.get(transition.start())), null);
        }
        return mirroredCopy;
    }

    protected State addNewTransition(Automaton automaton, State fromState, OWLObjectPropertyExpression objectPropertyExpression) {
        OWLObjectPropertyExpression propertyOfChain = objectPropertyExpression;
        State toState = automaton.addState(false, false);
        automaton.addTransition(new Transition(fromState, propertyOfChain, toState), "Could not create automaton");
        return toState;
    }
}

