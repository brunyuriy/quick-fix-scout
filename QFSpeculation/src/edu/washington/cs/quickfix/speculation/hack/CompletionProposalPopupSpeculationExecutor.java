package edu.washington.cs.quickfix.speculation.hack;

//import org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover.AnnotationInformationControl;
import edu.washington.cs.quickfix.speculation.hack.HoverDialogCoordinator.AnnotationInformationControl;
import org.eclipse.jface.text.contentassist.CompletionProposalPopup;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.widgets.Table;

import edu.washington.cs.hack.Hack;
import edu.washington.cs.quickfix.speculation.hack.ICompletionProposalPopupSpeculationHack;

@SuppressWarnings("restriction")
public class CompletionProposalPopupSpeculationExecutor extends Hack implements ICompletionProposalPopupSpeculationHack
{
    @Override
    public void proposalTableSet(Object tableObject, Object popupObject)
    {
        // These cannot fail!
        CompletionProposalPopup popup = (CompletionProposalPopup) popupObject;
        Table table = (Table) tableObject;
        CompletionProposalPopupCoordinator.getCoordinator().proposalTableSet(table, popup);
    }

    @Override
    public void tableUpdated()
    {
        CompletionProposalPopupCoordinator.getCoordinator().tableUpdated();
    }

    @Override
    public void popupClosed()
    {
        QuickFixDialogCoordinator.getCoordinator().popupClosed();
    }

    @Override
    public void proposalSelected(Object proposalObject)
    {
        // This cannot fail!
        ICompletionProposal proposal = (ICompletionProposal) proposalObject;
        QuickFixDialogCoordinator.getCoordinator().proposalSelected(proposal);
    }

    @Override
    public Object getSelectedProposal(int index)
    {
        return CompletionProposalPopupCoordinator.getCoordinator().getSelectedProposal(index);
    }

    @Override
    public void hoverPopupCreated(Object annotationInformationControl)
    {
        // This cannot fail!
        AnnotationInformationControl hoverController = (AnnotationInformationControl) annotationInformationControl;
        HoverDialogCoordinator.getCoordinator().hoverCreated(hoverController);
    }

    @Override
    public void hoverProposalsSet(Object [] proposalsObject)
    {
        // This cannot fail!
        ICompletionProposal [] proposals = (ICompletionProposal []) proposalsObject;
        HoverDialogCoordinator.getCoordinator().setEclipseProposals(proposals);
    }
}
