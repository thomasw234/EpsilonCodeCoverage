package org.eclipse.epsilon.eol.coverage.AST2CFG.statements;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.eol.coverage.AST2CFG.AST2CFG;
import org.eclipse.epsilon.eol.coverage.AST2CFG.IStatementConversion;
import org.eclipse.epsilon.eol.parse.EolLexer;

public class If implements IStatementConversion {

	public If() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void preRecursion(CFG lnext, AST next, CFG current,
			List<CFG> continueList, CFG start, CFG end) {
		
		if (next.getChildCount() == 3) {
			 
			 current.addChild(next.getChild(1).getCFG(), "True");
			 current.addChild(next.getChild(2).getCFG(), "False");
			 next.getChild(2).getCFG().blockMoreParents();
			 
			 // Add final to end if link
			 continueList.add(AST2CFG.findCaseLastStatement(next.getChild(1)).getCFG());
			 continueList.add(AST2CFG.findCaseLastStatement(next.getChild(2)).getCFG());				 
		 }
		
		 else if (next.getChildCount() == 2) {
			 current.addChild(next.getChild(1).getCFG(), "True");
			 continueList.add(AST2CFG.findCaseLastStatement(next.getChild(1)).getCFG());
			 continueList.add(current);		 
		 }
		
		 current.blockChild(next.getChild(0).getCFG());
		 for (AST child : next.getChild(0).getChildren()) {
			 current.blockChild(child.getCFG());
		 }
	}

	@Override
	public CFG postRecursion(CFG last, AST next, List<CFG> continueList) {
		
		CFG endIf = new CFG(If.getEndType(), AST.getNextID(), "END IF");
		
		List<CFG> toNotRemove = new ArrayList<CFG>();
		for (CFG end : continueList) {
			if (end.getType() != EolLexer.BREAK
					&& end.getType() != EolLexer.CONTINUE
					&& end.getType() != EolLexer.BREAKALL) {
				end.addChild(endIf);
				end.blockMoreChildren();
			}
				
			else
				toNotRemove.add(end);
		}
		
		continueList.clear();
		
		continueList.addAll(toNotRemove);
		continueList.add(endIf);
		
		last = endIf;
		
		return last;

	}

	@Override
	public int getType() {
		return EolLexer.IF;
	}
	
	public static int getEndType() {
		return -105;
	}

}
