package edu.cs.washington.quickfix.speculation.converter;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.ui.cleanup.ICleanUp;

@SuppressWarnings("restriction")
public class ICleanUpConverter extends EclipseObjectConverter
{
    public ICleanUpConverter(IProject original)
    {
        super(original, "clean up");
    }

    public ICleanUp convert(ICleanUp shadowCleanUp, FixCorrectionProposal shadowProposal)
    {
        ICleanUp result = shadowCleanUp;
        if (shadowCleanUp instanceof UnusedCodeCleanUp)
            result = convert((UnusedCodeCleanUp) shadowCleanUp, shadowProposal);
        else
            logUnknownObjectType(shadowCleanUp);
        return result;
    }

    private UnusedCodeCleanUp convert(UnusedCodeCleanUp shadowCleanUp, FixCorrectionProposal shadowProposal)
    {
        return shadowCleanUp;
    }
}
