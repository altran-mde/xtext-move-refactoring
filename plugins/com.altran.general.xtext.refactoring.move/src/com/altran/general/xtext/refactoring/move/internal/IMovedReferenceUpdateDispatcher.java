package com.altran.general.xtext.refactoring.move.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.ui.refactoring.IRefactoringUpdateAcceptor;

import com.altran.general.xtext.refactoring.move.MoveElementArguments;
import com.google.inject.ImplementedBy;

@SuppressWarnings("restriction")

@ImplementedBy(DefaultMovedReferenceUpdateDispatcher.class)
public interface IMovedReferenceUpdateDispatcher {

	void createReferenceUpdates(MoveElementArguments moveElementArguments, ResourceSet resourceSet,
			IRefactoringUpdateAcceptor updateAcceptor, IProgressMonitor monitor);

}
