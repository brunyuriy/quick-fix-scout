package edu.washington.cs.quickfix.speculation.gui;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import edu.washington.cs.quickfix.speculation.SpeculationStarter;
import edu.washington.cs.quickfix.speculation.Speculator;
import edu.washington.cs.util.eclipse.EclipseUIUtility;
import edu.washington.cs.util.eclipse.PreferencePageUtility;
import edu.washington.cs.util.eclipse.PreferencesUtility;
import edu.washington.cs.util.eclipse.ResourceUtility;
import edu.washington.cs.util.exception.NotInitializedException;

/**
 * This class is written mostly by imitating the methods in: org.eclipse.jdt.internal.debug.ui.JavaDebugPreferencePage
 * org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage
 * org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage
 * 
 * @author kivanc
 */
public class SpeculationPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage
{
    private IntegerFieldEditor typingSessionLengthField_;
    private Button speculationDisabledButton_;
    private Button speculationEnabledButton_;
    private Button speculationAugmentedButton_;
    private final PreferencesUtility preferences_;
    public static final String QF_SPECULATION_TYPING_SESSION_LENGTH = "QF Speculation Typing Session Time";
    public static final String QF_SPECULATION_DISABLED = "QF Speculation Disabled";
    public static final String QF_SPECULATION_ENABLED = "QF Speculation Enabled";
    public static final String QF_SPECULATION_AUGMENTED = "QF Speculation Augmented";
    public static SpeculationPreferencePage instance_ = new SpeculationPreferencePage();
    
    private static final int DEFAULT_TYPING_SESSION_LENGTH = 2000;
    
    private static final Logger logger = Logger.getLogger(SpeculationPreferencePage.class.getName());

    public SpeculationPreferencePage()
    {
        String version = ResourceUtility.getExternalVersion(SpeculationStarter.DEPENDENT_PLUG_INS);
        setDescription("Version = " + version);
        preferences_ = new PreferencesUtility(Speculator.PLUG_IN_ID);
        instance_ = this;
    }
    
    public static SpeculationPreferencePage getInstance()
    {
        return instance_;
    }
    
    public int getTypingSessionLength()
    {
        return preferences_.get(QF_SPECULATION_TYPING_SESSION_LENGTH, DEFAULT_TYPING_SESSION_LENGTH);
    }
    
    public boolean isActivated()
    {
        return preferences_.getBoolean(QF_SPECULATION_AUGMENTED) || preferences_.getBoolean(QF_SPECULATION_ENABLED);
    }
    
    public boolean isAugmentationActivated()
    {
        return preferences_.getBoolean(QF_SPECULATION_AUGMENTED);
    }

    /*
     * @see PreferencePage#performDefaults()
     */
    protected void performDefaults()
    {
        super.performDefaults();
        typingSessionLengthField_.setStringValue(DEFAULT_TYPING_SESSION_LENGTH + "");
        speculationDisabledButton_.setSelection(true);
        speculationEnabledButton_.setSelection(false);
        speculationAugmentedButton_.setSelection(false);
    }

