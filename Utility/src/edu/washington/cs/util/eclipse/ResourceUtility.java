package edu.washington.cs.util.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

/**
 * This utility class provides static helper methods for resource handling. <br>
 * Currently offered functionality is:
 * <ul>
 * <li>Adding a project into a working set.</li>
 * <li>Getting different workspace elements, such as projects, files, members of a project, etc..</li>
 * <li>Check whether a file is on the classpath of a project or not.</li>
 * <li>Check whether a file is Java-like or not.</li>
 * <li>Various resource operations, such as deleting a resource, creating a file, comparing two files contents, etc..</li>
 * </ul>
 * 
 * @author Kivanc Muslu
 */
public class ResourceUtility
{
    /** The URL where the version list is stored in the Internet. */
    public static final String VERSION_URL = "http://www.kivancmuslu.com/Quick_Fix_Scout_Files/version.txt";
    /** Logger for file comparison operations. */
    private static final Logger fileComparisonLogger_ = Logger.getLogger(ResourceUtility.class.getName()
            + ".file.comparison");
    /** Logger for debugging. */
    private static final Logger logger_ = Logger.getLogger(ResourceUtility.class.getName());
    static
    {
        fileComparisonLogger_.setLevel(Level.WARNING);
        logger_.setLevel(Level.INFO);
    }

    /**
     * This class cannot be instantiated.
     */
    private ResourceUtility()
    {}

    /**
     * @return The workspace.
     */
    public static IWorkspace getWorkspace()
    {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        return workspace;
    }

    /**
     * Removes the working set represented by 'workingSetName'.
     * 
     * @param workingSetName The name of the target working set.
     */
    public static void removeWorkingSet(String workingSetName)
    {
        IWorkingSetManager wsManager = getWorkingSetManager();
        IWorkingSet ws = getWorkingSet(workingSetName);
        wsManager.removeWorkingSet(ws);
    }

    /**
     * Adds the given project to the given working set. <br>
     * If the workspace does not exist, it is created automatically.
     * 
     * @param workingSetName The name of the working set.
     * @param project The project that will be added to the working set.
     */
    public static void addToWorkingSet(String workingSetName, IProject project)
    {
        IWorkingSet ws = getWorkingSet(workingSetName);
        IAdaptable [] existingElements = ws.getElements();
        boolean exists = false;
        for (IAdaptable elt: existingElements)
        {
            IProject pro = (IProject) elt.getAdapter(IProject.class);
            if (pro != null && pro.getName().equals(project.getName()))
            {
                exists = true;
                break;
            }
        }
        if (!exists)
        {
            logger_.info("Adding project = " + project.getName() + " to working set = " + workingSetName);
            IAdaptable [] elements = new IAdaptable [existingElements.length + 1];
            for (int a = 0; a < existingElements.length; a++)
                elements[a] = existingElements[a];
            IAdaptable adaptedProject = ws.adaptElements(new IAdaptable [] {project})[0];
            elements[existingElements.length] = adaptedProject;
            ws.setElements(elements);
        }
    }

    /**
     * Returns the project that is represented by 'name'.
     * 
     * @param name Name of the project in the workspace.
     * @return The project that is represented by 'name'.
     */
    public static IProject getProject(String name)
    {
        IWorkspace workspace = getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IProject project = root.getProject(name);
        return project;
    }

    /**
     * Returns all projects in the workspace.
     * 
     * @return All projects in the workspace.
     */
    public static IProject [] getAllProjects()
    {
        IWorkspace workspace = getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        return root.getProjects();
    }

    /**
     * Returns the members of the given container.
     * 
     * @param container The input container.
     * @return The members of the given container.
     */
    public static IResource [] getMembers(IContainer container)
    {
        IResource [] result = null;
        try
        {
            result = container.members();
        }
        catch (CoreException e)
        {
            logger_.log(Level.SEVERE, "Cannot get container members for container = " + container.getName() + "!", e);
        }
        return result;
    }

    /**
     * Returns the file represented by the given path.
     * 
     * @param path The path that represents the result file.
     * @return The file that is represented by the path.
     */
    public static IFile getFile(IPath path)
    {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        return root.getFile(path);
    }

    /**
     * Returns <code>true</code> if the file is on the class path of its project, <code>false</code> otherwise.
     * 
     * @param file File that is being analyzed.
     * @return <code>true</code> if the file is on the class path of its project, <code>false</code> otherwise.
     */
    public static boolean isOnClassPath(IFile file)
    {
        IJavaProject javaProject = JavaCore.create(file.getProject());
        IJavaElement javaElement = JavaCore.create(file);
        return javaProject.isOnClasspath(javaElement);
    }

