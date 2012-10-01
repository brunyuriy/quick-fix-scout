package edu.washington.cs.util.eclipse;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PreferencesUtility
{
    /** Logger for debugging. */
    private static final Logger logger = Logger.getLogger(PreferencesUtility.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }

    /** Preferences to store key-value pairs. */
    private final Preferences prefs_;

    /**
     * Constructs a preference utility for the plug-in identified with the given plug-in id.
     * 
     * @param pluginId The identifier for the input plug-in.
     */
    @SuppressWarnings("deprecation")
    public PreferencesUtility(String pluginId)
    {
        prefs_ = new InstanceScope().getNode(pluginId);
    }

    /**
     * Deletes any preferences associated with this utility.
     */
    public void dispose()
    {
        try
        {
            prefs_.removeNode();
        }
        catch (BackingStoreException e)
        {
            logger.log(Level.SEVERE, "While trying to delete preferences, an error occured.", e);
        }
    }

    /**
     * @param preferenceName The name of the preference.
     * @param defaultValue Default value that will be returned if the given preference does not exist.
     * @return The value of the preference represented by 'preferenceName'.
     */
    public boolean get(String preferenceName, boolean defaultValue)
    {
        return prefs_.getBoolean(preferenceName, defaultValue);
    }

    /**
     * @param preferenceName The name of the preference.
     * @param defaultValue Default value that will be returned if the given preference does not exist.
     * @return The value of the preference represented by 'preferenceName'.
     */
    public int get(String preferenceName, int defaultValue)
    {
        return prefs_.getInt(preferenceName, defaultValue);
    }

    /**
     * @param preferenceName The name of the preference.
     * @return The value of the preference represented by 'preferenceName'. <br>
     *         Returns <code>false</code> if the given preference does not exist.
     */
    public boolean getBoolean(String preferenceName)
    {
        return prefs_.getBoolean(preferenceName, false);
    }

    /**
     * Stores the preference represented with the given key-value pair. 
     * 
     * @param key The name of the preference.
     * @param value The value of the preference.
     */
    public void put(String key, boolean value)
    {
        prefs_.putBoolean(key, value);
    }

    /**
     * Stores the preference represented with the given key-value pair. 
     * 
     * @param key The name of the preference.
     * @param value The value of the preference.
     */
    public void put(String key, int value)
    {
        prefs_.putInt(key, value);
    }

    /**
     * Removes the preference represented with the 'preferenceName'.
     * @param preferenceName The name of the preference. 
     */
    public void remove(String preferenceName)
    {
        prefs_.remove(preferenceName);
    }

    /**
     * Makes the current preference store permanent.
     */
    public void save()
    {
        try
        {
            prefs_.flush();
        }
        catch (BackingStoreException e)
        {
            logger.log(Level.SEVERE, "While trying to save preferences, an error occured.", e);
        }
    }
}
