package edu.washington.cs.synchronization;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;

import com.kivancmuslu.www.timer.Timer;
import com.kivancmuslu.www.zip.ZipException;
import com.kivancmuslu.www.zip.Zipper;

import edu.washington.cs.synchronization.sync.SynchronizerBufferChangedListener;
import edu.washington.cs.synchronization.sync.SynchronizerResourceChangeListener;
import edu.washington.cs.synchronization.sync.task.internal.TaskWorker;
import edu.washington.cs.util.eclipse.ResourceUtility;

/**
 * Project synchronizer is an implementation that will make sure that two projects are always in sync with the lowest
 * granularity of sync possible. <br>
 * At the moment, this is buffer level sync for .java files that are on class-path and file level sync for other files. <br>
 * A project synchronizer has 3 main elements:
 * <ol>
 * <li>Original project that the synchronization will be based on.</li>
 * <li>A shadow project that will be at sync with the original project at all times.</li>
 * <li>A {@link TaskWorker} that will make sure that the shadow and original projects are at sync by applying all
 * changes done to the original project to the shadow project.
 * </ol>
 * To observe the changes done to the original project, a synchronizer uses 3 notification mechanisms.
 * <ol>
 * <li>A {@link SynchronizerResourceChangeListener} to get notified about the files added and deleted to the original
 * project.</li>
 * <li>A {@link SynchronizerPartListener} to get notified about the files that are currently selected or active in the
 * Eclipse editor.</li>
 * <li>A {@link SynchronizerBufferChangedListener} (for each file) to get notified about all changes done to the buffers
 * of .java files on the class-path of the project.</li>
 * </ol>
 * Project synchronizer also provides static methods to create, maintain and retrieve different synchronizers for
 * different projects and purposes.
 * 
 * @author Kivanc Muslu
 */
public class ProjectSynchronizer
{
    public static final String SHADOW_PREFIX = "DO_NOT_DELETE_";
    /** Constant value that represents the working set name to be assigned to the shadow projects. */
    public static final String WORKING_SET_NAME = "QFS";
    public static final String PLUG_IN_ID = "edu.washington.cs.synchronization";
    /** logger for debugging. */
    private static final Logger logger = Logger.getLogger(ProjectSynchronizer.class.getName());
    
    private static final long MB = 1024*1024;
    private static final long ZIP_LIMIT = MB * 20;
    
    public static final boolean CONTOLLED_EXPERIMENT = false;
    
    private static double toMB(long bytes)
    {
        double result = bytes*1.0/MB;
        return result;
    }
    
    static
    {
        logger.setLevel(Level.INFO);
    }
    //@formatter:off
    /*
     * Mapping is done from shadow project to the synchronizers for the following reason:
     * We can have multiple plug-in (speculation, usage) using the same original source, so there will be
     * multiple synchronizers for the same original project. However, this is not the case for shadows.
     * For each different plug-in a new shadow is created with a unique name.  
     */
    //@formatter:on
    /**
     * a mapping from project name (of shadow projects) to the synchronizer that is responsible for that project.
     */
    private static final Map <String, ProjectSynchronizer> synchronizers_ = new HashMap <String, ProjectSynchronizer>();
    /**
     * a set of string that is used while creating the shadow projects. <br>
     * Used later on to retrieve shadow projects using only the original project information.
     */
    private static HashSet <String> knownPrefixes_ = new HashSet <String>();
    static
    {
        knownPrefixes_.add("Speculation");
        knownPrefixes_.add("Observation");
    }
    /** original project that the synchronization will be based on. */
    private final IProject original_;
    /** shadow project that will be in sync with the original project at all times. */
    private final IProject shadow_;
    /** task worker that will apply the changes done to the original project to the shadow project. */
    private final TaskWorker worker_;
    /** prefix that is used to during the construction of the shadow project. */
    private final String prefix_;
    private boolean internalCheck_ = false;
    private boolean internalResult_ = true;
    
    private final static String [] VCPrefixes = new String [] {".bzr", ".git", ".svn", ".hg", "CVS"};

