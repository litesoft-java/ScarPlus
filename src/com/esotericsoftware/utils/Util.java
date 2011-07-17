package com.esotericsoftware.utils;

import org.litesoft.logger.*;

@SuppressWarnings({"UnusedDeclaration"})
public class Util
{
    public static LineSink PROGRESS_LINE_SINK = LineSink.SYSTEM_OUT;

    public static void progress( String pMessage )
    {
        PROGRESS_LINE_SINK.addLine( pMessage );
    }

    /**
     * True if running on a Mac OS.
     */
    public static final boolean isMac = System.getProperty( "os.name" ).toLowerCase().contains( "mac os x" );

    /**
     * True if running on a Windows OS.
     */
    public static final boolean isWindows = System.getProperty( "os.name" ).toLowerCase().contains( "windows" );

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final Logger LOGGER = LoggerFactory.getLogger( Util.class );

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
            throw new IllegalArgumentException( pWhat + " cannot be empty/blank." );
        }
        return pToTest;
    }

    public static void assertNotEmpty( String pWhat, String[] pToTest )
    {
        assertNotNull( pWhat, pToTest );
        if ( pToTest.length == 0 )
        {
            throw new IllegalArgumentException( pWhat + " cannot be empty." );
        }
    }

    public static int assertNotNegative( String pWhat, int pInt )
    {
        if ( pInt < 0 )
        {
            throw new IllegalArgumentException( pWhat + " was " + pInt + ", cannot be negative." );
        }
        return pInt;
    }

    public static void assertPairedEntries( String pWhat, Object[] pArray )
    {
        if ( (pArray != null) && ((pArray.length & 1) == 1) ) // Odd Length == Not Paired!
        {
            throw new IllegalArgumentException( pWhat + " had '" + pArray.length + "' entries, should have been either '" + (pArray.length - 1) + "' or '" + (pArray.length + 1) + "'" );
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

    public static String toString( Object o )
    {
        return (o != null) ? o.toString() : null;
    }
}
