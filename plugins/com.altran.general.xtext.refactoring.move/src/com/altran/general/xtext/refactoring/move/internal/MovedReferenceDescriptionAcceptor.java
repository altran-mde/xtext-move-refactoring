package com.altran.general.xtext.refactoring.move.internal;

import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.xtext.findReferences.IReferenceFinder;
import org.eclipse.xtext.resource.IReferenceDescription;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.IResourceServiceProvider.Registry;
import org.eclipse.xtext.resource.impl.DefaultReferenceDescription;
import org.eclipse.xtext.ui.refactoring.impl.StatusWrapper;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

@SuppressWarnings("restriction")
final class MovedReferenceDescriptionAcceptor implements IReferenceFinder.Acceptor {
	private final Map<URI, IMovedReferenceUpdater> uri2Updater = Maps.newLinkedHashMap();
	private final Multimap<IMovedReferenceUpdater, IReferenceDescription> updater2descriptions = LinkedHashMultimap
			.create();

	private final IResourceServiceProvider.Registry resourceServiceProviderRegistry;
	private final StatusWrapper status;

	public MovedReferenceDescriptionAcceptor(final Registry resourceServiceProviderRegistry,
			final StatusWrapper status) {
		this.resourceServiceProviderRegistry = resourceServiceProviderRegistry;
		this.status = status;
	}

	@Override
	public void accept(final EObject source, final URI sourceUri, final EReference eReference, final int index,
			final EObject targetOrProxy, final URI targetUri) {
		accept(new DefaultReferenceDescription(sourceUri, targetUri, eReference, index, null));
	}

	@Override
	public void accept(final IReferenceDescription description) {
		if (description.getSourceEObjectUri() == null || description.getTargetEObjectUri() == null
				|| description.getEReference() == null) {
			status.add(RefactoringStatus.ERROR, "ReferenceDescription is incomplete", description);
		} else {
			final URI sourceResourceUri = description.getSourceEObjectUri().trimFragment();

			final IMovedReferenceUpdater referenceUpdater = uri2Updater.computeIfAbsent(sourceResourceUri, uri -> {
				final IResourceServiceProvider resourceServiceProvider = resourceServiceProviderRegistry
						.getResourceServiceProvider(uri);

				return resourceServiceProvider.get(IMovedReferenceUpdater.class);
			});

			if (referenceUpdater == null) {
				status.add(RefactoringStatus.ERROR, "Cannot get IMovedReferenceUpdater for " + sourceResourceUri);
			} else {
				getUpdater2descriptions().put(referenceUpdater, description);
			}
		}
	}

	public Multimap<IMovedReferenceUpdater, IReferenceDescription> getUpdater2descriptions() {
		return updater2descriptions;
	}
}