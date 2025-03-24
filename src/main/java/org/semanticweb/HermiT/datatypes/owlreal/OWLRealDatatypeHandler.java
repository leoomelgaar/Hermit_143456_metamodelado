package org.semanticweb.HermiT.datatypes.owlreal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.datatypes.DatatypeHandler;
import org.semanticweb.HermiT.datatypes.MalformedLiteralException;
import org.semanticweb.HermiT.datatypes.UnsupportedFacetException;
import org.semanticweb.HermiT.datatypes.ValueSpaceSubset;
import org.semanticweb.HermiT.model.Constant;
import org.semanticweb.HermiT.model.DatatypeRestriction;

public class OWLRealDatatypeHandler
implements DatatypeHandler {
    protected static final String OWL_NS = Prefixes.s_semanticWebPrefixes.get("owl:");
    protected static final String XSD_NS = Prefixes.s_semanticWebPrefixes.get("xsd:");
    protected static final Map<String, NumberInterval> s_intervalsByDatatype = new HashMap<String, NumberInterval>();
    protected static final Map<String, ValueSpaceSubset> s_subsetsByDatatype = new HashMap<String, ValueSpaceSubset>();
    protected static final ValueSpaceSubset EMPTY_SUBSET;
    protected static final Set<String> s_supportedFacetURIs;
    protected static final Map<String, Set<String>> s_datatypeSupersets;
    protected static final Map<String, Set<String>> s_datatypeDisjoints;

    @Override
    public Set<String> getManagedDatatypeURIs() {
        return s_intervalsByDatatype.keySet();
    }

    @Override
    public Object parseLiteral(String lexicalForm, String datatypeURI) throws MalformedLiteralException {
        assert (s_intervalsByDatatype.containsKey(datatypeURI));
        try {
            if ((OWL_NS + "real").equals(datatypeURI)) {
                throw new MalformedLiteralException(lexicalForm, datatypeURI);
            }
            if ((OWL_NS + "rational").equals(datatypeURI)) {
                return Numbers.parseRational(lexicalForm);
            }
            if ((XSD_NS + "decimal").equals(datatypeURI)) {
                return Numbers.parseDecimal(lexicalForm);
            }
            return Numbers.parseInteger(lexicalForm);
        }
        catch (NumberFormatException error) {
            throw new MalformedLiteralException(lexicalForm, datatypeURI, error);
        }
    }

    @Override
    public void validateDatatypeRestriction(DatatypeRestriction datatypeRestriction) throws UnsupportedFacetException {
        assert (s_intervalsByDatatype.containsKey(datatypeRestriction.getDatatypeURI()));
        for (int index = datatypeRestriction.getNumberOfFacetRestrictions() - 1; index >= 0; --index) {
            String facetURI = datatypeRestriction.getFacetURI(index);
            if (!s_supportedFacetURIs.contains(facetURI)) {
                throw new UnsupportedFacetException("A facet with URI '" + facetURI + "' is not supported on datatypes derived from owl:real. The owl:real derived datatypes support only xsd:minInclusive, xsd:maxInclusive, xsd:minExclusive, and xsd:maxExclusive, but the ontology contains a datatype restriction " + this);
            }
            Constant facetValue = datatypeRestriction.getFacetValue(index);
            Object facetDataValue = facetValue.getDataValue();
            if (!(facetDataValue instanceof Number)) {
                throw new UnsupportedFacetException("The '" + facetURI + "' facet takes only numbers as values when used on a datatype derived from owl:real, but the ontology contains a datatype restriction " + this + " where " + facetDataValue + " is not a number. ");
            }
            if (Numbers.isValidNumber((Number)facetDataValue)) continue;
            throw new UnsupportedFacetException("The facet with URI '" + facetURI + "' does not support '" + facetValue + "' as value. The value should be an integer, a decimal, or a rational, but this seems not to be the case in the datatype restriction " + this);
        }
    }

    @Override
    public ValueSpaceSubset createValueSpaceSubset(DatatypeRestriction datatypeRestriction) {
        assert (s_intervalsByDatatype.containsKey(datatypeRestriction.getDatatypeURI()));
        if (datatypeRestriction.getNumberOfFacetRestrictions() == 0) {
            return s_subsetsByDatatype.get(datatypeRestriction.getDatatypeURI());
        }
        NumberInterval interval = this.getIntervalFor(datatypeRestriction);
        if (interval == null) {
            return EMPTY_SUBSET;
        }
        return new OWLRealValueSpaceSubset(interval);
    }

    @Override
    public ValueSpaceSubset conjoinWithDR(ValueSpaceSubset valueSpaceSubset, DatatypeRestriction datatypeRestriction) {
        assert (s_intervalsByDatatype.containsKey(datatypeRestriction.getDatatypeURI()));
        NumberInterval interval = this.getIntervalFor(datatypeRestriction);
        if (interval == null) {
            return EMPTY_SUBSET;
        }
        OWLRealValueSpaceSubset realSubset = (OWLRealValueSpaceSubset)valueSpaceSubset;
        List<NumberInterval> oldIntervals = realSubset.m_intervals;
        ArrayList<NumberInterval> newIntervals = new ArrayList<NumberInterval>();
        for (int index = 0; index < oldIntervals.size(); ++index) {
            NumberInterval oldInterval = oldIntervals.get(index);
            NumberInterval intersection = oldInterval.intersectWith(interval);
            if (intersection == null) continue;
            newIntervals.add(intersection);
        }
        if (newIntervals.isEmpty()) {
            return EMPTY_SUBSET;
        }
        return new OWLRealValueSpaceSubset(newIntervals);
    }

    @Override
    public ValueSpaceSubset conjoinWithDRNegation(ValueSpaceSubset valueSpaceSubset, DatatypeRestriction datatypeRestriction) {
        assert (s_intervalsByDatatype.containsKey(datatypeRestriction.getDatatypeURI()));
        NumberInterval interval = this.getIntervalFor(datatypeRestriction);
        if (interval == null) {
            return valueSpaceSubset;
        }
        NumberInterval complementInterval1 = null;
        if (!interval.m_lowerBound.equals(MinusInfinity.INSTANCE)) {
            complementInterval1 = new NumberInterval(NumberRange.REAL, NumberRange.NOTHING, MinusInfinity.INSTANCE, BoundType.EXCLUSIVE, interval.m_lowerBound, interval.m_lowerBoundType.getComplement());
        }
        NumberInterval complementInterval2 = null;
        if (!interval.m_baseRange.equals(NumberRange.REAL)) {
            complementInterval2 = new NumberInterval(NumberRange.REAL, interval.m_baseRange, interval.m_lowerBound, interval.m_lowerBoundType, interval.m_upperBound, interval.m_upperBoundType);
        }
        NumberInterval complementInterval3 = null;
        if (!interval.m_upperBound.equals(PlusInfinity.INSTANCE)) {
            complementInterval3 = new NumberInterval(NumberRange.REAL, NumberRange.NOTHING, interval.m_upperBound, interval.m_upperBoundType.getComplement(), PlusInfinity.INSTANCE, BoundType.EXCLUSIVE);
        }
        OWLRealValueSpaceSubset realSubset = (OWLRealValueSpaceSubset)valueSpaceSubset;
        List<NumberInterval> oldIntervals = realSubset.m_intervals;
        ArrayList<NumberInterval> newIntervals = new ArrayList<NumberInterval>();
        for (int index = 0; index < oldIntervals.size(); ++index) {
            NumberInterval intersection;
            NumberInterval oldInterval = oldIntervals.get(index);
            if (complementInterval1 != null && (intersection = oldInterval.intersectWith(complementInterval1)) != null) {
                newIntervals.add(intersection);
            }
            if (complementInterval2 != null && (intersection = oldInterval.intersectWith(complementInterval2)) != null) {
                newIntervals.add(intersection);
            }
            if (complementInterval3 == null || (intersection = oldInterval.intersectWith(complementInterval3)) == null) continue;
            newIntervals.add(intersection);
        }
        if (newIntervals.isEmpty()) {
            return EMPTY_SUBSET;
        }
        return new OWLRealValueSpaceSubset(newIntervals);
    }

    protected NumberInterval getIntervalFor(DatatypeRestriction datatypeRestriction) {
        NumberInterval baseInterval = s_intervalsByDatatype.get(datatypeRestriction.getDatatypeURI());
        if (datatypeRestriction.getNumberOfFacetRestrictions() == 0) {
            return baseInterval;
        }
        NumberRange baseRange = baseInterval.m_baseRange;
        NumberRange excludedRange = baseInterval.m_excludedRange;
        Number lowerBound = baseInterval.m_lowerBound;
        BoundType lowerBoundType = baseInterval.m_lowerBoundType;
        Number upperBound = baseInterval.m_upperBound;
        BoundType upperBoundType = baseInterval.m_upperBoundType;
        for (int index = datatypeRestriction.getNumberOfFacetRestrictions() - 1; index >= 0; --index) {
            int comparison;
            String facetURI = datatypeRestriction.getFacetURI(index);
            Number facetDataValue = (Number)datatypeRestriction.getFacetValue(index).getDataValue();
            if ((XSD_NS + "minInclusive").equals(facetURI)) {
                comparison = Numbers.compare(facetDataValue, lowerBound);
                if (comparison <= 0) continue;
                lowerBound = facetDataValue;
                lowerBoundType = BoundType.INCLUSIVE;
                continue;
            }
            if ((XSD_NS + "minExclusive").equals(facetURI)) {
                comparison = Numbers.compare(facetDataValue, lowerBound);
                if (comparison > 0) {
                    lowerBound = facetDataValue;
                    lowerBoundType = BoundType.EXCLUSIVE;
                    continue;
                }
                if (comparison != 0) continue;
                lowerBoundType = BoundType.EXCLUSIVE;
                continue;
            }
            if ((XSD_NS + "maxInclusive").equals(facetURI)) {
                comparison = Numbers.compare(facetDataValue, upperBound);
                if (comparison >= 0) continue;
                upperBound = facetDataValue;
                upperBoundType = BoundType.INCLUSIVE;
                continue;
            }
            if ((XSD_NS + "maxExclusive").equals(facetURI)) {
                comparison = Numbers.compare(facetDataValue, upperBound);
                if (comparison < 0) {
                    upperBound = facetDataValue;
                    upperBoundType = BoundType.EXCLUSIVE;
                    continue;
                }
                if (comparison != 0) continue;
                upperBoundType = BoundType.EXCLUSIVE;
                continue;
            }
            throw new IllegalStateException("Internal error: facet '" + facetURI + "' is not supported by owl:real.");
        }
        if (NumberInterval.isIntervalEmpty(baseRange, excludedRange, lowerBound, lowerBoundType, upperBound, upperBoundType)) {
            return null;
        }
        return new NumberInterval(baseRange, excludedRange, lowerBound, lowerBoundType, upperBound, upperBoundType);
    }

    @Override
    public boolean isSubsetOf(String subsetDatatypeURI, String supersetDatatypeURI) {
        assert (s_intervalsByDatatype.containsKey(subsetDatatypeURI));
        assert (s_intervalsByDatatype.containsKey(supersetDatatypeURI));
        return s_datatypeSupersets.get(subsetDatatypeURI).contains(supersetDatatypeURI);
    }

    @Override
    public boolean isDisjointWith(String datatypeURI1, String datatypeURI2) {
        assert (s_intervalsByDatatype.containsKey(datatypeURI1));
        assert (s_intervalsByDatatype.containsKey(datatypeURI2));
        return s_datatypeDisjoints.get(datatypeURI1).contains(datatypeURI2);
    }

    static {
        Object[][] initializer = new Object[][]{{OWL_NS + "real", NumberRange.REAL, MinusInfinity.INSTANCE, BoundType.EXCLUSIVE, PlusInfinity.INSTANCE, BoundType.EXCLUSIVE}, {OWL_NS + "rational", NumberRange.RATIONAL, MinusInfinity.INSTANCE, BoundType.EXCLUSIVE, PlusInfinity.INSTANCE, BoundType.EXCLUSIVE}, {XSD_NS + "decimal", NumberRange.DECIMAL, MinusInfinity.INSTANCE, BoundType.EXCLUSIVE, PlusInfinity.INSTANCE, BoundType.EXCLUSIVE}, {XSD_NS + "integer", NumberRange.INTEGER, MinusInfinity.INSTANCE, BoundType.EXCLUSIVE, PlusInfinity.INSTANCE, BoundType.EXCLUSIVE}, {XSD_NS + "nonNegativeInteger", NumberRange.INTEGER, 0, BoundType.INCLUSIVE, PlusInfinity.INSTANCE, BoundType.EXCLUSIVE}, {XSD_NS + "positiveInteger", NumberRange.INTEGER, 0, BoundType.EXCLUSIVE, PlusInfinity.INSTANCE, BoundType.EXCLUSIVE}, {XSD_NS + "nonPositiveInteger", NumberRange.INTEGER, MinusInfinity.INSTANCE, BoundType.EXCLUSIVE, 0, BoundType.INCLUSIVE}, {XSD_NS + "negativeInteger", NumberRange.INTEGER, MinusInfinity.INSTANCE, BoundType.EXCLUSIVE, 0, BoundType.EXCLUSIVE}, {XSD_NS + "long", NumberRange.INTEGER, Long.MIN_VALUE, BoundType.INCLUSIVE, Long.MAX_VALUE, BoundType.INCLUSIVE}, {XSD_NS + "int", NumberRange.INTEGER, Integer.MIN_VALUE, BoundType.INCLUSIVE, Integer.MAX_VALUE, BoundType.INCLUSIVE}, {XSD_NS + "short", NumberRange.INTEGER, -32768, BoundType.INCLUSIVE, 32767, BoundType.INCLUSIVE}, {XSD_NS + "byte", NumberRange.INTEGER, -128, BoundType.INCLUSIVE, 127, BoundType.INCLUSIVE}, {XSD_NS + "unsignedLong", NumberRange.INTEGER, 0, BoundType.INCLUSIVE, new BigInteger("18446744073709551615"), BoundType.INCLUSIVE}, {XSD_NS + "unsignedInt", NumberRange.INTEGER, 0, BoundType.INCLUSIVE, 0xFFFFFFFFL, BoundType.INCLUSIVE}, {XSD_NS + "unsignedShort", NumberRange.INTEGER, 0, BoundType.INCLUSIVE, 65535, BoundType.INCLUSIVE}, {XSD_NS + "unsignedByte", NumberRange.INTEGER, 0, BoundType.INCLUSIVE, 255, BoundType.INCLUSIVE}};
        for (Object[] row : initializer) {
            String datatypeURI = (String)row[0];
            NumberInterval interval = new NumberInterval((NumberRange) row[1], NumberRange.NOTHING, (Number)row[2], (BoundType) row[3], (Number)row[4], (BoundType) row[5]);
            s_intervalsByDatatype.put(datatypeURI, interval);
            s_subsetsByDatatype.put(datatypeURI, new OWLRealValueSpaceSubset(interval));
        }
        EMPTY_SUBSET = new OWLRealValueSpaceSubset();
        s_supportedFacetURIs = new HashSet<String>();
        s_supportedFacetURIs.add(XSD_NS + "minInclusive");
        s_supportedFacetURIs.add(XSD_NS + "minExclusive");
        s_supportedFacetURIs.add(XSD_NS + "maxInclusive");
        s_supportedFacetURIs.add(XSD_NS + "maxExclusive");
        s_datatypeSupersets = new HashMap<String, Set<String>>();
        s_datatypeDisjoints = new HashMap<String, Set<String>>();
        for (String datatypeURI : s_intervalsByDatatype.keySet()) {
            s_datatypeSupersets.put(datatypeURI, new HashSet());
            s_datatypeDisjoints.put(datatypeURI, new HashSet());
        }
        for (Map.Entry entry1 : s_intervalsByDatatype.entrySet()) {
            String datatypeURI1 = (String)entry1.getKey();
            NumberInterval interval1 = (NumberInterval)entry1.getValue();
            for (Map.Entry<String, NumberInterval> entry2 : s_intervalsByDatatype.entrySet()) {
                String datatypeURI2 = entry2.getKey();
                NumberInterval interval2 = entry2.getValue();
                NumberInterval intersection = interval1.intersectWith(interval2);
                if (intersection == null) {
                    s_datatypeDisjoints.get(datatypeURI1).add(datatypeURI2);
                    continue;
                }
                if (intersection != interval1) continue;
                s_datatypeSupersets.get(datatypeURI1).add(datatypeURI2);
            }
        }
    }
}

