package org.semanticweb.HermiT.datatypes.xmlliteral;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.datatypes.DatatypeHandler;
import org.semanticweb.HermiT.datatypes.MalformedLiteralException;
import org.semanticweb.HermiT.datatypes.UnsupportedFacetException;
import org.semanticweb.HermiT.datatypes.ValueSpaceSubset;
import org.semanticweb.HermiT.model.DatatypeRestriction;

public class XMLLiteralDatatypeHandler
implements DatatypeHandler {
    protected static final String RDF_XML_LITERAL = Prefixes.s_semanticWebPrefixes.get("rdf:") + "XMLLiteral";
    protected static final ValueSpaceSubset XML_LITERAL_ALL = new XMLLiteralAll();
    protected static final ValueSpaceSubset EMPTY = new XMLLiteralNone();
    protected static final Set<String> s_managedDatatypeURIs = Collections.singleton(RDF_XML_LITERAL);

    @Override
    public Set<String> getManagedDatatypeURIs() {
        return s_managedDatatypeURIs;
    }

    @Override
    public Object parseLiteral(String lexicalForm, String datatypeURI) throws MalformedLiteralException {
        assert (RDF_XML_LITERAL.equals(datatypeURI));
        try {
            return XMLLiteral.parse(lexicalForm);
        }
        catch (Exception e) {
            throw new MalformedLiteralException(lexicalForm, datatypeURI, e);
        }
    }

    @Override
    public void validateDatatypeRestriction(DatatypeRestriction datatypeRestriction) throws UnsupportedFacetException {
        assert (RDF_XML_LITERAL.equals(datatypeRestriction.getDatatypeURI()));
        if (datatypeRestriction.getNumberOfFacetRestrictions() > 0) {
            throw new UnsupportedFacetException("The rdf:XMLLiteral datatype does not provide any facets, but the ontology contains a restriction on boolean with facets: " + this);
        }
    }

    @Override
    public ValueSpaceSubset createValueSpaceSubset(DatatypeRestriction datatypeRestriction) {
        assert (RDF_XML_LITERAL.equals(datatypeRestriction.getDatatypeURI()));
        assert (datatypeRestriction.getNumberOfFacetRestrictions() == 0);
        return XML_LITERAL_ALL;
    }

    @Override
    public ValueSpaceSubset conjoinWithDR(ValueSpaceSubset valueSpaceSubset, DatatypeRestriction datatypeRestriction) {
        assert (RDF_XML_LITERAL.equals(datatypeRestriction.getDatatypeURI()));
        assert (datatypeRestriction.getNumberOfFacetRestrictions() == 0);
        return XML_LITERAL_ALL;
    }

    @Override
    public ValueSpaceSubset conjoinWithDRNegation(ValueSpaceSubset valueSpaceSubset, DatatypeRestriction datatypeRestriction) {
        assert (RDF_XML_LITERAL.equals(datatypeRestriction.getDatatypeURI()));
        assert (datatypeRestriction.getNumberOfFacetRestrictions() == 0);
        return EMPTY;
    }

    @Override
    public boolean isSubsetOf(String subsetDatatypeURI, String supersetDatatypeURI) {
        assert (RDF_XML_LITERAL.equals(subsetDatatypeURI));
        assert (RDF_XML_LITERAL.equals(supersetDatatypeURI));
        return true;
    }

    @Override
    public boolean isDisjointWith(String datatypeURI1, String datatypeURI2) {
        assert (RDF_XML_LITERAL.equals(datatypeURI1));
        assert (RDF_XML_LITERAL.equals(datatypeURI2));
        return false;
    }

    protected static class XMLLiteralNone
    implements ValueSpaceSubset {
        protected XMLLiteralNone() {
        }

        @Override
        public boolean hasCardinalityAtLeast(int number) {
            return number <= 0;
        }

        @Override
        public boolean containsDataValue(Object dataValue) {
            return false;
        }

        @Override
        public void enumerateDataValues(Collection<Object> dataValues) {
        }
    }

    protected static class XMLLiteralAll
    implements ValueSpaceSubset {
        protected XMLLiteralAll() {
        }

        @Override
        public boolean hasCardinalityAtLeast(int number) {
            return true;
        }

        @Override
        public boolean containsDataValue(Object dataValue) {
            return dataValue instanceof XMLLiteral;
        }

        @Override
        public void enumerateDataValues(Collection<Object> dataValues) {
            throw new IllegalStateException("Internal errir: the value space is infinite!");
        }
    }

}

