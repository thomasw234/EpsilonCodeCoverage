package org.eclipse.epsilon.eol.coverage.AST2CFG.statements;

import java.util.List;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.eol.coverage.AST2CFG.IStatementConversion;
import org.eclipse.epsilon.eol.parse.EolLexer;

public class Case implements IStatementConversion {

	public Case() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void preRecursion(CFG last, AST next, CFG current,
			List<CFG> continueList, CFG start, CFG end) {
		continueList.add(last);
		if (last.getType() != EolLexer.SWITCH)
			last.deleteChild(current);

	}

	@Override
	public CFG postRecursion(CFG last, AST next, List<CFG> continueList) {
		return last;
	}

	@Override
	public int getType() {
		return EolLexer.CASE;
	}

}
