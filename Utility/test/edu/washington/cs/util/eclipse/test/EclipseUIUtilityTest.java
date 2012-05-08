package edu.washington.cs.util.eclipse.test;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.PartInitException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.washington.cs.util.eclipse.EclipseUIUtility;
import edu.washington.cs.util.eclipse.ResourceUtility;
import edu.washington.cs.util.exception.NotInitializedException;

public class EclipseUIUtilityTest
{
    
    private IProject markerlessProject_;
    private IFile markerlessFile_;
    
    @Before
    public void setUp()
    {
        markerlessProject_ = ResourceUtility.getProject("Builder Test 01");
        markerlessFile_ = markerlessProject_.getFile("src/BuilderTest01.java");
    }

    @After
    public void tearDown()
    {
        markerlessProject_ = null;
        markerlessFile_ = null;
    }

    @Test
    public void testInit()
    {
        Assert.assertTrue(markerlessProject_.exists());
        Assert.assertTrue(markerlessFile_.exists());
        
        try
        {
            IFile activeFile = EclipseUIUtility.getActiveEditorFile();
            if (activeFile != null)
            {
                EclipseUIUtility.closeActiveEditor();
                activeFile = EclipseUIUtility.getActiveEditorFile();
                Assert.assertNull(activeFile);
            }
        }
        catch (NotInitializedException e)
        {
            Assert.fail();
        }
    }
    
    @Test
    public void testEditorFileOperations()
    {
        try
        {
            IFile activeFile = EclipseUIUtility.getActiveEditorFile();
            Assert.assertNull(activeFile);
            
            EclipseUIUtility.openFileInEditor(markerlessFile_);
            activeFile = EclipseUIUtility.getActiveEditorFile();
            Assert.assertNotNull(activeFile);
            Assert.assertEquals(markerlessFile_.getFullPath().toString(), activeFile.getFullPath().toString());
            
            EclipseUIUtility.closeActiveEditor();
            activeFile = EclipseUIUtility.getActiveEditorFile();
            Assert.assertNull(activeFile);
        }
        catch (NotInitializedException e)
        {
            Assert.fail();
        }
        catch (PartInitException e)
        {
            Assert.fail();
        }
    }
}
