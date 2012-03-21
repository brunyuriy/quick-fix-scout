package edu.washington.cs.util.eclipse;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import edu.washington.cs.util.eclipse.model.Squiggly;

/**
 * This utility class provides static helper methods for building projects and manipulating markers and problem
 * locations. <br>
 * Currently offered functionality is:
 * <ul>
 * <li>Building a given project incrementally.</li>
 * <li>Calculating the number of compilation errors in a given project.</li>
 * <li>Getting the java problem markers in a given project.</li>
 * <li>Getting the {@link IProblemLocation} in a given project.</li>
 * </ul>
 * 
 * @author Kivanc Muslu
 */
public class BuilderUtility
{
    /** Logger for debugging. */
    private static final Logger logger = Logger.getLogger(BuilderUtility.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }

    /**
     * This class cannot be instantiated.
     */
    private BuilderUtility()
    {}

    /**************
     * PUBLIC API *
     *************/
    /**
     * Returns the number of compilation errors in the copy project.
     * 
     * @return The number of compilation errors in the copy project.
     * @see #calculateCompilationErrorMarkers(IProject)
     */
    public static int getNumberOfCompilationErrors(IProject project)
    {
        return calculateCompilationErrorMarkers(project).length;
    }

    public static Squiggly [] calculateCompilationErrors(IProject project)
    {
        ArrayList <Squiggly> result = new ArrayList <Squiggly>();
        IMarker [] markers = null;
        try
        {
            markers = findJavaProblemMarkers(project);
            for (IMarker marker: markers)
            {
                Integer severityType = (Integer) marker.getAttribute(IMarker.SEVERITY);
                if (severityType.intValue() == IMarker.SEVERITY_ERROR)
                {
                    logger.finer("Returning marker = " + marker);
                    result.add(new Squiggly(marker));
                }
            }
        }
        catch (CoreException e)
        {
            logger.log(Level.SEVERE, "Cannot get the marker or marker attribute for project: " + project.getName(), e);
        }
        return result.toArray(new Squiggly [result.size()]);
    }

    public static Squiggly [] calculateSquigglies(IProject project)
    {
        IMarker [] markers = findJavaProblemMarkers(project);
        Squiggly [] result = new Squiggly [markers.length];
        for (int a = 0; a < markers.length; a++)
            result[a] = new Squiggly(markers[a]);
        return result;
    }

    /**
     * Calculates and returns the markers available in the current project. <br>
     * <br>
     * This method assumes that the project is already built and saved (i.e., up to date).
     * 
     * @param project Project that is being analyzed.
     * @return The markers that the project generates with severity error (i.e., compilation error markers).
     */
    private static IMarker [] calculateCompilationErrorMarkers(IProject project)
    {
        ArrayList <IMarker> result = new ArrayList <IMarker>();
        IMarker [] markers = null;
        try
        {
            markers = findJavaProblemMarkers(project);
            for (IMarker marker: markers)
            {
                Integer severityType = (Integer) marker.getAttribute(IMarker.SEVERITY);
                if (severityType.intValue() == IMarker.SEVERITY_ERROR)
                {
                    logger.finer("Returning marker = " + marker);
                    result.add(marker);
                }
            }
        }
        catch (CoreException e)
        {
            logger.log(Level.SEVERE, "Cannot get the marker or marker attribute for project: " + project.getName(), e);
        }
        return result.toArray(new IMarker [result.size()]);
    }

    /**
     * Returns all java problem markers in the given project. <br>
     * The project must be built before calling this method. <br>
     * Returned markers correspond to the all markers generated for the Java files including warning, error and
     * exception markers.
     * 
     * @param project The project that the markers will be retrieved from.
     * @return All java problem markers in the given project.
     * @see IMarker
     */
    private static IMarker [] findJavaProblemMarkers(IProject project)
    {
        IMarker [] markers = null;
        try
        {
            markers = project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        }
        catch (CoreException e)
        {
            logger.log(Level.SEVERE, "Cannot find java problem markers for project: " + project.getName(), e);
        }
        return markers;
    }
    
    public static void setAutoBuilding(boolean value)
    {
        IWorkspace workspace= ResourcesPlugin.getWorkspace();
        IWorkspaceDescription desc= workspace.getDescription();
        boolean current = desc.isAutoBuilding();
        if (current != value)
        {
            desc.setAutoBuilding(value);
            try
            {
                workspace.setDescription(desc);
            }
            catch (CoreException e)
            {
                logger.log(Level.SEVERE, "Cannot set auto-build value to: " + value + ". e.cause() = " + e.getCause(), e);
            }
            if (value == false)
                joinAutoBuilder();
        }
    }
    
    public static boolean isAutoBuilding()
    {
        IWorkspace workspace= ResourcesPlugin.getWorkspace();
        IWorkspaceDescription desc= workspace.getDescription();
        return desc.isAutoBuilding();
    }
    
    @SuppressWarnings("deprecation")
    private static void joinAutoBuilder()
    {
        try
        {
            Platform.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
        }
        catch (OperationCanceledException e)
        {
            logger.log(Level.SEVERE, "Cannot join with auto-builder. e.cause() = " + e.getCause(), e);
        }
        catch (InterruptedException e)
        {
            logger.log(Level.SEVERE, "Cannot join with auto-builder. e.cause() = " + e.getCause(), e);
        }
    }

    /**
     * Builds the project using the incremental builder of Eclipse. <br>
     * This process joins to Eclipse auto-builder if auto-build is activated for the project or invokes an incremental
     * build itself.
     * 
     * @param project Project to be built.
     */
    public static void build(IProject project)
    {
//        IWorkspace workspace= ResourcesPlugin.getWorkspace();
//        IWorkspaceDescription desc= workspace.getDescription();
//        boolean isAutoBuilding= desc.isAutoBuilding(); 
        try
        {
//            if (isAutoBuilding)
//            {
//                desc.setAutoBuilding(false);
//                workspace.setDescription(desc); 
//            }
//                Platform.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
//            else
                project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
//           if (isAutoBuilding)
//           {
//               desc.setAutoBuilding(true);
//               workspace.setDescription(desc);
//           }
        }
        catch (CoreException e)
        {
            logger.log(Level.SEVERE, "Cannot build project: " + project.getName() + ". e.cause() = " + e.getCause(), e);
        }
//        catch (OperationCanceledException e)
//        {
//            logger.log(Level.SEVERE, "Cannot build project: " + project.getName() + ". e.cause() = " + e.getCause(), e);
//        }
//        catch (InterruptedException e)
//        {
//            logger.log(Level.SEVERE, "Cannot build project: " + project.getName() + ". e.cause() = " + e.getCause(), e);
//        }
    }
}
