package com.altran.general.xtext.refactoring.move;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.google.common.collect.Maps;

/**
 * @see org.eclipse.xtext.ui.refactoring.ElementRenameArguments
 *
 */
@SuppressWarnings("restriction")
public class MoveElementArguments {
	private final XtextMoveProcessorConfig config;

	private final EStructuralFeature targetFeature;

	private final IMoveStrategy moveStrategy;

	private Map<URI, URI> original2movedUris;

	private EReference sourceFeature;

	private int sourceIndex;

	private URI sourceContainerUri;

	private URI targetUri;

	private boolean sourceResourceProcessed;

	private boolean targetResourceProcessed;

	public MoveElementArguments(final XtextMoveProcessorConfig config, final EObject moveEObject,
			final EStructuralFeature targetFeature, final IMoveStrategy moveStrategy,
			final Map<URI, URI> original2movedUris) {
		super();
		this.config = config;
		this.targetFeature = targetFeature;
		this.moveStrategy = moveStrategy;
		this.original2movedUris = original2movedUris;

		initDerivedArguments(moveEObject);
	}

	public void initDerivedArguments(final EObject moveEObject) {
		final EObject sourceContainerEObject = moveEObject.eContainer();

		this.sourceFeature = moveEObject.eContainmentFeature();

		if (sourceFeature.isMany()) {
			@SuppressWarnings("unchecked")
			final List<Object> list = (List<Object>) sourceContainerEObject.eGet(sourceFeature);
			this.sourceIndex = list.indexOf(moveEObject);
		} else {
			this.sourceIndex = -1;
		}

		this.sourceContainerUri = EcoreUtil.getURI(sourceContainerEObject);
	}

	public XtextMoveProcessorConfig getConfig() {
		return config;
	}

	public EStructuralFeature getTargetFeature() {
		return targetFeature;
	}

	public IMoveStrategy getMoveStrategy() {
		return moveStrategy;
	}

	public Map<URI, URI> getOriginal2movedUris() {
		if (original2movedUris == null) {
			original2movedUris = Maps.newLinkedHashMap();
		}

		return original2movedUris;
	}

	public EReference getSourceFeature() {
		return sourceFeature;
	}

	public int getSourceIndex() {
		return sourceIndex;
	}

	public URI getSourceUri() {
		if (this.config != null) {
			return config.getSource();
		}

		return null;
	}

	public URI getTargetContainerUri() {
		if (this.config != null) {
			return config.getTargetContainer();
		}

		return null;
	}

	public URI getTargetUri() {
		return this.targetUri;
	}

	public void setTargetUri(final URI targetUri) {
		this.targetUri = targetUri;
	}

	public URI getSourceContainerUri() {
		return this.sourceContainerUri;
	}

	public void setSourceResourceProcessed(final boolean processed) {
		this.sourceResourceProcessed = processed;
	}

	public boolean isSourceResourceProcessed() {
		return sourceResourceProcessed;
	}

	public void setTargetResourceProcessed(final boolean processed) {
		this.targetResourceProcessed = processed;
	}

	public boolean isTargetResourceProcessed() {
		return targetResourceProcessed;
	}
}
