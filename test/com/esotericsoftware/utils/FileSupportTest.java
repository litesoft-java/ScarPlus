package com.esotericsoftware.utils;

import java.io.*;
import java.util.*;

import org.junit.*;

import static org.junit.Assert.*;

public class FileSupportTest
{
    private static class TestFileSystem implements IFileSystem
    {
        private boolean mWindows;
        private String[] mCanonicalExistingPaths;
        private String mCurrentPath;
        private Map<String, String> mCurrentDrivePaths = new HashMap<String, String>();

        public TestFileSystem( boolean pWindows, String... pCanonicalExistingPaths )
        {
            mWindows = pWindows;
            mCanonicalExistingPaths = pCanonicalExistingPaths;
        }

        public TestFileSystem setCurrentPath( String pCurrentPath )
        {
            mCurrentPath = pCurrentPath;
            return isWindows() ? addDriveAt( FileSupport.getWindowsDriveIndicator( mCurrentPath ), mCurrentPath.substring( 2 ) ) : this;
        }

        public TestFileSystem addDriveAt( String pDriveIndicator, String pCurrentCanonicalPath )
        {
            mCurrentDrivePaths.put( pDriveIndicator, pCurrentCanonicalPath );
            return this;
        }

        @Override
        public boolean isWindows()
        {
            return mWindows;
        }

        @Override
        public char separatorChar()
        {
            return mWindows ? '\\' : '/';
        }

        @Override
        public String canonicalCurrentPath()
        {
            return mCurrentPath;
        }

        @Override
        public boolean exists( String path )
        {
            return null != findCanonicalExistingPath( path );
        }

        @Override
        public String canonicalizeNormalizedExisting( String path )
                throws IOException
        {
            String zPath = findCanonicalExistingPath( path );
            if ( zPath != null )
            {
                return zPath;
            }
            throw new FileNotFoundException( path );
        }

        private String findCanonicalExistingPath( String pPath )
        {
            if ( pPath.equals( ".." ) )
            {
                pPath += "" + separatorChar();
            }
            if ( !isWindows() )
            {
                if ( pPath.startsWith( "../" ) )
                {
                    String zFront = mCurrentPath;
                    do
                    {
                        zFront = removeLastDirReference( zFront );
                        pPath = pPath.substring( 3 );
                    }
                    while ( pPath.startsWith( "..\\" ) );
                    pPath = (pPath.length() == 0) ? zFront : zFront + "/" + pPath;
                }
            }
            else
            {
                if ( !pPath.startsWith( FileSupport.WINDOWS_UNC_PATH_PREFIX ) )
                {
                    String zDriveIndicator = FileSupport.getWindowsDriveIndicator( pPath );
                    pPath = FileSupport.removeFromFront( zDriveIndicator, pPath );
                    if ( zDriveIndicator.length() == 0 )
                    {
                        zDriveIndicator = FileSupport.getWindowsDriveIndicator( mCurrentPath );
                    }
                    if ( pPath.equals( "." ) )
                    {
                        pPath = mCurrentDrivePaths.get( zDriveIndicator );
                    }
                    else if ( pPath.startsWith( ".\\" ) )
                    {
                        pPath = mCurrentDrivePaths.get( zDriveIndicator ) + pPath.substring( 1 );
                    }
                    else if ( pPath.startsWith( "..\\" ) )
                    {
                        String zFront = mCurrentDrivePaths.get( zDriveIndicator );
                        do
                        {
                            zFront = removeLastDirReference( zFront );
                            pPath = pPath.substring( 3 );
                        }
                        while ( pPath.startsWith( "..\\" ) );
                        pPath = (pPath.length() == 0) ? zFront : zFront + "\\" + pPath;
                    }
                    pPath = zDriveIndicator + pPath;
                }
            }
            if ( isWindows() )
            {
                for ( String zPath : mCanonicalExistingPaths )
                {
                    if ( zPath.equalsIgnoreCase( pPath ) )
                    {
                        return zPath;
                    }
                }
            }
            else
            {
                for ( String zPath : mCanonicalExistingPaths )
                {
                    if ( zPath.equals( pPath ) )
                    {
                        return zPath;
                    }
                }
            }
            return null;
        }

