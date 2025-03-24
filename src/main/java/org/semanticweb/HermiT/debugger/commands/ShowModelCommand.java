package org.semanticweb.HermiT.debugger.commands;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;
import org.semanticweb.HermiT.debugger.Debugger;
import org.semanticweb.HermiT.debugger.Printing;
import org.semanticweb.HermiT.model.Concept;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.NegatedAtomicRole;
import org.semanticweb.HermiT.tableau.ExtensionTable;
import org.semanticweb.HermiT.tableau.Node;

public class ShowModelCommand
extends AbstractCommand {
    public ShowModelCommand(Debugger debugger) {
        super(debugger);
    }

    @Override
    public String getCommandName() {
        return "showModel";
    }

    @Override
    public String[] getDescription() {
        return new String[]{"", "prints all assertions", "predicate", "prints all assertions for the given predicate"};
    }

    @Override
    public void printHelp(PrintWriter writer) {
        writer.println("usage: showModel");
        writer.println("    Prints the entire current model.");
        writer.println("usage: showModel predicate");
        writer.println("    Prints all assertions containing the supplied predicate.");
    }

    @Override
    public void execute(String[] args) {
        String title;
        TreeSet<Object[]> facts = new TreeSet<Object[]>(Printing.FactComparator.INSTANCE);
        if (args.length < 2) {
            for (ExtensionTable extensionTable : this.m_debugger.getTableau().getExtensionManager().getExtensionTables()) {
                ExtensionTable.Retrieval retrieval = extensionTable.createRetrieval(new boolean[extensionTable.getArity()], ExtensionTable.View.TOTAL);
                this.loadFacts(facts, retrieval);
            }
            title = "Current model";
        } else {
            DLPredicate dlPredicate = null;
            try {
                dlPredicate = this.getDLPredicate(args[1]);
            }
            catch (Exception e) {
                this.m_debugger.getOutput().println(args[1] + " is invalid: " + e.getMessage());
            }
            if (dlPredicate != null) {
                ExtensionTable extensionTable = this.m_debugger.getTableau().getExtensionManager().getExtensionTable(dlPredicate.getArity() + 1);
                boolean[] bindings = new boolean[extensionTable.getArity()];
                bindings[0] = true;
                ExtensionTable.Retrieval retrieval = extensionTable.createRetrieval(bindings, ExtensionTable.View.TOTAL);
                retrieval.getBindingsBuffer()[0] = dlPredicate;
                this.loadFacts(facts, retrieval);
                title = "Assertions containing the predicate '" + this.m_debugger.getPrefixes().abbreviateIRI(dlPredicate.toString()) + "'.";
            } else {
                int nodeID;
                try {
                    nodeID = Integer.parseInt(args[1]);
                }
                catch (NumberFormatException e) {
                    this.m_debugger.getOutput().println("Invalid ID of the node. " + e.getMessage());
                    return;
                }
                Node node = this.m_debugger.getTableau().getNode(nodeID);
                if (node == null) {
                    this.m_debugger.getOutput().println("Node with ID '" + nodeID + "' not found.");
                    return;
                }
                for (ExtensionTable extensionTable : this.m_debugger.getTableau().getExtensionManager().getExtensionTables()) {
                    for (int position = 0; position < extensionTable.getArity(); ++position) {
                        boolean[] bindings = new boolean[extensionTable.getArity()];
                        bindings[position] = true;
                        ExtensionTable.Retrieval retrieval = extensionTable.createRetrieval(bindings, ExtensionTable.View.TOTAL);
                        retrieval.getBindingsBuffer()[position] = node;
                        this.loadFacts(facts, retrieval);
                    }
                }
                title = "Assertions containing node '" + node.getNodeID() + "'.";
            }
        }
        CharArrayWriter buffer = new CharArrayWriter();
        PrintWriter writer = new PrintWriter(buffer);
        Object lastPredicate = null;
        for (Object[] fact : facts) {
            if (lastPredicate != fact[0]) {
                lastPredicate = fact[0];
                writer.println();
            }
            writer.print(' ');
            this.printFact(fact, writer);
            writer.println();
        }
        writer.flush();
        this.showTextInWindow(buffer.toString(), title);
        this.selectConsoleWindow();
    }

    protected void loadFacts(Set<Object[]> facts, ExtensionTable.Retrieval retrieval) {
        retrieval.open();
        while (!retrieval.afterLast()) {
            facts.add(retrieval.getTupleBuffer().clone());
            retrieval.next();
        }
    }

    protected void printFact(Object[] fact, PrintWriter writer) {
        Object dlPredicate = fact[0];
        if (dlPredicate instanceof Concept) {
            writer.print(((Concept)dlPredicate).toString(this.m_debugger.getPrefixes()));
        } else if (dlPredicate instanceof NegatedAtomicRole) {
            writer.print(((NegatedAtomicRole)dlPredicate).toString(this.m_debugger.getPrefixes()));
        } else if (dlPredicate instanceof DLPredicate) {
            writer.print(((DLPredicate)dlPredicate).toString(this.m_debugger.getPrefixes()));
        } else {
            throw new IllegalStateException("Internal error: invalid predicate.");
        }
        writer.print('[');
        for (int position = 1; position < fact.length; ++position) {
            if (position != 1) {
                writer.print(',');
            }
            writer.print(((Node)fact[position]).getNodeID());
        }
        writer.print(']');
    }
}

