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
    
    
   
    
    
    private void computeTableValues(HashSet <String> addedProposals, ArrayList <AugmentedCompletionProposal> globalBestProposals,
            ArrayList <AugmentedCompletionProposal> localProposals, ArrayList <ICompletionProposal> tableProposals)
    {
        if (!isCurrentPopupActive())
            return;
        
        ArrayList <AugmentedCompletionProposal> gbps;
        ArrayList <AugmentedCompletionProposal> lps;
        ArrayList <AugmentedCompletionProposal> notFixingGlobalProposal 
        						= new ArrayList<AugmentedCompletionProposal>(); //GBP not fixing local compilation error
        ArrayList<AugmentedCompletionProposal>notFixingLocalProposal 
        						= new ArrayList <AugmentedCompletionProposal>(); //Local Proposal not fixing local compilation error
        synchronized(lock_)
        {
            gbps = globalBestProposals_;
            lps = localProposals_;
        }
        
        // First enter the global best proposals.
        for (AugmentedCompletionProposal globalBestProposal: gbps)
        {
            try
            {
                QuickFixDialogCoordinator.getCoordinator().resolve(globalBestProposal);
                if (!addedProposals.contains(globalBestProposal.getDisplayString()))
                {
                    logger.finest("Adding proposal: " + globalBestProposal.getDisplayString() + " as GBP.");
                    globalBestProposal.makeGBP();
                    globalBestProposal.cacheDisplayFields();
                    //if GBP cannot not fix the local error, add it to the array list
                    if (globalBestProposal.doNoTFixLocalError())                    	
                    	notFixingGlobalProposal.add(globalBestProposal);
                    
                    tableProposals.add(globalBestProposal.getProposal());
                    addedProposals.add(globalBestProposal.getDisplayString());
                    globalBestProposals.add(globalBestProposal);
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
        for (AugmentedCompletionProposal localProposal: lps)
        {
            assert localProposal != null: "Received a local proposals that is null";
            if (!addedProposals.contains(localProposal.getDisplayString()))
            {
                logger.finest("Adding proposal: " + localProposal.getDisplayString() + " as local proposal.");
                //localProposal.cacheDisplayFields();
                //if the local proposal cannot not fix the local error, add it to the array list
                if (localProposal.doNoTFixLocalError())         
                      	notFixingLocalProposal.add(localProposal);
                
                tableProposals.add(localProposal.getProposal());
                addedProposals.add(localProposal.getDisplayString());
                localProposals.add(localProposal);
            }
        }
    }
  
    
    
    private void updateProposalTableInternalInUIThread()
    {
        if (!isCurrentPopupActive())
            return;
        
        final HashSet <String> addedProposals = new HashSet <String>();
        final ArrayList<AugmentedCompletionProposal> globalBestProposals = new ArrayList <AugmentedCompletionProposal>();
        final ArrayList<AugmentedCompletionProposal> localProposals = new ArrayList <AugmentedCompletionProposal>();
        final ArrayList<ICompletionProposal> tableProposals = new ArrayList <ICompletionProposal>();
        computeTableValues(addedProposals, globalBestProposals, localProposals, tableProposals);
        
        Display.getDefault().syncExec(new Runnable()
        {
            @Override
            public void run()
            {
                updateProposalTableInternal(addedProposals, globalBestProposals, localProposals, tableProposals);
            }
        });
    }
    
    private void updateProposalTableInternal(HashSet <String> addedProposals, ArrayList<AugmentedCompletionProposal> globalBestProposals,
            ArrayList <AugmentedCompletionProposal> localProposals, ArrayList <ICompletionProposal> tableProposals)
    {
        if (!isCurrentPopupActive())
        {
            logger.finer("Current popup is no longer active, not updating the UI.");
            return;
        }
        Table table;
        synchronized(lock_)
        {
            table = table_;
            tableProposals_.clear();
        }
        
        table.setRedraw(false);
        TableItem [] items = table.getItems();
        int knownStyle = items.length > 0 ? items[0].getStyle() : -1;
        // Here we need to decide which proposals are not included by our computation.
        // This is non-trivial, since the calculation re-ordered the proposals.
        ICompletionProposal [] nonProcessedProposals = getNonProcessedProposals(items);
        // First enter the global best proposals.
        for (int a = 0; a < globalBestProposals.size(); a++)
            setTableItem(globalBestProposals.get(a), a, knownStyle);
        // Then, enter the local proposals ordered.
        for (int a = 0; a < localProposals.size(); a++)
            setTableItem(localProposals.get(a), globalBestProposals.size() + a, knownStyle);

        // Then, enter the proposals that we don't have a calculation for.
        int nonProcessedProposalSize = 0;
        for (ICompletionProposal nonProcessedProposal: nonProcessedProposals)
        {
            if (!addedProposals.contains(nonProcessedProposal.getDisplayString()))
            {
                logger.finest("Adding proposal: " + nonProcessedProposal.getDisplayString()
                        + " as non processed proposal.");
                addedProposals.add(nonProcessedProposal.getDisplayString());
                tableProposals.add(nonProcessedProposal);
                setTableItem(nonProcessedProposal, nonProcessedProposalSize + globalBestProposals.size() + localProposals.size(), knownStyle);
                nonProcessedProposalSize++;
            }
        }
        table.setItemCount(nonProcessedProposalSize + globalBestProposals.size() + localProposals.size());
        
        synchronized(lock_)
        {
            tableProposals_ = tableProposals;
        }
        table.setRedraw(true);
        table.redraw();
    }

    private void setTableItem(AugmentedCompletionProposal proposal, int index, int knownStyle)
    {
        TableItem item = (table_.getItemCount() > index) ? table_.getItem(index) : null;
        // This can happen due to newly added items.
        if (item == null)
            item = new TableItem(table_, knownStyle, index);
        proposal.setYourselfAsTableItem(item);
    }

    private void setTableItem(ICompletionProposal proposal, int index, int knownStyle)
    {
        TableItem item = (table_.getItemCount() > index) ? table_.getItem(index) : null;
        // This can happen due to newly added items.
        if (item == null)
            item = new TableItem(table_, knownStyle, index);
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
                logger.fine("Got a proposal that is not iCompletionProposal. proposal.getClass() = "
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
        {
            final HashSet <String> addedProposals = new HashSet <String>();
            final ArrayList<AugmentedCompletionProposal> globalBestProposals = new ArrayList <AugmentedCompletionProposal>();
            final ArrayList<AugmentedCompletionProposal> localProposals = new ArrayList <AugmentedCompletionProposal>();
            final ArrayList<ICompletionProposal> tableProposals = new ArrayList <ICompletionProposal>();
            computeTableValues(addedProposals, globalBestProposals, localProposals, tableProposals);
            updateProposalTableInternal(addedProposals, localProposals, localProposals, tableProposals);
        }
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