    /**
     * Returns <code>true</code> if the given project is created as a shadow project, <code>false</code> otherwise.
     * 
     * @param project The project that is analyzed.
     * @return <code>true</code> if the given project is created as a shadow project, <code>false</code> otherwise.
     */
    public static boolean isShadowProject(IProject project)
    {
        return isShadowProject(project, ResourceUtility.getAllProjects());
    }

    private static boolean isShadowProject(IProject project, IProject [] allProjects)
    {
        String projectName = project.getName();
        for (IProject pro: allProjects)
        {
            for (String prefix: getKnownPrefixes())
            {
                String shadowName = getShadowProjectName(pro, prefix);
                if (projectName.equals(shadowName))
                    return true;
            }
        }
        return false;
    }

    private static boolean isDeadShadowProject(IProject project, IProject [] allProjects)
    {
        boolean shadow = isShadowProject(project, allProjects);
        if (!shadow)
            return project.getName().startsWith(SHADOW_PREFIX);
        return false;
    }

    static void deleteUnusedShadows()
    {
        IProject [] projects = ResourceUtility.getAllProjects();
        for (IProject project: projects)
        {
            if (isDeadShadowProject(project, projects))
            {
                logger.info("Deleting shadow project = " + project.getName()
                        + " since the corresponding original project no longer exists.");
                ResourceUtility.deleteResource(project);
            }
        }
    }
    
    static void updateShadowWorkingSet()
    {
        IProject [] projects = ResourceUtility.getAllProjects();
        for (IProject project: projects)
        {
            if (isShadowProject(project, projects))
                ResourceUtility.addToWorkingSet(WORKING_SET_NAME, project);
        }
    }

    /**
     * Returns an array of synchronizers that are associated with the given project. <br>
     * Project must be an original project. <br>
     * This method might return an empty array or an array with more than one elements. <br>
     * <br>
     * This method is synchronized over {@link ProjectSynchronizer}
     * 
     * @param project The original project that the synchronizers are created on.
     * @return An array of synchronizers that are associated with the given project. <br>
     */
    public static synchronized ProjectSynchronizer [] getSynchronizers(IProject project)
    {
        ArrayList <ProjectSynchronizer> result = new ArrayList <ProjectSynchronizer>();
        for (String prefix: knownPrefixes_)
        {
            ProjectSynchronizer synchronizer = getSynchronizerFromOriginal(project, prefix);
            if (synchronizer != null)
                result.add(synchronizer);
        }
        return result.toArray(new ProjectSynchronizer [result.size()]);
    }

    /**
     * Adds the synchronizer to the mapping of available synchronizers. <br>
     * <br>
     * This method is synchronized over {@link ProjectSynchronizer}
     * 
     * @param synchronizer The synchronizer that will be added to the mapping.
     */
    private static synchronized void addProjectSynchronizer(ProjectSynchronizer synchronizer)
    {
        logger.info("Adding synchronizer for project = " + synchronizer.original_.getName());
        synchronizers_.put(synchronizer.shadow_.getName(), synchronizer);
    }

    /**
     * Removes the synchronizer from the mapping of available synchronizers. <br>
     * <br>
     * This method is synchronized over {@link ProjectSynchronizer}
     * 
     * @param synchronizer The synchronizer that will be removed from the mapping.
     */
    private static synchronized void removeProjectSynchronizer(ProjectSynchronizer synchronizer)
    {
        logger.info("Removing synchronizer for project = " + synchronizer.original_.getName());
        synchronizers_.remove(synchronizer.shadow_.getName());
    }

    /**
     * Returns the synchronizer that is created using the given project and given prefix. <br>
     * <br>
     * This method is synchronized over {@link ProjectSynchronizer}
     * 
     * @param project The original project that is used during the construction of the synchronizer.
     * @param prefix The prefix that is used during the construction of the synchronizer.
     * @return The synchronizer that is created using the given project and given prefix.
     */
    private static synchronized ProjectSynchronizer getSynchronizerFromOriginal(IProject project, String prefix)
    {
        IProject shadow = getShadowProject(project, prefix);
        logger.fine("Searching synchronizer for project = " + shadow.getName());
        return synchronizers_.get(shadow.getName());
    }

