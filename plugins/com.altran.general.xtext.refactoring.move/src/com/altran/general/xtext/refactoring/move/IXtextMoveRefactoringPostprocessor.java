package com.altran.general.xtext.refactoring.move;

import org.eclipse.emf.ecore.resource.Resource;

public interface IXtextMoveRefactoringPostprocessor {
	String EXTENSION_POINT_ID = "com.altran.general.xtext.refactoring.move.postprocessor";

	void postprocess(final MoveElementArguments moveElementArguments, final Resource updatedResource); 
}
