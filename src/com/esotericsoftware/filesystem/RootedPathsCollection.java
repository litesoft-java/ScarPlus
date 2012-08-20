package com.esotericsoftware.filesystem;

import java.io.*;
import java.util.*;

import com.esotericsoftware.scar.*;

public final class RootedPathsCollection
{
    private long mGreatestLastModified;
    private final Map<File, RootedPaths> mPaths = new HashMap<File, RootedPaths>();

    public void add( FilePath pFilePath )
    {
        File zParentDir = pFilePath.getSomeParentDir();
        RootedPaths zExistingRootedPaths = mPaths.get( zParentDir );
        if ( zExistingRootedPaths == null )
        {
            mPaths.put( zParentDir, zExistingRootedPaths = new RootedPaths( zParentDir ) );
        }
        zExistingRootedPaths.addCanonicalRelativePath( pFilePath.getFileSubPath() );
        mGreatestLastModified = Math.max( mGreatestLastModified, zExistingRootedPaths.getGreatestLastModified() );
    }

    public void add( RootedPaths pRootedPaths )
    {
        Utils.assertNotNull( "RootedPaths", pRootedPaths );
        File zRootDirectory = pRootedPaths.getCanonicalRootDirectory();
        RootedPaths zExistingRootedPaths = mPaths.get( zRootDirectory );
        if ( zExistingRootedPaths == null )
        {
            mPaths.put( zRootDirectory, zExistingRootedPaths = pRootedPaths );
        }
        else
        {
            zExistingRootedPaths.mergeIn( pRootedPaths );
        }
        mGreatestLastModified = Math.max( mGreatestLastModified, zExistingRootedPaths.getGreatestLastModified() );
    }

    public long getGreatestLastModified()
    {
        return mGreatestLastModified;
    }

    public RootedPaths[] getRootedPaths()
    {
        return mPaths.values().toArray( new RootedPaths[mPaths.size()] );
    }

    public boolean isEmpty()
    {
        return mPaths.isEmpty() || (count() == 0);
    }

    public int count()
    {
        int zCount = 0;
        for ( RootedPaths zPaths : mPaths.values() )
        {
            zCount += zPaths.count();
        }
        return zCount;
    }

    public List<FilePath> collectPaths( List<FilePath> pPaths )
    {
        for ( RootedPaths zPaths : mPaths.values() )
        {
            zPaths.collectPaths( pPaths );
        }
        return pPaths;
    }

    /* Package Friendly */
    void mergeIn( RootedPathsCollection them )
    {
        for ( RootedPaths zPaths : them.mPaths.values() )
        {
            add( zPaths );
        }
    }
}
