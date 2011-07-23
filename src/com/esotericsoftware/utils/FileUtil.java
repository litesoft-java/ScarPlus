package com.esotericsoftware.utils;

import java.io.*;
import java.nio.channels.*;

@SuppressWarnings({"UnusedDeclaration"})
public class FileUtil extends Util
{
    public static final File CANONICAL_USER_DIR;

    static
    {
        try
        {
            CANONICAL_USER_DIR = new File( System.getProperty( "user.dir" ) ).getCanonicalFile();
        }
        catch ( IOException e )
        {
            throw new Error( e );
        }
    }

    private static final IFileSystem FILE_SYSTEM = new IFileSystem()
    {
        @Override
        public boolean isWindows()
        {
            return isWindows;
        }

        @Override
        public char separatorChar()
        {
            return File.separatorChar;
        }

        @Override
        public String canonicalCurrentPath()
        {
            return CANONICAL_USER_DIR.getPath();
        }

        @Override
        public boolean exists( String path )
        {
            return new File( path ).exists();
        }

        @Override
        public String canonicalizeNormalizedExisting( String path )
        {
            File zFile = new File( path );
            if ( zFile.exists() )
            {
                return getCanonicalPath( zFile );
            }
            throw new WrappedIOException( new FileNotFoundException( path ) );
        }
    };

    public static File assertExists( String pWhat, File pToTest )
    {
        assertNotNull( pWhat, pToTest );
        if ( !pToTest.exists() )
        {
            throw new IllegalArgumentException( pWhat + " not found: " + pToTest.getAbsolutePath() );
        }
        return pToTest;
    }

    /**
     * Reads to the end of the input stream and writes the bytes to the output stream.
     */
    public static void copyStreamAndCloseEm( InputStream input, OutputStream output )
    {
        try
        {
            writeStream( input, output );
        }
        finally
        {
            dispose( input );
        }
    }

    public static void writeStream( InputStream input, OutputStream output )
    {
        try
        {
            append( input, output );
            Closeable zCloseable = output;
            output = null;
            close( zCloseable );
        }
        finally
        {
            dispose( output );
        }
    }

