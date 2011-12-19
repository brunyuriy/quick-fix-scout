package edu.washington.cs.synchronization.sync;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import edu.washington.cs.synchronization.sync.internal.CursorChangedListener;
import edu.washington.cs.synchronization.sync.internal.CursorChangedNotifier;
import edu.washington.cs.util.eclipse.ResourceUtility;

public class SynchronizerCursorListener implements ISelectionListener, CursorChangedNotifier 
{
    private final ArrayList<CursorChangedListener> listeners_;
    private IFile lastFile_;
    
    private static final SynchronizerCursorListener instance_ = new SynchronizerCursorListener();
    
    private SynchronizerCursorListener() 
    {
        lastFile_ = null;
        listeners_ = new ArrayList <CursorChangedListener>();
    }
    
    public static synchronized SynchronizerCursorListener getInstance()
    {
        return instance_;
    }
    
    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection)
    {
        IFile file = getCurrentFile(part);
        if (file != null && (lastFile_ == null || isFileChanged(file)))
        {
            signalEditorFileChanged(file);
            lastFile_ = file;
        }
        Integer offset = getOffset(selection);
        if (offset != null)
            signalCursorChange(offset);
    }
    
    private Integer getOffset(ISelection selection)
    {
        Integer result = null;
        if (selection instanceof TextSelection)
        {
            TextSelection textSelection = (TextSelection) selection;
            result = textSelection.getOffset();
        }
        else
            System.out.println("Unknown selection type = " + selection.getClass());
        return result;
    }

    private boolean isFileChanged(IFile file)
    {
        // If you select something other than editor, we still get events. Therefore sometimes file can be null.
        if (file == null)
            return false;
        
        IPath oldLocation = lastFile_.getLocation();
        IPath newLocation = file.getLocation();
        if (oldLocation != null && newLocation != null)
            return !oldLocation.toString().equals(newLocation.toString());
        // It is possible for paths to return 'null'. Therefore, we fallback to name comparison in this case.
        return !lastFile_.getName().equals(file);
    }

    /**
     * Given a part reference that is instance of {@link IWorkbenchPart}, returns the file that lies inside the given part.
     * 
     * @param partRef The reference to the part that holds the file inside it.
     * @return The file that lies inside the given part.
     */
    private IFile getCurrentFile(IWorkbenchPart part)
    {
        IFile result = null;
        if (part instanceof IEditorPart)
        {
            IEditorPart editorPart = (IEditorPart) part;
            String path = "/" + editorPart.getTitleToolTip();
            result = ResourceUtility.getFile(new Path(path));
        }
        return result;
    }

    @Override
    public synchronized void addCursorChangedListener(CursorChangedListener listener)
    {
        listeners_.add(listener);
    }

    @Override
    public synchronized void signalCursorChange(int offset)
    {
        for (CursorChangedListener listener: listeners_)
            listener.cursorChanged(offset);
    }

    @Override
    public synchronized void signalEditorFileChanged(IFile file)
    {
        for (CursorChangedListener listener: listeners_)
            listener.editorFileChanged(file);
    }
}
