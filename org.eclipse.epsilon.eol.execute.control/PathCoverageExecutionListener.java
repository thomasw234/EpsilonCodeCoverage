package org.eclipse.epsilon.eol.execute.control;

import java.util.Stack;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.common.util.AstUtil;
import org.eclipse.epsilon.eol.coverage.StoredPath;
import org.eclipse.epsilon.eol.coverage.AST2CFG.AST2CFG;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.parse.EolLexer;

public class PathCoverageExecutionListener implements IExecutionListener {

	public PathCoverageExecutionListener() {
		// TODO Auto-generated constructor stub
	}


	@Override
	public void aboutToExecute(AST ast, IEolContext context) {

		// Catch anything executing from the header of a method that is not the executing block
		if (ast.getParent() != null &&
				ast.getParent().getType() == EolLexer.HELPERMETHOD &&
				!AstUtil.getChild(ast.getParent(), EolLexer.BLOCK).equals(ast))
			return;

		if (ast.getParent() != null && ((ast.getParent().getType() == EolLexer.CASE && ast.getParent().getChildCount() > 1 && ast.getParent().getChild(1).equals(ast)) ||
				(ast.getParent().getType() == EolLexer.DEFAULT))) {
			this.setLast(ast.getParent().getParent());
			//last = ast.getParent().getParent();
		}

		// Ignore certain types of branch information
		if (ast.getType() == EolLexer.CASE ||
				ast.getType() == EolLexer.DEFAULT ||
				ast.getType() == EolLexer.FEATURECALL)
			return;             

		// If this is a child of a case statement, ignore it if it's the first child
		// because that's the conditional statement
		if (ast.getParent() != null &&
				ast.getParent().getType() == EolLexer.CASE &&
				ast.equals(ast.getParent().getChild(0))) {
			return;
		}

		// If it's the second child of a case, or first of a default,
		// then pass the parent to be recorded

		// Perform a check to see if we've moved into a new function
		if (!sameOperation(ast, last)) {

			// Check the top of stack to see if we've returned to the last operation

			// Go through the list
			for (int i = lastStack.size() - 1; i >= 0; i--){
				AST previous = lastStack.get(i);
				if (sameOperation(ast, previous)) {
					while (last != previous) {
						this.setLast(lastStack.pop());
						//last = lastStack.pop();
					}
					return;
				}
			}

			if (lastStack.size() >= 1 && sameOperation(lastStack.peek(), ast)) {
				// Almost certainly moved back to the last operation, so pop the stack
				this.setLast(lastStack.pop());
				//last = lastStack.pop();
			}
			if (ast.getType() == EolLexer.BLOCK) {
				// Not returned, must be a new operation call, so add the current to the stack and finish
				lastStack.push(last);
				this.setLast(ast);
				//last = ast;
				return;
			}
		}

		if (last == null) {
			last = ast;
		}
		else {
			if (last.getCFG().getChildCount() > 1) {

				AST current = ast;
				while (current.getParent() != null &&
						!current.getParent().getCFG().equals(last.getCFG()) &&
						!current.getCFG().getAllParents().contains(last.getCFG())) {
					current = current.getParent();
				}

				if (current.getParent() != null &&
						current.getParent().getCFG().equals(last.getCFG())) {

					// Add an exception for when IF statement 1st child is executed, don't want to mark that!
					if ((last.getType() == EolLexer.IF) &&
							current.getCFG().equals(last.getChild(0).getCFG()))
						return;

					// Add an exception for when the non-BLOCK statement of a for loop is executing
					if ((last.getType() == EolLexer.FOR || last.getType() == EolLexer.WHILE) &&
							!current.getCFG().equals(AstUtil.getChild(last, EolLexer.BLOCK).getCFG()))
						return;
					else if ((last.getType() == EolLexer.FOR || last.getType() == EolLexer.WHILE) &&
							current.getCFG().equals(AstUtil.getChild(last, EolLexer.BLOCK).getCFG())) {
						// It is the block statement, but we need to mark the child as explored
						markBranchAsExecuted(last.getCFG().getChild(1));
					}

					// Otherwise mark the branch as executed
					markBranchAsExecuted(current.getParent().getCFG(), current.getCFG());
				}
				else if (current.getParent() != null &&
						current.getCFG().getAllParents().contains(last.getCFG())) {
					// Find out which parent that it is
					for (CFG parent : current.getCFG().getAllParents()) {
						if (parent.equals(last.getCFG())) {
							markBranchAsExecuted(parent, current.getCFG());
							break;
						}
					}
				}

			}

			if (AST2CFG.validType(ast.getCFG()) 
					&& ast.getCFG().getAllChildren().size() >= 1 
					&& ast.getCFG().getAllParents().size() >= 1)
				this.setLast(ast);
				//last = ast;

		}

	}

	private AST last;
	private Stack<AST> lastStack = new Stack<AST>();
	private StoredPath path = new StoredPath();

	@Override
	public void finishedExecuting(AST ast, Object result, IEolContext context) {

		// With the FOR and WHILE loop, execution returns to the header node before
		// continuing, so this must be recorded here
		if ((ast.getType() == EolLexer.FOR || ast.getType() == EolLexer.WHILE)
				&& ast.getCFG().getChildCount() > 1
				&& ((last.getType() != EolLexer.BREAK
				&& last.getType() != EolLexer.BREAKALL)
				|| last.getCFG().getAllChildren().contains(ast.getCFG()))) {
			// This line seems a bit shady. Might break everything
			this.markBranchAsExecuted(ast.getCFG());
			if (AST2CFG.validType(ast.getCFG())) {
				this.setLast(ast);
				//last = ast;
			}
		}
		else if (ast.getType() == EolLexer.SWITCH) {
			if (last.getParent().equals(ast)) {
				//last = ast;
				this.setLast(ast);
			}
		}
		else if (lastStack.size() >= 1 && lastStack.peek().equals(ast)) {
			//last = lastStack.pop();
			this.setLast(lastStack.pop());
		}

	}

	private static boolean sameOperation(AST first, AST second) {
		if (first == null || second == null)
			return true;

		AST firstRoot = first;
		AST secondRoot = second;
		while (firstRoot.getParent() != null) {
			firstRoot = firstRoot.getParent();
		}

		while (secondRoot.getParent() != null) {
			secondRoot = secondRoot.getParent();
		}

		if (firstRoot.getCFG().getUniqueID() == secondRoot.getCFG().getUniqueID())
			return true;

		if (dfFindVertex(firstRoot,second) && dfFindVertex(secondRoot,first))
			return true;
		else
			return false;
	}
	
	// Marks the branch as executed, but also records the path taken
	private void markBranchAsExecuted(CFG cfg) {
		last.getCFG().markBranchExecuted(cfg);		
		path.addEdge(last.getCFG(), cfg);
	}
	
	private void markBranchAsExecuted(CFG from, CFG to) {
		from.markBranchExecuted(to);
		path.addEdge(from, to);
	}
	
	private void setLast(AST last) {
		path.addEdge(this.last.getCFG(), last.getCFG());
		this.last = last;
	}

	private static boolean dfFindVertex(AST current, AST toFind) {
		if (current.getCFG().getUniqueID() == toFind.getCFG().getUniqueID())
			return true;

		boolean found = false;
		for (AST child : current.getChildren()) {
			if (dfFindVertex(child, toFind)) {
				found = true;
				break;
			}
		}
		return found;
	}

	public void markFinished(CFG end) {
		if (last != null)
			this.markBranchAsExecuted(end);
	}
	
	public StoredPath getPath() {
		return this.path;
	}

}