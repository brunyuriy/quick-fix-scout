package edu.washington.cs.quickfix.speculation;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import edu.washington.cs.quickfix.speculation.calc.SpeculationCalculator;
import edu.washington.cs.quickfix.speculation.gui.SpeculationPreferencePage;
import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.synchronization.sync.internal.CursorChangedListener;
import edu.washington.cs.util.log.LogHandlers;

// Source code that is change in Eclipse.
// 1. AbstractAnnotationHover in org.eclipse.ui
// 2. CompletionProposalPopup in org.eclipse.jface.text

public class Speculator implements CursorChangedListener
{
    public static final String PLUG_IN_ID = "edu.washington.cs.quickfix.speculation";
    public static final Logger logger = Logger.getLogger(Speculator.class.getName());
    static
    {
        LogHandlers.setMainHandler(System.err);
        logger.setLevel(Level.INFO);
    }
    private static Speculator instance_ = new Speculator();

    public static synchronized Speculator getSpeculator()
    {
        return instance_;
    }

    private Runner currentRunner_ = null;

    // singleton.
    private Speculator()
    {}

    public synchronized IProject getCurrentProject()
    {
        if (getCurrentRunner() == null)
            return null;
        return getCurrentRunner().project_;
    }

    private synchronized void setCurrentRunner(Runner runner)
    {
        currentRunner_ = runner;
    }

    private synchronized Runner getCurrentRunner()
    {
        return currentRunner_;
    }

    public void stopSpeculation()
    {
        Runner currentRunner = getCurrentRunner();
        if (currentRunner != null)
            currentRunner.stopRunning();
    }

    public synchronized ProjectSynchronizer getCurrentSynchronizer()
    {
        Runner currentRunner = getCurrentRunner();
        return (currentRunner == null ? null : currentRunner.synchronizer_);
    }

    public synchronized SpeculationCalculator getCurrentCalculator()
    {
        Runner currentRunner = getCurrentRunner();
        return (currentRunner == null ? null : currentRunner.calculator_);
    }

    public void speculateProject(IFile initialFile)
    {
        IProject project = initialFile.getProject();
        if (SpeculationPreferencePage.getInstance().isActivated() && !ProjectSynchronizer.isShadowProject(project))
        {
            Thread runner = new Runner(project, initialFile);
            runner.start();
        }
    }

    public class Runner extends Thread
    {
        private final IProject project_;
        private ProjectSynchronizer synchronizer_;
        private final IFile initialFile_;
        private SpeculationCalculator calculator_;

        public Runner(IProject project, IFile file)
        {
            logger.info("Creating a speculator for project: " + project.getName());
            project_ = project;
            initialFile_ = file;
        }

        public void run()
        {
            Runner current = getCurrentRunner();
            if (current != null)
                current.stopRunning();
            else
                logger.info("There is no current runner.");
            logger.info("Creating synchronizer for project = " + project_.getName());
            createSynchronizer();
            logger.info("Creating calculator for project = " + project_.getName());
            createCalculator();
            setCurrentRunner(this);
        }

        private void createSynchronizer()
        {
            synchronizer_ = new ProjectSynchronizer("Speculation", project_);
            synchronizer_.init();
        }

        private void createCalculator()
        {
            SpeculationCalculator calculator = new SpeculationCalculator(synchronizer_);
            calculator.start();
            calculator.setCurrentFile(initialFile_);
            calculator_ = calculator;
        }

        public void stopRunning()
        {
            calculator_.killAndJoin();
            logger.info("Stopped calculator for project = " + project_.getName());
            synchronizer_.stop();
            logger.info("Stopped synchronizer for project = " + project_.getName());
            setCurrentRunner(null);
        }
    }

    @Override
    public void editorFileChanged(IFile file)
    {
        IProject project = file.getProject();
        IProject currentProject = getCurrentProject();
        if (currentProject == null || !currentProject.getName().equals(project.getName()))
            speculateProject(file);
        else if (currentProject != null && project.getName().equals(currentProject.getName()))
        {
            SpeculationCalculator calculator = getCurrentCalculator();
            if (calculator != null)
                calculator.setCurrentFile(file);
        }
    }

    @Override
    public void cursorChanged(int offset)
    {
        SpeculationCalculator calculator = getCurrentCalculator();
        if (calculator != null)
            calculator.setCursorOffset(offset);
    }

    public void updateTypingSessionTime(int value)
    {
        SpeculationCalculator calculator = getCurrentCalculator();
        if (calculator != null)
            calculator.updateTypingSessionTime(value);
    }

    public void quickFixInvoked()
    {
        SpeculationCalculator calculator = getCurrentCalculator();
        if (calculator != null)
            calculator.quickFixInvoked();
    }
}
