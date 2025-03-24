package org.semanticweb.HermiT.hierarchy;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.InverseRole;
import org.semanticweb.HermiT.model.Role;

public class HierarchyPrinterFSS {
    protected final PrintWriter m_out;
    protected final String m_defaultPrefixIRI;
    protected final Set<String> m_prefixIRIs;
    protected Prefixes m_prefixes;

    public HierarchyPrinterFSS(PrintWriter out, String defaultPrefixIRI) {
        this.m_out = out;
        this.m_defaultPrefixIRI = defaultPrefixIRI;
        this.m_prefixIRIs = new TreeSet<String>();
        this.m_prefixIRIs.add(defaultPrefixIRI);
        this.m_prefixIRIs.add(Prefixes.s_semanticWebPrefixes.get("owl:"));
    }

    public void loadAtomicConceptPrefixIRIs(Collection<AtomicConcept> atomicConcepts) {
        for (AtomicConcept atomicConcept : atomicConcepts) {
            String uri = atomicConcept.getIRI();
            int hashIndex = uri.indexOf(35);
            if (hashIndex == -1) continue;
            String prefixIRI = uri.substring(0, hashIndex + 1);
            String localName = uri.substring(hashIndex + 1);
            if (!Prefixes.isValidLocalName(localName)) continue;
            this.m_prefixIRIs.add(prefixIRI);
        }
    }

    public void loadAtomicRolePrefixIRIs(Collection<AtomicRole> atomicRoles) {
        for (AtomicRole atomicRole : atomicRoles) {
            String uri = atomicRole.getIRI();
            int hashIndex = uri.indexOf(35);
            if (hashIndex == -1) continue;
            String prefixIRI = uri.substring(0, hashIndex + 1);
            String localName = uri.substring(hashIndex + 1);
            if (!Prefixes.isValidLocalName(localName)) continue;
            this.m_prefixIRIs.add(prefixIRI);
        }
    }

    public void startPrinting() {
        String owlPrefixIRI = Prefixes.s_semanticWebPrefixes.get("owl:");
        this.m_prefixes = new Prefixes();
        this.m_prefixes.declareDefaultPrefix(this.m_defaultPrefixIRI);
        this.m_prefixes.declarePrefix("owl:", owlPrefixIRI);
        int index = 1;
        for (String prefixIRI : this.m_prefixIRIs) {
            if (this.m_defaultPrefixIRI.equals(prefixIRI) || owlPrefixIRI.equals(prefixIRI)) continue;
            String prefixName = "a" + index++ + ":";
            this.m_prefixes.declarePrefix(prefixName, prefixIRI);
        }
        for (Map.Entry entry : this.m_prefixes.getPrefixIRIsByPrefixName().entrySet()) {
            if ("owl:".equals(entry.getKey())) continue;
            this.m_out.println("Prefix(" + entry.getKey() + "=<" + entry.getValue() + ">)");
        }
        this.m_out.println();
        this.m_out.println("Ontology(<" + this.m_prefixes.getPrefixIRIsByPrefixName().get(":") + ">");
        this.m_out.println();
    }

    public void printAtomicConceptHierarchy(Hierarchy<AtomicConcept> atomicConceptHierarchy) {
        Hierarchy<AtomicConcept> sortedAtomicConceptHierarchy = atomicConceptHierarchy.transform(new IdentityTransformer(), AtomicConceptComparator.INSTANCE);
        AtomicConceptPrinter atomicConceptPrinter = new AtomicConceptPrinter(sortedAtomicConceptHierarchy.getBottomNode());
        sortedAtomicConceptHierarchy.traverseDepthFirst(atomicConceptPrinter);
        atomicConceptPrinter.printNode(0, sortedAtomicConceptHierarchy.getBottomNode(), null, true);
    }

    public void printRoleHierarchy(Hierarchy<? extends Role> roleHierarchy, boolean objectProperties) {
        Hierarchy<Role> sortedRoleHierarchy = roleHierarchy.transform(new IdentityTransformer(), RoleComparator.INSTANCE);
        RolePrinter rolePrinter = new RolePrinter(sortedRoleHierarchy, objectProperties);
        sortedRoleHierarchy.traverseDepthFirst(rolePrinter);
        rolePrinter.printNode(0, sortedRoleHierarchy.getBottomNode(), null, true);
    }

    public void endPrinting() {
        this.m_out.println();
        this.m_out.println(")");
        this.m_out.flush();
    }

    protected class IdentityTransformer<E>
    implements Hierarchy.Transformer<E, E> {
        protected IdentityTransformer() {
        }

        @Override
        public E transform(E object) {
            return object;
        }

        @Override
        public E determineRepresentative(E oldRepresentative, Set<E> newEquivalentElements) {
            return ((SortedSet<E>)newEquivalentElements).first();
        }
    }

