package org.semanticweb.HermiT.hierarchy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class HierarchySearch {
    public static <E> HierarchyNode<E> findPosition(Relation<E> hierarchyRelation, E element, HierarchyNode<E> topNode, HierarchyNode<E> bottomNode) {
        Set<HierarchyNode<E>> childNodes;
        Set<HierarchyNode<E>> parentNodes = HierarchySearch.findParents(hierarchyRelation, element, topNode);
        if (parentNodes.equals(childNodes = HierarchySearch.findChildren(hierarchyRelation, element, bottomNode, parentNodes))) {
            assert (parentNodes.size() == 1 && childNodes.size() == 1);
            return parentNodes.iterator().next();
        }
        HashSet<E> equivalentElements = new HashSet<E>();
        equivalentElements.add(element);
        return new HierarchyNode<E>(element, equivalentElements, parentNodes, childNodes);
    }

    protected static <E> Set<HierarchyNode<E>> findParents(final Relation<E> hierarchyRelation, final E element, HierarchyNode<E> topNode) {
        return HierarchySearch.search(new SearchPredicate<HierarchyNode<E>>(){

            @Override
            public Set<HierarchyNode<E>> getSuccessorElements(HierarchyNode<E> u) {
                return u.m_childNodes;
            }

            @Override
            public Set<HierarchyNode<E>> getPredecessorElements(HierarchyNode<E> u) {
                return u.m_parentNodes;
            }

            @Override
            public boolean trueOf(HierarchyNode<E> u) {
                return hierarchyRelation.doesSubsume(u.getRepresentative(), element);
            }
        }, Collections.singleton(topNode), null);
    }

    protected static <E> Set<HierarchyNode<E>> findChildren(final Relation<E> hierarchyRelation, final E element, HierarchyNode<E> bottomNode, Set<HierarchyNode<E>> parentNodes) {
        if (parentNodes.size() == 1 && hierarchyRelation.doesSubsume(element, parentNodes.iterator().next().getRepresentative())) {
            return parentNodes;
        }
        Iterator<HierarchyNode<E>> parentNodesIterator = parentNodes.iterator();
        Set<HierarchyNode<E>> marked = new HashSet<HierarchyNode<E>>(parentNodesIterator.next().getDescendantNodes());
        while (parentNodesIterator.hasNext()) {
            HierarchyNode<E> currentNode;
            HashSet freshlyMarked = new HashSet();
            HashSet visited = new HashSet();
            LinkedList toProcess = new LinkedList();
            toProcess.add(parentNodesIterator.next());
            while (!toProcess.isEmpty()) {
                currentNode = (HierarchyNode)toProcess.remove();
                for (HierarchyNode childNode : currentNode.m_childNodes) {
                    if (marked.contains(childNode)) {
                        freshlyMarked.add(childNode);
                        continue;
                    }
                    if (!visited.add(childNode)) continue;
                    toProcess.add(childNode);
                }
            }
            toProcess.addAll(freshlyMarked);
            while (!toProcess.isEmpty()) {
                currentNode = (HierarchyNode)toProcess.remove();
                for (HierarchyNode childNode : currentNode.m_childNodes) {
                    if (!freshlyMarked.add(childNode)) continue;
                    toProcess.add(childNode);
                }
            }
            marked = freshlyMarked;
        }
        Set<HierarchyNode<E>> aboveBottomNodes = new HashSet<HierarchyNode<E>>();
        for (HierarchyNode<E> node : marked) {
            if (!node.m_childNodes.contains(bottomNode) || !hierarchyRelation.doesSubsume(element, node.getRepresentative())) continue;
            aboveBottomNodes.add(node);
        }
        if (aboveBottomNodes.isEmpty()) {
        	Set<HierarchyNode<E>> childNodes=new HashSet<HierarchyNode<E>>();
            childNodes.add(bottomNode);
            return childNodes;
        }
        return HierarchySearch.search(new SearchPredicate<HierarchyNode<E>>(){

            @Override
            public Set<HierarchyNode<E>> getSuccessorElements(HierarchyNode<E> u) {
                return u.m_parentNodes;
            }

            @Override
            public Set<HierarchyNode<E>> getPredecessorElements(HierarchyNode<E> u) {
                return u.m_childNodes;
            }

            @Override
            public boolean trueOf(HierarchyNode<E> u) {
                return hierarchyRelation.doesSubsume(element, u.getRepresentative());
            }
        }, aboveBottomNodes, marked);
    }

    public static <U> Set<U> search(SearchPredicate<U> searchPredicate, Collection<U> startSearch, Set<U> possibilities) {
        SearchCache<U> cache = new SearchCache<U>(searchPredicate, possibilities);
        HashSet result = new HashSet();
        HashSet<U> visited = new HashSet<U>(startSearch);
        LinkedList<U> toProcess = new LinkedList<U>(startSearch);
        while (!toProcess.isEmpty()) {
            U current = toProcess.remove();
            boolean foundSubordinateElement = false;
            Set<U> subordinateElements = searchPredicate.getSuccessorElements(current);
            for (U subordinateElement : subordinateElements) {
                if (!cache.trueOf(subordinateElement)) continue;
                foundSubordinateElement = true;
                if (!visited.add(subordinateElement)) continue;
                toProcess.add(subordinateElement);
            }
            if (foundSubordinateElement) continue;
            result.add(current);
        }
        return result;
    }

    protected static final class SearchCache<U> {
        private final SearchPredicate<U> m_searchPredicate;
        private final Set<U> m_possibilities;
        private final Set<U> m_positives;
        private final Set<U> m_negatives;

        public SearchCache(SearchPredicate<U> f, Set<U> possibilities) {
            this.m_searchPredicate = f;
            this.m_possibilities = possibilities;
            this.m_positives = new HashSet<U>();
            this.m_negatives = new HashSet<U>();
        }

        public boolean trueOf(U element) {
            if (this.m_positives.contains(element)) {
                return true;
            }
            if (this.m_negatives.contains(element) || this.m_possibilities != null && !this.m_possibilities.contains(element)) {
                return false;
            }
            for (U superordinateElement : this.m_searchPredicate.getPredecessorElements(element)) {
                if (this.trueOf(superordinateElement)) continue;
                this.m_negatives.add(element);
                return false;
            }
            if (this.m_searchPredicate.trueOf(element)) {
                this.m_positives.add(element);
                return true;
            }
            this.m_negatives.add(element);
            return false;
        }
    }

    public interface SearchPredicate<U> {
        Set<U> getSuccessorElements(U var1);

        Set<U> getPredecessorElements(U var1);

        boolean trueOf(U var1);
    }

    public interface Relation<U> {
        boolean doesSubsume(U var1, U var2);
    }

}