    /**
     * Returns <code>true</code> if the given file has java-like-extension, <code>false</code> otherwise.
     * 
     * @param file File that is being analyzed.
     * @return <code>true</code> if the given file has java-like-extension, <code>false</code> otherwise.
     */
    public static boolean isJavaLike(IFile file)
    {
        /*
         * For now, this methods seems to return only '.java'. Question: When and why it can return more?
         */
        String [] extensions = JavaCore.getJavaLikeExtensions();
        String ext = file.getFileExtension();
        for (String extension: extensions)
        {
            if (extension.equals(ext))
                return true;
        }
        return false;
    }

    /**
     * Synchronizes the given resource with the underlying file system if necessary.
     * 
     * @param resource The resource that will be synchronized with the underlying file system.
     * @return <code>true</code> if the synchronization is successful, <code>false</code> otherwise.
     */
    public static boolean syncWithFileSystemIfNecessary(IResource resource)
    {
        boolean result = true;
        if (!resource.isSynchronized(IResource.DEPTH_INFINITE))
        {
            try
            {
                resource.refreshLocal(IResource.DEPTH_INFINITE, null);
            }
            catch (CoreException e)
            {
                result = false;
                logger_.log(Level.SEVERE,
                        "Cannot refresh resource = " + resource.getProjectRelativePath() + " locally.", e);
            }
        }
        return result;
    }

    /**
     * Deletes the resource if it is in sync with the file system. <br>
     * This method assumes that the resource exists in the file system.
     * 
     * @param resource Resource that should be deleted.
     * @return <code>true</code> if the deletion is successful, <code>false</code> otherwise.
     */
    public static boolean deleteResource(IResource resource)
    {
        boolean result = true;
        try
        {
            resource.delete(false, null);
            logger_.fine("Deleting resource = " + resource.getName());
        }
        catch (CoreException e)
        {
            result = false;
            logger_.log(Level.SEVERE, "Cannot delete resource = " + resource.getName() + "!", e);
        }
        return result;
    }

    /**
     * Copies the resource represented by 'source' to the resource represented by 'target'. <br>
     * This method assumes that the resource represented by 'original' exists, and is sync with the file system. <br>
     * If target exists, it is deleted before the copy.
     * 
     * @param source The source file.
     * @param target The target file.
     * @return <code>true</code> if the copy is successful, <code>false</code> otherwise.
     */
    public static boolean copyResource(IResource source, IResource target)
    {
        if (target.exists())
            deleteResource(target);
        
        boolean result = true;
        IPath path = target.getFullPath();
        try
        {
            source.copy(path, true, null);
            logger_.fine("Copied " + target.getName() + " to shadow project.");
        }
        catch (CoreException e)
        {
            result = false;
            logger_.log(Level.SEVERE, "Cannot create " + source.getLocation() + ". Path used during copy = " + path, e);
        }
        return result;
    }

    /**
     * Copies the content of 'source' to the 'target'. <br>
     * This method assumes that the target exists.
     * 
     * @param source The source file.
     * @param target The target file.
     * @return <code>true</code> if the copy is successful, <code>false</code> otherwise.
     * @see #createFile(IFile, IFile)
     */
    public static boolean copyFile(IFile source, IFile target)
    {
        boolean result = true;
        try
        {
            target.setContents(source.getContents(), true, false, null);
            logger_.fine("Copied " + target.getName() + " to shadow project.");
        }
        catch (CoreException e)
        {
            result = false;
            logger_.log(Level.SEVERE, "Cannot copy the contents for file = " + source.getName()
                    + " from original project to the shadow project", e);
        }
        return result;
    }

    /**
     * Creates the given file with the contents of the 'content' file. <br>
     * If the given file exists, this method first deletes it.
     * 
     * @param file The input file.
     * @param content Initial content of the newly created file.
     * @return <code>true</code> if the folder is created successfully, <code>false</code> otherwise.<br>
     */
    public static boolean createFile(IFile file, IFile content)
    {
        boolean result = true;
        try
        {
            if (file.exists())
                deleteResource(file);
            file.create(content.getContents(), true, null);
        }
        catch (CoreException e)
        {
            result = false;
            logger_.log(Level.SEVERE, "Cannot create file = " + file.getName() + "!", e);
        }
        return result;
    }

    /**
     * Creates the given folder. <br>
     * If the given folder exists, this method first deletes it.
     * 
     * @param folder The input folder.
     * @return <code>true</code> if the folder is created successfully, <code>false</code> otherwise.
     */
    public static boolean createFolder(IFolder folder)
    {
        boolean result = true;
        try
        {
            if (folder.exists())
                deleteResource(folder);
            folder.create(false, true, null);
        }
        catch (CoreException e)
        {
            result = false;
            logger_.log(Level.SEVERE, "Cannot create folder = " + folder.getName() + "!", e);
        }
        return result;
    }

