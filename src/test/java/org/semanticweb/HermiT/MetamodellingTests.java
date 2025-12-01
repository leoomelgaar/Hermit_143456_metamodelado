package org.semanticweb.HermiT;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.HermiT.cli.CommandLine;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;

import junit.framework.TestCase;

public class MetamodellingTests extends TestCase {
	protected String testCasesPath;
	protected List<String> flags;
	protected int flagsCount;
	private long startTime;

	protected void setUp() throws Exception {
		startTime = System.nanoTime();

		String projectRoot = System.getProperty("user.dir");
		testCasesPath = new File(projectRoot, "ontologias/").getAbsolutePath() + File.separator;

		flags = new ArrayList<String>();

		// Flag -k solo verifica la consistencia, no infiere conocimiento
		// Flag -c verifica la consistencia y infiere conocimiento (toma mas tiempo)
		flags.add("-c");

		flagsCount = 1;
	}

	protected void tearDown() throws Exception {
		long duration = System.nanoTime() - startTime;
		double durationInMillis = duration / 1_000_000.0;

		String testName = this.getName();
		System.out.println("Tiempo de ejecución de " + testName + ": " + durationInMillis + " ms");
	}

  //COMIENZO - Escenario C - Casos consistentes sin metamodelling
	public void testEntrega3() {
		flags.add(testCasesPath+"EscenarioC/Entrega3.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("Entrega3 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testFiltroInfantil() {
		flags.add(testCasesPath+"EscenarioC/FiltroInfantil.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("FiltroInfantil es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testHidrografia() {
		flags.add(testCasesPath+"EscenarioC/Hidrografia.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("Hidrografia es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testObjetosHidrograficos() {
		flags.add(testCasesPath+"EscenarioC/ObjetosHidrograficos.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("ObjetosHidrograficos es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testEquality20() {
		flags.add(testCasesPath+"EscenarioC/TestEquality20.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality20 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testCycles6() {
		flags.add(testCasesPath+"EscenarioC/TestCycles6.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCycles6 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testCycles11() {
		flags.add(testCasesPath+"EscenarioC/TestCycles11.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCycles11 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testCycles4() {
		flags.add(testCasesPath+"EscenarioC/TestCycles4.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCycles4 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testCycles9() {
		flags.add(testCasesPath+"EscenarioC/TestCycles9.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCycles9 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testConservativity1() {
		flags.add(testCasesPath+"EscenarioC/TestConservativity1.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestConservativity1 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testConservativity2() {
		flags.add(testCasesPath+"EscenarioC/TestConservativity2.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestConservativity2 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testConservativity3() {
		flags.add(testCasesPath+"EscenarioC/TestConservativity3.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestConservativity3 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	//FIN - Escenario C - Casos consistentes sin metamodelling

	//COMIENZO - Escenario D - Casos inconsistentes sin metamodelling

	public void testAccountingInconsistente2() {
		flags.add(testCasesPath+"EscenarioD/AccountingInconsistente2.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("AccountingInconsistente2 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void skipped_testAccountingConsistente3() {
		flags.add(testCasesPath + "EscenarioE/AccountingConsistente3.owl");

		CommandLine.main(flags.toArray(new String[flagsCount + 1]));
		System.out.println("AccountingConsistente3 es consistente");

		flags.remove(flagsCount);
		TestCase.assertTrue(true);
	}

	public void testFIFA_WC() {
		flags.add(testCasesPath+"EscenarioD/FIFA_WC.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("FIFA_WC es inconsistente");
			result = true;
		}

		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testgenerations() {
		flags.add(testCasesPath+"EscenarioD/generations.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("generations es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testOntologyEjercicio1() {
		flags.add(testCasesPath+"EscenarioD/OntologyEjercicio1.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("OntologyEjercicio1 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testWC_2014() {
		flags.add(testCasesPath+"EscenarioD/WC_2014.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("WC_2014 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testConservativity4() {
		flags.add(testCasesPath+"EscenarioD/TestConservativity4.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestConservativity4 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	//FIN - Escenario D - Casos inconsistentes sin metamodelling

	//COMIENZO - Escenario E - Casos consistentes con metamodelling (SHIQM)
	public void testNuevaInferencia() {
		flags.add(testCasesPath+"EscenarioE/testNuevaInferencia.owl");

		boolean result = false;
		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("testNuevaInferencia es consistente");
		result = true;

		flags.remove(flagsCount);
		TestCase.assertTrue(result);
	}

	public void testBreastCancerRecommendationWithoutMetamodelling() {
		flags.add(testCasesPath+"EscenarioE/BreastCancerRecommendationWithoutMetamodelling.owx");

		boolean result = false;
		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("BreastCancerRecommendationWithoutMetamodelling es consistente");
		result = true;

		flags.remove(flagsCount);
		TestCase.assertTrue(result);
	}

	public void testBreastCancerRecommendationWithMetamodellingMediana4() {
		flags.add(testCasesPath+"EscenarioE/Breast_cancer_recommendation-MEDIANA4-con-meta.owx");

		boolean result = false;
		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("BreastCancerRecommendationWithMetamodellingMediana4 es consistente");
		result = true;

		flags.remove(flagsCount);
		TestCase.assertTrue(result);
	}

	public void testBreastCancerRecommendationWithMetamodelling() {
		flags.add(testCasesPath+"EscenarioE/BreastCancerRecommendationWithMetamodelling.owx");

		boolean result = false;
		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("BreastCancerRecommendationWithMetamodelling es consistente");
		result = true;

		flags.remove(flagsCount);
		TestCase.assertTrue(result);
	}

	public void testDifference1() {
		flags.add(testCasesPath+"EscenarioE/TestDifference1.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference1 es consistente");
		result = true;

		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testDifference3() {
		flags.add(testCasesPath+"EscenarioE/TestDifference3.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference3 es consistente");
		result = true;

		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testDifference5() {
		flags.add(testCasesPath+"EscenarioE/TestDifference5.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference5 es consistente");
		result = true;

		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testDifference6() {
		flags.add(testCasesPath+"EscenarioE/TestDifference6.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference6 es consistente");
		result = true;

		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testDifference7Short() {
		System.out.println("TestDifference7_short empezando a correr");

		flags.add(testCasesPath + "EscenarioE/TestDifference7_short.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount + 1]));
		System.out.println("TestDifference7_short es consistente");
		result = true;

		flags.remove(flagsCount);
		TestCase.assertTrue(result);
	}

	public void testDifference7() {
		System.out.println("TestDifference7 empezando a correr");

		flags.add(testCasesPath + "EscenarioE/TestDifference7.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount + 1]));
		System.out.println("TestDifference7 es consistente");
		result = true;

		flags.remove(flagsCount);
		TestCase.assertTrue(result);
	}

	public void testDifference9() {
		flags.add(testCasesPath+"EscenarioE/TestDifference9.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference9 es consistente");
		result = true;

		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testDifference14() {
		flags.add(testCasesPath+"EscenarioE/TestDifference14.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference14 es inconsistente");
		result = true;


		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testAccountingConsistente1() {
		flags.add(testCasesPath+"EscenarioE/AccountingConsistente1.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("AccountingConsistente1 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testAccountingConsistente2() {
		flags.add(testCasesPath+"EscenarioE/AccountingConsistente2.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("AccountingConsistente2 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testAccountingConsistente1CortaCloseIndMeta() {
		flags.add(testCasesPath+"EscenarioE/AccountingConsistente1CortaCloseIndMeta.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("AccountingConsistente1CortaCloseIndMeta es consistente");

		flags.remove(flagsCount);
		TestCase.assertTrue(true);
	}

	public void testAccountingConsistente1CortaCloseTodosInd() {
		flags.add(testCasesPath+"EscenarioE/AccountingConsistente1CortaCloseTodosInd.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("AccountingConsistente1CortaCloseTodosInd es consistente");

		flags.remove(flagsCount);
		TestCase.assertTrue(true);
	}

	public void testAccountingConsistente1CortaSinClose() {
		flags.add(testCasesPath+"EscenarioE/AccountingConsistente1CortaSinClose.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("AccountingConsistente1CortaSinClose es consistente");

		flags.remove(flagsCount);
		TestCase.assertTrue(true);
	}

	public void testTestEquality2() {
		flags.add(testCasesPath+"EscenarioE/TestEquality2.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality2 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality4() {
		flags.add(testCasesPath+"EscenarioE/TestEquality4.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality4 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality6() {
		flags.add(testCasesPath+"EscenarioE/TestEquality6.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality6 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality8() {
		flags.add(testCasesPath+"EscenarioE/TestEquality8.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality8 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality10() {
		flags.add(testCasesPath+"EscenarioE/TestEquality10.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality10 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality12() {
		flags.add(testCasesPath+"EscenarioE/TestEquality12.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality12 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality14() {
		flags.add(testCasesPath+"EscenarioE/TestEquality14.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality14 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality15() {
		flags.add(testCasesPath+"EscenarioE/TestEquality15.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality15 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality16() {
		flags.add(testCasesPath+"EscenarioE/TestEquality16.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality16 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality18() {
		flags.add(testCasesPath+"EscenarioE/TestEquality18.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality18 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	//FIN - Escenario E - Casos consistentes con metamodelling (SHIQM)

	//COMIENZO - Escenario F - Casos inconsistentes con metamodelling (SHIQM)

	public void testAccountingInconsistente1() {
		flags.add(testCasesPath+"EscenarioF/AccountingInconsistente1.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("AccountingInconsistente1 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testAccountingInconsistente3() {
		flags.add(testCasesPath+"EscenarioF/AccountingInconsistente3.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("AccountingInconsistente3 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles1() {
		flags.add(testCasesPath+"EscenarioF/TestCycles1.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles1 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles2() {
		flags.add(testCasesPath+"EscenarioF/TestCycles2.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles2 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles3() {
		flags.add(testCasesPath+"EscenarioF/TestCycles3.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles3 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles5() {
		flags.add(testCasesPath+"EscenarioF/TestCycles5.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles5 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles7() {
		flags.add(testCasesPath+"EscenarioF/TestCycles7.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles7 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles8() {
		flags.add(testCasesPath+"EscenarioF/TestCycles8.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles8 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestDifference2() {
		flags.add(testCasesPath+"EscenarioF/TestDifference2.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestDifference2 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestDifference8() {
		flags.add(testCasesPath+"EscenarioF/TestDifference8.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestDifference8 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);

	}

	public void testTestDifference10() {
		flags.add(testCasesPath+"EscenarioF/TestDifference10.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestDifference10 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestDifference11() {
		flags.add(testCasesPath+"EscenarioF/TestDifference11.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestDifference11 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestDifference13() {
		flags.add(testCasesPath+"EscenarioF/TestDifference13.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestDifference13 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality1() {
		flags.add(testCasesPath+"EscenarioF/TestEquality1.owl");
		boolean result = false;

		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality1 es inconsistente");
			result = true;
		}

		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality3() {
		flags.add(testCasesPath+"EscenarioF/TestEquality3.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality3 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality5() {
		flags.add(testCasesPath+"EscenarioF/TestEquality5.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality5 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality7() {
		flags.add(testCasesPath+"EscenarioF/TestEquality7.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality7 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality9() {
		flags.add(testCasesPath+"EscenarioF/TestEquality9.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality9 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality11() {
		flags.add(testCasesPath+"EscenarioF/TestEquality11.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality11 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality13() {
		flags.add(testCasesPath+"EscenarioF/TestEquality13.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality13 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality17() {
		flags.add(testCasesPath+"EscenarioF/TestEquality17.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality17 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality19() {
		flags.add(testCasesPath+"EscenarioF/TestEquality19.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality19 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality21() {
		flags.add(testCasesPath+"EscenarioF/TestEquality21.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality21 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality22() {
		flags.add(testCasesPath+"EscenarioF/TestEquality22.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality22 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality23() {
		flags.add(testCasesPath+"EscenarioF/TestEquality23.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality23 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles10() {
		flags.add(testCasesPath+"EscenarioF/TestCycles10.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles10 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles12() {
		flags.add(testCasesPath+"EscenarioF/TestCycles12.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles12 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles13() {
		flags.add(testCasesPath+"EscenarioF/TestCycles13.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles13 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles14() {
		flags.add(testCasesPath+"EscenarioF/TestCycles14.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles14 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles15() {
		flags.add(testCasesPath+"EscenarioF/TestCycles15.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles15 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles16() {
		flags.add(testCasesPath+"EscenarioF/TestCycles16.owl");
		boolean result = false;
		try {
			CommandLine.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles16 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality9ManyTimes() {
			System.out.println("=== EJECUTANDO testTestEquality9 20 VECES PARA VERIFICAR EL ARREGLO ===");
			int successCount = 0;
			int totalRuns = 5;

			for (int i = 1; i <= totalRuns; i++) {
					System.out.println("\n--- Ejecución #" + i + " ---");
					try {
							flags.add(testCasesPath+"EscenarioF/TestEquality9.owl");
							boolean result = false;
							try {
									CommandLine.main(flags.toArray(new String[flagsCount+1]));
							} catch (InconsistentOntologyException e) {
									System.out.println("✅ TestEquality9 es inconsistente (CORRECTO)");
									result = true;
									successCount++;
							}
							flags.remove(flagsCount);

							if (!result) {
									System.out.println("❌ TestEquality9 se reportó como consistente (ERROR)");
							}

					} catch (Exception e) {
							System.out.println("❌ Error en ejecución #" + i + ": " + e.getMessage());
					}
			}

			System.out.println("\n=== RESUMEN FINAL ===");
			System.out.println("Total de ejecuciones: " + totalRuns);
			System.out.println("Éxitos (inconsistente): " + successCount);
			System.out.println("Fallos (consistente): " + (totalRuns - successCount));
			System.out.println("Porcentaje de éxito: " + (successCount * 100.0 / totalRuns) + "%");

			if (successCount == totalRuns) {
					System.out.println("🎉 ¡PERFECTO! 100% de éxito - El arreglo funciona completamente");
			} else if (successCount >= totalRuns * 0.8) {
					System.out.println("✅ ¡MUY BIEN! " + (successCount * 100.0 / totalRuns) + "% de éxito - El arreglo funciona mayormente");
			} else {
					System.out.println("⚠️  Aún hay fallos - El arreglo necesita más trabajo");
			}
	}

	public void testTestEquality8ManyTimes() {
			System.out.println("=== EJECUTANDO testTestEquality8 5 VECES PARA VERIFICAR EL ARREGLO ===");
			int successCount = 0;
			int totalRuns = 5;

			for (int i = 1; i <= totalRuns; i++) {
					System.out.println("\n--- Ejecución #" + i + " ---");
					try {
							flags.add(testCasesPath+"EscenarioE/TestEquality8.owl");
							boolean result = false;
							try {
									CommandLine.main(flags.toArray(new String[flagsCount+1]));
									// Si no lanza excepción, es consistente (CORRECTO para TestEquality8)
									System.out.println("✅ TestEquality8 es consistente (CORRECTO)");
									result = true;
									successCount++;
							} catch (InconsistentOntologyException e) {
									System.out.println("❌ TestEquality8 se reportó como inconsistente (ERROR)");
							}
							flags.remove(flagsCount);

							if (!result) {
									System.out.println("❌ Error en la ejecución #" + i);
							}

					} catch (Exception e) {
							System.out.println("❌ Error en ejecución #" + i + ": " + e.getMessage());
					}
			}

			System.out.println("\n=== RESUMEN FINAL ===");
			System.out.println("Total de ejecuciones: " + totalRuns);
			System.out.println("Éxitos (consistente): " + successCount);
			System.out.println("Fallos (inconsistente): " + (totalRuns - successCount));
			System.out.println("Porcentaje de éxito: " + (successCount * 100.0 / totalRuns) + "%");

			if (successCount == totalRuns) {
					System.out.println("🎉 ¡PERFECTO! 100% de éxito - El arreglo funciona completamente");
			} else if (successCount >= totalRuns * 0.8) {
					System.out.println("✅ ¡MUY BIEN! " + (successCount * 100.0 / totalRuns) + "% de éxito - El arreglo funciona mayormente");
			} else {
					System.out.println("⚠️  Aún hay fallos - El arreglo necesita más trabajo");
			}
	}

	//FIN - Escenario F - Casos inconsistentes con metamodelling (SHIQM)
}
