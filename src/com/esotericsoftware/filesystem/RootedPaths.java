package com.esotericsoftware.filesystem;

import java.io.*;
import java.util.*;

import com.esotericsoftware.scar.*;

public final class RootedPaths
{
    private long mGreatestLastModified;
    private File mCanonicalRootDirectory;
    private Set<String> mCanonicalRelativePaths = new HashSet<String>();

    public RootedPaths( File pCanonicalRootDirectory )
    {
        Utils.assertNotNull( "CanonicalRootDirectory", mCanonicalRootDirectory = pCanonicalRootDirectory );
    }

    public void addCanonicalRelativePath( String pPath )
    {
        mGreatestLastModified = Math.max( mGreatestLastModified, new File( mCanonicalRootDirectory, pPath = Utils.assertNotEmpty( "Path", pPath ) ).lastModified() );
        mCanonicalRelativePaths.add( pPath );
    }

    public long getGreatestLastModified()
    {
        return mGreatestLastModified;
    }

    public File getCanonicalRootDirectory()
    {
        return mCanonicalRootDirectory;
    }

    public int count()
    {
        return mCanonicalRelativePaths.size();
    }

    public void collectPaths( List<FilePath> pPaths )
    {
        for ( String zPath : mCanonicalRelativePaths )
        {
            pPaths.add( FilePath.canonical( mCanonicalRootDirectory, zPath ) );
        }
    }

    @Override
    public int hashCode()
    {
        return mCanonicalRootDirectory.hashCode();
    }

    public boolean equals( RootedPaths them )
    {
        return (them != null) && this.mCanonicalRootDirectory.equals( them.mCanonicalRootDirectory );
    }

    @Override
    public boolean equals( Object o )
    {
        return (o instanceof RootedPaths) && equals( (RootedPaths) o );
    }

    /* Package Friendly */
    void mergeIn( RootedPaths them ) // Assume only called by the RootedPathsCollection when the mCanonicalRootDirectory(s) are equal!
    {
        this.mGreatestLastModified = Math.max( this.mGreatestLastModified, them.mGreatestLastModified );
        this.mCanonicalRelativePaths.addAll( them.mCanonicalRelativePaths );
    }
}
