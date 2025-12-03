package org.semanticweb.HermiT.structural;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLMetaRuleAxiom;
import org.semanticweb.owlapi.model.OWLMetamodellingAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.SWRLAtom;

public class OWLAxioms {
    final Set<OWLClass> m_classes = new HashSet<OWLClass>();
    final Set<OWLObjectProperty> m_objectProperties = new HashSet<OWLObjectProperty>();
    final Set<OWLObjectProperty> m_objectPropertiesOccurringInOWLAxioms = new HashSet<OWLObjectProperty>();
    final Set<OWLObjectPropertyExpression> m_complexObjectPropertyExpressions = new HashSet<OWLObjectPropertyExpression>();
    final Set<OWLDataProperty> m_dataProperties = new HashSet<OWLDataProperty>();
    final Set<OWLNamedIndividual> m_namedIndividuals = new HashSet<OWLNamedIndividual>();
    final Collection<OWLClassExpression[]> m_conceptInclusions = new ArrayList<OWLClassExpression[]>();
    final Collection<OWLDataRange[]> m_dataRangeInclusions = new ArrayList<OWLDataRange[]>();
    final Collection<OWLObjectPropertyExpression[]> m_simpleObjectPropertyInclusions = new ArrayList<OWLObjectPropertyExpression[]>();
    final Collection<ComplexObjectPropertyInclusion> m_complexObjectPropertyInclusions = new ArrayList<ComplexObjectPropertyInclusion>();
    final Collection<OWLObjectPropertyExpression[]> m_disjointObjectProperties = new ArrayList<OWLObjectPropertyExpression[]>();
    final Set<OWLObjectPropertyExpression> m_reflexiveObjectProperties = new HashSet<OWLObjectPropertyExpression>();
    final Set<OWLObjectPropertyExpression> m_irreflexiveObjectProperties = new HashSet<OWLObjectPropertyExpression>();
    final Set<OWLObjectPropertyExpression> m_asymmetricObjectProperties = new HashSet<OWLObjectPropertyExpression>();
    final Collection<OWLDataPropertyExpression[]> m_dataPropertyInclusions = new ArrayList<OWLDataPropertyExpression[]>();
    final Collection<OWLDataPropertyExpression[]> m_disjointDataProperties = new ArrayList<OWLDataPropertyExpression[]>();
    final Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> m_explicitInverses = new HashMap<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>>();
    final Collection<OWLIndividualAxiom> m_facts = new HashSet<OWLIndividualAxiom>();
    final Set<OWLHasKeyAxiom> m_hasKeys = new HashSet<OWLHasKeyAxiom>();
    public final Set<String> m_definedDatatypesIRIs = new HashSet<String>();
    final Collection<DisjunctiveRule> m_rules = new HashSet<DisjunctiveRule>();
    final Set<OWLMetamodellingAxiom> m_metamodellingAxioms = new HashSet<OWLMetamodellingAxiom>();
    final Set<OWLMetaRuleAxiom> m_metaRuleAxioms = new HashSet<OWLMetaRuleAxiom>();

    static class DisjunctiveRule {
        public final SWRLAtom[] m_body;
        public final SWRLAtom[] m_head;

        public DisjunctiveRule(SWRLAtom[] body, SWRLAtom[] head) {
            this.m_body = body;
            this.m_head = head;
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            boolean first = true;
            for (SWRLAtom atom : this.m_body) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(" /\\ ");
                }
                buffer.append(atom.toString());
            }
            buffer.append(" -: ");
            first = true;
            for (SWRLAtom atom : this.m_head) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(" \\/ ");
                }
                buffer.append(atom.toString());
            }
            return buffer.toString();
        }
    }

    static class ComplexObjectPropertyInclusion {
        public final OWLObjectPropertyExpression[] m_subObjectProperties;
        public final OWLObjectPropertyExpression m_superObjectProperty;

        public ComplexObjectPropertyInclusion(OWLObjectPropertyExpression[] subObjectProperties, OWLObjectPropertyExpression superObjectPropery) {
            this.m_subObjectProperties = subObjectProperties;
            this.m_superObjectProperty = superObjectPropery;
        }

        public ComplexObjectPropertyInclusion(OWLObjectPropertyExpression transitiveObjectProperty) {
            this.m_subObjectProperties = new OWLObjectPropertyExpression[]{transitiveObjectProperty, transitiveObjectProperty};
            this.m_superObjectProperty = transitiveObjectProperty;
        }
    }

}

