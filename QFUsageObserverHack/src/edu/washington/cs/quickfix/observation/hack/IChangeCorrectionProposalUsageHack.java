package edu.washington.cs.quickfix.observation.hack;

import edu.washington.cs.hack.IHack;

/**
 * Extension point declaration for org.eclipse.ui.ChangeCorrectionProposal.
 * 
 * @author Kivanc Muslu
 */
@Deprecated
public interface IChangeCorrectionProposalUsageHack extends IHack
{
    /**
     * This method is called by Eclipse whenever a change correction is applied to a project. <br>
     * <br>
     * Input argument must be an object since giving the actual type here would create a cyclic dependency. The class
     * that overwrites this method will know that the passed object will always be of type:
     * 
     * <pre>
     * proposal = ChangeCorrectionProposal
     * </pre>
     * 
     * @param proposal correction proposal that is recently applied to the project.
     */
    void changePerformed(Object proposal);
}
