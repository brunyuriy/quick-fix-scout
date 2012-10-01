package edu.washington.cs.hack;

public class Hack implements IHack
{
    private String id_;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setId(String id)
    {
        id_ = id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId()
    {
        return id_;
    }
}