    public static synchronized ProjectSynchronizer getSynchronizerFromShadow(IProject shadow)
    {
        return synchronizers_.get(shadow.getName());
    }

    /**
     * Returns the known prefixes that are used for creating shadow projects up to this point. <br>
     * <br>
     * This method is synchronized over {@link ProjectSynchronizer}
     * 
     * @return The known prefixes that are used for creating shadow projects up to this point.
     */
    private static synchronized Set <String> getKnownPrefixes()
    {
        return knownPrefixes_;
    }

    /**
     * Creates a project synchronizer.
     * 
     * @param prefix The prefix that will be used during the construction of the shadow project.
     * @param original Original project that the synchronizer will be based on.
     */
    public ProjectSynchronizer(String prefix, IProject original)
    {
        prefix_ = prefix;
        knownPrefixes_.add(prefix_);
        original_ = original;
        shadow_ = ResourceUtility.getProject(getShadowProjectName(original_, prefix_));
        worker_ = new TaskWorker();
        addProjectSynchronizer(this);
    }

    /**
     * Initializes the project synchronizer. <br>
     * This is a long running method that will do at least one sync over the projects. <br>
     * Calling this method makes the {@link #worker_} to start working and synchronizing the projects.
     */
    public void init()
    {
        syncProjects();
        worker_.start();
        logger.info("Created and initialized a synchronizer for project = " + original_.getName());
    }

    /**
     * Stops and kills the {@link #worker_}. <br>
     * After this point on, no more synchronization is done between the original and shadow projects.
     */
    public void stop()
    {
        worker_.killAndJoin();
        removeProjectSynchronizer(this);
        logger.finer("Killed worker.");
    }

    public boolean testSynchronization()
    {
        startInternalCheck();
        boolean result = syncProjects();
        completeInternalCheck();
        return result;
    }

    /**
     * Synchronizes the {@link #shadow_} project with respect to the {@link #original_} project. <br>
     * Returns <code>true</code> if there is no task (i.e., original project is in sync with the shadow project) at the
     * beginning of the synchronization, <code>false</code> otherwise.
     * 
     * @return <code>true</code> if nothing has changed between two projects during the synchronization, 
     * <code>false</code> otherwise.
     */
    public boolean syncProjects()
    {
        if (!original_.isOpen())
            return false;
            
        boolean result = true;
        if (internalCheck_)
        {
            logger.info("Synchronization between the original and speculation shadow is being checked.");
            internalResult_ = true;
        }
        Timer.startSession();
        boolean blocked = worker_.isBlocked();
        try
        {
            // Initially the result is equal to the emptiness of the work queue. If the queue
            // is empty, the project should supposed to be in sync.
            result = worker_.isEmpty();
            // In case the worker is blocked and there are changes in the queue, unblock it so that
            // these changes are also synced.
            worker_.unblock();
            // Block the worker so that during the comparison, we won't get changes.
            worker_.block();
            logger.fine("Waiting until the worker thread is done.");
            // Wait until the worker is completely blocked.
            worker_.waitUntilSynchronization();
            // worker_.clear();
            // Refresh the projects just in case.
            ResourceUtility.syncWithFileSystemIfNecessary(original_);
            ResourceUtility.syncWithFileSystemIfNecessary(shadow_);
            if (!shadow_.exists())
                createShadow();
            else
            {
                // Sync and clean the projects.
                syncContainers(original_, shadow_);
                cleanContainers(original_, shadow_);
            }
        } catch (Exception e)
        {
            // If there is an exception during sync, we return false no matter what.
            logger.log(Level.WARNING, "Could not sync projects: " + original_.getName() + " due to exception.", e);
            result = false;
            internalResult_ = false;
        }
        finally
        {
            Timer.completeSession();
            logger.fine("Synchronizing projects took: " + Timer.getTimeAsString());
            /*
             * Since when this happens we are invalidating the calculation and will apply the new coming change we don't
             * need to check this again and resync if it happens.
             */
            if (!worker_.isEmpty())
            {
                result = false;
                logger.warning("While syncing projects, the original project changed through a buffer change event.");
                syncProjects();
            }
            else
            {
                if (!blocked)
                    worker_.unblock();
                // if (blocked)
                // worker_.block();
            }
            // At the end the result depends on the initial result and the internal check (did we detect any
            // problems during the sync check).
            result =  result && internalResult_;
        }
        return result;
    }

