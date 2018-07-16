package com.altran.general.xtext.refactoring.move.internal;

import static com.altran.general.xtext.refactoring.move.internal.Utils.checkForCancellation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.findReferences.IReferenceFinder;
import org.eclipse.xtext.resource.IReferenceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.ui.editor.findrefs.ResourceAccess;
import org.eclipse.xtext.ui.editor.findrefs.TargetURIConverter;
import org.eclipse.xtext.ui.refactoring.IRefactoringUpdateAcceptor;

import com.altran.general.xtext.refactoring.move.MoveElementArguments;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @see org.eclipse.xtext.ui.refactoring.impl.ReferenceUpdaterDispatcher
 *
 */
@SuppressWarnings("restriction")
public class DefaultMovedReferenceUpdateDispatcher implements IMovedReferenceUpdateDispatcher {

	private static final int PROGRESS_FIND_ALL_REFERENCES = 50;
	private static final int PROGRESS_CREATE_REFERENCE_UPDATES = 50;
	private static final int PROGRESS_TOTAL = PROGRESS_FIND_ALL_REFERENCES + PROGRESS_CREATE_REFERENCE_UPDATES;

	@Inject
	private Provider<ResourceAccess> resourceAccessProvider;

	@Inject
	private IReferenceFinder referenceFinder;

	@Inject
	private TargetURIConverter targetURIConverter;

	@Inject
	private IResourceDescriptions indexData;

	@Inject
	private IResourceServiceProvider.Registry resourceServiceProviderRegistry;

	@Override
	public void createReferenceUpdates(final MoveElementArguments moveElementArguments, final ResourceSet resourceSet,
			final IRefactoringUpdateAcceptor updateAcceptor, final IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, PROGRESS_TOTAL);

		final ResourceAccess resourceAccess = resourceAccessProvider.get();
		resourceAccess.registerResourceSet(resourceSet);

		checkForCancellation(subMonitor);
		final MovedReferenceDescriptionAcceptor movedReferenceDescriptionAcceptor = createMovedReferenceDescriptionAcceptor(
				updateAcceptor);

		checkForCancellation(subMonitor);
		referenceFinder.findAllReferences(
				targetURIConverter.fromIterable(moveElementArguments.getOriginal2movedUris().keySet()), resourceAccess,
				indexData, movedReferenceDescriptionAcceptor, subMonitor.newChild(PROGRESS_FIND_ALL_REFERENCES));

		final Multimap<IMovedReferenceUpdater, IReferenceDescription> updater2descriptions = movedReferenceDescriptionAcceptor
				.getUpdater2descriptions();

		final SubMonitor loopMonitor = subMonitor.newChild(PROGRESS_CREATE_REFERENCE_UPDATES)
				.setWorkRemaining(updater2descriptions.keySet().size());
		for (final IMovedReferenceUpdater referenceUpdater : updater2descriptions.keySet()) {
			checkForCancellation(loopMonitor);
			referenceUpdater.createReferenceUpdates(moveElementArguments, updater2descriptions.get(referenceUpdater),
					updateAcceptor, loopMonitor.newChild(1));
		}
	}

	private MovedReferenceDescriptionAcceptor createMovedReferenceDescriptionAcceptor(
			final IRefactoringUpdateAcceptor updateAcceptor) {
		return new MovedReferenceDescriptionAcceptor(resourceServiceProviderRegistry,
				updateAcceptor.getRefactoringStatus());
	}

}
