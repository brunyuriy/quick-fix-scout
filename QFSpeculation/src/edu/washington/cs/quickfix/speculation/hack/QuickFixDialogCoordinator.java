package edu.washington.cs.quickfix.speculation.hack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import edu.washington.cs.quickfix.speculation.Speculator;
import edu.washington.cs.quickfix.speculation.calc.SpeculationCalculator;
import edu.washington.cs.quickfix.speculation.calc.model.AugmentedCompletionProposal;
import edu.washington.cs.quickfix.speculation.model.SpeculationUtility;
import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.util.eclipse.QuickFixUtility;
import edu.washington.cs.util.eclipse.model.Squiggly;

public class QuickFixDialogCoordinator
{
    private static final QuickFixDialogCoordinator instance_ = new QuickFixDialogCoordinator();

    private static final Logger logger = Logger.getLogger(QuickFixDialogCoordinator.class.getName());
    static
    {
        logger.setLevel(Level.FINE);
        // logger.setLevel(Level.FINER);
        // logger.setLevel(Level.FINEST);
    }
    
    // Local proposals and global best proposals are calculated locally.
    private AugmentedCompletionProposal [] localProposals_ = null;
    private ArrayList <AugmentedCompletionProposal> globalBestProposals_;

    // These are global and needs to be protected by lock_.
    private AugmentedCompletionProposal [] calculatedProposals_ = null;
    private IJavaCompletionProposal [] eclipseProposals_ = null;
    // Compilation error indicates the locations of the selected quick fix. It can be null
    // if the user invokes quick fix where no compilation error is present.
    // or sometimes Eclipse generates multiple compilation errors on the same line (or same error location),
    // so it can be more than one.
    private Squiggly [] compilationErrors_ = null;
    // best proposals also include the local bests. BP = GBP + LBP.
    private ArrayList <AugmentedCompletionProposal> bestProposals_;
    private Squiggly [] originalCompilationErrors_;
    private final Object lock_ = new Object();
    
    private final CompletionProposalPopupCoordinator completionProposalPopupCoordinator_;
    private final HoverDialogCoordinator hoverDialogCoordinator_;

    // singleton
    private QuickFixDialogCoordinator()
    {
        bestProposals_ = new ArrayList <AugmentedCompletionProposal>();
        globalBestProposals_ = new ArrayList <AugmentedCompletionProposal>();
        completionProposalPopupCoordinator_ = CompletionProposalPopupCoordinator.getCoordinator();
        hoverDialogCoordinator_ = HoverDialogCoordinator.getCoordinator();
    }
    
    private AugmentedCompletionProposal [] getLocalProposals()
    {
        return localProposals_;
    }
    
    public void popupClosed()
    {
        completionProposalPopupCoordinator_.popupClosed();
        hoverDialogCoordinator_.popupClosed();
    }

    public void setBestProposals(ArrayList <AugmentedCompletionProposal> bestProposals)
    {
        /*
         * For some reason if I synchronized the whole calculation, it deadlocks Eclipse UI thread.
         */
        synchronized(lock_)
        {
            bestProposals_ = bestProposals;
        }
        logger.finer("Setting best proposals.");
        for (AugmentedCompletionProposal proposal: bestProposals)
            logger.finest(proposal.toString());
        if (getLocalProposals() != null)
            update();
        else
            logger.finer("All proposals are null, not updating the UI.");
    }
    
    public static synchronized QuickFixDialogCoordinator getCoordinator()
    {
        return instance_;
    }
    
    public void setOriginalCompilationErrors(Squiggly [] errors)
    {
        synchronized(lock_)
        {
            originalCompilationErrors_ = errors;
        }
    }

    public void clearBestProposals()
    {
        synchronized(lock_)
        {
            logger.finer("Clearing best proposals.");
            bestProposals_.clear(); 
        }
    }
    
    private synchronized boolean shouldWait()
    {
        boolean cppShouldWait = completionProposalPopupCoordinator_.shouldWait();
        logger.fine("cpp should wait? " + cppShouldWait);
        boolean hdShouldWait = hoverDialogCoordinator_.shouldWait();
        logger.fine("cpp should wait? " + cppShouldWait + ", hd should wait? " + hdShouldWait);
        return  cppShouldWait && hdShouldWait;
    }
    
