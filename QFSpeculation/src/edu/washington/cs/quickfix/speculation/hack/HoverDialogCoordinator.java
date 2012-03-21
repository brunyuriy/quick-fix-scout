package edu.washington.cs.quickfix.speculation.hack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover.AnnotationInformationControl;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.widgets.Display;

import edu.washington.cs.quickfix.speculation.calc.model.AugmentedCompletionProposal;

@SuppressWarnings("restriction")
public class HoverDialogCoordinator
{
    private static final Logger logger = Logger.getLogger(HoverDialogCoordinator.class.getName());
    private static final HoverDialogCoordinator instance_ = new HoverDialogCoordinator();
    
    private AnnotationInformationControl hoverDialog_ = null;
    private volatile ICompletionProposal [] eclipseProposals_;
    
    private final Object hoverLock_ = new Object();
    
    public void hoverCreated(AnnotationInformationControl hoverController)
    {
        hoverDialog_ = hoverController;
        QuickFixDialogCoordinator.getCoordinator().awake();
    }
    
    public static synchronized HoverDialogCoordinator getCoordinator()
    {
        return instance_;
    }

    boolean shouldWait()
    {
        return hoverDialog_ == null;
//        return isHoverActive();
    }
    
    private boolean isHoverActive()
    {
        return hoverDialog_ != null;
//        return isHoverVisible();
    }
    
    private volatile boolean hoverVisible_;
    
    // Must be called on the Eclipse UI Thread.
    private boolean isHoverVisible()
    {
        Display.getDefault().syncExec(new Runnable()
        {
            @Override
            public void run()
            {
                AnnotationInformationControl hover;
                synchronized(hoverLock_)
                {
                    hover = hoverDialog_;
                }
                if (hover != null)
                    hoverVisible_ = hoverDialog_.isVisible();
                else
                    hoverVisible_ = false;
            }
        });
        return hoverVisible_;
    }
    
    void updateHover(ArrayList <AugmentedCompletionProposal> globalBestProposals,
            AugmentedCompletionProposal [] localProposals)
    {
        if (!isHoverActive())
            return;

        ICompletionProposal [] nonProcessedPropoals = QuickFixDialogCoordinator.getCoordinator().getNonProcessedProposals(eclipseProposals_);
        ICompletionProposal [] proposals = new ICompletionProposal [globalBestProposals.size() + localProposals.length + nonProcessedPropoals.length];
        String [] displayStrings = new String [proposals.length];
        HashSet <String> addedProposals = new HashSet <String>();
        
        int gbpSize = 0;
        for (int a = 0; a < globalBestProposals.size(); a++)
        {
            AugmentedCompletionProposal globalBestProposal = globalBestProposals.get(a);
            try
            {
                QuickFixDialogCoordinator.getCoordinator().resolve(globalBestProposal);
                if (!addedProposals.contains(globalBestProposal.getDisplayString()))
                {
                    logger.finest("Adding proposal: " + globalBestProposal.getDisplayString() + " as GBP.");
                    addedProposals.add(globalBestProposal.getDisplayString());
                    int index = gbpSize;
                    globalBestProposal.makeGBP();
                    proposals[index] = globalBestProposal.getProposal();
                    displayStrings[index] = globalBestProposal.getFinalDisplayString();
                    gbpSize++;
                }
            }
            catch (GBPResolutionException e)
            {
                logger.log(Level.INFO, "Cannot resolve global best proposal for shadow proposal = "
                        + globalBestProposal.getDisplayString(), e);
            }
        }
        
        int lpSize = 0;
        for (int a = 0; a < localProposals.length; a++)
        {
            AugmentedCompletionProposal localProposal = localProposals[a];
            if (!addedProposals.contains(localProposal.getDisplayString()))
            {
                logger.finest("Adding proposal: " + localProposal.getDisplayString() + " as local proposal.");
                addedProposals.add(localProposal.getDisplayString());
                int index = gbpSize + lpSize;
                proposals[index] = localProposal.getProposal();
                displayStrings[index] = localProposal.getFinalDisplayString();
                lpSize ++;
            }
        }
        
        int nppSize = 0;
        for (int a = 0; a < nonProcessedPropoals.length; a++)
        {
            ICompletionProposal nonProcessedProposal = nonProcessedPropoals[a];
            if (!addedProposals.contains(nonProcessedProposal.getDisplayString()))
            {
                logger.finest("Adding proposal: " + nonProcessedProposal.getDisplayString() + " as local proposal.");
                addedProposals.add(nonProcessedProposal.getDisplayString());
                int index = gbpSize + lpSize + nppSize;
                proposals[index] = nonProcessedPropoals[a];
                displayStrings[index] = "(N/A) " + nonProcessedPropoals[a].getDisplayString(); 
                nppSize ++;
            }
        }
        setProposalsInUIThread(proposals, displayStrings);
    }
    
    private void setProposalsInUIThread(final ICompletionProposal [] proposals, final String [] displayStrings)
    {
        if (!isHoverActive())
            return;
        Display.getDefault().syncExec(new Runnable()
        {
            @Override
            public void run()
            {
                setProposalsInternal(proposals, displayStrings);
            }
        });
    }

    // Done in Eclipse UI thread.
    private void setProposalsInternal(ICompletionProposal [] proposals, String [] displayStrings)
    {
        synchronized(hoverLock_)
        {
            if (isHoverActive())
                hoverDialog_.setProposals(proposals, displayStrings);
        }
    }

    void setEclipseProposals(ICompletionProposal [] proposals)
    {
        eclipseProposals_ = proposals;
    }

    void popupClosed()
    {
        synchronized(hoverLock_)
        {
            hoverDialog_ = null;
        }
    }
    
    public static class AnnotationInformationControl
    {
        public void setProposals(ICompletionProposal [] proposals, String [] displayStrings)
        {
        }
        public boolean isVisible()
        {
            return false;
        }
    }
}
