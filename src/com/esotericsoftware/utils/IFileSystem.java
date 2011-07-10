package com.esotericsoftware.utils;

import java.io.*;

public interface IFileSystem
{
    public boolean isWindows();

    public char separatorChar();

    public String canonicalCurrentPath();

    public boolean exists(String path);

    public String canonicalizeNormalizedExisting(String path) throws IOException;
}