        private String removeLastDirReference( String pPath )
        {
            int at = pPath.lastIndexOf( separatorChar() );
            return (at == -1) ? pPath : pPath.substring( 0, at );
        }
    }

    @Test
    public void test_normalizePath()
            throws IOException
    {
        IFileSystem zFileSystem = new TestFileSystem( true );

        assertEquals( "\\\\TheServer\\Fred", FileSupport.normalizePath( zFileSystem, "\\\\TheServer\\Fred" ) );
        assertEquals( "\\\\TheServer\\Fred", FileSupport.normalizePath( zFileSystem, "\\\\TheServer/Fred" ) );

        assertEquals( ".", FileSupport.normalizePath( zFileSystem, "." ) );
        assertEquals( "..", FileSupport.normalizePath( zFileSystem, ".." ) );

        assertEquals( "Fred", FileSupport.normalizePath( zFileSystem, "./Fred" ) );

        assertEquals( "Fred", FileSupport.normalizePath( zFileSystem, ".//.////./Fred////./." ) );

        assertEquals( "Fred", FileSupport.normalizePath( zFileSystem, ".\\Fred" ) );

        assertEquals( "..\\Fred", FileSupport.normalizePath( zFileSystem, "../Fred" ) );
        assertEquals( "..\\Fred", FileSupport.normalizePath( zFileSystem, "..\\Fred" ) );

        assertEquals( "C:.", FileSupport.normalizePath( zFileSystem, "C:" ) ); // Just Our Drive Letter is equivalent to "."

        assertEquals( "R:\\Fred", FileSupport.normalizePath( zFileSystem, "R:\\Fred" ) );
        assertEquals( "R:\\Fred", FileSupport.normalizePath( zFileSystem, "R:/Fred" ) );

        assertEquals( "R:.", FileSupport.normalizePath( zFileSystem, "R:" ) ); // Just the Other Drive Letter is equivalent to "."

        assertEquals( "R:Fred", FileSupport.normalizePath( zFileSystem, "R:.\\Fred" ) );
        assertEquals( "R:Fred", FileSupport.normalizePath( zFileSystem, "R:./Fred" ) );

        assertEquals( "R:..\\Fred", FileSupport.normalizePath( zFileSystem, "R:..\\Fred" ) );
        assertEquals( "R:..\\Fred", FileSupport.normalizePath( zFileSystem, "R:../Fred" ) );

        assertEquals( "\\Fred", FileSupport.normalizePath( zFileSystem, "\\Fred" ) );
        assertEquals( "\\Fred", FileSupport.normalizePath( zFileSystem, "/Fred" ) );

        // Now all the funky up Levels...
        assertEquals( ".", FileSupport.normalizePath( zFileSystem, "a\\.." ) );
        assertEquals( ".", FileSupport.normalizePath( zFileSystem, "a/.." ) );

        assertEquals( "\\", FileSupport.normalizePath( zFileSystem, "\\Fred\\.." ) );
        assertEquals( "\\", FileSupport.normalizePath( zFileSystem, "/Fred/.." ) );

        assertEquals( "Fred", FileSupport.normalizePath( zFileSystem, "Fred\\Wilma\\.." ) );
        assertEquals( "Fred", FileSupport.normalizePath( zFileSystem, "Fred/Wilma/.." ) );

        assertEquals( "\\Fred", FileSupport.normalizePath( zFileSystem, "\\Fred\\Wilma\\.." ) );
        assertEquals( "\\Fred", FileSupport.normalizePath( zFileSystem, "/Fred/Wilma/.." ) );

        assertEquals( "Wilma", FileSupport.normalizePath( zFileSystem, "Fred\\..\\Wilma" ) );
        assertEquals( "Wilma", FileSupport.normalizePath( zFileSystem, "Fred/../Wilma" ) );

        assertEquals( "\\Wilma", FileSupport.normalizePath( zFileSystem, "\\Fred\\..\\Wilma" ) );
        assertEquals( "\\Wilma", FileSupport.normalizePath( zFileSystem, "/Fred/../Wilma" ) );

        assertEquals( "\\..\\Wilma", FileSupport.normalizePath( zFileSystem, "\\..\\Wilma" ) );
        assertEquals( "\\..\\Wilma", FileSupport.normalizePath( zFileSystem, "/../Wilma" ) );

        zFileSystem = new TestFileSystem( false );

        assertEquals( ".", FileSupport.normalizePath( zFileSystem, "." ) );
        assertEquals( "..", FileSupport.normalizePath( zFileSystem, ".." ) );

        assertEquals( "Fred", FileSupport.normalizePath( zFileSystem, "./Fred" ) );

        assertEquals( "Fred", FileSupport.normalizePath( zFileSystem, ".//.////./Fred////./." ) );

        assertEquals( "../Fred", FileSupport.normalizePath( zFileSystem, "../Fred" ) );

        assertEquals( "/Fred", FileSupport.normalizePath( zFileSystem, "/Fred" ) );

        // Now all the funky up Levels...
        assertEquals( ".", FileSupport.normalizePath( zFileSystem, "a/.." ) );

        assertEquals( "/", FileSupport.normalizePath( zFileSystem, "/Fred/.." ) );

        assertEquals( "Fred", FileSupport.normalizePath( zFileSystem, "Fred/Wilma/.." ) );

        assertEquals( "/Fred", FileSupport.normalizePath( zFileSystem, "/Fred/Wilma/.." ) );

        assertEquals( "Wilma", FileSupport.normalizePath( zFileSystem, "Fred/../Wilma" ) );

        assertEquals( "/Wilma", FileSupport.normalizePath( zFileSystem, "/Fred/../Wilma" ) );

        assertEquals( "/../Wilma", FileSupport.normalizePath( zFileSystem, "/../Wilma" ) );
    }

