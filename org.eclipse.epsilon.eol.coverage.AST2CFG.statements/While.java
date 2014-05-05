package org.eclipse.epsilon.eol.coverage.AST2CFG.statements;

import org.eclipse.epsilon.eol.coverage.AST2CFG.IStatementConversion;
import org.eclipse.epsilon.eol.parse.EolLexer;

// While just extends the for loop implementation because they're basically the same
public class While extends For implements IStatementConversion {

	public While() {
	}
	
	@Override
	public int getType() {
		return EolLexer.WHILE;
	}

}
