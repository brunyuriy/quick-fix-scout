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

import edu.washington.cs.util.eclipse.model.Squiggly;

/**
 * This utility class provides static helper methods for building projects and manipulating markers and problem
 * locations. <br>
 * Currently offered functionality is:
 * <ul>
 * <li>Building a given project incrementally.</li>
 * <li>Retrieving the compilation errors and warnings for a given project.</li>
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
     * Returns the number of compilation errors in the given project.
     * 
     * @param project The target project.
     * @return The number of compilation errors in the target project.
     * @see #calculateCompilationErrors(IProject)
     */
    public static int getNumberOfCompilationErrors(IProject project)
    {
        return calculateCompilationErrors(project).length;
    }

    /**
     * Returns the number of warnings in the given project.
     * 
     * @param project The target project.
     * @return The number of warnings in the target project.
     * @see #calculateWarnings(IProject)
     */
    public static int getNumberOfWarnings(IProject project)
    {
        return calculateWarnings(project).length;
    }

    /**
     * Returns the compilation errors in the given project.
     * 
     * @param project The target project.
     * @return The compilation errors in the target project.
     * @see #calculateSquigglies(IProject)
     */
    public static Squiggly [] calculateCompilationErrors(IProject project)
    {
        Squiggly [] squigglies = calculateSquigglies(project);
        ArrayList <Squiggly> result = new ArrayList <Squiggly>();
        for (Squiggly squiggly: squigglies)
        {
            try
            {
                if (squiggly.isCompilationError())
                    result.add(squiggly);
            }
            catch (CoreException e)
            {
                logger.log(Level.SEVERE, "Cannot get the marker attribute for squiggly: " + squiggly
                        + ", not including it in the results.", e);
            }
        }
        return result.toArray(new Squiggly [result.size()]);
    }

    /**
     * Returns the warnings in the given project.
     * 
     * @param project The target project.
     * @return The warnings in the target project.
     * @see #calculateSquigglies(IProject)
     */
    public static Squiggly [] calculateWarnings(IProject project)
    {
        Squiggly [] squigglies = calculateSquigglies(project);
        ArrayList <Squiggly> result = new ArrayList <Squiggly>();
        for (Squiggly squiggly: squigglies)
        {
            try
            {
                if (squiggly.isWarning())
                    result.add(squiggly);
            }
            catch (CoreException e)
            {
                logger.log(Level.SEVERE, "Cannot get the marker attribute for squiggly: " + squiggly
                        + ", not including it in the results.", e);
            }
        }
        return result.toArray(new Squiggly [result.size()]);
    }

    /**
     * Sets the auto-building property for the 'workspace' with the given value. <br>
     * This method first checks whether the auto-building value is equal to the given value or not. If it is already
     * what is requested, the method does nothing.
     * 
     * @param value The new value for auto-building property for the 'workspace'.
     */
    public static void setAutoBuilding(boolean value)
    {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription desc = workspace.getDescription();
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
                logger.log(Level.SEVERE, "Cannot set auto-build value to: " + value, e);
            }
            if (value == false)
                joinAutoBuilder();
        }
    }

    /**
     * Returns the current value of auto-building for the workspace.
     * 
     * @return The current value of auto-building for the workspace.
     */
    public static boolean isAutoBuilding()
    {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription desc = workspace.getDescription();
        return desc.isAutoBuilding();
    }

    /**
     * Builds the given project using the incremental builder of Eclipse.
     * 
     * @param project Project to be built.
     */
    public static void build(IProject project)
    {
        try
        {
            project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        }
        catch (CoreException e)
        {
            logger.log(Level.SEVERE, "Cannot build project: " + project.getName() + ". e.cause() = " + e.getCause(), e);
        }
    }

    /*************************
     ****** PRIVATE API ******
     *************************/
    /**
     * Suspends the caller thread until the current auto-building is complete. <br>
     * Probably used best after calling setAutoBuilding(false) first.
     * 
     * @see #setAutoBuilding(boolean)
     */
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
     * Retrieves the compilation errors and warnings for the given project.
     * 
     * @param project The target project.
     * @return The compilation errors and warnings for the target project.
     * @see #findJavaProblemMarkers(IProject)
     */
    private static Squiggly [] calculateSquigglies(IProject project)
    {
        IMarker [] markers = findJavaProblemMarkers(project);
        Squiggly [] result = new Squiggly [markers.length];
        for (int a = 0; a < markers.length; a++)
            result[a] = new Squiggly(markers[a]);
        return result;
    }

    /**
     * Returns the compilation errors and warnings for the given project. <br>
     * The project must be built before calling this method. <br>
     * 
     * @param project The target project.
     * @return The compilation errors and warnings for the target project.
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
}
