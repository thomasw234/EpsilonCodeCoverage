package org.eclipse.epsilon.eol.coverage.AST2CFG.statements;

import java.util.List;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.eol.coverage.AST2CFG.AST2CFG;
import org.eclipse.epsilon.eol.coverage.AST2CFG.IStatementConversion;
import org.eclipse.epsilon.eol.parse.EolLexer;

public class For implements IStatementConversion {

	@Override
	public void preRecursion(CFG last, AST next, CFG current,
			List<CFG> continueList, CFG start, CFG end) {

			// Add a link from the for loop to the next statement
			current.addChild(next.getNextSibling().getCFG());			

	}

	@Override
	public CFG postRecursion(CFG last, AST next, List<CFG> continueList) {
		// Check that an escape loop was not the last thing to be called
			if (last.getType() != EolLexer.RETURN &&
				last.getType() != EolLexer.BREAK &&
				last.getType() != EolLexer.BREAKALL) {
					
					last.addChild(next.getCFG());
					last.blockMoreChildren();
					last = next.getCFG();

					AST2CFG.removeENDIF(next.getCFG(), last);
				}
			
			// Now check the continueList to see if any more links
			// need to be added
			
			for (CFG continueEdge : continueList) {
				// No more jumps necessary for a continue statement
				if (continueEdge.getType() == EolLexer.CONTINUE) 
					continueEdge.deleteAllChildren();
				
				continueEdge.addChild(next.getCFG());
			}
			
			// and clear the continueList
			
			continueList.clear();
			
			return last;

	}

	@Override
	public int getType() {
		return EolLexer.FOR;
	}

}
