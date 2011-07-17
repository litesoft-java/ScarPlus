package com.esotericsoftware.filesupport;

public interface IFile
{
    boolean exists();

    boolean isDirectory();

    boolean canRead();

    String getPath();

    IFile getCanonicalFile();
}
