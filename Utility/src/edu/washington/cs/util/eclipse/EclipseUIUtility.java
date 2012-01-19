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
    /**************
     * PUBLIC API *
     *************/
    /**
     * Retrieves and returns the currently active and selected file in the Eclipse editor if any, <code>null</code>
     * otherwise. <br>
     * This method must be called by the Eclipse UI thread.
     * 
     * @return The currently active and selected file in the Eclipse editor if any, <code>null</code> otherwise.
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
    
    private static IEditorPart getActiveEditorPart() throws NotInitializedException
    {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page == null)
            throw new NotInitializedException("There is no page loaded yet.");
        return page.getActiveEditor();
    }
    
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
    
    public static void closeActiveEditor() throws NotInitializedException
    {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page == null)
            throw new NotInitializedException("There is no page loaded yet.");
        IEditorPart editor = page.getActiveEditor();
        if (editor != null)
            page.closeEditor(editor, false);
    }
    
    public static void openFileInEditor(IFile file) throws NotInitializedException, PartInitException
    {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page == null)
            throw new NotInitializedException("There is no page loaded yet.");
//        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
        @SuppressWarnings("unused")
        IEditorPart part = IDE.openEditor(page, file);
//        IEditorPart part = page.openEditor(new FileEditorInput(file), desc.getId());
//        page.bringToTop(part);
    }
    
    public static void showInformationDialog(final String message, final String title, final int width)
    {
        showMessageDialog(message, title, JOptionPane.INFORMATION_MESSAGE, width);
    }
    
    public static void showErrorDialog(final String message, final String title, final int width)
    {
        showMessageDialog(message, title, JOptionPane.ERROR_MESSAGE, width);
    }
    
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

    /**
     * Internal variable that is used to represent the file that is active and open in the current Eclipse editor.
     */
    private static IFile eclipseEditorFile_;
    /**
     * Internal variable that is used to represent if the file that is active and open in the current Eclipse editor is
     * retrieved with success or not.
     */
    private static boolean fileRetrievedBySuccess_ = false;

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
        fileRetrievedBySuccess_ = true;
        Display.getDefault().syncExec(new Thread()
        {
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
        });
        if (!fileRetrievedBySuccess_)
            throw new NotInitializedException("There is no page loaded yet.");
        return eclipseEditorFile_;
    }
}
