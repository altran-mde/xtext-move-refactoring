package com.altran.general.xtext.refactoring.move.internal;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.resource.Resource;

import com.altran.general.xtext.refactoring.move.IXtextMoveRefactoringPostprocessor;
import com.altran.general.xtext.refactoring.move.MoveElementArguments;

public class PostprocessorRegistry {
	private static final String CLASS_ATTRIBUTE = "class";
	
	private static PostprocessorRegistry instance;
	
	public static PostprocessorRegistry getInstance() {
		if (instance == null) {
			instance = new PostprocessorRegistry();
		}
		
		return instance;
	}
	
	protected PostprocessorRegistry() {
		
	}
	
	public Set<IXtextMoveRefactoringPostprocessor> collectPostprocessors() {
		return Arrays.stream(Platform.getExtensionRegistry()
		.getExtensionPoint(IXtextMoveRefactoringPostprocessor.EXTENSION_POINT_ID)
		.getExtensions())
		.filter(Objects::nonNull)
		.flatMap(e -> Arrays.stream(e.getConfigurationElements()))
		.filter(Objects::nonNull)
		.map(c -> {
			try {
				return c.createExecutableExtension(CLASS_ATTRIBUTE);
			} catch (CoreException e) {
				return null;
			}
		})
		.filter(IXtextMoveRefactoringPostprocessor.class::isInstance)
		.map(IXtextMoveRefactoringPostprocessor.class::cast)
		.collect(Collectors.toSet());
	}
	
	public void postprocess(final MoveElementArguments moveElementArguments, final Resource updatedResource) {
		collectPostprocessors().forEach(p -> p.postprocess(moveElementArguments, updatedResource));
	}
}
