package org.eclipse.epsilon.eol.coverage.AST2CFG.statements;

import org.eclipse.epsilon.eol.coverage.AST2CFG.IStatementConversion;
import org.eclipse.epsilon.eol.parse.EolLexer;

public class Default extends Case implements IStatementConversion {

	public Default() {
	}
	
	@Override
	public int getType() {
		return EolLexer.DEFAULT;
	}

}
