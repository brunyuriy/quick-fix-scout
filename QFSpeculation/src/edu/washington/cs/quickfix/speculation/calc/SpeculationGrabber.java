package edu.washington.cs.quickfix.speculation.calc;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import edu.washington.cs.quickfix.speculation.Speculator;
import edu.washington.cs.quickfix.speculation.calc.model.AugmentedCompletionProposal;
import edu.washington.cs.quickfix.speculation.calc.model.QFPopupListener;
import edu.washington.cs.quickfix.speculation.calc.model.SpeculativeAnalysisListener;
import edu.washington.cs.quickfix.speculation.gui.SpeculationPreferencePage;
import edu.washington.cs.quickfix.speculation.hack.QuickFixDialogCoordinator;
import edu.washington.cs.quickfix.speculation.model.SpeculationUtility;
import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.util.eclipse.QuickFixUtility;
import edu.washington.cs.util.eclipse.model.Squiggly;

public class SpeculationGrabber extends Thread implements SpeculativeAnalysisListener, QFPopupListener
{
    private final IProblemLocation [] locations_;
    private final IInvocationContext context_;
    private IJavaCompletionProposal [] eclipseProposals_;
    private ArrayList <Squiggly> cachedCompilationErrors_;
    private final static Logger logger = Logger.getLogger(SpeculationGrabber.class.getName());
    static
    {
        //@formatter:off
        // FINE => Debug pre-caching function.
        // FINER => See what is passed to the coordinator.
        //@formatter:on
        logger.setLevel(Level.INFO);
    }
    private final SpeculationCalculator calculator_;
    private boolean notInitialized_;
    
    public SpeculationGrabber(IInvocationContext context, IProblemLocation [] locations)
    {
        // The problem locations can be zero if the user invokes quick fix where no compilation errors
        // are present. Sometimes I get two problem locations. What does that mean? I guess they mean that
        // there are multiple compilation errors (problems) on the same line.
        locations_ = locations;
        context_ = context;
        calculator_ = Speculator.getSpeculator().getCurrentCalculator();
        if (calculator_ != null)
        {
            notInitialized_ = false;
            cachedCompilationErrors_ = null;
        }
        else
            notInitialized_ = true;
    }

    /*
     * FIXME What happens if the user multi-clicks to quick fix dialog. Need to somehow validate or invalidate the older
     * the threads...
     */
    public void run()
    {
        if (notInitialized_)
            return;
        if (!SpeculationPreferencePage.getInstance().isAugmentationActivated())
            return;
        ICompilationUnit unit = context_.getCompilationUnit();
        try
        {
            IResource resource = unit.getCorrespondingResource();
            IProject project = resource.getProject();
            /*
             * This means that the currently selected proposals and quick fix section is related to a shadow project and
             * we don't care or observe what happens to the shadow project.
             */
            if (ProjectSynchronizer.isShadowProject(project))
                return;
        }
        catch (JavaModelException e)
        {
            // This should not happen.
            logger.log(Level.SEVERE,
                    "Cannot get the corresponding resource for compilation unit = " + unit.getElementName(), e);
        }
        eclipseProposals_ = QuickFixUtility.calculateCompletionProposals(context_, locations_);
        
        if (!calculator_.isSynched())
        {
            QuickFixDialogCoordinator.getCoordinator().addQFPopupListener(this);
            calculator_.addListener(this);
        }
        attemptToRetrieveResults();
    }

    private boolean preCacheLocations()
    {
        ArrayList <Squiggly> result = new ArrayList <Squiggly>();
        Map <Squiggly, IJavaCompletionProposal []> currentMap = calculator_.getProposalsMap();
        for (Squiggly compilationError: currentMap.keySet())
        {
            for (IProblemLocation loc: locations_)
            {
                if (SpeculationUtility.sameProblemLocationContent(compilationError.getLocation(), loc))
                    result.add(compilationError);
            }
        }
        // for (CompilationError compilationError: result)
        // logger.finer(compilationError.toString());
        if (result.size() == locations_.length)
        {
            // Caching succeed...
            logger.info("Successfully cached " + result.size() + " locations.");
            cachedCompilationErrors_ = result;
            return true;
        }
        else
        {
            String ls = System.getProperty("line.separator");
            StringBuilder extraDebug = new StringBuilder();
            extraDebug.append("Locations:" + ls);
            for (int a = 0; a < locations_.length; a++)
            {
                extraDebug.append("Problem Location # " + (a + 1) + ls);
                extraDebug.append(locations_[a].toString() + ls);
            }
            extraDebug.append("Successfully cached: " + ls);
            for (int a = 0; a < result.size(); a++)
            {
                extraDebug.append("Problem Location # " + (a + 1) + ls);
                extraDebug.append(result.get(a).getLocation().toString() + ls);
            }
            logger.fine("Precaching problem locations failed. # of locations = " + locations_.length
                    + ", cached # of locations = " + result.size() + ls + extraDebug);
            return false;
        }
    }

