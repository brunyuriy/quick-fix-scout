package edu.washington.cs.quickfix.observation;

import java.util.logging.Level;
import java.util.logging.Logger;


import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IStartup;

import edu.washington.cs.email.EmailSender;
import edu.washington.cs.hack.HackActionManager;
import edu.washington.cs.quickfix.bridge.BridgeActionManager;
import edu.washington.cs.quickfix.observation.gui.ObservationPreferencePage;
import edu.washington.cs.quickfix.observation.hack.ObserverHackActionManager;
import edu.washington.cs.quickfix.observation.log.ObservationLogSender;
import edu.washington.cs.quickfix.observation.log.ObservationLogger;
import edu.washington.cs.quickfix.observation.log.ObservationOperationHistoryListener;
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
 * {@link IStartup} implementation for Observation plug-in <br>
 * This class includes the code that needs to execute as soon as Eclipse UI (workbench) is created.
 * 
 * @author Kivanc Muslu
 */
public class ObservationStarter implements IStartup
{
    /** Logger for debugging. */
    private static final Logger logger = Logger.getLogger(ObservationStarter.class.getName());
    static
    {
        LogHandlers.init(SharedConstants.DEBUG_LOG_PATH);
        LogHandlers.setMainHandler(System.err);
        logger.setLevel(Level.INFO);
    }
    public static final String [] DEPENDENT_PLUG_INS = {Observer.PLUG_IN_ID, ObserverHackActionManager.PLUG_IN_ID,
            BridgeActionManager.PLUG_IN_ID, HackActionManager.PLUG_IN_ID, ProjectSynchronizer.PLUG_IN_ID,
            ResourceUtility.PLUG_IN_ID, EmailSender.PLUG_IN_ID, LogHandlers.PLUG_IN_ID,
            MortalThread.PLUG_IN_ID, SwingUtility.PLUG_IN_ID};

    /**
     * {@inheritDoc}
     * <p>
     * Initializes the observation plug-in and observes the current file's project if any.
     * </p>
     */
//    @SuppressWarnings("restriction")
    @Override
    public void earlyStartup()
    {
        logger.info("QFObservationStarter is running...");
        SynchronizerStarter.initGlobalListeners();
        SynchronizerCursorListener.getInstance().addCursorChangedListener(Observer.getUsageObserver());
        OperationHistoryFactory.getOperationHistory().addOperationHistoryListener(
                new ObservationOperationHistoryListener());
//        DocumentUndoManager undoManager = new DocumentUndoManager(document)
//        UndoManager2 undoManager = new UndoManager2();
//        undoManager.addListener(new ObservationUndoManagerListener());
        sendLogs();
        ResourceUtility.checkForUpdates("Observer", false, DEPENDENT_PLUG_INS);
        ResourceUtility.logSystemInformation(DEPENDENT_PLUG_INS);
        try
        {
            IFile currentFile = EclipseUIUtility.getActiveEditorFileInUIThread();
            if (currentFile != null)
                Observer.getUsageObserver().observeProject(currentFile.getProject());
        }
        catch (NotInitializedException e)
        {
            logger.log(Level.SEVERE, "Workbench page is not created yet", e);
        }
    }
    
    private void sendLogs()
    {
        final ObservationLogSender logSender;
        if (ObservationPreferencePage.getInstance().shouldSendLogs())
        {
            logSender = new ObservationLogSender();
            logSender.sendLogs();
        }
        else
            logSender = null;
        Thread logStarter = new Thread()
        {
            @Override
            public void run()
            {
                if (logSender != null)
                    logSender.waitUntilLogsSent();
                ObservationLogger.getLogger().startLogging();
            }
        };
        logStarter.start();
    }
}
