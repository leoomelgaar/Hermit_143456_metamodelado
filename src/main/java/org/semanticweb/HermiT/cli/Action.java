package org.semanticweb.HermiT.cli;

import java.io.PrintWriter;
import org.semanticweb.HermiT.Reasoner;

interface Action {
    void run(Reasoner var1, StatusOutput var2, PrintWriter var3, boolean var4);
}

