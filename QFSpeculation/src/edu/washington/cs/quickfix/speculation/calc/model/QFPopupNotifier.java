package edu.washington.cs.quickfix.speculation.calc.model;

public interface QFPopupNotifier
{
    void signalPopupClosed();
    void addQFPopupListener(QFPopupListener listener);
}
