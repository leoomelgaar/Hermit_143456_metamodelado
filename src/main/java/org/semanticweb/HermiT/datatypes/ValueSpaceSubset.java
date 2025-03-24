package org.semanticweb.HermiT.datatypes;

import java.util.Collection;

public interface ValueSpaceSubset {
    boolean hasCardinalityAtLeast(int var1);

    boolean containsDataValue(Object var1);

    void enumerateDataValues(Collection<Object> var1);
}

