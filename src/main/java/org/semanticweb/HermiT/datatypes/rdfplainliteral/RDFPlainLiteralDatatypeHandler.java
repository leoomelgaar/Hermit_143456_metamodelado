package org.semanticweb.HermiT.datatypes.rdfplainliteral;

import dk.brics.automaton.Automaton;

import java.util.*;

import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.datatypes.DatatypeHandler;
import org.semanticweb.HermiT.datatypes.MalformedLiteralException;
import org.semanticweb.HermiT.datatypes.UnsupportedFacetException;
import org.semanticweb.HermiT.datatypes.ValueSpaceSubset;
import org.semanticweb.HermiT.model.Constant;
import org.semanticweb.HermiT.model.DatatypeRestriction;

public class RDFPlainLiteralDatatypeHandler
implements DatatypeHandler {
    protected static final String XSD_NS = Prefixes.s_semanticWebPrefixes.get("xsd:");
    protected static final String RDF_NS = Prefixes.s_semanticWebPrefixes.get("rdf:");
    protected static final Map<String, ValueSpaceSubset> s_subsetsByDatatype = new HashMap<String, ValueSpaceSubset>();
    protected static final ValueSpaceSubset EMPTY_SUBSET;
    protected static final Map<String, Set<String>> s_datatypeSupersets;

    protected static void registerPatternDatatype(String datatypeURI) {
        Automaton automaton = RDFPlainLiteralPatternValueSpaceSubset.getDatatypeAutomaton(datatypeURI);
        s_subsetsByDatatype.put(datatypeURI, new RDFPlainLiteralPatternValueSpaceSubset(automaton));
    }

    @Override
    public Set<String> getManagedDatatypeURIs() {
        return s_subsetsByDatatype.keySet();
    }

    @Override
    public Object parseLiteral(String lexicalForm, String datatypeURI) throws MalformedLiteralException {
        Object dataValue;
        assert (s_subsetsByDatatype.containsKey(datatypeURI));
        if ((RDF_NS + "PlainLiteral").equals(datatypeURI)) {
            int lastAt = lexicalForm.lastIndexOf(64);
            if (lastAt == -1) {
                throw new MalformedLiteralException(lexicalForm, datatypeURI);
            }
            String string = lexicalForm.substring(0, lastAt);
            String languageTag = lexicalForm.substring(lastAt + 1);
            dataValue = languageTag.length() == 0 ? string : new RDFPlainLiteralDataValue(string, languageTag);
        } else {
            dataValue = lexicalForm;
        }
        if (s_subsetsByDatatype.get(datatypeURI).containsDataValue(dataValue)) {
            return dataValue;
        }
        throw new MalformedLiteralException(lexicalForm, datatypeURI);
    }

    @Override
    public void validateDatatypeRestriction(DatatypeRestriction datatypeRestriction) throws UnsupportedFacetException {
        String datatypeURI = datatypeRestriction.getDatatypeURI();
        assert (s_subsetsByDatatype.containsKey(datatypeURI));
        for (int index = datatypeRestriction.getNumberOfFacetRestrictions() - 1; index >= 0; --index) {
            String facetURI = datatypeRestriction.getFacetURI(index);
            Constant facetValue = datatypeRestriction.getFacetValue(index);
            Object facetDataValue = facetValue.getDataValue();
            if ((XSD_NS + "minLength").equals(facetURI) || (XSD_NS + "maxLength").equals(facetURI) || (XSD_NS + "length").equals(facetURI)) {
                if (facetDataValue instanceof Integer) {
                    int value = (Integer)facetDataValue;
                    if (value >= 0 && value != Integer.MAX_VALUE) continue;
                    throw new UnsupportedFacetException("The datatype restriction " + this + " cannot be handled. The facet with URI '" + facetURI + "' does not support integer " + value + " as value. " + (value < 0 ? "The value should not be negative. " : "The value is outside of the supported integer range, i.e., it is larger than 2147483647"));
                }
                throw new UnsupportedFacetException("The datatype rdf:PlainLiteral accepts only integers as facet values for the facet with URI '" + facetURI + "', but in the ontology we have a datatype restriction " + this + ". The value '" + facetValue + "' does not seem to be an integer.");
            }
            if ((XSD_NS + "pattern").equals(facetURI)) {
                if (facetDataValue instanceof String) {
                    String pattern = (String)facetDataValue;
                    if (RDFPlainLiteralPatternValueSpaceSubset.isValidPattern(pattern)) continue;
                    throw new UnsupportedFacetException("String '" + pattern + "' in the datatype restriction " + this + " is not a valid regular expression.");
                }
                throw new UnsupportedFacetException("The facet with URI '" + facetURI + "' supports only strings as values, but '" + facetValue + "' in the restriction " + this + " does not seem to be a string. It is an instance of the class " + facetValue.getClass() + ". ");
            }
            if ((RDF_NS + "langRange").equals(facetURI)) {
                if (facetDataValue instanceof String) continue;
                throw new UnsupportedFacetException("The facet with URI '" + facetURI + "' supports only strings as values, but '" + facetValue + "' in the restriction " + this + " does not seem to be a string. It is an instance of the class " + facetValue.getClass() + ". ");
            }
            throw new UnsupportedFacetException("Facet with URI '" + facetURI + "' is not supported on rdf:PlainLiteral; only xsd:minLength, xsd:maxLength, xsd:length, xsd:pattern, and rdf:langRange are supported, but the ontology contains the restriction: " + this);
        }
    }

    @Override
    public ValueSpaceSubset createValueSpaceSubset(DatatypeRestriction datatypeRestriction) {
        String datatypeURI = datatypeRestriction.getDatatypeURI();
        assert (s_subsetsByDatatype.containsKey(datatypeURI));
        if (datatypeRestriction.getNumberOfFacetRestrictions() == 0) {
            return s_subsetsByDatatype.get(datatypeURI);
        }
        if (this.needsAutomatons(datatypeRestriction)) {
            Automaton automaton = this.getAutomatonFor(datatypeRestriction);
            if (automaton == null) {
                return EMPTY_SUBSET;
            }
            return new RDFPlainLiteralPatternValueSpaceSubset(automaton);
        }
        RDFPlainLiteralLengthInterval[] intervals = this.getIntervalsFor(datatypeRestriction);
        if (intervals[0] == null && intervals[1] == null) {
            return EMPTY_SUBSET;
        }
        if (intervals[0] != null && intervals[1] != null) {
            return new RDFPlainLiteralLengthValueSpaceSubset(intervals[0], intervals[1]);
        }
        if (intervals[0] == null && intervals[1] != null) {
            return new RDFPlainLiteralLengthValueSpaceSubset(intervals[1]);
        }
        return new RDFPlainLiteralLengthValueSpaceSubset(intervals[0]);
    }

    @Override
    public ValueSpaceSubset conjoinWithDR(ValueSpaceSubset valueSpaceSubset, DatatypeRestriction datatypeRestriction) {
        String datatypeURI = datatypeRestriction.getDatatypeURI();
        assert (s_subsetsByDatatype.containsKey(datatypeURI));
        if (valueSpaceSubset == EMPTY_SUBSET) {
            return EMPTY_SUBSET;
        }
        if (valueSpaceSubset instanceof RDFPlainLiteralPatternValueSpaceSubset || this.needsAutomatons(datatypeRestriction)) {
            Automaton restrictionAutomaton = this.getAutomatonFor(datatypeRestriction);
            if (restrictionAutomaton == null) {
                return EMPTY_SUBSET;
            }
            Automaton valueSpaceSubsetAutomaton = this.getAutomatonFor(valueSpaceSubset);
            if (valueSpaceSubsetAutomaton == null) {
                return EMPTY_SUBSET;
            }
            Automaton intersection = valueSpaceSubsetAutomaton.intersection(restrictionAutomaton);
            if (intersection.isEmpty()) {
                return EMPTY_SUBSET;
            }
            return new RDFPlainLiteralPatternValueSpaceSubset(intersection);
        }
        RDFPlainLiteralLengthInterval[] intervals = this.getIntervalsFor(datatypeRestriction);
        if (intervals[0] == null && intervals[1] == null) {
            return EMPTY_SUBSET;
        }
        List<RDFPlainLiteralLengthInterval> oldIntervals = ((RDFPlainLiteralLengthValueSpaceSubset)valueSpaceSubset).m_intervals;
        ArrayList<RDFPlainLiteralLengthInterval> newIntervals = new ArrayList<RDFPlainLiteralLengthInterval>();
        for (int index = 0; index < oldIntervals.size(); ++index) {
            RDFPlainLiteralLengthInterval intersection;
            RDFPlainLiteralLengthInterval oldInterval = oldIntervals.get(index);
            if (intervals[0] != null && (intersection = oldInterval.intersectWith(intervals[0])) != null) {
                newIntervals.add(intersection);
            }
            if (intervals[1] == null || (intersection = oldInterval.intersectWith(intervals[1])) == null) continue;
            newIntervals.add(intersection);
        }
        if (newIntervals.isEmpty()) {
            return EMPTY_SUBSET;
        }
        return new RDFPlainLiteralLengthValueSpaceSubset(newIntervals);
    }

    @Override
    public ValueSpaceSubset conjoinWithDRNegation(ValueSpaceSubset valueSpaceSubset, DatatypeRestriction datatypeRestriction) {
        String datatypeURI = datatypeRestriction.getDatatypeURI();
        assert (s_subsetsByDatatype.containsKey(datatypeURI));
        if (valueSpaceSubset == EMPTY_SUBSET) {
            return EMPTY_SUBSET;
        }
        if (valueSpaceSubset instanceof RDFPlainLiteralPatternValueSpaceSubset || this.needsAutomatons(datatypeRestriction)) {
            Automaton restrictionAutomaton = this.getAutomatonFor(datatypeRestriction);
            if (restrictionAutomaton == null) {
                return valueSpaceSubset;
            }
            Automaton valueSpaceSubsetAutomaton = this.getAutomatonFor(valueSpaceSubset);
            if (valueSpaceSubsetAutomaton == null) {
                return EMPTY_SUBSET;
            }
            Automaton difference = valueSpaceSubsetAutomaton.minus(restrictionAutomaton);
            if (difference.isEmpty()) {
                return EMPTY_SUBSET;
            }
            return new RDFPlainLiteralPatternValueSpaceSubset(difference);
        }
        RDFPlainLiteralLengthInterval[] intervals = this.getIntervalsFor(datatypeRestriction);
        if (intervals[0] == null && intervals[1] == null) {
            return valueSpaceSubset;
        }
        ArrayList<RDFPlainLiteralLengthInterval> complementedIntervals = new ArrayList<RDFPlainLiteralLengthInterval>(4);
        if (intervals[0] != null) {
            if (intervals[0].m_minLength > 0) {
                complementedIntervals.add(new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.PRESENT, 0, intervals[0].m_minLength - 1));
            }
            if (intervals[0].m_maxLength < Integer.MAX_VALUE) {
                complementedIntervals.add(new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.PRESENT, intervals[0].m_maxLength + 1, Integer.MAX_VALUE));
            }
        } else {
            complementedIntervals.add(new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.PRESENT, 0, Integer.MAX_VALUE));
        }
        if (intervals[1] != null) {
            if (intervals[1].m_minLength > 0) {
                complementedIntervals.add(new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.ABSENT, 0, intervals[1].m_minLength - 1));
            }
            if (intervals[1].m_maxLength < Integer.MAX_VALUE) {
                complementedIntervals.add(new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.ABSENT, intervals[1].m_maxLength + 1, Integer.MAX_VALUE));
            }
        } else {
            complementedIntervals.add(new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.ABSENT, 0, Integer.MAX_VALUE));
        }
        List<RDFPlainLiteralLengthInterval> oldIntervals = ((RDFPlainLiteralLengthValueSpaceSubset)valueSpaceSubset).m_intervals;
        ArrayList<RDFPlainLiteralLengthInterval> newIntervals = new ArrayList<RDFPlainLiteralLengthInterval>();
        for (int index = 0; index < oldIntervals.size(); ++index) {
            RDFPlainLiteralLengthInterval oldInterval = oldIntervals.get(index);
            for (int complementedIndex = complementedIntervals.size() - 1; complementedIndex >= 0; --complementedIndex) {
                RDFPlainLiteralLengthInterval complementedInterval = complementedIntervals.get(complementedIndex);
                RDFPlainLiteralLengthInterval intersection = oldInterval.intersectWith(complementedInterval);
                if (intersection == null) continue;
                newIntervals.add(intersection);
            }
        }
        if (newIntervals.isEmpty()) {
            return EMPTY_SUBSET;
        }
        return new RDFPlainLiteralLengthValueSpaceSubset(newIntervals);
    }

    protected boolean needsAutomatons(DatatypeRestriction datatypeRestriction) {
        String datatypeURI = datatypeRestriction.getDatatypeURI();
        if (s_subsetsByDatatype.get(datatypeURI) instanceof RDFPlainLiteralLengthValueSpaceSubset) {
            for (int index = datatypeRestriction.getNumberOfFacetRestrictions() - 1; index >= 0; --index) {
                String facetURI = datatypeRestriction.getFacetURI(index);
                if (!(XSD_NS + "pattern").equals(facetURI) && !(RDF_NS + "langRange").equals(facetURI)) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    protected RDFPlainLiteralLengthInterval[] getIntervalsFor(DatatypeRestriction datatypeRestriction) {
        RDFPlainLiteralLengthInterval[] intervals = new RDFPlainLiteralLengthInterval[2];
        String datatypeURI = datatypeRestriction.getDatatypeURI();
        assert (s_subsetsByDatatype.get(datatypeURI) instanceof RDFPlainLiteralLengthValueSpaceSubset);
        int minLength = 0;
        int maxLength = Integer.MAX_VALUE;
        for (int index = datatypeRestriction.getNumberOfFacetRestrictions() - 1; index >= 0; --index) {
            String facetURI = datatypeRestriction.getFacetURI(index);
            assert ((XSD_NS + "minLength").equals(facetURI) || (XSD_NS + "maxLength").equals(facetURI) || (XSD_NS + "length").equals(facetURI));
            int facetDataValue = (Integer)datatypeRestriction.getFacetValue(index).getDataValue();
            if ((XSD_NS + "minLength").equals(facetURI)) {
                minLength = Math.max(minLength, facetDataValue);
                continue;
            }
            if ((XSD_NS + "maxLength").equals(facetURI)) {
                maxLength = Math.min(maxLength, facetDataValue);
                continue;
            }
            if (!(XSD_NS + "length").equals(facetURI)) continue;
            minLength = Math.max(minLength, facetDataValue);
            maxLength = Math.min(maxLength, facetDataValue);
        }
        if (minLength <= maxLength) {
            if ((RDF_NS + "PlainLiteral").equals(datatypeURI)) {
                intervals[0] = new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.PRESENT, minLength, maxLength);
                intervals[1] = new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.ABSENT, minLength, maxLength);
            } else if ((XSD_NS + "string").equals(datatypeURI)) {
                intervals[1] = new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.ABSENT, minLength, maxLength);
            }
        }
        return intervals;
    }

    protected Automaton getAutomatonFor(ValueSpaceSubset valueSpaceSubset) {
        if (valueSpaceSubset instanceof RDFPlainLiteralPatternValueSpaceSubset) {
            return ((RDFPlainLiteralPatternValueSpaceSubset)valueSpaceSubset).m_automaton;
        }
        return RDFPlainLiteralPatternValueSpaceSubset.toAutomaton((RDFPlainLiteralLengthValueSpaceSubset)valueSpaceSubset);
    }

    protected Automaton getAutomatonFor(DatatypeRestriction datatypeRestriction) {
        String datatypeURI = datatypeRestriction.getDatatypeURI();
        Automaton automaton = RDFPlainLiteralPatternValueSpaceSubset.getDatatypeAutomaton(datatypeURI);
        int minLength = 0;
        int maxLength = Integer.MAX_VALUE;
        for (int index = datatypeRestriction.getNumberOfFacetRestrictions() - 1; index >= 0; --index) {
            String facetURI = datatypeRestriction.getFacetURI(index);
            Object facetDataValue = datatypeRestriction.getFacetValue(index).getDataValue();
            if ((XSD_NS + "minLength").equals(facetURI)) {
                minLength = Math.max(minLength, (Integer)facetDataValue);
                continue;
            }
            if ((XSD_NS + "maxLength").equals(facetURI)) {
                maxLength = Math.min(maxLength, (Integer)facetDataValue);
                continue;
            }
            if ((XSD_NS + "length").equals(facetURI)) {
                minLength = Math.max(minLength, (Integer)facetDataValue);
                maxLength = Math.min(maxLength, (Integer)facetDataValue);
                continue;
            }
            if ((XSD_NS + "pattern").equals(facetURI)) {
                String pattern = (String)facetDataValue;
                Automaton facetAutomaton = RDFPlainLiteralPatternValueSpaceSubset.getPatternAutomaton(pattern);
                automaton = automaton.intersection(facetAutomaton);
                continue;
            }
            if ((RDF_NS + "langRange").equals(facetURI)) {
                String languageRange = (String)facetDataValue;
                Automaton languageRangeAutomaton = RDFPlainLiteralPatternValueSpaceSubset.getLanguageRangeAutomaton(languageRange);
                automaton = automaton.intersection(languageRangeAutomaton);
                continue;
            }
            throw new UnsupportedFacetException("Facet with URI '" + facetURI + "' not supported on '" + datatypeURI + "'.");
        }
        if (minLength > maxLength) {
            return null;
        }
        if (minLength != 0 || maxLength != Integer.MAX_VALUE) {
            automaton = automaton.intersection(RDFPlainLiteralPatternValueSpaceSubset.toAutomaton(minLength, maxLength));
        }
        if (automaton.isEmpty()) {
            return null;
        }
        return automaton;
    }

    @Override
    public boolean isSubsetOf(String subsetDatatypeURI, String supersetDatatypeURI) {
        assert (s_subsetsByDatatype.containsKey(subsetDatatypeURI));
        assert (s_subsetsByDatatype.containsKey(supersetDatatypeURI));
        return s_datatypeSupersets.get(subsetDatatypeURI).contains(supersetDatatypeURI);
    }

    @Override
    public boolean isDisjointWith(String datatypeURI1, String datatypeURI2) {
        assert (s_subsetsByDatatype.containsKey(datatypeURI1));
        assert (s_subsetsByDatatype.containsKey(datatypeURI2));
        return false;
    }

    static {
        s_subsetsByDatatype.put(RDF_NS + "PlainLiteral", new RDFPlainLiteralLengthValueSpaceSubset(new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.ABSENT, 0, Integer.MAX_VALUE), new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.PRESENT, 0, Integer.MAX_VALUE)));
        s_subsetsByDatatype.put(RDF_NS + "langString", new RDFPlainLiteralLengthValueSpaceSubset(new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.ABSENT, 0, Integer.MAX_VALUE), new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.PRESENT, 0, Integer.MAX_VALUE)));
        s_subsetsByDatatype.put(XSD_NS + "string", new RDFPlainLiteralLengthValueSpaceSubset(new RDFPlainLiteralLengthInterval(RDFPlainLiteralLengthInterval.LanguageTagMode.ABSENT, 0, Integer.MAX_VALUE)));
        RDFPlainLiteralDatatypeHandler.registerPatternDatatype(XSD_NS + "normalizedString");
        RDFPlainLiteralDatatypeHandler.registerPatternDatatype(XSD_NS + "token");
        RDFPlainLiteralDatatypeHandler.registerPatternDatatype(XSD_NS + "Name");
        RDFPlainLiteralDatatypeHandler.registerPatternDatatype(XSD_NS + "NCName");
        RDFPlainLiteralDatatypeHandler.registerPatternDatatype(XSD_NS + "NMTOKEN");
        RDFPlainLiteralDatatypeHandler.registerPatternDatatype(XSD_NS + "language");
        EMPTY_SUBSET = new RDFPlainLiteralLengthValueSpaceSubset();
        s_datatypeSupersets = new HashMap<String, Set<String>>();
        String[][] initializer = new String[][]{{RDF_NS + "PlainLiteral", RDF_NS + "PlainLiteral"}, {RDF_NS + "langString", RDF_NS + "PlainLiteral"}, {XSD_NS + "string", RDF_NS + "PlainLiteral", XSD_NS + "string"}, {XSD_NS + "normalizedString", RDF_NS + "PlainLiteral", XSD_NS + "string", XSD_NS + "normalizedString"}, {XSD_NS + "token", RDF_NS + "PlainLiteral", XSD_NS + "string", XSD_NS + "normalizedString", XSD_NS + "token"}, {XSD_NS + "Name", RDF_NS + "PlainLiteral", XSD_NS + "string", XSD_NS + "normalizedString", XSD_NS + "token", XSD_NS + "Name"}, {XSD_NS + "NCName", RDF_NS + "PlainLiteral", XSD_NS + "string", XSD_NS + "normalizedString", XSD_NS + "token", XSD_NS + "Name", XSD_NS + "NCName"}, {XSD_NS + "NMTOKEN", RDF_NS + "PlainLiteral", XSD_NS + "string", XSD_NS + "normalizedString", XSD_NS + "token", XSD_NS + "NMTOKEN"}, {XSD_NS + "language", RDF_NS + "PlainLiteral", XSD_NS + "string", XSD_NS + "normalizedString", XSD_NS + "token", XSD_NS + "language"}};
        for (int datatype1Index = 0; datatype1Index < initializer.length; ++datatype1Index) {
            String datatype1URI = initializer[datatype1Index][0];
            HashSet<String> set = new HashSet<String>();
            set.addAll(Arrays.asList(initializer[datatype1Index]).subList(1, initializer[datatype1Index].length));
            s_datatypeSupersets.put(datatype1URI, set);
        }
    }
}

