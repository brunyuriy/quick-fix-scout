package edu.washington.cs.util.eclipse.test;

import junit.framework.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.washington.cs.util.eclipse.BuilderUtility;
import edu.washington.cs.util.eclipse.ResourceUtility;
import edu.washington.cs.util.eclipse.model.Squiggly;

public class BuilderUtilityTest 
{
	private IProject markerlessProject_;
	private IProject oneWarningProject_;
	private IProject oneErrorProject_;
	private IProject oneWarningOneErrorProject_;
	
	@Before
	public void setUp()
	{
		markerlessProject_ = ResourceUtility.getProject("Builder Test 01");
		oneWarningProject_ = ResourceUtility.getProject("Builder Test 02");
		oneErrorProject_ = ResourceUtility.getProject("Builder Test 03");
		oneWarningOneErrorProject_ = ResourceUtility.getProject("Builder Test 04");
	}
	
	@After
	public void tearDown()
	{
		markerlessProject_ = null;
		oneWarningProject_ = null;
		oneErrorProject_ = null;
		oneWarningOneErrorProject_ = null;
	}
	
	@Test
	public void testInit()
	{
		Assert.assertTrue(markerlessProject_.exists());
		Assert.assertTrue(oneWarningProject_.exists());
		Assert.assertTrue(oneErrorProject_.exists());
		Assert.assertTrue(oneWarningOneErrorProject_.exists());
	}
	
	@Test
	public void testMarkerlessProject()
	{
		int warnings, errors;
		warnings = BuilderUtility.getNumberOfWarnings(markerlessProject_);
		errors = BuilderUtility.getNumberOfCompilationErrors(markerlessProject_);

		Assert.assertEquals(0, warnings);
		Assert.assertEquals(0, errors);
	}
	
	@Test
	public void testOneWarningProject()
	{
		int noWarnings, noErrors;
		noWarnings = BuilderUtility.getNumberOfWarnings(oneWarningProject_);
		noErrors = BuilderUtility.getNumberOfCompilationErrors(oneWarningProject_);

		Assert.assertEquals(1, noWarnings);
		Assert.assertEquals(0, noErrors);
		
		Squiggly warning = BuilderUtility.calculateWarnings(oneWarningProject_)[0];
		try 
		{
			String representation = warning.toDetailedString();
			Assert.assertEquals("BuilderTest02.java:4:20", representation);
		} catch (CoreException e) 
		{
			Assert.fail();
		}
	}
	
	@Test
	public void testOneErrorProject()
	{
		int noWarnings, noErrors;
		noWarnings = BuilderUtility.getNumberOfWarnings(oneErrorProject_);
		noErrors = BuilderUtility.getNumberOfCompilationErrors(oneErrorProject_);

		Assert.assertEquals(0, noWarnings);
		Assert.assertEquals(1, noErrors);
		
		Squiggly error = BuilderUtility.calculateCompilationErrors(oneErrorProject_)[0];
		try 
		{
			String representation = error.toDetailedString();
			Assert.assertEquals("BuilderTest03.java:4:13", representation);
		} catch (CoreException e) 
		{
			Assert.fail();
		}
	}
	
	@Test
	public void testOneWarningOneErrorProject()
	{
		int noWarnings, noErrors;
		noWarnings = BuilderUtility.getNumberOfWarnings(oneWarningOneErrorProject_);
		noErrors = BuilderUtility.getNumberOfCompilationErrors(oneWarningOneErrorProject_);

		Assert.assertEquals(1, noWarnings);
		Assert.assertEquals(1, noErrors);
		
		Squiggly warning = BuilderUtility.calculateWarnings(oneWarningOneErrorProject_)[0];
		Squiggly error = BuilderUtility.calculateCompilationErrors(oneWarningOneErrorProject_)[0];
		try 
		{
			String errorRepresentation = error.toDetailedString();
			String warningRepresentation = warning.toDetailedString();
			Assert.assertEquals("BuilderTest04.java:4:20", warningRepresentation);
			Assert.assertEquals("BuilderTest04.java:8:9", errorRepresentation);
		} catch (CoreException e) 
		{
			Assert.fail();
		}
	}

}
