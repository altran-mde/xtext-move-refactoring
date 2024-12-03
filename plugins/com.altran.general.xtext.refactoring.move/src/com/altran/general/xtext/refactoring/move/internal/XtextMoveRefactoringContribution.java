package com.altran.general.xtext.refactoring.move.internal;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

import com.altran.general.xtext.refactoring.move.XtextMoveDescriptor;

/**
 * Integrates the XtextMoveRefactoring into Eclipse refactoring infrastructure.
 *
 */
public class XtextMoveRefactoringContribution extends RefactoringContribution {

	private static final String ATTRIBUTE_SOURCE_URI = "sourceUri";
	private static final String ATTRIBUTE_TARGET_CONTAINER_URI = "targetContainerUri";
	private static final String ATTRIBUTE_TARGET_FEATURE = "targetFeature";

	@Override
	public RefactoringDescriptor createDescriptor() {
		return new XtextMoveDescriptor();
	}

	@Override
	public RefactoringDescriptor createDescriptor(final String id, final String project, final String description,
			final String comment, final Map<String, String> arguments, final int flags) throws IllegalArgumentException {
		final String sourceUriString = (String) arguments.get(ATTRIBUTE_SOURCE_URI);
		if (StringUtils.isBlank(sourceUriString)) {
			throw new IllegalArgumentException("Cannot restore XtextMoveDescriptor from map, sourceUri missing");
		}
		final URI sourceUri = URI.createURI(sourceUriString);

		final String targetContainerUriString = (String) arguments.get(ATTRIBUTE_TARGET_CONTAINER_URI);
		if (StringUtils.isBlank(targetContainerUriString)) {
			throw new IllegalArgumentException(
					"Cannot restore XtextMoveDescriptor from map, targetContainerUri missing");
		}
		final URI targetContainerUri = URI.createURI(targetContainerUriString);

		final String targetFeatureString = (String) arguments.get(ATTRIBUTE_TARGET_FEATURE);
		if (StringUtils.isBlank(targetFeatureString)) {
			throw new IllegalArgumentException("Cannot restore XtextMoveDescriptor from map, targetFeature missing");
		}
		final String targetFeature = StringUtils.trim(targetFeatureString);

		final XtextMoveDescriptor descriptor = new XtextMoveDescriptor();
		descriptor.setProject(project);
		descriptor.setDescription(description);
		descriptor.setComment(comment);
		descriptor.setFlags(flags);
		descriptor.setSourceUri(sourceUri);
		descriptor.setTargetContainerUri(targetContainerUri);
		descriptor.setTargetFeature(targetFeature);

		return descriptor;
	}

}
