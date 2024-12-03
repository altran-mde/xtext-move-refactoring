package com.altran.general.xtext.refactoring.move.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.xtext.parser.IEncodingProvider;
import org.eclipse.xtext.resource.SaveOptions;
import org.eclipse.xtext.ui.refactoring.IRefactoringUpdateAcceptor;
import org.eclipse.xtext.ui.refactoring.impl.IRefactoringDocument;

import com.google.inject.Inject;

/**
 * Copied from org.eclipse.xtext.ui.refactoring.impl.EmfResourceChangeUtil to add format option during save.
 */
@SuppressWarnings("restriction")
public class EmfResourceChangeUtil {
	@Inject
	private IEncodingProvider encodingProvider;

	public void addSaveAsUpdate(Resource resource, IRefactoringUpdateAcceptor updateAcceptor) throws IOException {
		IRefactoringDocument document = updateAcceptor.getDocument(resource.getURI());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		resource.save(outputStream, SaveOptions.newBuilder().format().getOptions().toOptionsMap());
		String newContent = new String(outputStream.toByteArray(), encodingProvider.getEncoding(resource.getURI()));
		updateAcceptor.accept(resource.getURI(),
				new ReplaceEdit(0, document.getOriginalContents().length(), newContent));
	}
}
