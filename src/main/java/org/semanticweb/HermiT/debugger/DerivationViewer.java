package org.semanticweb.HermiT.debugger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.semanticweb.HermiT.Prefixes;

public class DerivationViewer
extends JFrame {
    protected final Prefixes m_prefixes;
    protected final DerivationTreeTreeModel m_derivationTreeTreeModel;
    protected final JTree m_derivationTree;
    protected static final Font s_font = new Font("Serif", 1, 11);
    protected static final Icon DLCLAUSE_APPLICATION_ICON = new TextIcon(Color.YELLOW, Color.BLACK, "R", s_font);
    protected static final Icon DISJUNCT_APPLICATION_ICON = new TextIcon(Color.CYAN, Color.BLACK, "D", s_font);
    protected static final Icon MERGING_ICON = new TextIcon(Color.BLUE, Color.WHITE, "M", s_font);
    protected static final Icon GRAPH_CHECKING_ICON = new TextIcon(Color.DARK_GRAY, Color.WHITE, "G", s_font);
    protected static final Icon CLASH_DETECTION_ICON = new TextIcon(Color.BLACK, Color.WHITE, "C", s_font);
    protected static final Icon EXISTENTIAL_EXPANSION_ICON = new TextIcon(Color.RED, Color.WHITE, "E", s_font);
    protected static final Icon BASE_FACT_ICON = new TextIcon(Color.MAGENTA, Color.WHITE, "B", s_font);

    public DerivationViewer(Prefixes prefixes, DerivationHistory.Fact root) {
        super("Derivation tree for " + root.toString(prefixes));
        this.setDefaultCloseOperation(2);
        this.m_prefixes = prefixes;
        this.m_derivationTreeTreeModel = new DerivationTreeTreeModel(root);
        this.m_derivationTree = new JTree(this.m_derivationTreeTreeModel);
        this.m_derivationTree.setLargeModel(true);
        this.m_derivationTree.setShowsRootHandles(true);
        this.m_derivationTree.setCellRenderer(new DerivationTreeCellRenderer());
        JScrollPane scrollPane = new JScrollPane(this.m_derivationTree);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        JButton button = new JButton("Refresh");
        button.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                DerivationViewer.this.refresh();
            }
        });
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, "Center");
        panel.add(button, "South");
        this.setContentPane(panel);
        this.getRootPane().setDefaultButton(button);
        this.pack();
        this.setLocation(150, 150);
        this.setVisible(true);
    }

    public void refresh() {
        this.m_derivationTreeTreeModel.refresh();
    }

    protected class DerivationTreeCellRenderer
    extends DefaultTreeCellRenderer {
        protected DerivationTreeCellRenderer() {
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean s, boolean expanded, boolean leaf, int row, boolean focus) {
            DerivationHistory.Fact fact = (DerivationHistory.Fact)value;
            DerivationHistory.Derivation derivation = fact.getDerivation();
            String text = fact.toString(DerivationViewer.this.m_prefixes) +
                    derivation.toString(DerivationViewer.this.m_prefixes);
            super.getTreeCellRendererComponent(tree, text, s, expanded, leaf, row, focus);
            if (derivation instanceof DerivationHistory.DLClauseApplication) {
                this.setIcon(DerivationViewer.DLCLAUSE_APPLICATION_ICON);
            } else if (derivation instanceof DerivationHistory.DisjunctApplication) {
                this.setIcon(DerivationViewer.DISJUNCT_APPLICATION_ICON);
            } else if (derivation instanceof DerivationHistory.Merging) {
                this.setIcon(DerivationViewer.MERGING_ICON);
            } else if (derivation instanceof DerivationHistory.GraphChecking) {
                this.setIcon(DerivationViewer.GRAPH_CHECKING_ICON);
            } else if (derivation instanceof DerivationHistory.ClashDetection) {
                this.setIcon(DerivationViewer.CLASH_DETECTION_ICON);
            } else if (derivation instanceof DerivationHistory.ExistentialExpansion) {
                this.setIcon(DerivationViewer.EXISTENTIAL_EXPANSION_ICON);
            } else if (derivation instanceof DerivationHistory.BaseFact) {
                this.setIcon(DerivationViewer.BASE_FACT_ICON);
            } else {
                this.setIcon(null);
            }
            return this;
        }
    }

    protected static class TextIcon
    implements Icon,
    Serializable {
        private static final long serialVersionUID = 2955881594360729470L;
        protected static final int WIDTH16 = 16;
        protected static final int HEIGHT16 = 16;
        protected final Color m_background;
        protected final Color m_foreground;
        protected final String m_text;
        protected final Font m_font;

        public TextIcon(Color background, Color foreground, String text, Font font) {
            this.m_background = background;
            this.m_foreground = foreground;
            this.m_text = text;
            this.m_font = font;
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
            g.setColor(this.m_background);
            g.fillOval(x + 2, y + 2, x + 16 - 2, y + 16 - 2);
            g.setColor(this.m_foreground);
            Font oldFont = g.getFont();
            g.setFont(this.m_font);
            FontMetrics fontMetrics = g.getFontMetrics();
            int textX = x + (16 - fontMetrics.stringWidth(this.m_text)) / 2 + 2;
            int textY = y + (16 + fontMetrics.getAscent() - fontMetrics.getDescent()) / 2;
            g.drawString(this.m_text, textX, textY);
            g.setFont(oldFont);
            g.setColor(oldColor);
        }
    }

    protected static class DerivationTreeTreeModel
    implements TreeModel,
    Serializable {
        private static final long serialVersionUID = 9210217812084186766L;
        protected final EventListenerList m_eventListeners = new EventListenerList();
        protected final DerivationHistory.Fact m_root;

        public DerivationTreeTreeModel(DerivationHistory.Fact root) {
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
        public Object getChild(Object parent, int index) {
            DerivationHistory.Fact parentFact = (DerivationHistory.Fact)parent;
            DerivationHistory.Derivation derivation = parentFact.getDerivation();
            return derivation.getPremise(index);
        }

        @Override
        public int getChildCount(Object parent) {
            DerivationHistory.Fact parentFact = (DerivationHistory.Fact)parent;
            DerivationHistory.Derivation derivation = parentFact.getDerivation();
            return derivation.getNumberOfPremises();
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            DerivationHistory.Fact parentFact = (DerivationHistory.Fact)parent;
            DerivationHistory.Derivation derivation = parentFact.getDerivation();
            for (int index = 0; index < derivation.getNumberOfPremises(); ++index) {
                if (!child.equals(derivation.getPremise(index))) continue;
                return index;
            }
            return -1;
        }

        @Override
        public Object getRoot() {
            return this.m_root;
        }

        @Override
        public boolean isLeaf(Object node) {
            DerivationHistory.Fact nodeFact = (DerivationHistory.Fact)node;
            DerivationHistory.Derivation derivation = nodeFact.getDerivation();
            return derivation.getNumberOfPremises() == 0;
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