    @Test
    public void test_isAbsoluteNormalizedPath()
            throws IOException
    {
        IFileSystem zFileSystem = new TestFileSystem( true ).setCurrentPath( "C:\\" );
        assertAbsolute( zFileSystem, "C:\\Flintstone", "\\\\TheServer\\Fred" );

        assertRelative( zFileSystem, "C:\\Flintstone", "." );
        assertRelative( zFileSystem, "C:\\Flintstone", "Fred" );
        assertRelative( zFileSystem, "C:\\Flintstone", "..\\Fred" );

        assertRelative( zFileSystem, "C:\\Flintstone", "C:." ); // Just Our Drive Letter is equivalent to "."

        assertAbsolute( zFileSystem, "D:\\Flintstone", "C:." ); // Just Our Drive Letter is equivalent to "."

        assertAbsolute( zFileSystem, "C:\\Flintstone", "R:\\Fred" );

        assertAbsolute( zFileSystem, "C:\\Flintstone", "R:." ); // Just the Other Drive Letter is equivalent to "."

        assertAbsolute( zFileSystem, "C:\\Flintstone", "R:Fred" );
        assertAbsolute( zFileSystem, "C:\\Flintstone", "R:..\\Fred" );

        assertAbsolute( zFileSystem, "C:\\Flintstone", "\\Fred" );

        zFileSystem = new TestFileSystem( false ).setCurrentPath( "/" );

        assertRelative( zFileSystem, "/Flintstone", "Fred" );
        assertRelative( zFileSystem, "/Flintstone", "../Fred" );

        assertAbsolute( zFileSystem, "/Flintstone", "/Fred" );
    }

