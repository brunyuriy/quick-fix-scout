package edu.washington.cs.synchronization.sync;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;

import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.synchronization.sync.task.internal.ResourceChangeTask;
import edu.washington.cs.synchronization.sync.task.internal.TaskWorker;

/**
 * Synchronizer resource change listener is the implementation of an {@link IResourceChangeListener} that reports the
 * files that are added to and deleted from the projects in the workspace. <br>
 * This way project synchronizers can take care of the same changes in the shadow projects to make sure that the
 * synchronization is not broken.
 * 
 * @author Kivanc Muslu
 */
public class SynchronizerResourceChangeListener implements IResourceChangeListener
{
    /** logger for debugging. */
    private static final Logger logger = Logger.getLogger(SynchronizerResourceChangeListener.class.getName());
    static
    {
        //@formatter:off
        /*
         * Level.FINE => See all the tasks created and added. 
         * Level.FINER => See the traversing of delta trees.
         */
        //@formatter:on
        logger.setLevel(Level.INFO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resourceChanged(final IResourceChangeEvent event)
    {
        Thread worker = new Thread()
        {
            public void run()
            {
                IResourceDelta delta = event.getDelta();
                processResourceDelta(delta, 0);
            }
        };
        worker.start();
    }

    /**
     * Processes the resource delta that represents the change for a resource. <br>
     * Resource deltas may represent a tree of changes, so this method traverses the whole tree (visits also the
     * children of the deltas). <br>
     * This method creates {@link ResourceChangeTask}s that represent the addition or removing of files to the
     * workspace. <br>
     * The tasks are handled by {@link TaskWorker}s.
     * 
     * @param delta Resource delta that is being analyzed.
     * @param tab Tab level used for pretty printing.
     */
    private void processResourceDelta(IResourceDelta delta, int tab)
    {
        // Deltas can be null? Be careful...
        if (delta == null)
            return;
        IResource resource = delta.getResource();
        if (resource.getType() == IResource.FILE)
        {
            IFile file = (IFile) resource;
            if (file.getFileExtension() != null && !file.getFileExtension().equals("class"))
            {
                int kind = delta.getKind();
                if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED)
                    addTask(file, kind);
            }
            if (file.getName().equals(".classpath"))
                addTask(file, delta.getKind());
        }
        IResourceDelta [] children = delta.getAffectedChildren();
        logger.finer("" + delta.getFullPath());
        for (IResourceDelta child: children)
            processResourceDelta(child, tab + 1);
    }

    private void addTask(IFile file, int kind)
    {
        IProject project = file.getProject();
        if (!ProjectSynchronizer.isShadowProject(project))
        {
            ProjectSynchronizer [] synchronizers = ProjectSynchronizer.getSynchronizers(project);
            for (ProjectSynchronizer synchronizer: synchronizers)
            {
                ResourceChangeTask task = new ResourceChangeTask(file, kind, synchronizer);
                logger.fine("Adding task = " + task);
                synchronizer.getTaskWorker().addTask(task);
            }
        }
    }
}
