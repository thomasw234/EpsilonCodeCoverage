package org.eclipse.epsilon.eol.coverage;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.common.parse.CFG_old;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/*
 * This class stores the branches that were executed during a branch coverage session
 * The class can then be merged with other instances to give the overall
 * branch coverage of a program
 */
public class BranchAnalysisResultHolder {

	private Hashtable<Integer, List<Integer>> results;
	private AST start = null;
	private List<CFG_old> visited = new ArrayList<CFG_old>();
	
	public BranchAnalysisResultHolder() {
		results = new Hashtable<Integer, List<Integer>>();
	}
	
	public void serialize(AST start) {
		if (this.start == null)
			this.start = start;
		else {
			if (start.getCFG().getAllExecutedBranches().size() > 0) {
				List<Integer> executedBranches = new ArrayList<Integer>();
				if (results.containsKey(start.getCFG().getUniqueID())) {
					executedBranches = results.get(start.getCFG().getUniqueID());
				}
				for (CFG executedBranch : start.getCFG().getAllExecutedBranches()) {
					if (!executedBranches.contains(executedBranch.getUniqueID()))
						executedBranches.add(executedBranch.getUniqueID());
				}
				results.remove(start.getCFG().getUniqueID());
				results.put(start.getCFG().getUniqueID(), executedBranches);
			}
			for (AST child : start.getChildren()) {
				this.serialize(child);
			}
		}
	}
	
	public List<Integer> getExecuted(CFG cfg) {
		if (results.containsKey(cfg.getUniqueID())) {
			return results.get(cfg.getUniqueID());
		}
		else {
			return new ArrayList<Integer>();
		}
	}
	
	
	public AST getMergedCFG() {
		visited = new ArrayList<CFG_old>();
		dfMergeCFG(start);
		return start;
	}
	
	private void dfMergeCFG(AST cfg) {
		if (results.containsKey(cfg.getCFG().getUniqueID())) {
			List<Integer> executedChildren = results.get(cfg.getCFG().getUniqueID());
			for (CFG child : cfg.getCFG().getAllChildren()) {
				if (executedChildren.contains(child.getUniqueID()))
					cfg.getCFG().markBranchExecuted(child);
			}
		}
		
		for (AST child : cfg.getChildren())
			dfMergeCFG(child);
	}
	
	public void setStart(AST start) {
		this.start = start;
	}
	
	public AST getStart() {
		return this.start;
	}

}
