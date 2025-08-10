package org.semanticweb.HermiT.tableau;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.ArrayDeque;
import java.util.Deque;
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
	private static Deque<Node> nodeStack;
	private static boolean existCycle;
	private static Set<Node> flaggedNodes;

	public static boolean findCyclesInM(Tableau tableau) {
		flaggedNodes = new HashSet<Node>();
		nodeStack = new ArrayDeque<Node>();
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

		System.out.println("    Created DL clauses:");
		System.out.println("    " + dlClause1);
		System.out.println("    " + dlClause2);

		ontology.getDLClauses().add(dlClause1);
		ontology.getDLClauses().add(dlClause2);

		System.out.println("    Total DL clauses in ontology now: " + ontology.getDLClauses().size());

		List<DLClause> dlClauses = new ArrayList<DLClause>() {
            {
                add(dlClause1);
                add(dlClause2);
            }
        };

		System.out.println("    Creating new HyperresolutionManager...");
		createHyperResolutionManager(tableau, dlClauses);

		// CRITICAL FIX: Immediately apply the new axioms to existing facts
		System.out.println("    Immediately applying new axioms to existing facts...");

		// The new HyperresolutionManager has been set, now we need to trigger its application
		// to existing facts. The key insight is that we need to make the existing facts
		// available in the DELTA_OLD range so they get processed by applyDLClauses()

		// Force propagation to make existing facts available for re-processing
		boolean hadDelta = tableau.getExtensionManager().propagateDeltaNew();
		System.out.println("    Delta propagation result: " + hadDelta);

		// Now immediately apply the new DL clauses
		tableau.getPermanentHyperresolutionManager().applyDLClauses();
		System.out.println("    Applied new DL clauses, clash status: " + tableau.getExtensionManager().containsClash());

		System.out.println("    +++ addSubClassOfAxioms END +++");

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

	public static boolean areClassesDisjoint(OWLClassExpression classA, OWLClassExpression classB, DLOntology ontology, Tableau tableau) {
		// Check for explicit disjointness in DL clauses
		for (DLClause dlClause : ontology.getDLClauses()) {
			// Look for clauses of the form: :- A(X), B(X) (bottom/clash from A and B)
			if (dlClause.getHeadLength() == 0 && dlClause.getBodyLength() == 2) {
				Atom bodyAtom1 = dlClause.getBodyAtom(0);
				Atom bodyAtom2 = dlClause.getBodyAtom(1);
				// Check if the body atoms correspond to our classes
				// Note: classA.toString() gives "<TE7#A1>", DL predicate also gives "<TE7#A1>"
				String classAStr = classA.toString();
				String classBStr = classB.toString();
				if ((bodyAtom1.getDLPredicate().toString().equals(classAStr) &&
					 bodyAtom2.getDLPredicate().toString().equals(classBStr)) ||
					(bodyAtom1.getDLPredicate().toString().equals(classBStr) &&
					 bodyAtom2.getDLPredicate().toString().equals(classAStr))) {
					System.out.println("    Found explicit disjointness clause: " + dlClause);
					return true;
				}
			}
		}
		// Check for INDIRECT disjointness through subsumption chains
		// If A1 ≡ A3 and A2 ⊆ A3 and A1 ∩ A2 = ∅, then A1 and A3 are indirectly disjoint
		System.out.println("    Checking for indirect disjointness...");
		if (checkIndirectDisjointness(classA, classB, ontology)) {
			return true;
		}

		// Check for complementary relationships
		// Look for patterns like: A(X) :- not(B(X)) or B(X) :- not(A(X))
		for (DLClause dlClause : ontology.getDLClauses()) {
			if (dlClause.getHeadLength() == 1 && dlClause.getBodyLength() == 1) {
				Atom headAtom = dlClause.getHeadAtom(0);
				Atom bodyAtom = dlClause.getBodyAtom(0);

				// Check if A implies not(B) or B implies not(A)
				String fullClassA = classA.toString();
				String fullClassB = classB.toString();

				if (headAtom.getDLPredicate().toString().equals(fullClassA) &&
					bodyAtom.getDLPredicate().toString().equals("not(" + fullClassB + ")")) {
					return true;
				}
				if (headAtom.getDLPredicate().toString().equals(fullClassB) &&
					bodyAtom.getDLPredicate().toString().equals("not(" + fullClassA + ")")) {
					return true;
				}
			}
		}

		// CRITICAL FIX: Check if any individual has classA and not(classB) or classB and not(classA)
		// This handles the case where p is A1 and not(A2) - making A1 ≡ A2 would be inconsistent
		// Check the binary extension table for conflicting assertions
		// Look for any tuple where a node has both A and not(B) or B and not(A)
		ExtensionTable binaryExtensionTable = tableau.getExtensionManager().getBinaryExtensionTable();

		// Create a map to track which nodes have which assertions
		Map<Node, Set<String>> nodeAssertions = new HashMap<>();

		// Retrieve all binary concept assertions (concept, node)
		Object[] tupleBuffer = new Object[2];
		ExtensionTable.Retrieval retrieval = binaryExtensionTable.createRetrieval(new int[]{-1, -1}, new Object[2], tupleBuffer, true, ExtensionTable.View.TOTAL);

		try {
			retrieval.open();
			while (!retrieval.afterLast()) {
				// tupleBuffer[0] is the concept, tupleBuffer[1] is the node
				Object concept = tupleBuffer[0];
				Node node = (Node) tupleBuffer[1];
				nodeAssertions.putIfAbsent(node, new HashSet<>());
				nodeAssertions.get(node).add(concept.toString());

				retrieval.next();
			}
		} finally {
			retrieval.clear();
		}

		// Check for conflicting assertions
		for (Map.Entry<Node, Set<String>> entry : nodeAssertions.entrySet()) {
			Node node = entry.getKey();
			Set<String> assertions = entry.getValue();

			// Use the full class names with < > brackets as they appear in the assertions
			String fullClassA = classA.toString(); // e.g., "<TE1#A2>"
			String fullClassB = classB.toString(); // e.g., "<TE1#A1>"

			boolean hasClassA = assertions.contains(fullClassA);
			boolean hasClassB = assertions.contains(fullClassB);
			boolean hasNotClassA = assertions.contains("not(" + fullClassA + ")");
			boolean hasNotClassB = assertions.contains("not(" + fullClassB + ")");

			// Check if the node has A and not(B) or B and not(A)
			if ((hasClassA && hasNotClassB) || (hasClassB && hasNotClassA)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check for indirect disjointness through subsumption chains.
	 * Example: If A1 ∩ A2 = ∅ and A2 ⊆ A3, then A1 and A3 are indirectly disjoint
	 * because A1 ≡ A3 would imply A2 ⊆ A1, but A1 ∩ A2 = ∅.
	 */
	private static boolean checkIndirectDisjointness(OWLClassExpression classA, OWLClassExpression classB, DLOntology ontology) {
		String classAStr = classA.toString();
		String classBStr = classB.toString();

		// Collect all subsumption relationships (C ⊆ D) from DL clauses
		Map<String, Set<String>> subsumptions = new HashMap<>();

		// Collect all direct disjointness relationships from DL clauses
		Set<String> disjointPairs = new HashSet<>();

		for (DLClause dlClause : ontology.getDLClauses()) {
			// Look for subsumption: C(X) :- D(X) means D ⊆ C
			if (dlClause.getHeadLength() == 1 && dlClause.getBodyLength() == 1) {
				Atom headAtom = dlClause.getHeadAtom(0);
				Atom bodyAtom = dlClause.getBodyAtom(0);

				String superClass = headAtom.getDLPredicate().toString();
				String subClass = bodyAtom.getDLPredicate().toString();

				subsumptions.putIfAbsent(superClass, new HashSet<>());
				subsumptions.get(superClass).add(subClass);

				// System.out.println("    Found subsumption: " + subClass + " ⊆ " + superClass);
			}

			// Look for disjointness: :- A(X), B(X) means A ∩ B = ∅
			if (dlClause.getHeadLength() == 0 && dlClause.getBodyLength() == 2) {
				Atom bodyAtom1 = dlClause.getBodyAtom(0);
				Atom bodyAtom2 = dlClause.getBodyAtom(1);

				String class1 = bodyAtom1.getDLPredicate().toString();
				String class2 = bodyAtom2.getDLPredicate().toString();

				disjointPairs.add(class1 + "|" + class2);
				disjointPairs.add(class2 + "|" + class1);

				// System.out.println("    Found disjointness: " + class1 + " ∩ " + class2 + " = ∅");
			}
		}

		// Check if A and B are indirectly disjoint
		// Case 1: A ∩ C = ∅ and C ⊆ B → A and B are indirectly disjoint
		if (subsumptions.containsKey(classBStr)) {
			for (String subClassOfB : subsumptions.get(classBStr)) {
				if (disjointPairs.contains(classAStr + "|" + subClassOfB)) {
					System.out.println("    Found indirect disjointness: " + classAStr + " ∩ " + subClassOfB + " = ∅ and " + subClassOfB + " ⊆ " + classBStr);
					return true;
				}
			}
		}

		// Case 2: B ∩ C = ∅ and C ⊆ A → A and B are indirectly disjoint
		if (subsumptions.containsKey(classAStr)) {
			for (String subClassOfA : subsumptions.get(classAStr)) {
				if (disjointPairs.contains(classBStr + "|" + subClassOfA)) {
					System.out.println("    Found indirect disjointness: " + classBStr + " ∩ " + subClassOfA + " = ∅ and " + subClassOfA + " ⊆ " + classAStr);
					return true;
				}
			}
		}

		// System.out.println("    No indirect disjointness found");
		return false;
	}

	/**
	 * Check for indirect disjointness through subsumption chains.
	 * Example: If A1 ∩ A2 = ∅ and A2 ⊆ A3, then A1 and A3 are indirectly disjoint
	 * because A1 ≡ A3 would imply A2 ⊆ A1, but A1 ∩ A2 = ∅.
	 */
	private static boolean checkIndirectDisjointness(OWLClassExpression classA, OWLClassExpression classB, DLOntology ontology) {
		String classAStr = classA.toString();
		String classBStr = classB.toString();

		// Collect all subsumption relationships (C ⊆ D) from DL clauses
		Map<String, Set<String>> subsumptions = new HashMap<>();

		// Collect all direct disjointness relationships from DL clauses
		Set<String> disjointPairs = new HashSet<>();

		for (DLClause dlClause : ontology.getDLClauses()) {
			// Look for subsumption: C(X) :- D(X) means D ⊆ C
			if (dlClause.getHeadLength() == 1 && dlClause.getBodyLength() == 1) {
				Atom headAtom = dlClause.getHeadAtom(0);
				Atom bodyAtom = dlClause.getBodyAtom(0);

				String superClass = headAtom.getDLPredicate().toString();
				String subClass = bodyAtom.getDLPredicate().toString();

				subsumptions.putIfAbsent(superClass, new HashSet<>());
				subsumptions.get(superClass).add(subClass);

				// System.out.println("    Found subsumption: " + subClass + " ⊆ " + superClass);
			}

			// Look for disjointness: :- A(X), B(X) means A ∩ B = ∅
			if (dlClause.getHeadLength() == 0 && dlClause.getBodyLength() == 2) {
				Atom bodyAtom1 = dlClause.getBodyAtom(0);
				Atom bodyAtom2 = dlClause.getBodyAtom(1);

				String class1 = bodyAtom1.getDLPredicate().toString();
				String class2 = bodyAtom2.getDLPredicate().toString();

				disjointPairs.add(class1 + "|" + class2);
				disjointPairs.add(class2 + "|" + class1);

				// System.out.println("    Found disjointness: " + class1 + " ∩ " + class2 + " = ∅");
			}
		}

		// Check if A and B are indirectly disjoint
		// Case 1: A ∩ C = ∅ and C ⊆ B → A and B are indirectly disjoint
		if (subsumptions.containsKey(classBStr)) {
			for (String subClassOfB : subsumptions.get(classBStr)) {
				if (disjointPairs.contains(classAStr + "|" + subClassOfB)) {
					System.out.println("    Found indirect disjointness: " + classAStr + " ∩ " + subClassOfB + " = ∅ and " + subClassOfB + " ⊆ " + classBStr);
					return true;
				}
			}
		}

		// Case 2: B ∩ C = ∅ and C ⊆ A → A and B are indirectly disjoint
		if (subsumptions.containsKey(classAStr)) {
			for (String subClassOfA : subsumptions.get(classAStr)) {
				if (disjointPairs.contains(classBStr + "|" + subClassOfA)) {
					System.out.println("    Found indirect disjointness: " + classBStr + " ∩ " + subClassOfA + " = ∅ and " + subClassOfA + " ⊆ " + classAStr);
					return true;
				}
			}
		}

		// System.out.println("    No indirect disjointness found");
		return false;
	}

}
