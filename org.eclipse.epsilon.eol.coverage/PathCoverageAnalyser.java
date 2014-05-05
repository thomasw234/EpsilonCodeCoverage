package org.eclipse.epsilon.eol.coverage;

import java.io.File;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.coverage.AST2CFG.AST2CFG;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.control.BranchCoverageListener;
import org.eclipse.epsilon.eol.execute.control.PathCoverageExecutionListener;
import org.eclipse.epsilon.eol.parse.CFGtoDOT;

/*
 * This class is unfinished
 * Analysis the result of path coverage
 */
public class PathCoverageAnalyser {

	
	
	public PathCoverageAnalyser() {
	}
	
	private static String defaultParseFile = "/Users/thomaswormald/Documents/workspace/all/org.eclipse.epsilon/trunk/plugins/org.eclipse.epsilon.eol.engine/src/org/eclipse/epsilon/eol/parse/test2.eol";
	public static void main(String[] args) {
		EolModule module = new EolModule();
		boolean parsed = false;
		File executableFile = new File(args.length > 0 ? args[0] : defaultParseFile);
		try {
			parsed = module.parse(executableFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
		
		PathCoverageExecutionListener listener = new PathCoverageExecutionListener();
		module.getContext().getExecutorFactory().addExecutionListener(listener);

		try {
			module.execute();
		} catch (EolRuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		listener.markFinished(end);
		
		CFGtoDOT.CFGtoDOT(start, "/Users/thomaswormald/Desktop/test.dot");

		//BranchCoverageAnalysis output = new BranchCoverageAnalysis(start);
		//output.analyse();
		
		System.out.println(listener.getPath().toString());

	}

}
