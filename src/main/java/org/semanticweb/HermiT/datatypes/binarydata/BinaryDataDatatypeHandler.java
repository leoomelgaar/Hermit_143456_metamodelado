package org.semanticweb.HermiT.datatypes.binarydata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.datatypes.DatatypeHandler;
import org.semanticweb.HermiT.datatypes.MalformedLiteralException;
import org.semanticweb.HermiT.datatypes.UnsupportedFacetException;
import org.semanticweb.HermiT.datatypes.ValueSpaceSubset;
import org.semanticweb.HermiT.model.DatatypeRestriction;

public class BinaryDataDatatypeHandler
implements DatatypeHandler {
    protected static final String XSD_NS = Prefixes.s_semanticWebPrefixes.get("xsd:");
    protected static final String XSD_HEX_BINARY = XSD_NS + "hexBinary";
    protected static final String XSD_BASE_64_BINARY = XSD_NS + "base64Binary";
    protected static final ValueSpaceSubset HEX_BINARY_ALL = new BinaryDataValueSpaceSubset(new BinaryDataLengthInterval(BinaryDataType.HEX_BINARY, 0, Integer.MAX_VALUE));
    protected static final ValueSpaceSubset BASE_64_BINARY_ALL = new BinaryDataValueSpaceSubset(new BinaryDataLengthInterval(BinaryDataType.BASE_64_BINARY, 0, Integer.MAX_VALUE));
    protected static final ValueSpaceSubset EMPTY = new BinaryDataValueSpaceSubset();
    protected static final Set<String> s_managedDatatypeURIs = new HashSet<String>();
    protected static final Set<String> s_supportedFacetURIs;

    @Override
    public Set<String> getManagedDatatypeURIs() {
        return s_managedDatatypeURIs;
    }

    @Override
    public Object parseLiteral(String lexicalForm, String datatypeURI) throws MalformedLiteralException {
        assert (s_managedDatatypeURIs.contains(datatypeURI));
        BinaryData binaryDataValue = XSD_HEX_BINARY.equals(datatypeURI) ? BinaryData.parseHexBinary(lexicalForm) : BinaryData.parseBase64Binary(lexicalForm);
        if (binaryDataValue == null) {
            throw new MalformedLiteralException(lexicalForm, datatypeURI);
        }
        return binaryDataValue;
    }

    @Override
    public void validateDatatypeRestriction(DatatypeRestriction datatypeRestriction) throws UnsupportedFacetException {
        String datatypeURI = datatypeRestriction.getDatatypeURI();
        assert (s_managedDatatypeURIs.contains(datatypeURI));
        for (int index = datatypeRestriction.getNumberOfFacetRestrictions() - 1; index >= 0; --index) {
            String facetURI = datatypeRestriction.getFacetURI(index);
            if (!s_supportedFacetURIs.contains(facetURI)) {
                throw new UnsupportedFacetException("Facet with URI '" + facetURI + "' is not supported on binary datatypes; only xsd:minLength, xsd:maxLength, and xsd:length are supported, but the ontology contains the restriction: " + this);
            }
            Object facetDataValue = datatypeRestriction.getFacetValue(index).getDataValue();
            if (!(facetDataValue instanceof Integer)) {
                throw new UnsupportedFacetException("The binary datatypes accept only integers as facet values, but for the facet with URI '" + facetURI + "' there is a non-integer value " + facetDataValue + " in the datatype restriction " + this + ". ");
            }
            int value = (Integer)facetDataValue;
            if (value >= 0 && value != Integer.MAX_VALUE) continue;
            throw new UnsupportedFacetException("The datatype restriction " + this + " cannot be handled. The facet with URI '" + facetURI + "' does not support integer " + value + " as value. " + (value < 0 ? "The value should not be negative. " : "The value is outside of the supported integer range, i.e., it is larger than 2147483647"));
        }
    }

    @Override
    public ValueSpaceSubset createValueSpaceSubset(DatatypeRestriction datatypeRestriction) {
        String datatypeURI = datatypeRestriction.getDatatypeURI();
        assert (s_managedDatatypeURIs.contains(datatypeURI));
        if (datatypeRestriction.getNumberOfFacetRestrictions() == 0) {
            if (XSD_HEX_BINARY.equals(datatypeURI)) {
                return HEX_BINARY_ALL;
            }
            return BASE_64_BINARY_ALL;
        }
        BinaryDataLengthInterval interval = this.getIntervalFor(datatypeRestriction);
        if (interval == null) {
            return EMPTY;
        }
        return new BinaryDataValueSpaceSubset(interval);
    }

    @Override
    public ValueSpaceSubset conjoinWithDR(ValueSpaceSubset valueSpaceSubset, DatatypeRestriction datatypeRestriction) {
        assert (s_managedDatatypeURIs.contains(datatypeRestriction.getDatatypeURI()));
        if (datatypeRestriction.getNumberOfFacetRestrictions() == 0 || valueSpaceSubset == EMPTY) {
            return valueSpaceSubset;
        }
        BinaryDataLengthInterval interval = this.getIntervalFor(datatypeRestriction);
        if (interval == null) {
            return EMPTY;
        }
        BinaryDataValueSpaceSubset doubleSubset = (BinaryDataValueSpaceSubset)valueSpaceSubset;
        List<BinaryDataLengthInterval> oldIntervals = doubleSubset.m_intervals;
        ArrayList<BinaryDataLengthInterval> newIntervals = new ArrayList<BinaryDataLengthInterval>();
        for (int index = 0; index < oldIntervals.size(); ++index) {
            BinaryDataLengthInterval oldInterval = oldIntervals.get(index);
            BinaryDataLengthInterval intersection = oldInterval.intersectWith(interval);
            if (intersection == null) continue;
            newIntervals.add(intersection);
        }
        if (newIntervals.isEmpty()) {
            return EMPTY;
        }
        return new BinaryDataValueSpaceSubset(newIntervals);
    }

    @Override
    public ValueSpaceSubset conjoinWithDRNegation(ValueSpaceSubset valueSpaceSubset, DatatypeRestriction datatypeRestriction) {
        String datatypeURI = datatypeRestriction.getDatatypeURI();
        assert (datatypeRestriction.getNumberOfFacetRestrictions() != 0);
        if (datatypeRestriction.getNumberOfFacetRestrictions() == 0 || valueSpaceSubset == EMPTY) {
            return EMPTY;
        }
        BinaryDataLengthInterval interval = this.getIntervalFor(datatypeRestriction);
        if (interval == null) {
            return valueSpaceSubset;
        }
        BinaryDataType binaryDataType = XSD_HEX_BINARY.equals(datatypeURI) ? BinaryDataType.HEX_BINARY : BinaryDataType.BASE_64_BINARY;
        BinaryDataValueSpaceSubset doubleSubset = (BinaryDataValueSpaceSubset)valueSpaceSubset;
        BinaryDataLengthInterval complementInterval1 = null;
        if (interval.m_minLength != 0) {
            complementInterval1 = new BinaryDataLengthInterval(binaryDataType, 0, interval.m_minLength - 1);
        }
        BinaryDataLengthInterval complementInterval2 = null;
        if (interval.m_maxLength != Integer.MAX_VALUE) {
            complementInterval2 = new BinaryDataLengthInterval(binaryDataType, interval.m_maxLength + 1, Integer.MAX_VALUE);
        }
        List<BinaryDataLengthInterval> oldIntervals = doubleSubset.m_intervals;
        ArrayList<BinaryDataLengthInterval> newIntervals = new ArrayList<BinaryDataLengthInterval>();
        for (int index = 0; index < oldIntervals.size(); ++index) {
            BinaryDataLengthInterval intersection;
            BinaryDataLengthInterval oldInterval = oldIntervals.get(index);
            if (complementInterval1 != null && (intersection = oldInterval.intersectWith(complementInterval1)) != null) {
                newIntervals.add(intersection);
            }
            if (complementInterval2 == null || (intersection = oldInterval.intersectWith(complementInterval2)) == null) continue;
            newIntervals.add(intersection);
        }
        if (newIntervals.isEmpty()) {
            return EMPTY;
        }
        return new BinaryDataValueSpaceSubset(newIntervals);
    }

    protected BinaryDataLengthInterval getIntervalFor(DatatypeRestriction datatypeRestriction) {
        BinaryDataType binaryDataType;
        String datatypeURI = datatypeRestriction.getDatatypeURI();
        assert (datatypeRestriction.getNumberOfFacetRestrictions() != 0);
        int minLength = 0;
        int maxLength = Integer.MAX_VALUE;
        for (int index = datatypeRestriction.getNumberOfFacetRestrictions() - 1; index >= 0; --index) {
            String facetURI = datatypeRestriction.getFacetURI(index);
            int facetDataValue = (Integer)datatypeRestriction.getFacetValue(index).getDataValue();
            if ((XSD_NS + "minLength").equals(facetURI)) {
                minLength = Math.max(minLength, facetDataValue);
                continue;
            }
            if ((XSD_NS + "maxLength").equals(facetURI)) {
                maxLength = Math.min(maxLength, facetDataValue);
                continue;
            }
            if ((XSD_NS + "length").equals(facetURI)) {
                minLength = Math.max(minLength, facetDataValue);
                maxLength = Math.min(maxLength, facetDataValue);
                continue;
            }
            throw new IllegalStateException("Internal error: facet '" + facetURI + "' is not supported by " + Prefixes.STANDARD_PREFIXES.abbreviateIRI(datatypeURI) + ".");
        }
        BinaryDataType binaryDataType2 = binaryDataType = XSD_HEX_BINARY.equals(datatypeURI) ? BinaryDataType.HEX_BINARY : BinaryDataType.BASE_64_BINARY;
        if (BinaryDataLengthInterval.isIntervalEmpty(minLength, maxLength)) {
            return null;
        }
        return new BinaryDataLengthInterval(binaryDataType, minLength, maxLength);
    }

    @Override
    public boolean isSubsetOf(String subsetDatatypeURI, String supersetDatatypeURI) {
        return subsetDatatypeURI.equals(supersetDatatypeURI);
    }

    @Override
    public boolean isDisjointWith(String datatypeURI1, String datatypeURI2) {
        return !datatypeURI1.equals(datatypeURI2);
    }

    static {
        s_managedDatatypeURIs.add(XSD_HEX_BINARY);
        s_managedDatatypeURIs.add(XSD_BASE_64_BINARY);
        s_supportedFacetURIs = new HashSet<String>();
        s_supportedFacetURIs.add(XSD_NS + "minLength");
        s_supportedFacetURIs.add(XSD_NS + "maxLength");
        s_supportedFacetURIs.add(XSD_NS + "length");
    }
}

