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

import com.kivancmuslu.www.zip.Zipper;

import edu.washington.cs.quickfix.observation.Observer;
import edu.washington.cs.quickfix.observation.gui.ObservationPreferencePage;
import edu.washington.cs.quickfix.observation.log.ObservationCompilationErrorLogger.Type;
import edu.washington.cs.quickfix.observation.log.internal.QFSession;
import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.util.eclipse.QuickFixUtility;
import edu.washington.cs.util.eclipse.SharedConstants;

public class ObservationGrabber extends Thread
{
    private static int counter = 1;
    private static final Logger logger = Logger.getLogger(ObservationGrabber.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
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
        /*
         * Normally this is already done in a non-UI thread (this), so maybe there is no need to spawn a new thread.
         * However, I didn't want to directly call 'run'.
         */
        ProjectSynchronizer synchronizer = Observer.getUsageObserver().getCurrentSynchronizer();
        synchronizer.startInternalCheck();
        boolean internalCheck = synchronizer.syncProjects();
        synchronizer.completeInternalCheck();
        if (!internalCheck)
        {
            session.invalidate();
            logger.severe("Projects getting out of sync problem occured. Current quick fix session is invalidated.");
        }
        else
        {
            snapshot();
            Thread compilationErrorLogger = new ObservationCompilationErrorLogger(session, Type.BEFORE);
            compilationErrorLogger.start();
        }
    }

    private void snapshot()
    {
        try
        {
            // check if snapshotting is activated, and if it is snapshot the shadow project.
            if (ObservationPreferencePage.getInstance().isSnapshotActivated())
            {
                IProject shadowProject = Observer.getUsageObserver().getCurrentShadowProject();
                if (shadowProject != null)
                {
                    IProject originalProject = Observer.getUsageObserver().getCurrentProject();
                    Zipper zipper = new Zipper(new File(SharedConstants.DEBUG_LOG_DIR),
                            originalProject.getName() + "_" + SharedConstants.UNIQUE_TIME_STAMP + "_snapshot_"
                                    + counter + ".zip");
                    zipper.addFolder(new File(shadowProject.getLocation().toString()));
                    zipper.close();
                    counter++;
                    logger.info("Created snapshot with success.");
                }
            }
        }
        catch (Exception e)
        {
            logger.log(Level.INFO, "Cannot create snapshot due to exception. ", e);
        }
    }
}
