package edu.washington.cs.synchronization.sync.internal;

import org.eclipse.core.resources.IFile;

public interface CursorChangedNotifier
{
    void addCursorChangedListener(CursorChangedListener listener);
    void signalCursorChange(int offset);
    void signalEditorFileChanged(IFile file);
}
