package com.esotericsoftware.filesupport;

import java.io.*;

public interface IFile
{
    boolean exists();

    boolean isDirectory();

    boolean canRead();

    String getPath();

    IFile getCanonicalFile()
            throws IOException;
}
