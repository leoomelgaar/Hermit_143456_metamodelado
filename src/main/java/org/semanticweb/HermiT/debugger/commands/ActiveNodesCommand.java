package org.semanticweb.HermiT.debugger.commands;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import org.semanticweb.HermiT.debugger.Debugger;
import org.semanticweb.HermiT.tableau.Node;

public class ActiveNodesCommand
extends AbstractCommand {
    public ActiveNodesCommand(Debugger debugger) {
        super(debugger);
    }

    @Override
    public String getCommandName() {
        return "activeNodes";
    }

    @Override
    public String[] getDescription() {
        return new String[]{"", "shows all active nodes"};
    }

    @Override
    public void printHelp(PrintWriter writer) {
        writer.println("usage: activeNodes");
        writer.println("    Prints list of all active (non-blocked) nodes in the current model.");
    }

    @Override
    public void execute(String[] args) {
        int numberOfNodes = 0;
        CharArrayWriter buffer = new CharArrayWriter();
        PrintWriter writer = new PrintWriter(buffer);
        writer.println("===========================================");
        writer.println("      ID");
        writer.println("===========================================");
        for (Node node = this.m_debugger.getTableau().getFirstTableauNode(); node != null; node = node.getNextTableauNode()) {
            if (node.isBlocked()) continue;
            ++numberOfNodes;
            writer.print("  ");
            writer.println(node.getNodeID());
        }
        writer.flush();
        this.showTextInWindow("Active nodes (" + numberOfNodes + "):" + buffer, "Active nodes");
        this.selectConsoleWindow();
    }
}

