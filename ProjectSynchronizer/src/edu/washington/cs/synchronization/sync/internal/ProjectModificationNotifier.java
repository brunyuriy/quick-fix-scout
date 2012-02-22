package edu.washington.cs.synchronization.sync.internal;

/**
 * Notification interface of listener-notifier pattern for detecting a modification inside a project. <br>
 * Project modifications are textual buffer changes to project's elements.
 * 
 * @author Kivanc Muslu
 */
public interface ProjectModificationNotifier
{
    /**
     * Adds the given listener to the list of listeners. <br>
     * The listener will be notified for the future changes.
     * 
     * @param listener Listener that wants to be notified of possible project modifications.
     */
    void addProjectChangeListener(ProjectModificationListener listener);

    /**
     * Signal to indicate that the project is modified.
     */
    void signalProjectModification();

    /**
     * Signal to indicate that a new event (or task) has been received that will cause a project modification.
     */
    void signalProjectIsAboutToBeModified();
}