    /**
     * Deletes any file that is included in shadow container but not included in the original container.
     * 
     * @param original The original container which the cleaning will be based on.
     * @param shadow The shadow container which will be cleaned.
     */
    private void cleanContainers(IContainer original, IContainer shadow)
    {
        // Both original and shadow must exist! Also Shadow must have every element that original
        // has.
        IResource [] members = ResourceUtility.getMembers(shadow);
        IProject originalProject = original.getProject();
        for (IResource member: members)
        {
            int type = member.getType();
            if (type == IResource.FOLDER)
            {
                IFolder shadowFolder = (IFolder) member;
                IFolder originalFolder = originalProject.getFolder(shadowFolder.getProjectRelativePath());
                if (originalFolder.exists())
                    cleanContainers(originalFolder, shadowFolder);
                else
                    ResourceUtility.deleteResource(shadowFolder);
            }
            else if (type == IResource.FILE)
            {
                IFile shadowFile = (IFile) member;
                IFile originalFile = originalProject.getFile(shadowFile.getProjectRelativePath());
                if (!originalFile.exists())
                {
                    logger.fine("Deleting shadow file = " + originalFile.getName()
                            + " since it does not exist in original project.");
                    ResourceUtility.deleteResource(shadowFile);
                }
            }
            else
                logger.warning("Unknown resource type inside project = " + original.getName() + ". .getClass() = "
                        + member.getClass());
        }
    }

    /**
     * Synchronizes two containers.
     * 
     * @param original The original container which the synchronization will be based on.
     * @param shadow The shadow container which will be synchronized.
     */
    private void syncContainers(IContainer original, IContainer shadow)
    {
        IProject shadowProject = shadow.getProject();
        for (IResource member: ResourceUtility.getMembers(original))
        {
            IPath relativePath = member.getProjectRelativePath();
            logger.finest("Syncing containers, relative path = " + relativePath);
            int type = member.getType();
            if (type == IResource.FILE)
                syncFiles((IFile) member, shadowProject.getFile(relativePath));
            else if (type == IResource.FOLDER)
                syncFolders((IFolder) member, shadowProject.getFolder(relativePath));
            else
                logger.warning("Unknown resource type inside container = " + original.getName() + ". .getClass() = "
                        + member.getClass());
        }
    }

    /**
     * Synchronizes two folders.
     * 
     * @param original The original folder which the synchronization will be based on.
     * @param shadow The shadow folder which will be synchronized.
     */
    private void syncFolders(IFolder original, IFolder shadow)
    {
        if (shouldSkip(original))
            return;
        if (!shadow.exists())
            ResourceUtility.copyResource(original, shadow);
        else
            syncContainers(original, shadow);
    }

