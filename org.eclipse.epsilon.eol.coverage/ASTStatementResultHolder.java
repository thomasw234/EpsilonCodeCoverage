package org.eclipse.epsilon.eol.coverage;

import org.eclipse.epsilon.common.parse.AST;

/*
 * This class holds an AST that has been executed with a statement coverage listener attached,
 * for merging with other ASTs in the same position once all execution has completed.
 */
public class ASTStatementResultHolder {

	public ASTStatementResultHolder() {
	}
	
	private static AST merged;
	
	public static void mergeNewAST(AST ast) {
		
		if (merged == null)
			merged = ast;
		
		dfMerge(ast, merged);
	}
	
	private static void dfMerge(AST newAST, AST current) {
		if (!current.getVisited() && newAST.getVisited())
			current.setVisited();
		
		for (int i = 0; i < current.getChildCount(); i++) {
			dfMerge(newAST.getChild(i), current.getChild(i));
		}
	}
	
	public static AST getAST() {
		return merged;
	}

}
