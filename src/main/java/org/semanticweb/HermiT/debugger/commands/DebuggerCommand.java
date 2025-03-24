package org.semanticweb.HermiT.debugger.commands;

import java.io.PrintWriter;

public interface DebuggerCommand {
    String getCommandName();

    String[] getDescription();

    void printHelp(PrintWriter var1);

    void execute(String[] var1);
}

