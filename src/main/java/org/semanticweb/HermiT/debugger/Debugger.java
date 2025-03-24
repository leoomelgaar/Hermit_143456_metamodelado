package org.semanticweb.HermiT.debugger;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.debugger.commands.ActiveNodesCommand;
import org.semanticweb.HermiT.debugger.commands.AgainCommand;
import org.semanticweb.HermiT.debugger.commands.BreakpointTimeCommand;
import org.semanticweb.HermiT.debugger.commands.ClearCommand;
import org.semanticweb.HermiT.debugger.commands.ContinueCommand;
import org.semanticweb.HermiT.debugger.commands.DebuggerCommand;
import org.semanticweb.HermiT.debugger.commands.DerivationTreeCommand;
import org.semanticweb.HermiT.debugger.commands.ExitCommand;
import org.semanticweb.HermiT.debugger.commands.ForeverCommand;
import org.semanticweb.HermiT.debugger.commands.HelpCommand;
import org.semanticweb.HermiT.debugger.commands.HistoryCommand;
import org.semanticweb.HermiT.debugger.commands.IsAncestorOfCommand;
import org.semanticweb.HermiT.debugger.commands.ModelStatsCommand;
import org.semanticweb.HermiT.debugger.commands.NodesForCommand;
import org.semanticweb.HermiT.debugger.commands.OriginStatsCommand;
import org.semanticweb.HermiT.debugger.commands.QueryCommand;
import org.semanticweb.HermiT.debugger.commands.ReuseNodeForCommand;
import org.semanticweb.HermiT.debugger.commands.ShowDLClausesCommand;
import org.semanticweb.HermiT.debugger.commands.ShowDescriptionGraphCommand;
import org.semanticweb.HermiT.debugger.commands.ShowExistsCommand;
import org.semanticweb.HermiT.debugger.commands.ShowModelCommand;
import org.semanticweb.HermiT.debugger.commands.ShowNodeCommand;
import org.semanticweb.HermiT.debugger.commands.ShowSubtreeCommand;
import org.semanticweb.HermiT.debugger.commands.SingleStepCommand;
import org.semanticweb.HermiT.debugger.commands.UnprocessedDisjunctionsCommand;
import org.semanticweb.HermiT.debugger.commands.WaitForCommand;
import org.semanticweb.HermiT.model.AtLeastConcept;
import org.semanticweb.HermiT.model.ExistentialConcept;
import org.semanticweb.HermiT.model.ExistsDescriptionGraph;
import org.semanticweb.HermiT.monitor.TableauMonitorForwarder;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.ReasoningTaskDescription;
import org.semanticweb.HermiT.tableau.Tableau;

