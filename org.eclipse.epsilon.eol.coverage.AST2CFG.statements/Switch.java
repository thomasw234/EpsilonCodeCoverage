package org.eclipse.epsilon.eol.coverage.AST2CFG.statements;

import java.util.List;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.common.util.AstUtil;
import org.eclipse.epsilon.eol.coverage.AST2CFG.AST2CFG;
import org.eclipse.epsilon.eol.coverage.AST2CFG.IStatementConversion;
import org.eclipse.epsilon.eol.parse.EolLexer;

public class Switch implements IStatementConversion {

	public Switch() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void preRecursion(CFG last, AST next, CFG current,
			List<CFG> continueList, CFG start, CFG end) {

		for (AST child : next.getChildren()) {
			if (child.getType() == EolLexer.CASE || child.getType() == EolLexer.DEFAULT) {
				current.addChild(child.getCFG());
				continueList.add(AST2CFG.findCaseLastStatement(child).getCFG());
			}
			
		}

		// Now do a search for the nasty continue statement within the switch block
		AST continueLoc = Switch.switchSearchForContinue(next);
		AST nextCaseLoc = null;
		if (continueLoc != null) {
			do {
				if (nextCaseLoc != null) {
					continueLoc = AST2CFG.findCaseLastStatement(nextCaseLoc);
				}
				if (continueLoc != null) {
					// Found a continue statement. Now do linking
					nextCaseLoc = continueLoc;
					while (nextCaseLoc.getType() != EolLexer.CASE && nextCaseLoc.getType() != EolLexer.DEFAULT)
						nextCaseLoc = nextCaseLoc.getParent();
					nextCaseLoc = nextCaseLoc.getNextSibling();

					if (nextCaseLoc != null) {

						// Add link from the continue statement to the next block
						continueLoc.getCFG().addChild(nextCaseLoc.getChild(nextCaseLoc.getType() == EolLexer.CASE ? 1 : 0).getCFG());

						if (continueLoc.getType() == EolLexer.CONTINUE) {
							// No more children from the continue block!
							continueLoc.getCFG().blockMoreChildren();
						}
					}
				}

			} while (nextCaseLoc != null);
		}

	}

	@Override
	public CFG postRecursion(CFG last, AST next, List<CFG> continueList) {

		CFG variableToDelete = next.getChild(0).getCFG();
		next.getCFG().deleteChild(variableToDelete);
		
		CFG endIf = new CFG(Switch.getEndType(), AST.getNextID(), "END SWITCH");
		
		if (AstUtil.getChild(next, EolLexer.DEFAULT) == null)
			next.getCFG().addChild(endIf);
		else
			next.getCFG().blockChild(endIf);
		
		for (CFG end : continueList) {
			end.addChild(endIf);
		}
		
		last = endIf;
				
		// and clear the continueList
		
		continueList.clear();
		
		return last;
	}

	@Override
	public int getType() {
		return EolLexer.SWITCH;
	}
	
	public static int getEndType() {
		return -105;
	}
	
	private static AST switchSearchForContinue(AST ast) {
		if (ast.getType() == EolLexer.WHILE || ast.getType() == EolLexer.FOR)
			return null;
		else if (ast.getType() == EolLexer.CONTINUE) {
			return ast;
		}
		else {
			for (AST child : ast.getChildren()) {
				AST temp = Switch.switchSearchForContinue(child);
				if (temp != null) return temp;
			}
			return null;
		}
	}

}
