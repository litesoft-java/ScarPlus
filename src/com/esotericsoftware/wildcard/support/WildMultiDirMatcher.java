package com.esotericsoftware.wildcard.support;

import com.esotericsoftware.wildcard.*;

public class WildMultiDirMatcher implements DirMatcher
{
    private String mDirPath;

    public WildMultiDirMatcher( String[] zDirParts )
    {
//        mDirPath = pDirPath;
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
