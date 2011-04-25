package com.esotericsoftware.wildcard;

import com.esotericsoftware.wildcard.support.*;

public class Pattern
{
    public static final java.util.regex.Pattern SLASH = java.util.regex.Pattern.compile( "/" );

    private final String mPattern;
    private final DirMatcher mDirMatcher;
    private final FileNameMatcher mFileNameMatcher;
    final String[] values;

    String value;

    private int index;

    public Pattern( String pattern )
    {
        mPattern = clean( pattern.replace( '\\', '/' ).trim() );
        int lastSep = pattern.lastIndexOf( '/' );
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

        // ************************************ OLD ************************************
        values = SLASH.split( pattern, 0 );
        value = values[0];
        System.out.println( "Pattern (" + values.length + "): " + pattern );
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
        String[] zDirParts = SLASH.split( pDirPattern, 0 );
        return (zDirParts.length == 1) ? new WildSingleDirMatcher( pDirPattern ) : new WildMultiDirMatcher( zDirParts );
    }

    private static FileNameMatcher createFileNameMatcher( String pFileNamePattern )
    {
        return null;  // todo: To change body of created methods use File | Settings | File Templates.
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
        if ( !mDirMatcher.acceptable( dirPath ) )
        {
            return false;
        }
        if ( mFileNameMatcher != null )
        {
            return mFileNameMatcher.acceptable( fileName );
        }
        if ( value.equals( "**" ) )
        {
            return true;
        }

        // filePath = filePath.toLowerCase();

        // Shortcut if no wildcards.
        if ( value.indexOf( '*' ) == -1 && value.indexOf( '?' ) == -1 )
        {
            return filePath.equals( value );
        }

        int i = 0, j = 0;
        while ( i < filePath.length() && j < value.length() && value.charAt( j ) != '*' )
        {
            if ( value.charAt( j ) != filePath.charAt( i ) && value.charAt( j ) != '?' )
            {
                return false;
            }
            i++;
            j++;
        }

        // If reached end of mPattern without finding a * wildcard, the match has to fail if not same length.
        if ( j == value.length() )
        {
            return filePath.length() == value.length();
        }

        int cp = 0;
        int mp = 0;
        while ( i < filePath.length() )
        {
            if ( j < value.length() && value.charAt( j ) == '*' )
            {
                if ( j++ >= value.length() )
                {
                    return true;
                }
                mp = j;
                cp = i + 1;
            }
            else if ( j < value.length() && (value.charAt( j ) == filePath.charAt( i ) || value.charAt( j ) == '?') )
            {
                j++;
                i++;
            }
            else
            {
                j = mp;
                i = cp++;
            }
        }

        // Handle trailing asterisks.
        while ( j < value.length() && value.charAt( j ) == '*' )
        {
            j++;
        }

        return j >= value.length();
    }

    String nextValue()
    {
        if ( index + 1 == values.length )
        {
            return null;
        }
        return values[index + 1];
    }

    boolean incr( String fileName )
    {
        if ( value.equals( "**" ) )
        {
            if ( index == values.length - 1 )
            {
                return false;
            }
            incr();
            if ( matchesFilePath( fileName ) )
            {
                incr();
            }
            else
            {
                decr();
                return false;
            }
        }
        else
        {
            incr();
        }
        return true;
    }

    void incr()
    {
        index++;
        if ( index >= values.length )
        {
            value = null;
        }
        else
        {
            value = values[index];
        }
    }

    void decr()
    {
        index--;
        if ( index > 0 && values[index - 1].equals( "**" ) )
        {
            index--;
        }
        value = values[index];
    }

    void reset()
    {
        index = 0;
        value = values[0];
    }

    boolean isExhausted()
    {
        return index >= values.length;
    }

    boolean isLast()
    {
        return index >= values.length - 1;
    }

    boolean wasFinalMatch()
    {
        return isExhausted() || (isLast() && value.equals( "**" ));
    }

    @Override
    public String toString()
    {
        return mPattern;
    }
}
