package com.esotericsoftware.utils;

import java.io.*;

public class Util
{
    public static Object deNull( Object pToTest, Object pDefaultValue )
    {
        return (pToTest != null) ? pToTest : pDefaultValue;
    }

    public static void assertNotNull( String pWhat, Object pToTest )
    {
        if ( pToTest == null )
        {
            throw new IllegalArgumentException( pWhat + " cannot be null." );
        }
    }

    public static void assertExists( String pWhat, File pToTest )
    {
        assertNotNull( pWhat, pToTest );
        if ( !pToTest.exists() )
        {
            throw new IllegalArgumentException( pWhat + " not found: " + pToTest.getAbsolutePath() );
        }
    }

    public static String replace( String pSource, String pOfInterest, String pReplaceWith )
    {
        for ( int at; -1 != (at = pSource.indexOf( pOfInterest )); )
        {
            pSource = pSource.substring( 0, at ) + pReplaceWith + pSource.substring( at + pOfInterest.length() );
        }
        return pSource;
    }
}
