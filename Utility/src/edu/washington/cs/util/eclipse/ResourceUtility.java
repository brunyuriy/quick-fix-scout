package edu.washington.cs.util.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * This utility class provides static helper methods for resource handling. <br>
 * Currently offered functionality is:
 * <ul>
 * <li>Deleting a resource.</li>
 * <li>Copying a resource.</li>
 * <li>Creating a file.</li>
 * <li>Getting the file represented by an {@link IPath}.</li>
 * <li>Creating a folder.</li>
 * <li>Getting the project represented by a sting.</li>
 * </ul>
 * 
 * @author Kivanc Muslu
 */
public class ResourceUtility
{
    public static final String PLUG_IN_ID = "edu.washington.cs.util";
    public static final String VERSION_URL = "http://www.kivancmuslu.com/Quick_Fix_Scout_Files/version.txt";
    /** Logger for debugging. */
    private static final Logger logger = Logger.getLogger(ResourceUtility.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }

    /**
     * This class cannot be instantiated.
     */
    private ResourceUtility()
    {}

    /**************
     * PUBLIC API *
     *************/
    /**
     * Returns the project that is represented by 'name'.
     * 
     * @param name Name of the project in the workspace.
     * @return Project that is represented by 'name'.
     */
    public static IProject getProject(String name)
    {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IProject project = root.getProject(name);
        return project;
    }

    public static IResource [] getMembers(IContainer container)
    {
        IResource [] result = null;
        try
        {
            result = container.members();
        }
        catch (CoreException e)
        {
            logger.log(Level.SEVERE, "Cannot get container members for container = " + container.getName() + "!", e);
        }
        return result;
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
        // Should not fail since first isJavaLike(...) called.
        IJavaElement javaElement = JavaCore.create(file);
        return javaProject.isOnClasspath(javaElement);
    }

