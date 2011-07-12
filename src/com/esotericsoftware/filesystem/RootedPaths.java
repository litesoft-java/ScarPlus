package com.esotericsoftware.filesystem;

import java.io.*;
import java.util.*;

public class RootedPaths
{
    private long mGreatestLastModified;
    private File mCanonicalRootDirectory;
    private Set<String> mCanonicalRelativePaths = new HashSet<String>(  );

    public RootedPaths( File pCanonicalRootDirectory )
    {
        mCanonicalRootDirectory = pCanonicalRootDirectory;
    }

    public static RootedPaths createForFile( File pCanonicalRootDirectory )
    {
//        mCanonicalRootDirectory = pCanonicalRootDirectory;
        return null; // todo...
    }

    public void addCanonicalRelativePath( String pPath )
    {
        mCanonicalRelativePaths.add( pPath );

    }
}
