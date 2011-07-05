package com.esotericsoftware.wildcard;

import java.io.*;

import com.esotericsoftware.scar.*;

public final class FilePath
{
    private final File mSomeParentDir;
    private final String mFileSubPath;
    private final File mAbsolutePath;

    public FilePath( File pSomeParentDir, String pFileSubPath )
    {
        mAbsolutePath = Utils.canonical( new File( mSomeParentDir = pSomeParentDir, mFileSubPath = pFileSubPath ) );
    }

    public File getSomeParentDir()
    {
        return mSomeParentDir;
    }

    public String getFileSubPath()
    {
        return mFileSubPath;
    }

    public String absolute()
    {
        return mAbsolutePath.getPath();
    }

    public File file()
    {
        return mAbsolutePath;
    }

    public boolean equals( FilePath them )
    {
        return this == them || ((them != null) && this.absolute().equals( them.absolute() ));
    }

    public boolean equals( Object obj )
    {
        return (this == obj) || ((obj instanceof FilePath) && equals( (FilePath) obj ));
    }

    public int hashCode()
    {
        return absolute().hashCode();
    }
}
