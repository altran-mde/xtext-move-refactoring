package com.altran.general.xtext.refactoring.move.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;

import com.altran.general.xtext.refactoring.move.MoveElementArguments;
import com.google.inject.ImplementedBy;

/**
 * @see org.eclipse.xtext.ui.refactoring.IRenamedElementTracker
 *
 */
@ImplementedBy(DefaultMovedElementTracker.class)
@SuppressWarnings("restriction")
public interface IMovedElementTracker {

	void moveAndTrack(Iterable<URI> dependentElementUris, MoveElementArguments moveElementArguments,
			ResourceSet resourceSet, IProgressMonitor monitor);

}
