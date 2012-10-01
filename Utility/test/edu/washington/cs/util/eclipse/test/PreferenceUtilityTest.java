package edu.washington.cs.util.eclipse.test;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.washington.cs.util.eclipse.PreferencesUtility;

public class PreferenceUtilityTest
{
    private PreferencesUtility persistentPreferences_;
    private PreferencesUtility nonPersistentPreferences_;

    private final static String PERSISTENT_TEST_PLUG_IN_ID = "edu.washington.cs.utility.persistence";
    private final static String NOT_PERSISTENT_TEST_PLUG_IN_ID = "edu.washington.cs.utility.non.persistence";
    
    private final static String BOOLEAN_KEY = "Test Boolean";
    private final static String INTEGER_KEY = "Test Integer";
    
    private final static boolean BOOLEAN_VALUE = true;
    private final static int INTEGER_VALUE = 42;
    
    @Before
    public void setUp()
    {
        persistentPreferences_ = new PreferencesUtility(PERSISTENT_TEST_PLUG_IN_ID);
        nonPersistentPreferences_ = new PreferencesUtility(NOT_PERSISTENT_TEST_PLUG_IN_ID);
        nonPersistentPreferences_.put(BOOLEAN_KEY, BOOLEAN_VALUE);
        nonPersistentPreferences_.put(INTEGER_KEY, INTEGER_VALUE);
    }

    @After
    public void tearDown()
    {
        // Note that we are not disposing the persistent preferences and this is totally logical so that we can test the
        // persistence in the next run.
        persistentPreferences_ = null;
        nonPersistentPreferences_.dispose();
        nonPersistentPreferences_ = null;
    }

    // Note that this test will always fail for the first time it is run on a new workspace since it is checking for
    // persistence and it needs to load Eclipse twice to check it.
    @Test
    public void testPersistence()
    {
        boolean booleanValue = persistentPreferences_.getBoolean(BOOLEAN_KEY);
        if (booleanValue != BOOLEAN_VALUE)
        {
            persistentPreferences_.put(BOOLEAN_KEY, BOOLEAN_VALUE);
            persistentPreferences_.save();
        }
        
        int integerValue = persistentPreferences_.get(INTEGER_KEY, 0);
        if (integerValue != INTEGER_VALUE)
        {
            persistentPreferences_.put(INTEGER_KEY, INTEGER_VALUE);
            persistentPreferences_.save();
        }
        Assert.assertEquals(BOOLEAN_VALUE, booleanValue);
        Assert.assertEquals(INTEGER_VALUE, integerValue);
    }
    
    @Test
    public void testNonPersistence()
    {
        boolean booleanValue = nonPersistentPreferences_.getBoolean(BOOLEAN_KEY);
        int integerValue = nonPersistentPreferences_.get(INTEGER_KEY, 0);
        Assert.assertEquals(BOOLEAN_VALUE, booleanValue);
        Assert.assertEquals(INTEGER_VALUE, integerValue);
        
    }
}
