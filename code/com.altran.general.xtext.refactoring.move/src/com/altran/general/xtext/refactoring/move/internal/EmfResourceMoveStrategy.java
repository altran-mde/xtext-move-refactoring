package com.altran.general.xtext.refactoring.move.internal;

import java.io.IOException;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.ui.refactoring.IRefactoringUpdateAcceptor;

import com.altran.general.xtext.refactoring.move.IMoveStrategy;
import com.altran.general.xtext.refactoring.move.MoveElementArguments;
import com.google.inject.Inject;

/**
 * @see org.eclipse.xtext.ui.refactoring.impl.EmfResourceRenameStrategy
 *
 */
@SuppressWarnings("restriction")
public class EmfResourceMoveStrategy implements IMoveStrategy {

	@Inject
	private EmfResourceChangeUtil changeUtil;

	@Override
	public void createUpdates(final MoveElementArguments moveElementArguments, final ResourceSet resourceSet,
			final IRefactoringUpdateAcceptor updateAcceptor) {
		final URI sourceUri = moveElementArguments.getSourceUri();
		final URI sourceResourceUri = sourceUri.trimFragment();

		resolveContainedCrossReferences(sourceUri, resourceSet);

		applyChange(moveElementArguments, resourceSet);
		try {
			if (!moveElementArguments.isSourceResourceProcessed()) {
				changeUtil.addSaveAsUpdate(resourceSet.getResource(sourceResourceUri, true), updateAcceptor);
				moveElementArguments.setSourceResourceProcessed(true);
			}

			final URI targetResourceUri = moveElementArguments.getTargetUri().trimFragment();
			if (!moveElementArguments.isTargetResourceProcessed() && !sourceResourceUri.equals(targetResourceUri)) {
				final Resource targetResource = resourceSet.getResource(targetResourceUri, true);
				
				PostprocessorRegistry.getInstance().postprocess(moveElementArguments, targetResource);

				changeUtil.addSaveAsUpdate(targetResource, updateAcceptor);
				moveElementArguments.setTargetResourceProcessed(true);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} finally {
			revertChange(moveElementArguments, resourceSet);
		}

	}

	@Override
	public void resolveContainedCrossReferences(final URI uri, final ResourceSet resourceSet) {
		resourceSet.getEObject(uri, true).eAllContents()
				.forEachRemaining(e -> e.eCrossReferences().forEach(r -> EcoreUtil.resolve(r, resourceSet)));
	}

	@Override
	public void applyChange(final MoveElementArguments moveElementArguments, final ResourceSet resourceSet) {
		final EObject moveEObject = resourceSet.getEObject(moveElementArguments.getSourceUri(), true);

		move(moveEObject, resourceSet.getEObject(moveElementArguments.getTargetContainerUri(), true),
				moveElementArguments.getTargetFeature(), -1);

		moveElementArguments.setTargetUri(EcoreUtil.getURI(moveEObject));
	}

	@Override
	public void revertChange(final MoveElementArguments moveElementArguments, final ResourceSet resourceSet) {
		move(resourceSet.getEObject(moveElementArguments.getTargetUri(), true),
				resourceSet.getEObject(moveElementArguments.getSourceContainerUri(), true),
				moveElementArguments.getSourceFeature(), moveElementArguments.getSourceIndex());
	}

	protected void move(final EObject moveEObject, final EObject targetContainerEObject,
			final EStructuralFeature targetFeature, final int index) {
		if (targetFeature.isMany()) {
			@SuppressWarnings("unchecked")
			final List<Object> list = (List<Object>) targetContainerEObject.eGet(targetFeature);
			if (index != -1 && index < list.size()) {
				list.add(index, moveEObject);
			} else {
				list.add(moveEObject);
			}
		} else {
			targetContainerEObject.eSet(targetFeature, moveEObject);
		}
	}
}
