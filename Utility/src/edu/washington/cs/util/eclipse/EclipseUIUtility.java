package edu.washington.cs.util.eclipse;

import javax.swing.JOptionPane;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import edu.washington.cs.swing.KDialog;
import edu.washington.cs.util.exception.NotInitializedException;

/**
 * This utility class provides static helper methods for handling Eclipse UI requests. <br>
 * Currently offered functionality is:
 * <ul>
 * <li>Getting the selected file on the active editor after the Eclipse UI is created.</li>
 * <li>Getting the selected file on the active editor after the Eclipse UI is created using Eclipse UI thread.</li>
 * </ul>
 * 
 * @author Kivanc Muslu
 */
public class EclipseUIUtility
{
    /**
     * Retrieves and returns the currently active and selected file in the Eclipse editor if any, <code>null</code>
     * otherwise. <br>
     * This method must be called by an non-Eclipse UI thread.
     * 
     * @return The currently active and selected file in the Eclipse editor if any, <code>null</code> otherwise.
     * @throws NotInitializedException If the Eclipse UI and editors are not created yet.
     */
    public static IFile getActiveEditorFileInUIThread() throws NotInitializedException
    {
        ActiveFileRetriever retriever = new ActiveFileRetriever();
        Display.getDefault().syncExec(retriever);
        return retriever.getEditorFile();
    }

    /**
     * Retrieves the current file in Eclipse editor.
     * 
     * @author Kivanc Muslu
     */
    private static class ActiveFileRetriever extends Thread
    {
        /**
         * Active editor file in Eclipse.
         */
        private IFile eclipseEditorFile_ = null;

        /**
         * Indicates whether the editor file is retrieved by success or not.
         */
        private boolean fileRetrievedBySuccess_ = true;

        public void run()
        {
            try
            {
                eclipseEditorFile_ = getActiveEditorFile();
            }
            catch (NotInitializedException e)
            {
                fileRetrievedBySuccess_ = false;
            }
        }

        /**
         * Returns the editor file that is retrieved at the moment this thread is run if the retrieval is successful,
         * throws {@link NotInitializedException} otherwise.
         * 
         * @return The editor file that is retrieved at the moment this thread is run.
         * @throws NotInitializedException If the retrieval fails.
         */
        public IFile getEditorFile() throws NotInitializedException
        {
            if (!fileRetrievedBySuccess_)
                throw new NotInitializedException("There is no page loaded yet.");
            return eclipseEditorFile_;
        }
    }

    /**
     * Retrieves and returns the currently active and selected file in the Eclipse editor if any, <code>null</code>
     * otherwise. <br>
     * This method must be called by the Eclipse UI thread.
     * 
     * @return The currently active and selected file in the Eclipse editor if any, <code>null</code> otherwise.
     * @throws NotInitializedException If the Eclipse UI and editors are not created yet.
     */
    public static IFile getActiveEditorFile() throws NotInitializedException
    {
        IEditorPart editor = getActiveEditorPart();
        if (editor == null)
            return null;
        else
        {
            String path = "/" + editor.getTitleToolTip();
            return ResourceUtility.getFile(new Path(path));
        }
    }

