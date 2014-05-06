package org.eclipse.epsilon.common.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CFG {
	
	private int type;
	private List<CFG> children;
	private List<CFG> branchExecuted;
	private List<CFG> parents;
	private List<CFG> blocked;
	private List<String> edgeLabels;
	private int uniqueID = 0;
	private String text = "START";
	private boolean moreChildrenAllowed = true;
	private boolean moreParentsAllowed = true;
	private int recursionLevel = -1;

	public CFG() {
		children = new ArrayList<CFG>();
		parents = new ArrayList<CFG>();
		blocked = new ArrayList<CFG>();
		edgeLabels = new ArrayList<String>();
		branchExecuted = new ArrayList<CFG>();
		type = -1;
	}
	
	public CFG(int type, int uniqueID, String label) {
		this();
		this.type = type;
		this.uniqueID = uniqueID;
		this.text = label;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public void addChild(CFG cfg) {

		if (this.uniqueID == 798)
			System.out.println();
		else if (this.uniqueID == 1140 && cfg.uniqueID ==  1132)
			System.out.println();
		
		if (this.uniqueID == -104)
			return;
		if (this.text.equalsIgnoreCase("END"))
			return;

		if ((moreChildrenAllowed || cfg.text.equalsIgnoreCase("END")) 
				&& !children.contains(cfg) 
				&& !blocked.contains(cfg)
				&& cfg.getMoreParentsAllowed())
		{
			children.add(cfg);
			edgeLabels.add("");
			cfg.addParent(this);
		}
	}
	
	public void addChild(CFG cfg, String edgeLabel) {
		this.addChild(cfg);
		edgeLabels.set(edgeLabels.size() - 1, edgeLabel);
	}

	public void deleteAllChildren() {
		children = new ArrayList<CFG>();
		edgeLabels = new ArrayList<String>();
	}
	
	public CFG getChild(int index) {
		return children.get(index);
	}
	
	public int getChildCount() {
		return children.size();
	}
	
	public List<CFG> getAllChildren() {
		return children;
	}
	
	public int getType() {
		return type;
	}
	
	public void blockMoreChildren() {
		moreChildrenAllowed = false;
	}
	
	public void allowMoreChildren() {
		moreChildrenAllowed = true;
	}
	
	public boolean childrenBlocked() {
		return moreChildrenAllowed;
	}
	
	public boolean getMoreParentsAllowed() {
		return this.moreParentsAllowed;
	}
	
	public void blockMoreParents() {
		this.moreParentsAllowed = false;
	}
	
	public void setRecursionLevel(int level) {
		this.recursionLevel = level;
	}
	
	public int getRecursionLevel() {
		return this.recursionLevel;
	}
	
	public String getEdgeLabel(CFG cfg) {
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i).equals(cfg)) {
				if (edgeLabels.size() > i)
					return edgeLabels.get(i);
			}
		}
		
		return "";
	}
	
	public List<CFG> getAllParents() {
		return this.parents;
	}
	
	public void addParent(CFG parent) {
		if (!this.parents.contains(parent))
			this.parents.add(parent);
	}
	
	public void removeParent(CFG parent) {
		if (parents.contains(parent)) {
			parents.remove(parent);
			parent.deleteChild(this);
		}
	}
	
	public void deleteChild(CFG child) {
		if (children.contains(child)) {
			child.removeParent(this);
			children.remove(child);
		}
	}
	
	public void deleteAllParents() {
		while (parents.size() > 0) {
			parents.get(0).deleteChild(this);
		}
	}
	
	public void blockChild(CFG toBlock) {
		blocked.add(toBlock);
	}
	
	public void markBranchExecuted(CFG child) {
		if (children.contains(child)) {
			if (!branchExecuted.contains(child)) {
				branchExecuted.add(child);
			}
		}
	}
	
	public int getNumberOfBranches() {
		return children.size();
	}
	
	public int getNumberOfExecutedBranches() {
		return branchExecuted.size();
	}
	
	public boolean getBranchExecuted(CFG child) {
		return (branchExecuted.contains(child));
	}
	
	public List<CFG> getAllExecutedBranches() {
		return this.branchExecuted;
	}
	
	public void setAllExecutedBranches(List<CFG> executed) {
		this.branchExecuted = executed;
	}
	
	public void setUniqueID(int id) {
		this.uniqueID = id;
	}
	
	public int getUniqueID() {
		return this.uniqueID;
	}
	
	public static int getStartType() {
		return -103;
	}
	
	public static int getEndType() {
		return -104;
	}
	
	public String getText() {
		return this.text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public String toString() {
		return this.getText();
	}
}
