package com.esotericsoftware.wildcard.support;

import com.esotericsoftware.wildcard.*;

public class CurrentDirMatcher implements DirMatcher
{
    public static final DirMatcher INSTANCE = new CurrentDirMatcher();

    private CurrentDirMatcher()
    {
    }

    @Override
    public boolean acceptableParentDir( String dirPath )
    {
        return false;
    }

    @Override
    public boolean acceptable( String dirPath )
    {
        return "".equals( dirPath );
    }
}