public class Debugger
extends TableauMonitorForwarder {
    private static final long serialVersionUID = -1061073966460686069L;
    public static final Font s_monospacedFont = new Font("Monospaced", 0, 12);
    protected final Map<String, DebuggerCommand> m_commandsByName = new TreeMap<String, DebuggerCommand>();
    protected final Prefixes m_prefixes;
    protected final DerivationHistory m_derivationHistory;
    protected final ConsoleTextArea m_consoleTextArea;
    protected final JFrame m_mainFrame;
    protected final PrintWriter m_output;
    protected final BufferedReader m_input;
    protected final Set<WaitOption> m_waitOptions;
    protected final Map<Node, NodeCreationInfo> m_nodeCreationInfos;
    protected Node m_lastExistentialNode;
    protected ExistentialConcept m_lastExistentialConcept;
    protected Tableau m_tableau;
    protected String m_lastCommand;
    protected boolean m_forever;
    protected long m_lastStatusMark;
    protected boolean m_singlestep;
    protected boolean m_inMainLoop;
    protected int m_breakpointTime;
    protected int m_currentIteration;

    public Debugger(Prefixes prefixes, boolean historyOn) {
        super(new DerivationHistory());
        this.registerCommands();
        this.m_prefixes = prefixes;
        this.m_derivationHistory = (DerivationHistory)this.m_forwardingTargetMonitor;
        this.m_consoleTextArea = new ConsoleTextArea();
        this.m_consoleTextArea.setFont(s_monospacedFont);
        this.m_output = new PrintWriter(this.m_consoleTextArea.getWriter());
        this.m_input = new BufferedReader(this.m_consoleTextArea.getReader());
        JScrollPane scrollPane = new JScrollPane(this.m_consoleTextArea);
        scrollPane.setPreferredSize(new Dimension(800, 300));
        this.m_mainFrame = new JFrame("HermiT Debugger");
        this.m_mainFrame.setDefaultCloseOperation(3);
        this.m_mainFrame.setContentPane(scrollPane);
        this.m_mainFrame.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension preferredSize = this.m_mainFrame.getPreferredSize();
        this.m_mainFrame.setLocation((screenSize.width - preferredSize.width) / 2, screenSize.height - 100 - preferredSize.height);
        this.m_forwardingOn = historyOn;
        this.m_waitOptions = new HashSet<WaitOption>();
        this.m_nodeCreationInfos = new HashMap<Node, NodeCreationInfo>();
        this.m_forever = false;
        this.m_singlestep = false;
        this.m_breakpointTime = 30000;
        this.m_mainFrame.setVisible(true);
        this.m_output.println("Good morning Dr. Chandra. This is HAL. I'm ready for my first lesson.");
        this.m_output.println("Derivation history is " + (this.m_forwardingOn ? "on" : "off") + ".");
    }

    protected void registerCommands() {
        this.registerCommand(new ActiveNodesCommand(this));
        this.registerCommand(new AgainCommand(this));
        this.registerCommand(new BreakpointTimeCommand(this));
        this.registerCommand(new ClearCommand(this));
        this.registerCommand(new ContinueCommand(this));
        this.registerCommand(new DerivationTreeCommand(this));
        this.registerCommand(new ExitCommand(this));
        this.registerCommand(new ForeverCommand(this));
        this.registerCommand(new HelpCommand(this));
        this.registerCommand(new HistoryCommand(this));
        this.registerCommand(new IsAncestorOfCommand(this));
        this.registerCommand(new ModelStatsCommand(this));
        this.registerCommand(new NodesForCommand(this));
        this.registerCommand(new OriginStatsCommand(this));
        this.registerCommand(new QueryCommand(this));
        this.registerCommand(new ReuseNodeForCommand(this));
        this.registerCommand(new ShowDescriptionGraphCommand(this));
        this.registerCommand(new ShowDLClausesCommand(this));
        this.registerCommand(new ShowExistsCommand(this));
        this.registerCommand(new ShowModelCommand(this));
        this.registerCommand(new ShowNodeCommand(this));
        this.registerCommand(new ShowSubtreeCommand(this));
        this.registerCommand(new SingleStepCommand(this));
        this.registerCommand(new UnprocessedDisjunctionsCommand(this));
        this.registerCommand(new WaitForCommand(this));
    }

    protected void registerCommand(DebuggerCommand command) {
        this.m_commandsByName.put(command.getCommandName().toLowerCase(), command);
    }

    public Map<String, DebuggerCommand> getDebuggerCommands() {
        return Collections.unmodifiableMap(this.m_commandsByName);
    }

    public Tableau getTableau() {
        return this.m_tableau;
    }

    public PrintWriter getOutput() {
        return this.m_output;
    }

    public JFrame getMainFrame() {
        return this.m_mainFrame;
    }

    public String getLastCommand() {
        return this.m_lastCommand;
    }

    public ConsoleTextArea getConsoleTextArea() {
        return this.m_consoleTextArea;
    }

    public Prefixes getPrefixes() {
        return this.m_prefixes;
    }

    public DerivationHistory getDerivationHistory() {
        return this.m_derivationHistory;
    }

    public NodeCreationInfo getNodeCreationInfo(Node node) {
        return this.m_nodeCreationInfos.get(node);
    }

    public void setBreakpointTime(int time) {
        this.m_breakpointTime = time;
    }

    public void setInMainLoop(boolean inMainLoop) {
        this.m_inMainLoop = inMainLoop;
    }

    public void setForever(boolean forever) {
        this.m_forever = forever;
    }

    public void setSinglestep(boolean singlestep) {
        this.m_singlestep = singlestep;
    }

    public boolean addWaitOption(WaitOption option) {
        return this.m_waitOptions.add(option);
    }

    public boolean removeWaitOption(WaitOption option) {
        return this.m_waitOptions.remove(option);
    }

    public DebuggerCommand getCommand(String commandName) {
        return this.m_commandsByName.get(commandName.toLowerCase());
    }

    public void mainLoop() {
        try {
            this.m_inMainLoop = true;
            while (this.m_inMainLoop) {
                this.m_output.print("> ");
                String commandLine = this.m_input.readLine();
                if (commandLine == null) continue;
                commandLine = commandLine.trim();
                this.processCommandLine(commandLine);
            }
            this.m_output.flush();
        }
        catch (IOException commandLine) {
            // empty catch block
        }
        this.m_lastStatusMark = System.currentTimeMillis();
    }

    public void processCommandLine(String commandLine) {
        String[] parsedCommand = this.parse(commandLine);
        String commandName = parsedCommand[0];
        DebuggerCommand command = this.getCommand(commandName);
        if (command == null) {
            this.m_output.println("Unknown command '" + commandName + "'.");
        } else {
            command.execute(parsedCommand);
            if (!(command instanceof AgainCommand)) {
                this.m_lastCommand = commandLine;
            }
        }
    }

    protected String[] parse(String command) {
        command = command.trim();
        ArrayList<String> arguments = new ArrayList<String>();
        int firstChar = 0;
        int nextSpace = command.indexOf(32);
        while (nextSpace != -1) {
            arguments.add(command.substring(firstChar, nextSpace));
            for (firstChar = nextSpace; firstChar < command.length() && command.charAt(firstChar) == ' '; ++firstChar) {
            }
            nextSpace = command.indexOf(32, firstChar);
        }
        arguments.add(command.substring(firstChar));
        String[] result = new String[arguments.size()];
        arguments.toArray(result);
        return result;
    }

    protected void printState() {
        int numberOfNodes = 0;
        int inactiveNodes = 0;
        int blockedNodes = 0;
        int nodesWithExistentials = 0;
        int pendingExistentials = 0;
        for (Node node = this.m_tableau.getFirstTableauNode(); node != null; node = node.getNextTableauNode()) {
            ++numberOfNodes;
            if (!node.isActive()) {
                ++inactiveNodes;
                continue;
            }
            if (node.isBlocked()) {
                ++blockedNodes;
                continue;
            }
            if (node.hasUnprocessedExistentials()) {
                ++nodesWithExistentials;
            }
            pendingExistentials += node.getUnprocessedExistentials().size();
        }
        this.m_output.println("Nodes: " + numberOfNodes + "  Inactive nodes: " + inactiveNodes + "  Blocked nodes: " + blockedNodes + "  Nodes with exists: " + nodesWithExistentials + "  Pending existentials: " + pendingExistentials);
    }

    @Override
    public void setTableau(Tableau tableau) {
        super.setTableau(tableau);
        this.m_tableau = tableau;
    }

    @Override
    public void isSatisfiableStarted(ReasoningTaskDescription reasoningTaskDescription) {
        super.isSatisfiableStarted(reasoningTaskDescription);
        this.m_output.println("Reasoning task started: " + reasoningTaskDescription.getTaskDescription(this.m_prefixes));
        this.mainLoop();
    }

    @Override
    public void isSatisfiableFinished(ReasoningTaskDescription reasoningTaskDescription, boolean result) {
        super.isSatisfiableFinished(reasoningTaskDescription, result);
        if (reasoningTaskDescription.flipSatisfiabilityResult()) {
            result = !result;
        }
        this.m_output.println("Reasoning task finished: " + (result ? "true" : "false"));
        this.mainLoop();
    }

    @Override
    public void tableauCleared() {
        super.tableauCleared();
        this.m_nodeCreationInfos.clear();
        this.m_lastExistentialNode = null;
        this.m_lastExistentialConcept = null;
    }

    @Override
    public void saturateStarted() {
        super.saturateStarted();
        this.m_currentIteration = 0;
        if (this.m_singlestep) {
            this.m_output.println("Saturation starting...");
            this.mainLoop();
        }
    }

    @Override
    public void iterationStarted() {
        super.iterationStarted();
        ++this.m_currentIteration;
        if (this.m_singlestep) {
            this.m_output.println("Iteration " + this.m_currentIteration + " starts...");
            this.mainLoop();
        }
    }

    @Override
    public void iterationFinished() {
        super.iterationFinished();
        if (this.m_singlestep) {
            this.m_output.println("Iteration " + this.m_currentIteration + " finished...");
        }
        if (System.currentTimeMillis() - this.m_lastStatusMark > (long)this.m_breakpointTime) {
            this.printState();
            if (!this.m_forever) {
                this.mainLoop();
            }
            this.m_lastStatusMark = System.currentTimeMillis();
        }
    }

    @Override
    public void clashDetected() {
        super.clashDetected();
        if (this.m_waitOptions.contains(WaitOption.CLASH)) {
            this.m_forever = false;
            this.m_output.println("Clash detected.");
            this.mainLoop();
        }
    }

    @Override
    public void mergeStarted(Node mergeFrom, Node mergeInto) {
        super.mergeStarted(mergeFrom, mergeInto);
        if (this.m_waitOptions.contains(WaitOption.MERGE)) {
            this.m_forever = false;
            this.m_output.println("Node '" + mergeFrom.getNodeID() + "' will be merged into node '" + mergeInto.getNodeID() + "'.");
            this.mainLoop();
        }
    }

    @Override
    public void existentialExpansionStarted(ExistentialConcept existentialConcept, Node forNode) {
        super.existentialExpansionStarted(existentialConcept, forNode);
        this.m_lastExistentialNode = forNode;
        this.m_lastExistentialConcept = existentialConcept;
    }

    @Override
    public void existentialExpansionFinished(ExistentialConcept existentialConcept, Node forNode) {
        super.existentialExpansionFinished(existentialConcept, forNode);
        this.m_lastExistentialNode = null;
        this.m_lastExistentialConcept = null;
        if (existentialConcept instanceof ExistsDescriptionGraph && this.m_waitOptions.contains(WaitOption.GRAPH_EXPANSION) || existentialConcept instanceof AtLeastConcept && this.m_waitOptions.contains(WaitOption.EXISTENTIAL_EXPANSION)) {
            this.m_forever = false;
            this.m_output.println(existentialConcept.toString(this.m_prefixes) + " expanded for node " + forNode.getNodeID());
            this.mainLoop();
        }
    }

    @Override
    public void nodeCreated(Node node) {
        super.nodeCreated(node);
        this.m_nodeCreationInfos.put(node, new NodeCreationInfo(this.m_lastExistentialNode, this.m_lastExistentialConcept));
        if (this.m_lastExistentialNode != null) {
            this.m_nodeCreationInfos.get(this.m_lastExistentialNode).m_children.add(node);
        }
    }

    @Override
    public void nodeDestroyed(Node node) {
        super.nodeDestroyed(node);
        NodeCreationInfo nodeCreationInfo = this.m_nodeCreationInfos.remove(node);
        if (nodeCreationInfo.m_createdByNode != null) {
            this.m_nodeCreationInfos.get(nodeCreationInfo.m_createdByNode).m_children.remove(node);
        }
    }

    @Override
    public void datatypeCheckingStarted() {
        super.datatypeCheckingStarted();
        if (this.m_waitOptions.contains(WaitOption.DATATYPE_CHECKING)) {
            this.m_forever = false;
            this.m_output.println("Will check whether the datatype constraints are satisfiable.");
            this.mainLoop();
        }
    }

    @Override
    public void blockingValidationStarted() {
        super.blockingValidationStarted();
        if (this.m_waitOptions.contains(WaitOption.BLOCKING_VALIDATION_STARTED)) {
            this.m_forever = false;
            this.m_output.println("Will validate blocking.");
            this.mainLoop();
        }
    }

    @Override
    public void blockingValidationFinished(int noInvalidlyBlocked) {
        super.blockingValidationFinished(noInvalidlyBlocked);
        if (this.m_waitOptions.contains(WaitOption.BLOCKING_VALIDATION_FINISHED)) {
            this.m_forever = false;
            this.m_output.println("Blocking validated.");
            this.mainLoop();
        }
    }

    public static class NodeCreationInfo {
        public final Node m_createdByNode;
        public final ExistentialConcept m_createdByExistential;
        public final List<Node> m_children;

        public NodeCreationInfo(Node createdByNode, ExistentialConcept createdByExistential) {
            this.m_createdByNode = createdByNode;
            this.m_createdByExistential = createdByExistential;
            this.m_children = new ArrayList<Node>(4);
        }
    }

    public enum WaitOption {
        GRAPH_EXPANSION,
        EXISTENTIAL_EXPANSION,
        CLASH,
        MERGE,
        DATATYPE_CHECKING,
        BLOCKING_VALIDATION_STARTED,
        BLOCKING_VALIDATION_FINISHED

    }

}

