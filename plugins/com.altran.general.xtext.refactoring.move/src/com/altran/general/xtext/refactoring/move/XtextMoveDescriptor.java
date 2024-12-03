package com.altran.general.xtext.refactoring.move;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Integrates the XtextMoveRefactoring into Eclipse refactoring infrastructure.
 *
 */
public class XtextMoveDescriptor extends RefactoringDescriptor {
	private URI sourceUri;
	private URI targetContainerUri;
	private String targetFeature;

	public static final String ID = "com.altran.general.xtext.refactoring.move.refactoringId";

	public XtextMoveDescriptor() {
		super(ID, null, "Xtext Move Description", "Xtext Move Comment",
				RefactoringDescriptor.MULTI_CHANGE | RefactoringDescriptor.STRUCTURAL_CHANGE);
	}

	@Override
	public Refactoring createRefactoring(final RefactoringStatus status) throws CoreException {
		final URI sourceUri = getSourceUri();
		if (sourceUri == null) {
			status.addFatalError("Missing source");
			return null;
		}

		final URI targetContainerUri = getTargetContainerUri();
		if (targetContainerUri == null) {
			status.addFatalError("Missing targetContainer");
			return null;
		}

		final String targetFeature = getTargetFeature();
		if (targetFeature == null) {
			status.addFatalError("Missing targetFeature");
			return null;
		}

		final XtextMoveProcessorConfig config = new XtextMoveProcessorConfig(sourceUri, targetContainerUri,
				targetFeature);
		final XtextMoveProcessor processor = new XtextMoveProcessor();
		processor.init(config);

		return new org.eclipse.ltk.core.refactoring.participants.MoveRefactoring(processor);
	}

	public URI getSourceUri() {
		return sourceUri;
	}

	public void setSourceUri(final URI sourceUri) {
		Assert.isNotNull(sourceUri);
		this.sourceUri = sourceUri;
	}

	public URI getTargetContainerUri() {
		return targetContainerUri;
	}

	public void setTargetContainerUri(final URI targetContainerUri) {
		Assert.isNotNull(targetContainerUri);
		this.targetContainerUri = targetContainerUri;
	}

	public String getTargetFeature() {
		return targetFeature;
	}

	public void setTargetFeature(final String targetFeature) {
		Assert.isNotNull(targetFeature);
		this.targetFeature = targetFeature;
	}
}
