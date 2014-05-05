package org.eclipse.epsilon.eol.coverage.AST2CFG;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.common.parse.CFG_old;
import org.eclipse.epsilon.eol.parse.EolLexer;

// Note: Singleton used because one-time *lazy* initialization
// is appropriate here - not all programs will have operations
// and sometimes an operation holder can exist in the ASTtoCFG
// that is used for defining an operation.

/*
 * This class holds ALL operations in a program
 */
public class OperationHolder {

	private static OperationHolder instance;
	private List<Operation> operations;
	
	public static OperationHolder getInstance() {
		if (instance == null) 
			instance = new OperationHolder();
		
		return instance;
	}
	
	// A class that holds the CFG of various operations
	private OperationHolder() {
		operations = new ArrayList<Operation>();
	}
	
	public void addOperation(AST ast) {
		if (ast.getType() != EolLexer.HELPERMETHOD) {
			System.out.println("Invalid AST passed in");
			return;
		}
		
		// Generation the operation header
		OperationHeader header = new OperationHeader(ast);
		
		// Check if the operation has already been added
		for (Operation operation : operations) {
			if (operation.getHeader().compareTo(header)) {
				// Already exists, so just return
				return;
			}
		}
		
		// Must not exist, so we must traverse and add it to the 
		// list of existing operations
		CFG start = new CFG(-103, AST.getNextID(), "START");
		
		CFG end = new CFG(-104, AST.getNextID(), "END");
		
		// Find the block child
		for (AST child : ast.getChildren()) {
			if (child.getType() == EolLexer.BLOCK) {
				ast = child;
				break;
			}
		}

		Operation newOp = new Operation(header);
		newOp.addStart(start);
		newOp.addEnd(end);
		
		// Add to the list of operations
		operations.add(newOp);
		
		ast.setParent(null);
		
		new AST2CFG(start, end).work(ast);
				
	}
	
	public List<Operation> getOperationList() {
		return operations;
	}
	
	public void clearOperations() {
		operations = new ArrayList<Operation>();		
	}

}
