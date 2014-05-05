package org.eclipse.epsilon.eol.coverage.AST2CFG;

import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.common.parse.CFG_old;

/*
 * This class holds the details of an operation
 */
public class Operation {

	private OperationHeader header;
	private CFG start, end;
	
	public Operation(OperationHeader header) {
		this.header = header;
	}
	
	public OperationHeader getHeader() {
		return this.header;
	}
	
	public void addStart(CFG start) {
		this.start = start;
		this.start.setText(header.toString());
	}
	
	public void addEnd(CFG end) {
		this.end = end;
	}
	
	public CFG getStart() {
		return this.start;
	}
	
	public CFG getEnd() {
		return this.end;
	}

}
