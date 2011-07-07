package com.esotericsoftware.wildcard;

import java.io.*;

import org.junit.*;

import static org.junit.Assert.*;

public class FileTest
{
    @Test
    public void test_isAbsolute()
    {
        if ( isWindows() )
        {
            assertNotAbsolute( "./Fred" );
            assertNotAbsolute( "../Fred" );
            assertNotAbsolute( ".\\Fred" );
            assertNotAbsolute( "..\\Fred" );

            assertIsAbsolute( "r:\\Fred" );
            assertIsAbsolute( "r:/Fred" );

            assertNotAbsolute( "r:.\\Fred" );
            assertNotAbsolute( "r:..\\Fred" );
            assertNotAbsolute( "r:./Fred" );
            assertNotAbsolute( "r:../Fred" );

            assertIsAbsolute( "\\\\TheServer\\Fred" );

            assertIsAbsolute( "\\\\TheServer/Fred" );

            assertNotAbsolute( "\\Fred" );  // What the F!!!
            assertNotAbsolute( "/Fred" );
        }
    }

    private void assertIsAbsolute( String pPath )
    {
        assertTrue( "!Absolute?: " + pPath, new File( pPath ).isAbsolute() );
    }

    private void assertNotAbsolute( String pPath )
    {
        assertFalse( "Absolute?: " + pPath, new File( pPath ).isAbsolute() );
    }

    private boolean isWindows()
    {
        String osName = System.getProperty( "os.name" );
        String fileSeparator = System.getProperty( "file.separator" );
        return "\\".equals( fileSeparator ) && osName.contains( "Windows" );
    }
}
