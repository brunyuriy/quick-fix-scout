package edu.washington.cs.quickfix.observation.gui;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import edu.washington.cs.quickfix.observation.ObservationStarter;
import edu.washington.cs.quickfix.observation.Observer;
import edu.washington.cs.quickfix.observation.log.ObservationLogSender;
import edu.washington.cs.quickfix.observation.log.ObservationLogger;
import edu.washington.cs.util.eclipse.EclipseUIUtility;
import edu.washington.cs.util.eclipse.PreferencePageUtility;
import edu.washington.cs.util.eclipse.PreferencesUtility;
import edu.washington.cs.util.eclipse.ResourceUtility;
import edu.washington.cs.util.exception.NotInitializedException;
import edu.washington.cs.util.log.LogHandlers;

/**
 * This class is written mostly by imitating the methods in: org.eclipse.jdt.internal.debug.ui.JavaDebugPreferencePage
 * org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage
 * org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage
 * 
 * @author Kivanc Muslu
 */
public class ObservationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
    private Button observationActivatedButton_;
    private Button snapshotActivatedButton_;
    private PreferencesUtility preferences_;
    private Button sendLogsPeriodicallyEditor_;
    public static final String QF_OBSERVATION_PLUGIN_ACTIVATED = "QF Observation Plug-in Activated";
    public static final String QF_OBSERVATION_SNAPSHOT_ACTIVATED = "QF Observation Snapshot Activated";
    public static final String QF_OBSERVATION_SEND_LOGS_PERIODICALLY = "QF Observation Send Logs Periodically";
    public static final String QF_OBSERVATION_SKIP_SEND_LOGS_CONFIRMATION = "QF Observation Skip Send Logs Confirmation";
    public static final String QF_OBSERVATION_SKIP_SNAPSHOT_CONFIRMATION = "QF Observation Skip Snapshot Confirmation";
    public static ObservationPreferencePage instance_ = new ObservationPreferencePage();
    private static final Logger logger = Logger.getLogger(ObservationPreferencePage.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    public ObservationPreferencePage()
    {
        preferences_ = new PreferencesUtility(Observer.PLUG_IN_ID);
        String version = ResourceUtility.getExternalVersion(ObservationStarter.DEPENDENT_PLUG_INS);
        setDescription("Version = " + version);
        instance_ = this;
    }

    public static ObservationPreferencePage getInstance()
    {
        return instance_;
    }

    public boolean isActivated()
    {
        return preferences_.get(QF_OBSERVATION_PLUGIN_ACTIVATED, true);
    }
    
    public boolean isSnapshotActivated()
    {
        return isActivated() && preferences_.get(QF_OBSERVATION_SNAPSHOT_ACTIVATED, false);
    }

    public boolean shouldSendLogs()
    {
        return preferences_.getBoolean(QF_OBSERVATION_SEND_LOGS_PERIODICALLY);
    }
    
    public boolean shouldSkipConfirmingSendLogs()
    {
        return preferences_.getBoolean(QF_OBSERVATION_SKIP_SEND_LOGS_CONFIRMATION);
    }

    public boolean shouldSkipConfirmingSnapshot()
    {
        return preferences_.getBoolean(QF_OBSERVATION_SKIP_SNAPSHOT_CONFIRMATION);
    }

    /*
     * @see IPreferencePage#performOk()
     */
    public boolean performOk()
    {
        boolean wasActivated = preferences_.getBoolean(QF_OBSERVATION_PLUGIN_ACTIVATED);
        boolean isActivated = observationActivatedButton_.getSelection();
        boolean result = super.performOk();
        saveChanges();
        if (wasActivated && !isActivated)
            // plug-in is deactivated, kill the current observation.
            Observer.getUsageObserver().stopObservation();
        else if (!wasActivated && isActivated)
        {
            // plug-in is activated, start a new observation.
            try
            {
                IFile initialFile = EclipseUIUtility.getActiveEditorFile();
                if (initialFile != null)
                {
                    IProject project = initialFile.getProject();
                    Observer.getUsageObserver().observeProject(project);
                }
            }
            catch (NotInitializedException e)
            {
                // This really should not happen!
                logger.log(Level.SEVERE, "Could not retrieve the active file!", e);
            }
        }
        return result;
    }

    @Override
    public void init(IWorkbench workbench)
    {}

    @Override
    protected Control createContents(Composite parent)
    {
        Group generalSettingsGroup = PreferencePageUtility.createVerticalGroup(parent,
        "General Settings");
        Composite generalSettingsComposite = new Composite(generalSettingsGroup, SWT.LEFT);
        observationActivatedButton_ = PreferencePageUtility.createCheckBox(generalSettingsComposite, "Activate Observation Feature");
        Button observationCheckVersionButton = PreferencePageUtility.createButton(generalSettingsComposite, "Check For Updates");
        observationCheckVersionButton.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                ResourceUtility.checkForUpdates("Observer", true, ObservationStarter.DEPENDENT_PLUG_INS);
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });
        PreferencePageUtility.makeHorizontalComposite(generalSettingsComposite, observationActivatedButton_, observationCheckVersionButton);
        
        Composite snapshotComposite = new Composite(generalSettingsGroup, SWT.LEFT);
        snapshotActivatedButton_ = PreferencePageUtility.createCheckBox(snapshotComposite, "Activate Snapshotting Feature");
        PreferencePageUtility.makeHorizontalComposite(snapshotComposite, snapshotActivatedButton_);
        
        Group logGroup = PreferencePageUtility.createVerticalGroup(parent, "Log Settings");
        // Create label for log information.
        PreferencePageUtility.createLabel(logGroup, "Most recent logs can be accessed in the following path:");
        // Create text field for log paths.
        Composite usageLogComposite = new Composite(logGroup, SWT.LEFT);
        Label usageLogLabel = PreferencePageUtility.createLabel(usageLogComposite, "Usage Log: ");
        Text usageLogText = PreferencePageUtility.createText(usageLogComposite, ObservationLogger.LOG_PATH, false);
        PreferencePageUtility.makeHorizontalComposite(usageLogComposite, usageLogLabel, usageLogText);
        Composite debugLogComposite = new Composite(logGroup, SWT.LEFT);
        Label debugLogLabel = PreferencePageUtility.createLabel(debugLogComposite, "Debug Log: ");
        Text debugLogText = PreferencePageUtility.createText(debugLogComposite, LogHandlers.logPath_, false);
        PreferencePageUtility.makeHorizontalComposite(debugLogComposite, debugLogLabel, debugLogText);
        // Install send logs periodically field.
        sendLogsPeriodicallyEditor_ = PreferencePageUtility.createCheckBox(logGroup, "Send Logs Periodically");
        // Install send logs now button.
        Button sendLogsNowButton = PreferencePageUtility.createButton(logGroup, "Send Logs Now");
        sendLogsNowButton.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                ObservationLogSender logSender = new ObservationLogSender();
                logSender.sendLogs();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });
        PreferencePageUtility.makeControlVisible(logGroup);

        loadInitialValues();
        return parent;
    }

    private void saveChanges()
    {
        preferences_.put(QF_OBSERVATION_SNAPSHOT_ACTIVATED, snapshotActivatedButton_.getSelection());
        preferences_.put(QF_OBSERVATION_PLUGIN_ACTIVATED, observationActivatedButton_.getSelection());
        preferences_.put(QF_OBSERVATION_SEND_LOGS_PERIODICALLY, sendLogsPeriodicallyEditor_.getSelection());
        preferences_.save();
    }

    private void loadInitialValues()
    {
        snapshotActivatedButton_.setSelection(preferences_.get(QF_OBSERVATION_SNAPSHOT_ACTIVATED, false));
        observationActivatedButton_.setSelection(preferences_.get(QF_OBSERVATION_PLUGIN_ACTIVATED, true));
        sendLogsPeriodicallyEditor_.setSelection(preferences_.getBoolean(QF_OBSERVATION_SEND_LOGS_PERIODICALLY));
    }
    
    public boolean getPreferenceValue(String preferenceIdentifier)
    {
        return preferences_.getBoolean(preferenceIdentifier);
    }

    public void activatePreference(String preferenceIdentifier)
    {
        setPreference(preferenceIdentifier, true);
    }

    public void deactivate(String preferenceIdentifier)
    {
        setPreference(preferenceIdentifier, false);
    }
    
    private void setPreference(String preferenceIdentifier, boolean value)
    {
        preferences_.put(preferenceIdentifier, value);
        preferences_.save();
    }
}
