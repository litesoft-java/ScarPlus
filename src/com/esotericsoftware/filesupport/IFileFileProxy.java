package com.esotericsoftware.filesupport;

import java.io.*;

public class IFileFileProxy implements IFile
{
    private final File mFile;

    public IFileFileProxy( File pFile )
    {
        mFile = pFile;
    }

    @Override
    public boolean exists()
    {
        return mFile.exists();
    }

    @Override
    public boolean isDirectory()
    {
        return mFile.isDirectory();
    }

    @Override
    public boolean canRead()
    {
        return mFile.canRead();
    }

    @Override
    public String getPath()
    {
        return mFile.getPath();
    }

    @Override
    public IFile getCanonicalFile()
            throws IOException
    {
        return new IFileFileProxy( mFile.getCanonicalFile() );
    }
}
