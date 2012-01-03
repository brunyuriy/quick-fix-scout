package edu.washington.cs.synchronization;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

@SuppressWarnings("restriction")
public class QuickFixScoutNavigatorFilter extends ViewerFilter
{
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element)
    {
        IProject project = null;
        if (element instanceof IProject)
             project = (IProject) element;
        else if (element instanceof JavaProject)
        {
            JavaProject javaProject = (JavaProject) element;
            project = javaProject.getProject();
        }
        return project == null || !shouldFilter(project);
    }
    
    public static boolean shouldFilter(IProject project)
    {
        return project.getName().startsWith(ProjectSynchronizer.SHADOW_PREFIX);
    }
}
