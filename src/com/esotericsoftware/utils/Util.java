package com.esotericsoftware.utils;

import java.io.*;
import java.nio.channels.*;

@SuppressWarnings({"UnusedDeclaration"})
public class Util
{
    public static LineSink PROGRESS_LINE_SINK = LineSink.SYSTEM_OUT;

    public static void progress( String pMessage )
    {
        PROGRESS_LINE_SINK.addLine( pMessage );
    }

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

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

    /**
     * Copies one file to another.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void copyFile( File in, File out )
            throws IOException
    {
        out.getParentFile().mkdirs();
        FileChannel sourceChannel = new FileInputStream( in ).getChannel();
        FileChannel destinationChannel = new FileOutputStream( out ).getChannel();
        sourceChannel.transferTo( 0, sourceChannel.size(), destinationChannel );
        sourceChannel.close();
        destinationChannel.close();
    }

    /**
     * Deletes a directory and all files and directories it contains.
     */
    public static boolean delete( File pFile )
    {
        if ( pFile.isDirectory() )
        {
            File[] zFiles = pFile.listFiles();
            for ( File zFile : zFiles )
            {
                if ( !delete( zFile ) )
                {
                    return false;
                }
            }
        }
        return pFile.delete();
    }
}
