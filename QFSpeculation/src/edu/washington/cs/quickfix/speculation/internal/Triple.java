package edu.washington.cs.quickfix.speculation.internal;

public class Triple <Value1, Value2, Value3>
{
    private final Value1 value1_;
    private final Value2 value2_;
    private final Value3 value3_;

    public Triple(Value1 v1, Value2 v2, Value3 v3)
    {
        value1_ = v1;
        value2_ = v2;
        value3_ = v3;
    }

    public Value1 getValue1()
    {
        return value1_;
    }

    public Value2 getValue2()
    {
        return value2_;
    }

    public Value3 getValue3()
    {
        return value3_;
    }
}
