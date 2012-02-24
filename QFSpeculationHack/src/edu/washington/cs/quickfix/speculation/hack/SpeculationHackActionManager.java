package edu.washington.cs.quickfix.speculation.hack;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.washington.cs.hack.HackActionManager;
import edu.washington.cs.hack.IHack;
import edu.washington.cs.util.log.LogHandlers;

/**
 * Action manager class implementation for QuickFixSpeculation plug-in. <br>
 * This plug-in permits the usage observer plug-in to communicate with already existing Eclipse plug-ins (via hooks) and
 * also prevents the cyclic dependency issues. <br>
 * At the moment the only core plug-in that has extra hooks is: org.eclipse.jface.text. <br>
 * <br>
 * Note: This implementation is found at:
 * http://technical-tejash.blogspot.com/2010/03/eclipse-avoid-cyclic-dependency-between.html
 * 
 * @author Tejash Shah, Kivanc Muslu
 */
public class SpeculationHackActionManager extends HackActionManager
{
    public static final String PLUG_IN_ID = "edu.washington.cs.quickfix.speculation.hack";
    /**
     * complete identifier path for the extension point implemented for CorrectionProposalPopup class of jface.text
     * plug-in.
     */
    private static final String CORRECTION_PROPOSAL_POPUP_HACK_HANDLER = "edu.washington.cs.quickfix.speculation.hack.correctionProposalPopupSpeculationHack";
    /**
     * hardcoded identifier extension for the extender plug-ins (i.e., QuickFixUsageObserver plug-in) for extension
     * point correctionProposalPopupUsageHack.
     */
    private static final String CORRECTION_PROPOSAL_POPUP_HACK_ID = "correctionProposalPopupSpeculationExecutor";
    /** singleton of this class. */
    private static SpeculationHackActionManager handler_ = new SpeculationHackActionManager();
    /** logger for debugging. */
    private final Logger logger = Logger.getLogger(SpeculationHackActionManager.class.getName());

    /**
     * Cannot be instantiated.
     */
    private SpeculationHackActionManager()
    {
        super();
        LogHandlers.setMainHandler(System.err);
        logger.setLevel(Level.INFO);
        loadHandler(CORRECTION_PROPOSAL_POPUP_HACK_HANDLER);
        logger.info("Action handlers are loaded...");
    }

    /***************************
     * GETTER & SETTER METHODS *
     **************************/
    /**
     * Returns the action handler manager.
     * 
     * @return The action handler manager.
     */
    public static SpeculationHackActionManager getInstance()
    {
        return handler_;
    }

    /**
     * Returns the extension point implementation in speculation plug-in for
     * org.eclipse.jface.text.CompletionProposalPopup class. <br>
     * Returns null if the speculation plug-in is not loaded.
     * 
     * @return The extension point implementation in speculation plug-in for
     *         org.eclipse.jface.text.CompletionProposalPopup class.
     */
    public ICompletionProposalPopupSpeculationHack getCorrectionProposalPopupHandler()
    {
        IHack hack = getHandler(CORRECTION_PROPOSAL_POPUP_HACK_ID);
        if (hack instanceof ICompletionProposalPopupSpeculationHack)
            return (ICompletionProposalPopupSpeculationHack) hack;
        else if (hack == null)
            logger.info("Logic for CorrectionProposalPopupHandler hack (plug-in) is not included in the directory. The hook notifications will be ignored.");
        else
            logger.warning("This really should not happen: searched for a correction proposal handler but got another handler.");
        return null;
    }
}
