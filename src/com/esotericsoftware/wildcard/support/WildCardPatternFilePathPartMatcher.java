package com.esotericsoftware.wildcard.support;

public class WildCardPatternFilePathPartMatcher implements FilePathPartMatcher
{
    private static final java.util.regex.Pattern STAR_STAR = java.util.regex.Pattern.compile( "\\*\\*" );

    private final boolean mAcceptsAnything;
    private final java.util.regex.Pattern mPattern;

    public WildCardPatternFilePathPartMatcher( String pPart )
    {
        if ( (pPart = pPart.trim()).length() == 0 )
        {
            throw new IllegalArgumentException( "Wild Card Part May NOT be empty!" );
        }
        mAcceptsAnything = "*".equals( pPart = STAR_STAR.matcher( pPart ).replaceAll( "*" ) );
        if ( mAcceptsAnything )
        {
            mPattern = null;
        }
        else
        {
            StringBuilder sb = new StringBuilder().append( '^' );
            for ( int i = 0; i < pPart.length(); i++ )
            {
                char c = pPart.charAt( i );
                if ( c == '*' )
                {
                    sb.append( ".*" );
                }
                else if ( c == '?' )
                {
                    sb.append( '.' );
                }
                else
                {
                    if ( !Character.isLetter( c ) )
                    {
                        sb.append( '\\' );
                    }
                    sb.append( c );
                }
            }
            mPattern = java.util.regex.Pattern.compile( sb.append( '$' ).toString() );
        }
    }

    @Override
    public boolean acceptsAnyNumberOfParts()
    {
        return false;
    }

    @Override
    public boolean acceptsAnything()
    {
        return mAcceptsAnything;
    }

    @Override
    public boolean acceptable( String pFilePathPart )
    {
        return mAcceptsAnything || mPattern.matcher( pFilePathPart ).matches();
    }
}
