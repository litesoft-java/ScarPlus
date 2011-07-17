package com.esotericsoftware.filesupport;

import java.io.*;

import com.esotericsoftware.utils.*;

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
    {
        return new IFileFileProxy( FileUtil.getCanonicalFile( mFile ) );
    }
}
