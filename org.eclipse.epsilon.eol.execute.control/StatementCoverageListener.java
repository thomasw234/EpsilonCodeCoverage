package org.eclipse.epsilon.eol.execute.control;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.parse.EolLexer;

public class StatementCoverageListener implements IExecutionListener {

	@Override
	public void aboutToExecute(AST ast, IEolContext context) {
		
	}
	
	@Override
	public void finishedExecuting(AST ast, Object result, IEolContext context) {
		if (ast.getType() == EolLexer.PARAMETERS && ast.getParent() != null)
			ast.getParent().setVisited();
		else
			ast.setVisited();
	}
}
