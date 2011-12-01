/**
 * This package provides implementations for listeners that are provided by Eclipse to keep two projects in sync. <br>
 * Currently the following listeners are used:
 * <ol>
 * <li>{@link org.eclipse.jface.text.IDocumentListener} to get notified of the changes done to the buffer of the file
 * that is being edited</li>
 * <li>{@link org.eclipse.ui.IPartListener2} to get notified when the user changes the current file that is being
 * edited.</li>
 * <li>{@link org.eclipse.core.filebuffers.IFileBufferListener} to get notified whenever a new buffer is created for a
 * file.</li>
 * <li> {@link org.eclipse.core.resources.IResourceChangeListener} to get notified when a file is added to or deleted
 * from the current project.</li>
 * </ol>
 */
package edu.washington.cs.synchronization.sync;

