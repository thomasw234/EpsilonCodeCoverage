package org.eclipse.epsilon.eol.coverage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.common.parse.CFG_old;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.coverage.AST2CFG.AST2CFG;
import org.eclipse.epsilon.eol.coverage.AST2CFG.Operation;
import org.eclipse.epsilon.eol.coverage.AST2CFG.OperationHolder;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.control.BranchCoverageListener;
import org.eclipse.epsilon.eol.execute.control.StatementCoverageListener;
import org.eclipse.epsilon.eol.parse.CFGtoDOT;

/*
 * This class performs the branch coverage analysis once execution has completed
 */
public class BranchCoverageAnalysis {

	private CFG parent = null;
	
	public BranchCoverageAnalysis(CFG parent) {
		this.parent = parent;
	}

	public void analyse() {
		this.dfCFG(parent);
		for (Operation op : OperationHolder.getInstance().getOperationList()) {
			this.dfCFG(op.getStart());
		}
		System.out.println("Executed branches: " + this.executedBranches);
		System.out.println("Total branches :   " + this.totalBranches);
		FileWriter fw;
		try {
			fw = new FileWriter("/Users/thomaswormald/Desktop/branchoutput.txt");
			fw.write("Executed Branches: " + this.executedBranches + "\n");
			fw.write("Total Branches: " + this.totalBranches);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private int totalBranches = 0, executedBranches = 0;
	private List<CFG> visited = new ArrayList<CFG>();
	
	private void dfCFG(CFG parent) {
		if (parent.getNumberOfBranches() > 1) {
			totalBranches += parent.getNumberOfBranches();
			executedBranches += parent.getNumberOfExecutedBranches();
		}
		
		for (CFG child : parent.getAllChildren()) {
			if (!visited.contains(child)) {
				visited.add(child);
				this.dfCFG(child);
			}
		}
	}
	
	private static String defaultParseFile = "/Users/thomaswormald/Documents/workspace/all/org.eclipse.epsilon/trunk/plugins/org.eclipse.epsilon.eol.engine/src/org/eclipse/epsilon/eol/parse/test2.eol";
	public static void main(String[] args) {
		EolModule module = new EolModule();
		boolean parsed = false;
		File executableFile = new File(args.length > 0 ? args[0] : defaultParseFile);
		try {
			parsed = module.parse(executableFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!parsed || !module.getParseProblems().isEmpty()) {
			for (ParseProblem p : module.getParseProblems()) {
				System.err.println(p);
			}
			System.exit(1);
		}

		// Grab the AST
		AST ast = module.getAst();

		CFG start = new CFG(CFG.getStartType(), AST.getNextID(), "START");
		
		CFG end = new CFG(CFG.getEndType(), AST.getNextID(), "END");
		
		new AST2CFG(start, end).work(ast);
		
		BranchCoverageListener listener = new BranchCoverageListener();
		module.getContext().getExecutorFactory().addExecutionListener(listener);

		try {
			module.execute();
		} catch (EolRuntimeException e) {
			e.printStackTrace();
		}
		
		listener.markFinished(end);
		
		CFGtoDOT.CFGtoDOT(start, "/Users/thomaswormald/Desktop/test.dot");

		BranchCoverageAnalysis output = new BranchCoverageAnalysis(start);
		output.analyse();

	}

}
