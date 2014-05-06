/*******************************************************************************
 * Copyright (c) 2008 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 ******************************************************************************/
package org.eclipse.epsilon.eugenia;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.nio.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epsilon.common.dt.console.EpsilonConsole;
import org.eclipse.epsilon.common.dt.util.LogUtil;
import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.CFG;
import org.eclipse.epsilon.common.parse.CFG_old;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolExecutableModule;
import org.eclipse.epsilon.eol.coverage.ASTStatementResultHolder;
import org.eclipse.epsilon.eol.coverage.BranchAnalysisResultHolder;
import org.eclipse.epsilon.eol.coverage.BranchCoverageAnalysis;
import org.eclipse.epsilon.eol.coverage.StatementCoverageToHTML;
import org.eclipse.epsilon.eol.coverage.AST2CFG.AST2CFG;
import org.eclipse.epsilon.eol.coverage.AST2CFG.Operation;
import org.eclipse.epsilon.eol.coverage.AST2CFG.OperationHolder;
//import org.eclipse.epsilon.eol.coverage.StatementCoverageToHTML;
import org.eclipse.epsilon.eol.dt.ExtensionPointToolNativeTypeDelegate;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.execute.control.BranchCoverageListener;
import org.eclipse.epsilon.eol.execute.control.StatementCoverageListener;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.parse.CFGtoDOT;
import org.eclipse.epsilon.eol.parse.EolLexer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public abstract class EugeniaActionDelegate implements IObjectActionDelegate {

	private Shell shell;
	private IResource selection;
	private List<IModel> extraModels = null;

	protected GmfFileSet gmfFileSet;
	protected boolean clearConsole = true;

	public boolean isClearConsole() {
		return clearConsole;
	}
	
	public EugeniaActionDelegate setClearConsole(boolean clearConsole) {
		this.clearConsole = clearConsole;
		return this;
	}
	
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.shell = targetPart.getSite().getShell();
	}
	
	public GmfFileSet getGmfFileSet() {
		return gmfFileSet;
	}

	public abstract String getTitle();
	
	public abstract EugeniaActionDelegateStep getStep();
	
	public boolean requiresUIThread() {
		return false;
	}
	
	public void run(final IAction action) {
		Job job = new Job(getTitle()) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					runImpl(action);
				} catch (Exception ex) {
					// Produce log message before displaying message
					// Swapping the order seems to prevent the message
					// from being logged
					LogUtil.log(ex);
					
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

						public void run() {
							MessageDialog.openError(shell, "Error",
							"An error has occured. Please see the Error Log.");
						}
						
					});
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.SHORT);
		job.schedule(); // start as soon as possible*/		
		
	}
	private StatementCoverageListener statementListener = null;
	private static BranchAnalysisResultHolder branchResultHolder = new BranchAnalysisResultHolder();
	private BranchCoverageListener branchListener = null;
	
	public abstract List<IModel> getModels() throws Exception;
	
	public IFile getSelectedFile() {
		return (selection instanceof IFile) ? (IFile)selection : null;
	}
	
	public IResource getSelection() {
		return selection;
	}
	
	public void setSelection(IResource selection) {
		this.selection = selection;
		// The following doesn't work with Jazz - see bug #407183
		//this.gmfFileSet   = new GmfFileSet(selectedFile.getLocationURI().toString());
		this.gmfFileSet = createGmfFileSetFromSelection(selection);		
	}

	protected GmfFileSet createGmfFileSetFromSelection(IResource selection) {
		return new GmfFileSet(selection.getLocation().toFile().toURI().toString());
	}

	CFG start = null;
	private CFG end = null;
	
	public IEolExecutableModule createBuiltinModule() throws EolRuntimeException {
		EolModule module = new EolModule();
				
		//module.getContext().getExecutorFactory().addExecutionListener(statementListener = new StatementCoverageListener());
		module.getContext().getExecutorFactory().addExecutionListener(branchListener = new BranchCoverageListener());
		return module;
	}
	
	public IEolExecutableModule createCustomizationModule() throws EolRuntimeException {
		return new EolModule();
	}
	
	public abstract String getBuiltinTransformation();
	
	public abstract String getCustomizationTransformation();
	
	public List<Variable> getExtraVariables() {
		return new ArrayList<Variable>();
	}
	
	public boolean isApplicable() {
		return true;
	}
	
	static int outputnum = 0;
	public void runImpl(IAction action) throws Exception {
					  
		IEolExecutableModule builtin = createBuiltinModule();
		IEolExecutableModule customization = createCustomizationModule();

		URI uri = Activator.getDefault().getBundle().getResource(getBuiltinTransformation()).toURI();
		builtin.parse(uri);
		if (!builtin.getParseProblems().isEmpty()) {
			throw new Exception("Syntax error(s) in the built-in transformation " + uri + ": " + builtin.getParseProblems());
		}
		
		for (Variable variable : getExtraVariables()) {
			builtin.getContext().getFrameStack().put(variable);
		}
		
		for (IModel model : getModels()) {
			builtin.getContext().getModelRepository().addModel(model);
		}

		
		builtin.getContext().setErrorStream(EpsilonConsole.getInstance().getErrorStream());
		builtin.getContext().setOutputStream(EpsilonConsole.getInstance().getDebugStream());
		builtin.getContext().getNativeTypeDelegates().add(new ExtensionPointToolNativeTypeDelegate());
		if (clearConsole) EpsilonConsole.getInstance().clear();
		
		try {

			AST.setNextID(0);
			OperationHolder.getInstance().clearOperations();
			
			start = new CFG(CFG.getStartType(), AST.getNextID(), "START");
			
			end = new CFG(CFG.getEndType(), AST.getNextID(), "END");
			
			System.out.println("Setting up execution listener");
			
			builtin.getContext().getExecutorFactory().addExecutionListener(branchListener = new BranchCoverageListener());
			
			
			if (builtin.getAst().getType() == EolLexer.EOLMODULE && builtin.getAst().getBasename().contains("ECore2GMF.eol"))  {
				System.out.println("EOLModule found, setting up CFG");
				new AST2CFG(start, end).work(builtin.getAst());
				//branchResultHolder.setStart(start);
			}
			
			//if (branchResultHolder.getStart() == null)
				branchResultHolder.setStart(builtin.getAst());
			System.out.println("Executing Code");
			builtin.execute();
			
			System.out.println("Generating HTML output");
						
			System.out.println("Producing graph");
			if (branchListener != null  && builtin.getAst().getBasename().contains("ECore2GMF.eol")) {
				ASTStatementResultHolder.mergeNewAST(builtin.getAst());
				for (Operation o : OperationHolder.getInstance().getOperationList()) {
					branchResultHolder.serialize(o.getHeader().getAST());
				}
				branchResultHolder.serialize(builtin.getAst());
				//output.analyseCoverage();
				branchListener.markFinished(end);
				CFGtoDOT.ASTtoDOT(builtin.getAst(), "/Users/thomaswormald/Desktop/output/AST" + outputnum + ".dot");
				branchResultHolder.getMergedCFG();
				CFGtoDOT.CFGtoDOT(start, "/Users/thomaswormald/Desktop/output/output" + (outputnum) + ".dot");
				new BranchCoverageAnalysis(start).analyse();
			}
			
			//StatementCoverageToHTML output = new StatementCoverageToHTML(ASTStatementResultHolder.getAST(),
				//	new File("/Users/thomaswormald/Desktop/output" + (outputnum++) + ".html"), new File(EugeniaActionDelegate.class.getProtectionDomain().getCodeSource().getLocation().getPath() + builtin.getSourceUri().getPath().replaceFirst("/", "")));
			
			
			if (getCustomizationTransformation() != null) {
				String customizationPath = getSelectedFile().getParent().getFile(new Path(getCustomizationTransformation())).getLocation().toOSString();
				File customizationFile = new File(customizationPath);
				if (customizationFile.exists()) {
					customization.parse(customizationFile);
					if (customization.getParseProblems().size() == 0) {
						customization.getContext().getNativeTypeDelegates().add(new ExtensionPointToolNativeTypeDelegate());
						customization.getContext().setModelRepository(builtin.getContext().getModelRepository());
						customization.getContext().setErrorStream(EpsilonConsole.getInstance().getErrorStream());
						customization.getContext().setOutputStream(EpsilonConsole.getInstance().getDebugStream());
						customization.getContext().setExtendedProperties(builtin.getContext().getExtendedProperties());
						for (Variable variable : getExtraVariables()) {
							customization.getContext().getFrameStack().put(variable);
						}
						if (getExtraModels() != null) {
							for (IModel model : getExtraModels()) {
								customization.getContext().getModelRepository().addModel(model);
							}
						}
						preExecuteCustomisation(customization);
						customization.execute();
					}
					else {
						throw new Exception("Syntax error(s) in the custom transformation " + customizationPath + ": " + customization.getParseProblems());
					}
				}
			}
		}
		catch (Exception ex) {
			System.err.println(ex.toString());
			throw ex;
		}
		finally {
			for (IModel model : builtin.getContext().getModelRepository().getModels()) {
				disposeModel(model);
			}
			builtin.getContext().dispose();
			customization.getContext().dispose();
			refresh();
		}
	}
	
	protected void disposeModel(IModel model) {
		model.dispose();
		//if (!(model.getName().equals("Ecore") || model.getName().equals("ECore"))) { model.dispose(); }
	}
	
	public void refresh() {
		try {
			getSelectedFile().getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
		}
		catch (Exception ex) {
			// Ignore
		}
	}
	
	public EmfModel loadModel(String name, String path, String nsUri, boolean readOnLoad, boolean storeOnDisposal, boolean expand) throws Exception {
		EmfModel model = new EmfModel();
		
		StringProperties properties = new StringProperties();
		
		properties.put(EmfModel.PROPERTY_MODEL_URI, org.eclipse.emf.common.util.URI.createURI(path, true));
		properties.put(EmfModel.PROPERTY_METAMODEL_URI, nsUri);
		properties.put(EmfModel.PROPERTY_IS_METAMODEL_FILE_BASED, "false");
		properties.put(EmfModel.PROPERTY_READONLOAD, readOnLoad + "");
		properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, storeOnDisposal + "");
		properties.put(EmfModel.PROPERTY_EXPAND, expand + "");
		properties.put(EmfModel.PROPERTY_NAME, name);
		
		//model.load(properties, EclipseUtil.getWorkspacePath());
		model.load(properties, null);
		return model;
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		Iterator<?> it = ((IStructuredSelection) selection).iterator();
		
		if (it.hasNext()){
			setSelection((IResource) it.next());
		}
	}

	public List<IModel> getExtraModels() {
		return extraModels;
	}

	public void setExtraModels(List<IModel> extraModels) {
		this.extraModels = extraModels;
	}
	
	protected void preExecuteCustomisation(IEolExecutableModule module) {}
	
}
