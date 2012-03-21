package edu.washington.cs.quickfix.speculation.model;

public class Pair <Value1, Value2>
{
    private Value1 value1_;
    private Value2 value2_;

    public Pair(Value1 value1, Value2 value2)
    {
        value1_ = value1;
        value2_ = value2;
    }

    public Value1 getValue1()
    {
        return value1_;
    }

    public Value2 getValue2()
    {
        return value2_;
    }
}
