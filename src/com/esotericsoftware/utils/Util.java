package com.esotericsoftware.utils;

import java.io.*;
import java.nio.channels.*;

import org.litesoft.logger.*;

@SuppressWarnings({"UnusedDeclaration"})
public class Util
{
    public static LineSink PROGRESS_LINE_SINK = LineSink.SYSTEM_OUT;

    public static void progress( String pMessage )
    {
        PROGRESS_LINE_SINK.addLine( pMessage );
    }

    public static final File CANONICAL_USER_DIR;

    static
    {
        try
        {
            CANONICAL_USER_DIR = new File( System.getProperty( "user.dir" ) ).getCanonicalFile();
        }
        catch ( IOException e )
        {
            throw new Error( e );
        }
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

    private static final IFileSystem FILE_SYSTEM = new IFileSystem()
    {
        @Override
        public char separatorChar()
        {
            return File.separatorChar;
        }

        @Override
        public boolean isWindows()
        {
            return isWindows;
        }

        @Override
        public boolean exists( String path )
        {
            return new File( path ).exists();
        }

        @Override
        public String canonicalizeNormalizedExisting( String path )
                throws IOException
        {
            File zFile = new File( path );
            if ( zFile.exists() )
            {
                return zFile.getCanonicalPath();
            }
            throw new FileNotFoundException( path );
        }
    };

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
        assertNotNull( "File", pFile );
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
        LOGGER.trace.log( "Deleting file: ", pFile );
        return pFile.delete();
    }

    /**
     * Deletes a file or directory and all files and subdirecties under it.
     */
    public static boolean delete( String fileName )
    {
        return delete( new File( assertNotEmpty( "fileName", fileName ) ) );
    }

    public static Closeable dispose( Closeable pCloseable )
    {
        if ( pCloseable != null )
        {
            try
            {
                pCloseable.close();
            }
            catch ( IOException ignore )
            {
                // Whatever!
            }
            pCloseable = null;
        }
        return pCloseable;
    }

    public static boolean isAbsolutePath( String path )
    {
        assertNotNull( "path", path );
        return FileSupport.isAbsoluteNormalizedPath( FILE_SYSTEM, CANONICAL_USER_DIR.getPath(), FileSupport.normalizePath( FILE_SYSTEM, path ) );
    }

    public static String normalizePath( String path )
    {
        assertNotNull( "path", path );
        return FileSupport.normalizePath( FILE_SYSTEM, path );
    }

    public static File canonicalizePath( String path )
    {
        return canonicalizePath( CANONICAL_USER_DIR, path );
    }

    public static File canonicalizePath( File pCanonicalParentDirIfPathRelative, String path )
    {
        assertNotNull( "path", path );
        return new File( FileSupport.canonicalizeNormalizedPath( FILE_SYSTEM, pCanonicalParentDirIfPathRelative.getPath(), FileSupport.normalizePath( FILE_SYSTEM, path ) ) );
    }
}