    /*
     * @see IPreferencePage#performOk()
     */
    public boolean performOk()
    {
        boolean validationResult = validate();
        if (!validationResult)
            return false;
        boolean wasActivated = preferences_.getBoolean(QF_SPECULATION_ENABLED) || preferences_.getBoolean(QF_SPECULATION_AUGMENTED);
        boolean isActivated = speculationAugmentedButton_.getSelection() || speculationEnabledButton_.getSelection();
        boolean result = super.performOk();
        saveChanges();
        Speculator.getSpeculator().updateTypingSessionTime(getTypingSessionLength());
        if (wasActivated && !isActivated)
            // plug-in is deactivated, kill the current speculative analysis.
            Speculator.getSpeculator().stopSpeculation();
        else
        {
            try
            {
                IFile initialFile = EclipseUIUtility.getActiveEditorFile();
                if (initialFile != null && !wasActivated && isActivated)
                    // plug-in is activated, start a new speculative analysis.
                    Speculator.getSpeculator().speculateProject(initialFile);
                else if (wasActivated && !isActivated)
                    // plug-in is deactivated, stop the current analysis.
                    Speculator.getSpeculator().stopSpeculation();
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
    protected void createFieldEditors()
    {
        // Install activate/deactivate fields.
        Group activationGroup = PreferencePageUtility.createVerticalGroup(getFieldEditorParent(), "Activation Settings");
        PreferencePageUtility.createLabel(activationGroup,"Before enabling Evaluation Features, please make sure that Observation Feature is enabled.");
        speculationDisabledButton_ = PreferencePageUtility.createRadioButton(activationGroup, "Disable Evaluation Feature");
        speculationEnabledButton_ = PreferencePageUtility.createRadioButton(activationGroup, "Enable Evaluation Feature (speculative analysis is done only for logging, nothing is shown to the user).");
        speculationAugmentedButton_ = PreferencePageUtility.createRadioButton(activationGroup, "Enable Quick Fix Scout (speculative analysis is done for both logging and updating the quick fix dialog).");
        Button speculationVersionCheckButton = PreferencePageUtility.createButton(activationGroup, "Check For Updates");
        speculationVersionCheckButton.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                ResourceUtility.checkForUpdates("Speculator", true, SpeculationStarter.DEPENDENT_PLUG_INS);
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });
        PreferencePageUtility.makeControlVisible(activationGroup);
        // Install typing session field.
        typingSessionLengthField_ = new IntegerFieldEditor("Typing session field editor",
                "Pause Before Computation (in ms):", getFieldEditorParent());
        installIntegerFieldEditor(typingSessionLengthField_, QF_SPECULATION_TYPING_SESSION_LENGTH, 1,
                Integer.MAX_VALUE, "Typing session length must be a positive integer.");
        
        loadInitialValues();
    }
    
    private void loadInitialValues()
    {
        typingSessionLengthField_.setStringValue(preferences_.get(QF_SPECULATION_TYPING_SESSION_LENGTH, DEFAULT_TYPING_SESSION_LENGTH) + "");
        speculationDisabledButton_.setSelection(preferences_.get(QF_SPECULATION_DISABLED, true));
        speculationEnabledButton_.setSelection(preferences_.getBoolean(QF_SPECULATION_ENABLED));
        speculationAugmentedButton_.setSelection(preferences_.getBoolean(QF_SPECULATION_AUGMENTED));
    }

    private void installIntegerFieldEditor(IntegerFieldEditor integerFieldEditor, String preferenceName, int minValue,
            int maxValue, String errorMessage)
    {
        integerFieldEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);
        integerFieldEditor.setValidRange(minValue, maxValue);
        integerFieldEditor.setErrorMessage(errorMessage);
        installFieldEditor(integerFieldEditor, preferenceName);
    }

    private void installFieldEditor(FieldEditor fieldEditor, String preferenceName)
    {
        fieldEditor.setPreferenceName(preferenceName);
        addField(fieldEditor);
    }
    
    private void saveChanges()
    {
      preferences_.put(QF_SPECULATION_TYPING_SESSION_LENGTH, typingSessionLengthField_.getIntValue());
      preferences_.put(QF_SPECULATION_AUGMENTED, speculationAugmentedButton_.getSelection());
      preferences_.put(QF_SPECULATION_ENABLED, speculationEnabledButton_.getSelection());
      preferences_.put(QF_SPECULATION_DISABLED, speculationDisabledButton_.getSelection());
      preferences_.save();
    }

    private boolean validate()
    {
        int typingSessionLength = Integer.MIN_VALUE;
        try
        {
            typingSessionLength = typingSessionLengthField_.getIntValue();
        }
        catch (NumberFormatException e)
        {}
        if (typingSessionLength > 0)
            // Spawn a thread and change the main project that we are speculating on.
            return true;
        else
        {
            String errorMessage = "";
            if (typingSessionLength == Integer.MIN_VALUE)
                errorMessage += "Typing session length must be a positive integer value.\n";
            else if (typingSessionLength <= 0)
                errorMessage += "Typing session length must be positive.\n";
            errorMessage = errorMessage.substring(0, errorMessage.length() - 1);
            setErrorMessage(errorMessage);
            return false;
        }
    }
}
