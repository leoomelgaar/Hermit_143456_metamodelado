package org.semanticweb.HermiT.reasoner;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

public class ComplexConceptTest extends AbstractReasonerTest {

    public ComplexConceptTest(String name) {
        super(name);
    }
    
    public void testConceptWithDatatypes() throws Exception {

        String buffer = "Declaration(NamedIndividual(:a))" +
                "Declaration(Class(:A))" +
                "Declaration(Class(:B))" +
                "Declaration(Class(:C))" +
                "Declaration(ObjectProperty(:f))" +
                "Declaration(DataProperty(:dp))" +
                "SubClassOf(:A ObjectSomeValuesFrom(:f :B))" +
                "SubClassOf(:A ObjectSomeValuesFrom(:f :C))" +
                "SubClassOf(:B DataSomeValuesFrom(:dp DataOneOf( \"abc\"^^xsd:string \"def\"^^xsd:string )))" +
                "SubClassOf(:C DataHasValue(:dp \"abc@\"^^rdf:PlainLiteral))" +
                "FunctionalObjectProperty(:f)" +
                "ClassAssertion(:A :a)";
        loadReasonerWithAxioms(buffer);
        
        OWLDataFactory df = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        OWLNamedIndividual a = df.getOWLNamedIndividual(IRI.create("file:/c/test.owl#a"));
        OWLObjectProperty f = df.getOWLObjectProperty(IRI.create("file:/c/test.owl#f"));
        OWLDataProperty dp = df.getOWLDataProperty(IRI.create("file:/c/test.owl#dp"));
        
        OWLClassExpression desc = df.getOWLObjectSomeValuesFrom(f, df.getOWLDataSomeValuesFrom(dp, df.getOWLDataOneOf(PL("abc",""))));
        assertInstanceOf(desc, a, true);
   }
 
    public void testConceptWithDatatypes2() throws Exception {

        String buffer = "Declaration(NamedIndividual(:a))" +
                "Declaration(Class(:A))" +
                "Declaration(DataProperty(:dp))" +
                "SubClassOf(:A DataAllValuesFrom(:dp DataComplementOf(rdfs:Literal)))" +
                "ClassAssertion(:A :a)";
        loadReasonerWithAxioms(buffer);
        
        OWLDataFactory df = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        OWLNamedIndividual a = df.getOWLNamedIndividual(IRI.create("file:/c/test.owl#a"));
        OWLDataProperty dp = df.getOWLDataProperty(IRI.create("file:/c/test.owl#dp"));
        
        OWLClassExpression desc = df.getOWLDataSomeValuesFrom(dp, df.getTopDatatype());
        assertInstanceOf(desc, a, false);
   }
    
    public void testConceptWithNominals() throws Exception {

        String buffer = "Declaration(NamedIndividual(:a))" +
                "Declaration(NamedIndividual(:b))" +
                "Declaration(NamedIndividual(:o))" +
                "Declaration(Class(:A))" +
                "Declaration(Class(:B))" +
                "Declaration(ObjectProperty(:f1))" +
                "Declaration(ObjectProperty(:f2))" +
                "Declaration(DataProperty(:dp))" +
                "ClassAssertion(ObjectSomeValuesFrom(:f1 ObjectSomeValuesFrom(:f2 ObjectOneOf(:o))) :a)" +
                "ClassAssertion(ObjectSomeValuesFrom(:f1 ObjectSomeValuesFrom(:f2 ObjectOneOf(:o))) :b)" +
                "InverseFunctionalObjectProperty(:f1)" +
                "InverseFunctionalObjectProperty(:f2)" +
                "ClassAssertion(ObjectAllValuesFrom(:f1 :A) :a)" +
                "ClassAssertion(ObjectAllValuesFrom(:f1 :B) :b)";
        loadReasonerWithAxioms(buffer);
        
        OWLDataFactory df = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        OWLNamedIndividual o = df.getOWLNamedIndividual(IRI.create("file:/c/test.owl#o"));
        OWLObjectProperty f2 = df.getOWLObjectProperty(IRI.create("file:/c/test.owl#f2"));
        OWLObjectPropertyExpression invf2 = df.getOWLObjectInverseOf(f2);
        OWLClass A = df.getOWLClass(IRI.create("file:/c/test.owl#A"));
        OWLClass B = df.getOWLClass(IRI.create("file:/c/test.owl#B"));
        
        OWLClassExpression desc = df.getOWLObjectAllValuesFrom(invf2, df.getOWLObjectIntersectionOf(A, B));
        assertInstanceOf(desc, o, true);
    }
    
    public void testConceptWithNominals2() throws Exception {

        String buffer = "Declaration(NamedIndividual(:a))" +
                "Declaration(NamedIndividual(:b))" +
                "Declaration(NamedIndividual(:o))" +
                "Declaration(Class(:A))" +
                "Declaration(Class(:B))" +
                "Declaration(ObjectProperty(:f1))" +
                "Declaration(ObjectProperty(:f2))" +
                "ClassAssertion(ObjectSomeValuesFrom(:f1 ObjectSomeValuesFrom(:f2 ObjectOneOf(:o))) :a)" +
                "ClassAssertion(ObjectSomeValuesFrom(:f1 ObjectSomeValuesFrom(:f2 ObjectOneOf(:o))) :b)" +
                "InverseFunctionalObjectProperty(:f1)" +
                "InverseFunctionalObjectProperty(:f2)" +
                "ClassAssertion(ObjectAllValuesFrom(:f1 :A) :a)" +
                "ClassAssertion(ObjectAllValuesFrom(:f1 :B) :b)";
        loadReasonerWithAxioms(buffer);
        
        OWLDataFactory df = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        OWLNamedIndividual a = df.getOWLNamedIndividual(IRI.create("file:/c/test.owl#a"));
        OWLNamedIndividual b = df.getOWLNamedIndividual(IRI.create("file:/c/test.owl#b"));
        
        OWLClassExpression desc = df.getOWLObjectIntersectionOf(df.getOWLObjectOneOf(a), df.getOWLObjectOneOf(b));
        assertInstanceOf(desc, a, true);
        assertInstanceOf(desc, b, true);
    }
    
