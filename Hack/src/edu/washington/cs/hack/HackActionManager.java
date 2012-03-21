package edu.washington.cs.hack;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import edu.washington.cs.util.log.LogHandlers;

/**
 * Action manager class implementation for common hook points. <br>
 * This plug-in permits the any plug-in to communicate with already existing Eclipse plug-ins (via hooks) and also
 * prevents the cyclic dependency issues. <br>
 * <br>
 * Note: This implementation is found at:
 * http://technical-tejash.blogspot.com/2010/03/eclipse-avoid-cyclic-dependency-between.html
 * 
 * @author Tejash Shah, Kivanc Muslu
 */
public abstract class HackActionManager
{
    public static final String PLUG_IN_ID = "edu.washington.cs.hack";
    /** logger for debugging. */
    private static final Logger logger = Logger.getLogger(HackActionManager.class.getName());
    static
    {
        LogHandlers.setMainHandler(System.err);
        logger.setLevel(Level.FINER);
    }
    /** constant that is used in the extension point declaration as 'id'. */
    private static final String ATTRIB_ID = "id";
    /** constant that is used in the extension point declaration as 'handlerClass'. */
    private static final String ATTRIB_CLASS = "handlerClass";
    /** mapping from the hardcoded ids to the extender plug-ins. */
    private final Map <String, IHack> handlers_;

    /**
     * Creates a general purpose hack action manager.
     */
    protected HackActionManager()
    {
        handlers_ = new HashMap <String, IHack>();
    }

    /***************
     * PRIVATE API *
     **************/
    /**
     * Loads the action handles (i.e., extender plug-in) with the given handler id.
     * 
     * @param handlerID id of the action handler.
     */
    protected void loadHandler(String handlerID)
    {
        IConfigurationElement [] configurationElements = getConfigurationElements(handlerID);
        logger.finer("configurationElements.length = " + configurationElements.length);
        for (int i = 0; i < configurationElements.length; i++)
        {
            try
            {
                Object extensionObject = configurationElements[i].createExecutableExtension(ATTRIB_CLASS);
                IHack contributor = (IHack) extensionObject;
                String id = configurationElements[i].getAttribute(ATTRIB_ID);
                contributor.setId(id);
                logger.finer("SampleActionManager: id = " + id);
                handlers_.put(id, contributor);
            }
            catch (CoreException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Given extension point id, retrieves all extender classes that use this extension point.
     * 
     * @param extensionPointID Identifier of the extension point that is given.
     * @return Extender classes that use the extension point represented by the id.
     */
    private IConfigurationElement [] getConfigurationElements(String extensionPointID)
    {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint contentTypesXP = registry.getExtensionPoint(extensionPointID);
        if (contentTypesXP == null)
            return new IConfigurationElement [0];
        logger.fine("contentTypes XP = " + contentTypesXP);
        IConfigurationElement [] allContentTypeCEs = contentTypesXP.getConfigurationElements();
        return allContentTypeCEs;
    }

    /***************************
     * GETTER & SETTER METHODS *
     **************************/
    protected IHack getHandler(String handlerID)
    {
        return handlers_.get(handlerID);
    }
}
