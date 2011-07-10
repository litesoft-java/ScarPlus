package com.esotericsoftware.utils;

import com.esotericsoftware.scar.*;

public class FileSupport
{
    public static String normalizePath( IFileSystem pFileSystem, String path )
    {
        path = path.trim();
        String zPrefix = "";
        if ( pFileSystem.isWindows() )
        {
            if ( path.startsWith( "\\\\" ) )
            {
                zPrefix = "\\\\";
                path = path.substring( 2 ).trim();
            }
            else if ( (path.length() > 1) && (path.charAt( 1 ) == ':') ) // Handle Drive Letter
            {
                zPrefix = path.substring( 0, 2 ).toUpperCase();
                path = path.substring( 2 ).trim();
            }
        }
        path = path.trim();
        if ( '/' != pFileSystem.separatorChar() )
        {
            path = path.replace( '/', pFileSystem.separatorChar() );
        }
        int at = path.indexOf( pFileSystem.separatorChar() );
        if ( at != -1 )
        {
            String zFileSeparator = "" + pFileSystem.separatorChar();

            // remove white space around file Parts
            StringBuilder sb = new StringBuilder( path.length() );
            int from = 0;
            do
            {
                sb.append( path.substring( from, at ).trim() ).append( pFileSystem.separatorChar() );
                from = at + 1;
            }
            while ( -1 != (at = path.indexOf( pFileSystem.separatorChar(), from )) );
            path = sb.append( path.substring( from ).trim() ).toString();

            // Clean up silly middle nothings
            path = Utils.replace( path, zFileSeparator + "." + zFileSeparator, zFileSeparator ); // "/./"
            path = Utils.replace( path, zFileSeparator + zFileSeparator, zFileSeparator ); // "//"

            // Remove ending "/."
            while ( path.endsWith( zFileSeparator + "." ) )
            {
                path = path.substring( 0, path.length() - 2 );
            }
            // Remove leading "./"
            while ( path.startsWith( "." + zFileSeparator ) )
            {
                path = path.substring( 2 );
            }

            // Process Funky ..
            String zUpLevel = zFileSeparator + "..";
            if ( path.endsWith( zUpLevel ) )
            {
                path += zFileSeparator;
            }
            zUpLevel += zFileSeparator;
            for ( at = path.lastIndexOf( zUpLevel ); at > 0; at = path.lastIndexOf( zUpLevel ) )
            {
                path = removeDotDot( path, at, pFileSystem.separatorChar() );
            }
            if ( (path.length() > 1) && path.endsWith( zFileSeparator ) )
            {
                path = path.substring( 0, path.length() - 1 );
            }
        }
        if ( path.length() == 0 )
        {
            path = ".";
        }
        path = zPrefix + path;
        return path;
    }

    private static String removeDotDot( String path, int pAt, char pSeparatorChar )
    {
        int zEnd = pAt + 4;
        while ( path.charAt( --pAt ) != pSeparatorChar )
        {
            if ( pAt == 0 )
            {
                return path.substring( zEnd );
            }
        }
        return path.substring( 0, pAt + 1 ) + path.substring( zEnd );
    }

    public static boolean isAbsoluteNormalizedPath( IFileSystem pFileSystem, String pCanonicalDirForWindowDriveLetterSourceRelativeness, String path )
    {
        if ( hasWindowsDriveLetter( pFileSystem, path ) ) // Handle Drive Letter
        {
            if ( !pCanonicalDirForWindowDriveLetterSourceRelativeness.substring( 0, 2 ).equalsIgnoreCase( path.substring( 0, 2 ) ) )
            {
                return true; // Has Drive Letter and it is NOT the same as the 'CanonicalDirForWindowDriveLetterSourceRelativeness'
            }
            path = path.substring( 2 );
        }
        // todo: ...

        return false;
    }

    public static String canonicalizeNormalizedPath( IFileSystem pFileSystem, String pCanonicalParentDirIfPathRelative, String path )
    {
        if ( hasWindowsDriveLetter( pFileSystem, path ) ) // Handle Drive Letter
        {
            if ( pCanonicalParentDirIfPathRelative.substring( 0, 2 ).equalsIgnoreCase( path.substring( 0, 2 ) ) )
            {
                path = path.substring( 2 );
            }
        }
        if ( !isAbsoluteNormalizedPath( pFileSystem, pCanonicalParentDirIfPathRelative, path ) )
        {
            path = normalizePath( pFileSystem, pCanonicalParentDirIfPathRelative + pFileSystem.separatorChar() + path );
        }
        // canonicalize
        return null; // todo...
    }

    private static boolean hasWindowsDriveLetter( IFileSystem pFileSystem, String path )
    {
        return pFileSystem.isWindows() && (path.length() > 1) && (path.charAt( 1 ) == ':');
    }
}
