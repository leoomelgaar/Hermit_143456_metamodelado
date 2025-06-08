package org.semanticweb.HermiT.tableau;

public class BranchedMetamodellingManager {
    private MetamodellingManager metamodellingManager;
    // index of the branching point
    private int branchingPoint;


    public BranchedMetamodellingManager(MetamodellingManager metamodellingManager, int branchingPoint) {
        this.metamodellingManager = metamodellingManager;
        this.branchingPoint = branchingPoint;
    }

    public MetamodellingManager getMetamodellingManager() {
        return metamodellingManager;
    }

    public void setMetamodellingManager(MetamodellingManager metamodellingManager) {
        this.metamodellingManager = metamodellingManager;
    }

    public int getBranchingPoint() {
        return branchingPoint;
    }

    public void setBranchingPoint(int branchingPoint) {
        this.branchingPoint = branchingPoint;
    }
}
