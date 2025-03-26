package org.semanticweb.HermiT.structural;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

public class OWLNormalization {
    protected final OWLDataFactory m_factory;
    protected final OWLAxioms m_axioms;
    protected final int m_firstReplacementIndex;
    protected final Map<OWLClassExpression, OWLClassExpression> m_definitions;
    protected final Map<OWLObjectOneOf, OWLClass> m_definitionsForNegativeNominals;
    protected final ExpressionManager m_expressionManager;
    protected final PLVisitor m_plVisitor;
    protected final Map<OWLDataRange, OWLDatatype> m_dataRangeDefinitions;

    public OWLNormalization(OWLDataFactory factory, OWLAxioms axioms, int firstReplacementIndex) {
        this.m_factory = factory;
        this.m_axioms = axioms;
        this.m_firstReplacementIndex = firstReplacementIndex;
        this.m_definitions = new HashMap<OWLClassExpression, OWLClassExpression>();
        this.m_definitionsForNegativeNominals = new HashMap<OWLObjectOneOf, OWLClass>();
        this.m_expressionManager = new ExpressionManager(this.m_factory);
        this.m_plVisitor = new PLVisitor();
        this.m_dataRangeDefinitions = new HashMap<OWLDataRange, OWLDatatype>();
    }

    public void processOntology(OWLOntology ontology) {
        this.m_axioms.m_classes.addAll(ontology.getClassesInSignature(Imports.INCLUDED));
        this.m_axioms.m_objectProperties.addAll(ontology.getObjectPropertiesInSignature(Imports.INCLUDED));
        this.m_axioms.m_dataProperties.addAll(ontology.getDataPropertiesInSignature(Imports.INCLUDED));
        this.m_axioms.m_namedIndividuals.addAll(ontology.getIndividualsInSignature(Imports.INCLUDED));
        this.processAxioms(ontology.getLogicalAxioms());
    }

    public void processAxioms(Collection<? extends OWLAxiom> axioms) {
        AxiomVisitor axiomVisitor = new AxiomVisitor();
        for (OWLAxiom axiom : axioms) {
            axiom.accept(axiomVisitor);
        }
        RuleNormalizer ruleNormalizer = new RuleNormalizer(this.m_axioms.m_rules, axiomVisitor.m_classExpressionInclusionsAsDisjunctions, axiomVisitor.m_dataRangeInclusionsAsDisjunctions);
        for (SWRLRule rule : axiomVisitor.m_rules) {
            ruleNormalizer.visit(rule);
        }
        this.normalizeInclusions(axiomVisitor.m_classExpressionInclusionsAsDisjunctions, axiomVisitor.m_dataRangeInclusionsAsDisjunctions);
    }

    protected void addFact(OWLIndividualAxiom axiom) {
        this.m_axioms.m_facts.add(axiom);
    }

    protected void addInclusion(OWLObjectPropertyExpression subObjectPropertyExpression, OWLObjectPropertyExpression superObjectPropertyExpression) {
        this.m_axioms.m_simpleObjectPropertyInclusions.add(new OWLObjectPropertyExpression[]{subObjectPropertyExpression, superObjectPropertyExpression});
    }

    protected void addInclusion(OWLObjectPropertyExpression[] subObjectPropertyExpressions, OWLObjectPropertyExpression superObjectPropertyExpression) {
        for (int index = subObjectPropertyExpressions.length - 1; index >= 0; --index) {
            subObjectPropertyExpressions[index] = subObjectPropertyExpressions[index];
        }
        this.m_axioms.m_complexObjectPropertyInclusions.add(new OWLAxioms.ComplexObjectPropertyInclusion(subObjectPropertyExpressions, superObjectPropertyExpression));
    }

    protected void addInclusion(OWLDataPropertyExpression subDataPropertyExpression, OWLDataPropertyExpression superDataPropertyExpression) {
        this.m_axioms.m_dataPropertyInclusions.add(new OWLDataPropertyExpression[]{subDataPropertyExpression, superDataPropertyExpression});
    }

    protected void makeTransitive(OWLObjectPropertyExpression objectPropertyExpression) {
        this.m_axioms.m_complexObjectPropertyInclusions.add(new OWLAxioms.ComplexObjectPropertyInclusion(objectPropertyExpression));
    }

    protected void makeReflexive(OWLObjectPropertyExpression objectPropertyExpression) {
        this.m_axioms.m_reflexiveObjectProperties.add(objectPropertyExpression);
    }

    protected void makeIrreflexive(OWLObjectPropertyExpression objectPropertyExpression) {
        this.m_axioms.m_irreflexiveObjectProperties.add(objectPropertyExpression);
    }

    protected void makeAsymmetric(OWLObjectPropertyExpression objectPropertyExpression) {
        this.m_axioms.m_asymmetricObjectProperties.add(objectPropertyExpression);
    }

    protected static boolean isSimple(OWLClassExpression description) {
        return description instanceof OWLClass || description instanceof OWLObjectComplementOf && ((OWLObjectComplementOf)description).getOperand() instanceof OWLClass;
    }

    protected static boolean isLiteral(OWLDataRange dr) {
        return OWLNormalization.isAtomic(dr) || OWLNormalization.isNegatedAtomic(dr);
    }

    protected static boolean isAtomic(OWLDataRange dr) {
        return dr instanceof OWLDatatype || dr instanceof OWLDatatypeRestriction || dr instanceof OWLDataOneOf;
    }

    protected static boolean isNegatedAtomic(OWLDataRange dr) {
        return dr instanceof OWLDataComplementOf && OWLNormalization.isAtomic(((OWLDataComplementOf)dr).getDataRange());
    }

    protected static boolean isNominal(OWLClassExpression description) {
        return description instanceof OWLObjectOneOf;
    }

    protected static boolean isNegatedOneNominal(OWLClassExpression description) {
        if (!(description instanceof OWLObjectComplementOf)) {
            return false;
        }
        OWLClassExpression operand = ((OWLObjectComplementOf)description).getOperand();
        if (!(operand instanceof OWLObjectOneOf)) {
            return false;
        }
        return ((OWLObjectOneOf)operand).getIndividuals().size() == 1;
    }

    protected void normalizeInclusions(List<OWLClassExpression[]> inclusions, List<OWLDataRange[]> dataRangeInclusions) {
        ClassExpressionNormalizer classExpressionNormalizer = new ClassExpressionNormalizer(inclusions, dataRangeInclusions);
        while (!inclusions.isEmpty()) {
            OWLClassExpression simplifiedDescription = this.m_expressionManager.getNNF(this.m_expressionManager.getSimplified(this.m_factory.getOWLObjectUnionOf(inclusions.remove(inclusions.size() - 1))));
            if (simplifiedDescription.isOWLThing()) continue;
            if (simplifiedDescription instanceof OWLObjectUnionOf) {
                OWLObjectUnionOf objectOr = (OWLObjectUnionOf)simplifiedDescription;
                OWLClassExpression[] descriptions = new OWLClassExpression[objectOr.getOperands().size()];
                objectOr.getOperands().toArray(descriptions);
                if (this.distributeUnionOverAnd(descriptions, inclusions) || this.optimizedNegativeOneOfTranslation(descriptions, this.m_axioms.m_facts)) continue;
                for (int index = 0; index < descriptions.length; ++index) {
                    descriptions[index] = (OWLClassExpression)descriptions[index].accept((OWLClassExpressionVisitorEx)classExpressionNormalizer);
                }
                this.m_axioms.m_conceptInclusions.add(descriptions);
                continue;
            }
            if (simplifiedDescription instanceof OWLObjectIntersectionOf) {
                OWLObjectIntersectionOf objectAnd = (OWLObjectIntersectionOf)simplifiedDescription;
                for (OWLClassExpression conjunct : objectAnd.getOperands()) {
                    inclusions.add(new OWLClassExpression[]{conjunct});
                }
                continue;
            }
            OWLClassExpression normalized = (OWLClassExpression)simplifiedDescription.accept((OWLClassExpressionVisitorEx)classExpressionNormalizer);
            this.m_axioms.m_conceptInclusions.add(new OWLClassExpression[]{normalized});
        }
        DataRangeNormalizer dataRangeNormalizer = new DataRangeNormalizer(dataRangeInclusions);
        while (!dataRangeInclusions.isEmpty()) {
            OWLDataRange simplifiedDescription = this.m_expressionManager.getNNF(this.m_expressionManager.getSimplified(this.m_factory.getOWLDataUnionOf(dataRangeInclusions.remove(classExpressionNormalizer.m_newDataRangeInclusions.size() - 1))));
            if (simplifiedDescription.isTopDatatype()) continue;
            if (simplifiedDescription instanceof OWLDataUnionOf) {
                OWLDataUnionOf dataOr = (OWLDataUnionOf)simplifiedDescription;
                OWLDataRange[] descriptions = new OWLDataRange[dataOr.getOperands().size()];
                dataOr.getOperands().toArray(descriptions);
                if (this.distributeUnionOverAnd(descriptions, dataRangeInclusions)) continue;
                for (int index = 0; index < descriptions.length; ++index) {
                    descriptions[index] = (OWLDataRange)descriptions[index].accept((OWLDataVisitorEx)dataRangeNormalizer);
                }
                this.m_axioms.m_dataRangeInclusions.add(descriptions);
                continue;
            }
            if (simplifiedDescription instanceof OWLDataIntersectionOf) {
                OWLDataIntersectionOf dataAnd = (OWLDataIntersectionOf)simplifiedDescription;
                for (OWLDataRange conjunct : dataAnd.getOperands()) {
                    dataRangeInclusions.add(new OWLDataRange[]{conjunct});
                }
                continue;
            }
            OWLDataRange normalized = (OWLDataRange)simplifiedDescription.accept((OWLDataVisitorEx)dataRangeNormalizer);
            dataRangeInclusions.add(new OWLDataRange[]{normalized});
        }
    }

