package edu.washington.cs.quickfix.speculation.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ltk.core.refactoring.Change;

import com.kivancmuslu.www.timer.Timer;

//import edu.cs.washington.quickfix.speculation.converter.IJavaCompletionProposalConverter;
import edu.washington.cs.quickfix.speculation.calc.model.ActivationRecord;
import edu.washington.cs.quickfix.speculation.calc.model.AugmentedCompletionProposal;
import edu.washington.cs.quickfix.speculation.calc.model.SpeculativeAnalysisListener;
import edu.washington.cs.quickfix.speculation.calc.model.SpeculativeAnalysisNotifier;
import edu.washington.cs.quickfix.speculation.exception.InvalidatedException;
import edu.washington.cs.quickfix.speculation.gui.SpeculationPreferencePage;
import edu.washington.cs.quickfix.speculation.hack.QuickFixDialogCoordinator;
import edu.washington.cs.quickfix.speculation.model.Pair;
import edu.washington.cs.quickfix.speculation.model.SpeculationUtility;
import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.synchronization.sync.internal.ProjectModificationListener;
import edu.washington.cs.synchronization.sync.task.internal.TaskWorker;
import edu.washington.cs.threading.MortalThread;
import edu.washington.cs.util.eclipse.BuilderUtility;
import edu.washington.cs.util.eclipse.EclipseUIUtility;
import edu.washington.cs.util.eclipse.QuickFixUtility;
import edu.washington.cs.util.eclipse.model.Squiggly;

