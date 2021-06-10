package com.altran.general.xtext.refactoring.move;

import static com.altran.general.xtext.refactoring.move.internal.Utils.checkForCancellation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.ui.XtextProjectHelper;
import org.eclipse.xtext.ui.refactoring.IDependentElementsCalculator;
import org.eclipse.xtext.ui.refactoring.IRefactoringUpdateAcceptor;
import org.eclipse.xtext.ui.refactoring.impl.ProjectUtil;
import org.eclipse.xtext.ui.refactoring.impl.RefactoringException;
import org.eclipse.xtext.ui.refactoring.impl.RefactoringResourceSetProvider;
import org.eclipse.xtext.ui.refactoring.impl.StatusWrapper;

import com.altran.general.xtext.refactoring.move.internal.IMovedElementTracker;
import com.altran.general.xtext.refactoring.move.internal.IMovedReferenceUpdateDispatcher;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 *
 * @see org.eclipse.xtext.ui.refactoring.impl.RenameElementProcessor
 */
@SuppressWarnings("restriction")
public class XtextMoveProcessor extends MoveProcessor {

	private static final int PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_RESOURCE_SET = 25;
	private static final int PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_MOVE_EOBJECT = 25;
	private static final int PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_TARGET_CONTAINER_EOBJECT = 25;
	private static final int PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_TARGET_FEATURE = 25;
	private static final int PROGRESS_CHECK_INITIAL_CONDITIONS_TOTAL = PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_RESOURCE_SET
			+ PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_MOVE_EOBJECT
			+ PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_TARGET_CONTAINER_EOBJECT
			+ PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_TARGET_FEATURE;

	private static final int PROGESS_CHECK_FINAL_CONDITIONS_GET_DEPENDENT_URIS = 1;
	private static final int PROGESS_CHECK_FINAL_CONDITIONS_MOVE_AND_TRACK = 1;
	private static final int PROGESS_CHECK_FINAL_CONDITIONS_CREATE_REFERENCE_UPDATES = 98;
	private static final int PROGESS_CHECK_FINAL_CONDITIONS_TOTAL = PROGESS_CHECK_FINAL_CONDITIONS_GET_DEPENDENT_URIS
			+ PROGESS_CHECK_FINAL_CONDITIONS_MOVE_AND_TRACK + PROGESS_CHECK_FINAL_CONDITIONS_CREATE_REFERENCE_UPDATES;

	@Inject
	private RefactoringResourceSetProvider resourceSetProvider;

	@Inject
	private IDependentElementsCalculator dependentElementsCalculator;

	@Inject
	private ProjectUtil projectUtil;

	@Inject
	private Provider<IRefactoringUpdateAcceptor> updateAcceptorProvider;

	@Inject
	private Provider<StatusWrapper> statusProvider;

	@Inject
	private IMoveStrategy moveStrategy;

	@Inject
	private IMovedElementTracker movedElementTracker;

	@Inject
	private IMovedReferenceUpdateDispatcher referenceUpdaterDispatcher;

	private StatusWrapper status;

	private XtextMoveProcessorConfig config;

	private ResourceSet resourceSet;

	private IRefactoringUpdateAcceptor updateAcceptor;

	private EObject moveEObject;

	private EObject targetContainerEObject;

	private EStructuralFeature targetFeature;

	public void init(final XtextMoveProcessorConfig config) {
		this.config = config;

		status = statusProvider.get();

		resourceSet = createResourceSet();

		moveEObject = resolveEObject(config.getSource());
		targetContainerEObject = resolveEObject(config.getTargetContainer());
		targetFeature = resolveEFeature(targetContainerEObject, config.getTargetFeature());

		return;
	}

	@Override
	public RefactoringStatus checkInitialConditions(final IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, PROGRESS_CHECK_INITIAL_CONDITIONS_TOTAL);
		try {
			validateResourceSet();
			subMonitor.newChild(PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_RESOURCE_SET).done();
			validateMoveEObject();
			subMonitor.newChild(PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_MOVE_EOBJECT).done();
			validateTargetContainerEObject();
			subMonitor.newChild(PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_TARGET_CONTAINER_EOBJECT).done();
			validateTargetFeature();
			subMonitor.newChild(PROGRESS_CHECK_INITIAL_CONDITIONS_VALIDATE_TARGET_FEATURE).done();
		} catch (final OperationCanceledException e) {
			throw e;
		} catch (final Exception e) {
			handleException(e);
		} finally {
			subMonitor.done();
		}

