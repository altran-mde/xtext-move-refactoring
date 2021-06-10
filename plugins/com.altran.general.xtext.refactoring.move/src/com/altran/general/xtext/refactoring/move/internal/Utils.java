package com.altran.general.xtext.refactoring.move.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Simple helper to de-clutter cancellation checks.
 *
 */
public class Utils {
	public static void checkForCancellation(final IProgressMonitor monitor) throws OperationCanceledException {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}
}