@SuppressWarnings("restriction")
public class SpeculationCalculator extends MortalThread implements ProjectModificationListener,
        SpeculativeAnalysisNotifier
{
    private Squiggly [] shadowCompilationErrors_;
    private Map<Squiggly, Squiggly> shadowCompilationErrorResolutionMap_;
    private Map <Squiggly, IJavaCompletionProposal []> shadowProposalsMap_;
    /**
     * Mapping that stores the current calculation information. <br>
     * This field is protected by {@link #speculativeProposalsLock_}.
     */
    // Speculative proposals map is from shadow compilation errors to original augmented completion proposals.
    private Map <Squiggly, AugmentedCompletionProposal []> speculativeProposalsMap_;
    private IProject shadowProject_;
//    private IJavaCompletionProposalConverter proposalConverter_;
    private static final Logger logger = Logger.getLogger(SpeculationCalculator.class.getName());
    private Map <String, Squiggly []> cachedProposals_;
    /**
     * Current file that is open in the Eclipse editor. Used for prioritizing the speculative analysis (i.e., deciding
     * which markers to compute first). <br>
     */
    private volatile IFile currentFile_;
    private volatile int currentCursorOffset_;
    private volatile ActivationRecord activationRecord_;
    private ArrayList <SpeculativeAnalysisListener> speculativeAnalysisListeners_;
    private ArrayList <SpeculativeAnalysisListener> speculativeAnalysisListenersToRemove_;
    private ReentrantLock speculativeAnalysisListenersLock_;
    /** List of proposals that represent the best outcome for the current calculation. */
    // Does not need locking. Calculated and accessed locally, only exposed after the calculation is complete.
    private ArrayList <AugmentedCompletionProposal> bestProposals_;
    static
    {
        //@formatter:off
        /*
         * INFO =>  High level message passing, beginning and end of each speculative analysis, 
         *          and each quick fix that is speculated.
         * FINE =>  See information for each proposal (# of compilation errors).
         */
        //@formatter:on
        logger.setLevel(Level.INFO);
    }
    private final ProjectSynchronizer synchronizer_;
    private static final boolean DEVELOPMENT_TEST = false;
    public final static boolean TEST_TRANSFORMATION = DEVELOPMENT_TEST;
    public final static boolean TEST_SYNCHRONIZATION = true;
    private Date localSpeculationCompletionTime_ = null;
    private Date analysisCompletionTime_ = null;
    private ReentrantLock timingLock_;
    private volatile int typingSessionLength_ = -1;

    public SpeculationCalculator(ProjectSynchronizer synchronizer)
    {
        super(0, "Speculation Calculator");
        setPriority(Thread.MIN_PRIORITY);
        synchronizer_ = synchronizer;
        shadowProject_ = synchronizer_.getShadowProject();
//        proposalConverter_ = new IJavaCompletionProposalConverter(synchronizer_.getProject());
        shadowProposalsMap_ = new HashMap <Squiggly, IJavaCompletionProposal []>();
        speculativeProposalsMap_ = new HashMap <Squiggly, AugmentedCompletionProposal []>();
        cachedProposals_ = new HashMap <String, Squiggly []>();
        bestProposals_ = new ArrayList <AugmentedCompletionProposal>();
        speculativeAnalysisListeners_ = new ArrayList <SpeculativeAnalysisListener>();
        speculativeAnalysisListenersToRemove_ = new ArrayList <SpeculativeAnalysisListener>();
        speculativeAnalysisListenersLock_ = new ReentrantLock();
        synchronizer_.getTaskWorker().addProjectChangeListener(this);
        // Initialize locks
        activationRecord_ = new ActivationRecord(true, false);
        timingLock_ = new ReentrantLock();
        
        shadowCompilationErrorResolutionMap_ = new HashMap <Squiggly, Squiggly>();
        
        if (typingSessionLength_ == -1)
            updateTypingSessionTime(SpeculationPreferencePage.getInstance().getTypingSessionLength());
    }
    
    public void updateTypingSessionTime(int value)
    {
        typingSessionLength_ = value;
        getTaskWorker().updateTypingSessionLength(typingSessionLength_);
    }
    
    private TaskWorker getTaskWorker()
    {
        return synchronizer_.getTaskWorker();
    }

    /**
     * Returns the display string of calculated {@link AugmentedCompletionProposal}s, which are computed by the
     * speculative analysis, that correspond to the given problem locations. <br>
     * If the speculative analysis is completed, then best proposals are also added to this list. <br>
     * The returned list is sorted (as it would be presented in the Eclipse). <br>
     * If there is no calculated information at the time of query, <code>null</code> is returned.
     * 
     * @param locations Problem locations that create the proposals.
     * @return The display string of proposals that are computed during the speculative analysis. <br>
     *         The list is sorted.
     */
    public String [] getCalculatedProposals(IProblemLocation [] locations)
    {
        // Create a local copy of currently calculated proposals.
        Map <Squiggly, AugmentedCompletionProposal []> speculativeProposalsLocalMap = getSpeculativeProposalsMap();
        HashSet <AugmentedCompletionProposal> calculatedProposals = new HashSet <AugmentedCompletionProposal>();
        // The locations passed may have different references (i.e., we cannot compare object equality).
        for (IProblemLocation loc: locations)
        {
            for (Squiggly shadowCompilationError: speculativeProposalsLocalMap.keySet())
            {
                if (SpeculationUtility.sameProblemLocationContent(shadowCompilationError.getLocation(), loc))
                {
                    AugmentedCompletionProposal [] proposals = speculativeProposalsLocalMap.get(shadowCompilationError);
                    if (proposals != null)
                    {
                        for (AugmentedCompletionProposal proposal: proposals)
                            calculatedProposals.add(proposal);
                    }
                    break;
                }
            }
        }
        // super.isSynched() ==> !isWorking(), which means the speculative analysis is completed.
        if (super.isSynched())
        {
            for (AugmentedCompletionProposal proposal: bestProposals_)
                calculatedProposals.add(proposal);
        }
        if (calculatedProposals.size() == 0)
            return null;
        else
        {
            AugmentedCompletionProposal [] calculatedProposalsArray = calculatedProposals
                    .toArray(new AugmentedCompletionProposal [calculatedProposals.size()]);
            Arrays.sort(calculatedProposalsArray);
            String [] result = new String [calculatedProposalsArray.length];
            for (int a = 0; a < result.length; a++)
                result[a] = calculatedProposalsArray[a].getFinalDisplayString();
            return result;
        }
    }

    private boolean updateShadowCompilationErrors(Squiggly [] shadowCompilationErrors)
    {
        Squiggly [] currentCompilationErrors;
        synchronized(this)
        {
            currentCompilationErrors = shadowCompilationErrors_;
            shadowCompilationErrorResolutionMap_.clear();
        }
        
        boolean result = true;
        if (shadowCompilationErrors == null)
            result = false;
        else if (currentCompilationErrors == null)
            result = false;
        else if (currentCompilationErrors.length != shadowCompilationErrors.length)
            result = false;
        else
        {
            HashMap <Squiggly, Squiggly> ceResolutionMap = new HashMap <Squiggly, Squiggly>();
            // The number of compilation errors are the same. Let's check the content.
            ArrayList<Squiggly> oldCEs = new ArrayList<Squiggly>(Arrays.asList(currentCompilationErrors));
            ArrayList<Squiggly> newCEs = new ArrayList<Squiggly>(Arrays.asList(shadowCompilationErrors));
            outer: while (oldCEs.size() > 0)
            {
                Squiggly oldCE = oldCEs.get(0);
                ArrayList <Squiggly> matches = new ArrayList <Squiggly>();
                for (Squiggly newCE: newCEs)
                {
                    try
                    {
                        if (SpeculationUtility.sameSquigglyContent(oldCE, newCE))
                            matches.add(newCE);
                    }
                    catch (JavaModelException e)
                    {
                        // We don't care the exceptions...
                        result = false;
                        break outer;
                    }
                    catch (BadLocationException e)
                    {
                        // We don't care the exceptions...
                        result = false;
                        break outer;
                    }
                }
                if (matches.size() == 1)
                {
                    // We have found a corresponding pair that matches the current compilation error. 
                    // Remove them both and continue.
                    oldCEs.remove(oldCE);
                    Squiggly match = matches.get(0);
                    newCEs.remove(match);
                    ceResolutionMap.put(oldCE, match);
                }
                else
                {
                    System.out.println("Cannot find a match for compilation error = " + oldCE);
                    // We couldn't find a pair for the current compilation error. Something has changed.
                    result = false;
                    break;
                }
            }
            if (result)
            {
                synchronized(this)
                {
                    shadowCompilationErrorResolutionMap_ = ceResolutionMap;
                }
            }
        }
        if (!result)
        {
            synchronized(this)
            {
                shadowCompilationErrors_ = shadowCompilationErrors;
            }
            clearGlobalState();
        }
        return result;
    }

    private synchronized Squiggly [] getShadowCompilationErrors()
    {
        return shadowCompilationErrors_;
    }

    private ArrayList <Squiggly> getShadowCompilationErrorsAsList()
    {
        Squiggly [] shadowCompilationErrors = getShadowCompilationErrors();
        if (shadowCompilationErrors == null)
            return new ArrayList <Squiggly>();
        else
            return new ArrayList <Squiggly>(Arrays.asList(shadowCompilationErrors));
    }

    @Override
    protected void preDoWork() throws InterruptedException
    {
        activationRecord_.waitUntilActivated();
        if (isDead())
            return;
    }

    private void compareWithCurrentBest(AugmentedCompletionProposal proposal)
    {
        if (bestProposals_.isEmpty())
        // there is no current best.
        {
            if (proposal.isResultAvaliable())
                bestProposals_.add(proposal);
            // else, this proposal is still not a good candidate.
        }
        else
        {
            AugmentedCompletionProposal bestProposal = bestProposals_.get(0);
            // comparison indicates the relative location of best proposal vs. proposal.
            int comparison = bestProposal.compareTo(proposal);
            if (comparison == 0)
            {
                // as good as current best.
                if (!bestProposals_.contains(proposal))
                    // make sure that it is not already added
                    bestProposals_.add(proposal);
            }
            // > means that best proposal should be coming later in the list. That is why here we swap them.
            else if (comparison > 0)
            {
                // new proposal is better then the old best(s). Clear the old list and add this proposal.
                bestProposals_.clear();
                bestProposals_.add(proposal);
            }
            // else, current proposal is worse then the best, ignore.
        }
    }
    
    private TaskWorker synchronizeWithTaskWorker()
    {
        TaskWorker currentWorker = synchronizer_.getTaskWorker();
        currentWorker.block();
        currentWorker.waitUntilSynchronization();
        return currentWorker;
    }

    @Override
    protected void doWork() throws InterruptedException
    {
        if (isDead())
            return;
        Timer.startSession();
        TaskWorker currentWorker = synchronizeWithTaskWorker();
        // We make sure to deactivate auto-building so that from this point on it will not collide with our calculations.
        boolean prevAutoBuilding = deactivateAutoBuilding();
        boolean shallSkip = false;
        try
        {
            if (TEST_SYNCHRONIZATION)
                // Note that test synchronization method puts the projects in sync even if they aren't.
                // Since we haven't done any calculation yet, wee can assume everything is okay even if it wasn't.
                testSynchronization();
            Squiggly [] shadowCompilationErrors = computeShadowCompilationErrors();
            shallSkip = updateShadowCompilationErrors(shadowCompilationErrors);
            // Reset the activation record so that the current round will be valid and speculator will be deactivated
            // before the next round.
            activationRecord_.reset();
            if (!shallSkip)
                // If we detect no change in the content of the compilation errors, then there is no need to run the
                // speculative analysis again. Just skip the analysis, we will return the results from the mapping.
                doSpeculativeAnalysis();
            else
                logger.info("Speculative analysis for this round is skipped since there is no change on the compilation errors.");
        }
        catch (InvalidatedException e)
        {
            // If for some reason, the analysis is invalidated (meaning the contents of the projects has changed), 
            // then it means that we should not wait for the next change in the project, we need to reactivate
            // the calculator for the next round.
            activationRecord_.activate();
            // This is a known exception, so we don't need to log it (at least not with severity).
            logger.info("Current speculative analysis instance is invalidated.");
        }
        finally
        {
            // Everything about the analysis is completed at this moment good or bad. We first reactivate the auto-building
            // if it was active at the beginning.
            if (prevAutoBuilding)
                activateAutoBuilding();
            else
                logger.info("Not activating auto-building since it was deactivated at the beginning of the analysis.");
            // Unblock the task worker so that any changes done to the other project can be applied to the shadow.
            currentWorker.unblock();
            // signal that the thread completed the analysis and the results can be grabbed.
            // TODO Do we really need this? Doesn't signal analysis round completed after each compilation error kind of do it?
            stopWorking();
            // This extra check on activation record is needed because we can also enter this block into the exceptional path.
            // We also cannot move this up above in 'try' since it needs to be done after the auto-building reactivation, worker
            // unblock, etc.
            if (activationRecord_.isValid())
            {
                // if everything went well, 
                if (shallSkip)
                    // and we didn't need to do any analysis this round, we need to update the best proposals, because their
                    // location might have been changed. We basically re-map them looking at the mapping.
                    updateBestProposals();
                // set the best proposals to the dialogs (if they are open).
                QuickFixDialogCoordinator.getCoordinator().setBestProposals(new ArrayList<AugmentedCompletionProposal>(bestProposals_));
                // signal analysis completion.
                signalSpeculativeAnalysisComplete();
            }
            logger.info("");
            Timer.completeSession();
            logger.info("Completing the speculative analysis took: " + Timer.getTimeAsString());
        }
    }

    private void updateBestProposals()
    {
        if (shadowCompilationErrorResolutionMap_.size() == 0 || bestProposals_ == null)
            return;
        for (AugmentedCompletionProposal bestProposal: bestProposals_)
        {
            Squiggly shadowCompilationError = bestProposal.getCompilationError();
            Squiggly recentShadowCE = shadowCompilationErrorResolutionMap_.get(shadowCompilationError);
            if (recentShadowCE == null)
                logger.finer("Cannot resolve the recent version of the compilation error: " + shadowCompilationError);
//            assert recentShadowCE != null: "Cannot resolve the recent version of the compilation error: " + shadowCompilationError;
            bestProposal.updateCompilationError(recentShadowCE);
        }
    }

    private boolean deactivateAutoBuilding()
    {
        boolean prevValue = BuilderUtility.isAutoBuilding();
        if (prevValue)
            BuilderUtility.setAutoBuilding(false);
        return prevValue;
    }
    
    private void activateAutoBuilding()
    {
        BuilderUtility.setAutoBuilding(true);
    }

    private Squiggly [] computeShadowCompilationErrors()
    {
        buildShadowProject();
        Squiggly [] shadowCompilationErrors = null;
        try
        {
            shadowCompilationErrors = getShadowCEs();
            
            // Added for debugging (i.e., better understanding of errors vs. warnings)
//            Squiggly [] shadowSquigglies = getShadowSquigglies();
//            for (Squiggly squiggly: shadowSquigglies)
//            {
//                if (squiggly.isWarning())
//                    System.out.println("Detected a warning marker of type = " + squiggly.getErrorCode() + " in " + squiggly.getResource().getName());
//            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return shadowCompilationErrors;
    }

    private void clearGlobalState()
    {
        clearSpeculativeProposalsMap();
        clearProposalsMap();
        clearTimings();
        bestProposals_ = new ArrayList <AugmentedCompletionProposal>();
        cachedProposals_.clear();
        QuickFixDialogCoordinator.getCoordinator().clearBestProposals();
        QuickFixDialogCoordinator.getCoordinator().clear();
    }

    private void clearTimings()
    {
        timingLock_.lock();
        localSpeculationCompletionTime_ = null;
        analysisCompletionTime_ = null;
        timingLock_.unlock();
    }

    private void doSpeculativeAnalysis() throws InvalidatedException
    {
        if (activationRecord_.isInvalid() || isDead())
            throw new InvalidatedException();
        logger.info("Speculative analysis started...");
        // The place of signal is very important. Basically, it has to be done after all accessible state is cleared
        // to defaults.
        signalSpeculativeAnalysisStart();
        processCompilationErrors();
    }

    private void processCompilationErrors() throws InvalidatedException
    {
        try
        {
            int counter = 0;
            // Clear the global cached quick fixes.
            ArrayList <Squiggly> compilationErrors = getShadowCompilationErrorsAsList();
            while (!compilationErrors.isEmpty())
            {
                CompilationErrorComparator cec = new CompilationErrorComparator();
                Collections.sort(compilationErrors, cec);
                Squiggly shadowCompilationError = compilationErrors.get(0);
                IJavaCompletionProposal [] shadowProposals = computeShadowProposals(shadowCompilationError);
                addToShadowProposalsMap(shadowCompilationError, shadowProposals);
                if (shadowProposals != null)
                {
                    AugmentedCompletionProposal [] shadowCalculatedProposal = processCompilationError(shadowCompilationError);
                    counter += shadowProposals.length;
                    addToSpeculationProposalsMap(shadowCompilationError, shadowCalculatedProposal);
                    // Signal the quick fix grabbers so that they might attempt to look for the
                    // results..
                    signalSpeculativeAnalysisRoundComplete();
                }
                else
                    logger.warning("For compilation error = " + shadowCompilationError.toString()
                            + ", there are no proposals!");
                compilationErrors.remove(shadowCompilationError);
            }
            logger.info("Speculative analysis completed: Available proposals (" + counter
                    + ") and their results calculated in advance...");
        }
        catch (InvalidatedException ie)
        {
            // This is a known exception, so we re-throw it instead of logging.
            throw ie;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "During speculative analysis, an exception occurred.", e);
            /*
             * I have removed this recursive call, because for some errors, it starts to throw the same error over and
             * over which causes the log to become very very big.
             */
            // doSpeculativeAnalysis();
        }
    }

    private AugmentedCompletionProposal [] processCompilationError(Squiggly shadowCompilationError)
            throws InvalidatedException
    {
        if (activationRecord_.isInvalid() || isDead())
            throw new InvalidatedException();
        try
        {
            int errorsBefore = getNumberOfErrors();
            logger.info("For compilation error: " + shadowCompilationError.toString() + " type = " + shadowCompilationError.getErrorCode());
            // This access to proposalsMap_ is safe since the only thread that can modify it is the calculator (this).
            IJavaCompletionProposal [] shadowProposals = shadowProposalsMap_.get(shadowCompilationError);
            logger.info("Number of proposals = " + shadowProposals.length);
            AugmentedCompletionProposal [] result = new AugmentedCompletionProposal [shadowProposals.length];
            int counter = 0;
            for (int a = 0; a < shadowProposals.length; a++)
            {
                IJavaCompletionProposal shadowProposal = shadowProposals[a];
                // Note: Transformation does nothing if the original proposal is not null and development test is not
                // active.
                String displayString = shadowProposal.getDisplayString();
                // Do a quick lookup from the cache map, and if it exists there, return from there
                // without calculating it again.
                Squiggly [] errorsAfter;
                if (cachedProposals_.containsKey(displayString))
                {
                    errorsAfter = cachedProposals_.get(displayString);
                    logger.fine("Proposal (" + displayString + ") was already calculated in this pass, returning "
                            + errorsAfter.length + " from cache map.");
                }
                else
                {
                    errorsAfter = processProposal(shadowProposal);
                    cachedProposals_.put(displayString, errorsAfter);
                    // TODO Why do I need this?
                    if (errorsAfter == Squiggly.UNKNOWN)
                        errorsAfter = getShadowCEs();
                    int errorsAfterUndo = getShadowCEs().length;
                    if (errorsAfterUndo != errorsBefore)
                    {
                        logger.warning("For proposal = " + displayString + ", class = " + shadowProposal.getClass()
                                + ", applying change and undo broke the synchronization of the projects. "
                                + "Before change = " + errorsBefore + ", after change = " + errorsAfter.length
                                + ". Re-synching projects...");
                        syncProjects();
                    }
                }
                if (errorsAfter == Squiggly.UNKNOWN)
                    errorsAfter = getShadowCEs();
                AugmentedCompletionProposal augmentedProposal = new AugmentedCompletionProposal(shadowProposal,
                        shadowCompilationError, errorsAfter, errorsBefore);
                compareWithCurrentBest(augmentedProposal);
                result[counter] = augmentedProposal;
                counter++;
                logger.fine("");
            }
            return result;
        }
        catch (InvalidatedException e)
        {
            throw e;
        }
    }

//    @SuppressWarnings("unused")
//    private IJavaCompletionProposal transformShadowProposal(IJavaCompletionProposal shadowProposal,
//            Squiggly shadowCompilationError, IJavaCompletionProposal originalProposal)
//    {
//        if (originalProposal == null || TEST_TRANSFORMATION)
//        {
//            if (originalProposal == null)
//                logger.warning("Couldn't get the corresponding proposal from original project =  "
//                        + shadowProposal.getDisplayString() + ", proposal.class = " + shadowProposal.getClass());
//            try
//            {
//                IJavaCompletionProposal convertedProposal = convertToOriginalProposal(shadowProposal,
//                        shadowCompilationError.getLocation());
//                if (convertedProposal != null)
//                    originalProposal = convertedProposal;
//            }
//            catch (Exception e)
//            {
//                logger.log(Level.SEVERE, "Cannot convert propoal of type = " + shadowProposal.getClass(), e);
//            }
//        }
//        return originalProposal;
//    }
    
//    private IJavaCompletionProposal convertToOriginalProposal(IJavaCompletionProposal shadowProposals,
//            IProblemLocation shadowLocation)
//    {
//        return proposalConverter_.convert(shadowProposals, shadowLocation);
//    }

    // returns the remaining compilation errors after the proposal is applied to the project.
    private Squiggly [] processProposal(IJavaCompletionProposal shadowProposal) throws InvalidatedException
    {
        if (activationRecord_.isInvalid() || isDead())
            throw new InvalidatedException();
        if (SpeculationUtility.isFlaggedProposal(shadowProposal))
//            return new Squiggly[2];
            return Squiggly.NOT_COMPUTED;
        if (SpeculationUtility.isInteractiveProposal(shadowProposal))
//            return new Squiggly[2];
            return Squiggly.NOT_COMPUTED;
        
        Squiggly [] errors = Squiggly.UNKNOWN;
        if (shadowProposal instanceof ChangeCorrectionProposal)
        {
            ChangeCorrectionProposal shadowChangeCorrection = (ChangeCorrectionProposal) shadowProposal;
            logger.fine("For change correction = " + shadowChangeCorrection.getDisplayString()
                    + ", shadowProposal.class() = " + shadowProposal.getClass());
            Change shadowChange = null;
            try
            {
                shadowChange = shadowChangeCorrection.getChange();
                Pair <Change, Squiggly []> result = applyChange(shadowChange);
                Change undo = result.getValue1();
                errors = result.getValue2();
                boolean success = applyUndo(undo);
                if (!success)
                {
                    logger.warning("Exception raised while trying to apply undo... Re-syncing the projects...");
                    logger.warning("change.getClass() = " + shadowChange.getClass());
                    syncProjects();
                }
            }
            catch (Exception e)
            {
                logger.log(
                        Level.SEVERE,
                        "Cannot get change for proposal due to internal exception: "
                                + shadowProposal.getDisplayString(), e);
                logger.severe("Re-syncing projects...");
                syncProjects();
            }
        }
        else if (shadowProposal instanceof LinkedNamesAssistProposal)
        {
            // This is Rename field ...
            logger.info("Proposal: " + shadowProposal.getDisplayString()
                    + " is not a ChangeCorrectionProposal. Proposal.class = " + shadowProposal.getClass());
        }
        else
            logger.warning("Proposal: " + shadowProposal.getDisplayString()
                    + " is not a ChangeCorrectionProposal. Proposal.class = " + shadowProposal.getClass());
        return errors;
    }

    private Pair <Change, Squiggly []> applyChange(Change shadowChange)
    {
        if (shadowChange == null)
            return null;
        Change undo = null;
        Squiggly [] errors = Squiggly.UNKNOWN;
        logger.finer("change.getClass() = " + shadowChange.getClass());
        logger.finer("change.getModifiedElement() = " + shadowChange.getModifiedElement());
        logger.finest("change.isEnabled() = " + shadowChange.isEnabled());
        try
        {
            /*
             * Problem: If I build the project before actually saving it, the markers are not generated correctly... The
             * changed files buffer should be saved! Weird but true...
             */
            logger.fine("Performing change...");
            undo = performChangeAndSave(shadowChange);
            logger.fine("Performed change...");
            buildShadowProject();
            errors = getShadowCEs();
            logger.fine("Number of compilation errors = " + errors.length);
        }
        catch (CoreException e)
        {
            logger.log(Level.SEVERE, "Cannot perform change!", e);
        }
        return new Pair <Change, Squiggly []>(undo, errors);
    }

    private boolean applyUndo(Change undo)
    {
        boolean result = true;
        try
        {
            logger.fine("Performing undo...");
            performChangeAndSave(undo);
            buildShadowProject();
        }
        catch (CoreException e)
        {
            // This should not happen if I check the compilation errors correctly and the projects
            // are in sync.
            logger.log(Level.SEVERE, "Cannot perform undo!", e);
            result = false;
        }
        return result;
    }

    public boolean isSynched()
    {
        // Both sub-calls are thread safe.
        return super.isSynched() && activationRecord_.isValid();
    }

    public void killAndJoin()
    {
        kill();
        logger.finer("Killed the calculator.");
        activationRecord_.activate();
        logger.finer("Notified the calculator.");
        try
        {
            join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        logger.finer("Joined the calculator.");
    }

    @Override
    public void projectModified()
    {
        logger.fine("Calculator is notified by the worker.");
        activationRecord_.activate();
    }

    @Override
    public void projectIsAboutToBeModified()
    {
        /* i.e., isWorking(), doing the speculative analysis. */
        // if (!isSynched())
        activationRecord_.invalidate();
        // Very important: never activate the activation record here!
        // If done, speculative analysis starts again with every key stroke, which is
        // something we don't want. It is guaranteed that projectModified() method
        // will be eventually called and we will be re-activated again.
        
        // invalidate current calculation.
        QuickFixDialogCoordinator.getCoordinator().clear();
    }

    @Override
    public void signalSpeculativeAnalysisRoundComplete()
    {
        speculativeAnalysisListenersLock_.lock();
        processRemoveList();
        for (SpeculativeAnalysisListener listener: speculativeAnalysisListeners_)
            listener.speculativeAnalysisRoundCompleted();
//        Thread.yield();
        speculativeAnalysisListenersLock_.unlock();
    }

    @Override
    public void signalSpeculativeAnalysisStart()
    {
        speculativeAnalysisListenersLock_.lock();
        processRemoveList();
        for (SpeculativeAnalysisListener listener: speculativeAnalysisListeners_)
            listener.speculativeAnalysisStarted();
        speculativeAnalysisListenersLock_.unlock();
    }

    @Override
    public void addListener(SpeculativeAnalysisListener listener)
    {
        speculativeAnalysisListenersLock_.lock();
        speculativeAnalysisListeners_.add(listener);
        speculativeAnalysisListenersLock_.unlock();
    }

    @Override
    public void removeListener(SpeculativeAnalysisListener listener)
    {
        Date currentTime = new Date();
        // Remove listener is only used when local compilation error is analyzed.
        timingLock_.lock();
        if (localSpeculationCompletionTime_ == null)
            localSpeculationCompletionTime_ = currentTime;
        timingLock_.unlock();
        speculativeAnalysisListenersLock_.lock();
        speculativeAnalysisListenersToRemove_.add(listener);
        speculativeAnalysisListenersLock_.unlock();
    }

    @Override
    public void signalSpeculativeAnalysisComplete()
    {
        Date currentTime = new Date();
        // set analysis completion time.
        timingLock_.lock();
        if (localSpeculationCompletionTime_ == null)
            localSpeculationCompletionTime_ = currentTime;
        analysisCompletionTime_ = currentTime;
        timingLock_.unlock();
        speculativeAnalysisListenersLock_.lock();
        processRemoveList();
        for (SpeculativeAnalysisListener listener: speculativeAnalysisListeners_)
            listener.speculativeAnalysisCompleted();
        speculativeAnalysisListenersLock_.unlock();
    }

    private void processRemoveList()
    {
        for (SpeculativeAnalysisListener listener: speculativeAnalysisListenersToRemove_)
            speculativeAnalysisListeners_.remove(listener);
    }

    private class CompilationErrorComparator implements Comparator <Squiggly>
    {
        private final String currentFilePath_;
        private final int cursorOffset_;
        
        private CompilationErrorComparator()
        {
            cursorOffset_ = currentCursorOffset_;
            IFile currentFile = currentFile_;
            if (currentFile != null)
                currentFilePath_ = currentFile.getProjectRelativePath().toString();
            else
                currentFilePath_ = null;
        }

        @Override
        public int compare(Squiggly error1, Squiggly error2)
        {
            String path1 = error1.getResource().getProjectRelativePath().toString();
            String path2 = error2.getResource().getProjectRelativePath().toString();
            if (currentFilePath_ != null)
            {
                if (currentFilePath_.equals(path1))
                {
                    if (currentFilePath_.equals(path2))
                    {
                        // Both compilation errors are on the current editor file. We need to compare the offsets.
                        int offset1 = error1.getLocation().getOffset();
                        int offset2 = error2.getLocation().getOffset();
                        
                        int diff1 = Math.abs(cursorOffset_ - offset1);
                        int diff2 = Math.abs(cursorOffset_ - offset2);
                        
                        return diff1 - diff2;
                    }
                    // The first compilation error is on the current editor file whereas the second one is not.
                    // So, we process the first one earlier.
                    else
                        return -1;
                }
                // The second compilation error is on the current editor file whereas the first one is not.
                // So, we process the second one earlier.
                else if (currentFilePath_.equals(path2))
                    return 1;
                else
                    return 0;
            }
            // There is no selected file in the Eclipse editor, so all compilation errors are equal.
            else
                return 0;
        }
    }

    /*********************
     * GETTERS & SETTERS *
     ********************/
    /**
     * <p>
     * Sets the current file that is opened in Eclipse editor.
     * </p>
     * Should only be set by Eclipse UI thread using {@link SynchronizerPartListener}
     * 
     * @param file The file that is currently selected in Eclipse editor.
     */
    public void setCurrentFile(IFile file)
    {
        currentFile_ = file;
    }
    
    public void setCursorOffset(int offset)
    {
        currentCursorOffset_ = offset;
    }

    /**
     * Returns the number of errors in the current state of the shadow project. <br>
     * Used when constructing {@link AugmentedCompletionProposal}s.
     * 
     * @return The number of errors in the current state of the shadow project.
     */
    private synchronized int getNumberOfErrors()
    {
        if (shadowCompilationErrors_ == null)
            return 0;
        return shadowCompilationErrors_.length;
    }

    /**
     * <p>
     * Returns a <strong>copy</strong> of the currently calculated proposals.
     * </p>
     * This method is protected by {@link #speculativeProposalsLock_}. <br>
     * This method is currently called by the following threads:
     * <ol>
     * <li>Eclipse UI thread, when it is trying to record the speculative analysis results.</li>
     * <li>SpeculationGrabber thread, when trying to figure out if the selected problem location has been calculated or
     * not.</li>
     * </ol>
     * 
     * @return A <strong>copy</strong> of the currently calculated proposals.
     */
    public Map <Squiggly, AugmentedCompletionProposal []> getSpeculativeProposalsMap()
    {
        if (activationRecord_.isInvalid())
            return new HashMap <Squiggly, AugmentedCompletionProposal[]>();
        
        Map <Squiggly, Squiggly> ceResolutionMap;
        Map <Squiggly, AugmentedCompletionProposal []> speculativeProposalMap;
        synchronized(this)
        {
            ceResolutionMap = shadowCompilationErrorResolutionMap_;
            speculativeProposalMap = speculativeProposalsMap_;
        }
        
        Map <Squiggly, AugmentedCompletionProposal []> result;
        if (ceResolutionMap.size() != 0)
        {
            result = new HashMap <Squiggly, AugmentedCompletionProposal[]>();
            for (Squiggly shadowCompilationError: speculativeProposalMap.keySet())
            {
                Squiggly currentShadowCE = ceResolutionMap.get(shadowCompilationError);
                result.put(currentShadowCE, speculativeProposalMap.get(shadowCompilationError));
                for (AugmentedCompletionProposal speculativeProposal: speculativeProposalMap.get(shadowCompilationError))
                    speculativeProposal.updateCompilationError(currentShadowCE);
            }
        }
        else
            result = new HashMap <Squiggly, AugmentedCompletionProposal []>(speculativeProposalMap);
        return result;
    }

    /**
     * <p>
     * Adds the given key-value pair to the currently calculated proposals map.
     * </p>
     * This method is protected by {@link #speculativeProposalsLock_}. <br>
     * This method can only be called by the speculator (this) thread.
     * 
     * @param shadowCompilationError Currently calculated problem location.
     * @param shadowCalculatedProposals Calculated error information related to this problem location.
     */
    private synchronized void addToSpeculationProposalsMap(Squiggly shadowCompilationError,
            AugmentedCompletionProposal [] shadowCalculatedProposals)
    {
        speculativeProposalsMap_.put(shadowCompilationError, shadowCalculatedProposals);
    }

    /**
     * <p>
     * Clears the currently calculated proposals map. <br>
     * This is done by the calculator thread at the beginning of each calculation.
     * </p>
     * This method is protected by {@link #speculativeProposalsLock_}. <br>
     */
    private synchronized void clearSpeculativeProposalsMap()
    {
        speculativeProposalsMap_.clear();
    }

    /**
     * <p>
     * Returns a copy of the currently traversed proposals offered by Eclipse.
     * </p>
     * This method is protected by {@link #shadowProposalsLock_}. <br>
     * This method should be called only by the SpeculationGrabber thread in order to map the received problem locations
     * to the problem locations in the shadow project.
     * 
     * @return A copy of the currently traversed proposals offered by Eclipse.
     */
    public Map <Squiggly, IJavaCompletionProposal []> getProposalsMap()
    {
        if (activationRecord_.isInvalid())
            return new HashMap <Squiggly, IJavaCompletionProposal[]>();
            
        Map <Squiggly, Squiggly> ceResolutionMap;
        Map <Squiggly, IJavaCompletionProposal []> shadowProposalMap;
        synchronized(this)
        {
            ceResolutionMap = shadowCompilationErrorResolutionMap_;
            shadowProposalMap = shadowProposalsMap_;
        }
        
        Map <Squiggly, IJavaCompletionProposal []> result;
        if (ceResolutionMap.size() != 0)
        {
            result = new HashMap <Squiggly, IJavaCompletionProposal[]>();
            for (Squiggly shadowCompilationError: shadowProposalMap.keySet())
            {
                Squiggly currentShadowCE = ceResolutionMap.get(shadowCompilationError);
                result.put(currentShadowCE, shadowProposalMap.get(shadowCompilationError));
            }
        }
        else
            result = new HashMap <Squiggly, IJavaCompletionProposal []>(shadowProposalMap);
        return result;
    }

    /**
     * <p>
     * Adds the give key-value pair to the proposals map.
     * </p>
     * This method is protected by {@link #shadowProposalsLock_}. <br>
     * This method should only be called by the calculator thread (this).
     * 
     * @param location The problem location that will be processed.
     * @param proposals The proposals offered by Eclipse for that problem location.
     */
    private synchronized void addToShadowProposalsMap(Squiggly compilationError, IJavaCompletionProposal [] proposals)
    {
        shadowProposalsMap_.put(compilationError, proposals);
    }

    /**
     * <p>
     * Clears the proposals map. <br>
     * Done at the beginning of each analysis.
     * </p>
     * This method is protected by {@link #shadowProposalsLock_}. <br>
     * This method should only be called by the calculator thread (this).
     */
    private synchronized void clearProposalsMap()
    {
        shadowProposalsMap_.clear();
    }
    
    public void quickFixInvoked()
    {
        EclipseUIUtility.saveAllEditors(false);
        buildOriginalProject();
        Squiggly [] originalCompilationErrors = getOriginalCEs();
        QuickFixDialogCoordinator.getCoordinator().setOriginalCompilationErrors(originalCompilationErrors);  
    }

    public Date getAnalysisCompletionTime()
    {
        Date result;
        timingLock_.lock();
        result = analysisCompletionTime_;
        timingLock_.unlock();
        return result;
    }

    public Date getLocalSpeculationCompletionTime()
    {
        Date result;
        timingLock_.lock();
        result = localSpeculationCompletionTime_;
        timingLock_.unlock();
        return result;
    }

    // Code written for profiling.
    private void buildShadowProject()
    {
        BuilderUtility.build(shadowProject_);
    }

    private void buildOriginalProject()
    {
        BuilderUtility.build(synchronizer_.getProject());
    }

    private Squiggly [] getShadowCEs()
    {
        return BuilderUtility.calculateCompilationErrors(shadowProject_);
    }
    
    private Squiggly [] getOriginalCEs()
    {
        return BuilderUtility.calculateCompilationErrors(synchronizer_.getProject());
    }

    private IJavaCompletionProposal [] computeShadowProposals(Squiggly shadowCE) throws Exception
    {
        return QuickFixUtility.computeQuickFix(shadowCE);
    }

    private Change performChangeAndSave(Change shadowChange) throws CoreException
    {
        return SpeculationUtility.performChangeAndSave(shadowChange);
    }

    private void syncProjects()
    {
        synchronizer_.syncProjects();
    }

    private boolean testSynchronization()
    {
        return synchronizer_.testSynchronization();
    }
}