    // @formatter:off
    /*
     * Works pretty well. However, for some reason I get the offered proposals less then the actual ones.
     * This might be related to QFSpeculationCalculator.
     */
    // @formatter:on
    private void attemptToRetrieveResults()
    {
        if (cachedCompilationErrors_ == null)
        {
            boolean success = preCacheLocations();
            if (!success)
                return;
        }
        logger.fine("Attempting quick fix calculation.");
        ArrayList <AugmentedCompletionProposal> calculatedProposals;
        calculatedProposals = new ArrayList <AugmentedCompletionProposal>();
        Map <Squiggly, IJavaCompletionProposal []> problemLocationToProposalMap = calculator_.getProposalsMap();
        Map <Squiggly, AugmentedCompletionProposal []> problemLocationToCompilationErrorMap = calculator_
                .getSpeculativeProposalsMap();
        for (Squiggly compilationError: cachedCompilationErrors_)
        {
            IJavaCompletionProposal [] proposals = problemLocationToProposalMap.get(compilationError);
            AugmentedCompletionProposal [] augmentedProposals = problemLocationToCompilationErrorMap
                    .get(compilationError);
            if (augmentedProposals == null)
            {
                /*
                 * If resultMap does not contain the searched location, we should wait for the calculation to advance.
                 */
                logger.info("Speculative analysis for this location is not completed yet, not updating the UI.");
                return;
            }
            /*
             * Add the elements one by one to make sure that the ordering (proposal - # of compilation errors) is not
             * broken.
             */
            for (int a = 0; a < proposals.length; a++)
            {
                // Sometimes due to multiple error locations, the same proposal can be generated from different error
                // locations. Here, we filter them and make sure that they are shown as one proposal in the UI.
                if (!doesInclude(calculatedProposals, augmentedProposals[a]))
                    calculatedProposals.add(augmentedProposals[a]);
            }
        }
        calculator_.removeListener(this);
        logger.info("For the clicked quick fix, there are: " + calculatedProposals.size()
                + " proposals calculated in advance.");
        for (int a = 0; a < calculatedProposals.size(); a++)
            logger.finer((a + 1) + "-) " + calculatedProposals.get(a).getDisplayString() + " will result with "
                    + calculatedProposals.get(a).getRemainingErrors().length + " compilation errors.");
        QuickFixDialogCoordinator.getCoordinator().updateWithSpeculationResults(eclipseProposals_,
                calculatedProposals.toArray(new AugmentedCompletionProposal [calculatedProposals.size()]),
                cachedCompilationErrors_.toArray(new Squiggly [cachedCompilationErrors_.size()]));
    }

    private boolean doesInclude(ArrayList <AugmentedCompletionProposal> calculatedProposals,
            AugmentedCompletionProposal augmentedCompletionProposal)
    {
        for (AugmentedCompletionProposal calculatedProposal: calculatedProposals)
        {
            if (calculatedProposal.getProposal().getDisplayString()
                    .equals(augmentedCompletionProposal.getProposal().getDisplayString()))
                return true;
        }
        return false;
    }
    
    private void retrieveResultsConcurrently()
    {
        new Thread()
        {
            public void run()
            {
                attemptToRetrieveResults();
            }
        }.start();
    }
    
    @Override
    public void speculativeAnalysisRoundCompleted()
    {
        retrieveResultsConcurrently();
//        attemptToRetrieveResults();
    }

    @Override
    public void speculativeAnalysisStarted()
    {
        preCacheLocations();
    }

    @Override
    public void speculativeAnalysisCompleted()
    {
        retrieveResultsConcurrently();
//        attemptToRetrieveResults();
    }

    public void popupClosed()
    {
        calculator_.removeListener(this);
    }
}
