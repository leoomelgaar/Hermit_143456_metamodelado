package org.semanticweb.HermiT.tableau;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLOntology;
import org.semanticweb.HermiT.model.Individual;
import org.semanticweb.HermiT.model.LiteralConcept;
import org.semanticweb.HermiT.model.Variable;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLMetamodellingAxiom;

public class MetamodellingAxiomHelper {
	
	protected final static String DEF_STRING = "<internal:def#";
	private static Stack<Node> nodeStack;
	private static boolean existCycle;
	private static List<Node> flaggedNodes;
	
	public static boolean findCyclesInM(Tableau tableau) {
		flaggedNodes = new ArrayList<Node>();
		nodeStack = new Stack<Node>();
		existCycle = false;
		for (Node metamodellingNode : tableau.getMetamodellingNodes()) {
			if (!flaggedNodes.contains(metamodellingNode)) {
				controlCycle(metamodellingNode, tableau);
				if (existCycle) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static void controlCycle(Node node, Tableau tableau) {
		nodeStack.push(node);
		flaggedNodes.add(node);
		for (Node instance : getInstancesFromMetamodellingEqualClasses(node, tableau)) {
			if (existCycle) {
				return;
			} else {
				if (!flaggedNodes.contains(instance)) {
					controlCycle(instance, tableau);
				} else {
					if (nodeStack.contains(instance)) {
						existCycle = true;
					}
				}
			}
			
		}
		nodeStack.pop();
    }
	
	private static Set<Node> getInstancesFromMetamodellingEqualClasses(Node node, Tableau tableau) {
		Set<Node> instances = new HashSet<Node>();
		for (OWLMetamodellingAxiom metamodellingAxiom : tableau.getPermanentDLOntology().getMetamodellingAxioms()) {
			if (tableau.areSameIndividual(node, tableau.m_metamodellingManager.getMetamodellingNodeFromIndividual(metamodellingAxiom.getMetamodelIndividual()))) {
				instances.addAll(tableau.getClassInstances(metamodellingAxiom.getModelClass().toString()));
			}
		}	
		return instances;
	}
	
	public static List<OWLClassExpression> getMetamodellingClassesByIndividual(Individual ind, DLOntology ontology) {
		List<OWLClassExpression> classes = new ArrayList<OWLClassExpression>();
		if (ind != null) {
			for (OWLMetamodellingAxiom metamodellingAxiom : ontology.getMetamodellingAxioms()) {
				if (ind.toString().equals(metamodellingAxiom.getMetamodelIndividual().toString())) {
					classes.add(metamodellingAxiom.getModelClass());
				}
			}
		}
		return classes;
	}
	
	public static void addMetaRuleAddedAxiom(String classA, String propertyS, List<String> classesFromImage, Tableau tableau) {
		String defClass = DEF_STRING + getNextDef(tableau.getPermanentDLOntology()) + ">";
		
		Atom defAtomY = Atom.create(AtomicConcept.create(defClass.substring(1, defClass.length()-1)), Variable.create("Y"));
		Atom classAAtom = Atom.create(AtomicConcept.create(classA.substring(1, classA.length()-1)), Variable.create("X"));
		Atom propertySAtom = Atom.create(AtomicRole.create(propertyS.substring(1, propertyS.length()-1)), Variable.create("X"), Variable.create("Y"));
		
		Atom[] headAtoms1 = {defAtomY};
		Atom[] bodyAtoms1 = {classAAtom, propertySAtom};
		
		DLClause dlClause1 = DLClause.create(headAtoms1, bodyAtoms1);
		
		List<Atom> headAtoms2List = new ArrayList<Atom>();
		for (String classFromImage : classesFromImage) {
			Atom classFromImageAtom = Atom.create(AtomicConcept.create(classFromImage.substring(1, classFromImage.length()-1)), Variable.create("X"));
			headAtoms2List.add(classFromImageAtom);
		}
		
		Atom defAtomX = Atom.create(AtomicConcept.create(defClass.substring(1, defClass.length()-1)), Variable.create("X"));
		
		Atom[] headAtoms2 = headAtoms2List.toArray(new Atom[0]);
		Atom[] bodyAtoms2 = {defAtomX};
		
		DLClause dlClause2 = DLClause.create(headAtoms2, bodyAtoms2);
		
		tableau.getPermanentDLOntology().getDLClauses().add(dlClause1);
		tableau.getPermanentDLOntology().getDLClauses().add(dlClause2);
		
		List<DLClause> dlClauses = new ArrayList<DLClause>() { 
            { 
                add(dlClause1);
                add(dlClause2); 
            } 
        }; 

		createHyperResolutionManager(tableau, dlClauses);
	}
	
	public static boolean containsMetaRuleAddedAxiom(String classA, String propertyS, List<String> classesFromImage, Tableau tableau) {
		DLOntology ontology = tableau.getPermanentDLOntology();
		for (DLClause dlClause1 : ontology.getDLClauses()) {
			if (dlClause1.isGeneralConceptInclusion() && dlClause1.getHeadLength() == 1 && dlClause1.getBodyLength() == 2 && 
					dlClause1.getHeadAtoms()[0].toString().startsWith(DEF_STRING) && dlClause1.getBodyAtoms()[0].toString().startsWith(classA) && dlClause1.getBodyAtoms()[1].toString().startsWith(propertyS)) {
				Atom headAtom = dlClause1.getHeadAtoms()[0];
				Atom bodyAtom1 = dlClause1.getBodyAtoms()[0];
				Atom bodyAtom2 = dlClause1.getBodyAtoms()[1];
				Variable variableY = headAtom.getArgumentVariable(0);
				Variable variableX = bodyAtom1.getArgumentVariable(0);
				if (bodyAtom2.getArgumentVariable(0).toString().equals(variableX.toString()) && bodyAtom2.getArgumentVariable(1).toString().equals(variableY.toString())) {
					String internalDefinition = headAtom.getDLPredicate().toString();
					//FOUND FIRST AXIOM
					for (DLClause dlClause2 : ontology.getDLClauses()) {
						if (dlClause2.isGeneralConceptInclusion() && dlClause2.getBodyLength() == 1 && dlClause2.getBodyAtoms()[0].toString().startsWith(internalDefinition)) {
							List<String> headAtomClasses = new ArrayList<String>();
							for (Atom headClassAtom :dlClause2.getHeadAtoms()) {
								headAtomClasses.add(headClassAtom.getDLPredicate().toString());
							}
							boolean containsAllClasses = true;
							for (String classFromImage : classesFromImage) {
								containsAllClasses = containsAllClasses && headAtomClasses.contains(classFromImage);
							}
							if (containsAllClasses) {
								return true;
							}
						}
					}
				}
				
			}
		}
		return false;
	}
	
	public static Atom containsInequalityRuleAxiom(OWLClassExpression classA, OWLClassExpression classB, Tableau tableau) {
		DLOntology ontology = tableau.getPermanentDLOntology();
		
		for (DLClause dlClause : ontology.getDLClauses()) {
			if (dlClause.isGeneralConceptInclusion() && dlClause.getHeadLength() == 2 && dlClause.getBodyLength() == 1) {
				Atom def0 = dlClause.getBodyAtom(0);
				Atom def1 = dlClause.getHeadAtom(0);
				Atom def2 = dlClause.getHeadAtom(1);
				if (def0.toString().startsWith(DEF_STRING) && def1.toString().startsWith(DEF_STRING) && def2.toString().startsWith(DEF_STRING)) {
					//Identify the possible axiom
					
					//Set conditions 1
					boolean hasDef1SubClassA = false;
					boolean hasDef2SubClassB = false;
					boolean hasDef2DiffClassA = false;
					boolean hasDef1DiffClassB = false;
					//Set conditions 2
					boolean hasDef2SubClassA = false;
					boolean hasDef1SubClassB = false;
					boolean hasDef1DiffClassA = false;
					boolean hasDef2DiffClassB = false;
					
					//Search for the other subAxioms
					for (DLClause subDLClause : ontology.getDLClauses()) {
						if (subDLClause.isAtomicConceptInclusion() && subDLClause.isGeneralConceptInclusion() && subDLClause.getHeadLength() == 1 && subDLClause.getBodyLength() == 1) { 
							
							//<#A>(X) :- <internal:def#1>(X) || <internal:def#1> :- <#A>(X)
							if ((subDLClause.getHeadAtom(0).getDLPredicate().toString().equals(classA.toString()) && subDLClause.getBodyAtom(0).toString().equals(def1.toString())) ||
									subDLClause.getHeadAtom(0).toString().equals(def1.toString()) && subDLClause.getBodyAtom(0).getDLPredicate().toString().equals(classA.toString())) {
								hasDef1SubClassA = true;
							}
							
							//<#B>(X) :- <internal:def#2>(X) || <internal:def#2> :- <#B>(X)
							if ((subDLClause.getHeadAtom(0).getDLPredicate().toString().equals(classB.toString()) && subDLClause.getBodyAtom(0).toString().equals(def2.toString())) ||
									subDLClause.getHeadAtom(0).toString().equals(def2.toString()) && subDLClause.getBodyAtom(0).getDLPredicate().toString().equals(classB.toString())) {
								hasDef2SubClassB = true;
							}
																
							//<#B>(X) :- <internal:def#1>(X) || <internal:def#1> :- <#B>(X)
							if ((subDLClause.getHeadAtom(0).getDLPredicate().toString().equals(classB.toString()) && subDLClause.getBodyAtom(0).toString().equals(def1.toString())) ||
									subDLClause.getHeadAtom(0).toString().equals(def1.toString()) && subDLClause.getBodyAtom(0).getDLPredicate().toString().equals(classB.toString())) {
								hasDef1SubClassB = true;
							}
							
							//<#A>(X) :- <internal:def#2>(X) || <internal:def#2> :- <#A>(X)
							if ((subDLClause.getHeadAtom(0).getDLPredicate().toString().equals(classA.toString()) && subDLClause.getBodyAtom(0).toString().equals(def2.toString())) ||
									subDLClause.getHeadAtom(0).toString().equals(def2.toString()) && subDLClause.getBodyAtom(0).getDLPredicate().toString().equals(classA.toString())) {
								hasDef2SubClassA = true;
							}
						} else if (subDLClause.isGeneralConceptInclusion() && subDLClause.getHeadLength() == 0 && subDLClause.getBodyLength() == 2) {
							
							//:- <#A>(X), <internal:def#2>(X) || :- <internal:def#2>(X), <#A>(X)
							if ((subDLClause.getBodyAtom(0).getDLPredicate().toString().equals(classA.toString()) && subDLClause.getBodyAtom(1).toString().equals(def2.toString())) || 
									(subDLClause.getBodyAtom(0).toString().equals(def2.toString()) && subDLClause.getBodyAtom(1).getDLPredicate().toString().equals(classA.toString()))) {
								hasDef2DiffClassA = true;
							}
							
							//:- <#B>(X), <internal:def#1>(X) || :- <internal:def#1>(X), <#B>(X)
							if ((subDLClause.getBodyAtom(0).getDLPredicate().toString().equals(classB.toString()) && subDLClause.getBodyAtom(1).toString().equals(def1.toString())) || 
									(subDLClause.getBodyAtom(0).toString().equals(def1.toString()) && subDLClause.getBodyAtom(1).getDLPredicate().toString().equals(classB.toString()))) {
								hasDef1DiffClassB = true;
							}
														
							//:- <#A>(X), <internal:def#1>(X) || :- <internal:def#1>(X), <#A>(X)
							if ((subDLClause.getBodyAtom(0).getDLPredicate().toString().equals(classA.toString()) && subDLClause.getBodyAtom(1).toString().equals(def1.toString())) || 
									(subDLClause.getBodyAtom(0).toString().equals(def1.toString()) && subDLClause.getBodyAtom(1).getDLPredicate().toString().equals(classA.toString()))) {
								hasDef1DiffClassA = true;
							}
							
							//:- <#B>(X), <internal:def#2>(X) || :- <internal:def#2>(X), <#B>(X)
							if ((subDLClause.getBodyAtom(0).getDLPredicate().toString().equals(classB.toString()) && subDLClause.getBodyAtom(1).toString().equals(def2.toString())) || 
									(subDLClause.getBodyAtom(0).toString().equals(def2.toString()) && subDLClause.getBodyAtom(1).getDLPredicate().toString().equals(classB.toString()))) {
								hasDef2DiffClassB = true;
							}
						}
					}
					
					if ((hasDef1SubClassA && hasDef2SubClassB && hasDef2DiffClassA && hasDef1DiffClassB) || 
							(hasDef2SubClassA && hasDef1SubClassB && hasDef1DiffClassA && hasDef2DiffClassB)) {
						return def0;
					}
				}
			}
		}
		return null;
	}
	
	public static boolean containsSubClassOfAxiom(OWLClassExpression classA, OWLClassExpression classB, DLOntology ontology) {
		for (DLClause dlClause : ontology.getDLClauses()) {
			if (dlClause.isAtomicConceptInclusion()) {
				if (dlClause.getHeadAtom(0).getDLPredicate().toString().equals(classA.toString()) && dlClause.getBodyAtom(0).getDLPredicate().toString().equals(classB.toString())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean addSubClassOfAxioms(OWLClassExpression classA, OWLClassExpression classB, DLOntology ontology, Tableau tableau) {
		
		Atom classAAtom = Atom.create(AtomicConcept.create(classA.toString().substring(1, classA.toString().length()-1)), Variable.create("X"));
		Atom classBAtom = Atom.create(AtomicConcept.create(classB.toString().substring(1, classB.toString().length()-1)), Variable.create("X"));
		
		Atom[] headAtoms1 = {classAAtom};
		Atom[] bodyAtoms1 = {classBAtom};
		
		DLClause dlClause1 = DLClause.create(headAtoms1, bodyAtoms1);
		
		Atom[] headAtoms2 = {classBAtom};
		Atom[] bodyAtoms2 = {classAAtom};
		
		DLClause dlClause2 = DLClause.create(headAtoms2, bodyAtoms2);
		
		ontology.getDLClauses().add(dlClause1);
		ontology.getDLClauses().add(dlClause2);
		
		List<DLClause> dlClauses = new ArrayList<DLClause>() { 
            { 
                add(dlClause1); 
                add(dlClause2); 
            } 
        }; 

		createHyperResolutionManager(tableau, dlClauses);
		
		return true;
	}
	
	private static int getNextDef(DLOntology ontology) {
		int nextDef = -1;
		for (DLClause dlClause : ontology.getDLClauses()) {
			for (Atom atom : dlClause.getHeadAtoms()) {
				if (atom.getDLPredicate().toString().startsWith(DEF_STRING)) {
					String defString = atom.getDLPredicate().toString().substring(atom.getDLPredicate().toString().indexOf("#")+1, atom.getDLPredicate().toString().length() - 1);
					int def = Integer.parseInt(defString);
					if (def > nextDef) {
						nextDef = def;
					}
				}
			}
			for (Atom atom : dlClause.getBodyAtoms()) {
				if (atom.getDLPredicate().toString().startsWith(DEF_STRING)) {
					String defString = atom.getDLPredicate().toString().substring(atom.getDLPredicate().toString().indexOf("#")+1, atom.getDLPredicate().toString().length() - 1);
					int def = Integer.parseInt(defString);
					if (def > nextDef) {
						nextDef = def;
					}
				}
			}
		}
		return nextDef + 1;
	}
	
	public static void addInequalityMetamodellingRuleAxiom(OWLClassExpression classA, OWLClassExpression classB, DLOntology ontology, Tableau tableau, Atom def0AtomParam, Map<OWLClassExpression,Map<OWLClassExpression,Atom>> inequalityMetamodellingPairs) {
		
		if (def0AtomParam == null) {
			int nextDef = getNextDef(ontology);
			String def0 = DEF_STRING + nextDef + ">";
			String def1 = DEF_STRING + (nextDef+1) + ">";
			String def2 = DEF_STRING + (nextDef+2) + ">";
			
			Atom def0Atom = Atom.create(AtomicConcept.create(def0.substring(1, def0.length()-1)), Variable.create("X"));
			Atom def1Atom = Atom.create(AtomicConcept.create(def1.substring(1, def1.length()-1)), Variable.create("X"));
			Atom def2Atom = Atom.create(AtomicConcept.create(def2.substring(1, def2.length()-1)), Variable.create("X"));
			Atom classAAtom = Atom.create(AtomicConcept.create(classA.toString().substring(1, classA.toString().length()-1)), Variable.create("X"));
			Atom classBAtom = Atom.create(AtomicConcept.create(classB.toString().substring(1, classB.toString().length()-1)), Variable.create("X"));
		
			Atom[] headAtoms1 = {def1Atom, def2Atom};
			Atom[] bodyAtoms1 = {def0Atom};
			
			DLClause dlClause1 = DLClause.create(headAtoms1, bodyAtoms1);
			
			Atom[] headAtoms2 = {classAAtom};
			Atom[] bodyAtoms2 = {def1Atom};
			
			DLClause dlClause2 = DLClause.create(headAtoms2, bodyAtoms2);
			
			Atom[] headAtoms3 = {classBAtom};
			Atom[] bodyAtoms3 = {def2Atom};
		
			DLClause dlClause3 = DLClause.create(headAtoms3, bodyAtoms3);
			
			Atom[] headAtoms4 = {};
			Atom[] bodyAtoms4 = {classAAtom, def2Atom};
			
			DLClause dlClause4 = DLClause.create(headAtoms4, bodyAtoms4);
			
			Atom[] headAtoms5 = {};
			Atom[] bodyAtoms5 = {classBAtom, def1Atom};
			
			DLClause dlClause5 = DLClause.create(headAtoms5, bodyAtoms5);
			
			ontology.getDLClauses().add(dlClause1);
			
			ontology.getDLClauses().add(dlClause1);
			ontology.getDLClauses().add(dlClause2);
			ontology.getDLClauses().add(dlClause3);
			ontology.getDLClauses().add(dlClause4);
			ontology.getDLClauses().add(dlClause5);
			
			List<DLClause> dlClauses = new ArrayList<DLClause>() { 
	            { 
	                add(dlClause1); 
	                add(dlClause2); 
	                add(dlClause3); 
	                add(dlClause4); 
	                add(dlClause5);
	            } 
	        }; 
	        
			DependencySet dependencySet = tableau.m_dependencySetFactory.getActualDependencySet();

	        //create node
	        Node zNode = tableau.createNewNamedNode(dependencySet);
	        
	        //create axiom in binary table
	        tableau.getExtensionManager().addConceptAssertion((LiteralConcept) def0Atom.getDLPredicate(), zNode, dependencySet, true);

			createHyperResolutionManager(tableau, dlClauses);
			
			inequalityMetamodellingPairs.putIfAbsent(classA, new HashMap<OWLClassExpression,Atom>());
			inequalityMetamodellingPairs.get(classA).putIfAbsent(classB, def0Atom);
			tableau.m_metamodellingManager.defAssertions.add(def0);
			
		} else {
			DependencySet dependencySet = tableau.m_dependencySetFactory.getActualDependencySet();

	        //create node
	        Node zNode = tableau.createNewNamedNode(dependencySet);
	        
	        //create axiom in binary table
	        tableau.getExtensionManager().addConceptAssertion((LiteralConcept) def0AtomParam.getDLPredicate(), zNode, dependencySet, true);
	        tableau.m_metamodellingManager.defAssertions.add(def0AtomParam.getDLPredicate().toString());
		}	
	}
	
	private static void createHyperResolutionManager(Tableau tableau, List<DLClause> dlClauses) {
		
		HyperresolutionManager hypM =  new HyperresolutionManager(tableau, tableau.getPermanentDLOntology().getDLClauses());
		
		BranchedHyperresolutionManager branchedHypM = new BranchedHyperresolutionManager();
		branchedHypM.setHyperresolutionManager(hypM);
		branchedHypM.setBranchingIndex(tableau.getCurrentBranchingPointLevel());
		branchedHypM.setBranchingPoint(tableau.getM_currentBranchingPoint());
		for (DLClause dlClause: dlClauses) {
			branchedHypM.getDlClausesAdded().add(dlClause);
		}
		
		tableau.getBranchedHyperresolutionManagers().add(branchedHypM);	

		
		tableau.setPermanentHyperresolutionManager(hypM);

	}
  
}
