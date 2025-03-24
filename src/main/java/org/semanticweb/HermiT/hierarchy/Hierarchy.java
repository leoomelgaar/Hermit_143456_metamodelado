package org.semanticweb.HermiT.hierarchy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Hierarchy<E> {
    protected final HierarchyNode<E> m_topNode;
    protected final HierarchyNode<E> m_bottomNode;
    protected final Map<E, HierarchyNode<E>> m_nodesByElements;

    public Hierarchy(HierarchyNode<E> topNode, HierarchyNode<E> bottomNode) {
        this.m_topNode = topNode;
        this.m_bottomNode = bottomNode;
        this.m_nodesByElements = new HashMap<E, HierarchyNode<E>>();
        for (E element : this.m_topNode.m_equivalentElements) {
            this.m_nodesByElements.put(element, this.m_topNode);
        }
        for (E element : this.m_bottomNode.m_equivalentElements) {
            this.m_nodesByElements.put(element, this.m_bottomNode);
        }
    }

    public HierarchyNode<E> getTopNode() {
        return this.m_topNode;
    }

    public HierarchyNode<E> getBottomNode() {
        return this.m_bottomNode;
    }

    public boolean isEmpty() {
        return this.m_nodesByElements.size() == 2 && this.m_topNode.m_equivalentElements.size() == 1 && this.m_bottomNode.m_equivalentElements.size() == 1;
    }

    public HierarchyNode<E> getNodeForElement(E element) {
        return this.m_nodesByElements.get(element);
    }

    public Collection<HierarchyNode<E>> getAllNodes() {
        return Collections.unmodifiableCollection(this.m_nodesByElements.values());
    }

    public Set<HierarchyNode<E>> getAllNodesSet() {
        return Collections.unmodifiableSet(new HashSet<HierarchyNode<E>>(this.m_nodesByElements.values()));
    }

    public Set<E> getAllElements() {
        return Collections.unmodifiableSet(this.m_nodesByElements.keySet());
    }

    public int getDepth() {
        HierarchyDepthFinder<E> depthFinder = new HierarchyDepthFinder<E>(this.m_bottomNode);
        this.traverseDepthFirst(depthFinder);
        return depthFinder.depth;
    }

    public <T> Hierarchy<T> transform(Transformer<? super E, T> transformer, Comparator<T> comparator) {
        HierarchyNodeComparator<T> newNodeComparator=new HierarchyNodeComparator<T>(comparator);
        Map<HierarchyNode<E>,HierarchyNode<T>> oldToNew=new HashMap<HierarchyNode<E>,HierarchyNode<T>>();
        for (HierarchyNode<E> oldNode : m_nodesByElements.values()) {
            Set<T> newEquivalentElements;
            Set<HierarchyNode<T>> newParentNodes;
            Set<HierarchyNode<T>> newChildNodes;
            if (comparator==null) {
                newEquivalentElements=new HashSet<T>();
                newParentNodes=new HashSet<HierarchyNode<T>>();
                newChildNodes=new HashSet<HierarchyNode<T>>();
            }
            else {
                newEquivalentElements=new TreeSet<T>(comparator);
                newParentNodes=new TreeSet<HierarchyNode<T>>(newNodeComparator);
                newChildNodes=new TreeSet<HierarchyNode<T>>(newNodeComparator);
            }
            for (E oldElement : oldNode.m_equivalentElements) {
                T newElement=transformer.transform(oldElement);
                newEquivalentElements.add(newElement);
            }
            T newRepresentative=transformer.determineRepresentative(oldNode.m_representative,newEquivalentElements);
            HierarchyNode<T> newNode=new HierarchyNode<T>(newRepresentative,newEquivalentElements,newParentNodes,newChildNodes);
            oldToNew.put(oldNode,newNode);
        }
        for (HierarchyNode<E> oldParentNode : m_nodesByElements.values()) {
            HierarchyNode<T> newParentNode=oldToNew.get(oldParentNode);
            for (HierarchyNode<E> oldChildNode : oldParentNode.m_childNodes) {
                HierarchyNode<T> newChildNode=oldToNew.get(oldChildNode);
                newParentNode.m_childNodes.add(newChildNode);
                newChildNode.m_parentNodes.add(newParentNode);
            }
        }
        HierarchyNode<T> newTopNode=oldToNew.get(m_topNode);
        HierarchyNode<T> newBottomNode=oldToNew.get(m_bottomNode);
        Hierarchy<T> newHierarchy=new Hierarchy<T>(newTopNode,newBottomNode);
        for (HierarchyNode<T> newNode : oldToNew.values())
            for (T newElement : newNode.m_equivalentElements)
                newHierarchy.m_nodesByElements.put(newElement,newNode);
        return newHierarchy;
    }

    public void traverseDepthFirst(HierarchyNodeVisitor<E> visitor) {
        HierarchyNode[] redirectBuffer = new HierarchyNode[2];
        HashSet<HierarchyNode<E>> visited = new HashSet<HierarchyNode<E>>();
        this.traverseDepthFirst(visitor, 0, this.m_topNode, null, visited, redirectBuffer);
    }

    protected void traverseDepthFirst(HierarchyNodeVisitor<E> visitor, int level, HierarchyNode<E> node, HierarchyNode<E> parentNode, Set<HierarchyNode<E>> visited, HierarchyNode<E>[] redirectBuffer) {
        redirectBuffer[0] = node;
        redirectBuffer[1] = parentNode;
        if (visitor.redirect(redirectBuffer)) {
            node = redirectBuffer[0];
            parentNode = redirectBuffer[1];
            boolean firstVisit = visited.add(node);
            visitor.visit(level, node, parentNode, firstVisit);
            if (firstVisit) {
                for (HierarchyNode childNode : node.m_childNodes) {
                    this.traverseDepthFirst(visitor, level + 1, childNode, node, visited, redirectBuffer);
                }
            }
        }
    }

    public String toString() {
        StringWriter buffer = new StringWriter();
        final PrintWriter output = new PrintWriter(buffer);
        this.traverseDepthFirst(new HierarchyNodeVisitor<E>(){

            @Override
            public boolean redirect(HierarchyNode<E>[] nodes) {
                return true;
            }

            @Override
            public void visit(int level, HierarchyNode<E> node, HierarchyNode<E> parentNode, boolean firstVisit) {
                if (!node.equals(Hierarchy.this.m_bottomNode)) {
                    this.printNode(level, node, parentNode, firstVisit);
                }
            }

            public void printNode(int level, HierarchyNode<E> node, HierarchyNode<E> parentNode, boolean firstVisit) {
                boolean printEquivalences;
                Set<E> equivalences = node.getEquivalentElements();
                boolean printSubClasOf = parentNode != null;
                boolean bl = printEquivalences = firstVisit && equivalences.size() > 1;
                if (printSubClasOf || printEquivalences) {
                    for (int i = 4 * level; i > 0; --i) {
                        output.print(' ');
                    }
                    output.print(node.getRepresentative().toString());
                    if (printEquivalences) {
                        output.print('[');
                        boolean first = true;
                        for (E element : equivalences) {
                            if (node.getRepresentative().equals(element)) continue;
                            if (first) {
                                first = false;
                            } else {
                                output.print(' ');
                            }
                            output.print(element);
                        }
                        output.print(']');
                    }
                    if (printSubClasOf) {
                        assert (parentNode != null);
                        output.print(" -> ");
                        output.print(parentNode.getRepresentative().toString());
                    }
                    output.println();
                }
            }
        });
        output.flush();
        return buffer.toString();
    }

    public static <T> Hierarchy<T> emptyHierarchy(Collection<T> elements, T topElement, T bottomElement) {
        HierarchyNode<T> topBottomNode = new HierarchyNode<T>(topElement);
        topBottomNode.m_equivalentElements.add(topElement);
        topBottomNode.m_equivalentElements.add(bottomElement);
        topBottomNode.m_equivalentElements.addAll(elements);
        return new Hierarchy<T>(topBottomNode, topBottomNode);
    }

    public static <T> Hierarchy<T> trivialHierarchy(T topElement, T bottomElement) {
        HierarchyNode<T> topNode = new HierarchyNode<T>(topElement);
        topNode.m_equivalentElements.add(topElement);
        HierarchyNode<T> bottomNode = new HierarchyNode<T>(bottomElement);
        bottomNode.m_equivalentElements.add(bottomElement);
        topNode.m_childNodes.add(bottomNode);
        bottomNode.m_parentNodes.add(topNode);
        return new Hierarchy<T>(topNode, bottomNode);
    }

    protected static class HierarchyNodeComparator<E>
    implements Comparator<HierarchyNode<E>> {
        protected final Comparator<E> m_elementComparator;

        public HierarchyNodeComparator(Comparator<E> elementComparator) {
            this.m_elementComparator = elementComparator;
        }

        @Override
        public int compare(HierarchyNode<E> n1, HierarchyNode<E> n2) {
            return this.m_elementComparator.compare(n1.m_representative, n2.m_representative);
        }
    }

    public interface Transformer<E, T> {
        T transform(E var1);

        T determineRepresentative(E var1, Set<T> var2);
    }

    protected interface HierarchyNodeVisitor<E> {
        boolean redirect(HierarchyNode<E>[] var1);

        void visit(int var1, HierarchyNode<E> var2, HierarchyNode<E> var3, boolean var4);
    }

    protected final class HierarchyDepthFinder<T>
    implements HierarchyNodeVisitor<T> {
        private final HierarchyNode<T> bottomNode;
        private int depth = 0;

        public HierarchyDepthFinder(HierarchyNode<T> bottomNode) {
            this.bottomNode = bottomNode;
        }

        @Override
        public boolean redirect(HierarchyNode<T>[] nodes) {
            return true;
        }

        @Override
        public void visit(int level, HierarchyNode<T> node, HierarchyNode<T> parentNode, boolean firstVisit) {
            if (node.equals(this.bottomNode) && level > this.depth) {
                this.depth = level;
            }
        }
    }

}

