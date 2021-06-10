package com.altran.general.xtext.refactoring.move.internal;

import static com.altran.general.xtext.refactoring.move.internal.Utils.checkForCancellation;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.eclipse.ltk.core.refactoring.RefactoringStatus.ERROR;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.IReferenceDescription;
import org.eclipse.xtext.ui.refactoring.IRefactoringUpdateAcceptor;
import org.eclipse.xtext.ui.refactoring.impl.RefactoringResourceSetProvider;
import org.eclipse.xtext.ui.refactoring.impl.ReferenceDescriptionSorter;
import org.eclipse.xtext.ui.refactoring.impl.StatusWrapper;

import com.altran.general.xtext.refactoring.move.MoveElementArguments;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

/**
 * @see org.eclipse.xtext.ui.refactoring.impl.EmfResourceReferenceUpdater
 *
 */
@SuppressWarnings("restriction")
public class EmfResourceMovedReferenceUpdater implements IMovedReferenceUpdater {

	private static final int PROGRESS_CREATE_REFERENCE_UPDATES__SORT_BY_PROJECT = 1;
	private static final int PROGRESS_CREATE_REFERENCE_UPDATES__CREATE_CLUSTERED_REFERENCE_UPDATES = 99;
	private static final int PROGRESS_CREATE_REFERENCE_UPDATES__TOTAL = PROGRESS_CREATE_REFERENCE_UPDATES__SORT_BY_PROJECT
			+ PROGRESS_CREATE_REFERENCE_UPDATES__CREATE_CLUSTERED_REFERENCE_UPDATES;

	private static final int PROGRESS_CREATE_CLUSTERED_REFERENCE_UPDATES__LOAD_TARGET_RESOURCES = 1;
	private static final int PROGRESS_CREATE_CLUSTERED_REFERENCE_UPDATES__CREATE_REFERENCE_UPDATES_FOR_CLUSTER = 99;
	private static final int PROGRESS_CREATE_CLUSTERED_REFERENCE_UPDATES__TOTAL = PROGRESS_CREATE_CLUSTERED_REFERENCE_UPDATES__LOAD_TARGET_RESOURCES
			+ PROGRESS_CREATE_CLUSTERED_REFERENCE_UPDATES__CREATE_REFERENCE_UPDATES_FOR_CLUSTER;
	// deliberately excluded from TOTAL
	private static final int PROGRESS_CREATE_CLUSTERED_REFERENCE_UPDATES__CREATE_REFERENCE_UPDATES_FOR_CLUSTER__EXTRA = 1;

	private static final int PROGRESS_CREATE_REFERENCE_UPDATES_FOR_CLUSTER__CREATE_REFERENCE_UPDATES = 20;
	private static final int PROGRESS_CREATE_REFERENCE_UPDATES_FOR_CLUSTER__RESOLVE_REFERENCE_PROXIES = 70;
	private static final int PROGRESS_CREATE_REFERENCE_UPDATES_FOR_CLUSTER__LOAD_REFERRING_RESOURCES = 10;
	private static final int PROGRESS_CREATE_REFERENCE_UPDATES_FOR_CLUSTER__TOTAL = PROGRESS_CREATE_REFERENCE_UPDATES_FOR_CLUSTER__CREATE_REFERENCE_UPDATES
			+ PROGRESS_CREATE_REFERENCE_UPDATES_FOR_CLUSTER__RESOLVE_REFERENCE_PROXIES
			+ PROGRESS_CREATE_REFERENCE_UPDATES_FOR_CLUSTER__LOAD_REFERRING_RESOURCES;

	@Inject
	private ReferenceDescriptionSorter sorter;

	@Inject
	private EmfResourceChangeUtil changeUtil;

	@Inject
	private RefactoringResourceSetProvider resourceSetProvider;

	@Override
	public void createReferenceUpdates(final MoveElementArguments moveElementArguments,
			final Iterable<IReferenceDescription> referenceDescriptions,
			final IRefactoringUpdateAcceptor updateAcceptor, final IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, PROGRESS_CREATE_REFERENCE_UPDATES__TOTAL);

		subMonitor.beginTask("Sort references by project", PROGRESS_CREATE_REFERENCE_UPDATES__SORT_BY_PROJECT);
		final Multimap<IProject, IReferenceDescription> project2references = sorter
				.sortByProject(referenceDescriptions);

