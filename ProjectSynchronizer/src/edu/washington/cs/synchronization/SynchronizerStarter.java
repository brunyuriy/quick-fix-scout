package edu.washington.cs.synchronization;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import edu.washington.cs.swing.KDialog;
import edu.washington.cs.synchronization.sync.SynchronizerFileBufferListener;
import edu.washington.cs.synchronization.sync.SynchronizerPartListener;
import edu.washington.cs.synchronization.sync.SynchronizerResourceChangeListener;
import edu.washington.cs.util.eclipse.PreferencesUtility;

/**
 * {@link IStartup} implementation for Project Synchronizer plug-in. <br>
 * This class includes the code that needs to execute as soon as Eclipse UI (workbench) is created.
 * 
 * @author Kivanc Muslu
 */
public class SynchronizerStarter implements IStartup
{
    private static final String WELCOME_MESSAGE_ID = "Quick Fix Scout Alpha 5";
    /**
     * variable that indicates whether the global listener are added or not. <br>
     * Currently there are 2 global listeners: one {@link IResourceChangeListener} and one {@link IPartListener2}.
     */
    private volatile static boolean globalListenersAdded_ = false;

    /**
     * {@inheritDoc}
     * <p>
     * Creates and installs workspace wide listeners.
     * </p>
     */
    @Override
    public void earlyStartup()
    {
        initGlobalListeners();
    }

    public static void initGlobalListeners()
    {
        if (!globalListenersAdded_)
        {
            Display.getDefault().syncExec(new Thread()
            {
                public void run()
                {
                    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    // manageWorkingSets(window);
                    initPartListener(window);
                }
            });
            initResourceListener();
            initFileBufferListener();
            showWelcomeMessageUsingJava();
            globalListenersAdded_ = true;
        }
    }

    // TODO There is no icon for the welcome screen.
    private static void showWelcomeMessageUsingJava()
    {
        PreferencesUtility prefs_ = new PreferencesUtility(ProjectSynchronizer.PLUG_IN_ID);
        // Open the comment for testing.
//         prefs_.put(WELCOME_MESSAGE_ID, false);
        boolean welcome = prefs_.getBoolean(WELCOME_MESSAGE_ID);
        if (!welcome)
        {
            String ls = "<br>";
            //@formatter:off
            /*
             * Actual message: 
             * Thank you for installing QuickFixScout, developed by Kivanc Muslu.
             * 
             * To enable or disable features, use the preferences menu.
             * QuickFixScout will create several extra projects (names will start with DO_NOT_DELETE) in your workspace. Please don't touch these projects.  I recommend you switch to "working sets view" for this reason.  
             * 
             * For more information about the plug-in, or to give feedback, please visit: <website>.
             * Please note that this message will only show once for each of your Workspaces.
             */
            String message = "Thank you for installing Quick Fix Scout, developed by Kivanc Muslu." + ls + ls
            + "- To enable or disable features, use the preferences menu." + ls
            + "- Quick Fix Scout will create several extra projects (names will start with <b>DO_NOT_DELETE</b>) in your workspace"
            + ", which are hidden by default. Please <b>don't touch</b> these projects. ." + ls + ls
            + "For more information about the plug-in, or to give feedback, please visit: " + ls + 
            "http://www.kivancmuslu.com/Quick_Fix_Scout" + ls
            + "<b>Note: </b> This message will only show <b>once</b> for each of your Workspaces."
            ;
            // messagePane.setIcon(null);
            KDialog.showInformation(null, message, "Welcome", 500);
            //@formatter:on
            prefs_.put(WELCOME_MESSAGE_ID, true);
            prefs_.save();
        }
    }

    @SuppressWarnings("unused")
    private static void showWelcomeMessage(IWorkbenchWindow window)
    {
        PreferencesUtility prefs_ = new PreferencesUtility(ProjectSynchronizer.PLUG_IN_ID);
        // prefs_.put(WELCOME_MESSAGE_ID, false);
        boolean welcome = prefs_.getBoolean(WELCOME_MESSAGE_ID);
        if (!welcome)
        {
            String ls = System.getProperty("line.separator");
            //@formatter:off
            /*
             * Actual message: 
             * Thank you for installing QuickFixScout, developed by Kivanc Muslu.
             * 
             * To enable or disable features, use the preferences menu.
             * QuickFixScout will create several extra projects (names will start with DO_NOT_DELETE) in your workspace. Please don't touch these projects.  I recommend you switch to "working sets view" for this reason.  
             * 
             * For more information about the plug-in, or to give feedback, please visit: <website>.
             * Please note that this message will only show once for each of your Workspaces.
             */
            String message = "Thank you for installing QuickFixScout, developed by Kivanc Muslu." + ls + ls 
            + "To enable or disable features, use the preferences menu." + ls 
            + "QuickFixScout will create several extra projects (names will start with DO_NOT_DELETE) in your workspace. " +
            "Please don't touch these projects.  I recommend you switch to \"working sets view\" for this reason" + ls + ls
            + "For more information about the plug-in, or to give feedback, please visit: <website>." + ls
            + "Please note that this message will only show once for each of your Workspaces."
            ;
            MessageDialog.openInformation(window.getShell(), "Welcome", message);
            //@formatter:on
            prefs_.put(WELCOME_MESSAGE_ID, true);
            prefs_.save();
        }
    }

    /**
     * Creates and installs a part listener for the active page in the workbench. <br>
     * Should only be called once and after org.eclipse.ui plug-in is completely loaded.
     * 
     * @param window
     */
    private static void initPartListener(IWorkbenchWindow window)
    {
        IWorkbenchPage page = window.getActivePage();
        page.addPartListener(new SynchronizerPartListener());
    }

    private static void initResourceListener()
    {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(new SynchronizerResourceChangeListener());
    }

    private static void initFileBufferListener()
    {
        IFileBuffer [] buffers = FileBuffers.getTextFileBufferManager().getFileBuffers();
        FileBuffers.getTextFileBufferManager().addFileBufferListener(new SynchronizerFileBufferListener());
        for (IFileBuffer buffer: buffers)
            SynchronizerFileBufferListener.attachListenerToBuffer(buffer);
    }
}