    /**
     * Closes the active editor in Eclipse. <br>
     * This method must be called by the Eclipse UI thread.
     * 
     * @throws NotInitializedException If the Eclipse UI and editors are not created yet.
     */
    public static void closeActiveEditor() throws NotInitializedException
    {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page == null)
            throw new NotInitializedException("There is no page loaded yet.");
        IEditorPart editor = page.getActiveEditor();
        if (editor != null)
            page.closeEditor(editor, false);
    }

    /**
     * Opens the given file in a new Eclipse editor.
     * 
     * @param file The file that will be opened in a new editor.
     * @throws NotInitializedException If the Eclipse UI and editors are not created yet.
     * @throws PartInitException If the Eclipse UI thread cannot open the given file in a new editor.
     */
    public static void openFileInEditor(IFile file) throws NotInitializedException, PartInitException
    {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page == null)
            throw new NotInitializedException("There is no page loaded yet.");
        IDE.openEditor(page, file);
    }

    /**
     * Saves all opened Eclipse editors.
     * 
     * @param confirm If <code>true</code>, the UI will ask for confirmation before saving, otherwise the save is done
     *            without user interaction.
     */
    public static void saveAllEditors(final boolean confirm)
    {
        Display.getDefault().syncExec(new Thread()
        {
            public void run()
            {
                PlatformUI.getWorkbench().saveAllEditors(confirm);
            }
        });
    }

    /**
     * Displays the given message to the user as 'information' using Swing components.
     * 
     * @param message Message that will be displayed.
     * @param title Title that will be used.
     * @param width The width of the dialog that will be created.
     */
    public static void showInformationDialog(final String message, final String title, final int width)
    {
        showMessageDialog(message, title, JOptionPane.INFORMATION_MESSAGE, width);
    }

    /**
     * Displays the given message to the user as 'error' using Swing components.
     * 
     * @param message Message that will be displayed.
     * @param title Title that will be used.
     * @param width The width of the dialog that will be created.
     */
    public static void showErrorDialog(final String message, final String title, final int width)
    {
        showMessageDialog(message, title, JOptionPane.ERROR_MESSAGE, width);
    }

    /**
     * Shows an option dialog to the user and returns the option that is selected by the user. The dialog is created
     * using Swing components.
     * 
     * @param message Message to be displayed.
     * @param title Title to be used.
     * @param width Width of the option dialog.
     * @param options Options to be displayed.
     * @return The option that is selected by the user.
     */
    public static Object showOptionDialog(final String message, final String title, final int width,
            final Object [] options)
    {
        OptionDialogCreator dialogCreator = new OptionDialogCreator(message, title, width, options);
        dialogCreator.start();
        return dialogCreator.getSelectedOption();
    }

    /**
     * Shows an option dialog to the user using Swing components and stores the result selected by the user.
     * 
     * @author Kivanc Muslu
     */
    private static class OptionDialogCreator extends Thread
    {
        /** The selection made by the user. */
        private Object optionDialogResult_;

        /** Message to be displayed. */
        private final String message_;
        /** Title to be used. */
        private final String title_;
        /** Width of the option dialog. */
        private final int width_;
        /** Options to be displayed. */
        private final Object [] options_;

        /**
         * Creates an Option Dialog Creator.
         * 
         * @param message Message to be displayed.
         * @param title Title to be used.
         * @param width Width of the option dialog.
         * @param options Options to be displayed.
         */
        public OptionDialogCreator(String message, String title, int width, Object [] options)
        {
            message_ = message;
            title_ = title;
            width_ = width;
            options_ = options;

            optionDialogResult_ = null;
        }

        public void run()
        {
            optionDialogResult_ = KDialog.showOptionDialog(null, message_, title_, width_, options_);
        }

        /**
         * Returns the selected option by the user. The thread must be run before calling this method.
         * 
         * @return The selected option by the user.
         */
        public Object getSelectedOption()
        {
            try
            {
                join();
            }
            catch (InterruptedException e)
            {}
            return optionDialogResult_;
        }
    }

    /**************************************
     ************ PRIVATE API *************
     **************************************/
    /**
     * Returns the active editor part in Eclipse.
     * 
     * @return The active editor part in Eclipse.
     * @throws NotInitializedException If the Eclipse UI and editors are not created yet.
     */
    private static IEditorPart getActiveEditorPart() throws NotInitializedException
    {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page == null)
            throw new NotInitializedException("There is no page loaded yet.");
        return page.getActiveEditor();
    }

    /**
     * Displays the given message to the user using Swing components.
     * 
     * @param message Message that will be displayed.
     * @param title Title that will be used.
     * @param type The type of the dialog that will be created.
     * @param width The width of the dialog that will be created.
     */
    private static void showMessageDialog(final String message, final String title, final int type, final int width)
    {
        new Thread()
        {
            public void run()
            {
                KDialog.showDialog(null, message, title, type, width);
            }
        }.start();
    }
}
