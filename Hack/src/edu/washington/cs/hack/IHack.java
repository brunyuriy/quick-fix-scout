package edu.washington.cs.hack;

/**
 * Common implementation that is shared by all extender plug-ins.
 * 
 * @author Kivanc Muslu
 */
public interface IHack
{
    /**
     * This method is used by ActionManager class to set the id when contribution is added in plugin.xml
     * 
     * @param id Identifier of the extender.
     */
    void setId(String id);

    /**
     * @return the identifier of the extender.
     */
    String getId();
}
