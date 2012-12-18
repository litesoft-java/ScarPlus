package com.esotericsoftware.filesystem;

import com.esotericsoftware.scar.Utils;
import com.esotericsoftware.utils.FileSupport;
import com.esotericsoftware.utils.FileUtil;

import java.io.File;

public final class FilePath
{
    private final File mSomeParentDir;
    private final String mFileSubPath;
    private final File mCanonicalPath;

    private FilePath( File pSomeParentDir, String pFileSubPath, File pCanonicalPath )
    {
        mSomeParentDir = pSomeParentDir;
        mFileSubPath = pFileSubPath;
        mCanonicalPath = pCanonicalPath;
    }

    public FilePath( File pSomeParentDir, String pFileSubPath )
    {
        this( pSomeParentDir, pFileSubPath, Utils.canonical( new File( pSomeParentDir, pFileSubPath ) ) );
    }

    public static FilePath canonical( File pSomeParentDir, String pFileSubPath )
    {
        return new FilePath( pSomeParentDir, pFileSubPath, new File( pSomeParentDir, pFileSubPath ) );
    }

    private static FilePath innerAlreadyCanonical( File pFilePath )
    {
        return new FilePath( pFilePath.getParentFile(), pFilePath.getName(), pFilePath );
    }

    public static FilePath canonicalize( File pFilePath )
    {
        return innerAlreadyCanonical( Utils.canonical( pFilePath ) );
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

    public String relativeFromDir(File pCanonicalJarDir)
    {
        File zCommonDirPath = Utils.findCommonDirPathFromCanonicalDirPaths(pCanonicalJarDir, mCanonicalPath.getParentFile());
        if ( zCommonDirPath == null )
        {
            throw new IllegalStateException( "Unable to create Relative path from '" + pCanonicalJarDir + "' to: " + this );
        }
        String zJarDir = normalize(pCanonicalJarDir, zCommonDirPath);
        String zFilePath = normalize(mCanonicalPath, zCommonDirPath);
        while ( zJarDir.length() != 0 )
        {
            zFilePath = "../" + zFilePath;
            int zAt = zJarDir.lastIndexOf("/");
            if ( zAt == -1 )
            {
                zJarDir = "";
            }
            else
            {
                zJarDir = zJarDir.substring(0,zAt);
            }
        }
        return zFilePath;
    }

    private String normalize(File pPath, File pCommonDirPath) {
        String zPath = pPath.getPath().substring(pCommonDirPath.getPath().length()).replace('\\', '/');
        return zPath.startsWith("/") ? zPath.substring(1) : zPath;
    }

    public boolean equals( FilePath them )
    {
        return this == them || ((them != null) && this.canonical().equals( them.canonical() ));
    }

    @Override
    public boolean equals( Object obj )
    {
        return (this == obj) || ((obj instanceof FilePath) && equals( (FilePath) obj ));
    }

    @Override
    public int hashCode()
    {
        return canonical().hashCode();
    }

    @Override
    public String toString()
    {
        return canonical();
    }
}
