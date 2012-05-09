package edu.washington.cs.util.eclipse.test;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.washington.cs.util.eclipse.ResourceUtility;

public class ResourceUtilityTest
{
    private IWorkspace workspace_;
    private IProject [] projects_;
    private IProject resourceTestProject_;
    private IFile file0_;
    private IFile file1_;
    private IFile file2_;
    private IFile file3_;

    private static final String TEST_WS_NAME = "QFS Test";


    @Before
    public void setUp()
    {
        workspace_ = ResourceUtility.getWorkspace();
        projects_ = ResourceUtility.getAllProjects();
        resourceTestProject_ = ResourceUtility.getProject("Resource Test 01");
        file0_ = resourceTestProject_.getFile("src/p0/ResourceTest01.java");
        file1_ = resourceTestProject_.getFile("src/p1/ResourceTest01.java");
        file2_ = resourceTestProject_.getFile("src/p2/ResourceTest01.java");
        file3_ = resourceTestProject_.getFile("src/p3/ResourceTest01.java");
        for (IProject project: projects_)
            ResourceUtility.addToWorkingSet(TEST_WS_NAME, project);
    }

    @After
    public void tearDown()
    {
        workspace_ = null;
        projects_ = null;
        resourceTestProject_ = null;
        file0_ = null;
        file1_ = null;
        file2_ = null;
        file3_ = null;
        ResourceUtility.removeWorkingSet(TEST_WS_NAME);
    }

    @Test
    public void testInit()
    {
        Assert.assertNotNull(workspace_);
        Assert.assertTrue(resourceTestProject_.exists());
        Assert.assertTrue(file0_.exists());
        Assert.assertTrue(file1_.exists());
        Assert.assertTrue(file2_.exists());
        Assert.assertTrue(file3_.exists());
        // TODO I cannot assert this at the moment due to the fact that there are also some shadow projects created (I
        // don't know why at this moment) and I don't know the exact number of projects in the workspace.
        // Assert.assertEquals(5, projects_.length);
    }

    @Test
    public void testCompare()
    {
        boolean compare1 = ResourceUtility.areFilesIdentical(file0_, file1_);
        Assert.assertTrue(compare1);
        boolean compare2 = ResourceUtility.areFilesIdentical(file0_, file2_);
        Assert.assertFalse(compare2);
        boolean compare3 = ResourceUtility.areFilesIdentical(file0_, file3_);
        Assert.assertFalse(compare3);
    }

    @Test
    public void testFileOperations()
    {
        IFile file2Copy = resourceTestProject_.getFile("src/ResourceTest01.java");
        boolean result = ResourceUtility.createFile(file2Copy, file2_);
        Assert.assertTrue(result);
        boolean compare = ResourceUtility.areFilesIdentical(file2Copy, file2_);
        Assert.assertTrue(compare);

        IFile file3Copy = file2Copy;
        result = ResourceUtility.copyFile(file3_, file3Copy);
        Assert.assertTrue(result);
        compare = ResourceUtility.areFilesIdentical(file3Copy, file3_);
        Assert.assertTrue(compare);

        IFile file1Copy = file3Copy;
        result = ResourceUtility.copyResource(file1_, file1Copy);
        Assert.assertTrue(result);
        compare = ResourceUtility.areFilesIdentical(file1Copy, file1_);
        Assert.assertTrue(compare);

        result = ResourceUtility.deleteResource(file1Copy);
        Assert.assertTrue(result);
        Assert.assertFalse(file1Copy.exists());
    }
}
