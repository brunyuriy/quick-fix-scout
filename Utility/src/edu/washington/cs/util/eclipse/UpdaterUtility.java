package edu.washington.cs.util.eclipse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JCheckBox;

import org.eclipse.core.runtime.Platform;

import edu.washington.cs.swing.SwingUtility;

public class UpdaterUtility
{
    /** Logger for debugging. */
    private static Logger logger_ = Logger.getLogger(UpdaterUtility.class.getName());
    static
    {
        logger_.setLevel(Level.INFO);
    }

    /** Unique plug-in id. */
    public static final String PLUG_IN_ID = "edu.washington.cs.util";
    /** Preferences to store skipping the update detection or not. */
    private static final PreferencesUtility preferences_ = new PreferencesUtility(PLUG_IN_ID);
    /** Preference string for skipping the update detection. */
    public static final String SKIP_UPDATE_DETECTION = "QF Resource Skip Update Detection";

    /**
     * This class cannot be instantiated.
     */
    private UpdaterUtility()
    {}

    /**
     * Returns the plug-in version for the plug-in identified with the given 'pluginID'.
     * 
     * @param pluginId The identifier for the target plug-in.
     * @return The plug-in version for the plug-in identified with the given 'pluginID'.
     */
    public static Object getPluginVersion(String pluginId)
    {
        return Platform.getBundle(pluginId).getHeaders().get("Bundle-Version");
    }

    /**
     * Logs the system information (that contains the OS name and plug-in versions) for the plug-ins that are
     * represented by the given 'pluginIds'.
     * 
     * @param pluginIds A set of identifiers representing the target plug-ins.
     */
    public static void logSystemInformation(String... pluginIds)
    {
        String os = System.getProperty("os.name");
        logger_.info("=== System information ===");
        logger_.info("OS name = " + os);
        for (String pluginId: pluginIds)
            logger_.info(pluginId + " v." + getPluginVersion(pluginId));
        logger_.info("=== System information ===");
    }

    /**
     * Computes and returns the externally visible version string for the plug-ins represented by the given
     * 'pluginsIds'.
     * 
     * @param pluginIds A set of identifiers representing the target plug-ins.
     * @return The externally visible version string for the plug-ins represented by the given 'pluginsIds'.
     */
    public static String getExternalVersion(String... pluginIds)
    {
        String [] internalVersions = new String [pluginIds.length];
        for (int a = 0; a < internalVersions.length; a++)
            internalVersions[a] = (String) getPluginVersion(pluginIds[a]);
        return createExternalVersion(internalVersions);
    }

    /**
     * Computes and returns the externally visible version string for the given internal versions for a bunch of
     * plug-ins.
     * 
     * @param internalVersions Internal version for a list of plug-ins.
     * @return The externally visible version string for the given internal versions for a bunch of plug-ins.
     */
    private static String createExternalVersion(String [] internalVersions)
    {
        int major = 0;
        int minor = 0;
        int micro = 0;
        for (String externalVersion: internalVersions)
        {
            String [] parts = externalVersion.split("\\.");
            if (parts.length != 3)
            {
                logger_.severe("Malformed version number for string: " + externalVersion);
                for (String part: parts)
                    logger_.severe(part);
                throw new RuntimeException("External Version ID cannot be calculated.");
            }
            major += Integer.parseInt(parts[0].trim());
            minor += Integer.parseInt(parts[1].trim());
            micro += Integer.parseInt(parts[2].trim());
        }
        double majorAvg = major * 1.0 / internalVersions.length;
        double minorAvg = minor * 1.0 / internalVersions.length;
        double microAvg = micro * 1.0 / internalVersions.length;
        String result = String.format("%.2f:%.2f:%.2f", majorAvg, minorAvg, microAvg);
        return result;
    }

    /**
     * Checks for the updates for the plug-in represented with the given 'pluginName' that consists the plug-ins
     * represented by 'pluginIds'
     * 
     * @param pluginName The name of the main plug-in (kind of feature).
     * @param showNetworkError If there is a network error while checking for updates, if this argument is
     *            <code>true</code>, then the error is shown to the user in a dialog. Otherwise, nothing is shown to the
     *            user even if the check cannot be completed.
     * @param pluginIds A set of identifiers representing the target plug-ins.
     */
    public static void checkForUpdates(String pluginName, boolean showNetworkError, String... pluginIds)
    {
        // Open only for debugging.
        // preferences_.put(SKIP_UPDATE_DETECTION, false);
        // preferences_.save();

        boolean skipUpdate = preferences_.getBoolean(SKIP_UPDATE_DETECTION);
        if (skipUpdate)
            return;

        String installedVersion = getExternalVersion(pluginIds);
        HashMap <String, String> versionMap = new HashMap <String, String>();
        readVersionMap(showNetworkError, versionMap);
        boolean recent = true;
        ArrayList <String> relatedVersions = new ArrayList <String>();
        for (String pluginId: pluginIds)
        {
            String currentVersion = versionMap.get(pluginId);
            Object version = getPluginVersion(pluginId);
            relatedVersions.add(currentVersion);
            if (currentVersion != null && !version.equals(currentVersion))
                recent = false;
        }
        if (recent)
        {
            if (showNetworkError)
                EclipseUIUtility.showInformationDialog("You are already using the most recent version.",
                        "No New Version Avaliable", 300);
            logger_.info("User is using the final version (" + installedVersion + ") for " + pluginName + " plug-in");
        }
        else
        {
            String externalName = pluginName;
            if (pluginName.equals("Speculator"))
                externalName = "Evaluator";
            String currentVersion = createExternalVersion(relatedVersions.toArray(new String [relatedVersions.size()]));
            JCheckBox neverRemindBox = new JCheckBox("Don't remind again");
            Object [] options = new Object [] {"Okay", neverRemindBox};
            //@formatter:off
            EclipseUIUtility.showOptionDialog("<div align=left>Quick Fix Scout plug-in (" + externalName + " feature) is outdated.<br>" +
                    "A new version is available at: " + SwingUtility.makeHyperlink("http://code.google.com/p/quick-fix-scout/downloads/list") +
                    "<br><br>Installed version = " + installedVersion + ", current version = " + currentVersion + "</div>"
                    , "New Version Available!", 450, options);
            //@formatter:on
            if (neverRemindBox.isSelected())
            {
                preferences_.put(SKIP_UPDATE_DETECTION, true);
                preferences_.save();
            }
        }
    }

    private static void readVersionMap(boolean showError, HashMap <String, String> versionMap)
    {
        try
        {
            URL versionURL = new URL(ResourceUtility.VERSION_URL);
            Scanner reader = new Scanner(versionURL.openStream());
            while (reader.hasNext())
            {
                String line = reader.nextLine();
                String [] parts = line.split(" - ");
                if (parts.length != 2)
                    logger_.finer("Malformed version information line for = " + line);
                else
                    versionMap.put(parts[0].trim(), parts[1].trim());
            }
            reader.close();
        }
        catch (MalformedURLException e)
        {
            assert false: "The version URL cannot be malformed!";
        }
        catch (IOException e)
        {
            if (showError)
                //@formatter:off
                EclipseUIUtility.showErrorDialog("The plug-in cannot check version information from server<br>"
                                + "Please make sure that you are connected to the Internet and try again.",
                        "Cannot Check for Updates", 300);
            //@formatter:on
        }
    }
}
