package edu.washington.cs.quickfix.observation.hack;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import edu.washington.cs.quickfix.observation.hack.ICompletionProposalPopupUsageHack;
import edu.washington.cs.quickfix.observation.log.ObservationLogger;

public class CompletionProposalPopupUsageExecutor implements ICompletionProposalPopupUsageHack
{
    private String id_ = null;

    @Override
    public void setId(String id)
    {
        id_ = id;
    }

    @Override
    public String getId()
    {
        return id_;
    }

    @Override
    public void proposalTableSet()
    {
        ObservationLogger.getLogger().logPopupCreated();
    }

    @Override
    public void popupClosed()
    {
        ObservationLogger.getLogger().logPopupClosed();
    }

    @Override
    public void proposalSelected(Object proposalObject)
    {
        // This cannot fail!
        ICompletionProposal proposal = (ICompletionProposal) proposalObject;
        ObservationLogger.getLogger().logProposalSelected(proposal);
    }
}