    protected boolean distributeUnionOverAnd(OWLClassExpression[] descriptions, List<OWLClassExpression[]> inclusions) {
        int andIndex = -1;
        for (int index = 0; index < descriptions.length; ++index) {
            OWLClassExpression description = descriptions[index];
            if (OWLNormalization.isSimple(description)) continue;
            if (description instanceof OWLObjectIntersectionOf) {
                if (andIndex == -1) {
                    andIndex = index;
                    continue;
                }
                return false;
            }
            return false;
        }
        if (andIndex == -1) {
            return false;
        }
        OWLObjectIntersectionOf objectAnd = (OWLObjectIntersectionOf)descriptions[andIndex];
        for (OWLClassExpression description : objectAnd.getOperands()) {
            OWLClassExpression[] newDescriptions = descriptions.clone();
            newDescriptions[andIndex] = description;
            inclusions.add(newDescriptions);
        }
        return true;
    }

    protected boolean distributeUnionOverAnd(OWLDataRange[] descriptions, List<OWLDataRange[]> inclusions) {
        int andIndex = -1;
        for (int index = 0; index < descriptions.length; ++index) {
            OWLDataRange description = descriptions[index];
            if (OWLNormalization.isLiteral(description)) continue;
            if (description instanceof OWLDataIntersectionOf) {
                if (andIndex == -1) {
                    andIndex = index;
                    continue;
                }
                return false;
            }
            return false;
        }
        if (andIndex == -1) {
            return false;
        }
        OWLDataIntersectionOf dataAnd = (OWLDataIntersectionOf)descriptions[andIndex];
        for (OWLDataRange description : dataAnd.getOperands()) {
            OWLDataRange[] newDescriptions = descriptions.clone();
            newDescriptions[andIndex] = description;
            inclusions.add(newDescriptions);
        }
        return true;
    }

    protected boolean optimizedNegativeOneOfTranslation(OWLClassExpression[] descriptions, Collection<OWLIndividualAxiom> facts) {
        if (descriptions.length == 2) {
            OWLObjectOneOf nominal = null;
            OWLClassExpression other = null;
            if (descriptions[0] instanceof OWLObjectComplementOf && ((OWLObjectComplementOf)descriptions[0]).getOperand() instanceof OWLObjectOneOf) {
                nominal = (OWLObjectOneOf)((OWLObjectComplementOf)descriptions[0]).getOperand();
                other = descriptions[1];
            } else if (descriptions[1] instanceof OWLObjectComplementOf && ((OWLObjectComplementOf)descriptions[1]).getOperand() instanceof OWLObjectOneOf) {
                other = descriptions[0];
                nominal = (OWLObjectOneOf)((OWLObjectComplementOf)descriptions[1]).getOperand();
            }
            if (nominal != null && (other instanceof OWLClass || other instanceof OWLObjectComplementOf && ((OWLObjectComplementOf)other).getOperand() instanceof OWLClass)) {
                assert (other != null);
                for (OWLIndividual individual : nominal.getIndividuals()) {
                    facts.add(this.m_factory.getOWLClassAssertionAxiom(other, individual));
                }
                return true;
            }
        }
        return false;
    }

    protected OWLClassExpression getDefinitionFor(OWLClassExpression description, boolean[] alreadyExists, boolean forcePositive) {
        OWLClassExpression definition = this.m_definitions.get(description);
        if (definition == null || forcePositive && !(definition instanceof OWLClass)) {
            definition = this.m_factory.getOWLClass(IRI.create("internal:def#" + (this.m_definitions.size() + this.m_firstReplacementIndex)));
            if (!forcePositive && !((Boolean)description.accept((OWLClassExpressionVisitorEx)this.m_plVisitor)).booleanValue()) {
                definition = this.m_factory.getOWLObjectComplementOf(definition);
            }
            this.m_definitions.put(description, definition);
            alreadyExists[0] = false;
        } else {
            alreadyExists[0] = true;
        }
        return definition;
    }

    protected OWLDatatype getDefinitionFor(OWLDataRange dr, boolean[] alreadyExists) {
        OWLDatatype definition = this.m_dataRangeDefinitions.get(dr);
        if (definition == null) {
            definition = this.m_factory.getOWLDatatype(IRI.create("internal:defdata#" + this.m_dataRangeDefinitions.size()));
            this.m_dataRangeDefinitions.put(dr, definition);
            alreadyExists[0] = false;
        } else {
            alreadyExists[0] = true;
        }
        return definition;
    }

    protected OWLClassExpression getDefinitionFor(OWLClassExpression description, boolean[] alreadyExists) {
        return this.getDefinitionFor(description, alreadyExists, false);
    }

    protected OWLClass getClassFor(OWLClassExpression description, boolean[] alreadyExists) {
        return (OWLClass)this.getDefinitionFor(description, alreadyExists, true);
    }

    protected OWLClass getDefinitionForNegativeNominal(OWLObjectOneOf nominal, boolean[] alreadyExists) {
        OWLClass definition = this.m_definitionsForNegativeNominals.get(nominal);
        if (definition == null) {
            definition = this.m_factory.getOWLClass(IRI.create("internal:nnq#" + this.m_definitionsForNegativeNominals.size()));
            this.m_definitionsForNegativeNominals.put(nominal, definition);
            alreadyExists[0] = false;
        } else {
            alreadyExists[0] = true;
        }
        return definition;
    }

    protected OWLClassExpression positive(OWLClassExpression description) {
        return this.m_expressionManager.getNNF(this.m_expressionManager.getSimplified(description));
    }

    protected OWLClassExpression negative(OWLClassExpression description) {
        return this.m_expressionManager.getComplementNNF(this.m_expressionManager.getSimplified(description));
    }

    protected OWLDataRange positive(OWLDataRange dataRange) {
        return this.m_expressionManager.getNNF(this.m_expressionManager.getSimplified(dataRange));
    }

    protected OWLDataRange negative(OWLDataRange dataRange) {
        return this.m_expressionManager.getComplementNNF(this.m_expressionManager.getSimplified(dataRange));
    }

    protected class PLVisitor
    implements OWLClassExpressionVisitorEx<Boolean> {
        protected PLVisitor() {
        }

        public Boolean visit(OWLClass object) {
            if (object.isOWLThing()) {
                return Boolean.FALSE;
            }
            if (object.isOWLNothing()) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

        public Boolean visit(OWLObjectIntersectionOf object) {
            for (OWLClassExpression desc : object.getOperands()) {
                if (!((Boolean)desc.accept((OWLClassExpressionVisitorEx)this)).booleanValue()) continue;
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }

        public Boolean visit(OWLObjectUnionOf object) {
            for (OWLClassExpression desc : object.getOperands()) {
                if (!((Boolean)desc.accept((OWLClassExpressionVisitorEx)this)).booleanValue()) continue;
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }

        public Boolean visit(OWLObjectComplementOf object) {
            return Boolean.FALSE;
        }

        public Boolean visit(OWLObjectOneOf object) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLObjectSomeValuesFrom object) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLObjectAllValuesFrom object) {
            return (Boolean) object.getFiller().accept((OWLClassExpressionVisitorEx)this);
        }

        public Boolean visit(OWLObjectHasValue object) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLObjectHasSelf object) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLObjectMinCardinality object) {
            return object.getCardinality() > 0;
        }

