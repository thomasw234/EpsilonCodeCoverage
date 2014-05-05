package org.eclipse.epsilon.eol.coverage.AST2CFG;

import java.util.List;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;

// Would be nicer if this contained static methods, but apparently Java 7 can't do that
public interface IStatementConversion {
		
		/**
		 * The pre-recursion method. Called prior to continuing the depth-first search of the AST
		 * @param last The last CFG vertex added to the CFG
		 * @param next The AST vertex that is to be dealt with
		 * @param current The CFG vertex that is to be dealt with
		 * @param continueList A list for storing any pointers that many be of use in future recursive calls
		 * @param start TODO
		 * @param end TODO
		 */
		public void preRecursion(CFG last, AST next, CFG current, List<CFG> continueList, CFG start, CFG end);
				
		/**
		 * Called after the recursive depth first search has been performed
		 * @param last The last vertex to be added to the CFG
		 * @param next The AST vertex that is to be dealt with
		 * @param continueList A list for storing or accessing any pointers to CFG vertices
		 * @return The last pointer
		 */
		public CFG postRecursion(CFG last, AST next, List<CFG> continueList);
		
		public int getType();
}