    /**
     * Returns <code>true</code> if the given file has java-like-extension with respect to Eclipse ( {@link JavaCore}),
     * <code>false</code> otherwise.
     * 
     * @param file File that is being analyzed.
     * @return <code>true</code> if the given file has java-like-extension with respect to Eclipse ( {@link JavaCore}),
     *         <code>false</code> otherwise.
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
     * Returns <code>true</code> if two given files differ in content, <code>false</code> otherwise. <br>
     * This method assumes that the files don't have any buffer associated with them and reads the content from disc.
     * 
     * @param file1 First file to check the content.
     * @param file2 Second file to check the content.
     * @return <code>true</code> if two given files differ in content, <code>false</code> otherwise.
     */
    public static boolean areFilesIdentical(IFile file1, IFile file2)
    {
        // TODO Add a quick check over the files length so that you don't need to read the whole
        // files.
        InputStream is1 = null;
        InputStream is2 = null;
        boolean same = false;
        try
        {
            is1 = file1.getContents();
            is2 = file2.getContents();
            boolean end = false;
            while (!end)
            {
                int size = 4 * 1024;
                byte [] data1 = new byte [size];
                byte [] data2 = new byte [size];
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
                        // Read chunks have different content. We detected that files are not same,
                        // terminate.
                        end = true;
                }
                else
                    // Read chunks have different length. We detected that files are not same,
                    // terminate.
                    end = true;
            }
        }
        catch (CoreException e)
        {
            logger.log(Level.SEVERE, "Cannot get contents of file (original or shadow) = " + file1.getName(), e);
        }
        catch (IOException e)
        {
            logger.log(Level.SEVERE, "Cannot read contents of file (original or shadow) = " + file1.getName(), e);
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
                logger.log(Level.SEVERE,
                        "Cannot close input stream for file (original or shadow) = " + file1.getName(), e);
            }
        }
        return same;
    }

    /**
     * Synchronizes the given resource with the underlying file system if necessary.
     * 
     * @param resource The resource that will be synchronized with the underlying file system.
     * @return <code>true</code> if the operation completed with success, <code>false</code> if an internal exception
     *         occurred.
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
                logger.log(Level.SEVERE,
                        "Cannot refresh resource = " + resource.getProjectRelativePath() + " locally.", e);
            }
        }
        return result;
    }

    /**
     * Deletes the resource if it is in sync with the file system. <br>
     * This method assumes that the resource exists in the file system. <br>
     * This method also assumes that the resource is in sync with the file system.
     * 
     * @param resource Resource that should be deleted.
     * @return <code>true</code> if the operation completed with success, <code>false</code> if an internal exception
     *         occurred.
     */
    public static boolean deleteResource(IResource resource)
    {
        boolean result = true;
        try
        {
            resource.delete(false, null);
            logger.fine("Deleting resource = " + resource.getName());
        }
        catch (CoreException e)
        {
            result = false;
            logger.log(Level.SEVERE, "Cannot delete resource = " + resource.getName() + "!", e);
        }
        return result;
    }

    /**
     * Copies the resource represented by 'original' to the resource represented by 'target'. <br>
     * This method assumes that the resource represented by 'original' exists, and is sync with the file system. <br>
     * This method also assumes that the resource represented by 'target' does not exist in the file system.
     * 
     * @param source Source file that will be copied from.
     * @param target Destination file that will be copied to.
     * @return <code>true</code> if the operation completed with success, <code>false</code> if an internal exception
     *         occurred.
     */
    public static boolean copyResource(IResource source, IResource target)
    {
        boolean result = true;
        IPath path = target.getFullPath();
        try
        {
            source.copy(path, true, null);
            logger.fine("Copied " + target.getName() + " to shadow project.");
        }
        catch (CoreException e)
        {
            result = false;
            logger.log(Level.SEVERE, "Cannot create " + source.getLocation()
                    + " in the shadow project! Path used during copy = " + path, e);
        }
        return result;
    }

    public static boolean copyFile(IFile source, IFile target)
    {
        boolean result = true;
        try
        {
            target.setContents(source.getContents(), true, false, null);
            logger.fine("Copied " + target.getName() + " to shadow project.");
        }
        catch (CoreException e)
        {
            result = false;
            logger.log(Level.SEVERE, "Cannot copy the contents for file = " + source.getName()
                    + " from original project to the shadow project", e);
        }
        return result;
    }

    /**
     * Creates the folder. <br>
     * This method represents that the folder does not exist in the file system.
     * 
     * @param folder Folder to be created.
     * @return <code>true</code> if the operation completed with success, <code>false</code> if an internal exception
     *         occurred.
     */
    public static boolean createFolder(IFolder folder)
    {
        boolean result = true;
        try
        {
            folder.create(false, true, null);
        }
        catch (CoreException e)
        {
            result = false;
            logger.log(Level.SEVERE, "Cannot create folder = " + folder.getName() + "!", e);
        }
        return result;
    }

    /**
     * Creates the file with the given content. <br>
     * If the given file exists, this method first deletes it.
     * 
     * @param file File to be created.
     * @param content Initial content of the file.
     * @return <code>true</code> if the operation completed with success, <code>false</code> if an internal exception
     *         occurred.
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
            logger.log(Level.SEVERE, "Cannot create file = " + file.getName() + "!", e);
        }
        return result;
    }

    /**
     * Returns the file represented by the path.
     * 
     * @param path The path that is representing the file.
     * @return File that is represented by the path.
     */
    public static IFile getFile(IPath path)
    {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IFile result = null;
        try
        {
            result = root.getFile(path);
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Cannot retrieve file for path = " + path.toString(), e);
        }
        return result;
    }

    public static Object getPluginVersion(String pluginId)
    {
        return Platform.getBundle(pluginId).getHeaders().get("Bundle-Version");
    }

    public static void logSystemInformation(String... pluginIds)
    {
        String os = System.getProperty("os.name");
        logger.info("=== System information ===");
        logger.info("OS name = " + os);
        for (String pluginId: pluginIds)
            logger.info(pluginId + " v." + getPluginVersion(pluginId));
        logger.info("=== System information ===");
    }
    
    public static String getExternalVersion(String ... pluginIds)
    {
        String [] internalVersions = new String [pluginIds.length];
        for (int a = 0; a < internalVersions.length; a++)
            internalVersions[a] = (String) getPluginVersion(pluginIds[a]);
        return createExternalVersion(internalVersions);
    }
    
    private static String createExternalVersion(String [] internalVersions)
    {
        int major = 0;
        int minor = 0;
        int micro = 0;
        for (String externalVersion: internalVersions)
        {
            String [] parts = externalVersion.split("\\.");
            if (parts.length != 3)
            {
                for (String part: parts)
                    System.out.println(part);
                throw new RuntimeException("External Version ID cannot be calculated.");
            }
            major += Integer.parseInt(parts[0].trim());
            minor += Integer.parseInt(parts[1].trim());
            micro += Integer.parseInt(parts[2].trim());
        }
        
        double majorAvg = major * 1.0 / internalVersions.length;
        double minorAvg = minor * 1.0 / internalVersions.length;
        double microAvg = micro * 1.0 / internalVersions.length;
        String result = String.format("%.2f:%.2f:%.2f", majorAvg, minorAvg, microAvg);
        return result;
    }

    public static void checkForUpdates(String pluginName, boolean showNetworkError, String... pluginIds)
    {
        String installedVersion = getExternalVersion(pluginIds);
        HashMap <String, String> versionMap = new HashMap <String, String>();
        readVersionMap(showNetworkError, versionMap);
        boolean recent = true;
        ArrayList <String> relatedVersions = new ArrayList <String>();
        for (String pluginId: pluginIds)
        {
            String currentVersion = versionMap.get(pluginId);
            Object version = getPluginVersion(pluginId);
            relatedVersions.add(currentVersion);
            if (currentVersion != null && !version.equals(currentVersion))
                recent = false;
        }
        if (recent)
        {
            if (showNetworkError)
                EclipseUIUtility.showInformationDialog("You are already using the most recent version.",
                        "No New Version Avaliable", 300);
            logger.info("User is using the final version (" + installedVersion + ") for " + pluginName + " plug-in");
        }
        else
        {
            String externalName = pluginName;
            if (pluginName.equals("Speculator"))
                externalName = "Evaluator";
            String currentVersion = createExternalVersion(relatedVersions.toArray(new String [relatedVersions.size()]));
            //@formatter:off
            EclipseUIUtility.showInformationDialog("<div align=left>Quick Fix Scout plug-in (" + externalName + " feature) is outdated.<br>" +
                    "A new version is available at: http://www.kivancmuslu.com/Quick_Fix_Scout/Releases<br><br>" +
                    "Installed version = " + installedVersion + ", current version = " + currentVersion + "</div>"
                    , "New Version Available!", 450);
            //@formatter:on
        }
    }
    
    private static void readVersionMap(boolean showError, HashMap <String, String> versionMap)
    {
        try
        {
            URL versionURL = new URL(ResourceUtility.VERSION_URL);
            Scanner reader = new Scanner(versionURL.openStream());
            while (reader.hasNext())
            {
                String line = reader.nextLine();
                String [] parts = line.split(" - ");
                if (parts.length != 2)
                    logger.severe("Malformed version information line for = " + line);
                else
                    versionMap.put(parts[0].trim(), parts[1].trim());
            }
            reader.close();
        }
        catch (MalformedURLException e)
        {
            assert false: "The version URL cannot be malformed!";
        }
        catch (IOException e)
        {
            if (showError)
                //@formatter:off
                EclipseUIUtility.showErrorDialog("The plug-in cannot check version information from server<br>"
                                + "Please make sure that you are connected to the Internet and try again.",
                        "Cannot Check for Updates", 300);
            //@formatter:on
        }
    }
}
