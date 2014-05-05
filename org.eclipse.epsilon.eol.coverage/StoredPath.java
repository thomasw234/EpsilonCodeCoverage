package org.eclipse.epsilon.eol.coverage;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.common.parse.CFG_old;

/*
 * This class is incomplete
 * Contains a list of edges that have been executed for use in path analysis
 */
public class StoredPath {

	private List<ExecutedEdge> edges;
	
	public StoredPath() {
		edges = new ArrayList<ExecutedEdge>();
	}
	
	public void addEdge(CFG startVertex, CFG endVertex) {
		
		// Check that the branch being marked has at least 2 children, otherwise
		// it is not of interest
		if (!this.checkMoreThanOneChild(startVertex))
			return;
		
		ExecutedEdge toAdd = new ExecutedEdge(startVertex.getUniqueID(), endVertex.getUniqueID());
		
		// Check to see if it is a repetition of the previous record
		if (this.sameAsPrevious(toAdd))
			return;
		
		// Check to see if the last record had the same start vertex,
		// and if so, remove it
		if (this.parentOfPreviousSame(toAdd))
			//this.removeLastRecord();
			return;
		
		edges.add(toAdd);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (ExecutedEdge edge : edges) {
			sb.append(edge.toString() + "\n");
		}
		
		return sb.toString();
	}
	
	private boolean sameAsPrevious(ExecutedEdge nextEdge) {
		if (edges.size() == 0)
			return false;
		
		return edges.get(edges.size() - 1).equals(nextEdge);
	}
	
	private boolean parentOfPreviousSame(ExecutedEdge nextEdge) {
		if (edges.size() == 0)
			return false;
		
		if (nextEdge.getFromVertex() == edges.get(edges.size() - 1).getFromVertex())
			return true;
		else
			return false;
	}
	
	private void removeLastRecord() {
		if (edges.size() == 0)
			return;
		
		else
			edges.remove(edges.size() - 1);
	}
	
	private boolean checkMoreThanOneChild(CFG next) {
		return next.getChildCount() > 1;
	}

}
