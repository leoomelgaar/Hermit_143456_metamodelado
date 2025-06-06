package org.semanticweb.HermiT.debugger.commands;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.semanticweb.HermiT.debugger.Debugger;
import org.semanticweb.HermiT.debugger.Printing;
import org.semanticweb.HermiT.model.AtLeastConcept;
import org.semanticweb.HermiT.model.ExistentialConcept;
import org.semanticweb.HermiT.tableau.Node;

public class SubtreeViewer
extends JFrame {
    protected final Debugger m_debugger;
    protected final SubtreeTreeModel m_subtreeTreeModel;
    protected final JTextArea m_nodeInfoTextArea;
    protected final JTree m_tableauTree;
    protected final JTextField m_nodeIDField;

    public SubtreeViewer(Debugger debugger, Node rootNode) {
        super("Subtree for node " + rootNode.getNodeID());
        this.setDefaultCloseOperation(2);
        this.m_debugger = debugger;
        this.m_subtreeTreeModel = new SubtreeTreeModel(debugger, rootNode);
        this.m_tableauTree = new JTree(this.m_subtreeTreeModel);
        this.m_tableauTree.setLargeModel(true);
        this.m_tableauTree.setShowsRootHandles(true);
        this.m_tableauTree.setCellRenderer(new NodeCellRenderer(debugger));
        this.m_tableauTree.addTreeSelectionListener(new TreeSelectionListener(){

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath selectionPath = SubtreeViewer.this.m_tableauTree.getSelectionPath();
                if (selectionPath == null) {
                    SubtreeViewer.this.showNodeLabels(null);
                } else {
                    SubtreeViewer.this.showNodeLabels((Node)selectionPath.getLastPathComponent());
                }
            }
        });
        this.m_nodeInfoTextArea = new JTextArea();
        this.m_nodeInfoTextArea.setFont(Debugger.s_monospacedFont);
        JScrollPane modelScrollPane = new JScrollPane(this.m_tableauTree);
        modelScrollPane.setPreferredSize(new Dimension(600, 400));
        JScrollPane nodeInfoScrollPane = new JScrollPane(this.m_nodeInfoTextArea);
        nodeInfoScrollPane.setPreferredSize(new Dimension(400, 400));
        JSplitPane mainSplit = new JSplitPane(1, modelScrollPane, nodeInfoScrollPane);
        JPanel commandsPanel = new JPanel(new FlowLayout(0, 5, 3));
        commandsPanel.add(new JLabel("Node ID:"));
        this.m_nodeIDField = new JTextField();
        this.m_nodeIDField.setPreferredSize(new Dimension(200, this.m_nodeIDField.getPreferredSize().height));
        commandsPanel.add(this.m_nodeIDField);
        JButton button = new JButton("Search");
        button.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                int nodeID;
                String nodeIDText = SubtreeViewer.this.m_nodeIDField.getText();
                try {
                    nodeID = Integer.parseInt(nodeIDText);
                }
                catch (NumberFormatException error) {
                    JOptionPane.showMessageDialog(SubtreeViewer.this, "Invalid node ID '" + nodeIDText + "'. " + error.getMessage());
                    return;
                }
                Node node = SubtreeViewer.this.m_debugger.getTableau().getNode(nodeID);
                if (node == null) {
                    JOptionPane.showMessageDialog(SubtreeViewer.this, "Node with ID " + nodeID + " cannot be found.");
                    return;
                }
                SubtreeViewer.this.findNode(node);
            }
        });
        this.getRootPane().setDefaultButton(button);
        commandsPanel.add(button);
        button = new JButton("Refresh");
        button.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                SubtreeViewer.this.refresh();
            }
        });
        commandsPanel.add(button);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(mainSplit, "Center");
        mainPanel.add(commandsPanel, "South");
        this.setContentPane(mainPanel);
        this.pack();
        this.setLocation(200, 200);
        this.setVisible(true);
        this.m_nodeIDField.requestFocusInWindow();
    }

    public void refresh() {
        this.m_subtreeTreeModel.refresh();
    }

    public void findNode(Node node) {
        ArrayList<Node> pathToRoot = new ArrayList<Node>();
        Node currentNode = node;
        while (currentNode != null && currentNode != this.m_subtreeTreeModel.getRoot()) {
            pathToRoot.add(currentNode);
            currentNode = this.m_debugger.getNodeCreationInfo(currentNode).m_createdByNode;
        }
        if (currentNode == null) {
            JOptionPane.showMessageDialog(this, "Node with ID " + node.getNodeID() + " is not present in the shown subtree.");
            return;
        }
        MyTreePath treePath = new MyTreePath(null, this.m_subtreeTreeModel.getRoot());
        for (int index = pathToRoot.size() - 1; index >= 0; --index) {
            treePath = new MyTreePath(treePath, pathToRoot.get(index));
        }
        this.m_tableauTree.expandPath(treePath);
        this.m_tableauTree.setSelectionPath(treePath);
        this.m_tableauTree.scrollPathToVisible(treePath);
    }

    public void showNodeLabels(Node node) {
        if (node == null) {
            this.m_nodeInfoTextArea.setText("");
        } else {
            CharArrayWriter buffer = new CharArrayWriter();
            PrintWriter writer = new PrintWriter(buffer);
            Printing.printNodeData(this.m_debugger, node, writer);
            writer.flush();
            this.m_nodeInfoTextArea.setText(buffer.toString());
            this.m_nodeInfoTextArea.select(0, 0);
        }
    }

    protected static class MyTreePath
    extends TreePath {
        public MyTreePath(TreePath treePath, Object object) {
            super(treePath, object);
        }
    }

    protected static class DotIcon
    implements Icon {
        protected final Color m_color;

        public DotIcon(Color color) {
            this.m_color = color;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color oldColor = g.getColor();
            g.setColor(this.m_color);
            g.fillOval(x + 2, y + 2, x + 12, y + 12);
            g.setColor(oldColor);
        }
    }

    protected static class NodeCellRenderer
    extends DefaultTreeCellRenderer {
        protected static final Icon NOT_ACTIVE_ICON = new DotIcon(Color.LIGHT_GRAY);
        protected static final Icon BLOCKED_ICON = new DotIcon(Color.CYAN);
        protected static final Icon WITH_EXISTENTIALS_ICON = new DotIcon(Color.RED);
        protected static final Icon NI_NODE_ICON = new DotIcon(Color.BLACK);
        protected static final Icon NAMED_NODE_ICON = new DotIcon(Color.DARK_GRAY);
        protected static final Icon TREE_NODE_ICON = new DotIcon(Color.GREEN);
        protected static final Icon GRAPH_NODE_ICON = new DotIcon(Color.MAGENTA);
        protected static final Icon CONCRETE_NODE_ICON = new DotIcon(Color.BLUE);
        protected final Debugger m_debugger;

        public NodeCellRenderer(Debugger debugger) {
            this.m_debugger = debugger;
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean s, boolean expanded, boolean leaf, int row, boolean focus) {
            Node node = (Node)value;
            StringBuffer buffer = new StringBuffer();
            ExistentialConcept existentialConcept = this.m_debugger.getNodeCreationInfo(node).m_createdByExistential;
            if (existentialConcept == null) {
                buffer.append(node.getNodeID());
                buffer.append(":(root)");
            } else if (existentialConcept instanceof AtLeastConcept) {
                AtLeastConcept atLeastConcept = (AtLeastConcept)existentialConcept;
                buffer.append(atLeastConcept.getOnRole().toString(this.m_debugger.getPrefixes()));
                buffer.append("  -->  ");
                buffer.append(node.getNodeID());
                buffer.append(":[");
                buffer.append(atLeastConcept.getToConcept().toString(this.m_debugger.getPrefixes()));
                buffer.append("]");
            }
            super.getTreeCellRendererComponent(tree, buffer.toString(), s, expanded, leaf, row, focus);
            if (!node.isActive()) {
                this.setIcon(NOT_ACTIVE_ICON);
            } else if (node.isBlocked()) {
                this.setIcon(BLOCKED_ICON);
            } else if (node.hasUnprocessedExistentials()) {
                this.setIcon(WITH_EXISTENTIALS_ICON);
            } else {
                switch (node.getNodeType()) {
                    case NAMED_NODE: {
                        this.setIcon(NAMED_NODE_ICON);
                        break;
                    }
                    case TREE_NODE: {
                        this.setIcon(TREE_NODE_ICON);
                        break;
                    }
                    case GRAPH_NODE: {
                        this.setIcon(GRAPH_NODE_ICON);
                        break;
                    }
                    case NI_NODE: {
                        this.setIcon(NI_NODE_ICON);
                        break;
                    }
                    default: {
                        this.setIcon(CONCRETE_NODE_ICON);
                    }
                }
            }
            return this;
        }
    }

    protected static class SubtreeTreeModel
    implements TreeModel {
        protected final EventListenerList m_eventListeners = new EventListenerList();
        protected final Debugger m_debugger;
        protected final Node m_root;

        public SubtreeTreeModel(Debugger debugger, Node root) {
            this.m_debugger = debugger;
            this.m_root = root;
        }

        @Override
        public void addTreeModelListener(TreeModelListener listener) {
            this.m_eventListeners.add(TreeModelListener.class, listener);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener listener) {
            this.m_eventListeners.remove(TreeModelListener.class, listener);
        }

        @Override
        public Node getChild(Object parent, int index) {
            Debugger.NodeCreationInfo nodeCreationInfo = null;
            if (parent instanceof Node) {
                nodeCreationInfo = this.m_debugger.getNodeCreationInfo((Node)parent);
            }
            if (nodeCreationInfo == null) {
                return null;
            }
            return nodeCreationInfo.m_children.get(index);
        }

        @Override
        public int getChildCount(Object parent) {
            Debugger.NodeCreationInfo nodeCreationInfo = null;
            if (parent instanceof Node) {
                nodeCreationInfo = this.m_debugger.getNodeCreationInfo((Node)parent);
            }
            if (nodeCreationInfo == null) {
                return 0;
            }
            return nodeCreationInfo.m_children.size();
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            Debugger.NodeCreationInfo nodeCreationInfo = null;
            if (parent instanceof Node) {
                nodeCreationInfo = this.m_debugger.getNodeCreationInfo((Node)parent);
            }
            if (nodeCreationInfo == null) {
                return -1;
            }
            return nodeCreationInfo.m_children.indexOf(child);
        }

        @Override
        public Object getRoot() {
            return this.m_root;
        }

        @Override
        public boolean isLeaf(Object node) {
            return this.getChildCount(node) == 0;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
        }

        public void refresh() {
            Object[] listeners = this.m_eventListeners.getListenerList();
            TreeModelEvent e = new TreeModelEvent(this, new Object[]{this.getRoot()});
            for (Object listener : listeners) {
                if (!(listener instanceof TreeModelListener)) continue;
                ((TreeModelListener)listener).treeStructureChanged(e);
            }
        }
    }

}

