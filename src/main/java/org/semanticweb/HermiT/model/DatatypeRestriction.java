package org.semanticweb.HermiT.model;

import org.semanticweb.HermiT.Prefixes;

public class DatatypeRestriction
extends AtomicDataRange {
    private static final long serialVersionUID = 524235536504588458L;
    public static final String[] NO_FACET_URIs = new String[0];
    public static final Constant[] NO_FACET_VALUES = new Constant[0];
    protected final String m_datatypeURI;
    protected final String[] m_facetURIs;
    protected final Constant[] m_facetValues;
    protected static final InterningManager<DatatypeRestriction> s_interningManager = new InterningManager<DatatypeRestriction>(){

        @Override
        protected boolean equal(DatatypeRestriction object1, DatatypeRestriction object2) {
            if (!object1.m_datatypeURI.equals(object2.m_datatypeURI) || object1.m_facetURIs.length != object2.m_facetURIs.length) {
                return false;
            }
            for (int index = object1.m_facetURIs.length - 1; index >= 0; --index) {
                if (this.contains(object2, object1.m_facetURIs[index], object1.m_facetValues[index])) continue;
                return false;
            }
            return true;
        }

        private boolean contains(DatatypeRestriction datatypeRestriction, String facetURI, Object facetValue) {
            for (int i = datatypeRestriction.m_facetURIs.length - 1; i >= 0; --i) {
                if (!datatypeRestriction.m_facetURIs[i].equals(facetURI) || !datatypeRestriction.m_facetValues[i].equals(facetValue)) continue;
                return true;
            }
            return false;
        }

        @Override
        protected int getHashCode(DatatypeRestriction object) {
            int hashCode = object.m_datatypeURI.hashCode();
            for (int index = object.m_facetURIs.length - 1; index >= 0; --index) {
                hashCode += object.m_facetURIs[index].hashCode() + object.m_facetValues[index].hashCode();
            }
            return hashCode;
        }
    };

    public DatatypeRestriction(String datatypeURI, String[] facetURIs, Constant[] facetValues) {
        this.m_datatypeURI = datatypeURI;
        this.m_facetURIs = facetURIs;
        this.m_facetValues = facetValues;
    }

    public String getDatatypeURI() {
        return this.m_datatypeURI;
    }

    public int getNumberOfFacetRestrictions() {
        return this.m_facetURIs.length;
    }

    public String getFacetURI(int index) {
        return this.m_facetURIs[index];
    }

    public Constant getFacetValue(int index) {
        return this.m_facetValues[index];
    }

    @Override
    public LiteralDataRange getNegation() {
        return AtomicNegationDataRange.create(this);
    }

    @Override
    public boolean isAlwaysTrue() {
        return false;
    }

    @Override
    public boolean isAlwaysFalse() {
        return false;
    }

    @Override
    public String toString(Prefixes prefixes) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(prefixes.abbreviateIRI(this.m_datatypeURI));
        if (this.m_facetURIs.length > 0) {
            buffer.append('[');
            for (int index = 0; index < this.m_facetURIs.length; ++index) {
                if (index > 0) {
                    buffer.append(',');
                }
                buffer.append(prefixes.abbreviateIRI(this.m_facetURIs[index]));
                buffer.append('=');
                buffer.append(this.m_facetValues[index].toString(prefixes));
            }
            buffer.append(']');
        }
        return buffer.toString();
    }

    protected Object readResolve() {
        return s_interningManager.intern(this);
    }

    public static DatatypeRestriction create(String datatypeURI, String[] facetURIs, Constant[] facetValues) {
        return s_interningManager.intern(new DatatypeRestriction(datatypeURI, facetURIs, facetValues));
    }

}

