package com.esotericsoftware.utils;

public interface LineSink
{
    void addLine( String pLine );

    public static final LineSink SYSTEM_OUT = new LineSink()
    {
        @Override public void addLine( String pLine )
        {
            System.out.println( pLine );
        }
    };
}
