package org.semanticweb.HermiT.datatypes.floatnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.datatypes.DatatypeHandler;
import org.semanticweb.HermiT.datatypes.MalformedLiteralException;
import org.semanticweb.HermiT.datatypes.UnsupportedFacetException;
import org.semanticweb.HermiT.datatypes.ValueSpaceSubset;
import org.semanticweb.HermiT.model.DatatypeRestriction;

public class FloatDatatypeHandler
implements DatatypeHandler {
    protected static final String XSD_NS = Prefixes.s_semanticWebPrefixes.get("xsd:");
    protected static final String XSD_FLOAT = XSD_NS + "float";
    protected static final ValueSpaceSubset FLOAT_ENTIRE = new EntireFloatSubset();
    protected static final ValueSpaceSubset EMPTY_SUBSET = new EmptyFloatSubset();
    protected static final Set<String> s_managedDatatypeURIs = Collections.singleton(XSD_FLOAT);
    protected static final Set<String> s_supportedFacetURIs = new HashSet<String>();

    @Override
    public Set<String> getManagedDatatypeURIs() {
        return s_managedDatatypeURIs;
    }

    @Override
    public Object parseLiteral(String lexicalForm, String datatypeURI) throws MalformedLiteralException {
        assert (XSD_FLOAT.equals(datatypeURI));
        try {
            return Float.valueOf(Float.parseFloat(lexicalForm));
        }
        catch (NumberFormatException error) {
            if (lexicalForm.equals("INF")) {
                return Float.valueOf(Float.POSITIVE_INFINITY);
            }
            if (lexicalForm.equals("-INF")) {
                return Float.valueOf(Float.NEGATIVE_INFINITY);
            }
            throw new MalformedLiteralException(lexicalForm, datatypeURI, error);
        }
    }

    @Override
    public void validateDatatypeRestriction(DatatypeRestriction datatypeRestriction) throws UnsupportedFacetException {
        assert (XSD_FLOAT.equals(datatypeRestriction.getDatatypeURI()));
        for (int index = datatypeRestriction.getNumberOfFacetRestrictions() - 1; index >= 0; --index) {
            String facetURI = datatypeRestriction.getFacetURI(index);
            if (!s_supportedFacetURIs.contains(facetURI)) {
                throw new UnsupportedFacetException("A facet with URI '" + facetURI + "' is not supported on xsd:float. The xsd:float datatype supports only xsd:minInclusive, xsd:maxInclusive, xsd:minExclusive, and xsd:maxExclusive, but the ontology contains a datatype restriction " + this);
            }
            Object facetDataValue = datatypeRestriction.getFacetValue(index).getDataValue();
            if (facetDataValue instanceof Float) continue;
            throw new UnsupportedFacetException("The '" + facetURI + "' facet takes only floats as values when used on an xsd:float datatype, but the ontology contains a datatype restriction " + this);
        }
    }

    @Override
    public ValueSpaceSubset createValueSpaceSubset(DatatypeRestriction datatypeRestriction) {
        assert (XSD_FLOAT.equals(datatypeRestriction.getDatatypeURI()));
        if (datatypeRestriction.getNumberOfFacetRestrictions() == 0) {
            return FLOAT_ENTIRE;
        }
        FloatInterval interval = this.getIntervalFor(datatypeRestriction);
        if (interval == null) {
            return EMPTY_SUBSET;
        }
        return new NoNaNFloatSubset(interval);
    }

    @Override
    public ValueSpaceSubset conjoinWithDR(ValueSpaceSubset valueSpaceSubset, DatatypeRestriction datatypeRestriction) {
        assert (XSD_FLOAT.equals(datatypeRestriction.getDatatypeURI()));
        if (datatypeRestriction.getNumberOfFacetRestrictions() == 0 || valueSpaceSubset == EMPTY_SUBSET) {
            return valueSpaceSubset;
        }
        FloatInterval interval = this.getIntervalFor(datatypeRestriction);
        if (interval == null) {
            return EMPTY_SUBSET;
        }
        if (valueSpaceSubset == FLOAT_ENTIRE) {
            return new NoNaNFloatSubset(interval);
        }
        NoNaNFloatSubset floatSubset = (NoNaNFloatSubset)valueSpaceSubset;
        List<FloatInterval> oldIntervals = floatSubset.m_intervals;
        ArrayList<FloatInterval> newIntervals = new ArrayList<FloatInterval>();
        for (int index = 0; index < oldIntervals.size(); ++index) {
            FloatInterval oldInterval = oldIntervals.get(index);
            FloatInterval intersection = oldInterval.intersectWith(interval);
            if (intersection == null) continue;
            newIntervals.add(intersection);
        }
        if (newIntervals.isEmpty()) {
            return EMPTY_SUBSET;
        }
        return new NoNaNFloatSubset(newIntervals);
    }

    @Override
    public ValueSpaceSubset conjoinWithDRNegation(ValueSpaceSubset valueSpaceSubset, DatatypeRestriction datatypeRestriction) {
        assert (XSD_FLOAT.equals(datatypeRestriction.getDatatypeURI()));
        if (datatypeRestriction.getNumberOfFacetRestrictions() == 0 || valueSpaceSubset == EMPTY_SUBSET) {
            return EMPTY_SUBSET;
        }
        FloatInterval interval = this.getIntervalFor(datatypeRestriction);
        if (interval == null) {
            return valueSpaceSubset;
        }
        if (valueSpaceSubset == FLOAT_ENTIRE) {
            ArrayList<FloatInterval> newIntervals = new ArrayList<FloatInterval>();
            if (!FloatInterval.areIdentical(interval.m_lowerBoundInclusive, Float.NEGATIVE_INFINITY)) {
                newIntervals.add(new FloatInterval(Float.NEGATIVE_INFINITY, FloatInterval.previousFloat(interval.m_lowerBoundInclusive)));
            }
            if (!FloatInterval.areIdentical(interval.m_upperBoundInclusive, Float.POSITIVE_INFINITY)) {
                newIntervals.add(new FloatInterval(FloatInterval.nextFloat(interval.m_upperBoundInclusive), Float.POSITIVE_INFINITY));
            }
            if (newIntervals.isEmpty()) {
                return EMPTY_SUBSET;
            }
            return new NoNaNFloatSubset(newIntervals);
        }
        NoNaNFloatSubset floatSubset = (NoNaNFloatSubset)valueSpaceSubset;
        FloatInterval complementInterval1 = null;
        if (!FloatInterval.areIdentical(interval.m_lowerBoundInclusive, Float.NEGATIVE_INFINITY)) {
            complementInterval1 = new FloatInterval(Float.NEGATIVE_INFINITY, FloatInterval.previousFloat(interval.m_lowerBoundInclusive));
        }
        FloatInterval complementInterval2 = null;
        if (!FloatInterval.areIdentical(interval.m_upperBoundInclusive, Float.POSITIVE_INFINITY)) {
            complementInterval2 = new FloatInterval(FloatInterval.nextFloat(interval.m_upperBoundInclusive), Float.POSITIVE_INFINITY);
        }
        List<FloatInterval> oldIntervals = floatSubset.m_intervals;
        ArrayList<FloatInterval> newIntervals = new ArrayList<FloatInterval>();
        for (int index = 0; index < oldIntervals.size(); ++index) {
            FloatInterval intersection;
            FloatInterval oldInterval = oldIntervals.get(index);
            if (complementInterval1 != null && (intersection = oldInterval.intersectWith(complementInterval1)) != null) {
                newIntervals.add(intersection);
            }
            if (complementInterval2 == null || (intersection = oldInterval.intersectWith(complementInterval2)) == null) continue;
            newIntervals.add(intersection);
        }
        if (newIntervals.isEmpty()) {
            return EMPTY_SUBSET;
        }
        return new NoNaNFloatSubset(newIntervals);
    }

    protected FloatInterval getIntervalFor(DatatypeRestriction datatypeRestriction) {
        assert (datatypeRestriction.getNumberOfFacetRestrictions() != 0);
        float lowerBoundInclusive = Float.NEGATIVE_INFINITY;
        float upperBoundInclusive = Float.POSITIVE_INFINITY;
        for (int index = datatypeRestriction.getNumberOfFacetRestrictions() - 1; index >= 0; --index) {
            String facetURI = datatypeRestriction.getFacetURI(index);
            float facetDataValue = ((Float)datatypeRestriction.getFacetValue(index).getDataValue()).floatValue();
            if ((XSD_NS + "minInclusive").equals(facetURI)) {
                if (FloatInterval.areIdentical(facetDataValue, 0.0f)) {
                    facetDataValue = -0.0f;
                }
                if (!FloatInterval.isSmallerEqual(lowerBoundInclusive, facetDataValue)) continue;
                lowerBoundInclusive = facetDataValue;
                continue;
            }
            if ((XSD_NS + "minExclusive").equals(facetURI)) {
                if (FloatInterval.areIdentical(facetDataValue, -0.0f)) {
                    facetDataValue = 0.0f;
                }
                if (!FloatInterval.isSmallerEqual(lowerBoundInclusive, facetDataValue = FloatInterval.nextFloat(facetDataValue))) continue;
                lowerBoundInclusive = facetDataValue;
                continue;
            }
            if ((XSD_NS + "maxInclusive").equals(facetURI)) {
                if (FloatInterval.areIdentical(facetDataValue, -0.0f)) {
                    facetDataValue = 0.0f;
                }
                if (!FloatInterval.isSmallerEqual(facetDataValue, upperBoundInclusive)) continue;
                upperBoundInclusive = facetDataValue;
                continue;
            }
            if ((XSD_NS + "maxExclusive").equals(facetURI)) {
                if (FloatInterval.areIdentical(facetDataValue, 0.0f)) {
                    facetDataValue = -0.0f;
                }
                if (!FloatInterval.isSmallerEqual(facetDataValue = FloatInterval.previousFloat(facetDataValue), upperBoundInclusive)) continue;
                upperBoundInclusive = facetDataValue;
                continue;
            }
            throw new IllegalStateException("Internal error: facet '" + facetURI + "' is not supported by xsd:float.");
        }
        if (FloatInterval.isIntervalEmpty(lowerBoundInclusive, upperBoundInclusive)) {
            return null;
        }
        return new FloatInterval(lowerBoundInclusive, upperBoundInclusive);
    }

    @Override
    public boolean isSubsetOf(String subsetDatatypeURI, String supersetDatatypeURI) {
        assert (XSD_FLOAT.equals(subsetDatatypeURI));
        assert (XSD_FLOAT.equals(supersetDatatypeURI));
        return true;
    }

    @Override
    public boolean isDisjointWith(String datatypeURI1, String datatypeURI2) {
        assert (XSD_FLOAT.equals(datatypeURI1));
        assert (XSD_FLOAT.equals(datatypeURI2));
        return false;
    }

    static {
        s_supportedFacetURIs.add(XSD_NS + "minInclusive");
        s_supportedFacetURIs.add(XSD_NS + "minExclusive");
        s_supportedFacetURIs.add(XSD_NS + "maxInclusive");
        s_supportedFacetURIs.add(XSD_NS + "maxExclusive");
    }
}

