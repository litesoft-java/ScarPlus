package com.esotericsoftware.wildcard.support;

import com.esotericsoftware.wildcard.*;

public class AllDirMatcher implements DirMatcher
{
    public static final DirMatcher INSTANCE = new AllDirMatcher();

    private AllDirMatcher()
    {
    }

    @Override
    public boolean acceptableParentDir( String dirPath )
    {
        return true;
    }

    @Override
    public boolean acceptable( String dirPath )
    {
        return true;
    }
}
