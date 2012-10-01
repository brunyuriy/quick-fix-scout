package edu.washington.cs.quickfix.observation;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import edu.washington.cs.quickfix.observation.gui.ObservationPreferencePage;
import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.synchronization.sync.internal.CursorChangedListener;
import edu.washington.cs.synchronization.sync.task.internal.TaskWorker;
import edu.washington.cs.util.log.LogHandlers;

public class Observer implements CursorChangedListener
{
    public static final String PLUG_IN_ID = "edu.washington.cs.quickfix.observation";
    public static final Logger logger = Logger.getLogger(Observer.class.getName());
    static
    {
        LogHandlers.setMainHandler(System.err);
        logger.setLevel(Level.INFO);
    }
    private static Observer instance_ = new Observer();

    private Runner currentRunner_ = null;
    private IFile currentFile_ = null;

    // singleton
    private Observer()
    {}

    private synchronized Runner getCurrentRunner()
    {
        return currentRunner_;
    }

    private synchronized void setCurrentRunner(Runner runner)
    {
        currentRunner_ = runner;
    }

    public class Runner extends Thread
    {
        private final IProject project_;
        private ProjectSynchronizer synchronizer_;

        public Runner(IProject project)
        {
            project_ = project;
        }

        public void run()
        {
            Runner current = getCurrentRunner();
            if (current != null)
                current.stopRunning();
            logger.info("Creating synchronizer for project = " + project_.getName());
            createSynchronizer();
            setCurrentRunner(this);
        }

        private void createSynchronizer()
        {
            synchronizer_ = new ProjectSynchronizer("Observation", project_);
            synchronizer_.init();
        }

        private void stopRunning()
        {
            synchronizer_.stop();
            logger.info("Stopped the synchronizer for project = " + project_.getName());
            setCurrentRunner(null);
        }
    }

    public synchronized void stopObservation()
    {
        getCurrentRunner().stopRunning();
    }

    public void observeProject(IProject project)
    {
        boolean isActive = ObservationPreferencePage.getInstance().isActivated();
        if (isActive && !ProjectSynchronizer.isShadowProject(project))
        {
            Thread runner = new Runner(project);
            runner.start();
        }
    }

    public static synchronized Observer getUsageObserver()
    {
        return instance_;
    }

    public synchronized TaskWorker getCurrentTaskWorker()
    {
        if (getCurrentSynchronizer() == null)
            return null;
        return getCurrentSynchronizer().getTaskWorker();
    }

    public synchronized ProjectSynchronizer getCurrentSynchronizer()
    {
        if (currentRunner_ == null)
            return null;
        return currentRunner_.synchronizer_;
    }
    
    public synchronized IProject getCurrentShadowProject()
    {
        ProjectSynchronizer synchronizer = getCurrentSynchronizer();
        if (synchronizer == null)
            return null;
        return synchronizer.getShadowProject();
    }

    public synchronized IProject getCurrentProject()
    {
        if (getCurrentSynchronizer() == null)
            return null;
        else
            return getCurrentSynchronizer().getProject();
    }

    @Override
    public void editorFileChanged(IFile file)
    {
    	currentFile_ = file;
        IProject project = currentFile_.getProject();
        logger.fine("Active project changed => " + project.getName());
        if (!ProjectSynchronizer.isShadowProject(project))
        {
            if (getCurrentProject() == null || !project.getName().equals(getCurrentProject().getName()))
                observeProject(project);
        }
    }

    @Override
    public void cursorChanged(int offset)
    {
        // Usage observer does not need an implementation based on cursor change.
        // Left empty intentionally. 
    }
    
    public IFile getCurrentFile()
    {
    	return currentFile_;
    }
}
