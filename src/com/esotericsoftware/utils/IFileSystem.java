package com.esotericsoftware.utils;

import java.io.*;

public interface IFileSystem
{
    public char separatorChar();

    public boolean isWindows();

    public boolean exists(String path);

    public String canonicalizeNormalizedExisting(String path) throws IOException;
}