    public static void append( InputStream input, OutputStream output )
    {
        assertNotNull( "input", input );
        assertNotNull( "output", output );
        byte[] buf = new byte[1024];
        try
        {
            for ( int len; (len = input.read( buf )) > -1; )
            {
                if ( len != 0 )
                {
                    output.write( buf, 0, len );
                }
            }
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
    }

    /**
     * Copies one file to another.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void copyFile( File in, File out )
    {
        assertNotNull( "in", in );
        assertNotNull( "out", out );
        LOGGER.trace.log( "Copying file: ", in, " -> ", out );
        FileChannel sourceChannel = createFileInputStream( in ).getChannel();
        out.getParentFile().mkdirs();
        try
        {
            FileChannel destinationChannel = createFileOutputStream( out ).getChannel();
            try
            {
                sourceChannel.transferTo( 0, sourceChannel.size(), destinationChannel );
                Closeable zCloseable = destinationChannel;
                destinationChannel = null;
                close( zCloseable );
            }
            catch ( IOException e )
            {
                throw new WrappedIOException( e );
            }
            finally
            {
                dispose( destinationChannel );
            }
        }
        finally
        {
            dispose( sourceChannel );
        }
    }

    /**
     * Copies a file, overwriting any existing file at the destination.
     */
    public static String copyFile( String in, String out )
    {
        copyFile( new File( assertNotEmpty( "in", in ) ), new File( out = assertNotEmpty( "out", out ) ) );
        return out;
    }

    /**
     * Moves a file, overwriting any existing file at the destination.
     */
    static public String moveFile( String in, String out )
    {
        copyFile( in = assertNotEmpty( "in", in ), out );
        delete( in );
        return out;
    }

    /**
     * Returns the textual contents of the specified file.
     */
    static public String fileContents( String path )
    {
        StringBuilder stringBuffer = new StringBuilder( 4096 );
        FileReader reader = createFileReader( new File( path ) );
        try
        {
            char[] buffer = new char[2048];
            for ( int length; -1 != (length = reader.read( buffer )); )
            {
                stringBuffer.append( buffer, 0, length );
            }
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        finally
        {
            dispose( reader );
        }
        return stringBuffer.toString();
    }

    public static BufferedOutputStream createBufferedFileOutputStream( String filePath )
    {
        return createBufferedFileOutputStream( new File( filePath ) );
    }

    public static BufferedOutputStream createBufferedFileOutputStream( File out )
    {
        return new BufferedOutputStream( createFileOutputStream( out ) );
    }

    public static FileOutputStream createFileOutputStream( File out )
    {
        mkdir( out.getParentFile() );
        try
        {
            return new FileOutputStream( out );
        }
        catch ( FileNotFoundException e )
        {
            throw new WrappedIOException( e );
        }
    }

    public static FileInputStream createFileInputStream( File in )
    {
        try
        {
            return new FileInputStream( in );
        }
        catch ( FileNotFoundException e )
        {
            throw new WrappedIOException( e );
        }
    }

    public static FileReader createFileReader( File in )
    {
        try
        {
            return new FileReader( in );
        }
        catch ( FileNotFoundException e )
        {
            throw new WrappedIOException( e );
        }
    }

    /**
     * Creates the directories in the specified path.
     */
    public static File mkdir( File path )
    {
        assertNotNull( "path", path );
        if ( path.mkdirs() )
        {
            LOGGER.trace.log( "Created directory: ", path.getPath() );
        }
        return path;
    }

    /**
     * Creates the directories in the specified path.
     */
    public static String mkdir( String path )
    {
        mkdir( new File( path = assertNotEmpty( "path", path ) ) );
        return path;
    }

    /**
     * Deletes a directory and all files and directories it contains.
     */
    public static boolean delete( File pFile )
    {
        assertNotNull( "File", pFile );
        if ( pFile.isDirectory() )
        {
            File[] zFiles = pFile.listFiles();
            for ( File zFile : zFiles )
            {
                if ( !delete( zFile ) )
                {
                    return false;
                }
            }
        }
        LOGGER.trace.log( "Deleting file: ", pFile );
        return pFile.delete();
    }

    /**
     * Deletes a file or directory and all files and subdirecties under it.
     */
    public static boolean delete( String fileName )
    {
        fileName = noEmpty( fileName );
        return (fileName != null) && delete( new File( fileName ) );
    }

    public static Closeable dispose( Closeable pCloseable )
    {
        if ( pCloseable != null )
        {
            try
            {
                pCloseable.close();
            }
            catch ( IOException ignore )
            {
                // Whatever!
            }
            pCloseable = null;
        }
        return pCloseable;
    }

    public static void close( Closeable pCloseable )
    {
        if ( pCloseable != null )
        {
            try
            {
                pCloseable.close();
            }
            catch ( IOException e )
            {
                throw new WrappedIOException( e );
            }
        }
    }

    public static String getCanonicalPath( File pFile )
    {
        return getCanonicalFile( pFile ).getPath();
    }

    public static File getCanonicalFile( File pFile )
    {
        try
        {
            return pFile.getCanonicalFile();
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
    }

    public static boolean isAbsolutePath( String path )
    {
        assertNotNull( "path", path );
        return FileSupport.isAbsoluteNormalizedPath( FILE_SYSTEM, CANONICAL_USER_DIR.getPath(), FileSupport.normalizePath( FILE_SYSTEM, path ) );
    }

    public static String normalizePath( String path )
    {
        assertNotNull( "path", path );
        return FileSupport.normalizePath( FILE_SYSTEM, path );
    }

    public static File canonicalizePath( String path )
    {
        return canonicalizePath( CANONICAL_USER_DIR, path );
    }

    public static File canonicalizePath( File pCanonicalParentDirIfPathRelative, String path )
    {
        assertNotNull( "path", path );
        return new File( FileSupport.canonicalizeNormalizedPath( FILE_SYSTEM, pCanonicalParentDirIfPathRelative.getPath(), FileSupport.normalizePath( FILE_SYSTEM, path ) ) );
    }
}
