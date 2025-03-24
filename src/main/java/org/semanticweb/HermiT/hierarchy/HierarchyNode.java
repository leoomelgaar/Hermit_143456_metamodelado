package org.semanticweb.HermiT.hierarchy;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class HierarchyNode<E> {
    protected final E m_representative;
    protected final Set<E> m_equivalentElements;
    protected final Set<HierarchyNode<E>> m_parentNodes;
    protected final Set<HierarchyNode<E>> m_childNodes;

    public HierarchyNode(E representative) {
        this.m_representative = representative;
        this.m_equivalentElements = new HashSet<>();
        this.m_equivalentElements.add(this.m_representative);
        this.m_parentNodes = new HashSet<HierarchyNode<E>>();
        this.m_childNodes = new HashSet<HierarchyNode<E>>();
    }

    public HierarchyNode(E element, Set<E> equivalentElements, Set<HierarchyNode<E>> parentNodes, Set<HierarchyNode<E>> childNodes) {
        this.m_representative = element;
        this.m_equivalentElements = equivalentElements;
        this.m_parentNodes = parentNodes;
        this.m_childNodes = childNodes;
    }

    public E getRepresentative() {
        return this.m_representative;
    }

    public boolean isEquivalentElement(E element) {
        return this.m_equivalentElements.contains(element);
    }

    public boolean isAncestorElement(E ancestor) {
        for (HierarchyNode<E> node : this.getAncestorNodes()) {
            if (!node.isEquivalentElement(ancestor)) continue;
            return true;
        }
        return false;
    }

    public boolean isDescendantElement(E descendant) {
        for (HierarchyNode<E> node : this.getDescendantNodes()) {
            if (!node.isEquivalentElement(descendant)) continue;
            return true;
        }
        return false;
    }

    public Set<E> getEquivalentElements() {
        return Collections.unmodifiableSet(this.m_equivalentElements);
    }

    public Set<HierarchyNode<E>> getParentNodes() {
        return Collections.unmodifiableSet(this.m_parentNodes);
    }

    public Set<HierarchyNode<E>> getChildNodes() {
        return Collections.unmodifiableSet(this.m_childNodes);
    }

    public Set<HierarchyNode<E>> getAncestorNodes() {
        return HierarchyNode.getAncestorNodes(Collections.singleton(this));
    }

    public Set<HierarchyNode<E>> getDescendantNodes() {
        return HierarchyNode.getDescendantNodes(Collections.singleton(this));
    }

    public String toString() {
        return this.m_equivalentElements.toString();
    }

    public static <T> Set<HierarchyNode<T>> getAncestorNodes(Set<HierarchyNode<T>> inputNodes) {
        HashSet<HierarchyNode<T>> result = new HashSet<HierarchyNode<T>>();
        LinkedList<HierarchyNode<T>> toVisit = new LinkedList<HierarchyNode<T>>(inputNodes);
        while (!toVisit.isEmpty()) {
            HierarchyNode current = toVisit.poll();
            if (!result.add(current)) continue;
            toVisit.addAll(current.getParentNodes());
        }
        return result;
    }

    public static <T> Set<HierarchyNode<T>> getDescendantNodes(Set<HierarchyNode<T>> inputNodes) {
        HashSet<HierarchyNode<T>> result = new HashSet<HierarchyNode<T>>();
        LinkedList<HierarchyNode<T>> toVisit = new LinkedList<HierarchyNode<T>>(inputNodes);
        while (!toVisit.isEmpty()) {
            HierarchyNode current = toVisit.poll();
            if (!result.add(current)) continue;
            toVisit.addAll(current.getChildNodes());
        }
        return result;
    }
}