		return status.getRefactoringStatus();
	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context)
			throws CoreException, OperationCanceledException {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, PROGESS_CHECK_FINAL_CONDITIONS_TOTAL);

		status = statusProvider.get();

		try {
			updateAcceptor = updateAcceptorProvider.get();

			checkForCancellation(subMonitor);
			final Iterable<URI> dependentElementURIs = dependentElementsCalculator.getDependentElementURIs(moveEObject,
					subMonitor.newChild(PROGESS_CHECK_FINAL_CONDITIONS_GET_DEPENDENT_URIS));

			checkForCancellation(subMonitor);
			final MoveElementArguments moveElementArguments = new MoveElementArguments(config, moveEObject,
					targetFeature, moveStrategy, null);

			checkForCancellation(subMonitor);
			movedElementTracker.moveAndTrack(Iterables.concat(getElementUris(), dependentElementURIs),
					moveElementArguments, resourceSet,
					subMonitor.newChild(PROGESS_CHECK_FINAL_CONDITIONS_MOVE_AND_TRACK));

			checkForCancellation(subMonitor);
			referenceUpdaterDispatcher.createReferenceUpdates(moveElementArguments, resourceSet, updateAcceptor,
					subMonitor.newChild(PROGESS_CHECK_FINAL_CONDITIONS_CREATE_REFERENCE_UPDATES));

			checkForCancellation(subMonitor);
			if (!moveElementArguments.isSourceResourceProcessed()
					|| !moveElementArguments.isTargetResourceProcessed()) {
				moveStrategy.createUpdates(moveElementArguments, resourceSet, updateAcceptor);
			}

			checkForCancellation(subMonitor);
			status.merge(updateAcceptor.getRefactoringStatus());

		} catch (final OperationCanceledException e) {
			throw e;
		} catch (final Exception e) {
			handleException(e);
		} finally {
			subMonitor.done();
		}

		return status.getRefactoringStatus();
	}

	@Override
	public Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			return updateAcceptor.createCompositeChange(
					"Move " + config.getSource() + " to " + config.getTargetContainer(), monitor);
		} finally {
			monitor.done();
		}
	}

	@Override
	public RefactoringParticipant[] loadParticipants(final RefactoringStatus status,
			final SharableParticipants sharedParticipants) throws CoreException {
		final RefactoringParticipant[] result = ParticipantManager.loadMoveParticipants(status, this, config,
				new MoveArguments(config.getTargetContainer(), true), new String[] { XtextProjectHelper.NATURE_ID },
				sharedParticipants);
		return result;
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return true;
	}

	@Override
	public String getProcessorName() {
		return "Move Element";
	}

	@Override
	public String getIdentifier() {
		return XtextMoveProcessor.class.getName();
	}

	@Override
	public Object[] getElements() {
		return new Object[] { config.getSource() };
	}

	protected Set<URI> getElementUris() {
		return Arrays.stream(getElements()).filter(URI.class::isInstance).map(URI.class::cast)
				.collect(Collectors.toSet());
	}

	protected ResourceSet createResourceSet() {
		final IProject targetProject = projectUtil.getProject(config.getTargetContainer());
		return resourceSetProvider.get(targetProject);
	}

	protected EObject resolveEObject(final URI uri) {
		if (resourceSet != null && uri != null) {
			return resourceSet.getEObject(uri, true);
		}

		return null;
	}

	protected EStructuralFeature resolveEFeature(final EObject targetContainerEObject2, final String featureName) {
		return targetContainerEObject2.eClass().getEStructuralFeature(featureName);
	}

	protected void validateResourceSet() {
		if (resourceSet == null) {
			throw new RefactoringException("Cannot get ResourceSet for " + config.getSource());
		}
	}

	protected void validateMoveEObject() {
		if (moveEObject == null) {
			throw new RefactoringException("Cannot resolve source element " + config.getSource());
		}
		final EObject sourceContainerEObject = moveEObject.eContainer();
		if (sourceContainerEObject == null) {
			throw new RefactoringException("Cannot move element without container: " + config.getSource());
		}
		final Resource sourceResource = moveEObject.eResource();
		if (sourceResource == null) {
			throw new RefactoringException("Cannot move element without eResource: " + config.getSource());
		}
	}

	protected void validateTargetContainerEObject() {
		if (targetContainerEObject == null) {
			throw new RefactoringException("Cannot resolve target container element " + config.getTargetContainer());
		}
		final Resource targetResource = moveEObject.eResource();
		if (targetResource == null) {
			throw new RefactoringException(
					"Cannot move element to target without eResource: " + config.getTargetContainer());
		}
	}

	protected void validateTargetFeature() {
		if (null == targetFeature) {
			throw new RefactoringException("target container does not have feature " + config.getTargetFeature());
		}
		
		if (targetContainerEObject.eClass().getFeatureID(targetFeature) == -1) {
			throw new RefactoringException("target container does not have feature " + targetFeature);
		}

		if (!EcoreUtil2.isAssignableFrom((EClass) targetFeature.getEType(), moveEObject.eClass())) {
			throw new RefactoringException(
					"targetFeature " + targetFeature.getName() + "'s type " + targetFeature.getEType().getName()
							+ " is not a supertype of the moved object's type " + moveEObject.eClass().getName());
		}
	}

	protected void handleException(final Exception e) {
		status.add(RefactoringStatus.FATAL, "Error during refactoring: {0}", e);
	}
}
