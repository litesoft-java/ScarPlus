package com.esotericsoftware.scar;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import javax.tools.*;

import org.litesoft.logger.*;

import com.esotericsoftware.filesystem.*;
import com.esotericsoftware.scar.support.*;
import com.esotericsoftware.utils.*;

/**
 * Generic Data structure that contains information needed to perform tasks.
 */
@SuppressWarnings("UnusedDeclaration")
public class Project extends ProjectParameters
{
    protected static final Logger LOGGER = LoggerFactory.getLogger( Project.class );

    private static final String META_INF_MANIFEST_MF = "META-INF/MANIFEST.MF";

    public Project( ProjectParameters pParameters )
    {
        super( pParameters );
        applyDefaults();
    }

    public String getTargetJavaVersion()
    {
        return "1.6";
    }

    public void set( Object key, Object object )
    {
        mManager.put( updatableKey( key ), object );
    }

    public void remove( Object key )
    {
        set( key, null );
    }

    /**
     * Removes an item from a list or map. If the mData under the specified key is a list, the entry equal to the specified value is
     * removed. If the mData under the specified key is a map, the entry with the key specified by value is removed.
     */
    public void remove( Object key, Object value )
    {
        mManager.remove( updatableKey( key ), value );
    }

    private Object updatableKey( Object pKey )
    {
        if ( pKey instanceof String )
        {
            String zStrKey = Util.noEmpty( pKey.toString().toLowerCase() );
            if ( Parameter.reservedNames().contains( zStrKey ) )
            {
                throw new IllegalArgumentException( zStrKey + " not updatable!" );
            }
            pKey = zStrKey;
        }
        Util.assertNotNull( "key", pKey );
        return pKey;
    }

    /**
     * Executes the buildDependencies, clean, compile, jar, and dist utility methods.
     */
    public synchronized boolean build()
    {
        if ( mBuilt )
        {
            return false;
        }
        mBuilt = true;
        if ( !buildDependencies() && !needToBuild() )
        {
            progress( "Build: " + this + " NOT Needed!" );
            return packageIt();
        }
        progress( "Build: " + this );
        return buildIt();
    }

    protected boolean needToBuild()
    {
        File zJarFile = getJarPathFile();
        if ( !zJarFile.isFile() )
        {
            return true;
        }
        long zJarTimestamp = zJarFile.lastModified();
        return checkNewer( zJarTimestamp, classpath() ) || checkNewer( zJarTimestamp, getSource() ) || checkNewer( zJarTimestamp, getResources() );
    }

    protected boolean checkNewer( long pJarTimestamp, Paths pPaths )
    {
        Long zLastModified = pPaths.getGreatestLastModified();
        return (zLastModified != null) && (zLastModified > pJarTimestamp);
    }

    protected boolean buildIt()
    {
        clean();
        compile();
        String zJarFile = jar();
        return packageIt() || (zJarFile != null);
    }

    protected boolean packageIt()
    {
        String zDistDir = dist();
        return (null != oneJAR()) || (zDistDir != null);
    }

    /**
     * Deletes the "target" directory and all files and directories under it.
     */
    public void clean()
    {
        progress( "Clean: " + this );
        delete( getAppDirPath() );
        delete( getJarPath() );
        delete( getTargetPath() );
    }

    /**
     * Collects the source files using the "source" property and compiles them into a "classes" directory under the target
     * directory. It uses "classpath" and "dependencies" to find the libraries required to compile the source.
     * <p/>
     * Note: Each dependency project is not built automatically. Each needs to be built before the dependent project.
     *
     * @return The path to the "classes" directory or null if there was no sources to compile
     */
    public String compile()
    {
        Paths source = getSource();
        if ( source.isEmpty() )
        {
            return null;
        }
        Paths classpath = classpath();

        String classesDir = mkdir( path( "$target$/classes/" ) );

        String zMessage = "Compile: " + this;
        if ( LOGGER.debug.isEnabled() )
        {
            zMessage += " | " + source.count() + " source files";
            if ( !classpath.isEmpty() )
            {
                zMessage += "\n         Classpath: " + classpath;
            }
        }
        progress( zMessage );

        List<String> zCompileArgs = createCompileJavaArgs( classpath, source, classesDir );

        compileJava( classpath, source, zCompileArgs );

        return classesDir;
    }

