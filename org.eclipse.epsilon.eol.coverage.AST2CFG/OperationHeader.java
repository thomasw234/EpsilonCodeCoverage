package org.eclipse.epsilon.eol.coverage.AST2CFG;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.eol.parse.EolLexer;

/*
 * This class holds header information about an operation
 */
public class OperationHeader {

	private String name;
	private String returnType= null;
	private String contextType;
	private boolean contextless = true;
	private List<String> parameters;
	private AST ast;
	
	private OperationHeader() {
		parameters = new ArrayList<String>();
	}
	
	// Pass in the opertation root node (type EolLexer.HELPERMETHOD)
	public OperationHeader(AST ast) {
		parameters = new ArrayList<String>();
		this.ast = ast;
		
		// Extract the header information
		for (AST child : ast.getChildren()) {
			switch (child.getType()) {
				case EolLexer.NAME:
					this.name = child.getText();
					break;
				case EolLexer.TYPE:
					if (returnType == null)
						this.returnType = child.getText();
					else {
						// Do a swap because it must be a context operation						
						this.contextType = this.returnType;
						this.returnType = child.getText();
						this.contextless = false;
					}
					break;
				case EolLexer.PARAMLIST:
					for (AST parameter : child.getChildren()) {
						// Goes through each of the parameters
						String paramInfo = "";
						// Add the name:
						paramInfo += parameter.getChild(0).getText();
						
						if (parameter.getChildCount() > 1) {
							// Add the type
							paramInfo += ":" + parameter.getChild(1).getText();
						}
						// Add to the list of parameters
						parameters.add(paramInfo);
					}
					break;
			}
		}
	}
	
	// Pass in the operation call root node (type EolLexer.FEATURECALL)
	public static OperationHeader getFromFeatureCall(AST ast) {
		OperationHeader header = new OperationHeader();
		header.name = ast.getText();
		if (ast.getChild(0).getType() == EolLexer.PARAMETERS) {
			ast = ast.getChild(0);
			for (AST child : ast.getChildren()) {
				String paramInfo = "";
				// Add the name
				paramInfo += "" + ":"; // Name not available here, leave blank
				// Add the type
				paramInfo += parameterTypeMap(child.getType());
				// Add to the list of parameters
				header.parameters.add(paramInfo);
			}
		}
		else return null; // Hit a problem
		return header;
	}
	
	private static String parameterTypeMap(int type) {
		switch (type) {
		case EolLexer.INT:
			return "Integer";
		case EolLexer.STRING:
			return "String";
		case EolLexer.BOOLEAN:
			return "Boolean";
		case EolLexer.FLOAT:
			return "Real";
		}
		
		return "unknown";
	}
	
	// Compares two operation headers and determines if they are the same
	public boolean compareTo(OperationHeader header) {
		if (header.name != this.name) return false;
		//if (header.contextless != this.contextless) return false;
		if (header.parameters.size() != this.parameters.size()) return false;
		//if (header.contextType != this.contextType) return false;
		if (header.returnType != this.returnType) return false;
		
		
		boolean[] found = new boolean[header.parameters.size()];
		for (int i = 0; i < found.length; i++) {
			found[i] = false;
		}
		
		boolean tryingNameMatching = true;
		for (String parameter : this.parameters) {
			boolean foundParam = false;
			for (int i = 0; i < header.parameters.size() && tryingNameMatching; i++) {
				if (found[i]) continue;
				
				// Try a direct comparison first (i.e. use both name and type)
				if (parameter.equalsIgnoreCase(header.parameters.get(i))) {
					found[i] = true;
					foundParam = true;
					break;
				}
				
				if (!foundParam)
					tryingNameMatching = false;			
			}
			
			// NOTE I suspect that this may be buggy
			// Failing that, just try a type comparison
			if (!foundParam) {
				String currentType = parameter.split(":")[1];
				for (int i = 0; i < header.parameters.size(); i++) {
					String headerType = header.parameters.get(i).split(":")[1]; // Not ideal!
					if (!found[i] && headerType.equalsIgnoreCase(currentType)) {
						found[i] = true;
						foundParam = true;
						break;
					}
				}
			}
				
			if (!foundParam) return false;
		}
		
		for (boolean foundParam : found) {
			if (!foundParam) return false;
		}
		
		return true;
	}
	
	@Override
	public String toString() {
		String toReturn = this.name + "(";
		
		int newLineCount = 0;
		for (String parameter : this.parameters) {
			toReturn += parameter + ", ";
			newLineCount += parameter.length() + 2;
			
			if (newLineCount > 10) {
				toReturn += "\n";
				newLineCount = 0;
			}
		}
		
		if (toReturn.endsWith("\n"))
			toReturn = toReturn.substring(0, toReturn.length() - 3);
		else if (!toReturn.endsWith("("))
			toReturn = toReturn.substring(0, toReturn.length() - 2);
		
		toReturn += ")";
		
		return toReturn;
	}
	
	public String getName() {
		return this.name;
	}
	
	public AST getAST() {
		return this.ast;
	}

}
