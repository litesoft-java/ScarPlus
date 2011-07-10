package com.esotericsoftware.utils;

import java.io.*;

import org.junit.*;

import static org.junit.Assert.*;

public class FileSupportTest
{
    private static class TestFileSystem implements IFileSystem
    {
        private boolean mWindows;
        private String[] mCanonicalExistingPaths;

        public TestFileSystem( boolean pWindows, String... pCanonicalExistingPaths )
        {
            mWindows = pWindows;
            mCanonicalExistingPaths = pCanonicalExistingPaths;
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
            for ( String zPath : mCanonicalExistingPaths )
            {
                if ( zPath.equalsIgnoreCase( pPath ) )
                {
                    return zPath;
                }
            }
            return null;
        }
    }

    @Test
    public void test_normalizePath()
            throws IOException
    {
        IFileSystem zFileSystem = new TestFileSystem( true );

        assertEquals( "\\\\TheServer\\Fred", FileSupport.normalizePath( zFileSystem, "\\\\TheServer\\Fred" ));
        assertEquals( "\\\\TheServer\\Fred", FileSupport.normalizePath( zFileSystem, "\\\\TheServer/Fred" ) );

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
        IFileSystem zFileSystem = new TestFileSystem( true, //
                                                      "C:\\Fred", //
                                                      "C:\\Flintstones\\Fred", //
                                                      "\\\\TheServer\\Fred" );
        assertAbsolute( zFileSystem, "\\\\TheServer\\Fred" );

        assertRelative( zFileSystem, "Fred" );
        assertRelative( zFileSystem, "..\\Fred" );

        assertRelative( zFileSystem, "C:." ); // Just Our Drive Letter is equivalent to "."

        assertAbsolute( zFileSystem, "R:\\Fred" );

        assertAbsolute( zFileSystem, "R:." ); // Just the Other Drive Letter is equivalent to "."

        assertAbsolute( zFileSystem, "R:Fred" );
        assertAbsolute( zFileSystem, "R:..\\Fred" );

        assertAbsolute( zFileSystem, "\\Fred" );

        zFileSystem = new TestFileSystem( false, //
                                          "/Fred", //
                                          "/Flintstones/Fred", //
                                          "/TheServer/Fred" );

        assertRelative( zFileSystem, "Fred" );
        assertRelative( zFileSystem, "../Fred" );

        assertAbsolute( zFileSystem, "/Fred" );
    }

    private void assertAbsolute( IFileSystem pFileSystem, String pPath )
    {
        assertTrue( "!Absolute?: " + pPath, FileSupport.isAbsoluteNormalizedPath( pFileSystem, "C:", pPath ) );
    }

    private void assertRelative( IFileSystem pFileSystem, String pPath )
    {
        assertFalse( "!Relative?: " + pPath, FileSupport.isAbsoluteNormalizedPath( pFileSystem, "C:", pPath ) );
    }
}
