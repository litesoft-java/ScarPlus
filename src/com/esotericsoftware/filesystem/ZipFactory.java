package com.esotericsoftware.filesystem;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public interface ZipFactory
{
    ZipOutputStream createZOS( String pFilePath, List<FilePath> pPaths )
            throws IOException;

    ZipEntry createZE( String pRelativePath );

    public ZipFactory FOR_ZIPS = new ZipFactory()
    {
        @Override
        public ZipOutputStream createZOS( String pFilePath, List<FilePath> pPaths )
                throws IOException
        {
            return new ZipOutputStream( new BufferedOutputStream( new FileOutputStream( pFilePath ) ) );
        }

        @Override
        public ZipEntry createZE( String pRelativePath )
        {
            return new ZipEntry( pRelativePath );
        }
    };
}