    public void testConceptWithNominals3() throws Exception {

        String buffer = "Declaration(NamedIndividual(:a))" +
                "Declaration(NamedIndividual(:b))" +
                "Declaration(NamedIndividual(:o))" +
                "Declaration(Class(:A))" +
                "Declaration(Class(:B))" +
                "Declaration(ObjectProperty(:f1))" +
                "Declaration(ObjectProperty(:f2))" +
                "DisjointClasses(:A :B)" +
                "ClassAssertion(ObjectSomeValuesFrom(:f1 ObjectSomeValuesFrom(:f2 ObjectOneOf(:o))) :a)" +
                "ClassAssertion(ObjectSomeValuesFrom(:f1 ObjectSomeValuesFrom(:f2 ObjectOneOf(:o))) :b)" +
                "InverseFunctionalObjectProperty(:f1)" +
                "InverseFunctionalObjectProperty(:f2)" +
                "ClassAssertion(ObjectAllValuesFrom(:f1 :A) :a)" +
                "ClassAssertion(ObjectAllValuesFrom(:f1 :B) :b)";
        loadReasonerWithAxioms(buffer);
        
        assertABoxSatisfiable(false);
    }
   
    
    public void testConceptWithNominals4() throws Exception {

        String buffer = "Declaration(NamedIndividual(:a))" +
                "Declaration(NamedIndividual(:b))" +
                "Declaration(NamedIndividual(:o))" +
                "Declaration(ObjectProperty(:f1))" +
                "Declaration(ObjectProperty(:f2))" +
                "DisjointClasses(ObjectOneOf(:a) ObjectOneOf(:b))" +
                "ClassAssertion(ObjectSomeValuesFrom(:f1 ObjectSomeValuesFrom(:f2 ObjectOneOf(:o))) :a)" +
                "ClassAssertion(ObjectSomeValuesFrom(:f1 ObjectSomeValuesFrom(:f2 ObjectOneOf(:o))) :b)" +
                "InverseFunctionalObjectProperty(:f1)" +
                "InverseFunctionalObjectProperty(:f2)";
        loadReasonerWithAxioms(buffer);
        
        assertABoxSatisfiable(false);
    }
    
    public void testConceptWithNominals5() throws Exception {

        String buffer = "Declaration(NamedIndividual(:a))" +
                "Declaration(NamedIndividual(:b))" +
                "Declaration(Class(:B))" +
                "Declaration(ObjectProperty(:f))" +
                "Declaration(DataProperty(:dp))" +
                "ClassAssertion(ObjectSomeValuesFrom(:f :B) :a)" +
                "ObjectPropertyAssertion(:f :a :b)" +
                "FunctionalObjectProperty(:f)";
        loadReasonerWithAxioms(buffer);
        
        OWLDataFactory df = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        OWLIndividual b = df.getOWLNamedIndividual(IRI.create("file:/c/test.owl#b"));
        OWLClass B = df.getOWLClass(IRI.create("file:/c/test.owl#B"));
        
        assertSubsumedBy(df.getOWLObjectOneOf(b), B, true);
    }
    
    public void testJustifications() throws Exception {
        // test for Matthew's justifications that HermiT originally didn't answer correctly

        String buffer = "Declaration(NamedIndividual(:Matt))" +
                "Declaration(NamedIndividual(:Gemma))" +
                "Declaration(Class(:Person))" +
                "Declaration(Class(:Sibling))" +
                "Declaration(ObjectProperty(:hasSibling))" +
                "Declaration(ObjectProperty(:f2))" +
                "Declaration(DataProperty(:dp))" +
                "ClassAssertion(:Person :Matt)" +
                "ClassAssertion(:Person :Gemma)" +
                "ObjectPropertyAssertion(:hasSibling :Matt :Gemma)" +
                "SubClassOf(ObjectIntersectionOf(:Person ObjectSomeValuesFrom(:hasSibling :Person)) :Sibling)" +
                "SubClassOf(:Sibling ObjectIntersectionOf(:Person ObjectSomeValuesFrom(:hasSibling :Person)))";
        loadReasonerWithAxioms(buffer);
        
        OWLDataFactory df = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        OWLIndividual matt = df.getOWLNamedIndividual(IRI.create("file:/c/test.owl#Matt"));
        OWLClass sibling = df.getOWLClass(IRI.create("file:/c/test.owl#Sibling"));
        OWLClassExpression desc = df.getOWLObjectIntersectionOf(
                df.getOWLObjectOneOf(matt),
                df.getOWLObjectComplementOf(sibling));
        
        assertSatisfiable(desc, false);
    }
    
}
