package org.eclipse.epsilon.eol.coverage.AST2CFG.statements;

import java.util.List;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.eol.coverage.AST2CFG.IStatementConversion;
import org.eclipse.epsilon.eol.parse.EolLexer;

public class BreakAll implements IStatementConversion {

	public BreakAll() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void preRecursion(CFG last, AST next, CFG current,
			List<CFG> continueList, CFG start, CFG end) {
		AST temp = next;
		AST lastLoopSeen = null;
		while ((temp = temp.getParent()) != null) {
			if (temp.getType() == EolLexer.FOR ||
					temp.getType() == EolLexer.WHILE) {
				lastLoopSeen = temp;
			}
		}
		
		if (lastLoopSeen != null) {
			if (lastLoopSeen.getNextSibling() != null)
				current.addChild(lastLoopSeen.getNextSibling().getCFG());
			else {
				while (lastLoopSeen != null &&
						lastLoopSeen.getNextSibling() == null) {
					lastLoopSeen = lastLoopSeen.getParent();
				}
				
				if (lastLoopSeen != null && lastLoopSeen.getNextSibling() != null) {
					current.addChild(lastLoopSeen.getNextSibling().getCFG());
				}
				else
					current.addChild(end);
			}
		}
		current.blockMoreChildren();


	}

	@Override
	public CFG postRecursion(CFG last, AST next, List<CFG> continueList) {
		return last;

	}

	@Override
	public int getType() {
		return EolLexer.BREAKALL;
	}

}
