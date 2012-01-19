package edu.washington.cs.quickfix.speculation.hack;

import org.eclipse.jface.text.contentassist.CompletionProposalPopup;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.widgets.Table;

import edu.washington.cs.hack.Hack;
import edu.washington.cs.quickfix.speculation.hack.ICompletionProposalPopupSpeculationHack;

public class CompletionProposalPopupSpeculationExecutor extends Hack implements ICompletionProposalPopupSpeculationHack
{
    @Override
    public void proposalTableSet(Object tableObject, Object popupObject)
    {
        // These cannot fail!
        CompletionProposalPopup popup = (CompletionProposalPopup) popupObject;
        Table table = (Table) tableObject;
        CompletionProposalPopupCoordinator.getCoordinator().propsoalTableSet(table, popup);
    }

    @Override
    public void tableUpdated()
    {
        CompletionProposalPopupCoordinator.getCoordinator().tableUpdated();
    }

    @Override
    public void popupClosed()
    {
        CompletionProposalPopupCoordinator.getCoordinator().popupClosed();
    }

    @Override
    public void proposalSelected(Object proposalObject)
    {
        // This cannot fail!
        ICompletionProposal proposal = (ICompletionProposal) proposalObject;
        CompletionProposalPopupCoordinator.getCoordinator().proposalSelected(proposal);
    }

    @Override
    public Object getSelectedProposal(int index)
    {
        return CompletionProposalPopupCoordinator.getCoordinator().getSelectedProposal(index);
    }
}
