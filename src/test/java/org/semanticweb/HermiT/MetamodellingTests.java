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
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/Entrega3.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("Entrega3 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testFiltroInfantil() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/FiltroInfantil.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("FiltroInfantil es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testHidrografia() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/Hidrografia.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("Hidrografia es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testObjetosHidrograficos() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/ObjetosHidrograficos.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("ObjetosHidrograficos es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testEquality20() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/TestEquality20.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality20 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testCycles6() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/TestCycles6.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCycles6 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testCycles11() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/TestCycles11.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCycles11 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testCycles4() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/TestCycles4.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCycles4 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testCycles9() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/TestCycles9.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCycles9 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testConservativity1() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/TestConservativity1.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestConservativity1 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testConservativity2() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/TestConservativity2.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestConservativity2 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testConservativity3() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioC/TestConservativity3.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestConservativity3 es consistente");


		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	//FIN - Escenario C - Casos consistentes sin metamodelling

	//COMIENZO - Escenario D - Casos inconsistentes sin metamodelling

	public void testAccountingInconsistente2() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioD/AccountingInconsistente2.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("AccountingInconsistente2 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testFIFA_WC() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioD/FIFA_WC.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("FIFA_WC es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testgenerations() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioD/generations.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("generations es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testOntologyEjercicio1() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioD/OntologyEjercicio1.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("OntologyEjercicio1 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testWC_2014() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioD/WC_2014.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("WC_2014 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testConservativity4() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioD/TestConservativity4.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestConservativity4 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	//FIN - Escenario D - Casos inconsistentes sin metamodelling

	//COMIENZO - Escenario E - Casos consistentes con metamodelling (SHIQM)
	public void testBreastCancerRecommendationWithoutMetamodelling() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/BreastCancerRecommendationWithoutMetamodelling.owx");

		boolean result = false;
		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("BreastCancerRecommendationWithoutMetamodelling es consistente");
		result = true;

		flags.remove(flagsCount);
		TestCase.assertTrue(result);
	}

	public void testBreastCancerRecommendationWithMetamodelling() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/BreastCancerRecommendationWithMetamodelling.owx");

		boolean result = false;
		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("BreastCancerRecommendationWithMetamodelling es consistente");
		result = true;

		flags.remove(flagsCount);
		TestCase.assertTrue(result);
	}

	public void testDifference1() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestDifference1.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference1 es consistente");
		result = true;

		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testDifference3() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestDifference3.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference3 es consistente");
		result = true;

		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testDifference5() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestDifference5.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference5 es consistente");
		result = true;

		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testDifference6() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestDifference6.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference6 es consistente");
		result = true;

		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testDifference7Short() {
		CommandLine cl = new CommandLine();
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
		CommandLine cl = new CommandLine();
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
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestDifference9.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference9 es consistente");
		result = true;

		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testDifference14() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestDifference14.owl");
		boolean result = false;

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestDifference14 es inconsistente");
		result = true;


		flags.remove(flagsCount);
        TestCase.assertTrue(result);
	}

	public void testAccountingConsistente1() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/AccountingConsistente1.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("AccountingConsistente1 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testAccountingConsistente1Corta() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/AccountingConsistente1Corta.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("AccountingConsistente1Corta es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testAccountingConsistente2() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/AccountingConsistente2.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("AccountingConsistente2 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testAccountingConsistente3() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/AccountingConsistente3.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("AccountingConsistente3 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality2() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestEquality2.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality2 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality4() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestEquality4.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality4 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality6() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestEquality6.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality6 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality8() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestEquality8.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality8 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality10() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestEquality10.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality10 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality12() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestEquality12.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality12 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality14() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestEquality14.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality14 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality15() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestEquality15.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality15 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality16() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestEquality16.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality16 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	public void testTestEquality18() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioE/TestEquality18.owl");

		CommandLine.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestEquality18 es consistente");

		flags.remove(flagsCount);
        TestCase.assertTrue(true);
	}

	//FIN - Escenario E - Casos consistentes con metamodelling (SHIQM)

	//COMIENZO - Escenario F - Casos inconsistentes con metamodelling (SHIQM)

	public void testAccountingInconsistente1() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/AccountingInconsistente1.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("AccountingInconsistente1 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testAccountingInconsistente3() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/AccountingInconsistente3.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("AccountingInconsistente3 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles1() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles1.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles1 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles2() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles2.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles2 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles3() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles3.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles3 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles5() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles5.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles5 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles7() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles7.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles7 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCycles8() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles8.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles8 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestDifference2() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestDifference2.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestDifference2 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestDifference4() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestDifference4.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestDifference4 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestDifference8() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestDifference8.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestDifference8 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);

	}

	public void testTestDifference10() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestDifference10.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestDifference10 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestDifference11() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestDifference11.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestDifference11 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestDifference13() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestDifference13.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestDifference13 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality1() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality1.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality1 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality3() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality3.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality3 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality5() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality5.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality5 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality7() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality7.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality7 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality9() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality9.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality9 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality11() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality11.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality11 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality13() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality13.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality13 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality17() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality17.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality17 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality19() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality19.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality19 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality21() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality21.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality21 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality22() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality22.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality22 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestEquality23() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestEquality23.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestEquality23 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles10() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles10.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles10 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles12() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles12.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles12 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles13() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles13.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles13 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles14() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles14.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles14 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles15() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles15.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles15 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testCycles16() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioF/TestCycles16.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCycles16 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}



	//FIN - Escenario F - Casos inconsistentes con metamodelling (SHIQM)

	//COMIENZO - Escenario G - Casos consistentes con metamodelling (SHIQM*)

	public void testAccountingCons1CortaRule() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioG/AccountingCons1CortaRule.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("AccountingCons1CortaRule es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testTestCaseG1() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioG/TestCaseG1.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCaseG1 es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testTestCaseG2() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioG/TestCaseG2.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCaseG2 es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testTestCaseG3() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioG/TestCaseG3.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCaseG3 es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testTestCaseG4() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioG/TestCaseG4.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCaseG4 es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testTestCaseG5() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioG/TestCaseG5.owl");

		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("TestCaseG5 es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	//FIN - Escenario G - Casos consistentes con metamodelling (SHIQM*)

	//COMIENZO - Escenario H - Casos inconsistentes con metamodelling (SHIQM*)

	public void testTestCaseH1() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioH/TestCaseH1.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCaseH1 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCaseH2() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioH/TestCaseH2.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCaseH2 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCaseH3() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioH/TestCaseH3.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCaseH3 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCaseH4() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioH/TestCaseH4.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCaseH4 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testTestCaseH5() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioH/TestCaseH5.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("TestCaseH5 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	//FIN - Escenario H - Casos inconsistentes con metamodelling (SHIQM*)

	//COMIENZO - Escenario I - Casos consistentes solo con MetaRule (SHIQM*)

	public void testHidrografiaMetaRule() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioI/HidrografiaMetaRule.owl");
		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("HidrografiaMetaRule es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	//FIN - Escenario I - Casos consistentes solo con MetaRule (SHIQM*)

	//COMIENZO - Escenario J - Casos incconsistentes solo con MetaRule (SHIQM*)

	public void testOntologyEjercicio1MetaRule5() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"EscenarioJ/OntologyEjercicio1MetaRule.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("OntologyEjercicio1MetaRule es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	//FIN - Escenario J - Casos incconsistentes solo con MetaRule (SHIQM*)

	//COMIENZO - Prototipo

	public void testPrototipo1() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"Prototipo/ont1.owl");
		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("Prototipo2 es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testPrototipo2() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"Prototipo/ont2.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("Prototipo2 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testPrototipo3() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"Prototipo/ont3.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("Prototipo3 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testPrototipo4() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"Prototipo/ont4.owl");
		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("Prototipo4 es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testPrototipo5() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"Prototipo/ont5.owl");
		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("Prototipo5 es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	public void testPrototipo6() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"Prototipo/ont6.owl");
		boolean result = false;
		try {
			cl.main(flags.toArray(new String[flagsCount+1]));
		}catch (InconsistentOntologyException e) {
			System.out.println("Prototipo6 es inconsistente");
			result = true;
		}


		flags.remove(flagsCount);
		TestCase.assertEquals(true, result);
	}

	public void testPrototipo7() {
		CommandLine cl = new CommandLine();
		flags.add(testCasesPath+"Prototipo/ont7.owl");
		cl.main(flags.toArray(new String[flagsCount+1]));
		System.out.println("Prototipo7 es consistente");

		flags.remove(flagsCount);
		TestCase.assertEquals(true, true);
	}

	//FIN - Prototipo

}