        public Boolean visit(OWLObjectMaxCardinality object) {
            return object.getCardinality() > 0 ? Boolean.TRUE : (Boolean)OWLNormalization.this.m_expressionManager.getComplementNNF(object.getFiller()).accept((OWLClassExpressionVisitorEx)this);
        }

        public Boolean visit(OWLObjectExactCardinality object) {
            return object.getCardinality() > 0 ? Boolean.TRUE : (Boolean)OWLNormalization.this.m_expressionManager.getComplementNNF(object.getFiller()).accept((OWLClassExpressionVisitorEx)this);
        }

        public Boolean visit(OWLDataSomeValuesFrom desc) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLDataAllValuesFrom desc) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLDataHasValue desc) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLDataMinCardinality desc) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLDataMaxCardinality desc) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLDataExactCardinality desc) {
            return Boolean.TRUE;
        }
    }

    protected final class RuleNormalizer
    implements SWRLObjectVisitor {
        private final Collection<OWLAxioms.DisjunctiveRule> m_rules;
        private final Collection<OWLClassExpression[]> m_classExpressionInclusions;
        private final Collection<OWLDataRange[]> m_dataRangeInclusions;
        private final boolean[] m_alreadyExists;
        private final List<SWRLAtom> m_bodyAtoms = new ArrayList<SWRLAtom>();
        private final List<SWRLAtom> m_headAtoms = new ArrayList<SWRLAtom>();
        private final Set<SWRLAtom> m_normalizedBodyAtoms = new HashSet<SWRLAtom>();
        private final Set<SWRLAtom> m_normalizedHeadAtoms = new HashSet<SWRLAtom>();
        private final Map<SWRLVariable, SWRLVariable> m_variableRepresentative = new HashMap<SWRLVariable, SWRLVariable>();
        private final Map<OWLNamedIndividual, SWRLVariable> m_individualsToVariables = new HashMap<OWLNamedIndividual, SWRLVariable>();
        private final Set<SWRLVariable> m_bodyDataRangeVariables = new HashSet<SWRLVariable>();
        private final Set<SWRLVariable> m_headDataRangeVariables = new HashSet<SWRLVariable>();
        private int m_newVariableIndex = 0;
        private boolean m_isPositive;

        public RuleNormalizer(Collection<OWLAxioms.DisjunctiveRule> rules, Collection<OWLClassExpression[]> classExpressionInclusionsFromRules, Collection<OWLDataRange[]> newDataRangeInclusions) {
            this.m_rules = rules;
            this.m_classExpressionInclusions = classExpressionInclusionsFromRules;
            this.m_dataRangeInclusions = newDataRangeInclusions;
            this.m_alreadyExists = new boolean[1];
        }

        public void visit(SWRLRule rule) {
            for (SWRLAtom headAtom : rule.getHead()) {
                this.m_individualsToVariables.clear();
                this.m_bodyAtoms.clear();
                this.m_headAtoms.clear();
                this.m_variableRepresentative.clear();
                this.m_normalizedBodyAtoms.clear();
                this.m_normalizedHeadAtoms.clear();
                this.m_bodyDataRangeVariables.clear();
                this.m_headDataRangeVariables.clear();
                this.m_bodyAtoms.addAll(rule.getBody());
                this.m_headAtoms.add(headAtom);
                for (SWRLAtom atom : rule.getBody()) {
                    if (!(atom instanceof SWRLSameIndividualAtom)) continue;
                    this.m_bodyAtoms.remove(atom);
                    SWRLSameIndividualAtom sameIndividualAtom = (SWRLSameIndividualAtom)atom;
                    SWRLVariable variable1 = this.getVariableFor(sameIndividualAtom.getFirstArgument());
                    SWRLIArgument argument2 = sameIndividualAtom.getSecondArgument();
                    if (argument2 instanceof SWRLVariable) {
                        this.m_variableRepresentative.put((SWRLVariable)argument2, variable1);
                        continue;
                    }
                    OWLIndividual individual = ((SWRLIndividualArgument)argument2).getIndividual();
                    if (individual.isAnonymous()) {
                        throw new IllegalArgumentException("Internal error: Rules with anonymous individuals are not supported. ");
                    }
                    this.m_individualsToVariables.put(individual.asOWLNamedIndividual(), variable1);
                    this.m_bodyAtoms.add(OWLNormalization.this.m_factory.getSWRLClassAtom(OWLNormalization.this.m_factory.getOWLObjectOneOf(new OWLIndividual[]{individual}), variable1));
                }
                this.m_isPositive = true;
                while (!this.m_headAtoms.isEmpty()) {
                    this.m_headAtoms.remove(0).accept(this);
                }
                this.m_isPositive = false;
                while (!this.m_bodyAtoms.isEmpty()) {
                    this.m_bodyAtoms.remove(0).accept(this);
                }
                if (!this.m_bodyDataRangeVariables.containsAll(this.m_headDataRangeVariables)) {
                    throw new IllegalArgumentException("A SWRL rule contains data range variables in the head, but not in the body, and this is not supported.");
                }
                this.m_rules.add(new OWLAxioms.DisjunctiveRule(this.m_normalizedBodyAtoms.toArray(new SWRLAtom[this.m_normalizedBodyAtoms.size()]), this.m_normalizedHeadAtoms.toArray(new SWRLAtom[this.m_normalizedHeadAtoms.size()])));
            }
        }

        public void visit(SWRLClassAtom at) {
            OWLClassExpression c = OWLNormalization.this.m_expressionManager.getSimplified(OWLNormalization.this.m_expressionManager.getNNF(at.getPredicate()));
            SWRLVariable variable = this.getVariableFor(at.getArgument());
            if (this.m_isPositive) {
                if (c instanceof OWLClass) {
                    this.m_normalizedHeadAtoms.add(OWLNormalization.this.m_factory.getSWRLClassAtom(c, variable));
                } else {
                    OWLClass definition = OWLNormalization.this.getClassFor(at.getPredicate(), this.m_alreadyExists);
                    if (!this.m_alreadyExists[0]) {
                        this.m_classExpressionInclusions.add(new OWLClassExpression[]{OWLNormalization.this.negative(definition), at.getPredicate()});
                    }
                    this.m_normalizedHeadAtoms.add(OWLNormalization.this.m_factory.getSWRLClassAtom(definition, variable));
                }
            } else if (c instanceof OWLClass) {
                this.m_normalizedBodyAtoms.add(OWLNormalization.this.m_factory.getSWRLClassAtom(c, variable));
            } else {
                OWLClass definition = OWLNormalization.this.getClassFor(at.getPredicate(), this.m_alreadyExists);
                if (!this.m_alreadyExists[0]) {
                    this.m_classExpressionInclusions.add(new OWLClassExpression[]{OWLNormalization.this.negative(at.getPredicate()), definition});
                }
                this.m_normalizedBodyAtoms.add(OWLNormalization.this.m_factory.getSWRLClassAtom(definition, variable));
            }
        }

        public void visit(SWRLDataRangeAtom at) {
            OWLDataRange dr = at.getPredicate();
            SWRLDArgument argument = at.getArgument();
            if (!(argument instanceof SWRLVariable)) {
                throw new IllegalArgumentException("A SWRL rule contains a data range with an argument that is not a literal, and such rules are not supported.");
            }
            if (!this.m_isPositive) {
                dr = OWLNormalization.this.m_factory.getOWLDataComplementOf(dr);
            }
            if ((dr = OWLNormalization.this.m_expressionManager.getNNF(OWLNormalization.this.m_expressionManager.getSimplified(dr))) instanceof OWLDataIntersectionOf || dr instanceof OWLDataUnionOf) {
                OWLDatatype definition = OWLNormalization.this.getDefinitionFor(dr, this.m_alreadyExists);
                if (!this.m_alreadyExists[0]) {
                    this.m_dataRangeInclusions.add(new OWLDataRange[]{OWLNormalization.this.negative(definition), dr});
                }
                dr = definition;
            }
            SWRLDataRangeAtom atom = OWLNormalization.this.m_factory.getSWRLDataRangeAtom(dr, argument);
            this.m_normalizedHeadAtoms.add(atom);
            this.m_headDataRangeVariables.add((SWRLVariable)argument);
        }

        public void visit(SWRLObjectPropertyAtom at) {
            SWRLVariable variable2;
            SWRLVariable variable1;
            OWLObjectPropertyExpression ope = at.getPredicate();
            OWLObjectProperty op = ope.getNamedProperty();
            if (ope.isAnonymous()) {
                variable1 = this.getVariableFor(at.getSecondArgument());
                variable2 = this.getVariableFor(at.getFirstArgument());
            } else {
                variable1 = this.getVariableFor(at.getFirstArgument());
                variable2 = this.getVariableFor(at.getSecondArgument());
            }
            SWRLObjectPropertyAtom newAtom = OWLNormalization.this.m_factory.getSWRLObjectPropertyAtom(op, variable1, variable2);
            if (this.m_isPositive) {
                this.m_normalizedHeadAtoms.add(newAtom);
            } else {
                this.m_normalizedBodyAtoms.add(newAtom);
            }
        }

        public void visit(SWRLDataPropertyAtom at) {
            OWLDataProperty dp = at.getPredicate().asOWLDataProperty();
            SWRLVariable variable1 = this.getVariableFor(at.getFirstArgument());
            SWRLDArgument argument2 = at.getSecondArgument();
            if (argument2 instanceof SWRLVariable) {
                SWRLVariable variable2 = this.getVariableFor((SWRLVariable)argument2);
                if (this.m_isPositive) {
                    this.m_normalizedHeadAtoms.add(OWLNormalization.this.m_factory.getSWRLDataPropertyAtom(dp, variable1, variable2));
                    this.m_headDataRangeVariables.add(variable2);
                } else if (this.m_bodyDataRangeVariables.add(variable2)) {
                    this.m_normalizedBodyAtoms.add(OWLNormalization.this.m_factory.getSWRLDataPropertyAtom(dp, variable1, variable2));
                } else {
                    SWRLVariable variable2Fresh = this.getFreshVariable();
                    this.m_normalizedBodyAtoms.add(OWLNormalization.this.m_factory.getSWRLDataPropertyAtom(dp, variable1, variable2Fresh));
                    this.m_normalizedHeadAtoms.add(OWLNormalization.this.m_factory.getSWRLDifferentIndividualsAtom(variable2, variable2Fresh));
                }
            } else {
                OWLLiteral literal = ((SWRLLiteralArgument)argument2).getLiteral();
                SWRLClassAtom newAtom = OWLNormalization.this.m_factory.getSWRLClassAtom(OWLNormalization.this.m_factory.getOWLDataHasValue(dp, literal), variable1);
                if (this.m_isPositive) {
                    this.m_headAtoms.add(newAtom);
                } else {
                    this.m_bodyAtoms.add(newAtom);
                }
            }
        }

        public void visit(SWRLBuiltInAtom at) {
            throw new IllegalArgumentException("A SWRL rule uses a built-in atom, but built-in atoms are not supported yet.");
        }

        public void visit(SWRLSameIndividualAtom at) {
            if (!this.m_isPositive) {
                throw new IllegalStateException("Internal error: this SWRLSameIndividualAtom should have been processed earlier.");
            }
            this.m_normalizedHeadAtoms.add(OWLNormalization.this.m_factory.getSWRLSameIndividualAtom(this.getVariableFor(at.getFirstArgument()), this.getVariableFor(at.getSecondArgument())));
        }

        public void visit(SWRLDifferentIndividualsAtom at) {
            if (this.m_isPositive) {
                this.m_normalizedHeadAtoms.add(OWLNormalization.this.m_factory.getSWRLDifferentIndividualsAtom(this.getVariableFor(at.getFirstArgument()), this.getVariableFor(at.getSecondArgument())));
            } else {
                this.m_normalizedHeadAtoms.add(OWLNormalization.this.m_factory.getSWRLSameIndividualAtom(this.getVariableFor(at.getFirstArgument()), this.getVariableFor(at.getSecondArgument())));
            }
        }

        public void visit(SWRLVariable variable) {
        }

        public void visit(SWRLIndividualArgument argument) {
        }

        public void visit(SWRLLiteralArgument argument) {
        }

        private SWRLVariable getVariableFor(SWRLIArgument term) {
            SWRLVariable variable;
            if (term instanceof SWRLIndividualArgument) {
                OWLIndividual individual = ((SWRLIndividualArgument)term).getIndividual();
                if (individual.isAnonymous()) {
                    throw new IllegalArgumentException("Internal error: Rules with anonymous individuals are not supported. ");
                }
                variable = this.m_individualsToVariables.get(individual.asOWLNamedIndividual());
                if (variable == null) {
                    variable = this.getFreshVariable();
                    this.m_individualsToVariables.put(individual.asOWLNamedIndividual(), variable);
                    this.m_bodyAtoms.add(OWLNormalization.this.m_factory.getSWRLClassAtom(OWLNormalization.this.m_factory.getOWLObjectOneOf(new OWLIndividual[]{individual}), variable));
                }
            } else {
                variable = (SWRLVariable)term;
            }
            SWRLVariable representative = this.m_variableRepresentative.get(variable);
            if (representative == null) {
                return variable;
            }
            return representative;
        }

        private SWRLVariable getFreshVariable() {
            SWRLVariable variable = OWLNormalization.this.m_factory.getSWRLVariable(IRI.create("internal:swrl#" + this.m_newVariableIndex));
            ++this.m_newVariableIndex;
            return variable;
        }
    }

    protected class Rule2FactConverter
    implements SWRLObjectVisitor {
        protected final boolean[] m_alreadyExists = new boolean[1];
        protected final Collection<OWLClassExpression[]> m_newInclusions;
        protected int freshDataProperties = 0;
        protected int freshIndividuals = 0;

        public Rule2FactConverter(Collection<OWLClassExpression[]> newInclusions) {
            this.m_newInclusions = newInclusions;
        }

        protected OWLNamedIndividual getFreshIndividual() {
            OWLNamedIndividual freshInd = OWLNormalization.this.m_factory.getOWLNamedIndividual(IRI.create("internal:nom#swrlfact" + this.freshIndividuals));
            ++this.freshIndividuals;
            OWLNormalization.this.m_axioms.m_namedIndividuals.add(freshInd);
            return freshInd;
        }

        protected OWLDataProperty getFreshDataProperty() {
            ++this.freshDataProperties;
            return OWLNormalization.this.m_factory.getOWLDataProperty(IRI.create("internal:freshDP#" + this.freshDataProperties));
        }

        public void visit(SWRLRule rule) {
        }

        public void visit(SWRLClassAtom atom) {
            if (!(atom.getArgument() instanceof SWRLIndividualArgument)) {
                throw new IllegalArgumentException("A SWRL rule contains a head atom " + atom + " with a variable that does not occur in the body. ");
            }
            OWLIndividual ind = ((SWRLIndividualArgument)atom.getArgument()).getIndividual();
            if (ind.isAnonymous()) {
                this.throwAnonIndError(atom);
            }
            if (!OWLNormalization.isSimple(atom.getPredicate())) {
                OWLClassExpression definition = OWLNormalization.this.getDefinitionFor(atom.getPredicate(), this.m_alreadyExists);
                if (!this.m_alreadyExists[0]) {
                    this.m_newInclusions.add(new OWLClassExpression[]{OWLNormalization.this.negative(definition), atom.getPredicate()});
                }
                OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLClassAssertionAxiom(definition, ind.asOWLNamedIndividual()));
            } else {
                OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLClassAssertionAxiom(atom.getPredicate(), ind.asOWLNamedIndividual()));
            }
        }

        public void visit(SWRLDataRangeAtom atom) {
            if (atom.getArgument() instanceof SWRLVariable) {
                this.throwVarError(atom);
            }
            OWLLiteral lit = ((SWRLLiteralArgument)atom.getArgument()).getLiteral();
            OWLDataRange dr = atom.getPredicate();
            OWLNamedIndividual freshIndividual = this.getFreshIndividual();
            OWLDataProperty freshDP = this.getFreshDataProperty();
            OWLDataSomeValuesFrom some = OWLNormalization.this.m_factory.getOWLDataSomeValuesFrom(freshDP, OWLNormalization.this.m_factory.getOWLDataOneOf(new OWLLiteral[]{lit}));
            OWLClassExpression definition = OWLNormalization.this.getDefinitionFor(some, this.m_alreadyExists);
            if (!this.m_alreadyExists[0]) {
                this.m_newInclusions.add(new OWLClassExpression[]{OWLNormalization.this.negative(definition), some});
            }
            OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLClassAssertionAxiom(definition, freshIndividual));
            this.m_newInclusions.add(new OWLClassExpression[]{OWLNormalization.this.m_factory.getOWLDataAllValuesFrom(freshDP, dr)});
        }

        public void visit(SWRLObjectPropertyAtom atom) {
            if (!(atom.getFirstArgument() instanceof SWRLIndividualArgument) || !(atom.getSecondArgument() instanceof SWRLIndividualArgument)) {
                this.throwVarError(atom);
            }
            OWLObjectPropertyExpression ope = atom.getPredicate();
            OWLIndividual first = ((SWRLIndividualArgument)atom.getFirstArgument()).getIndividual();
            OWLIndividual second = ((SWRLIndividualArgument)atom.getSecondArgument()).getIndividual();
            if (first.isAnonymous() || second.isAnonymous()) {
                this.throwAnonIndError(atom);
            }
            if (ope.isAnonymous()) {
                OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLObjectPropertyAssertionAxiom(ope.getNamedProperty(), second.asOWLNamedIndividual(), first.asOWLNamedIndividual()));
            } else {
                OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLObjectPropertyAssertionAxiom(ope.asOWLObjectProperty(), first.asOWLNamedIndividual(), second.asOWLNamedIndividual()));
            }
        }

        public void visit(SWRLDataPropertyAtom atom) {
            OWLIndividual ind;
            if (!(atom.getSecondArgument() instanceof SWRLLiteralArgument)) {
                this.throwVarError(atom);
            }
            if (!(atom.getFirstArgument() instanceof SWRLIndividualArgument)) {
                this.throwVarError(atom);
            }
            if ((ind = ((SWRLIndividualArgument)atom.getFirstArgument()).getIndividual()).isAnonymous()) {
                this.throwAnonIndError(atom);
            }
            OWLLiteral lit = ((SWRLLiteralArgument)atom.getSecondArgument()).getLiteral();
            OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLDataPropertyAssertionAxiom(atom.getPredicate().asOWLDataProperty(), ind.asOWLNamedIndividual(), lit));
        }

        public void visit(SWRLBuiltInAtom atom) {
            throw new IllegalArgumentException("Error: A rule uses built-in atoms (" + atom + "), but built-in atoms are not supported yet. ");
        }

        public void visit(SWRLSameIndividualAtom atom) {
            HashSet<OWLNamedIndividual> inds = new HashSet<OWLNamedIndividual>();
            for (SWRLArgument arg : atom.getAllArguments()) {
                OWLIndividual ind;
                if (!(arg instanceof SWRLIndividualArgument)) {
                    this.throwVarError(atom);
                }
                if ((ind = ((SWRLIndividualArgument)arg).getIndividual()).isAnonymous()) {
                    this.throwAnonIndError(atom);
                }
                inds.add(ind.asOWLNamedIndividual());
            }
            OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLSameIndividualAxiom(inds));
        }

        public void visit(SWRLDifferentIndividualsAtom atom) {
            HashSet<OWLNamedIndividual> inds = new HashSet<OWLNamedIndividual>();
            for (SWRLArgument arg : atom.getAllArguments()) {
                OWLIndividual ind;
                if (!(arg instanceof SWRLIndividualArgument)) {
                    this.throwVarError(atom);
                }
                if ((ind = ((SWRLIndividualArgument)arg).getIndividual()).isAnonymous()) {
                    this.throwAnonIndError(atom);
                }
                inds.add(ind.asOWLNamedIndividual());
            }
            OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLDifferentIndividualsAxiom(inds));
        }

        public void visit(SWRLVariable variable) {
        }

        public void visit(SWRLIndividualArgument argument) {
        }

        public void visit(SWRLLiteralArgument argument) {
        }

        protected void throwAnonIndError(SWRLAtom atom) {
            throw new IllegalArgumentException("A SWRL rule contains a fact (" + atom + ") with an anonymous individual, which is not allowed. ");
        }

        protected void throwVarError(SWRLAtom atom) {
            throw new IllegalArgumentException("A SWRL rule contains a head atom (" + atom + ") with a variable that does not occur in the body. ");
        }
    }

    protected class DataRangeNormalizer
    implements OWLDataVisitorEx<OWLDataRange> {
        protected final Collection<OWLDataRange[]> m_newDataRangeInclusions;
        protected final boolean[] m_alreadyExists;

        public DataRangeNormalizer(Collection<OWLDataRange[]> newDataRangeInclusions) {
            this.m_newDataRangeInclusions = newDataRangeInclusions;
            this.m_alreadyExists = new boolean[1];
        }

        public OWLDataRange visit(OWLDatatype node) {
            return node;
        }

        public OWLDataRange visit(OWLDataComplementOf node) {
            return node;
        }

        public OWLDataRange visit(OWLDataOneOf node) {
            return node;
        }

        public OWLDataRange visit(OWLDataIntersectionOf object) {
            OWLDatatype definition = OWLNormalization.this.getDefinitionFor(object, this.m_alreadyExists);
            if (!this.m_alreadyExists[0]) {
                for (OWLDataRange description : object.getOperands()) {
                    this.m_newDataRangeInclusions.add(new OWLDataRange[]{OWLNormalization.this.negative(definition), description});
                }
            }
            return definition;
        }

        public OWLDataRange visit(OWLDataUnionOf node) {
            throw new IllegalStateException("OR should be broken down at the outermost level");
        }

        public OWLDataRange visit(OWLDatatypeRestriction node) {
            return node;
        }

        public OWLDataRange visit(OWLFacetRestriction node) {
            throw new IllegalStateException("Internal error: We shouldn't visit facet restrictions during normalization. ");
        }

        public OWLDataRange visit(OWLLiteral node) {
            throw new IllegalStateException("Internal error: We shouldn't visit typed literals during normalization. ");
        }
    }

    protected class ClassExpressionNormalizer
    implements OWLClassExpressionVisitorEx<OWLClassExpression> {
        protected final Collection<OWLClassExpression[]> m_newInclusions;
        protected final Collection<OWLDataRange[]> m_newDataRangeInclusions;
        protected final boolean[] m_alreadyExists;

        public ClassExpressionNormalizer(Collection<OWLClassExpression[]> newInclusions, Collection<OWLDataRange[]> newDataRangeInclusions) {
            this.m_newInclusions = newInclusions;
            this.m_newDataRangeInclusions = newDataRangeInclusions;
            this.m_alreadyExists = new boolean[1];
        }

        public OWLClassExpression visit(OWLClass object) {
            return object;
        }

        public OWLClassExpression visit(OWLObjectIntersectionOf object) {
            OWLClassExpression definition = OWLNormalization.this.getDefinitionFor(object, this.m_alreadyExists);
            if (!this.m_alreadyExists[0]) {
                for (OWLClassExpression description : object.getOperands()) {
                    this.m_newInclusions.add(new OWLClassExpression[]{OWLNormalization.this.negative(definition), description});
                }
            }
            return definition;
        }

        public OWLClassExpression visit(OWLObjectUnionOf object) {
            throw new IllegalStateException("OR should be broken down at the outermost level");
        }

        public OWLClassExpression visit(OWLObjectComplementOf object) {
            if (OWLNormalization.isNominal(object.getOperand())) {
                OWLObjectOneOf objectOneOf = (OWLObjectOneOf)object.getOperand();
                OWLClass definition = OWLNormalization.this.getDefinitionForNegativeNominal(objectOneOf, this.m_alreadyExists);
                if (!this.m_alreadyExists[0]) {
                    for (OWLIndividual individual : objectOneOf.getIndividuals()) {
                        OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLClassAssertionAxiom(definition, individual));
                    }
                }
                return OWLNormalization.this.m_factory.getOWLObjectComplementOf(definition);
            }
            return object;
        }

        public OWLClassExpression visit(OWLObjectOneOf object) {
            for (OWLIndividual ind : object.getIndividuals()) {
                if (!ind.isAnonymous()) continue;
                throw new IllegalArgumentException("Error: The class expression " + object + " contains anonymous individuals, which is not allowed in OWL 2 (erratum in first OWL 2 spec, to be fixed with next publication of minor corrections). ");
            }
            return object;
        }

        public OWLClassExpression visit(OWLObjectSomeValuesFrom object) {
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(object.getProperty().getNamedProperty());
            OWLClassExpression filler = object.getFiller();
            if (OWLNormalization.isSimple(filler) || OWLNormalization.isNominal(filler)) {
                return object;
            }
            OWLClassExpression definition = OWLNormalization.this.getDefinitionFor(filler, this.m_alreadyExists);
            if (!this.m_alreadyExists[0]) {
                this.m_newInclusions.add(new OWLClassExpression[]{OWLNormalization.this.negative(definition), filler});
            }
            return OWLNormalization.this.m_factory.getOWLObjectSomeValuesFrom(object.getProperty(), definition);
        }

        public OWLClassExpression visit(OWLObjectAllValuesFrom object) {
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(object.getProperty().getNamedProperty());
            OWLClassExpression filler = object.getFiller();
            if (OWLNormalization.isSimple(filler) || OWLNormalization.isNominal(filler) || OWLNormalization.isNegatedOneNominal(filler)) {
                return object;
            }
            OWLClassExpression definition = OWLNormalization.this.getDefinitionFor(filler, this.m_alreadyExists);
            if (!this.m_alreadyExists[0]) {
                this.m_newInclusions.add(new OWLClassExpression[]{OWLNormalization.this.negative(definition), filler});
            }
            return OWLNormalization.this.m_factory.getOWLObjectAllValuesFrom(object.getProperty(), definition);
        }

        public OWLClassExpression visit(OWLObjectHasValue object) {
            throw new IllegalStateException("Internal error: object value restrictions should have been simplified.");
        }

        public OWLClassExpression visit(OWLObjectHasSelf object) {
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(object.getProperty().getNamedProperty());
            return object;
        }

        public OWLClassExpression visit(OWLObjectMinCardinality object) {
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(object.getProperty().getNamedProperty());
            OWLClassExpression filler = object.getFiller();
            if (OWLNormalization.isSimple(filler)) {
                return object;
            }
            OWLClassExpression definition = OWLNormalization.this.getDefinitionFor(filler, this.m_alreadyExists);
            if (!this.m_alreadyExists[0]) {
                this.m_newInclusions.add(new OWLClassExpression[]{OWLNormalization.this.negative(definition), filler});
            }
            return OWLNormalization.this.m_factory.getOWLObjectMinCardinality(object.getCardinality(), object.getProperty(), definition);
        }

        public OWLClassExpression visit(OWLObjectMaxCardinality object) {
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(object.getProperty().getNamedProperty());
            OWLClassExpression filler = object.getFiller();
            if (OWLNormalization.isSimple(filler)) {
                return object;
            }
            OWLClassExpression complementDescription = OWLNormalization.this.m_expressionManager.getComplementNNF(filler);
            OWLClassExpression definition = OWLNormalization.this.getDefinitionFor(complementDescription, this.m_alreadyExists);
            if (!this.m_alreadyExists[0]) {
                this.m_newInclusions.add(new OWLClassExpression[]{OWLNormalization.this.negative(definition), complementDescription});
            }
            return OWLNormalization.this.m_factory.getOWLObjectMaxCardinality(object.getCardinality(), object.getProperty(), OWLNormalization.this.m_expressionManager.getComplementNNF(definition));
        }

        public OWLClassExpression visit(OWLObjectExactCardinality object) {
            throw new IllegalStateException("Internal error: exact object cardinality restrictions should have been simplified.");
        }

        public OWLClassExpression visit(OWLDataSomeValuesFrom object) {
            OWLDataRange filler = object.getFiller();
            OWLDataPropertyExpression prop = object.getProperty();
            if (prop.isOWLTopDataProperty()) {
                this.throwInvalidTopDPUseError(object);
            }
            if (OWLNormalization.isLiteral(filler)) {
                return OWLNormalization.this.m_factory.getOWLDataSomeValuesFrom(object.getProperty(), filler);
            }
            OWLDatatype definition = OWLNormalization.this.getDefinitionFor(filler, this.m_alreadyExists);
            if (!this.m_alreadyExists[0]) {
                this.m_newDataRangeInclusions.add(new OWLDataRange[]{OWLNormalization.this.negative(definition), filler});
            }
            return OWLNormalization.this.m_factory.getOWLDataSomeValuesFrom(object.getProperty(), definition);
        }

        public OWLClassExpression visit(OWLDataAllValuesFrom object) {
            OWLDataRange filler = object.getFiller();
            OWLDataPropertyExpression prop = object.getProperty();
            if (prop.isOWLTopDataProperty()) {
                this.throwInvalidTopDPUseError(object);
            }
            if (OWLNormalization.isLiteral(filler)) {
                return OWLNormalization.this.m_factory.getOWLDataAllValuesFrom(prop, filler);
            }
            OWLDatatype definition = OWLNormalization.this.getDefinitionFor(filler, this.m_alreadyExists);
            if (!this.m_alreadyExists[0]) {
                this.m_newDataRangeInclusions.add(new OWLDataRange[]{OWLNormalization.this.negative(definition), filler});
            }
            return OWLNormalization.this.m_factory.getOWLDataAllValuesFrom(prop, definition);
        }

        protected void throwInvalidTopDPUseError(OWLClassExpression ex) {
            throw new IllegalArgumentException("Error: In OWL 2 DL, owl:topDataProperty is only allowed to occur in the super property position of SubDataPropertyOf axioms, but the ontology contains an axiom with the class expression " + ex + " that violates this restriction.");
        }

        public OWLClassExpression visit(OWLDataHasValue object) {
            throw new IllegalStateException("Internal error: data value restrictions should have been simplified.");
        }

        public OWLClassExpression visit(OWLDataMinCardinality object) {
            OWLDataRange filler = object.getFiller();
            OWLDataPropertyExpression prop = object.getProperty();
            if (prop.isOWLTopDataProperty()) {
                this.throwInvalidTopDPUseError(object);
            }
            if (OWLNormalization.isLiteral(filler)) {
                return OWLNormalization.this.m_factory.getOWLDataMinCardinality(object.getCardinality(), prop, filler);
            }
            OWLDatatype definition = OWLNormalization.this.getDefinitionFor(filler, this.m_alreadyExists);
            if (!this.m_alreadyExists[0]) {
                this.m_newDataRangeInclusions.add(new OWLDataRange[]{OWLNormalization.this.negative(definition), filler});
            }
            return OWLNormalization.this.m_factory.getOWLDataMinCardinality(object.getCardinality(), prop, definition);
        }

        public OWLClassExpression visit(OWLDataMaxCardinality object) {
            OWLDataRange filler = object.getFiller();
            OWLDataPropertyExpression prop = object.getProperty();
            if (prop.isOWLTopDataProperty()) {
                this.throwInvalidTopDPUseError(object);
            }
            if (OWLNormalization.isLiteral(filler)) {
                return OWLNormalization.this.m_factory.getOWLDataMaxCardinality(object.getCardinality(), prop, filler);
            }
            OWLDataRange complementDescription = OWLNormalization.this.m_expressionManager.getComplementNNF(filler);
            OWLDatatype definition = OWLNormalization.this.getDefinitionFor(complementDescription, this.m_alreadyExists);
            if (!this.m_alreadyExists[0]) {
                this.m_newDataRangeInclusions.add(new OWLDataRange[]{OWLNormalization.this.negative(definition), filler});
            }
            return OWLNormalization.this.m_factory.getOWLDataMaxCardinality(object.getCardinality(), prop, OWLNormalization.this.m_expressionManager.getComplementNNF(definition));
        }

        public OWLClassExpression visit(OWLDataExactCardinality object) {
            throw new IllegalStateException("Internal error: exact data cardinality restrictions should have been simplified.");
        }
    }

    protected class AxiomVisitor
    implements OWLAxiomVisitor {
        protected final List<OWLClassExpression[]> m_classExpressionInclusionsAsDisjunctions = new ArrayList<OWLClassExpression[]>();
        protected final List<OWLDataRange[]> m_dataRangeInclusionsAsDisjunctions = new ArrayList<OWLDataRange[]>();
        protected final Collection<SWRLRule> m_rules = new HashSet<SWRLRule>();
        protected final boolean[] m_alreadyExists = new boolean[1];

        public void visit(OWLDeclarationAxiom axiom) {
        }

        public void visit(OWLAnnotationAssertionAxiom axiom) {
        }

        public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {
        }

        public void visit(OWLAnnotationPropertyDomainAxiom axiom) {
        }

        public void visit(OWLAnnotationPropertyRangeAxiom axiom) {
        }

        public void visit(OWLSubClassOfAxiom axiom) {
            this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{OWLNormalization.this.negative(axiom.getSubClass()), OWLNormalization.this.positive(axiom.getSuperClass())});
        }

        public void visit(OWLEquivalentClassesAxiom axiom) {
            if (axiom.getClassExpressions().size() > 1) {
                OWLClassExpression first;
                Iterator iterator = axiom.getClassExpressions().iterator();
                OWLClassExpression last = first = (OWLClassExpression)iterator.next();
                while (iterator.hasNext()) {
                    OWLClassExpression next = (OWLClassExpression)iterator.next();
                    this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{OWLNormalization.this.negative(last), OWLNormalization.this.positive(next)});
                    last = next;
                }
                this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{OWLNormalization.this.negative(last), OWLNormalization.this.positive(first)});
            }
        }

        public void visit(OWLDisjointClassesAxiom axiom) {
            int i;
            if (axiom.getClassExpressions().size() <= 1) {
                throw new IllegalArgumentException("Error: Parsed " + axiom + ". A DisjointClasses axiom in OWL 2 DL must have at least two classes as parameters. ");
            }
            OWLClassExpression[] descriptions = new OWLClassExpression[axiom.getClassExpressions().size()];
            axiom.getClassExpressions().toArray(descriptions);
            for (i = 0; i < descriptions.length; ++i) {
                descriptions[i] = OWLNormalization.this.m_expressionManager.getComplementNNF(descriptions[i]);
            }
            for (i = 0; i < descriptions.length; ++i) {
                for (int j = i + 1; j < descriptions.length; ++j) {
                    this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{descriptions[i], descriptions[j]});
                }
            }
        }

        public void visit(OWLDisjointUnionAxiom axiom) {
            int i;
            HashSet<OWLClassExpression> inclusion = new HashSet<OWLClassExpression>(axiom.getClassExpressions());
            inclusion.add(OWLNormalization.this.m_expressionManager.getComplementNNF(axiom.getOWLClass()));
            OWLClassExpression[] inclusionArray = new OWLClassExpression[axiom.getClassExpressions().size() + 1];
            inclusion.toArray(inclusionArray);
            this.m_classExpressionInclusionsAsDisjunctions.add(inclusionArray);
            for (OWLClassExpression description : axiom.getClassExpressions()) {
                this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{OWLNormalization.this.negative(description), axiom.getOWLClass()});
            }
            OWLClassExpression[] descriptions = new OWLClassExpression[axiom.getClassExpressions().size()];
            axiom.getClassExpressions().toArray(descriptions);
            for (i = 0; i < descriptions.length; ++i) {
                descriptions[i] = OWLNormalization.this.m_expressionManager.getComplementNNF(descriptions[i]);
            }
            for (i = 0; i < descriptions.length; ++i) {
                for (int j = i + 1; j < descriptions.length; ++j) {
                    this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{descriptions[i], descriptions[j]});
                }
            }
        }

        public void visit(OWLSubObjectPropertyOfAxiom axiom) {
            if (!axiom.getSubProperty().isOWLBottomObjectProperty() && !axiom.getSuperProperty().isOWLTopObjectProperty()) {
                OWLNormalization.this.addInclusion(axiom.getSubProperty(), axiom.getSuperProperty());
            }
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getSubProperty().getNamedProperty());
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getSuperProperty().getNamedProperty());
        }

        public void visit(OWLSubPropertyChainOfAxiom axiom) {
            List<OWLObjectPropertyExpression> subPropertyChain = axiom.getPropertyChain();
            if (!this.containsBottomObjectProperty(subPropertyChain) && !axiom.getSuperProperty().isOWLTopObjectProperty()) {
                OWLObjectPropertyExpression superObjectPropertyExpression = axiom.getSuperProperty();
                if (subPropertyChain.size() == 1) {
                    OWLNormalization.this.addInclusion(subPropertyChain.get(0), superObjectPropertyExpression);
                } else if (subPropertyChain.size() == 2 && subPropertyChain.get(0).equals(superObjectPropertyExpression) && subPropertyChain.get(1).equals(superObjectPropertyExpression)) {
                    OWLNormalization.this.makeTransitive(axiom.getSuperProperty());
                } else {
                    if (subPropertyChain.isEmpty()) {
                        throw new IllegalArgumentException("Error: In OWL 2 DL, an empty property chain in property chain axioms is not allowed, but the ontology contains an axiom that the empty chain is a subproperty of " + superObjectPropertyExpression + ".");
                    }
                    OWLObjectPropertyExpression[] subObjectProperties = new OWLObjectPropertyExpression[subPropertyChain.size()];
                    subPropertyChain.toArray(subObjectProperties);
                    OWLNormalization.this.addInclusion(subObjectProperties, superObjectPropertyExpression);
                }
            }
            for (OWLObjectPropertyExpression objectPropertyExpression : subPropertyChain) {
                OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(objectPropertyExpression.getNamedProperty());
            }
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getSuperProperty().getNamedProperty());
        }

        protected boolean containsBottomObjectProperty(List<OWLObjectPropertyExpression> properties) {
            for (OWLObjectPropertyExpression property : properties) {
                if (!property.isOWLBottomObjectProperty()) continue;
                return true;
            }
            return false;
        }

        public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
            Set<OWLObjectPropertyExpression> objectPropertyExpressions = axiom.getProperties();
            if (objectPropertyExpressions.size() > 1) {
                OWLObjectPropertyExpression first;
                Iterator iterator = objectPropertyExpressions.iterator();
                OWLObjectPropertyExpression last = first = (OWLObjectPropertyExpression)iterator.next();
                while (iterator.hasNext()) {
                    OWLObjectPropertyExpression next = (OWLObjectPropertyExpression)iterator.next();
                    OWLNormalization.this.addInclusion(last, next);
                    last = next;
                }
                OWLNormalization.this.addInclusion(last, first);
            }
            for (OWLObjectPropertyExpression objectPropertyExpression : objectPropertyExpressions) {
                OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(objectPropertyExpression.getNamedProperty());
            }
        }

        public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
            OWLObjectPropertyExpression[] objectPropertyExpressions = new OWLObjectPropertyExpression[axiom.getProperties().size()];
            axiom.getProperties().toArray(objectPropertyExpressions);
            for (int i = 0; i < objectPropertyExpressions.length; ++i) {
                objectPropertyExpressions[i] = objectPropertyExpressions[i];
                OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(objectPropertyExpressions[i].getNamedProperty());
            }
            OWLNormalization.this.m_axioms.m_disjointObjectProperties.add(objectPropertyExpressions);
        }

        public void visit(OWLInverseObjectPropertiesAxiom axiom) {
            OWLObjectPropertyExpression first = axiom.getFirstProperty();
            OWLObjectPropertyExpression second = axiom.getSecondProperty();
            OWLNormalization.this.m_axioms.m_explicitInverses.computeIfAbsent(first, x -> new HashSet()).add(second);
            OWLNormalization.this.m_axioms.m_explicitInverses.computeIfAbsent(second, x -> new HashSet()).add(first);
            OWLNormalization.this.addInclusion(first, second.getInverseProperty());
            OWLNormalization.this.addInclusion(second, first.getInverseProperty());
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(first.getNamedProperty());
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(second.getNamedProperty());
        }

        public void visit(OWLObjectPropertyDomainAxiom axiom) {
            OWLObjectAllValuesFrom allPropertyNohting = OWLNormalization.this.m_factory.getOWLObjectAllValuesFrom(axiom.getProperty(), OWLNormalization.this.m_factory.getOWLNothing());
            this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{OWLNormalization.this.positive(axiom.getDomain()), allPropertyNohting});
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
        }

        public void visit(OWLObjectPropertyRangeAxiom axiom) {
            OWLObjectAllValuesFrom allPropertyRange = OWLNormalization.this.m_factory.getOWLObjectAllValuesFrom(axiom.getProperty(), OWLNormalization.this.positive(axiom.getRange()));
            this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{allPropertyRange});
        }

        public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
            this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{OWLNormalization.this.m_factory.getOWLObjectMaxCardinality(1, axiom.getProperty())});
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
        }

        public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
            this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{OWLNormalization.this.m_factory.getOWLObjectMaxCardinality(1, axiom.getProperty().getInverseProperty())});
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
        }

        public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
            OWLNormalization.this.makeReflexive(axiom.getProperty());
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
        }

        public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
            OWLNormalization.this.makeIrreflexive(axiom.getProperty());
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
        }

        public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
            OWLObjectPropertyExpression objectProperty = axiom.getProperty();
            OWLNormalization.this.addInclusion(objectProperty, objectProperty.getInverseProperty());
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
        }

        public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
            OWLNormalization.this.makeAsymmetric(axiom.getProperty());
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
        }

        public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
            OWLNormalization.this.makeTransitive(axiom.getProperty());
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
        }

        public void visit(OWLSubDataPropertyOfAxiom axiom) {
            OWLDataPropertyExpression subDataProperty = axiom.getSubProperty();
            this.checkTopDataPropertyUse(subDataProperty, axiom);
            OWLDataPropertyExpression superDataProperty = axiom.getSuperProperty();
            if (!subDataProperty.isOWLBottomDataProperty() && !superDataProperty.isOWLTopDataProperty()) {
                OWLNormalization.this.addInclusion(subDataProperty, superDataProperty);
            }
        }

        public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
            for (OWLDataPropertyExpression dataPropertyExpression : axiom.getProperties()) {
                this.checkTopDataPropertyUse(dataPropertyExpression, axiom);
            }
            if (axiom.getProperties().size() > 1) {
                OWLDataPropertyExpression first;
                Iterator iterator = axiom.getProperties().iterator();
                OWLDataPropertyExpression last = first = (OWLDataPropertyExpression)iterator.next();
                while (iterator.hasNext()) {
                    OWLDataPropertyExpression next = (OWLDataPropertyExpression)iterator.next();
                    OWLNormalization.this.addInclusion(last, next);
                    last = next;
                }
                OWLNormalization.this.addInclusion(last, first);
            }
        }

        public void visit(OWLDisjointDataPropertiesAxiom axiom) {
            OWLDataPropertyExpression[] dataProperties = new OWLDataPropertyExpression[axiom.getProperties().size()];
            axiom.getProperties().toArray(dataProperties);
            for (OWLDataPropertyExpression dataProperty : dataProperties) {
                this.checkTopDataPropertyUse(dataProperty, axiom);
            }
            OWLNormalization.this.m_axioms.m_disjointDataProperties.add(dataProperties);
        }

        public void visit(OWLDataPropertyDomainAxiom axiom) {
            OWLDataPropertyExpression dataProperty = axiom.getProperty();
            this.checkTopDataPropertyUse(dataProperty, axiom);
            OWLDataComplementOf dataNothing = OWLNormalization.this.m_factory.getOWLDataComplementOf(OWLNormalization.this.m_factory.getTopDatatype());
            OWLDataAllValuesFrom allPropertyDataNothing = OWLNormalization.this.m_factory.getOWLDataAllValuesFrom(dataProperty, dataNothing);
            this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{OWLNormalization.this.positive(axiom.getDomain()), allPropertyDataNothing});
        }

        public void visit(OWLDataPropertyRangeAxiom axiom) {
            OWLDataPropertyExpression dataProperty = axiom.getProperty();
            this.checkTopDataPropertyUse(dataProperty, axiom);
            OWLDataAllValuesFrom allPropertyRange = OWLNormalization.this.m_factory.getOWLDataAllValuesFrom(dataProperty, OWLNormalization.this.positive(axiom.getRange()));
            this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{allPropertyRange});
        }

        public void visit(OWLFunctionalDataPropertyAxiom axiom) {
            OWLDataPropertyExpression dataProperty = axiom.getProperty();
            this.checkTopDataPropertyUse(dataProperty, axiom);
            this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{OWLNormalization.this.m_factory.getOWLDataMaxCardinality(1, dataProperty)});
        }

        protected void checkTopDataPropertyUse(OWLDataPropertyExpression dataPropertyExpression, OWLAxiom axiom) {
            if (dataPropertyExpression.isOWLTopDataProperty()) {
                throw new IllegalArgumentException("Error: In OWL 2 DL, owl:topDataProperty is only allowed to occur in the super property position of SubDataPropertyOf axioms, but the ontology contains an axiom " + axiom + " that violates this condition.");
            }
        }

        public void visit(OWLSameIndividualAxiom axiom) {
            if (axiom.containsAnonymousIndividuals()) {
                throw new IllegalArgumentException("The axiom " + axiom + " contains anonymous individuals, which is not allowed in OWL 2. ");
            }
            OWLNormalization.this.addFact(axiom);
        }

        public void visit(OWLDifferentIndividualsAxiom axiom) {
            if (axiom.containsAnonymousIndividuals()) {
                throw new IllegalArgumentException("The axiom " + axiom + " contains anonymous individuals, which is not allowed in OWL 2. ");
            }
            OWLNormalization.this.addFact(axiom);
        }

        public void visit(OWLClassAssertionAxiom axiom) {
            OWLDataSomeValuesFrom someValuesFrom;
            OWLDataOneOf oneOf;
            OWLDataRange dataRange;
            OWLClassExpression classExpression = axiom.getClassExpression();
            if (classExpression instanceof OWLDataHasValue) {
                OWLDataHasValue hasValue = (OWLDataHasValue)classExpression;
                OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLDataPropertyAssertionAxiom(hasValue.getProperty(), axiom.getIndividual(), hasValue.getFiller()));
                return;
            }
            if (classExpression instanceof OWLDataSomeValuesFrom && (dataRange = (someValuesFrom = (OWLDataSomeValuesFrom)classExpression).getFiller()) instanceof OWLDataOneOf && (oneOf = (OWLDataOneOf)dataRange).getValues().size() == 1) {
                OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLDataPropertyAssertionAxiom(someValuesFrom.getProperty(), axiom.getIndividual(), oneOf.getValues().iterator().next()));
                return;
            }
            if (!OWLNormalization.isSimple(classExpression = OWLNormalization.this.positive(classExpression))) {
                OWLClassExpression definition = OWLNormalization.this.getDefinitionFor(classExpression, this.m_alreadyExists);
                if (!this.m_alreadyExists[0]) {
                    this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{OWLNormalization.this.negative(definition), classExpression});
                }
                classExpression = definition;
            }
            OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLClassAssertionAxiom(classExpression, axiom.getIndividual()));
        }

        public void visit(OWLObjectPropertyAssertionAxiom axiom) {
            OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLObjectPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject()));
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
        }

        public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
            if (axiom.containsAnonymousIndividuals()) {
                throw new IllegalArgumentException("The axiom " + axiom + " contains anonymous individuals, which is not allowed in OWL 2 DL. ");
            }
            OWLNormalization.this.addFact(OWLNormalization.this.m_factory.getOWLNegativeObjectPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject()));
            OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
        }

        public void visit(OWLDataPropertyAssertionAxiom axiom) {
            this.checkTopDataPropertyUse(axiom.getProperty(), axiom);
            OWLNormalization.this.addFact(axiom);
        }

        public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
            this.checkTopDataPropertyUse(axiom.getProperty(), axiom);
            if (axiom.containsAnonymousIndividuals()) {
                throw new IllegalArgumentException("The axiom " + axiom + " contains anonymous individuals, which is not allowed in OWL 2 DL. ");
            }
            OWLNormalization.this.addFact(axiom);
        }

        public void visit(OWLDatatypeDefinitionAxiom axiom) {
            OWLNormalization.this.m_axioms.m_definedDatatypesIRIs.add(axiom.getDatatype().getIRI().toString());
            this.m_dataRangeInclusionsAsDisjunctions.add(new OWLDataRange[]{OWLNormalization.this.negative(axiom.getDatatype()), OWLNormalization.this.positive(axiom.getDataRange())});
            this.m_dataRangeInclusionsAsDisjunctions.add(new OWLDataRange[]{OWLNormalization.this.negative(axiom.getDataRange()), OWLNormalization.this.positive(axiom.getDatatype())});
        }

        public void visit(OWLHasKeyAxiom axiom) {
            for (OWLDataPropertyExpression dataPropertyExpression : axiom.getDataPropertyExpressions()) {
                this.checkTopDataPropertyUse(dataPropertyExpression, axiom);
            }
            OWLClassExpression description = OWLNormalization.this.positive(axiom.getClassExpression());
            if (!OWLNormalization.isSimple(description)) {
                OWLClassExpression definition = OWLNormalization.this.getDefinitionFor(description, this.m_alreadyExists);
                if (!this.m_alreadyExists[0]) {
                    this.m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{OWLNormalization.this.negative(definition), description});
                }
                description = definition;
            }
            OWLNormalization.this.m_axioms.m_hasKeys.add(OWLNormalization.this.m_factory.getOWLHasKeyAxiom(description, axiom.getPropertyExpressions()));
            for (OWLObjectPropertyExpression objectPropertyExpression : axiom.getObjectPropertyExpressions()) {
                OWLNormalization.this.m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(objectPropertyExpression.getNamedProperty());
            }
        }
        
        @Override
		public void visit(OWLMetamodellingAxiom axiom) {
			// TODO Auto-generated method stub
        	OWLNormalization.this.m_axioms.m_metamodellingAxioms.add(axiom);
		}
        
        @Override
		public void visit(OWLMetaRuleAxiom axiom) {
			// TODO Auto-generated method stub
        	OWLNormalization.this.m_axioms.m_metaRuleAxioms.add(axiom);
		}

        public void visit(SWRLRule rule) {
            for (Object atom : rule.getBody()) {
                if (!(atom instanceof SWRLDataPropertyAtom)) continue;
                this.checkTopDataPropertyUse(((SWRLDataPropertyAtom)atom).getPredicate(), rule);
            }
            for (Object atom : rule.getHead()) {
                if (!(atom instanceof SWRLDataPropertyAtom)) continue;
                this.checkTopDataPropertyUse(((SWRLDataPropertyAtom)atom).getPredicate(), rule);
            }
            if (rule.getBody().isEmpty()) {
                Rule2FactConverter r2fConverter = new Rule2FactConverter(this.m_classExpressionInclusionsAsDisjunctions);
                for (SWRLAtom at : rule.getHead()) {
                    at.accept(r2fConverter);
                }
            } else {
                this.m_rules.add(rule);
            }
        }
    }

}

