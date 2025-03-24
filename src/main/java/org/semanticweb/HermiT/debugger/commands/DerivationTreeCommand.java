package org.semanticweb.HermiT.debugger.commands;

import java.io.PrintWriter;
import org.semanticweb.HermiT.debugger.Debugger;
import org.semanticweb.HermiT.debugger.DerivationHistory;
import org.semanticweb.HermiT.debugger.DerivationViewer;
import org.semanticweb.HermiT.tableau.Node;

public class DerivationTreeCommand
extends AbstractCommand {
    public DerivationTreeCommand(Debugger debugger) {
        super(debugger);
    }

    @Override
    public String getCommandName() {
        return "dertree";
    }

    @Override
    public String[] getDescription() {
        return new String[]{"clash", "shows the derivation tree for the clash", "predicate [nodeID]+", "shows the derivation tree for the given atom"};
    }

    @Override
    public void printHelp(PrintWriter writer) {
        writer.println("usage: dertree clash");
        writer.println("    Shows the derivation tree for the clash.");
        writer.println("usage: dertree predicate [nodeID]+");
        writer.println("    Shows the derivation tree for the given atom.");
        writer.println("    yellow: DL clause application");
        writer.println("    cyan: disjunct application (choose and apply a disjunct)");
        writer.println("    blue: merged two nodes");
        writer.println("    dark grey: description graph checking");
        writer.println("    black: clash");
        writer.println("    red: existential expansion");
        writer.println("    magenta: base/given fact");
    }

    @Override
    public void execute(String[] args) {
        Object[] tuple;
        if (args.length < 2) {
            this.m_debugger.getOutput().println("The specification of the predicate is missing.");
            return;
        }
        String predicate = args[1];
        if ("clash".equalsIgnoreCase(predicate)) {
            tuple = new Object[]{};
        } else {
            tuple = new Object[args.length - 1];
            try {
                tuple[0] = this.getDLPredicate(predicate);
            }
            catch (Exception e) {
                this.m_debugger.getOutput().println("Invalid predicate '" + predicate + "':" + e.getMessage());
            }
            if (tuple[0] == null) {
                this.m_debugger.getOutput().println("Invalid predicate '" + predicate + "'.");
                return;
            }
        }
        for (int index = 1; index < tuple.length; ++index) {
            int nodeID;
            try {
                nodeID = Integer.parseInt(args[index + 1]);
            }
            catch (NumberFormatException e) {
                this.m_debugger.getOutput().println("Invalid ID of the node at argument " + index + ". " + e.getMessage());
                return;
            }
            Node node = this.m_debugger.getTableau().getNode(nodeID);
            if (node == null) {
                this.m_debugger.getOutput().println("Node with ID '" + nodeID + "' not found.");
                return;
            }
            tuple[index] = node;
        }
        DerivationHistory.Atom atom = this.m_debugger.getDerivationHistory().getAtom(tuple);
        if (atom != null) {
            new DerivationViewer(this.m_debugger.getPrefixes(), atom);
            this.selectConsoleWindow();
        } else {
            this.m_debugger.getOutput().println("Atom not found.");
        }
    }
}

