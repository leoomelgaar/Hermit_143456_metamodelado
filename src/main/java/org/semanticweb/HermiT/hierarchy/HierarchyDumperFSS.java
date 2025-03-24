package org.semanticweb.HermiT.hierarchy;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.TreeSet;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.InverseRole;
import org.semanticweb.HermiT.model.Role;

public class HierarchyDumperFSS {
    protected final PrintWriter m_out;

    public HierarchyDumperFSS(PrintWriter out) {
        this.m_out = out;
    }

    public void printAtomicConceptHierarchy(Hierarchy<AtomicConcept> atomicConceptHierarchy) {
        for (HierarchyNode<AtomicConcept> node : atomicConceptHierarchy.getAllNodesSet()) {
            TreeSet<AtomicConcept> equivs = new TreeSet<AtomicConcept>(AtomicConceptComparator.INSTANCE);
            equivs.addAll(node.getEquivalentElements());
            AtomicConcept representative = equivs.first();
            if (equivs.size() > 1) {
                boolean first = true;
                for (AtomicConcept equiv : equivs) {
                    if (first) {
                        this.m_out.print("EquivalentClasses( <");
                        this.m_out.print(representative.getIRI());
                        this.m_out.print(">");
                        first = false;
                        continue;
                    }
                    this.m_out.print(" <");
                    this.m_out.print(equiv.getIRI());
                    this.m_out.print(">");
                }
                this.m_out.print(" )");
                this.m_out.println();
            }
            if (representative.equals(AtomicConcept.THING)) continue;
            for (HierarchyNode<AtomicConcept> sub : node.getChildNodes()) {
                AtomicConcept subRepresentative = sub.getRepresentative();
                if (subRepresentative.equals(AtomicConcept.NOTHING)) continue;
                this.m_out.print("SubClassOf( <");
                this.m_out.print(subRepresentative.getIRI());
                this.m_out.print("> <");
                this.m_out.print(representative.getIRI());
                this.m_out.print("> )");
                this.m_out.println();
            }
        }
        this.m_out.println();
    }

    public void printObjectPropertyHierarchy(Hierarchy<Role> objectRoleHierarchy) {
        for (HierarchyNode<Role> node : objectRoleHierarchy.getAllNodesSet()) {
            TreeSet<Role> equivs = new TreeSet<Role>(ObjectRoleComparator.INSTANCE);
            equivs.addAll(node.getEquivalentElements());
            Role representative = equivs.first();
            if (equivs.size() > 1) {
                boolean first = true;
                for (Role equiv : equivs) {
                    if (first) {
                        this.m_out.print("EquivalentObjectProperties( ");
                        this.print(representative);
                        first = false;
                        continue;
                    }
                    this.m_out.print(" ");
                    this.print(equiv);
                }
                this.m_out.print(" )");
                this.m_out.println();
            }
            if (representative.equals(AtomicRole.TOP_OBJECT_ROLE)) continue;
            for (HierarchyNode<Role> sub : node.getChildNodes()) {
                Role subRepresentative = sub.getRepresentative();
                if (subRepresentative.equals(AtomicRole.BOTTOM_OBJECT_ROLE)) continue;
                this.m_out.print("SubObjectPropertyOf( ");
                this.print(subRepresentative);
                this.m_out.print(" ");
                this.print(representative);
                this.m_out.print(" )");
                this.m_out.println();
            }
        }
        this.m_out.println();
    }

    public void printDataPropertyHierarchy(Hierarchy<AtomicRole> dataRoleHierarchy) {
        for (HierarchyNode<AtomicRole> node : dataRoleHierarchy.getAllNodesSet()) {
            TreeSet<AtomicRole> equivs = new TreeSet<AtomicRole>(DataRoleComparator.INSTANCE);
            equivs.addAll(node.getEquivalentElements());
            AtomicRole representative = equivs.first();
            if (equivs.size() > 1) {
                boolean first = true;
                for (AtomicRole equiv : equivs) {
                    if (first) {
                        this.m_out.print("EquivalentDataProperties( <");
                        this.m_out.print(representative.getIRI());
                        this.m_out.print(">");
                        first = false;
                        continue;
                    }
                    this.m_out.print(" >");
                    this.m_out.print(equiv.getIRI());
                    this.m_out.print(">");
                }
                this.m_out.print(" )");
                this.m_out.println();
            }
            if (representative.equals(AtomicRole.TOP_DATA_ROLE)) continue;
            for (HierarchyNode<AtomicRole> sub : node.getChildNodes()) {
                AtomicRole subRepresentative = sub.getRepresentative();
                if (subRepresentative.equals(AtomicRole.BOTTOM_DATA_ROLE)) continue;
                this.m_out.print("SubDataPropertyOf( <");
                this.m_out.print(subRepresentative.getIRI());
                this.m_out.print("> <");
                this.m_out.print(representative.getIRI());
                this.m_out.print("> )");
                this.m_out.println();
            }
        }
        this.m_out.println();
    }

    protected void print(Role role) {
        if (role instanceof AtomicRole) {
            this.print((AtomicRole)role);
        } else {
            this.m_out.print("ObjectInverseOf( ");
            this.print(((InverseRole)role).getInverseOf());
            this.m_out.print(" )");
        }
    }

    protected void print(AtomicRole atomicRole) {
        this.m_out.print("<");
        this.m_out.print(atomicRole.getIRI());
        this.m_out.print(">");
    }

    protected static class DataRoleComparator
    implements Comparator<AtomicRole> {
        public static final DataRoleComparator INSTANCE = new DataRoleComparator();

        protected DataRoleComparator() {
        }

        @Override
        public int compare(AtomicRole atomicRole1, AtomicRole atomicRole2) {
            int comparison = this.getAtomicRoleClass(atomicRole1) - this.getAtomicRoleClass(atomicRole2);
            if (comparison != 0) {
                return comparison;
            }
            return atomicRole1.getIRI().compareTo(atomicRole2.getIRI());
        }

        protected int getAtomicRoleClass(AtomicRole atomicRole) {
            if (AtomicRole.BOTTOM_DATA_ROLE.equals(atomicRole)) {
                return 0;
            }
            if (AtomicRole.TOP_DATA_ROLE.equals(atomicRole)) {
                return 1;
            }
            return 2;
        }
    }

    protected static class ObjectRoleComparator
    implements Comparator<Role> {
        public static final ObjectRoleComparator INSTANCE = new ObjectRoleComparator();

        protected ObjectRoleComparator() {
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
            return 2;
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

}