    /**
     * Synchronizes the buffers of given files. If buffers can be retrieved for both files, then synchronization is done
     * and this method returns <code>true</code>. Returns <code>false</code> otherwise.
     * 
     * @param original The original file which the synchronization will be based on.
     * @param shadow The shadow file which's buffer will be synchronized.
     * @return <code>true</code> if the buffers can be retrieved from the files and the synchronization can be done,
     *         <code>false</code> otherwise.
     */
    private boolean syncBuffers(IFile original, IFile shadow)
    {
        logger.finer("Trying to sync buffers for file = " + original.getName());
        // If any of the files are not java files, return false.
        if (!ResourceUtility.isJavaLike(original) || !ResourceUtility.isOnClassPath(original)
                || !ResourceUtility.isJavaLike(shadow))
        {
            logger.finer("Cannot sync files at buffer level since they are not java-like.");
            return false;
        }
        boolean result = true;
        try
        {
            ICompilationUnit originalUnit = JavaCore.createCompilationUnitFrom(original);
            ICompilationUnit shadowUnit = JavaCore.createCompilationUnitFrom(shadow);
            // Make sure that compilation units are created correctly.
            if (originalUnit != null && shadowUnit != null)
            {
                IBuffer originalBuffer = originalUnit.getBuffer();
                IBuffer shadowBuffer = shadowUnit.getBuffer();
                if (!originalBuffer.hasUnsavedChanges())
                {
//                    System.out.println("Not doing a buffer sync since there is no unsaved changes.");
                    return false;
                }
                String originalContents = originalBuffer.getContents();
                String shadowContents = shadowBuffer.getContents();
                if (!originalContents.equals(shadowContents))
                {
                    if (internalCheck_)
                    {
                        logger.warning("File " + shadow.getName()
                                + " (in buffer sync) has different content in project = "
                                + original.getProject().getName() + "!");
                        internalResult_ = false;
                    }
                    shadowBuffer.setContents(originalContents);
                    shadowBuffer.save(null, false);
                    logger.fine("Sync completed.");
                }
                else
                    logger.fine("Buffers contain the same content, skipping sync.");
            }
            // If compilation units cannot be created (i.e., cannot work on buffer level), return
            // false.
            else
            {
                logger.fine("Files don't have a buffer, cannot sync at buffer level!");
                result = false;
            }
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Cannot sync buffers for files = " + original.getName(), e);
            result = false;
        }
        return result;
    }

    /**
     * Synchronizes the shadow file with respect to the original file at buffer level.
     * 
     * @param original The original file that the synchronization will be based on.
     * @param shadow The shadow file that will be synchronized.
     */
    public void syncFiles(IFile original, IFile shadow)
    {
        syncFiles(original, shadow, true);
    }

    public void snapshotShadow(File directory, String zipName)
    {
        if (shadow_ != null)
        {
            try
            {
                Zipper zipper = new Zipper(directory, zipName);
                zipper.excludePrefixes(VCPrefixes);
                zipper.addFolder(new File(shadow_.getLocation().toString()));
                zipper.close();
                File zipFile = new File(directory, zipName);
                if (zipFile.exists())
                {
                    if (zipFile.length() > ZIP_LIMIT)
                    {
                        String size = String.format("%.2f", toMB(zipFile.length()));
                        logger.info("Snapshot for project: " + shadow_.getName() + " is deleted because zipped version was " + size + "MB big.");
                        zipFile.delete();
                    }
                    else
                        logger.fine("Created snapshot with success.");
                }
                //else, Zip file is not created for some reasons. Since these are shadows, we don't really care.
            }
            catch (ZipException e)
            {
                logger.log(Level.SEVERE, "Cannot create snapshot for project: " + shadow_.getName());
            }
        }
    }

    private boolean shouldSkip(IFolder folder)
    {
        return isHGDirectory(folder);
    }

    private boolean isHGDirectory(IFolder folder)
    {
        String name = folder.getName();
        return name.equals(".hg");
    }

    private boolean shouldSkip(IFile file)
    {
        return generatedFile(file);
    }

    private boolean generatedFile(IFile file)
    {
        if (file == null || file.getFileExtension() == null)
            return false;
        String extension = file.getFileExtension();
        if (extension.equals("class"))
            return true;
        return false;
    }

