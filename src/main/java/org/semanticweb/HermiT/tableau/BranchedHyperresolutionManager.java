package org.semanticweb.HermiT.tableau;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.HermiT.model.DLClause;

public class BranchedHyperresolutionManager {

	private HyperresolutionManager hyperresolutionManager;

	// agregado por bruno, dado que para hacer backracking de metamodelado
	// se precisa tener puntero a un hyperresolutionManager
	private int branchingPoint;
	private int branchingIndex;
	private List<DLClause> dlClausesAdded;

	public BranchedHyperresolutionManager() {
		dlClausesAdded = new ArrayList<DLClause>();
	}

	public List<DLClause> getDlClausesAdded() {
		return dlClausesAdded;
	}
	public void setDlClausesAdded(List<DLClause> dlClausesAdded) {
		this.dlClausesAdded = dlClausesAdded;
	}
	public int getBranchingIndex() {
		return branchingIndex;
	}
	public void setBranchingIndex(int branchingIndex) {
		this.branchingIndex = branchingIndex;
	}

	public HyperresolutionManager getHyperresolutionManager() {
		return hyperresolutionManager;
	}
	public void setHyperresolutionManager(HyperresolutionManager hyperresolutionManager) {
		this.hyperresolutionManager = hyperresolutionManager;
	}
	public int getBranchingPoint() {
		return branchingPoint;
	}
	public void setBranchingPoint(int branchingPoint) {
		this.branchingPoint = branchingPoint;
	}
}