    /**
     * Compares the given files and returns <code>true</code> if two given files are identical in content,
     * <code>false</code> otherwise. <br>
     * This method assumes that the files don't have any buffer associated with them and reads the content from disc.
     * 
     * @param file1 First input file.
     * @param file2 Second input file.
     * @return <code>true</code> if two given files differ in content, <code>false</code> otherwise.
     */
    public static boolean areFilesIdentical(IFile file1, IFile file2)
    {
        fileComparisonLogger_.info("Comparing files: " + file1.getFullPath().toString() + " vs. "
                + file2.getFullPath().toString());
        // Quickly look at the file sizes to see if they differ.
        try
        {
            if (areFilesSameSize(file1, file2))
            {
                fileComparisonLogger_.info("Files have the same size.");
                // if (isFirstModifiedLater(file1, file2))
                // {
                // fileComparisonLogger_
                // .info("The original file is modified later than the shadow file, returning false.");
                // return false;
                // }
            }
            else
            {
                fileComparisonLogger_.info("Files does not have the same size, returning false.");
                return false;
            }
        }
        catch (CoreException e)
        {
            fileComparisonLogger_.log(Level.WARNING, "Tried to check file sizes for files " + file1.getName() + " and "
                    + file2.getName()
                    + " and it failed. However, this was an optimization and does not affect the execution.", e);
        }
        InputStream is1 = null;
        InputStream is2 = null;
        boolean same = false;
        try
        {
            is1 = file1.getContents();
            is2 = file2.getContents();
            boolean end = false;
            int size = 4 * 1024;
            byte [] data1 = new byte [size];
            byte [] data2 = new byte [size];
            while (!end)
            {
                int length1 = is1.read(data1);
                int length2 = is2.read(data2);
                if (length1 == -1 && length1 == length2)
                {
                    // Both files are read completely.
                    same = true;
                    end = true;
                }
                else if (length1 == length2)
                {
                    // Read chunks have the same length, we need to compare them.
                    if (!Arrays.equals(data1, data2))
                    {
                        // Read chunks have different content. We detected that files are not same,
                        // terminate.
                        end = true;
                        fileComparisonLogger_.info("The files differ in content for: " + new String(data1) + " vs. " + new String(data2));
                    }
                }
                else
                    // Read chunks have different length. We detected that files are not same,
                    // terminate.
                    end = true;
            }
        }
        catch (CoreException e)
        {
            fileComparisonLogger_.log(Level.SEVERE,
                    "Cannot get contents of file (original or shadow) = " + file1.getName(), e);
        }
        catch (IOException e)
        {
            fileComparisonLogger_.log(Level.SEVERE,
                    "Cannot read contents of file (original or shadow) = " + file1.getName(), e);
        }
        finally
        {
            try
            {
                if (is1 != null)
                    is1.close();
                if (is2 != null)
                    is2.close();
            }
            catch (IOException e)
            {
                fileComparisonLogger_.log(Level.SEVERE, "Cannot close input stream for file (original or shadow) = "
                        + file1.getName(), e);
            }
        }
        fileComparisonLogger_.info("Comparison result: " + same);
        return same;
    }
    
    /**
     * Returns the information for the given file.
     * 
     * @param file The input file.
     * @return The information for the given file.
     * @throws CoreException If the file information cannot be retrieved for the input.
     */
    public static IFileInfo getFileInfo(IFile file) throws CoreException
    {
        IFileStore fileStore = EFS.getStore(file.getLocationURI());
        return fileStore.fetchInfo();
    }

    /******************************
     ******** PRIVATE API *********
     ******************************/
    /**
     * Returns the working set represented by 'name'. <br>
     * If no such workspace exists, one is created and returned.
     * 
     * @param name The name of the working set.
     * @return The working set represented by 'name'.
     */
    private static IWorkingSet getWorkingSet(String name)
    {
        IWorkingSetManager wsManager = getWorkingSetManager();
        IWorkingSet [] workingSets = wsManager.getAllWorkingSets();
        IWorkingSet result = null;
        for (IWorkingSet ws: workingSets)
        {
            if (ws.getName().equals(name))
            {
                result = ws;
                break;
            }
        }
        if (result == null)
        {
            logger_.info("Creating working set = " + name);
            result = wsManager.createWorkingSet(name, new IAdaptable [0]);
            wsManager.addWorkingSet(result);
        }
        return result;
    }

    /**
     * @return The working set manager for the workspace.
     */
    private static IWorkingSetManager getWorkingSetManager()
    {
        IWorkingSetManager result = PlatformUI.getWorkbench().getWorkingSetManager();
        return result;
    }

    /**
     * Compares two files and returns <code>true</code> if the input files have the same size and <code>false</code>
     * otherwise.
     * 
     * @param file1 The first file.
     * @param file2 The second file.
     * @return Returns <code>true</code> if the input files have the same size and <code>false</code> otherwise.
     * @throws CoreException If the file size cannot be retrieved for any of the files.
     */
    private static boolean areFilesSameSize(IFile file1, IFile file2) throws CoreException
    {
        IFileInfo fileInfo1 = getFileInfo(file1);
        IFileInfo fileInfo2 = getFileInfo(file2);
        return fileInfo1.getLength() == fileInfo2.getLength();
    }
}