    /**
     * Synchronizes the shadow file with respect to the original file with the given sync level.
     * 
     * @param original The original file that the synchronization will be based on.
     * @param shadow The shadow file that will be synchronized.
     * @param bufferLevelSync value that represents the synchronization level. <br>
     *            If <code>true</code>, then synchronization is done over buffers, is <code>false</code> synchronization
     *            is done over files.
     */
    public void syncFiles(IFile original, IFile shadow, boolean bufferLevelSync)
    {
        if (shouldSkip(original))
        {
            // FIXME Why not detecting these?
            // Can be hacked by looking for a '.' at the beginning of the name of the resource.
            return;
        }
        logger.finest("Syncing files, shadow = " + shadow.getLocation() + ", original = " + original.getLocation());
        if (!shadow.exists())
        {
            ResourceUtility.copyResource(original, shadow);
            // Try to update the copy over buffer level... Here we don't care about the result of
            // the buffer sync operation since we have already copied the file.
            syncBuffers(original, shadow);
        }
        else
        {
            // Try to update the copy over buffer level...
            boolean done = false;
            if (bufferLevelSync)
                done = syncBuffers(original, shadow);
            if (!done && !ResourceUtility.areFilesIdentical(original, shadow))
            {
                if (internalCheck_)
                {
                    logger.warning("File " + shadow.getName() + " has different content in project "
                            + original.getProject().getName() + "!");
                    internalResult_ = false;
                }
                logger.fine("File = " + shadow.getName()
                        + " has different content in shadow project, copying it again.");
                ResourceUtility.copyFile(original, shadow);
                // ResourceUtility.deleteResource(shadow);
                // ResourceUtility.copyResource(original, shadow);
                syncBuffers(original, shadow);
            }
            // else, it means that the files are already in sync.
        }
    }

    /**
     * Creates a copy of the {@link #original_}. <br>
     * If the copy project already exists, this method first deletes that project.
     * 
     * @param original Original project
     */
    private void createShadow()
    {
        IPath destination = new Path(getShadowProjectName(original_, prefix_));
        IProject shadow = ResourceUtility.getProject(destination.toString());
        if (shadow.exists())
            ResourceUtility.deleteResource(shadow);
        ResourceUtility.copyResource(original_, shadow);
        ResourceUtility.addToWorkingSet(WORKING_SET_NAME, shadow);
    }

    /**
     * Returns the shadow project's name that is associated with the original project. <br>
     * <br>
     * The shadow project's name is calculated as the prefix_hash(original project's name).
     * 
     * @param original Project that the shadow project is based on.
     * @param prefix The prefix that will be used when constructing the name.
     * @return The shadow project's name that is associated with the original project.
     */
    private static String getShadowProjectName(IProject original, String prefix)
    {
        String result = null;
        result = SHADOW_PREFIX + prefix + "_" + original.getName() + "_" + original.getProjectRelativePath().toString()
                + original.hashCode();
        return result;
    }

    /**
     * Returns the shadow project that is created using the given project and the prefix.
     * 
     * @param original Original project that is used during the creation of the shadow.
     * @param prefix Prefix that is used during the creation of the shadow.
     * @return The shadow project that is created using the given project and the prefix.
     */
    private static IProject getShadowProject(IProject original, String prefix)
    {
        return ResourceUtility.getProject(getShadowProjectName(original, prefix));
    }

    /*********************
     * GETTERS & SETTERS *
     ********************/
    /**
     * Returns the task worker that synchronizes the two projects. <br>
     * <br>
     * This method is synchronized over 'this'.
     * 
     * @return The task worker that synchronizes the two projects.
     */
    public synchronized TaskWorker getTaskWorker()
    {
        return worker_;
    }

    /**
     * Returns the shadow project that is being synchronized with the original project. <br>
     * <br>
     * This method is synchronized over 'this'.
     * 
     * @return The shadow project that is being synchronized with the original project.
     */
    public synchronized IProject getShadowProject()
    {
        return shadow_;
    }

    /**
     * Returns the original project that this synchronizer is based on. <br>
     * <br>
     * This method is synchronized over 'this'.
     * 
     * @return The original project that this synchronizer is based on.
     */
    public synchronized IProject getProject()
    {
        return original_;
    }

    public void startInternalCheck()
    {
        internalCheck_ = true;
    }

    public void completeInternalCheck()
    {
        internalCheck_ = false;
    }
}