		final SubMonitor allProjectsMonitor = subMonitor
				.newChild(PROGRESS_CREATE_REFERENCE_UPDATES__CREATE_CLUSTERED_REFERENCE_UPDATES)
				.setWorkRemaining(project2references.keySet().size());
		for (final IProject project : project2references.keySet()) {
			checkForCancellation(allProjectsMonitor);

			final Multimap<URI, IReferenceDescription> resource2references = sorter
					.sortByResource(project2references.get(project));
			final ResourceSet resourceSet = resourceSetProvider.get(project);
			createClusteredReferenceUpdates(moveElementArguments, resource2references, resourceSet, updateAcceptor,
					allProjectsMonitor.newChild(1));
		}
	}

	private void createClusteredReferenceUpdates(final MoveElementArguments moveElementArguments,
			final Multimap<URI, IReferenceDescription> resource2references, final ResourceSet resourceSet,
			final IRefactoringUpdateAcceptor updateAcceptor, final IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, PROGRESS_CREATE_CLUSTERED_REFERENCE_UPDATES__TOTAL);

		if (loadTargetResources(moveElementArguments, resourceSet, updateAcceptor.getRefactoringStatus(),
				subMonitor.newChild(PROGRESS_CREATE_CLUSTERED_REFERENCE_UPDATES__LOAD_TARGET_RESOURCES))) {
			if (getClusterSize() > 0) {
				final Set<Resource> targetResources = newHashSet(resourceSet.getResources());
				final Multimap<URI, IReferenceDescription> cluster = HashMultimap.create();
				final SubMonitor clusterMonitor = subMonitor
						.newChild(PROGRESS_CREATE_CLUSTERED_REFERENCE_UPDATES__CREATE_REFERENCE_UPDATES_FOR_CLUSTER)
						.setWorkRemaining(resource2references.keySet().size()
								+ PROGRESS_CREATE_CLUSTERED_REFERENCE_UPDATES__CREATE_REFERENCE_UPDATES_FOR_CLUSTER__EXTRA);
				for (final URI referringResourceURI : resource2references.keySet()) {
					cluster.putAll(referringResourceURI, resource2references.get(referringResourceURI));
					if (cluster.keySet().size() == getClusterSize()) {
						unloadNonTargetResources(resourceSet, targetResources);
						createReferenceUpdatesForCluster(moveElementArguments, cluster, resourceSet, updateAcceptor,
								clusterMonitor.newChild(1));
						cluster.clear();
					}
				}
				if (!cluster.isEmpty()) {
					unloadNonTargetResources(resourceSet, targetResources);
					createReferenceUpdatesForCluster(moveElementArguments, cluster, resourceSet, updateAcceptor,
							clusterMonitor.newChild(1));
				}
			} else {
				createReferenceUpdatesForCluster(moveElementArguments, resource2references, resourceSet, updateAcceptor,
						subMonitor.newChild(
								PROGRESS_CREATE_CLUSTERED_REFERENCE_UPDATES__CREATE_REFERENCE_UPDATES_FOR_CLUSTER));
			}
		}
	}

	private void createReferenceUpdatesForCluster(final MoveElementArguments moveElementArguments,
			final Multimap<URI, IReferenceDescription> resource2references, final ResourceSet resourceSet,
			final IRefactoringUpdateAcceptor updateAcceptor, final IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, PROGRESS_CREATE_REFERENCE_UPDATES_FOR_CLUSTER__TOTAL);

		checkForCancellation(subMonitor);
		final List<URI> unloadableResources = loadReferringResources(resourceSet, resource2references.keySet(),
				updateAcceptor.getRefactoringStatus(),
				subMonitor.newChild(PROGRESS_CREATE_REFERENCE_UPDATES_FOR_CLUSTER__LOAD_REFERRING_RESOURCES));
		for (final URI unloadableResouce : unloadableResources) {
			resource2references.removeAll(unloadableResouce);
		}

		checkForCancellation(subMonitor);
		final List<IReferenceDescription> unresolvableReferences = resolveReferenceProxies(resourceSet,
				resource2references.values(), updateAcceptor.getRefactoringStatus(),
				subMonitor.newChild(PROGRESS_CREATE_REFERENCE_UPDATES_FOR_CLUSTER__RESOLVE_REFERENCE_PROXIES));

		for (final IReferenceDescription unresolvableReference : unresolvableReferences) {
			final URI unresolvableReferringResource = unresolvableReference.getSourceEObjectUri().trimFragment();
			resource2references.remove(unresolvableReferringResource, unresolvableReference);
		}

		checkForCancellation(subMonitor);
		moveElementArguments.getMoveStrategy().resolveContainedCrossReferences(moveElementArguments.getSourceUri(),
				resourceSet);
		moveElementArguments.getMoveStrategy().applyChange(moveElementArguments, resourceSet);
		try {
			createReferenceUpdates(moveElementArguments, resource2references, resourceSet, updateAcceptor,
					subMonitor.newChild(PROGRESS_CREATE_REFERENCE_UPDATES_FOR_CLUSTER__CREATE_REFERENCE_UPDATES));
			checkForCancellation(subMonitor);
		} finally {
			moveElementArguments.getMoveStrategy().revertChange(moveElementArguments, resourceSet);
		}
	}

	private void createReferenceUpdates(final MoveElementArguments moveElementArguments,
			final Multimap<URI, IReferenceDescription> resource2references, final ResourceSet resourceSet,
			final IRefactoringUpdateAcceptor updateAcceptor, final IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, resource2references.keySet().size());

		for (final URI referringResourceURI : resource2references.keySet()) {
			try {
				final Resource resource = resourceSet.getResource(referringResourceURI, true);

				PostprocessorRegistry.getInstance().postprocess(moveElementArguments, resource);

				changeUtil.addSaveAsUpdate(resource, updateAcceptor);
				if (referringResourceURI.equals(moveElementArguments.getSourceUri().trimFragment())) {
					moveElementArguments.setSourceResourceProcessed(true);
				}
				if (referringResourceURI.equals(moveElementArguments.getTargetUri().trimFragment())) {
					moveElementArguments.setTargetResourceProcessed(true);
				}
				subMonitor.newChild(1);
			} catch (final OperationCanceledException e) {
				throw e;
			} catch (final Exception exc) {
				throw new WrappedException(exc);
			}
		}
	}

	private List<URI> loadReferringResources(final ResourceSet resourceSet, final Set<URI> referringResourceURIs,
			final StatusWrapper status, final IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, referringResourceURIs.size());

		final List<URI> unloadableResources = Lists.newArrayList();
		for (final URI referringResourceURI : referringResourceURIs) {
			checkForCancellation(subMonitor);
			final Resource referringResource = resourceSet.getResource(referringResourceURI, true);
			if (referringResource == null) {
				status.add(ERROR, "Could not load referring resource ", referringResourceURI);
				unloadableResources.add(referringResourceURI);
			}
			subMonitor.newChild(1);
		}
		return unloadableResources;
	}

	private List<IReferenceDescription> resolveReferenceProxies(final ResourceSet resourceSet,
			final Collection<IReferenceDescription> values, final StatusWrapper status,
			final IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, values.size());

		final List<IReferenceDescription> unresolvedDescriptions = newArrayList();
		for (final IReferenceDescription referenceDescription : values) {
			checkForCancellation(subMonitor);
			try {
				EObject sourceEObject = resourceSet.getEObject(referenceDescription.getSourceEObjectUri(), true);
				if (sourceEObject == null) {
					handleCannotLoadReferringElement(referenceDescription, status);
				} else {
					// this should not be necessary. see
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=385408
					if (sourceEObject.eIsProxy()) {
						sourceEObject = EcoreUtil.resolve(sourceEObject, sourceEObject.eResource());
						if (sourceEObject.eIsProxy()) {
							handleCannotLoadReferringElement(referenceDescription, status);
						}
					}
					final EObject resolvedReference = resolveReference(sourceEObject, referenceDescription);
					if (resolvedReference == null || resolvedReference.eIsProxy()) {
						handleCannotResolveExistingReference(sourceEObject, referenceDescription, status);
					} else {
						continue;
					}
				}
				unresolvedDescriptions.add(referenceDescription);
			} finally {
				subMonitor.newChild(1);
			}
		}
		return unresolvedDescriptions;
	}

	private void handleCannotResolveExistingReference(final EObject sourceEObject,
			final IReferenceDescription referenceDescription, final StatusWrapper status) {
		status.add(ERROR, "Cannot resolve existing reference.\nMaybe the index is be corrupt. Consider a rebuild.",
				referenceDescription.getSourceEObjectUri());
	}

	private EObject resolveReference(final EObject referringElement, final IReferenceDescription referenceDescription) {
		Object resolvedValue = referringElement.eGet(referenceDescription.getEReference());
		if (referenceDescription.getEReference().isMany()) {
			final List<?> list = (List<?>) resolvedValue;
			resolvedValue = list.get(referenceDescription.getIndexInList());
		}
		return (EObject) resolvedValue;
	}

	private void handleCannotLoadReferringElement(final IReferenceDescription referenceDescription,
			final StatusWrapper status) {
		status.add(ERROR, "Cannot find referring element {0}.\nMaybe the index is be corrupt. Consider a rebuild.",
				referenceDescription.getSourceEObjectUri());
	}

	private void unloadNonTargetResources(final ResourceSet resourceSet, final Set<Resource> targetResources) {
		for (final Resource resource : newArrayList(resourceSet.getResources())) {
			if (!targetResources.contains(resource)) {
				resource.unload();
				resourceSet.getResources().remove(resource);
			}
		}
	}

	private boolean loadTargetResources(final MoveElementArguments moveElementArguments, final ResourceSet resourceSet,
			final StatusWrapper status, final IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor,
				moveElementArguments.getOriginal2movedUris().keySet().size());

		boolean isSuccess = true;
		for (final URI renamedElementURI : moveElementArguments.getOriginal2movedUris().keySet()) {
			checkForCancellation(subMonitor);
			final EObject renamedElement = resourceSet.getEObject(renamedElementURI, true);
			if (renamedElement == null || renamedElement.eIsProxy()) {
				status.add(ERROR, "Cannot load target element {0}.", renamedElementURI);
				isSuccess = false;
			}
			subMonitor.newChild(1);
		}
		return isSuccess;
	}

	private int getClusterSize() {
		return 20;
	}
}
