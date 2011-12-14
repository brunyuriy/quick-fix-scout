package edu.washington.cs.synchronization.sync.internal;

import org.eclipse.core.resources.IFile;

/**
 * Notifier pattern for listening the changes to the active files. <br>
 * In Eclipse active file is defined as the file that is currently selected in the Eclipse editor. <br>
 * <br>
 * Listeners will receive a notification whenever the user changes the active file while using Eclipse.
 * 
 * @author Kivanc Muslu
 * @see ActiveFileChangedListener
 */
@Deprecated
public interface ActiveFileChangedNotifier
{
    /**
     * Adds the given listener to the list of listeners.
     * 
     * @param listener A listener that will be notified whenever the active project in the workbench is changed.
     */
    void addActiveFileChangedListener(ActiveFileChangedListener listener);

    /**
     * Signals an active project change to all listeners.
     * 
     * @param file The file that is selected.
     */
    void signalActiveProjectChange(IFile file);
}
