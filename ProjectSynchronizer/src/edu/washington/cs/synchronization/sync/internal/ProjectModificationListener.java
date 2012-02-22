package edu.washington.cs.synchronization.sync.internal;

/**
 * Listener interface of listener-notifier pattern for detecting a modification inside a project. <br>
 * Project modifications are textual buffer changes to project's elements.
 * 
 * @author Kivanc Muslu
 */
public interface ProjectModificationListener
{
    /**
     * Indicates that the project is modified.
     */
    void projectModified();

    /**
     * Indicates that an event (or task) has been received that will cause a project modification in the near future.
     */
    void projectIsAboutToBeModified();
}
