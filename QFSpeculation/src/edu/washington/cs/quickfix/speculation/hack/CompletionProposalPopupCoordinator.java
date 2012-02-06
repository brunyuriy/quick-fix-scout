package edu.washington.cs.quickfix.speculation.hack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.text.contentassist.CompletionProposalPopup;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import edu.washington.cs.quickfix.speculation.calc.model.AugmentedCompletionProposal;
import edu.washington.cs.util.log.CommonLoggers;
//@formatter:off
/*
 * Facts:
 * 1-) Eclipse loads proposal information on demand. If there are a lot of proposals, however only some of them are visible in the popup,
 * then information for the non-visible quick fixes are not added to the popup unless the user scrolls down.
 * 2-) I have modified this fact. I use a new hook to Eclipse's load and select methods so that I load all the available proposals right away.
 * 3-) Don't use any logging with the methods that Eclipse UI thread will call to create the popup menu, add proposals etc., because
 * due to synchronization of logging (I think) it blocks the UI thread until the whole computation is done (weird !..)
 */
//@formatter:on
public class CompletionProposalPopupCoordinator
{
    private static final CompletionProposalPopupCoordinator instance_ = new CompletionProposalPopupCoordinator();

    private static final Logger logger = Logger.getLogger(CompletionProposalPopupCoordinator.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
        // logger.setLevel(Level.FINER);
        // logger.setLevel(Level.FINEST);
    }

    private Table table_ = null;
    private CompletionProposalPopup popup_ = null;
    private ArrayList <ICompletionProposal> tableProposals_ = new ArrayList <ICompletionProposal>();

    private ArrayList<AugmentedCompletionProposal> globalBestProposals_;
    private ArrayList <AugmentedCompletionProposal> localProposals_;
    
    private Object lock_ = new Object();

    // singleton
    private CompletionProposalPopupCoordinator() {}

    public static synchronized CompletionProposalPopupCoordinator getCoordinator()
    {
        return instance_;
    }

    void updatePopup(ArrayList<AugmentedCompletionProposal> globalBestProposals, AugmentedCompletionProposal [] localProposals)
    {
        // Too bad, we are too late. Popup is closed.
        if (!isCurrentPopupActive())
        {
            logger.finer("Current popup is no longer active, not updating the UI.");
            return;
        }
        synchronized(lock_)
        {
            globalBestProposals_ = new ArrayList <AugmentedCompletionProposal>(globalBestProposals);
            localProposals_ = new ArrayList <AugmentedCompletionProposal>(Arrays.asList(localProposals));
        }
        // Cannnot synchronize this since this will be done inside another thread.
        updateProposalTableInternalInUIThread();
    }

    private void updateProposalTableInternalInUIThread()
    {
        if (!isCurrentPopupActive())
            return;
        Display.getDefault().syncExec(new Runnable()
        {
            @Override
            public void run()
            {
                updateProposalTableInternal();
            }
        });
    }
    
    private void updateProposalTableInternal()
    {
        if (!isCurrentPopupActive())
        {
            logger.finer("Current popup is no longer active, not updating the UI.");
            return;
        }
        synchronized(lock_)
        {
            table_.setRedraw(false);
            tableProposals_.clear();
            TableItem [] items = table_.getItems();
            int knownStyle = items.length > 0 ? items[0].getStyle() : -1;
            // Here we need to decide which proposals are not included by our computation.
            // This is non-trivial, since the calculation re-ordered the proposals.
            ICompletionProposal [] nonProcessedProposals = getNonProcessedProposals(items);
            HashSet <String> addedProposals = new HashSet <String>();
            // First enter the global best proposals.
            int gbpSize = 0;
            for (AugmentedCompletionProposal globalBestProposal: globalBestProposals_)
            {
                try
                {
                    QuickFixDialogCoordinator.getCoordinator().resolve(globalBestProposal);
                    if (!addedProposals.contains(globalBestProposal.getDisplayString()))
                    {
                        logger.finest("Adding proposal: " + globalBestProposal.getDisplayString() + " as GBP.");
                        addedProposals.add(globalBestProposal.getDisplayString());
                        setTableItem(globalBestProposal, gbpSize, knownStyle, true);
                        gbpSize++;
                    }
                }
                catch (GBPResolutionException e)
                {
                    // This is known when the proposals from Eclipse is not retrieved yet. So, just pass.
                    logger.log(Level.FINE, "Cannot resolve global best proposal for shadow proposal = "
                            + globalBestProposal.getDisplayString(), e);
                }
            }
            // Then, enter the local proposals ordered.
            int localProposalSize = 0;
            for (AugmentedCompletionProposal localProposal: localProposals_)
            {
                assert localProposal != null: "Received a local proposals that is null";
                if (!addedProposals.contains(localProposal.getDisplayString()))
                {
                    logger.finest("Adding proposal: " + localProposal.getDisplayString() + " as local proposal.");
                    addedProposals.add(localProposal.getDisplayString());
                    setTableItem(localProposal, localProposalSize + gbpSize, knownStyle, false);
                    localProposalSize++;
                }
            }
            // Then, enter the proposals that we don't have a calculation for.
            int nonProcessedProposalSize = 0;
            for (ICompletionProposal nonProcessedProposal: nonProcessedProposals)
            {
                if (!addedProposals.contains(nonProcessedProposal.getDisplayString()))
                {
                    logger.finest("Adding proposal: " + nonProcessedProposal.getDisplayString()
                            + " as non processed proposal.");
                    addedProposals.add(nonProcessedProposal.getDisplayString());
                    setTableItem(nonProcessedProposal, nonProcessedProposalSize + gbpSize + localProposalSize, knownStyle);
                    nonProcessedProposalSize++;
                }
            }
            table_.setItemCount(nonProcessedProposalSize + gbpSize + localProposalSize);
            table_.setRedraw(true);
            table_.redraw();
        }
    }

