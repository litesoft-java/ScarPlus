package com.esotericsoftware.utils;

import java.io.*;

import org.junit.*;

import static org.junit.Assert.*;

public class FileTest
{
    @Test
    public void test_FileStuff()
            throws IOException
    {
        if ( Util.isWindows )
        {
            assertAbsolute( "\\\\TheServer\\Fred" );
            assertAbsolute( "\\\\TheServer/Fred" );

            assertRelative( "./Fred" );
            assertRelative( "../Fred" );
            assertRelative( ".\\Fred" );
            assertRelative( "..\\Fred" );

            String zPath = Util.CANONICAL_USER_DIR.getPath();
            System.out.println( "Current User Dir Path: " + zPath );

            assertEquals( zPath, new File( zPath + '\\' ).getCanonicalPath() );

            assertTrue( zPath, (zPath.length() > 1) && (zPath.charAt( 1 ) == ':') );

            char zDriveLetter = zPath.charAt( 0 );

            assertTrue( zPath, ('A' <= zDriveLetter) && (zDriveLetter <= 'Z') );

            String zDrivePath = zPath.substring( 0, 2 );

            assertEquals( zPath, new File( zDrivePath ).getCanonicalPath() ); // Just Our Drive Letter is equivalent to "."

            assertRelative( zDrivePath ); // Just Our Drive Letter is equivalent to "."

            assertTrue( new File( zDrivePath ).exists() );

            String zOtherDrivePath = "" + findNonExistingDriveLetter() + ':';
            System.out.println( "The Other Drive Path is " + zOtherDrivePath );

            assertAbsolute( zOtherDrivePath + "\\Fred" );
            assertAbsolute( zOtherDrivePath + "/Fred" );

            // *** All of the following are: *** What the F!!! ***

            assertRelative( zOtherDrivePath ); // Just the Other Drive Letter is equivalent to "."

            assertRelative( zOtherDrivePath + ".\\Fred" );
            assertRelative( zOtherDrivePath + "..\\Fred" );
            assertRelative( zOtherDrivePath + "./Fred" );
            assertRelative( zOtherDrivePath + "../Fred" );

            assertRelative( "\\Fred" );
            assertRelative( "/Fred" );
        }
    }

    private void assertAbsolute( String pPath )
    {
        assertTrue( "!Absolute?: " + pPath, new File( pPath ).isAbsolute() );
    }

    private void assertRelative( String pPath )
    {
        assertFalse( "!Relative?: " + pPath, new File( pPath ).isAbsolute() );
    }

    private char findNonExistingDriveLetter()
    {
        for ( int i = 'Z'; 'A' <= i; i-- )
        {
            if ( !new File( "" + ((char) i) + ':' ).exists() )
            {
                return (char) i;
            }
        }
        throw new IllegalStateException( "All Drive Letters Exist!" );
    }
}
