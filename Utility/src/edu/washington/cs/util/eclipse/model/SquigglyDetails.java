package edu.washington.cs.util.eclipse.model;

import org.eclipse.core.resources.IFile;

public class SquigglyDetails
{
    private final int line_;
    private final int offset_;
    private final IFile file_;
    
    public SquigglyDetails(IFile file, int line, int offset)
    {
        file_ = file;
        line_ = line;
        offset_ = offset;
    }
    
    public int getLine()
    {
        return line_;
    }
    
    public int getOffset()
    {
        return offset_;
    }
    
    public IFile getFile()
    {
        return file_;
    }
    
    public String toString()
    {
        return file_.getName() + ":" + line_ + ":" + offset_;
    }
}
