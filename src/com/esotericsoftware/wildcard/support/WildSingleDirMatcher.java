package com.esotericsoftware.wildcard.support;

import com.esotericsoftware.wildcard.*;

public class WildSingleDirMatcher implements DirMatcher
{
    private final FilePathPartMatcher mFilePathPartMatcher;

    public WildSingleDirMatcher( String pDirPath )
    {
        mFilePathPartMatcher = new WildCardPatternFilePathPartMatcher( pDirPath );
    }

    @Override
    public boolean acceptableParentDir( String dirPath )
    {
        return false;
    }

    @Override
    public boolean acceptable( String dirPath )
    {
        return !dirPath.contains( "/" ) && mFilePathPartMatcher.acceptable( dirPath );
    }
}
