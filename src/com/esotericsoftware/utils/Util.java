package com.esotericsoftware.utils;

import java.io.*;

@SuppressWarnings({"UnusedDeclaration"})
public class Util
{
    public static String noEmpty( String pToTest )
    {
        if ( pToTest != null )
        {
            if ( (pToTest = pToTest.trim()).length() != 0 )
            {
                return pToTest;
            }
        }
        return null;
    }

    public static <T> T deNull( T pToTest, T pDefaultValue )
    {
        return (pToTest != null) ? pToTest : pDefaultValue;
    }

    public static <T> T assertNotNull( String pWhat, T pToTest )
    {
        if ( pToTest == null )
        {
            throw new IllegalArgumentException( pWhat + " cannot be null." );
        }
        return pToTest;
    }

    public static String assertNotEmpty( String pWhat, String pToTest )
    {
        if ( (pToTest = assertNotNull( pWhat, pToTest ).trim()).length() == 0 )
        {
            throw new IllegalArgumentException( pWhat + " cannot be empty/black." );
        }
        return pToTest;
    }

    public static void assertPairedEntries( String pWhat, Object[] pArray )
    {
        if ( (pArray != null) && ((pArray.length & 1) == 1) ) // Odd Length == Not Paired!
        {
            throw new IllegalArgumentException( pWhat + " had '" + pArray.length + "' entries, should have been either '" + (pArray.length - 1) + "' or '" + (pArray.length + 1) + "'" );
        }
    }

    public static File assertExists( String pWhat, File pToTest )
    {
        assertNotNull( pWhat, pToTest );
        if ( !pToTest.exists() )
        {
            throw new IllegalArgumentException( pWhat + " not found: " + pToTest.getAbsolutePath() );
        }
        return pToTest;
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
