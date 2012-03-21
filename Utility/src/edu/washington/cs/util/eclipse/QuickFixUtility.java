package edu.washington.cs.util.eclipse;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.QuickFixProcessor;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import edu.washington.cs.util.eclipse.model.Squiggly;

/**
 * This utility class provides static helper methods for quick fix related issues. <br>
 * Currently offered functionality is:
 * <ul>
 * <li>Computing the offered quick fixes for a given project.</li>
 * <li>Computing the proposals offered for a particular quick fix (represented by the context and problem locations).</li>
 * </ul>
 * 
 * @author Kivanc Muslu
 */
@SuppressWarnings("restriction")
public class QuickFixUtility
{
    /** Logger for debugging. */
    private static final Logger logger = Logger.getLogger(QuickFixUtility.class.getName());
    private static QuickFixProcessor qfProcessor_ = new QuickFixProcessor();
    static
    {
        logger.setLevel(Level.INFO);
    }

    /**
     * This class cannot be instantiated.
     */
    private QuickFixUtility()
    {}
    
    public static class EclipseProposalSorter implements Comparator<IJavaCompletionProposal>
    {
        @Override
        public int compare(IJavaCompletionProposal proposal1, IJavaCompletionProposal proposal2)
        {
            int difference = proposal2.getRelevance() - proposal1.getRelevance();
            if (difference == 0)
                return proposal1.getDisplayString().compareTo(proposal2.getDisplayString());
            return difference;
        }
    }

    /**************
     * PUBLIC API *
     *************/
    /**
     * Computes the possible quick fixes that are applicable to the given markers and problem locations
     * {@link IQuickFixProcessor}.
     * 
     * @param markers Java problem markers that generated the problem locations.
     * @param locations Problem locations that will generate the quick fixes.
     * @return A mapping from each problem location to the array of proposals that are available for that problem
     *         location. <br>
     * <br>
     *         Problem locations represent each of the available quick fix dialog on the left of the screen and each
     *         proposal in the array represents the different kind of selections you can do for that particular location
     *         (i.e., different quick fixes).
     */
    public static Map <Squiggly, IJavaCompletionProposal []> computeQuickFixes(Squiggly [] compilationErrors) throws Exception
    {
        Map <Squiggly, IJavaCompletionProposal []> result = new HashMap <Squiggly, IJavaCompletionProposal []>();
        for(Squiggly compilationError: compilationErrors)
        {
            logger.fine("Processing marker = " + compilationError.getMarker() + " for quick fix calculation");
            logger.fine("Corresponding compilation unit for marker = " + compilationError.getCompilationUnit().getResource().getProjectRelativePath());
            IProblemLocation location = compilationError.getLocation();
            if (location == null)
            {
                logger.warning("Got a null location for a non-null marker, recreating the shadow project...");
                throw new Exception();
            }
            else
            {
                logger.fine("Corresponding location for marker = " + location);
                IJavaCompletionProposal [] proposals = getCompletionProposalsFrom(compilationError);
                result.put(compilationError, proposals);
            }
        }
        return result;
    }
    
    public static IJavaCompletionProposal [] computeQuickFix(Squiggly compilationError)
            throws Exception
    {
        logger.fine("Processing marker = " + compilationError.getMarker() + " for quick fix calculation");
        IProblemLocation location = compilationError.getLocation();
        if (location == null)
        {
            logger.warning("Got a null location for a non-null marker, recreating the shadow project...");
            throw new Exception();
        }
        else
        {
            logger.fine("Corresponding location for marker = " + location);
            IJavaCompletionProposal [] proposals = getCompletionProposalsFrom(compilationError);
            return proposals;
        }
    }

    /**
     * Calculates and returns the {@link ICompletionProposal}s that are offered for the given context and problem
     * locations. <br>
     * Uses {@link IQuickFixProcessor} to retrieve the information.
     * 
     * @param context Context that is analyzed.
     * @param locations Problem locations that is analyzed.
     * @return The {@link ICompletionProposal}s that are offered for the given context and problem locations.
     * @see IQuickFixProcessor#getCorrections(IInvocationContext, IProblemLocation[])
     */
    public static IJavaCompletionProposal [] calculateCompletionProposals(IInvocationContext context,
            IProblemLocation [] locations)
    {
        try
        {
            IJavaCompletionProposal [] proposals = qfProcessor_.getCorrections(context, locations);
            return proposals;
        }
        catch (CoreException e)
        {
            logger.log(Level.SEVERE, "Cannot calculate the proposals.", e);
        }
        catch (Exception e)
        {
            /*
             * FIXME This exception seems to happen because Display is 'null' at the moment a change correction is being
             * calculated. (CorrectPackageDeclarationProposal) This also happens for other proposals (it is related with
             * the image on the left of the proposal). I think this happens before I try to calculate quick fixes too
             * quickly. It seems that the first time it fails, Eclipse creates a display.
             */
            logger.warning("Unknown exception happened while trying to get proposals from QuickFixProcessor.");
            logger.warning("Yielding the thread...");
            Thread.yield();
            return calculateCompletionProposals(context, locations);
        }
        return new IJavaCompletionProposal [0];
    }

    /***************
     * PRIVATE API *
     **************/
    /**
     * Returns the {@link ICompletionProposal}s that are offered for the given location in the compilation unit. <br>
     * This method is a wrapper for {@link #calculateCompletionProposals(IInvocationContext, IProblemLocation[])}.
     * 
     * @param location Location that is analyzed.
     * @param cu Compilation unit that contains the location.
     * @return The {@link ICompletionProposal}s that are offered for the given location in the compilation unit.
     */
    private static IJavaCompletionProposal [] getCompletionProposalsFrom(Squiggly compilationError)
    {
//        ArrayList <IJavaCompletionProposal> proposals = new ArrayList <IJavaCompletionProposal>();
//        IProblemLocation location = compilationError.getLocation();
//        IInvocationContext context = new AssistContext(compilationError.getCompilationUnit(), location.getOffset(), location.getLength());
//        IProblemLocation [] locations = new IProblemLocation [1];
//        locations[0] = location;
//        JavaCorrectionProcessor.collectCorrections(context, locations, proposals);
//        return proposals.toArray(new IJavaCompletionProposal[proposals.size()]);
        
        IProblemLocation location = compilationError.getLocation();
        IInvocationContext context = new AssistContext(compilationError.getCompilationUnit(), location.getOffset(), location.getLength());
        IProblemLocation [] locations = new IProblemLocation [1];
        locations[0] = location;
        return calculateCompletionProposals(context, locations);
    }
}
