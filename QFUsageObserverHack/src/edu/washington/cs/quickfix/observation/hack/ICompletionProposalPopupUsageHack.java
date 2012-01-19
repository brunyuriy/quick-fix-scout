package edu.washington.cs.quickfix.observation.hack;

import edu.washington.cs.hack.IHack;

/**
 * Extension point declaration for org.eclipse.jface.text.CompletionPropsoalPopup.
 * 
 * @author Kivanc Muslu
 */
public interface ICompletionProposalPopupUsageHack extends IHack
{
    /**
     * This method is called by Eclipse whenever a new completion proposal popup object is created and shown to the
     * user.
     */
    void proposalTableSet();

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
}
