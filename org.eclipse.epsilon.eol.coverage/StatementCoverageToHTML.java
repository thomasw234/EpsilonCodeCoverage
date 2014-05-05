package org.eclipse.epsilon.eol.coverage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.control.StatementCoverageListener;
import org.eclipse.epsilon.eol.parse.CFGtoDOT;
import org.eclipse.epsilon.eol.parse.EolLexer;

/*
 * Outputs the result of statement coverage to HTML
 */
public class StatementCoverageToHTML {

	private AST ast;
	private File targetFile;
	private File inputFile;
	private List<String> fileContents;
	private String[] covered;
	private int statementCount = 0, coveredStatementCount = 0;

	public StatementCoverageToHTML(AST ast, File targetFile, File inputFile) {
		this.ast = ast;
		this.targetFile = targetFile;
		this.inputFile = inputFile;
		fileContents = new ArrayList<String>();
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
		StatementCoverageListener listener = new StatementCoverageListener();
		module.getContext().getExecutorFactory().addExecutionListener(listener);

		try {
			module.execute();
		} catch (EolRuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		StatementCoverageToHTML output = new StatementCoverageToHTML(ast,
				new File("/Users/thomaswormald/Desktop/output.html"), executableFile);
		output.analyseCoverage();

	}

	public void analyseCoverage() {
		this.readInLines();
		this.fillCoveredArray();
		this.dfAST(ast);
		CFGtoDOT.ASTtoDOT(ast, "/Users/thomaswormald/Desktop/covered.dot");
		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new FileWriter(targetFile));
			this.outputHTMLHeader(writer);
			this.outputTitle(writer);
			this.outputCoverageStats(writer);
			this.outputEOLFile(writer);
			this.ouputHTMLFooter(writer);
			writer.flush();
			writer.close();	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void readInLines() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));

			String line;
			while ((line = reader.readLine()) != null) {
				fileContents.add(line);
			}

			reader.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
	}

	private void fillCoveredArray() {
		covered = new String[fileContents.size()];
		for (int i = 0; i < fileContents.size(); i++) {
			covered[i] = "";
			for (int x = 0; x < fileContents.get(i).length(); x++) {
				covered[i] += "N";
			}
		}
	}

	private void dfAST(AST ast) {
		// Mark if visited and real
		if (ast.getType() != EolLexer.EOLMODULE && !ast.isImaginary()) {
			statementCount++;		
			if (ast.getVisited()) {
				coveredStatementCount++;
				char[] array = covered[ast.getLine() - 1].toCharArray();
				for (int i = ast.getRegion().getStart().getColumn(); i <= ast.getRegion().getEnd().getColumn() && i < array.length; i++) {
					array[i] = 'Y';
				}
				covered[ast.getLine() - 1] = String.valueOf(array);
			}
		}

		for (AST child : ast.getChildren()) {
			dfAST(child);
		}
	}

	private void outputHTMLHeader(BufferedWriter writer) throws IOException {
		writer.write("<HTML>\n");
		writer.write("<HEAD><title>Analysis Output</title></HEAD>\n");
		writer.write("<BODY>\n");
	}
	
	private void outputTitle(BufferedWriter writer) throws IOException {
		writer.write("<h1>Coverage Analysis</h1>");
	}
	
	private void outputCoverageStats(BufferedWriter writer) throws IOException {
		writer.write("File name: " + this.inputFile.getName()+ "<br>");
		writer.write("Total number of statements: ");
		writer.write(String.valueOf(statementCount));
		writer.write("<br>Number of executed statements: ");
		writer.write(String.valueOf(coveredStatementCount));
		writer.write("<br>Coverage percentage: ");
		float percentage = ((float)100 / (float)statementCount) * (float)coveredStatementCount;
		writer.write(Math.round(percentage) + "%<br><br>");
	}

	private void enableHighlighting(BufferedWriter writer) throws IOException {
		writer.write("<span style=\"background-color: #5EDA57\">");
	}

	private void disableHighlighting(BufferedWriter writer) throws IOException {
		writer.write("</span>");
	}

	private void printTabKey(BufferedWriter writer) throws IOException {
		writer.write("&nbsp;&nbsp;&nbsp;&nbsp;");
	}

	private void outputEOLFile(BufferedWriter writer) throws IOException {

		writer.write("<FONT FACE=\"Consolas\">");

		for (int i = 0; i < fileContents.size(); i++) {
			String line = fileContents.get(i);

			boolean highlightingEnabled = false;
			for (int j = 0; j < line.length(); j++) {
				if (covered[i].charAt(j) == 'Y' && !highlightingEnabled) {
					this.enableHighlighting(writer);
					highlightingEnabled = true;
				}
				else if (covered[i].charAt(j) == 'N' && highlightingEnabled) {
					this.disableHighlighting(writer);
					highlightingEnabled = false;
				}


				if (line.charAt(j) == '\t') this.printTabKey(writer);
				else writer.write(String.valueOf(line.charAt(j)));
			}

			if (highlightingEnabled) {
				this.disableHighlighting(writer);
			}
			writer.write("<br>\n");
		}

		writer.write("</FONT>");

	}

	private void ouputHTMLFooter(BufferedWriter writer) throws IOException {
		writer.write("</BODY>");
		writer.write("</HTML>");
	}
}
