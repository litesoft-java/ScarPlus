package com.esotericsoftware.wildcard.support;

import com.esotericsoftware.wildcard.*;

public class WildMultiDirMatcher implements DirMatcher
{
    private final FilePathPartMatcher[] mMatchers;
    private final boolean mFirstPartIsStarStar;
    private final int mMinimumParts;

    public WildMultiDirMatcher( String[] zDirParts )
    {
        int requiredParts = 0;
        mMatchers = new FilePathPartMatcher[zDirParts.length];
        for ( int i = 0; i < zDirParts.length; i++ )
        {
            if ( !(mMatchers[i] = createMatcher( zDirParts[i] )).acceptsAnyNumberOfParts() )
            {
                requiredParts++;
            }
        }
        mFirstPartIsStarStar = mMatchers[0].acceptsAnyNumberOfParts();
        mMinimumParts = requiredParts;
    }

    private FilePathPartMatcher createMatcher( String pPart )
    {
        if ( "**".equals( pPart ) )
        {
            return StarStarFilePathPartMatcher.INSTANCE;
        }
        if ( !pPart.contains( "*" ) && !pPart.contains( "?" ) )
        {
            return new ExactFilePathPartMatcher( pPart );
        }
        return new WildCardPatternFilePathPartMatcher( pPart );
    }

    @Override
    public boolean acceptableParentDir( String dirPath )
    {
        if ( !mFirstPartIsStarStar )
        {
            String[] zDirParts = FilePathPartMatcher.SLASH.split( dirPath, 0 );
            for ( int i = 0; (i < zDirParts.length) && (i < mMatchers.length); i++ )
            {
                FilePathPartMatcher zMatcher = mMatchers[i];
                if ( !zMatcher.acceptable( zDirParts[i] ) )
                {
                    return false;
                }
                if ( zMatcher.acceptsAnyNumberOfParts() )
                {
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public boolean acceptable( String dirPath )
    {
        String[] zDirParts = FilePathPartMatcher.SLASH.split( dirPath, 0 );
        return (zDirParts.length >= mMinimumParts) && checkAcceptable( 0, zDirParts, 0 );
    }

    private boolean checkAcceptable( int pMatcherIndex, String[] pDirParts, int pPartsIndex )
    {
        while ( true )
        {
            // Check: No Matcher & No Parts -> true, but No Matcher & Have Parts -> false
            boolean noParts = pDirParts.length <= pPartsIndex;
            if ( mMatchers.length <= pMatcherIndex ) // No Matcher!
            {
                return noParts; // No Parts!
            }
            FilePathPartMatcher zMatcher = mMatchers[pMatcherIndex];
            if ( zMatcher.acceptsAnyNumberOfParts() )
            {
                // Check 0 parts match
                if ( checkAcceptable( pMatcherIndex + 1, pDirParts, pPartsIndex ) )
                {
                    return true;
                }
                // Check n skipped parts
                for ( int i = pPartsIndex + 1; i < pDirParts.length; i++ )
                {
                    if ( checkAcceptable( pMatcherIndex + 1, pDirParts, i ) )
                    {
                        return true;
                    }
                }
            }
            if ( noParts || !zMatcher.acceptable( pDirParts[pPartsIndex] ) )
            {
                return false;
            }
            pMatcherIndex++;
            pPartsIndex++;
        }
    }
}
