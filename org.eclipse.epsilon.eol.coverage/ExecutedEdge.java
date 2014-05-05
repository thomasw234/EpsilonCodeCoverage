package org.eclipse.epsilon.eol.coverage;

/*
 * A container that holds information about an edge that has been executed
 */
public class ExecutedEdge implements Comparable<ExecutedEdge> {

	private int fromVertex = 0, toVertex = 0;
	
	public ExecutedEdge(int fromVertex, int toVertex) {
		this.fromVertex = fromVertex;
		this.toVertex = toVertex;
	}
	
	public int compareTo(ExecutedEdge other) {
		if (other.fromVertex == this.fromVertex &&
				other.toVertex == this.toVertex)
			return 1;
		else
			return 0;
	}
	
	public int getFromVertex() {
		return this.fromVertex;
	}
	
	public int getToVertex() {
		return this.toVertex;
	}
	
	public boolean equals(Object o) {
		if (o == null)
			return false;
		
		if (o == this)
			return true;
		
		if (!(o instanceof ExecutedEdge))
			return false;
		
		return (((ExecutedEdge)o).compareTo(this) == 1);
	}
	
	public String toString() {
		return fromVertex + " -> " + toVertex;
	}

}
