import org.semanticweb.HermiT.MetamodellingTests;

public class test_debug {
    public static void main(String[] args) {
        try {
            MetamodellingTests test = new MetamodellingTests();
            test.testTestEquality1();
            System.out.println("Test completed successfully");
        } catch (Exception e) {
            System.out.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 