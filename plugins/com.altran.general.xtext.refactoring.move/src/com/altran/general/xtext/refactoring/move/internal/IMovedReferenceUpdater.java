package com.altran.general.xtext.refactoring.move.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.resource.IReferenceDescription;
import org.eclipse.xtext.ui.refactoring.IRefactoringUpdateAcceptor;

import com.altran.general.xtext.refactoring.move.MoveElementArguments;
import com.google.inject.ImplementedBy;

/**
 * @see org.eclipse.xtext.ui.refactoring.IReferenceUpdater
 *
 */
@ImplementedBy(EmfResourceMovedReferenceUpdater.class)
@SuppressWarnings("restriction")
public interface IMovedReferenceUpdater {

	void createReferenceUpdates(MoveElementArguments moveElementArguments,
			Iterable<IReferenceDescription> referenceDescriptions, IRefactoringUpdateAcceptor updateAcceptor,
			IProgressMonitor monitor);
}