    public void updateWithSpeculationResults(IJavaCompletionProposal [] eclipseProposals,
            AugmentedCompletionProposal [] calculatedProposals, Squiggly [] compilationErrors)
    {
        logger.finer("Updating proposal table. EclipseProposals.length = "
                + (eclipseProposals == null ? 0 : eclipseProposals.length) + ", augmentedProposals.length = "
                + (calculatedProposals == null ? 0 : calculatedProposals.length));
        while (shouldWait())
        {
            try
            {
                synchronized (this)
                {
                    wait();
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        synchronized (lock_)
        {
            eclipseProposals_ = eclipseProposals;
            calculatedProposals_ = calculatedProposals;
            // Compilation errors can be none if the user invokes a quick fix where there is no compilation error
            // is present. At this moment only the global best proposals should be shown (if any).
            compilationErrors_ = compilationErrors;
        }
        logger.finer("Received compilation errors:");
        for (Squiggly compilationError: compilationErrors)
            logger.finer(compilationError.toString());
        update();
    }
    
    private void update()
    {
        logger.fine("Updating dialogs...");
        constructLocalProposalsInternally();
        logger.fine("Proposals are created locally.");
        completionProposalPopupCoordinator_.updatePopup(globalBestProposals_, localProposals_);
        logger.fine("Quick Fix Dialog is updated.");
        hoverDialogCoordinator_.updateHover(globalBestProposals_, localProposals_);
        logger.fine("Quick Fix Hover is updated.");
    }

    private void calculateGBPs()
    {
        AugmentedCompletionProposal [] calculatedProposals;
        ArrayList <AugmentedCompletionProposal> bestProposals;
        Squiggly [] compilationErrors;
        synchronized(lock_)
        {
            calculatedProposals = calculatedProposals_;
            compilationErrors = compilationErrors_;
            bestProposals = bestProposals_;
            globalBestProposals_.clear();
        }
        
        ArrayList <AugmentedCompletionProposal> gbps = new ArrayList <AugmentedCompletionProposal>();
        HashSet <String> currentDisplayStrings = new HashSet <String>();
        for (AugmentedCompletionProposal proposal: calculatedProposals)
            currentDisplayStrings.add(proposal.getDisplayString());
        for (AugmentedCompletionProposal proposal: bestProposals)
        {
            if (!currentDisplayStrings.contains(proposal.getDisplayString()))
            {
                boolean fixesAtLeastOne = false;
                for (Squiggly compilationError: compilationErrors)
                {
                    if (proposal.canFix(compilationError))
                        fixesAtLeastOne = true;
                }
                if (fixesAtLeastOne)
                    gbps.add(proposal);
            }
        }
        
        synchronized(lock_)
        {
            globalBestProposals_ = gbps;
        }
    }

    // TODO proposals are completely contained by augmented proposals. Not needed.
    // This todo does not make sense, I wonder if I tried to mean eclipseProposals are contained by
    // augmentedProposals?
    private void constructLocalProposalsInternally()
    {
        AugmentedCompletionProposal [] calculatedProposals;
        IJavaCompletionProposal [] eclipseProposals;
        synchronized(lock_)
        {
            calculatedProposals = calculatedProposals_;
            eclipseProposals = eclipseProposals_;
            localProposals_ = null;
        }
        
        calculateGBPs();
        ArrayList <AugmentedCompletionProposal> localProposals = new ArrayList <AugmentedCompletionProposal>();
        for (int a = 0; a < calculatedProposals.length; a++)
        {
            AugmentedCompletionProposal calculatedProposal = calculatedProposals[a];
            Squiggly [] errorAfter = calculatedProposal.getRemainingErrors();
            int errorBefore = calculatedProposal.getErrorBefore();
            ICompletionProposal eclipseProposal = (SpeculationCalculator.TEST_TRANSFORMATION) ? calculatedProposal
                    .getProposal() : null;
            if (eclipseProposal == null)
            {
                for (ICompletionProposal eclipseProp: eclipseProposals)
                {
                    if (eclipseProp.getDisplayString().equals(calculatedProposal.getDisplayString()))
                        eclipseProposal = eclipseProp;
                }
            }
            if (eclipseProposal == null)
            {
                String ls = System.getProperty("line.separator");
                StringBuilder log = new StringBuilder();
                log.append("Couldn't find the corresponding eclipse proposal for calculated proposal: "
                        + calculatedProposal);
                log.append("Eclipse proposals: " + ls);
                for (ICompletionProposal proposal: eclipseProposals)
                    log.append(proposal.getDisplayString() + ls);
                logger.warning(log.toString());
            }
            else
                // For eclipse proposals, it is okay to pass 'null' as compilation error since we are only using
                // location for
                // best proposals.
                localProposals.add(new AugmentedCompletionProposal(eclipseProposal, null, errorAfter, errorBefore));
        }
        Collections.sort(localProposals);
        logger.finest("Constructed the following modified strings for the invoked quick fix:");
        for (AugmentedCompletionProposal proposal: localProposals)
            logger.finest(proposal.toString());
        synchronized (lock_)
        {
            localProposals_ = localProposals.toArray(new AugmentedCompletionProposal [localProposals.size()]);
        }
    }
    
    // Since the resolution from shadow proposals to original proposals are no longer done at the end of
    // speculative analysis, we need to resolve the remaining global best proposals the momment quick fix
    // dialog is created.
    void resolve(AugmentedCompletionProposal globalBestProposal)
    {
        Squiggly [] originalErrors = null;
        synchronized (lock_)
        {
            originalErrors = originalCompilationErrors_;
        }
        Squiggly shadowCompilationError = globalBestProposal.getRecentCompilationError();
        IProblemLocation shadowLocation = shadowCompilationError.getLocation();
        Squiggly originalCompilationError = null;
        for (Squiggly originalError: originalErrors)
        {
            IProblemLocation originalLocation = originalError.getLocation();
            if (SpeculationUtility.sameProblemLocationContent(shadowLocation, originalLocation))
                originalCompilationError = originalError;
        }
        if (originalCompilationError == null)
            throw new GBPResolutionException("Cannot resolve the original compilation error for shadow proposal = "
                    + globalBestProposal);
        try
        {
            IJavaCompletionProposal [] originalProposals = computeOriginalProposals(originalCompilationError);
            IJavaCompletionProposal originalProposal = findOriginalProposal(originalProposals,
                    globalBestProposal.getProposal());
            globalBestProposal.setProposal(originalProposal);
        }
        catch (Exception e)
        {
            throw new GBPResolutionException("Cannot resolve original proposal for shadow proposal = "
                    + globalBestProposal);
        }
    }
    
    ICompletionProposal [] getNonProcessedProposals(ICompletionProposal [] existingProposals)
    {
        ArrayList <ICompletionProposal> result = new ArrayList <ICompletionProposal>();
        for (ICompletionProposal proposal: existingProposals)
        {
            String displayInformation = proposal.getDisplayString();
            boolean found = false;
            for (AugmentedCompletionProposal prop: localProposals_)
            {
                if (prop != null && prop.getDisplayString().equals(displayInformation))
                    found = true;
            }
            if (!found)
                result.add(proposal);
        }
        return result.toArray(new ICompletionProposal [result.size()]);
    }
    
    private IJavaCompletionProposal findOriginalProposal(IJavaCompletionProposal [] originalProposals,
            ICompletionProposal shadowProposal) throws Exception
    {
        for (IJavaCompletionProposal proposal: originalProposals)
        {
            if (proposal.getDisplayString().equals(shadowProposal.getDisplayString()))
                return proposal;
        }
        logger.info("Cannot find the corresponding original proposal for = " + shadowProposal.getDisplayString());
        int counter = 0;
        for (IJavaCompletionProposal originalProposal: originalProposals)
        {
            counter++;
            logger.info("\tOriginal proposal #" + counter + " = " + originalProposal.getDisplayString());
        }
        throw new Exception();
    }

    private IJavaCompletionProposal [] computeOriginalProposals(Squiggly originalCE) throws Exception
    {
        return QuickFixUtility.computeQuickFix(originalCE);
    }
    
    public void proposalSelected(ICompletionProposal proposal)
    {
        /*
         * If the speculator plug-in disabled, then current synchronizer returns null and we have to check this to
         * prevent NullPointerException. If thrown, it prevents Eclipse to apply the proposal selected to the project
         * when the speculator plug-in is disabled. Bug fix for v.0.6.1
         */
        ProjectSynchronizer synchronizer = Speculator.getSpeculator().getCurrentSynchronizer();
        if (synchronizer != null)
            synchronizer.getTaskWorker().bypassTypingSessionCheck();
        clear();
        /*
         * TODO Somehow invalidate quick fix calculator at this point. Otherwise if user clicks the quick fix too fast
         * (i.e., before the next calculation starts), grabber reads the old values and thinks that it has the results!
         */
    }

    public void clear()
    {
        synchronized(lock_)
        {
            localProposals_ = null;
            calculatedProposals_ = null;
            eclipseProposals_ = null;
        }
    }

    public void awake()
    {
        synchronized(this)
        {
            notifyAll();
        }
    }
}
