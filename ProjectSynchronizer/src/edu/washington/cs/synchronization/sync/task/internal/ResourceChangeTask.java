package edu.washington.cs.synchronization.sync.task.internal;

import java.util.Stack;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;

import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.synchronization.sync.SynchronizerResourceChangeListener;
import edu.washington.cs.util.eclipse.ResourceUtility;

/**
 * Represents that a resource that is being observed is changed (i.e., changed, added, or removed).
 * 
 * @author Kivanc Muslu
 * @see SynchronizerResourceChangeListener
 */
public class ResourceChangeTask implements Task
{
    /** The file that represents the resource change. */
    private final IFile originalFile_;
    /**
     * Variable that represents the change kind.
     * 
     * @see IResourceDelta#REMOVED
     * @see IResourceDelta#ADDED
     * @see IResourceDelta#CHANGED
     */
    private final int changeKind_;
    /** synchronizer object that is responsible to manage this change. */
    private final ProjectSynchronizer synchronizer_;

    /**
     * Creates a resource change task.
     * 
     * @param original The file that the change has occurred.
     * @param kind The change kind.
     * @param synchronizer Synchronizer that is responsible to manage this task.
     */
    public ResourceChangeTask(IFile original, int kind, ProjectSynchronizer synchronizer)
    {
        originalFile_ = original;
        changeKind_ = kind;
        synchronizer_ = synchronizer;
    }

    /**
     * {@inheritDoc} <br>
     * <br>
     * Synchronizer that is responsible to manage this task finds the corresponding shadow file (shadow of
     * {@link #originalFile_}), and applies the same change to that file. <br>
     * In other words a shadow file is created if the original file is recently created, or the existing shadow file is
     * deleted if the original file is deleted.
     * 
     * @return <code>null</code>.
     */
    @Override
    public SaveTask doTask()
    {
        IProject shadowProject = synchronizer_.getShadowProject();
        IFile shadowFile = shadowProject.getFile(originalFile_.getProjectRelativePath());
        if (changeKind_ == IResourceDelta.REMOVED)
            ResourceUtility.deleteResource(shadowFile);
        else if (changeKind_ == IResourceDelta.ADDED)
        {
            // Get the folders that need to be created before creating the file.
            IContainer currentContainer = shadowFile.getParent();
            IProject project = shadowFile.getProject();
            Stack <IFolder> folders = new Stack <IFolder>();
            while (!currentContainer.equals(project))
            {
                if (currentContainer.getType() == IResource.FOLDER)
                    folders.push((IFolder) currentContainer);
                currentContainer = currentContainer.getParent();
            }
            // Create the non-existing but required folders.
            while (!folders.empty())
            {
                IFolder folder = folders.pop();
                if (!folder.exists())
                    ResourceUtility.createFolder(folder);
            }
            ResourceUtility.createFile(shadowFile, originalFile_);
        }
        else if (changeKind_ == IResourceDelta.CHANGED)
            // This is a special case for build path changes.
            ResourceUtility.copyFile(originalFile_, shadowFile);
        else
            System.out.println("Unknown change kind = " + changeKind_ + " for task = " + this);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj)
    {
        if (obj instanceof ResourceChangeTask)
            return equals((ResourceChangeTask) obj);
        else
            return false;
    }

    /**
     * Two resource change tasks are equal if their string representation are equal.
     * 
     * @param other Second resource change task that 'this' will be compared to.
     * @return <code>true</code> if two resource change tasks are equal, <code>false</code> otherwise.
     */
    public boolean equals(ResourceChangeTask other)
    {
        return originalFile_.toString().equals(other.originalFile_.toString());
    }

    /**
     * {@inheritDoc} <br>
     * <br>
     * The hash code of resource change task only depends on the string representation of the {@link #originalFile_} and
     * {@link #changeKind_}.
     */
    public int hashCode()
    {
        int result = 17;
        result = result * 37 + originalFile_.toString().hashCode();
        result = result * 37 + changeKind_;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "[ResourceChangedTask: resource = " + originalFile_.getFullPath() + ", kind = " + changeKind_ + " ]";
    }
}
