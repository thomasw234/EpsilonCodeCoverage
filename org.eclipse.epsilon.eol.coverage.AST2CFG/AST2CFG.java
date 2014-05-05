package org.eclipse.epsilon.eol.coverage.AST2CFG;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
//import org.eclipse.epsilon.common.util.AstUtil;
import org.eclipse.epsilon.eol.coverage.AST2CFG.statements.If;
//import org.eclipse.epsilon.eol.parse.ASTtoCFG;
import org.eclipse.epsilon.eol.parse.CFGtoDOT;
import org.eclipse.epsilon.eol.parse.EolLexer;
import org.eclipse.epsilon.eol.parse.EolParserWorkbench;

/*
 * This is the main conversion class that performs an AST to CFG transformation
 */
public class AST2CFG {

	private AST ast;
	private CFG start;
	private CFG cfg;
	private CFG end;
	private OperationHolder opHolder;
	private Hashtable<Integer, IStatementConversion> registeredStatements = AST2CFG.getRegisteredStatements();

	public AST2CFG(CFG start, CFG end) {
		this.start = start;
		this.end = end;
	}
	
	public AST2CFG(CFG start, CFG end, int newUniqueID) {
		this.start = start;
		this.end = end;
	}
	
	public void work() {
		// Get the AST
		EolParserWorkbench epw = new EolParserWorkbench();
		try {
			epw.work();
			ast = epw.getAST();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Get the root node
		while (ast.getParent() != null && ast.getType() != EolLexer.EOLMODULE)
			ast = ast.getParent();
		
		// Find all the operations
		this.findAllOperations(ast);
		
		// Get the root node again, because findAllOperations will leave the ast
		// at a leaf node
		while (ast.getType() != EolLexer.EOLMODULE)
			ast = ast.getParent();
		
		cfg = new CFG(CFG.getStartType(), AST.getNextID(), "START");
		this.start = cfg;
		
		end = new CFG(CFG.getEndType(), AST.getNextID(), "END");
		end.blockMoreChildren();
		
		this.depthFirstAST(ast, this.start);
		this.findChildlessNodes(ast);
		
		CFGtoDOT.CFGtoDOT(this.start, "/Users/thomaswormald/Desktop/test.dot");
	}
	
	// Used when the AST already exists, such as when traversing an operation
	public void work(AST ast) {
		this.findAllOperations(ast);
		
		while (ast.getParent() != null && ast.getType() != EolLexer.EOLMODULE)
			ast = ast.getParent();
		
		this.depthFirstAST(ast, this.start);

		this.findChildlessNodes(ast);
	}
	
	private CFG last;
	
	// Recursive function to go through the tree in a depth
	// first manner
	// Returns the continue List at this level of recursion
	private List<CFG> depthFirstAST(AST ast, CFG previous) {
		
		CFG current = ast.getCFG();
		
		if (ast.getParent() != null &&
				ast.getParent().getChild(0).getID() != ast.getID()) {
			previous = last;
		}
		
		if (AST2CFG.validType(current))
		{
			previous.addChild(current);
		}
		else
		{
			current = previous;
		}
		
		List<CFG> continueList = new ArrayList<CFG>();
		
		boolean addToContinueList = false;
		try {
			this.handleAST(ast, current, continueList);
		} catch (NullPointerException ex) {
			AST parent = ast;
			
			//Bug Fix: while and for loops jump too far back if this line isn't executed:
			if (parent.getType() == EolLexer.WHILE || parent.getType() == EolLexer.FOR)
				parent = parent.getParent();
			
			// Can't always jump to an end point if we're in a loop
			boolean canJumpToEnd = true;
			// Search for the next node to connect to
			// Bug Fix: while and for loops jump too far back if the while and for condition isn't included
			while (parent != null && parent.getNextSibling() == null &&
					(parent.getType() != EolLexer.WHILE && parent.getType() != EolLexer.FOR)) {
				parent = parent.getParent();
				if (parent != null &&
					(parent.getType() == EolLexer.FOR ||
					parent.getType() == EolLexer.WHILE))
					canJumpToEnd = false;
			}
			
			if (parent != null && parent.getType() != EolLexer.WHILE && parent.getType() != EolLexer.FOR)
				current.addChild(parent.getNextSibling().getCFG());
			// If nowhere else to go within a loop, go back to the loop condition
			else if (parent != null && (parent.getType() == EolLexer.WHILE || parent.getType() == EolLexer.FOR))
				current.addChild(parent.getCFG());
			else if (canJumpToEnd)
				// Note: added default exception because otherwise default always executes on a switch statement
				current.addChild(end);
			else
				addToContinueList = true;
		}
		
		if (ast.getChildCount() == 0) {
			last = current;
		}
		
		for (AST child : ast.getChildren()) {
			// If this is an operation that has been discovered and we're not
			// at the beginning of this instance of ASTtoCFG, then don't
			// go into it. Another instance will handle it
			if (ast.getType() == EolLexer.HELPERMETHOD && this.start != current)
				continue;
			
			continueList.addAll(this.depthFirstAST(child, current));	
		}
		
		this.handlePostRecursion(ast, continueList);
		
		// Handles the case with while loop within another while loop
		// (or similar) where adding to the continue list before the
		// recursion causes a loop back to the inner while loop
		if (addToContinueList)
			continueList.add(current);
				
		return continueList;
		
	}
	
	// This function is called after a depth first traversal
	// If we've just traversed the contents of a for or while
	// loop, then a control flow edge of the CFG held in 'last'
	// is linked to the CFG of the ast parameter
	private void handlePostRecursion(AST ast, List<CFG> continueList) {
		if (registeredStatements.containsKey(ast.getType())) {
			last = registeredStatements.get(ast.getType()).postRecursion(last, ast, continueList);
		}
	}

	public static void removeENDIF(CFG current, CFG last) {
		
		// Check if came from an END IF
		List<CFG> parents = current.getAllParents();
		
		for (int x = 0; x < parents.size(); x++){

			if (parents.get(x).getType() == -105) { // END IF

				List<CFG> parentsparents = parents.get(x).getAllParents();
				
				while (parentsparents.size() > 0) {
					
					parentsparents.get(0).allowMoreChildren();
					parentsparents.get(0).addChild(current);
					parentsparents.get(0).deleteChild(parents.get(x));
				}

				parents.get(x).deleteAllChildren();
				last = current;
			}
		}
	}
	
	private void handleAST(AST ast, CFG current, List<CFG> continueList) {
		
		AST2CFG.removeENDIF(current, last);
		
		if (registeredStatements.containsKey(ast.getType())) {
			registeredStatements.get(ast.getType()).preRecursion(last, ast, current, continueList, start, end);
		}
		
	}
	
	public static AST findCaseLastStatement(AST ast) {
		AST found = AST2CFG.validType(ast.getCFG()) ? ast : null;
		
		if (found != null && 
				(found.getType() == EolLexer.WHILE || found.getType() == EolLexer.FOR))
			return found;
		
		for (AST child : ast.getChildren()) {
			AST temp = AST2CFG.findCaseLastStatement(child);
			if (temp != null) found = temp;
		}
		return found;
	}
	
	private static int[] valid = new int[] { 
			EolLexer.ASSIGNMENT,
			EolLexer.FOR,
			EolLexer.IF,
			EolLexer.WHILE,
			EolLexer.SWITCH,
			EolLexer.CASE,
			EolLexer.DEFAULT,
			EolLexer.RETURN,
			EolLexer.BREAK,
			//EolLexer.EOLMODULE,
			EolLexer.BLOCK,
			EolLexer.CONTINUE,
			EolLexer.BREAKALL,
			EolLexer.POINT,
			//EolLexer.HELPERMETHOD,
			EolLexer.FEATURECALL,
			EolLexer.DELETE,
			-104
	};
	
	public static boolean validType(CFG cfg) {	
		for (int n : valid) {
			if (n == cfg.getType()) {
				return true;
			}
		}
		
		return false;
	}
	
	private List<Integer> visitedCFGs = new ArrayList<Integer>();
	private void findChildlessNodes(CFG cfg) {
		System.out.println("CFG " + cfg);
		// Record that this has been visited
		if (visitedCFGs.contains(cfg.getUniqueID())) 
			return;
		else 
			visitedCFGs.add(cfg.getUniqueID());

		if (cfg.getType() == -104) return;
		
		if (cfg.getChildCount() == 0 && !cfg.equals(end)) {
			cfg.addChild(end);
		}
		
		cfg.allowMoreChildren();
		
		for (int i =0; i < cfg.getChildCount(); i++) {
			CFG child = cfg.getChild(i);
			if (child.getType() == If.getEndType()) { // END IF
				if (child.getChildCount() == 0 && child.getType() != -104) { // Check not end node
					// Was child.addChild(end), pretty sure that's a bug as we're trying to orphan the end if node
					cfg.addChild(end);
					child.removeParent(cfg);
					cfg.deleteChild(child);
					i--;
					break;
				}
				else {
					for (CFG childschild : child.getAllChildren()) {
						cfg.addChild(childschild);
					}
					cfg.deleteChild(child);
				}
			}
		}
		
		for (CFG child : cfg.getAllChildren()) {
			this.findChildlessNodes(child);
		}
	}
	
	private static int outputNum = 0;
	// Go through the CFG and remove any end if nodes that exist
	private void removeAllEndIfStatements(CFG cfg) {
		cfg.allowMoreChildren();
		for (int i = 0; i < cfg.getChildCount(); i++) {
			if (cfg.getChild(i).getType() == -105) {
				CFG child = cfg.getChild(i);
				List<CFG> childsChildren =child.getAllChildren();
				if (childsChildren.size() > 0) {
					for (CFG childchild : childsChildren) {
						cfg.addChild(childchild);
					}
					cfg.deleteChild(child);
				}
				else {
					cfg.addChild(end);
					cfg.deleteChild(child);
				}
				i--;
			}
		}
		
		
		
		for (CFG child : cfg.getAllChildren()) {
			this.removeAllEndIfStatements(child);
		}
	}
	
	
	private void findChildlessNodes(AST ast) {
		System.out.println("AST" + ast.getText());
		//visitedCFGs = new ArrayList<Integer>();
		this.findChildlessNodes(ast.getCFG());
		for (AST child : ast.getChildren()) {
			this.findChildlessNodes(child);
		}
	}
	
	public static void main(String[] args) {
		CFG start = new CFG(-103, AST.getNextID(), "START");
		
		CFG end = new CFG(-104, AST.getNextID(), "END");
		
		new AST2CFG(start, end).work();
	}
	
	private void findAllOperations(AST ast) {
		if (ast.getType() == EolLexer.HELPERMETHOD) {
			if (opHolder == null)
				opHolder = OperationHolder.getInstance();
			
			opHolder.addOperation(ast);
		}
		
		for (AST child : ast.getChildren()) {
			findAllOperations(child);
		}
	}
	
	public void printCFG() {
		List<Integer> visitedCFGs = new ArrayList<Integer>();
		Queue<CFG> queue = new LinkedList<CFG>();
		
		queue.add(this.start);
		
		while (queue.size() > 0) {
			CFG current = queue.remove();
			for (CFG child : current.getAllChildren()) {
				System.out.println(current.getUniqueID() + "(" + current.getType() + ") goes to " + child.getUniqueID() + " (" + child.getText() + ")");
				if (!visitedCFGs.contains(child.getUniqueID())) {
					
					visitedCFGs.add(child.getUniqueID());
					queue.add(child);
				}
			}
		}
	}
	
	private static Hashtable<Integer, IStatementConversion> getRegisteredStatements() {
		Hashtable<Integer, IStatementConversion> registered = new Hashtable<Integer, IStatementConversion>();
		
		// Would be nice if we could automatically add all classes that implement IStatementConversion, but it seems
		// that this is more effort than it is worth. See:		
		// http://stackoverflow.com/questions/176527/how-can-i-enumerate-all-classes-in-a-package-and-add-them-to-a-list
		
		// TODO If Epsilon ever moves to Java 8, change IStatementConversion to contain static methods so
		// that instances don't need to be created
		
		List<IStatementConversion> instances = new ArrayList<IStatementConversion>();
		
		instances.add(new org.eclipse.epsilon.eol.coverage.AST2CFG.statements.Block());
		instances.add(new org.eclipse.epsilon.eol.coverage.AST2CFG.statements.Break());
		instances.add(new org.eclipse.epsilon.eol.coverage.AST2CFG.statements.BreakAll());
		instances.add(new org.eclipse.epsilon.eol.coverage.AST2CFG.statements.Case());
		instances.add(new org.eclipse.epsilon.eol.coverage.AST2CFG.statements.Continue());
		instances.add(new org.eclipse.epsilon.eol.coverage.AST2CFG.statements.Default());
		instances.add(new org.eclipse.epsilon.eol.coverage.AST2CFG.statements.For());
		instances.add(new org.eclipse.epsilon.eol.coverage.AST2CFG.statements.If());
		instances.add(new org.eclipse.epsilon.eol.coverage.AST2CFG.statements.Return());
		instances.add(new org.eclipse.epsilon.eol.coverage.AST2CFG.statements.Switch());
		instances.add(new org.eclipse.epsilon.eol.coverage.AST2CFG.statements.While());
		
		for (IStatementConversion instance : instances) {
			registered.put(instance.getType(), instance);
		}
		
		return registered;
	}

}

