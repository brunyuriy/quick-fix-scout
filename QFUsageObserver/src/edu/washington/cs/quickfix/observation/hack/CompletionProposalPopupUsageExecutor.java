package edu.washington.cs.quickfix.observation.hack;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import edu.washington.cs.quickfix.observation.hack.ICompletionProposalPopupUsageHack;
import edu.washington.cs.quickfix.observation.log.ObservationLogger;
import edu.washington.cs.quickfix.observation.log.internal.QFSession.QFSessionType;

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
        ObservationLogger.getLogger().logPopupCreated(QFSessionType.DIALOG);
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

    @Override
    public void hoverPopupCreated()
    {
        ObservationLogger.getLogger().logPopupCreated(QFSessionType.HOVER);
    }

    @Override
    public void hoverPopupClosed()
    {
        ObservationLogger.getLogger().logPopupClosed();
    }
}
