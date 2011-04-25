package com.esotericsoftware.wildcard.support;

import com.esotericsoftware.wildcard.*;

public class ExactDirMatcher implements DirMatcher
{
    private String mDirPath;

    public ExactDirMatcher( String pDirPath )
    {
        mDirPath = pDirPath;
    }

    @Override
    public boolean acceptableParentDir( String dirPath )
    {
        return mDirPath.startsWith( dirPath + "/" );
    }

    @Override
    public boolean acceptable( String dirPath )
    {
        return mDirPath.equals( dirPath );
    }
}
