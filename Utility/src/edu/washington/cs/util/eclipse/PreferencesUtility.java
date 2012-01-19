package edu.washington.cs.util.eclipse;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PreferencesUtility
{
    private static final Logger logger = Logger.getLogger(PreferencesUtility.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    
    private final Preferences prefs_;
    
    @SuppressWarnings("deprecation")
    public PreferencesUtility(String pluginId)
    {
        prefs_ = new InstanceScope().getNode(pluginId);
    }
    
    public boolean get(String preferenceName, boolean defaultValue)
    {
        return prefs_.getBoolean(preferenceName, defaultValue);
    }
    
    public boolean getBoolean(String preferenceName)
    {
        return prefs_.getBoolean(preferenceName, false);
    }

    public int get(String preferenceName, int defaultValue)
    {
        return prefs_.getInt(preferenceName, defaultValue);
    }

    public void put(String key, boolean value)
    {
        prefs_.putBoolean(key, value);
    }

    public void save()
    {
        try
        {
            prefs_.flush();
        }
        catch (BackingStoreException e)
        {
            logger.log(Level.SEVERE, "While trying to save preferences for Observer plug-in an error occured.", e);
        }
    }

    public void put(String key, int value)
    {
        prefs_.putInt(key, value);
    }

}
