package edu.washington.cs.quickfix.observation.log;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import edu.washington.cs.quickfix.observation.Observer;
import edu.washington.cs.quickfix.observation.gui.ObservationPreferencePage;
import edu.washington.cs.quickfix.observation.log.ObservationCompilationErrorLogger.Type;
import edu.washington.cs.quickfix.observation.log.internal.QFSession;
import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.util.eclipse.QuickFixUtility;
import edu.washington.cs.util.eclipse.SharedConstants;

public class ObservationGrabber extends Thread
{
    private static final int SNAPSHOT_THRESHOLD = 2;
    private static final Logger logger = Logger.getLogger(ObservationGrabber.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    private static int counter = 1;
    private final IProblemLocation [] locations_;
    private final IInvocationContext context_;

    public ObservationGrabber(IInvocationContext context, IProblemLocation [] locations)
    {
        locations_ = locations;
        context_ = context;
    }

    public void run()
    {
        if (!ObservationPreferencePage.getInstance().isActivated())
        {
            logger.info("Communication: Not getting the proposals offered since the observer is disabled.");
            return;
        }
        ICompilationUnit unit = context_.getCompilationUnit();
        try
        {
            IResource resource = unit.getCorrespondingResource();
            IProject project = resource.getProject();
            /*
             * This means that the currently selected proposals and quick fix section is related to a shadow project and
             * we don't care or observe what happens to the shadow project.
             */
            // TODO This should never happen!
            if (ProjectSynchronizer.isShadowProject(project))
            {
                logger.severe("Communication: Not getting the proposals offered since current project is a shadow.");
                ObservationLogger.getLogger().invalidateCurrentSession();
                return;
            }
        }
        catch (JavaModelException e)
        {
            // This should not happen.
            logger.log(Level.SEVERE,
                    "Cannot get the corresponding resource for compilation unit = " + unit.getElementName(), e);
        }
        QFSession session = ObservationLogger.getLogger().getCurrentSession();
        session.setLocations(locations_);
        session.setProposals(QuickFixUtility.calculateCompletionProposals(context_, locations_));
        logger.info("Communication: Setting the proposals offered by Eclipse");
        
        ProjectSynchronizer synchronizer = Observer.getUsageObserver().getCurrentSynchronizer();
        if (synchronizer == null)
            return;
        boolean internalCheck = synchronizer.testSynchronization();
        if (!internalCheck)
        {
            session.invalidate();
            logger.severe("Projects getting out of sync problem occured. Current quick fix session is invalidated.");
        }
        else
        {
            /*
             * Normally this is already done in a non-UI thread (this), so maybe there is no need to spawn a new thread.
             * However, I didn't want to directly call 'run'.
             * Update: I am now calling run since I need the computation in compilation error logger finished before deciding
             * whether to snapshot or not.
             */
            ObservationCompilationErrorLogger compilationErrorLogger = new ObservationCompilationErrorLogger(session, Type.BEFORE);
            compilationErrorLogger.run();
            int errors = compilationErrorLogger.getNoCompilationErrors();
            if (errors >= SNAPSHOT_THRESHOLD)
                snapshot();
            else
                logger.info("Snapshot is not created since the number of compilation errors are: " + errors);
        }
    }

    private void snapshot()
    {
        try
        {
            // check if snapshotting is activated, and if it is snapshot the shadow project.
            if (ObservationPreferencePage.getInstance().isSnapshotActivated())
            {
                ProjectSynchronizer synchronizer = Observer.getUsageObserver().getCurrentSynchronizer();
                if (synchronizer != null)
                {
                    IProject originalProject = Observer.getUsageObserver().getCurrentProject();
                    File zipDir = new File(SharedConstants.DEBUG_LOG_DIR);
                    String zipName = originalProject.getName() + "_" + SharedConstants.UNIQUE_TIME_STAMP + "_snapshot_"
                            + counter + ".zip";
                    synchronizer.snapshotShadow(zipDir, zipName);
                    counter++;
                }
            }
        }
        catch (Exception e)
        {
            logger.log(Level.INFO, "Cannot create snapshot due to exception. ", e);
        }
    }
}
