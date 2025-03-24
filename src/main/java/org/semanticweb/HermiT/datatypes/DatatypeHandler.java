package org.semanticweb.HermiT.datatypes;

import java.util.Set;
import org.semanticweb.HermiT.model.DatatypeRestriction;

public interface DatatypeHandler {
    Set<String> getManagedDatatypeURIs();

    Object parseLiteral(String var1, String var2) throws MalformedLiteralException;

    void validateDatatypeRestriction(DatatypeRestriction var1) throws UnsupportedFacetException;

    ValueSpaceSubset createValueSpaceSubset(DatatypeRestriction var1);

    ValueSpaceSubset conjoinWithDR(ValueSpaceSubset var1, DatatypeRestriction var2);

    ValueSpaceSubset conjoinWithDRNegation(ValueSpaceSubset var1, DatatypeRestriction var2);

    boolean isSubsetOf(String var1, String var2);

    boolean isDisjointWith(String var1, String var2);
}

