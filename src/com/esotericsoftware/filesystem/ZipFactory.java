package com.esotericsoftware.filesystem;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.esotericsoftware.utils.*;

public interface ZipFactory
{
    ZipOutputStream createZOS( String pFilePath, List<FilePath> pPaths );

    ZipEntry createZE( String pRelativePath );

    public ZipFactory FOR_ZIPS = new ZipFactory()
    {
        @Override
        public ZipOutputStream createZOS( String pFilePath, List<FilePath> pPaths )
        {
            try
            {
                return new ZipOutputStream( new BufferedOutputStream( new FileOutputStream( pFilePath ) ) );
            }
            catch ( FileNotFoundException e )
            {
                throw new WrappedIOException( e );
            }
        }

        @Override
        public ZipEntry createZE( String pRelativePath )
        {
            return new ZipEntry( pRelativePath );
        }
    };
}
