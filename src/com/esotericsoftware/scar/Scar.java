package com.esotericsoftware.scar;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.Map.*;
import java.util.jar.*;
import java.util.zip.*;

import org.litesoft.logger.*;
import org.litesoft.logger.nonpublic.*;

import SevenZip.*;
import com.esotericsoftware.filesystem.*;
import com.esotericsoftware.scar.support.*;
import com.esotericsoftware.utils.*;
import com.esotericsoftware.yamlbeans.*;
import com.esotericsoftware.yamlbeans.parser.*;
import com.esotericsoftware.yamlbeans.tokenizer.*;

// BOZO - Add javadocs method.

/**
 * Provides utility methods for common Java build tasks.
 */
public class Scar extends Utils implements ProjectFactory
{
    public static final String DEFAULT_PROJECT_FILE_NAME = "Build";

    public static final String JAVA_EXTENSION = ".java";
    public static final String YAML_EXTENSION = ".yaml";

    protected static final Logger LOGGER = LoggerFactory.getLogger( Scar.class );

    protected final ProjectCache mProjectCache = new ProjectCache();

    private Project mLaunchProject;

    /**
     * The command line arguments Scar was started with. Empty if Scar was started with no arguments or Scar was not started from
     * the command line.
     */
    public final Arguments mArgs;

