package org.eclipse.epsilon.eol.coverage.AST2CFG.statements;

import java.util.List;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.eol.coverage.AST2CFG.IStatementConversion;
import org.eclipse.epsilon.eol.parse.EolLexer;

public class Break implements IStatementConversion {

	public Break() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void preRecursion(CFG last, AST next, CFG current,
			List<CFG> continueList, CFG start, CFG end) {
		AST parent = next.getParent();
		
		while (parent.getType() != EolLexer.WHILE && parent.getType() != EolLexer.FOR) {
			parent = parent.getParent();
		}
		
		int discoveredWhileEdge = parent.getCFG().getUniqueID();
		
		if (parent.getNextSibling() == null)
			parent = parent.getParent();
					
		while ((parent != null && parent.getNextSibling() == null) &&
				(parent.getType() != EolLexer.FOR && parent.getType() != EolLexer.WHILE)) {
			parent = parent.getParent();
		}
		if (parent == null) {
			current.addChild(end);
		}
		else {
			if ((parent.getType() != EolLexer.WHILE && parent.getType() != EolLexer.FOR) || (parent.getCFG().getUniqueID() == discoveredWhileEdge))
				current.addChild(parent.getNextSibling().getCFG());
			else
				current.addChild(parent.getCFG());
		}
		
		current.blockMoreChildren();
		

	}

	@Override
	public CFG postRecursion(CFG last, AST next, List<CFG> continueList) {
		return last;
	}

	@Override
	public int getType() {
		return EolLexer.BREAK;
	}

}