    private void setTableItem(AugmentedCompletionProposal proposal, int index, int knownStyle, boolean gbp)
    {
        TableItem item = (table_.getItemCount() > index) ? table_.getItem(index) : null;
        // This can happen due to newly added items.
        if (item == null)
            item = new TableItem(table_, knownStyle, index);
        tableProposals_.add(proposal.getProposal());
        proposal.setYourselfAsTableItem(item, gbp);
    }

    private void setTableItem(ICompletionProposal proposal, int index, int knownStyle)
    {
        TableItem item = (table_.getItemCount() > index) ? table_.getItem(index) : null;
        // This can happen due to newly added items.
        if (item == null)
            item = new TableItem(table_, knownStyle, index);
        tableProposals_.add(proposal);
        item.setData(proposal);
        String displayInformation = proposal.getDisplayString();
        if (!displayInformation.startsWith("(N/A) "))
//             displayInformation = "(2) " + displayInformation;
            displayInformation = "(N/A) " + displayInformation;
        item.setText(displayInformation);
    }
    
    // The caller of this method must run in Eclipse UI thread (due to the access on TableItem).
    // This method is just written as a helper for updateProposalTableInternal() method.
    private ICompletionProposal [] getNonProcessedProposals(TableItem [] items)
    {
        ArrayList<ICompletionProposal> proposals = new ArrayList <ICompletionProposal>();
        for (int a = 0; a < items.length; a++)
        {
            Object data = items[a].getData();
            if (data instanceof ICompletionProposal)
            {
                ICompletionProposal proposal = (ICompletionProposal) data;
                proposals.add(proposal);
            }
            else
                logger.warning("Got a proposal that is not iCompletionProposal. proposal.getClass() = "
                    + (data == null ? "null" : data.getClass()));
        }
        return QuickFixDialogCoordinator.getCoordinator().getNonProcessedProposals(proposals.toArray(new ICompletionProposal[proposals.size()]));
    }

    boolean shouldWait()
    {
        synchronized(lock_)
        {
            return table_ == null || popup_ == null;
        }
    }

    public void propsoalTableSet(Table proposalTable, CompletionProposalPopup popup)
    {
        synchronized(lock_)
        {
            // logger.fine("Eclipse notification: proposal table is set!");
            table_ = proposalTable;
            // System.out.println("Setting popup");
            popup_ = popup;
            tableProposals_.clear();
        }
        QuickFixDialogCoordinator.getCoordinator().clear();
    }

    // TODO Make sure that this really returns 'null' if the table has not been modified yet.
    public ICompletionProposal getSelectedProposal(int index)
    {
        synchronized(lock_)
        {
            /*
             * This is a bug fix for Windows. For some reason, in windows, this method gets called even before the dialog is
             * created and with index = -1. If I don't do this, I get an internal ArrayOutOfBoundsException.
             */
            if (index < 0 || tableProposals_.size() <= index)
                return null;
            else
                return tableProposals_.get(index);
        }
    }

    void popupClosed()
    {
        clear();
    }

    private boolean isCurrentPopupActive()
    {
        synchronized(lock_)
        {
            boolean result = popup_ != null && popup_.isActive();
            // System.out.println("isCurrentPopupActive() = " + result + ", popup = " + popup_);
            return result;
        }
    }

    /*
     * Eclipse only updates table elements on demand! (as they are visible on the screen!). Need to find another way to
     * get the elements.
     */
    public void tableUpdated()
    {
        CommonLoggers.getCommunicationLogger().fine("Table updated message is received.");
        if (localProposals_ != null)
            updateProposalTableInternal();
        else
            QuickFixDialogCoordinator.getCoordinator().awake();
    }

    private void clear()
    {
        // This method cannot assign Eclipse proposals and calculated proposals to 'null'
        // as the same quick fix can be called multiple times for the same computation.
        synchronized(lock_)
        {
            localProposals_ = null;
            globalBestProposals_ = null;
            table_ = null;
            popup_ = null;
        }
    }
}
