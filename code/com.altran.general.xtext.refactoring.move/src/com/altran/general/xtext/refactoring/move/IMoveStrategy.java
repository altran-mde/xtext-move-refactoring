package com.altran.general.xtext.refactoring.move;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.ui.refactoring.IRefactoringUpdateAcceptor;

import com.altran.general.xtext.refactoring.move.internal.EmfResourceMoveStrategy;
import com.google.inject.ImplementedBy;

/**
 * @see org.eclipse.xtext.ui.refactoring.IRenameStrategy
 *
 */
@SuppressWarnings("restriction")
@ImplementedBy(EmfResourceMoveStrategy.class)
public interface IMoveStrategy {

	void createUpdates(MoveElementArguments moveElementArguments, ResourceSet resourceSet,
			IRefactoringUpdateAcceptor updateAcceptor);

	void applyChange(MoveElementArguments moveElementArguments, ResourceSet resourceSet);

	void revertChange(MoveElementArguments moveElementArguments, ResourceSet resourceSet);

	void resolveContainedCrossReferences(URI uri, ResourceSet resourceSet);
}
