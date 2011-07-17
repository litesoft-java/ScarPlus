package com.esotericsoftware.filesystem;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.esotericsoftware.utils.*;
import com.esotericsoftware.wildcard.*;

/**
 * Collects filesystem paths using wildcards, preserving the directory structure. Copies, deletes, and zips paths.
 */
public class Paths
{
    static private List<String> sDefaultGlobExcludes = new ArrayList<String>();

    /**
     * Only the Files will be stored!
     */
    private final RootedPathsCollection mPaths = new RootedPathsCollection();

    /**
     * Creates an empty Paths object.
     */
    public Paths()
    {
    }

    /**
     * Creates a Paths object and calls {@link #glob(String, String[])} with the specified arguments.
     */
    public Paths( String dir, String... patterns )
    {
        glob( dir, patterns );
    }

    public boolean isEmpty()
    {
        return mPaths.isEmpty();
    }

    public int count()
    {
        return mPaths.count();
    }

    public List<FilePath> getPaths()
    {
        return mPaths.collectPaths( new ArrayList<FilePath>() );
    }

    public void add( FilePath pFilePath )
    {
        mPaths.add( pFilePath );
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void add( RootedPaths pRootedPaths )
    {
        mPaths.add( pRootedPaths );
    }

    public void add( RootedPathsCollection pRootedPathsCollection )
    {
        mPaths.mergeIn( pRootedPathsCollection );
    }

    /**
     * Adds all paths from the specified Paths object to this Paths object.
     */
    public void add( Paths paths )
    {
        add( paths.mPaths );
    }

    /**
     * Calls {@link #glob(String, String...)}.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void glob( String dir, List<String> patterns )
    {
        glob( dir, (patterns == null) ? Util.EMPTY_STRING_ARRAY : patterns.toArray( new String[patterns.size()] ) );
    }

    /**
     * Collects all files and directories in the specified directory matching the wildcard patterns.
     *
     * @param dir      The directory containing the paths to collect. If it does not exist, no paths are collected. If null, "." is
     *                 assumed.
     * @param patterns The wildcard patterns of the paths to collect or exclude. Patterns may optionally contain wildcards
     *                 represented by asterisks and question marks. If empty or omitted then the dir parameter is split on the "|"
     *                 character, the first element is used as the directory and remaining are used as the patterns. If null, ** is
     *                 assumed (collects all paths).<br>
     *                 <br>
     *                 A single question mark (?) matches any single character. Eg, something? collects any path that is named
     *                 "something" plus any character.<br>
     *                 <br>
     *                 A single asterisk (*) matches any characters up to the next slash (/). Eg, *\*\something* collects any path that
     *                 has two directories of any name, then a file or directory that starts with the name "something".<br>
     *                 <br>
     *                 A double asterisk (**) matches any characters. Eg, **\something\** collects any path that contains a directory
     *                 named "something".<br>
     *                 <br>
     *                 A pattern starting with an exclamation point (!) causes paths matched by the pattern to be excluded, even if other
     *                 patterns would select the paths.
     */
    public void glob( String dir, String... patterns )
    {
        new PathPatterns( dir, patterns ).addTo( mPaths );
    }

    // ^^^^^^^^^^^^^^^^^^^^^^^ Should These be supported as they can introduce potentially conflicting FileSubPaths ^^^^^^^^^^^^^^^^^^^^^^^

    /**
     * Copies the files and directories to the specified directory.
     *
     * @return A paths object containing the paths of the new files.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public Paths copyTo( String destDir )
    {
        File zDest = new File( destDir );
        zDest.mkdirs();

        Paths newPaths = new Paths();
        for ( FilePath path : getPaths() )
        {
            String zSubPath = path.getFileSubPath();
            FileUtil.copyFile( path.file(), new File( destDir, zSubPath ) );
            newPaths.mPaths.add( new FilePath( zDest, zSubPath ) );
        }
        return newPaths;
    }

    /**
     * Compresses the files and directories specified by the paths into a new zip file at the specified location. If there are no
     * paths or all the paths are directories, no zip file will be created.
     *
     * @return Files Zipped, 0 means Zip File not even created!
     */
    public int zip( String destFile )
    {
        return zip( destFile, ZipFactory.FOR_ZIPS );
    }

    public int zip( String destFile, ZipFactory pFactory )
    {
        List<FilePath> zPaths = getPaths();
        if ( !zPaths.isEmpty() )
        {
            byte[] buf = new byte[1024];
            ZipOutputStream out = pFactory.createZOS( destFile, zPaths );
            try
            {
                for ( FilePath path : zPaths )
                {
                    try
                    {
                        out.putNextEntry( pFactory.createZE( path.getFileSubPath().replace( '\\', '/' ) ) );
                    }
                    catch ( IOException e )
                    {
                        throw new WrappedIOException( e );
                    }
                    FileInputStream in = FileUtil.createFileInputStream( path.file() );
                    try
                    {
                        for ( int len; (len = in.read( buf )) > -1; )
                        {
                            if ( len != 0 )
                            {
                                out.write( buf, 0, len );
                            }
                        }
                        out.closeEntry();
                    }
                    catch ( IOException e )
                    {
                        throw new WrappedIOException( e );
                    }
                    finally
                    {
                        FileUtil.close( in );
                    }
                }
            }
            finally
            {
                FileUtil.close( out );
            }
        }
        return zPaths.size();
    }

    /**
     * Returns the absolute paths delimited by the specified character.
     */
    public String toString( String delimiter )
    {
        StringBuilder sb = new StringBuilder( 256 );
        for ( String path : getFullPaths() )
        {
            if ( sb.length() > 0 )
            {
                sb.append( delimiter );
            }
            sb.append( path );
        }
        return sb.toString();
    }

    /**
     * Returns the absolute paths delimited by commas.
     */
    public String toString()
    {
        return toString( ", " );
    }

    /**
     * Returns a Paths object containing the paths that are files, as if each file were selected from its parent directory.
     */
    public Paths flatten()
    {
        Paths newPaths = new Paths();
        for ( File zFile : getFiles() )
        {
            newPaths.add( new FilePath( zFile.getParentFile(), zFile.getName() ) );
        }
        return newPaths;
    }

    /**
     * Returns the paths as File objects.
     */
    public List<File> getFiles()
    {
        List<FilePath> zPaths = getPaths();
        List<File> files = new ArrayList<File>( zPaths.size() );
        for ( FilePath path : zPaths )
        {
            files.add( path.file() );
        }
        return files;
    }

    /**
     * Returns the portion of the path after the root directory where the path was collected.
     */
    public List<String> getRelativePaths()
    {
        List<FilePath> zPaths = getPaths();
        List<String> rv = new ArrayList<String>( zPaths.size() );
        for ( FilePath path : zPaths )
        {
            rv.add( path.getFileSubPath() );
        }
        return rv;
    }

    /**
     * Returns the full paths.
     */
    public List<String> getFullPaths()
    {
        List<File> zFiles = getFiles();
        List<String> rv = new ArrayList<String>( zFiles.size() );
        for ( File file : zFiles )
        {
            rv.add( file.getPath() );
        }
        return rv;
    }

    /**
     * Returns the paths' filenames.
     */
    public List<String> getNames()
    {
        List<File> zFiles = getFiles();
        List<String> rv = new ArrayList<String>( zFiles.size() );
        for ( File file : zFiles )
        {
            rv.add( file.getName() );
        }
        return rv;
    }

    /**
     * Clears the exclude patterns that will be used in addition to the excludes specified for all glob searches.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    static public void clearDefaultGlobExcludes()
    {
        sDefaultGlobExcludes.clear();
    }

    /**
     * Adds exclude patterns that will be used in addition to the excludes specified for all glob searches.
     */
    static public void addDefaultGlobExcludes( String... pDefaultGlobExcludes )
    {
        if ( pDefaultGlobExcludes != null )
        {
            sDefaultGlobExcludes.addAll( Arrays.asList( pDefaultGlobExcludes ) );
        }
    }

    private static class PathPatterns
    {
        private final File mPath;
        private final boolean mIsFile;
        private final List<Pattern> mIncludes = new ArrayList<Pattern>();
        private final List<Pattern> mExcludes = new ArrayList<Pattern>();

        public PathPatterns( String pPath, String[] pPatterns )
        {
            pPath = Util.deNull( pPath, "." ).trim();
            if ( pPatterns == null || pPatterns.length == 0 )
            {
                String[] split = pPath.split( "\\|" ); // split on a '|'
                pPath = split[0].trim();
                pPatterns = new String[split.length - 1];
                for ( int i = 1, n = split.length; i < n; i++ )
                {
                    pPatterns[i - 1] = split[i].trim();
                }
            }
            File zPath = new File( pPath );
            if ( zPath.isFile() )
            {
                if ( pPatterns.length != 0 )
                {
                    throw new IllegalArgumentException( "Files (e.g. " + zPath + ") may NOT have patterns: " + Arrays.asList( pPatterns ) );
                }
                mIsFile = true;
                mPath = FileUtil.getCanonicalFile( zPath );
                return;
            }
            if ( !zPath.isDirectory() )
            {
                throw new IllegalArgumentException( "Path Reference not a File or Directory: " + zPath );
            }
            mIsFile = false;
            mPath = FileUtil.getCanonicalFile( zPath );
            List<String> zIncludes = new ArrayList<String>();
            List<String> zExcludes = new ArrayList<String>();
            for ( String zPattern : pPatterns )
            {
                if ( null != (zPattern = Util.noEmpty( zPattern )) )
                {
                    List<String> zList = zIncludes;
                    if ( zPattern.charAt( 0 ) == '!' )
                    {
                        if ( null == (zPattern = Util.noEmpty( zPattern.substring( 1 ) )) )
                        {
                            continue;
                        }
                        zList = zExcludes;
                    }
                    zList.add( zPattern );
                }
            }
            if ( zIncludes.isEmpty() )
            {
                zIncludes.add( "**" );
            }
            if ( sDefaultGlobExcludes != null )
            {
                zExcludes.addAll( sDefaultGlobExcludes );
            }
            addPatterns( mIncludes, zIncludes );
            addPatterns( mExcludes, zExcludes );
        }

        private void addPatterns( List<Pattern> pTargetPatterns, List<String> pSourcePatterns )
        {
            for ( String zPattern : pSourcePatterns )
            {
                pTargetPatterns.add( new Pattern( zPattern ) );
            }
        }

        public void addTo( RootedPathsCollection pPaths )
        {
            if ( mIsFile )
            {
                pPaths.add( new FilePath( mPath.getParentFile(), mPath.getName() ) );
                return;
            }
            // Must be a Directory! (See Above)
            RootedPaths zPaths = new RootedPaths( mPath );
            String[] zFileNames = mPath.list();
            for ( String zFileName : zFileNames )
            {
                File zFile = new File( mPath, zFileName );
                if ( zFile.isDirectory() )
                {
                    if ( !excludedDir( zFileName ) && acceptableDir( zFileName ) )
                    {
                        addTo( zPaths, zFileName + "/", zFile );
                    }
                }
                else
                {
                    if ( !excludedFile( zFileName ) && acceptableFile( zFileName ) )
                    {
                        zPaths.addCanonicalRelativePath( zFileName );
                    }
                }
            }
            pPaths.add( zPaths );
        }

        private void addTo( RootedPaths pPaths, String pAdditionalDirPath, File pDirectory )
        {
            String[] zFileNames = pDirectory.list();
            for ( String zFileName : zFileNames )
            {
                String zPath = pAdditionalDirPath + "/" + zFileName;
                File zFile = new File( pDirectory, zFileName );
                if ( zFile.isDirectory() )
                {
                    if ( !excludedDir( zPath ) && acceptableDir( zPath ) )
                    {
                        addTo( pPaths, zPath + "/", zFile );
                    }
                }
                else
                {
                    if ( !excludedFile( zPath ) && acceptableFile( zPath ) )
                    {

                        String zFullCanonicalPath = FileUtil.getCanonicalPath( zFile );
                        String zRelativeCanonicalPath = zFullCanonicalPath.substring( mPath.getPath().length() + 1 );
                        pPaths.addCanonicalRelativePath( zRelativeCanonicalPath );
                    }
                }
            }
        }

        private boolean acceptableDir( String pPath )
        {
            for ( Pattern zInclude : mIncludes )
            {
                if ( zInclude.acceptableDirPath( pPath ) )
                {
                    return true;
                }
            }
            return false;
        }

        private boolean acceptableFile( String pPath )
        {
            for ( Pattern zInclude : mIncludes )
            {
                if ( zInclude.matchesFilePath( pPath ) )
                {
                    return true;
                }
            }
            return false;
        }

        private boolean excludedDir( String pPath )
        {
            for ( Pattern zExclude : mExcludes )
            {
                if ( zExclude.matchesDirPathAndChildren( pPath ) )
                {
                    return true;
                }
            }
            return false;
        }

        private boolean excludedFile( String pPath )
        {
            for ( Pattern zExclude : mExcludes )
            {
                if ( zExclude.matchesFilePath( pPath ) )
                {
                    return true;
                }
            }
            return false;
        }
    }

    public static void main( String[] args )
            throws Exception
    {
        if ( args.length == 0 )
        {
            System.out.println( "Usage: dir [pattern] [, pattern ...]" );
            System.exit( 0 );
        }
        List<String> patterns = Arrays.asList( args );
        patterns = patterns.subList( 1, patterns.size() );
        for ( String path : new Paths( args[0], patterns.toArray( new String[patterns.size()] ) ).getFullPaths() )
        {
            System.out.println( path );
        }
    }
}
