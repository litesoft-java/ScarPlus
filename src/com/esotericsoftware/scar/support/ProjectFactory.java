package com.esotericsoftware.scar.support;

import java.io.*;

import com.esotericsoftware.scar.*;

public interface ProjectFactory
{
    Project project( File pCurrentDirectory, String pPath )
            throws IOException;
}
