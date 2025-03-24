/*
 * Decompiled with CFR 0.137.
 */
package org.semanticweb.HermiT.datalog;

import org.semanticweb.HermiT.model.Term;

public interface QueryResultCollector {
    void processResult(ConjunctiveQuery var1, Term[] var2);
}

