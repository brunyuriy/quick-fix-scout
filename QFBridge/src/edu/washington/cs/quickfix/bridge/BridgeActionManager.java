package edu.washington.cs.quickfix.bridge;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.washington.cs.hack.HackActionManager;
import edu.washington.cs.hack.IHack;
import edu.washington.cs.util.log.LogHandlers;

public class BridgeActionManager extends HackActionManager
{
    public static final String PLUG_IN_ID = "edu.washington.cs.quickfix.bridge";
    private static final String SPECULATOR_OBSERVER_BRIDGE_HANDLER = "edu.washington.cs.quickfix.bridge.speculatorObserverBridge";
    private static final String SPECULATOR_OBSERVER_BRIDGE_ID = "speculatorObserverBridgeExecutor";
    /** singleton of this class. */
    private static BridgeActionManager handler_ = new BridgeActionManager();
    /** logger for debugging. */
    private final Logger logger = Logger.getLogger(HackActionManager.class.getName());

    /**
     * Cannot be instantiated.
     */
    private BridgeActionManager()
    {
        super();
        LogHandlers.setMainHandler(System.err);
        logger.setLevel(Level.INFO);
        loadHandler(SPECULATOR_OBSERVER_BRIDGE_HANDLER);
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
    public static BridgeActionManager getInstance()
    {
        return handler_;
    }

    /**
     * Returns the extension point implementation in speculation plug-in for edu.washington.cs.quickfix.observer
     * plug-in. <br>
     * Returns null if the speculation plug-in is not loaded.
     * 
     * @return The extension point implementation in speculation plug-in for edu.washington.cs.quickfix.observer
     *         plug-in.
     */
    public ISpeculatorObserverBridge getSpeculatorObserverBridge()
    {
        IHack hack = getHandler(SPECULATOR_OBSERVER_BRIDGE_ID);
        if (hack instanceof ISpeculatorObserverBridge)
            return (ISpeculatorObserverBridge) hack;
        else if (hack == null)
            logger.info("Logic for SpeculatorObserverBridge (plug-in) is not included in the directory. The hook notifications will be ignored.");
        else
            logger.warning("This really should not happen: searched for a speculator observer bridge but got another handler.");
        return null;
    }
}
