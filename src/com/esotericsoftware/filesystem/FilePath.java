package com.esotericsoftware.filesystem;

import java.io.*;

import com.esotericsoftware.scar.*;

public final class FilePath
{
    private final File mSomeParentDir;
    private final String mFileSubPath;
    private final File mCanonicalPath;

    public FilePath( File pSomeParentDir, String pFileSubPath )
    {
        mCanonicalPath = Utils.canonical( new File( mSomeParentDir = pSomeParentDir, mFileSubPath = pFileSubPath ) );
    }

    public File getSomeParentDir()
    {
        return mSomeParentDir;
    }

    public String getFileSubPath()
    {
        return mFileSubPath;
    }

    public String canonical()
    {
        return mCanonicalPath.getPath();
    }

    public File file()
    {
        return mCanonicalPath;
    }

    public boolean equals( FilePath them )
    {
        return this == them || ((them != null) && this.canonical().equals( them.canonical() ));
    }

    public boolean equals( Object obj )
    {
        return (this == obj) || ((obj instanceof FilePath) && equals( (FilePath) obj ));
    }

    public int hashCode()
    {
        return canonical().hashCode();
    }
}