    private void assertAbsolute( IFileSystem pFileSystem, String pCanonicalParentDirIfPathRelative, String pPath )
    {
        assertTrue( "!Absolute?: " + pPath, FileSupport.isAbsoluteNormalizedPath( pFileSystem, pCanonicalParentDirIfPathRelative, pPath ) );
    }

    private void assertRelative( IFileSystem pFileSystem, String pCanonicalParentDirIfPathRelative, String pPath )
    {
        assertFalse( "!Relative?: " + pPath, FileSupport.isAbsoluteNormalizedPath( pFileSystem, pCanonicalParentDirIfPathRelative, pPath ) );
    }

    @Test
    public void test_canonicalizeNormalizedPath()
            throws IOException
    {
        IFileSystem zFileSystem = new TestFileSystem( false, //
                                                      "/", //
                                                      "/Fred", //
                                                      "/Flintstone", //
                                                      "/Flintstone/Fred", //
                                                      "/Flintstone/Wilma", //
                                                      "/TheServer/Fred" ).setCurrentPath( "/Flintstone" );

        assertEquals( "/", FileSupport.canonicalizeNormalizedPath( zFileSystem, "/Flintstone", ".." ) );
        assertEquals( "/Flintstone", FileSupport.canonicalizeNormalizedPath( zFileSystem, "/Flintstone", "." ) );
        assertEquals( "/Flintstone/Fred", FileSupport.canonicalizeNormalizedPath( zFileSystem, "/Flintstone", "Fred" ) );
        assertEquals( "/Flintstone/Fred", FileSupport.canonicalizeNormalizedPath( zFileSystem, "/Flintstone/Wilma", "../Fred" ) );

        assertEquals( "/Fred", FileSupport.canonicalizeNormalizedPath( zFileSystem, "/Flintstone/Wilma", "/Fred" ) );

        assertEquals( "/Flintstone/Wilma/Pebbles", FileSupport.canonicalizeNormalizedPath( zFileSystem, "/Flintstone", "Wilma/Pebbles" ) );

        assertEquals( "/TheServer/Fred", FileSupport.canonicalizeNormalizedPath( zFileSystem, "/Flintstone/Wilma", "/TheServer/Fred" ) );

        zFileSystem = new TestFileSystem( true, //
                                          "C:\\", //
                                          "C:\\Fred", //
                                          "C:\\Flintstone", //
                                          "C:\\Flintstone\\Fred", //
                                          "C:\\Flintstone\\Wilma", //
                                          "R:\\", //
                                          "R:\\Barney", //
                                          "R:\\Rubble", //
                                          "R:\\Rubble\\Barney", //
                                          "R:\\Rubble\\Betty", //
                                          "\\\\TheServer\\Fred" ).setCurrentPath( "C:\\Flintstone" ).addDriveAt( "R:", "\\Rubble" );

        assertEquals( "\\\\TheServer\\Fred", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone\\Wilma", "\\\\TheServer\\Fred" ) );

        assertEquals( "C:\\", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone", ".." ) );
        assertEquals( "C:\\Flintstone", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone", "." ) );
        assertEquals( "C:\\Flintstone\\Fred", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone", "Fred" ) );
        assertEquals( "C:\\Flintstone\\Fred", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone\\Wilma", "..\\Fred" ) );

        assertEquals( "C:\\Flintstone\\Fred", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone\\Fred", "C:." ) );

        assertEquals( "C:\\Fred", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone\\Wilma", "\\Fred" ) );

        assertEquals( "C:\\Flintstone\\Wilma\\Pebbles", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone", "wilma\\Pebbles" ) );

        assertEquals( "R:\\Barney", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone\\Wilma", "R:\\Barney" ) );

        assertEquals( "R:\\Rubble", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone\\Wilma", "R:." ) );

        assertEquals( "R:\\Rubble\\Barney", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone\\Wilma", "R:Barney" ) );
        assertEquals( "R:\\Barney", FileSupport.canonicalizeNormalizedPath( zFileSystem, "C:\\Flintstone\\Wilma", "R:..\\Barney" ) );
    }
}