    protected List<String> createCompileJavaArgs( Paths pClasspath, Paths pSource, String pClassesDir )
    {
        List<String> args = new ArrayList<String>();
        if ( LOGGER.trace.isEnabled() )
        {
            args.add( "-verbose" );
        }
        args.add( "-d" );
        args.add( pClassesDir );
        args.add( "-g:source,lines" );
        args.add( "-target" );
        args.add( getTargetJavaVersion() );
        args.addAll( pSource.getFullPaths() );
        if ( !pClasspath.isEmpty() )
        {
            args.add( "-classpath" );
            args.add( pClasspath.toString( ";" ) );
        }
        return args;
    }

    protected void compileJava( Paths pClasspath, Paths pSource, List<String> pCompileArgs )
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        if ( compiler == null )
        {
            throw new RuntimeException( "No compiler available. Ensure you are running from a " + getTargetJavaVersion() + "+ JDK, and not a JRE *and* that your class path includes tools.jar." );
        }
        int zError = compiler.run( getCompile_in(), getCompile_out(), getCompile_err(), pCompileArgs.toArray( new String[pCompileArgs.size()] ) );
        if ( zError != 0 )
        {
            throw new RuntimeException( "Error (" + zError + ") during compilation of project: " + this + "\nSource: " + pSource.count() + " files\nClasspath: " + pClasspath );
        }
        try
        {
            Thread.sleep( 100 );
        }
        catch ( InterruptedException ex )
        {
            // Whatever
        }
    }

    protected InputStream getCompile_in()
    {
        return null;
    }

    protected OutputStream getCompile_out()
    {
        return null;
    }

    protected OutputStream getCompile_err()
    {
        return new OutputStream()
        {
            private StringBuilder mBuffer = new StringBuilder();
            private boolean mLastWasCRtreatedAsLF = false;

            private void dumpLine( int pByte )
            {
                if ( pByte == 13 )
                {
                    mLastWasCRtreatedAsLF = true;
                    pByte = 10;
                }
                else
                {
                    boolean zLastWasCRtreatedAsLF = mLastWasCRtreatedAsLF;
                    mLastWasCRtreatedAsLF = false;
                    if ( (pByte == 10) && zLastWasCRtreatedAsLF )
                    {
                        return;
                    }
                }
                mBuffer.append( (char) pByte ); // Assuming Ascii!
                String line = mBuffer.toString();
                mBuffer = new StringBuilder();
                if ( !line.startsWith( "Note: " ) )
                {
                    System.err.print( line );
                }
            }

            @Override
            public void write( int pByte ) // Not a Unicode character, just a BYTE!  --- Asumming Ascii ---
                    throws IOException
            {
                if ( (10 <= pByte) && (pByte <= 13) ) // New Line Indicator
                {                                        // LF: Line Feed, U+000A
                    dumpLine( pByte );                   // VT: Vertical Tab, U+000B
                    return;                              // FF: Form Feed, U+000C
                }                                        // CR: Carriage Return, U+000D
                mBuffer.append( (char) pByte ); // Assuming Ascii!
            }
        };
    }

    /**
     * Collects the class files from the "classes" directory and all the resource files using the "resources" property and encodes
     * them into a JAR file.
     * <p/>
     * If the resources don't contain a META-INF/MANIFEST.MF file, one is generated. If the project has a main property, the
     * generated manifest will include "Main-Class" and "Class-Path" entries to allow the main class to be run with "java -jar".
     *
     * @return The path to the created JAR file or null if No JAR created.
     */
    public String jar()
    {
        String zJarPath = getJarPath();

        Paths zClasses = new Paths( path( "$target$/classes/" ), "**.class" );
        Paths zResources = getResources();
        if ( zClasses.isEmpty() && zResources.isEmpty() )
        {
            delete( zJarPath );
            return null;
        }
        progress( "JAR: " + this + " -> " + zJarPath );

        String jarDir = mkdir( path( "$target$/jar/" ) );

        zClasses.copyTo( jarDir );
        zResources.copyTo( jarDir );

        File manifestFile = new File( jarDir, "META-INF/MANIFEST.MF" );
        if ( !manifestFile.exists() )
        {
            createDefaultManifestFile( zJarPath, manifestFile );
        }

        return jar( zJarPath, new Paths( jarDir ) );
    }

    protected void createDefaultManifestFile( String pJarFile, File pManifestFile )
    {
        LOGGER.debug.log( "Generating JAR manifest: ", pManifestFile );
        mkdir( pManifestFile.getParent() );
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue( Attributes.Name.MANIFEST_VERSION.toString(), "1.0" );
        if ( hasMain() )
        {
            LOGGER.debug.log( "Main class: ", getMain() );
            manifest.getMainAttributes().putValue( Attributes.Name.MAIN_CLASS.toString(), getMain() );
            StringBuilder buffer = new StringBuilder( 512 );
            buffer.append( Utils.fileName( pJarFile ) );
            buffer.append( " ." );
            Paths classpath = classpath();
            for ( String name : classpath.getRelativePaths() )
            {
                buffer.append( ' ' );
                buffer.append( name );
            }
            manifest.getMainAttributes().putValue( Attributes.Name.CLASS_PATH.toString(), buffer.toString() );
        }
        OutputStream output = createFileOutputStream( pManifestFile );
        try
        {
            manifest.write( output );
            Closeable zCloseable = output;
            output = null;
            close( zCloseable );
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        finally
        {
            dispose( output );
        }
    }

    /**
     * Collects the distribution files using the "dist" property, the project's JAR file, and everything on the project's classpath
     * (including dependency project classpaths) and places them into a "dist" directory under the "target" directory. This is also
     * done for depenency projects, recursively. This is everything the application needs to be run from JAR files.
     *
     * @return The path to the "dist" directory or null if no distribution (App Dir) is requested for this project.
     */
    public String dist()
    {
        String zAppDir = getAppDir();
        if ( zAppDir == null )
        {
            return null;
        }
        progress( "Dist: " + this );

        if ( !LOGGER.trace.isEnabled() )
        {
            return null; // todo: ...
        }

        String distDir = mkdir( path( "$target$/dist/" ) );
        classpath().copyTo( distDir );
        Paths distPaths = getDist();
        dependencyDistPaths( distPaths );
        distPaths.copyTo( distDir );
        new Paths( getTargetPath(), "*.jar" ).copyTo( distDir );
        return distDir;
    }

    private Paths dependencyDistPaths( Paths paths )
    {
        for ( String dependency : getDependencies() )
        {
            Project dependencyProject = null; // todo: project( null, path( dependency ) );
            String dependencyTarget = dependencyProject.getTargetPath();
            if ( !Utils.fileExists( dependencyTarget ) )
            {
                throw new RuntimeException( "Dependency has not been built: " + dependency );
            }
            paths.glob( dependencyTarget + "dist", "!*/**.jar" );
            paths.add( dependencyDistPaths( paths ) );
        }
        return paths;
    }

    /**
     * Encodes the specified paths into a JAR file.
     *
     * @return The path to the JAR file.
     */
    public String jar( String jarFile, Paths paths )
    {
        return innerJar( "", jarFile, paths );
    }

    /**
     * Encodes the specified paths into a JAR file.
     *
     * @return The path to the JAR file.
     */
    protected String innerJar( String pTypePrefix, String jarFile, Paths paths )
    {
        Util.assertNotNull( "jarFile", jarFile );
        Util.assertNotNull( "paths", paths );

        progress( "Creating " + pTypePrefix + "JAR (" + paths.count() + " entries): " + jarFile );

        int zZipped = paths.zip( jarFile, new ZipFactory()
        {
            @Override
            public ZipOutputStream createZOS( String pFilePath, List<FilePath> pPaths )
            {
                putManifestFirst( pPaths );
                try
                {
                    return new JarOutputStream( FileUtil.createBufferedFileOutputStream( pFilePath ) );
                }
                catch ( IOException e )
                {
                    throw new WrappedIOException( e );
                }
            }

            @Override
            public ZipEntry createZE( String pRelativePath )
            {
                return new JarEntry( pRelativePath );
            }

            private void putManifestFirst( List<FilePath> pPaths )
            {
                int at = findManifest( pPaths );
                if ( at > 0 )
                {
                    FilePath zManifest = pPaths.remove( at );
                    pPaths.add( 0, zManifest );
                }
            }

            private int findManifest( List<FilePath> pPaths )
            {
                for ( int i = 0; i < pPaths.size(); i++ )
                {
                    if ( META_INF_MANIFEST_MF.equals( pPaths.get( i ).getFileSubPath() ) )
                    {
                        return i;
                    }
                }
                return -1;
            }
        } );
        return zZipped == 0 ? null : jarFile;
    }

    /**
     * Unzips all JARs in the classpath and creates a single JAR containing those files and this Project's JAR (which MUST exist).
     * The manifest from the project's JAR is used. Putting everything into a single JAR makes it harder to see what libraries are
     * being used, but makes it easier for end users to distribute the application.
     * <p/>
     * Note: Files with the same path in different JARs will be overwritten. Files in the project's JAR will never be overwritten,
     * but may overwrite other files.
     *
     * @param pExcludeJARs The names of any JARs to exclude.
     *
     * @return The path to the "OneJAR" file or null if no OneJar is requested for this project.
     */
    public String oneJAR( String... pExcludeJARs )
    {
        File zOneJarPath = getOneJarPathFile();

        if ( zOneJarPath == null )
        {
            return null;
        }

        File zJarPath = getJarPathFile();
        if ( !zJarPath.isFile() )
        {
            progress( "One JAR: " + this + " BUT NO jar File produced at: " + zJarPath.getPath() );
            return null;
        }

        if ( zOneJarPath.isFile() && (zOneJarPath.lastModified() >= zJarPath.lastModified()) )
        {
            progress( "One JAR: " + this + " NOT Needed!" );
            return null;
        }

        Paths zClasspath = classpath();
        if ( zClasspath.isEmpty() )
        {
            progress( "One JAR: " + this + " No supporting Jars!  Simply Copying to: " + zOneJarPath.getPath() );
            copyFile( zJarPath, zOneJarPath );
            return zOneJarPath.getPath();
        }
        progress( "One JAR: " + this );

        File zOnejarDir = mkdir( new File( path( "$target$/onejar/" ) ) );

        List<String> zExcludeJARs = Arrays.asList( pExcludeJARs );
        for ( File jarFile : zClasspath.getFiles() ) // All our Class Path (dependant) JARS
        {
            if ( !zExcludeJARs.contains( jarFile.getName() ) )
            {
                unzip( jarFile, zOnejarDir );
            }
        }

        unzip( zJarPath, zOnejarDir ); // Our Jar! - Our Manifest will be "the" Manifest !!!!!! Need to remove class PATH!
        return innerJar( "'ONE' ", zOneJarPath.getPath(), new Paths( zOnejarDir.getPath() ) );
    }

    /**
     * Decodes the specified ZIP file.
     *
     * @return The path to the output directory.
     */
    protected void quiteUnzip( File zipFile, File outputDir )
    {
        ZipInputStream input = new ZipInputStream( createFileInputStream( zipFile ) );
        try
        {
            for ( ZipEntry entry; null != (entry = input.getNextEntry()); )
            {
                File file = new File( outputDir, entry.getName() );
                if ( entry.isDirectory() )
                {
                    mkdir( file.getPath() );
                    continue;
                }
                writeStream( input, createFileOutputStream( file ) );
            }
        }
        catch ( IOException e )
        {
            throw new WrappedIOException( e );
        }
        finally
        {
            dispose( input );
        }
    }

    /**
     * Decodes the specified ZIP file.
     */
    public void unzip( File zipFile, File outputDir )
    {
        Util.assertNotNull( "zipFile", zipFile );
        Util.assertNotNull( "outputDir", outputDir );
        progress( "ZIP decoding: " + zipFile.getPath() + " -> " + outputDir.getPath() );
        quiteUnzip( zipFile, outputDir );
    }

    /**
     * Decodes the specified ZIP file.
     *
     * @return The path to the output directory.
     */
    public String unzip( String zipFile, String outputDir )
    {
        zipFile = assertNotEmpty( "zipFile", zipFile );
        outputDir = assertNotEmpty( "outputDir", outputDir );
        progress( "ZIP decoding: " + zipFile + " -> " + outputDir );
        quiteUnzip( new File( zipFile ), new File( outputDir ) );
        return outputDir;
    }

    /**
     * Computes the classpath for the specified project and all its dependency projects, recursively.
     */
    protected Paths classpath()
    {
        Paths classpath = getClasspath();
        for ( Project zProject : mDependantProjects )
        {
            zProject.addDependantProjectsClassPaths( classpath );
        }
        return classpath;
    }

    /**
     * Computes the classpath for all the dependencies of the specified project, recursively.
     */
    protected void addDependantProjectsClassPaths( Paths pPathsToAddTo )
    {
        File zJarFile = getJarPathFile();
        if ( !zJarFile.isFile() )
        {
            throw new RuntimeException( "Dependency (" + this + ") Jar not found, not built?" );
        }
        pPathsToAddTo.add( new FilePath( zJarFile.getParentFile(), zJarFile.getName() ) );
        pPathsToAddTo.add( classpath() );
    }

    /**
     * Calls {@link #build(Project)} for each dependency project in the specified project.
     */
    public boolean buildDependencies()
    {
        boolean anyBuilt = false;
        for ( Project zProject : mDependantProjects )
        {
            anyBuilt |= zProject.build();
        }
        return anyBuilt;
    }

    /**
     * Replaces the Data in this project with the contents of the specified YAML file. If the specified project has Data with the
     * same key as this project, the value is overwritten. Keys in this project that are not in the specified project are not
     * affected.
     */
//    public void replace( Project project )
//    {
//        Util.assertNotNull( "project", project );
//        mData.putAll( project.mData );
//        document = project.document;
//        dir = project.dir;
//    }
    public synchronized void initialize( ProjectFactory pProjectFactory )
    {
        List<String> zDependencies = getDependencies();
        if ( zDependencies != null )
        {
            for ( String zDependency : zDependencies )
            {
                mDependantProjects.add( pProjectFactory.project( mCanonicalProjectDir, zDependency ) );
            }
        }

//        Project defaults = new Project();
//
//        File file = new File( canonical( pPath ) );
//        if ( file.isDirectory() )
//        {
//            String name = file.getName();
//            defaults.set( "name", name );
//            defaults.set( "target", file.getParent() + "/target/" + name + "/" );
//        }
//        else
//        {
//            String name = file.getParentFile().getName();
//            defaults.set( "name", name );
//            defaults.set( "target", file.getParentFile().getParent() + "/target/" + name + "/" );
//        }
//        defaults.set( "classpath", "lib|**/*.jar" );
//        defaults.set( "dist", "dist" );
//
//        List<String> source = new ArrayList<String>();
//        source.add( "src|**/*.java" );
//        source.add( "src/main/java|**/*.java" );
//        defaults.set( "source", source );
//
//        List<String> resources = new ArrayList<String>();
//        resources.add( "resources" );
//        resources.add( "src/main/resources" );
//        defaults.set( "resources", resources );
//
//        Project project = project( pPath, defaults );
//
//        // Remove dependency if a JAR of the same name is on the classpath.
//        Paths classpath = project.getPaths( "classpath" );
//        classpath.add( dependencyClasspaths( project, classpath, false, false ) );
//        for ( String dependency : project.getDependencies() )
//        {
//            String dependencyName = project( project.path( dependency ) ).getName();
//            for ( String classpathFile : classpath )
//            {
//                String name = fileWithoutExtension( classpathFile );
//                int dashIndex = name.lastIndexOf( '-' );
//                if ( dashIndex != -1 )
//                {
//                    name = name.substring( 0, dashIndex );
//                }
//                if ( name.equals( dependencyName ) )
//                {
//                    if ( DEBUG )
//                    {
//                        debug( "Ignoring " + project + " dependency: " + dependencyName + " (already on classpath: " + classpathFile + ")" );
//                    }
//                    project.remove( "dependencies", dependency );
//                    break;
//                }
//            }
//        }
//
//        if ( TRACE )
//        {
//            trace( "scar", "Project: " + project + "\n" + project );
//        }
//
//        return project;
    }

    protected boolean mBuilt = false;
    protected List<Project> mDependantProjects = new ArrayList<Project>();
}