    protected static class AtomicConceptComparator
    implements Comparator<AtomicConcept> {
        public static final AtomicConceptComparator INSTANCE = new AtomicConceptComparator();

        protected AtomicConceptComparator() {
        }

        @Override
        public int compare(AtomicConcept atomicConcept1, AtomicConcept atomicConcept2) {
            int comparison = this.getAtomicConceptClass(atomicConcept1) - this.getAtomicConceptClass(atomicConcept2);
            if (comparison != 0) {
                return comparison;
            }
            return atomicConcept1.getIRI().compareTo(atomicConcept2.getIRI());
        }

        protected int getAtomicConceptClass(AtomicConcept atomicConcept) {
            if (AtomicConcept.NOTHING.equals(atomicConcept)) {
                return 0;
            }
            if (AtomicConcept.THING.equals(atomicConcept)) {
                return 1;
            }
            return 2;
        }
    }

    protected static class RoleComparator
    implements Comparator<Role> {
        public static final RoleComparator INSTANCE = new RoleComparator();

        protected RoleComparator() {
        }

        @Override
        public int compare(Role role1, Role role2) {
            int comparison = this.getRoleClass(role1) - this.getRoleClass(role2);
            if (comparison != 0) {
                return comparison;
            }
            comparison = this.getRoleDirection(role1) - this.getRoleDirection(role2);
            if (comparison != 0) {
                return comparison;
            }
            return this.getInnerAtomicRole(role1).getIRI().compareTo(this.getInnerAtomicRole(role2).getIRI());
        }

        protected int getRoleClass(Role role) {
            if (AtomicRole.BOTTOM_OBJECT_ROLE.equals(role)) {
                return 0;
            }
            if (AtomicRole.TOP_OBJECT_ROLE.equals(role)) {
                return 1;
            }
            if (AtomicRole.BOTTOM_DATA_ROLE.equals(role)) {
                return 2;
            }
            if (AtomicRole.TOP_DATA_ROLE.equals(role)) {
                return 3;
            }
            return 4;
        }

        protected AtomicRole getInnerAtomicRole(Role role) {
            if (role instanceof AtomicRole) {
                return (AtomicRole)role;
            }
            return ((InverseRole)role).getInverseOf();
        }

        protected int getRoleDirection(Role role) {
            return role instanceof AtomicRole ? 0 : 1;
        }
    }

    protected class RolePrinter
    implements Hierarchy.HierarchyNodeVisitor<Role> {
        protected final Hierarchy<Role> m_hierarchy;
        protected final boolean m_objectProperties;

        public RolePrinter(Hierarchy<Role> hierarchy, boolean objectProperties) {
            this.m_hierarchy = hierarchy;
            this.m_objectProperties = objectProperties;
        }

        @Override
        public boolean redirect(HierarchyNode<Role>[] nodes) {
            return true;
        }

        @Override
        public void visit(int level, HierarchyNode<Role> node, HierarchyNode<Role> parentNode, boolean firstVisit) {
            if (!node.equals(this.m_hierarchy.getBottomNode())) {
                this.printNode(level, node, parentNode, firstVisit);
            }
        }

        public void printNode(int level, HierarchyNode<Role> node, HierarchyNode<Role> parentNode, boolean firstVisit) {
            Set<Role> equivalences = node.getEquivalentElements();
            boolean printSubPropertyOf = parentNode != null;
            boolean printEquivalences = firstVisit && equivalences.size() > 1;
            boolean printDeclarations = false;
            if (firstVisit) {
                for (Role role : equivalences) {
                    if (!this.needsDeclaration(role)) continue;
                    printDeclarations = true;
                    break;
                }
            }
            if (printSubPropertyOf || printEquivalences || printDeclarations) {
                for (int i = 2 * level; i > 0; --i) {
                    HierarchyPrinterFSS.this.m_out.print(' ');
                }
                boolean afterWS = true;
                if (printSubPropertyOf) {
                    assert (parentNode != null);
                    if (this.m_objectProperties) {
                        HierarchyPrinterFSS.this.m_out.print("SubObjectPropertyOf( ");
                    } else {
                        HierarchyPrinterFSS.this.m_out.print("SubDataPropertyOf( ");
                    }
                    this.print(node.getRepresentative());
                    HierarchyPrinterFSS.this.m_out.print(' ');
                    this.print(parentNode.getRepresentative());
                    HierarchyPrinterFSS.this.m_out.print(" )");
                    afterWS = false;
                }
                if (printEquivalences) {
                    if (!afterWS) {
                        HierarchyPrinterFSS.this.m_out.print(' ');
                    }
                    if (this.m_objectProperties) {
                        HierarchyPrinterFSS.this.m_out.print("EquivalentObjectProperties(");
                    } else {
                        HierarchyPrinterFSS.this.m_out.print("EquivalentDataProperties(");
                    }
                    for (Role role : equivalences) {
                        HierarchyPrinterFSS.this.m_out.print(' ');
                        this.print(role);
                    }
                    HierarchyPrinterFSS.this.m_out.print(" )");
                    afterWS = false;
                }
                if (printDeclarations) {
                    for (Role role : equivalences) {
                        if (!this.needsDeclaration(role)) continue;
                        if (!afterWS) {
                            HierarchyPrinterFSS.this.m_out.print(' ');
                        }
                        HierarchyPrinterFSS.this.m_out.print("Declaration( ");
                        if (this.m_objectProperties) {
                            HierarchyPrinterFSS.this.m_out.print("ObjectProperty( ");
                        } else {
                            HierarchyPrinterFSS.this.m_out.print("DataProperty( ");
                        }
                        this.print(role);
                        HierarchyPrinterFSS.this.m_out.print(" ) )");
                        afterWS = false;
                    }
                }
                HierarchyPrinterFSS.this.m_out.println();
            }
        }

        protected void print(Role role) {
            if (role instanceof AtomicRole) {
                HierarchyPrinterFSS.this.m_out.print(HierarchyPrinterFSS.this.m_prefixes.abbreviateIRI(((AtomicRole)role).getIRI()));
            } else {
                HierarchyPrinterFSS.this.m_out.print("ObjectInverseOf( ");
                this.print(((InverseRole)role).getInverseOf());
                HierarchyPrinterFSS.this.m_out.print(" )");
            }
        }

        protected void print(AtomicRole atomicRole) {
            HierarchyPrinterFSS.this.m_out.print(HierarchyPrinterFSS.this.m_prefixes.abbreviateIRI(atomicRole.getIRI()));
        }

        protected boolean needsDeclaration(Role role) {
            return !AtomicRole.BOTTOM_OBJECT_ROLE.equals(role) && !AtomicRole.TOP_OBJECT_ROLE.equals(role) && !AtomicRole.BOTTOM_DATA_ROLE.equals(role) && !AtomicRole.TOP_DATA_ROLE.equals(role) && role instanceof AtomicRole;
        }
    }

