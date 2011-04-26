package com.esotericsoftware.wildcard;

import com.esotericsoftware.wildcard.support.*;

public class Pattern
{
    private final String mPattern;
    private final DirMatcher mDirMatcher;
    private final FileNameMatcher mFileNameMatcher;

    public Pattern( String pattern )
    {
        mPattern = clean( pattern.replace( '\\', '/' ).trim() );
        int lastSep = mPattern.lastIndexOf( '/' );
        if ( lastSep == -1 )
        {
            mDirMatcher = CurrentDirMatcher.INSTANCE;
            mFileNameMatcher = createFileNameMatcher( mPattern );
        }
        else
        {
            mDirMatcher = createDirMatcher( mPattern.substring( 0, lastSep ).trim() );
            mFileNameMatcher = createFileNameMatcher( mPattern.substring( lastSep + 1 ).trim() );
        }
    }

    public static String clean( String pattern )
    {
        if ( pattern.length() == 0 )
        {
            pattern = "*";
        }
        pattern = replace( pattern, " /", "/" );
        pattern = replace( pattern, "/ ", "/" );
        pattern = replace( pattern, "/./", "//" );
        pattern = replace( pattern, "//", "/" );

        pattern = pattern.startsWith( "/" ) ? "." + pattern : "./" + pattern;
        if ( pattern.endsWith( "**" ) )
        {
            pattern += "/";
        }
        if ( pattern.endsWith( "/" ) )
        {
            pattern += "*";
        }
        String newPattern = processNonSlashedStarStar( pattern );
        newPattern = replace( newPattern, "/**/**/", "/**/" );
        if ( !newPattern.equals( pattern ) )
        {
            System.out.println( "Pattern '" + pattern + "' -> '" + newPattern + "'" );
        }
        return newPattern.substring( 2 );
    }

    private static String processNonSlashedStarStar( String pPattern )
    {
        int from = 0;
        for ( int at; -1 != (at = pPattern.indexOf( "**", from )); from = at + 1 )
        {
            if ( pPattern.charAt( at + 2 ) != '/' )
            {
                pPattern = pPattern.substring( 0, at + 2 ) + "/*" + pPattern.substring( at + 2 );
            }
            if ( pPattern.charAt( at - 1 ) != '/' )
            {
                pPattern = pPattern.substring( 0, at ) + "*/" + pPattern.substring( at );
                at += 2;
            }
        }
        return pPattern;
    }

    private static String replace( String pPattern, String pOfInterest, String pReplaceWith )
    {
        for ( int at; -1 != (at = pPattern.indexOf( pOfInterest )); )
        {
            pPattern = pPattern.substring( 0, at ) + pReplaceWith + pPattern.substring( at + pOfInterest.length() );
        }
        return pPattern;
    }

    private DirMatcher createDirMatcher( String pDirPattern )
    {
        if ( "".equals( pDirPattern ) || ".".equals( pDirPattern ) )
        {
            return CurrentDirMatcher.INSTANCE;
        }
        if ( "**".equals( pDirPattern ) )
        {
            return AllDirMatcher.INSTANCE;
        }
        if ( !pDirPattern.contains( "*" ) && !pDirPattern.contains( "?" ) )
        {
            return new ExactDirMatcher( pDirPattern );
        }
        String[] zDirParts = FilePathPartMatcher.SLASH.split( pDirPattern, 0 );
        return (zDirParts.length == 1) ? new WildSingleDirMatcher( pDirPattern ) : new WildMultiDirMatcher( zDirParts );
    }

    private static FileNameMatcher createFileNameMatcher( String pFileNamePattern )
    {
        pFileNamePattern = pFileNamePattern.trim();
        if ( !pFileNamePattern.contains( "*" ) && !pFileNamePattern.contains( "?" ) )
        {
            return new ExactFileNameMatcher( pFileNamePattern );
        }
        return new WildFileNameMatcher( pFileNamePattern );
    }

    /**
     * return True if the directory specified with <code>dirPath</code> <i>could possibly</i> host directories that <i>could</i> host files acceptable to this Pattern
     *
     * @param dirPath !null and path separators converted to '/'
     */
    public boolean acceptableParentDirPath( String dirPath )
    {
        return mDirMatcher.acceptableParentDir( dirPath );
    }

    /**
     * return True if the directory specified with <code>dirPath</code> <i>could</i> host files <b>directly</b> that are acceptable to this Pattern
     *
     * @param dirPath !null and path separators converted to '/'
     */
    public boolean acceptableDirPath( String dirPath )
    {
        return mDirMatcher.acceptable( dirPath );
    }

    /**
     * return True if the file specified with <code>filePath</code> is acceptable to this Pattern (any parent path is checked against the <code>matchesDirPath</code>)
     *
     * @param filePath !null and path separators converted to '/'
     */
    public boolean matchesFilePath( String filePath )
    {
        String dirPath = "";
        String fileName = filePath;
        int lastSep = filePath.lastIndexOf( '/' );
        if ( lastSep != -1 )
        {
            dirPath = filePath.substring( 0, lastSep );
            fileName = filePath.substring( lastSep + 1 );
        }
        return mDirMatcher.acceptable( dirPath ) && mFileNameMatcher.acceptable( fileName );
    }

    @Override
    public String toString()
    {
        return mPattern;
    }
}
