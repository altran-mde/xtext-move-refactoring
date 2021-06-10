package com.altran.general.xtext.refactoring.move.internal;

import static com.altran.general.xtext.refactoring.move.internal.Utils.checkForCancellation;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.ui.refactoring.impl.RefactoringException;

import com.altran.general.xtext.refactoring.move.MoveElementArguments;
import com.google.common.collect.Maps;

/**
 * @see org.eclipse.xtext.ui.refactoring.impl.RenamedElementTracker
 *
 */
@SuppressWarnings("restriction")
public class DefaultMovedElementTracker implements IMovedElementTracker {

	private static final int PROGRESS_RESOLVE_MOVED_ELEMENTS = 10;
	private static final int PROGRESS_APPLY_CHANGE = 10;
	private static final int PROGRESS_MOVE_DEPENDENT_ELEMENTS = 10;
	private static final int PROGRESS_REVERT_CHANGE = 10;
	private static final int PROGRESS_TOTAL = PROGRESS_RESOLVE_MOVED_ELEMENTS + PROGRESS_APPLY_CHANGE
			+ PROGRESS_MOVE_DEPENDENT_ELEMENTS + PROGRESS_REVERT_CHANGE;

	@Override
	public void moveAndTrack(final Iterable<URI> dependentElementUris, final MoveElementArguments moveElementArguments,
			final ResourceSet resourceSet, final IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, PROGRESS_TOTAL);

		checkForCancellation(subMonitor);
		final Map<EObject, URI> element2OriginalUri = resolveMovedElements(dependentElementUris, moveElementArguments,
				resourceSet, subMonitor.newChild(PROGRESS_RESOLVE_MOVED_ELEMENTS));

		checkForCancellation(subMonitor);
		moveElementArguments.getMoveStrategy().applyChange(moveElementArguments, resourceSet);
		subMonitor.newChild(PROGRESS_APPLY_CHANGE);

		checkForCancellation(subMonitor);
		moveDependentElements(element2OriginalUri, moveElementArguments,
				subMonitor.newChild(PROGRESS_MOVE_DEPENDENT_ELEMENTS));

		checkForCancellation(subMonitor);
		moveElementArguments.getMoveStrategy().revertChange(moveElementArguments, resourceSet);
		subMonitor.newChild(PROGRESS_REVERT_CHANGE);
	}

	protected Map<EObject, URI> resolveMovedElements(final Iterable<URI> dependentElementUris,
			final MoveElementArguments moveElementArguments, final ResourceSet resourceSet,
			final IProgressMonitor monitor) {
		final Map<EObject, URI> result = Maps.newLinkedHashMap();

		for (final URI uri : dependentElementUris) {
			final EObject movedElement = resourceSet.getEObject(uri, true);
			if (movedElement == null) {
				throw new RefactoringException("Cannot resolve contained element " + uri);
			}
			result.put(movedElement, uri);
		}

		return result;
	}

	protected void moveDependentElements(final Map<EObject, URI> element2OriginalUri,
			final MoveElementArguments moveElementArguments, final IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, element2OriginalUri.size());

		final Map<URI, URI> original2movedUris = moveElementArguments.getOriginal2movedUris();

		for (final Entry<EObject, URI> entry : element2OriginalUri.entrySet()) {
			original2movedUris.put(entry.getValue(), EcoreUtil.getURI(entry.getKey()));
			subMonitor.newChild(1);
		}
	}

}