    protected class AtomicConceptPrinter
    implements Hierarchy.HierarchyNodeVisitor<AtomicConcept> {
        protected final HierarchyNode<AtomicConcept> m_bottomNode;

        public AtomicConceptPrinter(HierarchyNode<AtomicConcept> bottomNode) {
            this.m_bottomNode = bottomNode;
        }

        @Override
        public boolean redirect(HierarchyNode<AtomicConcept>[] nodes) {
            return true;
        }

        @Override
        public void visit(int level, HierarchyNode<AtomicConcept> node, HierarchyNode<AtomicConcept> parentNode, boolean firstVisit) {
            if (!node.equals(this.m_bottomNode)) {
                this.printNode(level, node, parentNode, firstVisit);
            }
        }

        public void printNode(int level, HierarchyNode<AtomicConcept> node, HierarchyNode<AtomicConcept> parentNode, boolean firstVisit) {
            Set<AtomicConcept> equivalences = node.getEquivalentElements();
            boolean printSubClasOf = parentNode != null;
            boolean printEquivalences = firstVisit && equivalences.size() > 1;
            boolean printDeclarations = false;
            if (firstVisit) {
                for (AtomicConcept atomicConcept : equivalences) {
                    if (!this.needsDeclaration(atomicConcept)) continue;
                    printDeclarations = true;
                    break;
                }
            }
            if (printSubClasOf || printEquivalences || printDeclarations) {
                for (int i = 2 * level; i > 0; --i) {
                    HierarchyPrinterFSS.this.m_out.print(' ');
                }
                boolean afterWS = true;
                if (printSubClasOf) {
                    HierarchyPrinterFSS.this.m_out.print("SubClassOf( ");
                    this.print(node.getRepresentative());
                    HierarchyPrinterFSS.this.m_out.print(' ');
                    assert (parentNode != null);
                    this.print(parentNode.getRepresentative());
                    HierarchyPrinterFSS.this.m_out.print(" )");
                    afterWS = false;
                }
                if (printEquivalences) {
                    if (!afterWS) {
                        HierarchyPrinterFSS.this.m_out.print(' ');
                    }
                    HierarchyPrinterFSS.this.m_out.print("EquivalentClasses(");
                    for (AtomicConcept atomicConcept : equivalences) {
                        HierarchyPrinterFSS.this.m_out.print(' ');
                        this.print(atomicConcept);
                    }
                    HierarchyPrinterFSS.this.m_out.print(" )");
                    afterWS = false;
                }
                if (printDeclarations) {
                    for (AtomicConcept atomicConcept : equivalences) {
                        if (!this.needsDeclaration(atomicConcept)) continue;
                        if (!afterWS) {
                            HierarchyPrinterFSS.this.m_out.print(' ');
                        }
                        HierarchyPrinterFSS.this.m_out.print("Declaration( Class( ");
                        this.print(atomicConcept);
                        HierarchyPrinterFSS.this.m_out.print(" ) )");
                        afterWS = false;
                    }
                }
                HierarchyPrinterFSS.this.m_out.println();
            }
        }

        protected void print(AtomicConcept atomicConcept) {
            HierarchyPrinterFSS.this.m_out.print(HierarchyPrinterFSS.this.m_prefixes.abbreviateIRI(atomicConcept.getIRI()));
        }

        protected boolean needsDeclaration(AtomicConcept atomicConcept) {
            return !AtomicConcept.NOTHING.equals(atomicConcept) && !AtomicConcept.THING.equals(atomicConcept);
        }
    }

}

