package com.esotericsoftware.utils;

import java.io.*;

import com.esotericsoftware.scar.*;

public class FileSupport
{
    public static final String WINDOWS_UNC_PATH_PREFIX = "\\\\";

    public static String getWindowsDriveIndicator( String path )
    {
        return ((path.length() > 1) && (path.charAt( 1 ) == ':')) ? path.substring( 0, 2 ).toUpperCase() : "";
    }

    public static String removeFromFront( String pToRemove, String path )
    {
        return (pToRemove.length() == 0) ? path : path.substring( pToRemove.length() );
    }

    public static String normalizePath( IFileSystem pFileSystem, String path )
    {
        path = path.trim();
        String zPrefix = "";
        if ( pFileSystem.isWindows() )
        {
            if ( path.startsWith( WINDOWS_UNC_PATH_PREFIX ) )
            {
                path = removeFromFront( zPrefix = WINDOWS_UNC_PATH_PREFIX, path ).trim();
            }
            else
            {
                path = removeFromFront( zPrefix = getWindowsDriveIndicator( path ), path ).trim(); // Handle Drive Letter
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
            String zPrefixDotDotSlash = "";
            String zDotDotSlash = ".." + zFileSeparator;
            while ( path.startsWith( zDotDotSlash ) )
            {
                zPrefixDotDotSlash += zDotDotSlash;
                path = path.substring( 3 );
            }
            String zUpLevel = zFileSeparator + "..";
            if ( path.endsWith( zUpLevel ) )
            {
                path += zFileSeparator;
            }
            zUpLevel += zFileSeparator;
            for ( at = path.indexOf( zUpLevel ); at > 0; at = path.indexOf( zUpLevel ) )
            {
                path = removeDotDot( path, at, pFileSystem.separatorChar() );
            }
            path = zPrefixDotDotSlash + path;
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

    public static boolean isAbsoluteNormalizedPath( IFileSystem pFileSystem, String pCanonicalParentDirIfPathRelativeForWindowsDriveLetter, String path )
    {
        if ( pFileSystem.isWindows() )
        {
            if ( path.startsWith( WINDOWS_UNC_PATH_PREFIX ) )
            {
                return true;
            }
            String zDriveIndicator = getWindowsDriveIndicator( path );
            if ( zDriveIndicator.length() != 0 ) // Handle Drive Letter
            {
                if ( !pCanonicalParentDirIfPathRelativeForWindowsDriveLetter.startsWith( zDriveIndicator ) || !pFileSystem.canonicalCurrentPath().startsWith( zDriveIndicator ) )
                {
                    return true; // Has Drive Letter and it is NOT the same both the 'CanonicalDirForWindowDriveLetterSourceRelativeness' && pFileSystem.canonicalCurrentPath()
                }
                path = removeFromFront( zDriveIndicator, path );
            }
        }
        return (path.length() > 0) && (path.charAt( 0 ) == pFileSystem.separatorChar());
    }

    public static String canonicalizeNormalizedPath( IFileSystem pFileSystem, String pCanonicalParentDirIfPathRelative, String path )
            throws IOException
    {
        if ( !pFileSystem.isWindows() )
        {
            if ( !isAbsoluteNormalizedPath( pFileSystem, pCanonicalParentDirIfPathRelative, path ) )
            {
                path = normalizePath( pFileSystem, pCanonicalParentDirIfPathRelative + pFileSystem.separatorChar() + path );
            }
            return canonicalizeAbsoluteNormalizedPath( pFileSystem, path );
        }
        // Windows!
        if ( path.startsWith( WINDOWS_UNC_PATH_PREFIX ) )
        {
            return canonicalizeAbsoluteNormalizedPath( pFileSystem, path );
        }
        String zDriveIndicator = getWindowsDriveIndicator( path );
        if ( !isAbsoluteNormalizedPath( pFileSystem, pCanonicalParentDirIfPathRelative, path ) ) // Relative!
        {
            path = normalizePath( pFileSystem, pCanonicalParentDirIfPathRelative + pFileSystem.separatorChar() + removeFromFront( zDriveIndicator, path ) );
            return canonicalizeAbsoluteNormalizedPath( pFileSystem, path );
        }
        // Absolute
        if ( zDriveIndicator.length() == 0 ) // to "default" Drive
        {
            return canonicalizeAbsoluteNormalizedPath( pFileSystem, path );
        }
        // Windows path w/ DriveIndicator which 'might' actually be relative to the given DriveIndicator
        if ( (path = removeFromFront( zDriveIndicator, path )).length() == 0 ) // Should NOT be possible!
        {
            path = ".";
        }
        if ( (path.charAt( 0 ) != pFileSystem.separatorChar()) && !path.startsWith( "." ) )
        {
            path = "." + pFileSystem.separatorChar() + path;
        }
        return canonicalizeAbsoluteNormalizedPath( pFileSystem, zDriveIndicator + path );
    }

    private static String canonicalizeAbsoluteNormalizedPath( IFileSystem pFileSystem, String path )
            throws IOException
    {
        String origPath = path;
        if ( !pFileSystem.exists( origPath ) )
        {
            String zEnd = "";
            for ( int at; -1 != (at = path.lastIndexOf( pFileSystem.separatorChar() )); )
            {
                zEnd = path.substring( at ) + zEnd;
                if ( pFileSystem.exists( path = path.substring( 0, at ) ) )
                {
                    return pFileSystem.canonicalizeNormalizedExisting( path ) + zEnd;
                }
            }
        }
        return pFileSystem.canonicalizeNormalizedExisting( origPath );
    }
}
