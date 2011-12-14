package edu.washington.cs.synchronization.sync.internal;

import org.eclipse.core.resources.IFile;

/**
 * Listener pattern for listening the changes to the active files. <br>
 * In Eclipse active file is defined as the file that is currently selected in the Eclipse editor. <br>
 * <br>
 * Listeners will receive a notification whenever the user changes the active file while using Eclipse.
 * 
 * @author Kivanc Muslu
 * @see ActiveFileChangedNotifier
 */
@Deprecated
public interface ActiveFileChangedListener
{
    /**
     * Notification that indicates the user changed the active project is in Eclipse.
     * 
     * @param file The file that is selected.
     */
    void activeProjectChanged(IFile file);
}