    public Scar( Arguments pArgs )
    {
        mArgs = (pArgs != null) ? pArgs : new Arguments();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public Project getLaunchProject()
    {
        return mLaunchProject;
    }

    /**
     * Loads the specified project with default values and loads any other projects needed for the "include" property.
     *
     * @param pCanonicalCurrentDirectory
     * @param pPath                      Path to a YAML project file, or a directory containing a "project.yaml" file.
     */
    @Override
    public Project project( File pCanonicalCurrentDirectory, String pPath )
    {
        Util.assertNotNull( "CurrentDirectory", pCanonicalCurrentDirectory );
        pPath = Util.assertNotEmpty( "Path", pPath );
        File zFile = new File( pPath );
        if ( !zFile.isAbsolute() )
        {
            zFile = new File( pCanonicalCurrentDirectory, pPath );
        }
        try
        {
            zFile = zFile.getCanonicalFile();
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }

        String zPath = zFile.getPath();
        Project zProject = mProjectCache.getByPath( zPath );
        if ( zProject != null )
        {
            return zProject;
        }
        try
        {
            if ( zFile.isFile() ) // Assume Project Build File
            {
                return mProjectCache.initialize( this, zPath, createProject( zFile, zFile.getParentFile() ) );
            }
            if ( zFile.isDirectory() ) // Assume Project Dir
            {
                return mProjectCache.initialize( this, zPath, createProject( findBuildFile( zFile ), zFile, zFile.getName() ) );
            }
        }
        catch ( WrappedIOException e )
        {
            throw new WrappedIOException( pPath, e );
        }
        throw new IllegalArgumentException( "Project is Neither a Project File, nor a Project Directory: " + zFile );
    }

    protected File findBuildFile( File pProjectDir )
    {
        File[] zFiles = pProjectDir.listFiles( BuildFileFilter.INSTANCE );
        if ( (zFiles == null) || (zFiles.length == 0) )
        {
            return null;
        }
        if ( zFiles.length == 1 )
        {
            return zFiles[0];
        }
        File zFile = findBuildFile( zFiles, JAVA_EXTENSION );
        return (zFile != null) ? zFile : findBuildFile( zFiles, YAML_EXTENSION );
    }

    private File findBuildFile( File[] pFiles, String pExtension )
    {
        File rv = null;
        for ( File zFile : pFiles )
        {
            if ( zFile.getName().endsWith( pExtension ) )
            {
                if ( rv != null )
                {
                    throw new IllegalStateException( "Found Both:\n   " + rv + "\n   " + zFile );
                }
                rv = zFile;
            }
        }
        return rv;
    }

    protected Project createProject( File pPossibleBuildFile, File pCanonicalProjectDir )
    {
        String zBuildFileName = pPossibleBuildFile.getName();
        int zDotAt = zBuildFileName.lastIndexOf( '.' );
        if ( (zDotAt != -1) && (pCanonicalProjectDir != null) )
        {
            String zName = zBuildFileName.substring( 0, zDotAt ).trim();
            if ( DEFAULT_PROJECT_FILE_NAME.equalsIgnoreCase( zName ) )
            {
                zName = pCanonicalProjectDir.getName();
            }
            return createProject( pPossibleBuildFile, pCanonicalProjectDir, zName );
        }
        throw new IllegalArgumentException( "Unacceptable Project Path or Name, Project File " + pPossibleBuildFile );
    }

    protected Project createProject( File pBuildFile, File pCanonicalProjectDir, String pProjectName )
    {
        if ( pBuildFile == null )
        {
            throw new IllegalArgumentException( "No 'Build.java' or 'Build.yaml' file found in Project Directory: " + pCanonicalProjectDir );
        }
        String zBuildFileName = pBuildFile.getName();
        if ( zBuildFileName.endsWith( JAVA_EXTENSION ) )
        {
            return createJavaProject( pBuildFile, pCanonicalProjectDir, pProjectName );
        }
        if ( zBuildFileName.endsWith( YAML_EXTENSION ) )
        {
            return createYamlProject( pBuildFile, pCanonicalProjectDir, pProjectName );
        }
        throw new IllegalArgumentException( pBuildFile + " was NOT either a '.java' or a '.yaml' file!" );
    }

    protected Project createYamlProject( File zYamlBuildFile, File pCanonicalProjectDir, String pProjectName )
    {
        List<String> zLines = readLines( zYamlBuildFile );
        int at = findLine( zLines, "---" );
        String zYAML, zCode = null;
        if ( at == -1 )
        {
            zYAML = mergeLines( zLines, 0 );
        }
        else
        {
            zYAML = mergeLines( zLines, 0, at );
            zCode = Util.noEmpty( mergeLines( zLines, at + 1 ) );
        }
        Map<Object, Object> zData = (zYAML.length() == 0) ? new HashMap<Object, Object>() : parseYAML( pCanonicalProjectDir, zYAML );
        Class<? extends Project> zClass = createYamlProjectClass();
        if ( zCode != null )
        {
            zClass = createYamlCodeProjectClass( zClass, zCode, at + 1 );
        }
        return instantiate( zClass, pCanonicalProjectDir, pProjectName, zData );
    }

    protected Project createJavaProject( File zJavaBuildFile, File pCanonicalProjectDir, String pProjectName )
    {
        String zFile = mergeLines( readLines( zJavaBuildFile ), 0 );
        Class<? extends Project> zClass = createJavaProjectClass();
        zClass = createJavaCodeProjectClass( zClass, zFile );
        return instantiate( zClass, pCanonicalProjectDir, pProjectName, null );
    }

    protected Project instantiate( Class<? extends Project> pClass, File pCanonicalProjectDir, String pProjectName, Map<Object, Object> pData )
    {
        return instantiate( pClass, new ProjectParameters( pProjectName, pCanonicalProjectDir, pData ) );
    }

    protected Project instantiate( Class<? extends Project> pClass, ProjectParameters pParameters )
    {
        Throwable zCause;
        try
        {
            Constructor zConstructor = pClass.getConstructor( ProjectParameters.class );
            return (Project) zConstructor.newInstance( pParameters );
        }
        catch ( NoSuchMethodException e )
        {
            zCause = e;
        }
        catch ( InvocationTargetException e )
        {
            zCause = e;
        }
        catch ( ClassCastException e )
        {
            zCause = e;
        }
        catch ( InstantiationException e )
        {
            zCause = e;
        }
        catch ( IllegalAccessException e )
        {
            zCause = e;
        }
        catch ( RuntimeException e )
        {
            zCause = e;
        }
        throw new RuntimeException( "Unable to Instantiate Project Class for Project: " + pParameters.getName() + " in dir " + pParameters.getCanonicalProjectDir(), zCause );
    }

    protected Class<? extends Project> createYamlProjectClass()
    {
        return Project.class;
    }

    protected Class<? extends Project> createJavaProjectClass()
    {
        return Project.class;
    }

    protected Map<Object, Object> parseYAML( File pProjectDir, String pYAML )
    {
        final String zProjectDir = pProjectDir.getPath().replace( '\\', '/' );

        YamlReader yamlReader = new YamlReader( new StringReader( pYAML ) )
        {
            @Override
            protected Object readValue( Class type, Class elementType, Class defaultType )
                    throws YamlException, Parser.ParserException, Tokenizer.TokenizerException
            {
                Object value = super.readValue( type, elementType, defaultType );
                if ( value instanceof String )
                {
                    value = ((String) value).replaceAll( "\\$dir\\$", zProjectDir );
                }
                return value;
            }
        };
        try
        {
            //noinspection unchecked
            return yamlReader.read( HashMap.class );
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        finally
        {
            try
            {
                yamlReader.close();
            }
            catch ( IOException e )
            {
                throw new WrappedIOException( e );
            }
        }
    }

    protected String mergeLines( List<String> pLines, int pFrom )
    {
        return mergeLines( pLines, pFrom, pLines.size() );
    }

    protected String mergeLines( List<String> pLines, int pFrom, int pToExclusive )
    {
        StringBuilder sb = new StringBuilder();
        while ( (pFrom < pToExclusive) && (pFrom < pLines.size()) )
        {
            sb.append( pLines.get( pFrom++ ) ).append( '\n' );
        }
        return sb.toString();
    }

    private int findLine( List<String> pLines, String zLine )
    {
        for ( int i = 0; i < pLines.size(); i++ )
        {
            if ( zLine.equals( pLines.get( i ) ) )
            {
                return i;
            }
        }
        return -1;
    }

    protected List<String> readLines( File zFile )
    {
        BufferedReader fileReader;
        try
        {
            fileReader = new BufferedReader( new FileReader( zFile ) );
        }
        catch ( FileNotFoundException e )
        {
            throw new WrappedIOException( e );
        }
        try
        {
            List<String> lines = new ArrayList<String>();
            for ( String line; null != (line = fileReader.readLine()); )
            {
                lines.add( line.trim() );
            }
            Closeable c = fileReader;
            fileReader = null;
            c.close();
            return lines;
        }
        catch ( IOException e )
        {
            if ( fileReader != null )
            {
                try
                {
                    fileReader.close();
                }
                catch ( IOException e1 )
                {
                    // Whatever!
                }
            }
            throw new WrappedIOException( e );
        }
    }

    protected void initLoggerFactory()
    {
        LoggerFactory.init( createLoggerLevel() );
    }

    protected LoggerLevel createLoggerLevel()
    {
        int zLevel = LoggerLevel.ERROR;
        String[] zLevels = LoggerLevel.LEVELS;
        for ( int i = 0; i < zLevels.length; i++ )
        {
            if ( null != mArgs.get( zLevels[i] ) )
            {
                zLevel = i;
                break;
            }
        }
        final int zLoggerLevel = zLevel;
        return new LoggerLevel()
        {
            @Override
            public int getEnabledLevel( String pClassName )
            {
                return zLoggerLevel;
            }
        };
    }

    protected Class<Project> createYamlCodeProjectClass( Class<? extends Project> pClass, String pCode, int pOverheadStartLines )
    {
        throw new UnsupportedOperationException(); // todo: See - executeDocument();
    }

    protected Class<Project> createJavaCodeProjectClass( Class<? extends Project> pClass, String pCode )
    {
        throw new UnsupportedOperationException(); // todo: See - executeDocument()!;
    }

    /**
     * Cleans All projects - Normally called reflectively
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void cleanAll()
    {
        progress( "CleanAll" );
        Set<Project> zProjects = mProjectCache.getAllProjects();
        for ( Project zProject : zProjects )
        {
            zProject.clean();
        }
    }

    /**
     * Builds the Launch Project - Normally called reflectively
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void build()
    {
        mLaunchProject.build();
    }

    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv Here be Dragons vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    /**
     * Loads the specified project with the specified defaults and loads any other projects needed for the "include" property.
     *
     * @param path Path to a YAML project file, or a directory containing a "project.yaml" file.
     */
//    public Project project( String path, Project defaults )
//    {
//        Util.assertNotNull( "path", path );
//        Util.assertNotNull( "defaults", defaults );
//
//        Project actualProject = new Project( path, this );
//
//        Project project = new Project();
//        project.replace( defaults );
//
//        File parent = actualProject.getDirectory().getParentFile();
//        while ( parent != null )
//        {
//            File includeFile = new File( parent, "include.yaml" );
//            if ( includeFile.exists() )
//            {
//                try
//                {
//                    project.replace( project( includeFile.getAbsolutePath(), defaults ) );
//                }
//                catch ( RuntimeException ex )
//                {
//                    throw new RuntimeException( "Error loading included project: " + includeFile.getAbsolutePath(), ex );
//                }
//            }
//            parent = parent.getParentFile();
//        }
//
//        for ( String include : actualProject.getInclude() )
//        {
//            try
//            {
//                project.replace( project( actualProject.path( include ), defaults ) );
//            }
//            catch ( RuntimeException ex )
//            {
//                throw new RuntimeException( "Error loading included project: " + actualProject.path( include ), ex );
//            }
//        }
//        project.replace( actualProject );
//        return project;
//    }

    /**
     * Removes any code signatures on the specified JAR. Removes any signature files in the META-INF directory and removes any
     * signature entries from the JAR's manifest.
     *
     * @return The path to the JAR file.
     */
    public String unsign( String jarFile )
    {
        Util.assertNotNull( "jarFile", jarFile );

        progress( "Removing signature from JAR: " + jarFile );

        File tempFile;
        try
        {
            tempFile = File.createTempFile( "scar", "removejarsig" );
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        JarOutputStream jarOutput = null;
        JarInputStream jarInput = null;
        try
        {
            jarOutput = new JarOutputStream( new FileOutputStream( tempFile ) );
            jarInput = new JarInputStream( new FileInputStream( jarFile ) );
            Manifest manifest = jarInput.getManifest();
            if ( manifest != null )
            {
                // Remove manifest file entries.
                manifest.getEntries().clear();
                jarOutput.putNextEntry( new JarEntry( "META-INF/MANIFEST.MF" ) );
                manifest.write( jarOutput );
            }
            byte[] buffer = new byte[4096];
            while ( true )
            {
                JarEntry entry = jarInput.getNextJarEntry();
                if ( entry == null )
                {
                    break;
                }
                String name = entry.getName();
                // Skip signature files.
                if ( name.startsWith( "META-INF/" ) && (name.endsWith( ".SF" ) || name.endsWith( ".DSA" ) || name.endsWith( ".RSA" )) )
                {
                    continue;
                }
                jarOutput.putNextEntry( new JarEntry( name ) );
                while ( true )
                {
                    int length = jarInput.read( buffer );
                    if ( length == -1 )
                    {
                        break;
                    }
                    jarOutput.write( buffer, 0, length );
                }
            }
            jarInput.close();
            jarOutput.close();
            copyFile( tempFile.getAbsolutePath(), jarFile );
        }
        catch ( IOException ex )
        {
            throw new WrappedIOException( "Error unsigning JAR file: " + jarFile, ex );
        }
        finally
        {
            dispose( jarInput );
            dispose( jarOutput );
            tempFile.delete();
        }
        return jarFile;
    }

    /**
     * Signs the specified JAR.
     *
     * @return The path to the JAR.
     */
    public String sign( String jarFile, String keystoreFile, String alias, String password )
    {
        Util.assertNotNull( "jarFile", jarFile );
        Util.assertNotNull( "keystoreFile", keystoreFile );
        Util.assertNotNull( "alias", alias );
        Util.assertNotNull( "password", password );
        if ( password.length() < 6 )
        {
            throw new IllegalArgumentException( "password must be 6 or more characters." );
        }
        progress( "Signing JAR (" + keystoreFile + ", " + alias + ":" + password + "): " + jarFile );

        shell( "jarsigner", "-keystore", keystoreFile, "-storepass", password, "-keypass", password, jarFile, alias );
        return jarFile;
    }

    /**
     * Encodes the specified file with pack200. The resulting filename is the filename plus ".pack". The file is deleted after
     * encoding.
     *
     * @return The path to the encoded file.
     */
    public String pack200( String jarFile )
    {
        String packedFile = pack200( jarFile, jarFile + ".pack" );
        delete( jarFile );
        return packedFile;
    }

    /**
     * Encodes the specified file with pack200.
     *
     * @return The path to the encoded file.
     */
    public String pack200( String jarFile, String packedFile )
    {
        Util.assertNotNull( "jarFile", jarFile );
        Util.assertNotNull( "packedFile", packedFile );

        progress( "Packing JAR: " + jarFile + " -> " + packedFile );

        shell( "pack200", "--no-gzip", "--segment-limit=-1", "--no-keep-file-order", "--effort=7", "--modification-time=latest", packedFile, jarFile );
        return packedFile;
    }

    /**
     * Decodes the specified file with pack200. The filename must end in ".pack" and the resulting filename has this stripped. The
     * encoded file is deleted after decoding.
     *
     * @return The path to the decoded file.
     */
    public String unpack200( String packedFile )
    {
        Util.assertNotNull( "packedFile", packedFile );
        if ( !packedFile.endsWith( ".pack" ) )
        {
            throw new IllegalArgumentException( "packedFile must end with .pack: " + packedFile );
        }

        String jarFile = unpack200( packedFile, substring( packedFile, 0, -5 ) );
        delete( packedFile );
        return jarFile;
    }

    /**
     * Decodes the specified file with pack200.
     *
     * @return The path to the decoded file.
     */
    public String unpack200( String packedFile, String jarFile )
    {
        Util.assertNotNull( "packedFile", packedFile );
        Util.assertNotNull( "jarFile", jarFile );

        progress( "Unpacking JAR: " + packedFile + " -> " + jarFile );

        shell( "unpack200", packedFile, jarFile );
        return jarFile;
    }

    /**
     * Encodes the specified file with GZIP. The resulting filename is the filename plus ".gz". The file is deleted after encoding.
     *
     * @return The path to the encoded file.
     */
    public String gzip( String file )
    {
        String gzipFile = gzip( file, file + ".gz" );
        delete( file );
        return gzipFile;
    }

    /**
     * Encodes the specified file with GZIP.
     *
     * @return The path to the encoded file.
     */
    public String gzip( String file, String gzipFile )
    {
        Util.assertNotNull( "file", file );
        Util.assertNotNull( "gzipFile", gzipFile );

        progress( "GZIP encoding: " + file + " -> " + gzipFile );

        InputStream input;
        try
        {
            input = new FileInputStream( file );
        }
        catch ( FileNotFoundException e )
        {
            throw new WrappedIOException( e );
        }
        try
        {
            copyStreamAndCloseEm( input, new GZIPOutputStream( new FileOutputStream( gzipFile ) ) );
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        finally
        {
            dispose( input );
        }
        return gzipFile;
    }

    /**
     * Decodes the specified GZIP file. The filename must end in ".gz" and the resulting filename has this stripped. The encoded
     * file is deleted after decoding.
     *
     * @return The path to the decoded file.
     */
    public String ungzip( String gzipFile )
    {
        Util.assertNotNull( "gzipFile", gzipFile );
        if ( !gzipFile.endsWith( ".gz" ) )
        {
            throw new IllegalArgumentException( "gzipFile must end with .gz: " + gzipFile );
        }

        String file = ungzip( gzipFile, substring( gzipFile, 0, -3 ) );
        delete( gzipFile );
        return file;
    }

    /**
     * Decodes the specified GZIP file.
     *
     * @return The path to the decoded file.
     */
    public String ungzip( String gzipFile, String file )
    {
        Util.assertNotNull( "gzipFile", gzipFile );
        Util.assertNotNull( "file", file );
        progress( "GZIP decoding: " + gzipFile + " -> " + file );

        InputStream input;
        try
        {
            input = new GZIPInputStream( new FileInputStream( gzipFile ) );
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        try
        {
            copyStreamAndCloseEm( input, new FileOutputStream( file ) );
        }
        catch ( FileNotFoundException e )
        {
            throw new WrappedIOException( e );
        }
        finally
        {
            dispose( input );
        }
        return file;
    }

    /**
     * Encodes the specified files with ZIP.
     *
     * @return The path to the encoded file.
     */
    public String zip( Paths paths, String zipFile )
    {
        Util.assertNotNull( "paths", paths );
        Util.assertNotNull( "zipFile", zipFile );
        progress( "Creating ZIP (" + paths.count() + " entries): " + zipFile );

        paths.zip( zipFile );
        return zipFile;
    }


    /**
     * Encodes the specified file with LZMA. The resulting filename is the filename plus ".lzma". The file is deleted after
     * encoding.
     *
     * @return The path to the encoded file.
     */
    public String lzma( String file )
    {
        String lzmaFile = lzma( file, file + ".lzma" );
        delete( file );
        return lzmaFile;
    }

    /**
     * Encodes the specified file with LZMA.
     *
     * @return The path to the encoded file.
     */
    public String lzma( String file, String lzmaFile )
    {
        Util.assertNotNull( "file", file );
        Util.assertNotNull( "lzmaFile", lzmaFile );
        progress( "LZMA encoding: " + file + " -> " + lzmaFile );

        try
        {
            LzmaAlone.main( new String[]{"e", file, lzmaFile} );
        }
        catch ( Exception ex )
        {
            throw new WrappedIOException( "Error lzma compressing file: " + file, ex );
        }
        return lzmaFile;
    }

    /**
     * Decodes the specified LZMA file. The filename must end in ".lzma" and the resulting filename has this stripped. The encoded
     * file is deleted after decoding.
     *
     * @return The path to the decoded file.
     */
    public String unlzma( String lzmaFile )
    {
        Util.assertNotNull( "lzmaFile", lzmaFile );
        if ( !lzmaFile.endsWith( ".lzma" ) )
        {
            throw new IllegalArgumentException( "lzmaFile must end with .lzma: " + lzmaFile );
        }

        String file = unlzma( lzmaFile, substring( lzmaFile, 0, -5 ) );
        delete( lzmaFile );
        return file;
    }

    /**
     * Decodes the specified LZMA file.
     *
     * @return The path to the decoded file.
     */
    public String unlzma( String lzmaFile, String file )
    {
        Util.assertNotNull( "lzmaFile", lzmaFile );
        Util.assertNotNull( "file", file );
        progress( "LZMA decoding: " + lzmaFile + " -> " + file );

        try
        {
            LzmaAlone.main( new String[]{"d", lzmaFile, file} );
        }
        catch ( Exception ex )
        {
            throw new WrappedIOException( "Error lzma decompressing file: " + file, ex );
        }
        return file;
    }

    /**
     * Copies all the JAR and JNLP files from the "dist" directory to a "jws" directory under the "target" directory. It then uses
     * the specified keystore to sign each JAR. If the "pack" parameter is true, it also compresses each JAR using pack200 and
     * GZIP.
     */
    public void jws( Project project, boolean pack, String keystoreFile, String alias, String password )
    {
        Util.assertNotNull( "Project", project );
        Util.assertNotNull( "keystoreFile", keystoreFile );
        Util.assertNotNull( "alias", alias );
        Util.assertNotNull( "password", password );
        if ( password.length() < 6 )
        {
            throw new IllegalArgumentException( "password must be 6 or more characters." );
        }

        progress( "JWS: " + project );

        String jwsDir = mkdir( project.path( "$target$/jws/" ) );
        String distDir = project.path( "$target$/dist/" );
        new Paths( distDir, "*.jar", "*.jnlp" ).copyTo( jwsDir );
        for ( String file : new Paths( jwsDir, "*.jar" ).getFullPaths() )
        {
            sign( unpack200( pack200( unsign( file ) ) ), keystoreFile, alias, password );
        }
        if ( pack )
        {
            String unpackedDir = mkdir( jwsDir + "unpacked/" );
            String packedDir = mkdir( jwsDir + "packed/" );
            for ( String file : new Paths( jwsDir, "*.jar", "!*native*" ).getFullPaths() )
            {
                String fileName = fileName( file );
                String unpackedFile = unpackedDir + fileName;
                moveFile( file, unpackedFile );
                String packedFile = packedDir + fileName;
                gzip( pack200( copyFile( unpackedFile, packedFile ) ) );
            }
        }
    }

    /**
     * Generates ".htaccess" and "type map" VAR files in the "jws" directory. These files allow Apache to serve both pack200/GZIP
     * JARs and regular JARs, based on capability of the client requesting the JAR.
     */
    public void jwsHtaccess( Project project )
    {
        Util.assertNotNull( "Project", project );

        progress( "JWS htaccess: " + project );

        String jwsDir = mkdir( project.path( "$target$/jws/" ) );
        for ( String packedFile : new Paths( jwsDir + "packed", "*.jar.pack.gz" ).getFullPaths() )
        {
            String packedFileName = fileName( packedFile );
            String jarFileName = substring( packedFileName, 0, -8 );
            FileWriter writer;
            try
            {
                writer = new FileWriter( jwsDir + jarFileName + ".var" );
            }
            catch ( IOException e )
            {
                throw new WrappedIOException( e );
            }
            try
            {
                writer.write( "URI: packed/" + packedFileName + "\n" );
                writer.write( "Content-Type: x-java-archive\n" );
                writer.write( "Content-Encoding: pack200-gzip\n" );
                writer.write( "URI: unpacked/" + jarFileName + "\n" );
                writer.write( "Content-Type: x-java-archive\n" );
            }
            catch ( IOException e )
            {
                throw new WrappedIOException( e );
            }
            finally
            {
                dispose( writer );
            }
        }
        FileWriter writer;
        try
        {
            writer = new FileWriter( jwsDir + ".htaccess" );
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        try
        {
            writer.write( "AddType application/x-java-jnlp-file .jnlp" ); // JNLP mime type.
            writer.write( "AddType application/x-java-archive .jar\n" ); // JAR mime type.
            writer.write( "AddHandler application/x-type-map .var\n" ); // Enable type maps.
            writer.write( "Options +MultiViews\n" );
            writer.write( "MultiViewsMatch Any\n" ); // Apache 2.0 only.
            writer.write( "<Files *.pack.gz>\n" );
            writer.write( "AddEncoding pack200-gzip .jar\n" ); // Enable Content-Encoding header for .jar.pack.gz files.
            writer.write( "RemoveEncoding .gz\n" ); // Prevent mod_gzip from messing with the Content-Encoding response.
            writer.write( "</Files>\n" );
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        finally
        {
            dispose( writer );
        }
    }

    /**
     * Generates a JNLP file in the "jws" directory. JARs in the "jws" directory are included in the JNLP. JARs containing "native"
     * and "win", "mac", "linux", or "solaris" are properly included in the native section of the JNLP. The "main" property is used
     * for the main class in the JNLP.
     *
     * @param splashImage Can be null.
     */
    public void jnlp( Project project, String url, String company, String title, String splashImage )
    {
        Util.assertNotNull( "Project", project );
        Util.assertNotNull( "company", company );
        Util.assertNotNull( "title", title );
        Util.assertNotNull( "url", url );
        if ( !url.startsWith( "http" ) )
        {
            throw new RuntimeException( "Invalid url: " + url );
        }

        if ( LOGGER.debug.isEnabled() )
        {
            progress( "JNLP: " + project + " (" + url + ", " + company + ", " + title + ", " + splashImage + ")" );
        }
        else
        {
            progress( "JNLP: " + project );
        }

        if ( !project.hasMain() )
        {
            throw new RuntimeException( "Unable to generate JNLP: project has no main class" );
        }

        int firstSlash = url.indexOf( "/", 7 );
        int lastSlash = url.lastIndexOf( "/" );
        if ( firstSlash == -1 || lastSlash == -1 )
        {
            throw new RuntimeException( "Invalid url: " + url );
        }
        String domain = url.substring( 0, firstSlash + 1 );
        String path = url.substring( firstSlash + 1, lastSlash + 1 );
        String jnlpFile = url.substring( lastSlash + 1 );

        String jwsDir = mkdir( project.path( "$target$/jws/" ) );
        FileWriter writer;
        try
        {
            writer = new FileWriter( jwsDir + jnlpFile );
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        try
        {
            writer.write( "<?xml version='1.0' encoding='utf-8'?>\n" );
            writer.write( "<jnlp spec='1.0+' codebase='" + domain + "' href='" + path + jnlpFile + "'>\n" );
            writer.write( "<information>\n" );
            writer.write( "\t<title>" + title + "</title>\n" );
            writer.write( "\t<vendor>" + company + "</vendor>\n" );
            writer.write( "\t<homepage href='" + domain + "'/>\n" );
            writer.write( "\t<description>" + title + "</description>\n" );
            writer.write( "\t<description kind='short'>" + title + "</description>\n" );
            if ( splashImage != null )
            {
                writer.write( "\t<icon kind='splash' href='" + path + splashImage + "'/>\n" );
            }
            writer.write( "</information>\n" );
            writer.write( "<security>\n" );
            writer.write( "\t<all-permissions/>\n" );
            writer.write( "</security>\n" );
            writer.write( "<resources>\n" );
            writer.write( "\t<j2se href='http://java.sun.com/products/autodl/j2se' version='1.5+' max-heap-size='128m'/>\n" );

            // JAR with main class first.
            String projectJarName;
            if ( project.hasVersion() )
            {
                projectJarName = project.format( "$name$-$version$.jar" );
            }
            else
            {
                projectJarName = project.format( "$name$.jar" );
            }
            writer.write( "\t<jar href='" + path + projectJarName + "'/>\n" );

            // Rest of JARs, except natives.
            for ( String file : new Paths( jwsDir, "**/*.jar", "!*native*", "!**/" + projectJarName ).getFullPaths() )
            {
                writer.write( "\t<jar href='" + path + fileName( file ) + "'/>\n" );
            }

            writer.write( "</resources>\n" );
            Paths nativePaths = new Paths( jwsDir, "*native*win*", "*win*native*" );
            if ( nativePaths.count() == 1 )
            {
                writer.write( "<resources os='Windows'>\n" );
                writer.write( "\t<j2se href='http://java.sun.com/products/autodl/j2se' version='1.5+' max-heap-size='128m'/>\n" );
                writer.write( "\t<nativelib href='" + path + nativePaths.getNames().get( 0 ) + "'/>\n" );
                writer.write( "</resources>\n" );
            }
            nativePaths = new Paths( jwsDir, "*native*mac*", "*mac*native*" );
            if ( nativePaths.count() == 1 )
            {
                writer.write( "<resources os='Mac'>\n" );
                writer.write( "\t<j2se href='http://java.sun.com/products/autodl/j2se' version='1.5+' max-heap-size='128m'/>\n" );
                writer.write( "\t<nativelib href='" + path + nativePaths.getNames().get( 0 ) + "'/>\n" );
                writer.write( "</resources>\n" );
            }
            nativePaths = new Paths( jwsDir, "*native*linux*", "*linux*native*" );
            if ( nativePaths.count() == 1 )
            {
                writer.write( "<resources os='Linux'>\n" );
                writer.write( "\t<j2se href='http://java.sun.com/products/autodl/j2se' version='1.5+' max-heap-size='128m'/>\n" );
                writer.write( "\t<nativelib href='" + path + nativePaths.getNames().get( 0 ) + "'/>\n" );
                writer.write( "</resources>\n" );
            }
            nativePaths = new Paths( jwsDir, "*native*solaris*", "*solaris*native*" );
            if ( nativePaths.count() == 1 )
            {
                writer.write( "<resources os='SunOS'>\n" );
                writer.write( "\t<j2se href='http://java.sun.com/products/autodl/j2se' version='1.5+' max-heap-size='128m'/>\n" );
                writer.write( "\t<nativelib href='" + path + nativePaths.getNames().get( 0 ) + "'/>\n" );
                writer.write( "</resources>\n" );
            }
            writer.write( "<application-desc main-class='" + project.getMain() + "'/>\n" );
            writer.write( "</jnlp>" );
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        finally
        {
            dispose( writer );
        }
    }

    public String lwjglApplet( Project project, String keystoreFile, String alias, String password )
    {
        Util.assertNotNull( "Project", project );
        Util.assertNotNull( "keystoreFile", keystoreFile );
        Util.assertNotNull( "alias", alias );
        Util.assertNotNull( "password", password );
        if ( password.length() < 6 )
        {
            throw new IllegalArgumentException( "password must be 6 or more characters." );
        }

        progress( "LWJGL applet: " + project );

        String appletDir = mkdir( project.path( "$target$/applet-lwjgl/" ) );
        String distDir = project.path( "$target$/dist/" );
        new Paths( distDir, "**/*.jar", "*.html", "*.htm" ).flatten().copyTo( appletDir );
        for ( String jarFile : new Paths( appletDir, "*.jar" ).getFullPaths() )
        {
            sign( unpack200( pack200( unsign( jarFile ) ) ), keystoreFile, alias, password );
            String fileName = fileName( jarFile );
            if ( fileName.equals( "lwjgl_util_applet.jar" ) || fileName.equals( "lzma.jar" ) )
            {
                continue;
            }
            if ( fileName.contains( "native" ) )
            {
                lzma( jarFile );
            }
            else
            {
                lzma( pack200( jarFile ) );
            }
        }

        if ( !new Paths( appletDir, "*.html", "*.htm" ).isEmpty() )
        {
            return appletDir;
        }
        if ( !project.hasMain() )
        {
            LOGGER.debug.log( "Unable to generate applet.html: project has no main class" );
            return appletDir;
        }
        progress( "Generating: applet.html" );
        FileWriter writer;
        try
        {
            writer = new FileWriter( appletDir + "applet.html" );
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        try
        {
            writer.write( "<html>\n" );
            writer.write( "<head><title>Applet</title></head>\n" );
            writer.write( "<body>\n" );
            writer.write( "<applet code='org.lwjgl.util.applet.AppletLoader' archive='lwjgl_util_applet.jar, lzma.jar' codebase='.' width='640' height='480'>\n" );
            if ( project.hasVersion() )
            {
                writer.write( "<param name='al_version' value='" + project.getVersion() + "'>\n" );
            }
            writer.write( "<param name='al_title' value='" + project + "'>\n" );
            writer.write( "<param name='al_main' value='" + project.getMain() + "'>\n" );
            writer.write( "<param name='al_jars' value='" );
            int i = 0;
            for ( String name : new Paths( appletDir, "*.jar.pack.lzma" ).getNames() )
            {
                if ( i++ > 0 )
                {
                    writer.write( ", " );
                }
                writer.write( name );
            }
            writer.write( "'>\n" );
            Paths nativePaths = new Paths( appletDir, "*native*win*.jar.lzma", "*win*native*.jar.lzma" );
            if ( nativePaths.count() == 1 )
            {
                writer.write( "<param name='al_windows' value='" + nativePaths.getNames().get( 0 ) + "'>\n" );
            }
            nativePaths = new Paths( appletDir, "*native*mac*.jar.lzma", "*mac*native*.jar.lzma" );
            if ( nativePaths.count() == 1 )
            {
                writer.write( "<param name='al_mac' value='" + nativePaths.getNames().get( 0 ) + "'>\n" );
            }
            nativePaths = new Paths( appletDir, "*native*linux*.jar.lzma", "*linux*native*.jar.lzma" );
            if ( nativePaths.count() == 1 )
            {
                writer.write( "<param name='al_linux' value='" + nativePaths.getNames().get( 0 ) + "'>\n" );
            }
            nativePaths = new Paths( appletDir, "*native*solaris*.jar.lzma", "*solaris*native*.jar.lzma" );
            if ( nativePaths.count() == 1 )
            {
                writer.write( "<param name='al_solaris' value='" + nativePaths.getNames().get( 0 ) + "'>\n" );
            }
            writer.write( "<param name='al_logo' value='appletlogo.png'>\n" );
            writer.write( "<param name='al_progressbar' value='appletprogress.gif'>\n" );
            writer.write( "<param name='separate_jvm' value='true'>\n" );
            writer.write( "<param name='java_arguments' value='-Dsun.java2d.noddraw=true -Dsun.awt.noerasebackground=true -Dsun.java2d.d3d=false -Dsun.java2d.opengl=false -Dsun.java2d.pmoffscreen=false'>\n" );
            writer.write( "</applet>\n" );
            writer.write( "</body></html>\n" );
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        finally
        {
            dispose( writer );
        }
        return appletDir;
    }

    /**
     * Compiles and executes the specified Java code. The code is compiled as if it were a Java method body.
     * <p/>
     * Imports statements can be used at the start of the code. These imports are automatically used:<br>
     * import com.esotericsoftware.scar.Scar;<br>
     * import com.esotericsoftware.filesystem.Paths;<br>
     * import com.esotericsoftware.minlog.Log;<br>
     * import static com.esotericsoftware.scar.Scar.*;<br>
     * import static com.esotericsoftware.minlog.Log.*;<br>
     * <p/>
     * Entries can be added to the classpath by using "classpath [url];" statements at the start of the code. These classpath
     * entries are checked before the classloader that loaded the Scar class is checked. Examples:<br>
     * classpath someTools.jar;<br>
     * classpath some/directory/of/class/files;<br>
     * classpath http://example.com/someTools.jar;<br>
     *
     * @param parameters These parameters will be available in the scope where the code is executed.
     */
    public void executeCode( Project project, String code, HashMap<String, Object> parameters )
    {
        try
        {
            // Wrap code in a class.
            StringBuilder classBuffer = new StringBuilder( 2048 );
            classBuffer.append( "import com.esotericsoftware.scar.*;\n" );
            classBuffer.append( "import com.esotericsoftware.minlog.Log;\n" );
            classBuffer.append( "import com.esotericsoftware.filesystem.Paths;\n" );
            classBuffer.append( "import static com.esotericsoftware.scar.Scar.*;\n" );
            classBuffer.append( "import static com.esotericsoftware.minlog.Log.*;\n" );
            classBuffer.append( "public class Generated {\n" );
            int pOverheadStartLines = 6;
            classBuffer.append( "public void execute (" );
            int i = 0;
            for ( Entry<String, Object> entry : parameters.entrySet() )
            {
                if ( i++ > 0 )
                {
                    classBuffer.append( ',' );
                }
                classBuffer.append( '\n' );
                pOverheadStartLines++;
                classBuffer.append( entry.getValue().getClass().getName() );
                classBuffer.append( ' ' );
                classBuffer.append( entry.getKey() );
            }
            classBuffer.append( "\n) throws Exception {\n" );
            pOverheadStartLines += 2;

            // Append code, collecting imports statements and classpath URLs.
            StringBuilder importBuffer = new StringBuilder( 512 );
            ArrayList<URL> classpathURLs = new ArrayList<URL>();
            BufferedReader reader = new BufferedReader( new StringReader( code ) );
            boolean header = true;
            while ( true )
            {
                String line = reader.readLine();
                if ( line == null )
                {
                    break;
                }
                String trimmed = line.trim();
                if ( header && trimmed.startsWith( "import " ) && trimmed.endsWith( ";" ) )
                {
                    importBuffer.append( line );
                    importBuffer.append( '\n' );
                }
                else if ( header && trimmed.startsWith( "classpath " ) && trimmed.endsWith( ";" ) )
                {
                    String path = substring( line.trim(), 10, -1 );
                    try
                    {
                        classpathURLs.add( new URL( path ) );
                    }
                    catch ( MalformedURLException ex )
                    {
                        classpathURLs.add( new File( project.path( path ) ).toURI().toURL() );
                    }
                }
                else
                {
                    if ( trimmed.length() > 0 )
                    {
                        header = false;
                    }
                    classBuffer.append( line );
                    classBuffer.append( '\n' );
                }
            }
            classBuffer.append( "}}" );

            final String classCode = importBuffer.append( classBuffer ).toString();
            if ( LOGGER.trace.isEnabled() )
            {
                progress( "Executing code:\n" + classCode );
            }
            // Compile class.
            Class generatedClass = compileDynamicCodeToClass( pOverheadStartLines, classCode, classpathURLs.toArray( new URL[classpathURLs.size()] ) );

            // Execute.
            Class[] parameterTypes = new Class[parameters.size()];
            Object[] parameterValues = new Object[parameters.size()];
            i = 0;
            for ( Object object : parameters.values() )
            {
                parameterValues[i] = object;
                parameterTypes[i++] = object.getClass();
            }
            generatedClass.getMethod( "execute", parameterTypes ).invoke( generatedClass.newInstance(), parameterValues );
        }
        catch ( Throwable ex )
        {
            throw new RuntimeException( "Error executing code:\n" + code.trim(), ex );
        }
    }

    /**
     * Executes Java code in the specified project's document, if any.
     *
     * @return true if code was executed.
     */
    public boolean executeDocument( Project project )
    {
        String code = null; // todo: was -- project.getDocument();
        if ( code == null || code.trim().isEmpty() )
        {
            return false;
        }
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put( "project", project );
        executeCode( project, code, parameters );
        return true;
    }

    /**
     * List of project names that have been built. {@link #buildDependencies(Project)} will skip any projects with a matching name.
     */
    static public final List<String> builtProjects = new ArrayList<String>();

    static
    {
        Paths.addDefaultGlobExcludes( "**/.svn/**" );
    }

    /// todo: ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ Here be Dragons ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================
    /// todo: ==================================================================================================================

    private static class BuildFileFilter implements FileFilter
    {
        private static final String DEFAULT_JAVA_PROJECT_FILE_NAME = DEFAULT_PROJECT_FILE_NAME + JAVA_EXTENSION;
        private static final String DEFAULT_YAML_PROJECT_FILE_NAME = DEFAULT_PROJECT_FILE_NAME + YAML_EXTENSION;

        @Override
        public boolean accept( File pFile )
        {
            if ( pFile.isFile() )
            {
                String zName = pFile.getName();
                if ( DEFAULT_JAVA_PROJECT_FILE_NAME.equalsIgnoreCase( zName ) || DEFAULT_YAML_PROJECT_FILE_NAME.equalsIgnoreCase( zName ) )
                {
                    return pFile.canRead();
                }
            }
            return false;
        }

        public static final FileFilter INSTANCE = new BuildFileFilter();
    }

    private static class ProjectCache
    {
        private Map<String, Project> mProjectByName = new HashMap<String, Project>();
        private Map<String, Project> mProjectByPath = new HashMap<String, Project>();

        public synchronized Project getByPath( String pPath )
        {
            return mProjectByPath.get( pPath );
        }

        private Project initialize( ProjectFactory pFactory, String pPath, Project pProject )
        {
            synchronized ( this )
            {
                String zName = pProject.getName();
                Project zProject = mProjectByName.get( zName );
                if ( zProject != null )
                {
                    mProjectByPath.put( pPath, zProject );
                    return zProject;
                }
                mProjectByPath.put( pPath, pProject );
                mProjectByName.put( zName, pProject );
            }
            pProject.initialize( pFactory );
            return pProject;
        }

        public Set<Project> getAllProjects()
        {
            return new HashSet<Project>( mProjectByName.values() );
        }
    }

    protected Runnable createRunnableFor( String pMethodName )
    {
        Runnable zRunnable = createRunnableFor( this, pMethodName );
        return (zRunnable != null) ? zRunnable : createRunnableFor( mLaunchProject, pMethodName );
    }

    protected Runnable createRunnableFor( final Object pObject, String pMethodName )
    {
        final Method zMethod = getMatchingMethod( pObject, pMethodName );
        return (zMethod == null) ? null : new Runnable()
        {
            @Override public void run()
            {
                try
                {
                    zMethod.invoke( pObject );
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( e );
                }
            }
        };
    }

    protected Method getMatchingMethod( Object pObject, String pMethodName )
    {
        List<Method> zFound = new ArrayList<Method>();
        Method[] zMethods = pObject.getClass().getMethods();
        for ( Method zMethod : zMethods )
        {
            if ( zMethod.getReturnType().equals( Void.TYPE ) && (zMethod.getParameterTypes().length == 0) )
            {
                if ( pMethodName.equals( zMethod.getName() ) )
                {
                    return zMethod;
                }
                if ( pMethodName.equalsIgnoreCase( zMethod.getName() ) )
                {
                    zFound.add( zMethod );
                }
            }
        }
        if ( zFound.size() == 0 )
        {
            return null;
        }
        if ( zFound.size() == 1 )
        {
            return zFound.get( 0 );
        }
        throw new IllegalArgumentException( "Multiple Methods " + zFound + " found on '" + pObject.getClass().getSimpleName() + "' than match: " + pMethodName );
    }

    protected void createLaunchProject()
    {
        mLaunchProject = project( CANONICAL_USER_DIR, mArgs.get( "file", "." ) );
    }

    protected int run()
    {
        if ( mArgs.count() == 0 )
        {
            mLaunchProject.build();
            return 0;
        }
        List<Runnable> zToExecute = getArgsBasedRunnables();
        for ( Runnable zRunnable : zToExecute )
        {
            zRunnable.run();
        }
        return 0;
    }

    private ArrayList<Runnable> getArgsBasedRunnables()
    {
        ArrayList<Runnable> zRunnables = new ArrayList<Runnable>();
        List<String> zUnrecognizedNames = new ArrayList<String>();
        for ( Arguments.NameValuePair zPair; null != (zPair = mArgs.getNext()); )
        {
            Runnable zRunnable = createRunnableFor( zPair.getName() );
            if ( zRunnable != null )
            {
                zRunnables.add( zRunnable );
            }
            else
            {
                zUnrecognizedNames.add( zPair.getName() );
            }
        }
        if ( !zUnrecognizedNames.isEmpty() )
        {
            System.err.println( "\nUnrecognized Command Line Args:" );
            for ( String zName : zUnrecognizedNames )
            {
                System.err.println( "   " + zName );
            }
            System.exit( 1 );
        }
        return zRunnables;
    }

    public static void main( String[] args )
            throws Exception
    {
        Arguments arguments = new Arguments( args );
        Scar scar = new Scar( arguments );
        scar.initLoggerFactory();
        scar.createLaunchProject();
        System.exit( scar.run() );
    }
}
