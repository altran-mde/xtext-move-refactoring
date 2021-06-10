package com.altran.general.xtext.refactoring.move;

import org.eclipse.emf.common.util.URI;

/**
 *
 * @see org.eclipse.xtext.ui.refactoring.ui.IRenameElementContext
 *
 */
@SuppressWarnings("restriction")
public class XtextMoveProcessorConfig {
	private final URI source;
	private final URI targetContainer;
	private final String targetFeature;

	public XtextMoveProcessorConfig(final URI source, final URI targetContainer, final String targetFeature) {
		this.source = source;
		this.targetContainer = targetContainer;
		this.targetFeature = targetFeature;
	}

	public URI getSource() {
		return source;
	}

	public URI getTargetContainer() {
		return targetContainer;
	}

	public String getTargetFeature() {
		return targetFeature;
	}
}
