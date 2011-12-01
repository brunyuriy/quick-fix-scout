package edu.washington.cs.quickfix.speculation.hack;

import edu.washington.cs.hack.IHack;

/**
 * Extension point declaration for org.eclipse.jface.text.CompletionPropsoalPopup.
 * 
 * @author Kivanc Muslu
 */
public interface ICompletionProposalPopupSpeculationHack extends IHack
{
    /**
     * This method is called by Eclipse whenever a new completion proposal popup object is created and shown to the
     * user. <br>
     * <br>
     * The input arguments be objects since giving the actual type here would create a cyclic dependency. The class that
     * overwrites this method will know that the passed objects will always be of type:
     * 
     * <pre>
     * table = org.eclipse.swt.Widgets.Table
     * popup = org.eclipse.jface.text.contentassist.CompletionProposalPopup
     * </pre>
     */
    void proposalTableSet(Object table, Object popup);

    /**
     * This method is called by Eclipse whenever the current completion proposal popup's table is updated.
     */
    void tableUpdated();

    /**
     * This method is called by Eclipse whenever an existing completion proposal popup object is destroyed.
     */
    void popupClosed();

    /**
     * This method is called by Eclipse whenever a completion proposal is selected from the popup. <br>
     * <br>
     * Input argument must be an object since giving the actual type here would create a cyclic dependency. The class
     * that overwrites this method will know that the passed object will always be of type:
     * 
     * <pre>
     * proposal = org.eclipse.jface.text.contentassist.ICompletionProposal
     * </pre>
     * 
     * @param proposal Completion proposal that is selected.
     */
    void proposalSelected(Object proposal);

    /**
     * This method is called by Eclipse whenever Eclipse needs to figure out the selected proposal. <br>
     * The selected proposal cannot be identified by Eclipse anymore, since I change the ordering of the proposals
     * offered by Eclipse <br>
     * <br>
     * The return value must be an object since giving the actual type here would create a cyclic dependency. The class
     * that overwrites this method will know that it always has to return of type
     * org.eclipse.jface.text.contentassist.ICompletionProposal.
     * 
     * @param index Index of the selection
     * @return The selected proposal by the user as object.
     */
    Object getSelectedProposal(int index);
}
