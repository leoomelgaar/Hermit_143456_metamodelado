package org.semanticweb.HermiT.reasoner;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;

public class OWLReasonerTest extends AbstractReasonerTest {

    public OWLReasonerTest(String name) {
        super(name);
    }
    public void testgetInverseObjectPropertyExpressions() throws Exception {
        String axioms="SubObjectPropertyOf(:r ObjectInverseOf(:s))"
            + "SubObjectPropertyOf(:s ObjectInverseOf(:t))"
            + "SubObjectPropertyOf(:t :r)";
        loadOntologyWithAxioms(axioms);
        createOWLReasoner();
        Node<OWLObjectPropertyExpression> r_inverses=m_reasoner.getInverseObjectProperties(OP(IRI("r")));
        Node<OWLObjectPropertyExpression> invr_inverses=m_reasoner.getInverseObjectProperties(m_dataFactory.getOWLObjectInverseOf(OP(IRI("r"))));
        Set<OWLObjectPropertyExpression> r_inverses_expected=new HashSet<OWLObjectPropertyExpression>();
        r_inverses_expected.add(m_dataFactory.getOWLObjectInverseOf(OP(IRI("r"))));
        r_inverses_expected.add(OP(IRI("s")));
        r_inverses_expected.add(m_dataFactory.getOWLObjectInverseOf(OP(IRI("t"))));
        Set<OWLObjectPropertyExpression> invr_inverses_expected=new HashSet<OWLObjectPropertyExpression>();
        invr_inverses_expected.add(m_dataFactory.getOWLObjectInverseOf(OP(IRI("s"))));
        invr_inverses_expected.add(OP(IRI("r")));
        invr_inverses_expected.add(OP(IRI("t")));
        assertEquals(r_inverses.getEntities(), r_inverses_expected);
        assertEquals(invr_inverses.getEntities(), invr_inverses_expected);
    }
    public void testBottomObjectPropertySubs() throws Exception {
        String axioms="SubObjectPropertyOf(:r :s)";
        loadOntologyWithAxioms(axioms);
        createOWLReasoner();
        assertTrue(m_reasoner.getSubObjectProperties(m_dataFactory.getOWLBottomObjectProperty(), false).isEmpty());
        assertTrue(m_reasoner.getSubObjectProperties(m_dataFactory.getOWLBottomObjectProperty(), true).isEmpty());
    }
    public void testTopObjectPropertySupers() throws Exception {
        String axioms="SubObjectPropertyOf(:r :s)";
        loadOntologyWithAxioms(axioms);
        createOWLReasoner();
        assertTrue(m_reasoner.getSuperObjectProperties(m_dataFactory.getOWLTopObjectProperty(), false).isEmpty());
        assertTrue(m_reasoner.getSuperObjectProperties(m_dataFactory.getOWLTopObjectProperty(), true).isEmpty());
    }  
    public void testIncrementalAddition() throws Exception {
        String axioms="SubClassOf(:A :B)";
        loadOntologyWithAxioms(axioms);

        OWLClass a=m_dataFactory.getOWLClass(IRI.create(AbstractReasonerTest.NS+"A"));
        OWLClass b=m_dataFactory.getOWLClass(IRI.create(AbstractReasonerTest.NS+"B"));
        createOWLReasoner();
        NodeSet<OWLClass> aSuper=m_reasoner.getSuperClasses(a,false);
        NodeSet<OWLClass> bSuper=m_reasoner.getSuperClasses(b,false);
        NodeSet<OWLClass> aDirect=m_reasoner.getSuperClasses(a,true);
        NodeSet<OWLClass> bDirect=m_reasoner.getSuperClasses(b,true);
        assertFalse(aSuper.containsEntity(a));
        assertTrue(aSuper.containsEntity(b));
        assertTrue(aSuper.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(2, aSuper.getFlattened().size());
        assertFalse(bSuper.containsEntity(b));
        assertTrue(bSuper.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, bSuper.getFlattened().size());

        assertTrue(aDirect.containsEntity(b));
        assertFalse(aDirect.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, aDirect.getFlattened().size());
        assertTrue(bDirect.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, bDirect.getFlattened().size());

        OWLClass c=m_dataFactory.getOWLClass(IRI.create(AbstractReasonerTest.NS+"C"));
        OWLAxiom bImpliesC=m_dataFactory.getOWLSubClassOfAxiom(b,c);
        m_ontologyManager.addAxiom(m_ontology,bImpliesC);
        m_reasoner.flush();

        aSuper=m_reasoner.getSuperClasses(a,false);
        bSuper=m_reasoner.getSuperClasses(b,false);
        NodeSet<OWLClass> cSuper=m_reasoner.getSuperClasses(c,false);
        aDirect=m_reasoner.getSuperClasses(a,true);
        bDirect=m_reasoner.getSuperClasses(b,true);
        NodeSet<OWLClass> cDirect=m_reasoner.getSuperClasses(c,false);

        assertFalse(aSuper.containsEntity(a));
        assertTrue(aSuper.containsEntity(b));
        assertTrue(aSuper.containsEntity(c));
        assertTrue(aSuper.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(3, aSuper.getFlattened().size());
        assertFalse(bSuper.containsEntity(b));
        assertTrue(bSuper.containsEntity(c));
        assertTrue(bSuper.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(2, bSuper.getFlattened().size());
        assertFalse(cSuper.containsEntity(a));
        assertFalse(cSuper.containsEntity(b));
        assertFalse(cSuper.containsEntity(c));
        assertTrue(cSuper.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, cSuper.getFlattened().size());

        assertFalse(aDirect.containsEntity(a));
        assertTrue(aDirect.containsEntity(b));
        assertFalse(aDirect.containsEntity(c));
        assertFalse(aDirect.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, aDirect.getFlattened().size());
        assertFalse(bDirect.containsEntity(a));
        assertFalse(bDirect.containsEntity(b));
        assertTrue(bDirect.containsEntity(c));
        assertFalse(bDirect.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, bDirect.getFlattened().size());
        assertFalse(cDirect.containsEntity(a));
        assertFalse(cDirect.containsEntity(b));
        assertFalse(cDirect.containsEntity(c));
        assertTrue(cDirect.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, cDirect.getFlattened().size());

        m_ontologyManager.removeAxiom(m_ontology,bImpliesC);
        m_reasoner.flush();
        aSuper=m_reasoner.getSuperClasses(a,false);
        bSuper=m_reasoner.getSuperClasses(b,false);
        cSuper=m_reasoner.getSuperClasses(c,false);
        aDirect=m_reasoner.getSuperClasses(a,true);
        bDirect=m_reasoner.getSuperClasses(b,true);
        cDirect=m_reasoner.getSuperClasses(c,false);

        assertFalse(aSuper.containsEntity(a));
        assertTrue(aSuper.containsEntity(b));
        assertFalse(aSuper.containsEntity(c));
        assertTrue(aSuper.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(2, aSuper.getFlattened().size());
        assertFalse(bSuper.containsEntity(b));
        assertFalse(bSuper.containsEntity(c));
        assertTrue(bSuper.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, bSuper.getFlattened().size());
        assertFalse(cSuper.containsEntity(a));
        assertFalse(cSuper.containsEntity(b));
        assertFalse(cSuper.containsEntity(c));
        assertTrue(cSuper.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, cSuper.getFlattened().size());

        assertFalse(aDirect.containsEntity(a));
        assertTrue(aDirect.containsEntity(b));
        assertFalse(aDirect.containsEntity(c));
        assertFalse(aDirect.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, aDirect.getFlattened().size());
        assertFalse(bDirect.containsEntity(a));
        assertFalse(bDirect.containsEntity(b));
        assertFalse(bDirect.containsEntity(c));
        assertTrue(bDirect.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, bDirect.getFlattened().size());
        assertFalse(cDirect.containsEntity(a));
        assertFalse(cDirect.containsEntity(b));
        assertFalse(cDirect.containsEntity(c));
        assertTrue(cDirect.containsEntity(m_dataFactory.getOWLThing()));
        assertEquals(1, cDirect.getFlattened().size());
    }

    public void testIncrementalAddition2() throws Exception {
        String axioms="ObjectPropertyAssertion(:f :a :b) FunctionalObjectProperty(:f)";
        loadOntologyWithAxioms(axioms);

        createOWLReasoner();
        assertTrue(m_reasoner.isConsistent());

        OWLObjectProperty f=m_dataFactory.getOWLObjectProperty(IRI.create(AbstractReasonerTest.NS+"f"));
        OWLNamedIndividual a=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"a"));
        OWLNamedIndividual b=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"b"));
        OWLNamedIndividual c=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"c"));
        OWLAxiom fac=m_dataFactory.getOWLObjectPropertyAssertionAxiom(f,a,c);
        m_ontologyManager.addAxiom(m_ontology,fac);
        m_reasoner.flush();
        assertTrue(m_reasoner.isConsistent());
        OWLAxiom bneqc=m_dataFactory.getOWLDifferentIndividualsAxiom(b,c);
        m_ontologyManager.addAxiom(m_ontology,bneqc);
        assertTrue(m_reasoner.isConsistent());
        m_reasoner.flush();
        assertFalse(m_reasoner.isConsistent());
        m_ontologyManager.removeAxiom(m_ontology,fac);
        assertFalse(m_reasoner.isConsistent());
        m_reasoner.flush();
        assertTrue(m_reasoner.isConsistent());
    }

    public void testGetDataPropertyValues() throws Exception {
        loadOntologyWithAxioms(
            "DataPropertyAssertion(:dp :a \"RDFPlainLiteralwithEmptyLangTag@\"^^rdf:PlainLiteral) "+
            "DataPropertyAssertion(:dp :a \"RDFPlainLiteralwithEmptyLangTag\") "+
            "DataPropertyAssertion(:dp :a \"RDFPlainLiteralWithLangTag@en-gb\"^^rdf:PlainLiteral) "+
            "DataPropertyAssertion(:dp :a \"RDFPlainLiteralWithLangTag\"@en-gb) "+

            "DataPropertyAssertion(:dp :b \"abc\") "+
            "DataPropertyAssertion(:dp :b \"abc@\"^^rdf:PlainLiteral) "+

            "DataPropertyAssertion(:dp :c \"1\"^^xsd:integer) "+
            "DataPropertyAssertion(:dp :c \"01\"^^xsd:integer) "+
            "DataPropertyAssertion(:dp :c \"1\"^^xsd:short)"
        );

        createOWLReasoner();
        assertTrue(m_reasoner.isConsistent());

        assertContainsAll(m_reasoner.getDataPropertyValues(NS_NI("a"),NS_DP("dp")),
            PL("RDFPlainLiteralwithEmptyLangTag",""),
            PL("RDFPlainLiteralWithLangTag","en-gb")
        );

        assertContainsAll(m_reasoner.getDataPropertyValues(NS_NI("b"),NS_DP("dp")),
            PL("abc","")
        );

        assertContainsAll(m_reasoner.getDataPropertyValues(NS_NI("c"),NS_DP("dp")),
            TL("1","xsd:integer"),
            TL("01","xsd:integer"),
            TL("1","xsd:short")
        );
    }

    public void testEquivalenceClasses() throws Exception {
        Configuration c=new Configuration();
        c.individualNodeSetPolicy=IndividualNodeSetPolicy.BY_SAME_AS;
        loadSameAsTest(c);

        OWLNamedIndividual a1=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"a1"));
        OWLNamedIndividual a2_1=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"a2_1"));
        OWLNamedIndividual a2_2=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"a2_2"));
        OWLNamedIndividual b1_1=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"b1_1"));
        OWLNamedIndividual b1_2=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"b1_2"));
        OWLNamedIndividual b2=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"b2"));
        OWLClass A=m_dataFactory.getOWLClass(IRI.create(AbstractReasonerTest.NS+"A"));
        OWLClass B=m_dataFactory.getOWLClass(IRI.create(AbstractReasonerTest.NS+"B"));
        OWLClass C=m_dataFactory.getOWLClass(IRI.create(AbstractReasonerTest.NS+"C"));

        NodeSet<OWLNamedIndividual> As=m_reasoner.getInstances(A,false);
        NodeSet<OWLNamedIndividual> Bs=m_reasoner.getInstances(B,false);
        NodeSet<OWLNamedIndividual> directBs=m_reasoner.getInstances(B,true);
        NodeSet<OWLNamedIndividual> Cs=m_reasoner.getInstances(C,false);
        assertEquals(2, As.getNodes().size());
        assertEquals(3, As.getFlattened().size());
        assertEquals(2, Bs.getNodes().size());
        assertEquals(3, Bs.getFlattened().size());
        assertEquals(1, directBs.getNodes().size());
        assertEquals(1, directBs.getFlattened().size());
        assertEquals(1, Cs.getNodes().size());
        assertEquals(2, Cs.getFlattened().size());
        for (Node<OWLNamedIndividual> ANode : As.getNodes()) {
            if (ANode.getSize()==1)
                assertTrue(ANode.contains(a1));
            else if (ANode.getSize()==2) {
                assertTrue(ANode.contains(a2_1));
                assertTrue(ANode.contains(a2_2));
            }
            else
                fail();
        }
        for (Node<OWLNamedIndividual> BNode : Bs.getNodes()) {
            if (BNode.getSize()==1)
                assertTrue(BNode.contains(b2));
            else if (BNode.getSize()==2) {
                assertTrue(BNode.contains(b1_1));
                assertTrue(BNode.contains(b1_2));
            }
            else
                fail();
        }
        for (Node<OWLNamedIndividual> directBNode : directBs.getNodes()) {
            if (directBNode.getSize()==1)
                assertTrue(directBNode.contains(b2));
            else
                fail();
        }
        for (Node<OWLNamedIndividual> CNode : Cs.getNodes()) {
            if (CNode.getSize()==2) {
                assertTrue(CNode.contains(b1_1));
                assertTrue(CNode.contains(b1_2));
            }
            else
                fail();
        }
    }

    public void testNonEquivalenceClasses() throws Exception {
        Configuration c=new Configuration();
        c.individualNodeSetPolicy=IndividualNodeSetPolicy.BY_NAME;
        loadSameAsTest(c);

        OWLNamedIndividual a1=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"a1"));
        OWLNamedIndividual a2_1=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"a2_1"));
        OWLNamedIndividual a2_2=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"a2_2"));
        OWLNamedIndividual b1_1=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"b1_1"));
        OWLNamedIndividual b1_2=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"b1_2"));
        OWLNamedIndividual b2=m_dataFactory.getOWLNamedIndividual(IRI.create(AbstractReasonerTest.NS+"b2"));
        OWLClass A=m_dataFactory.getOWLClass(IRI.create(AbstractReasonerTest.NS+"A"));
        OWLClass B=m_dataFactory.getOWLClass(IRI.create(AbstractReasonerTest.NS+"B"));
        OWLClass C=m_dataFactory.getOWLClass(IRI.create(AbstractReasonerTest.NS+"C"));

        NodeSet<OWLNamedIndividual> As=m_reasoner.getInstances(A,false);
        NodeSet<OWLNamedIndividual> Bs=m_reasoner.getInstances(B,false);
        NodeSet<OWLNamedIndividual> directBs=m_reasoner.getInstances(B,true);
        NodeSet<OWLNamedIndividual> Cs=m_reasoner.getInstances(C,false);
        assertEquals(3, As.getNodes().size());
        assertEquals(3, As.getFlattened().size());
        assertEquals(3, Bs.getNodes().size());
        assertEquals(3, Bs.getFlattened().size());
        assertEquals(1, directBs.getNodes().size());
        assertEquals(1, directBs.getFlattened().size());
        assertEquals(2, Cs.getNodes().size());
        assertEquals(2, Cs.getFlattened().size());
        for (Node<OWLNamedIndividual> ANode : As.getNodes()) {
            assertEquals(1, ANode.getSize());
        }
        for (Node<OWLNamedIndividual> BNode : Bs.getNodes()) {
            assertEquals(1, BNode.getSize());
        }
        for (Node<OWLNamedIndividual> directBNode : directBs.getNodes()) {
            assertEquals(1, directBNode.getSize());
        }
        for (Node<OWLNamedIndividual> CNode : Cs.getNodes()) {
            assertEquals(1, CNode.getSize());
        }
        assertTrue(As.containsEntity(a1));
        assertTrue(As.containsEntity(a2_1));
        assertTrue(As.containsEntity(a2_2));
        assertTrue(Bs.containsEntity(b1_1));
        assertTrue(Bs.containsEntity(b1_2));
        assertTrue(Bs.containsEntity(b2));
        assertTrue(Cs.containsEntity(b1_1));
        assertTrue(Cs.containsEntity(b1_2));
        assertTrue(directBs.containsEntity(b2));
    }

    protected void loadSameAsTest(Configuration c) throws Exception {
        String axioms="Declaration(NamedIndividual(:a1)) Declaration(NamedIndividual(:b1_1)) Declaration(NamedIndividual(:b1_2)) Declaration(NamedIndividual(:a2_1)) Declaration(NamedIndividual(:a2_2)) Declaration(NamedIndividual(:b2)) Declaration(ObjectProperty(:f)) "
            +"ObjectPropertyAssertion(:f :a1 :b1_1) "
            +"ObjectPropertyAssertion(:f :a1 :b1_2) "
            +"SameIndividual(:a2_1 :a2_2) "
            +"ClassAssertion(:A :a1) "
            +"ClassAssertion(:A :a2_1) "
            +"ClassAssertion(:C :b1_1) "
            +"ClassAssertion(:B :b2) "
            +"SubClassOf(:C :B) "
            +"FunctionalObjectProperty(:f) ";
        loadOntologyWithAxioms(axioms);
        createOWLReasoner(c);
    }
}
