package edu.washington.cs.quickfix.speculation;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IStartup;

//import edu.cs.washington.quickfix.speculation.converter.EclipseObjectConverter;
import edu.washington.cs.hack.HackActionManager;
import edu.washington.cs.quickfix.bridge.BridgeActionManager;
import edu.washington.cs.quickfix.speculation.hack.SpeculationHackActionManager;
import edu.washington.cs.swing.SwingUtility;
import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.synchronization.SynchronizerStarter;
import edu.washington.cs.synchronization.sync.SynchronizerCursorListener;
import edu.washington.cs.threading.MortalThread;
import edu.washington.cs.util.eclipse.EclipseUIUtility;
import edu.washington.cs.util.eclipse.ResourceUtility;
import edu.washington.cs.util.eclipse.SharedConstants;
import edu.washington.cs.util.exception.NotInitializedException;
import edu.washington.cs.util.log.LogHandlers;

/**
 * {@link IStartup} implementation for Speculation plug-in <br>
 * This class includes the code that needs to execute as soon as Eclipse UI (workbench) is created.
 * 
 * @author Kivanc Muslu
 */
public class SpeculationStarter implements IStartup
{
    public static final String [] DEPENDENT_PLUG_INS = {Speculator.PLUG_IN_ID, SpeculationHackActionManager.PLUG_IN_ID,
        BridgeActionManager.PLUG_IN_ID, HackActionManager.PLUG_IN_ID, ProjectSynchronizer.PLUG_IN_ID,
        ResourceUtility.PLUG_IN_ID, /*EclipseObjectConverter.PLUG_IN_ID,*/
        LogHandlers.PLUG_IN_ID, MortalThread.PLUG_IN_ID, SwingUtility.PLUG_IN_ID};
    /** Logger for debugging. */
    private static final Logger logger = Logger.getLogger(SpeculationStarter.class.getName());
    static
    {
        LogHandlers.init(SharedConstants.DEBUG_LOG_PATH);
        LogHandlers.setMainHandler(System.err);
        logger.setLevel(Level.INFO);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Initializes the observation plug-in and observes the current file's project if any.
     * </p>
     */
    @Override
    public void earlyStartup()
    {
        logger.info("QFSpeculationStarter is running...");
        ResourceUtility.logSystemInformation(DEPENDENT_PLUG_INS);
        ResourceUtility.checkForUpdates("Speculator", false, DEPENDENT_PLUG_INS);
        SynchronizerStarter.initGlobalListeners();
        SynchronizerCursorListener.getInstance().addCursorChangedListener(Speculator.getSpeculator());
        IFile initialFile = null;
        try
        {
            initialFile = EclipseUIUtility.getActiveEditorFileInUIThread();
            if (initialFile != null)
                Speculator.getSpeculator().speculateProject(initialFile);
        }
        catch (NotInitializedException e)
        {
            logger.log(Level.SEVERE, "Workbench page is not created yet", e);
        }
    }
}
