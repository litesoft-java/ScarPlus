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

    private static final String VERSIONED_URL_PATTERN_PREFIX = "<url-pattern>/v";
    private static final String VERSIONED_SCRIPT_PREFIX = "<script src='v";
    private static final String VERSIONED_SCRIPT_SUFFIX = ".nocache.js'></script>";
    private static final String VERSIONED_MODULE_PREFIX = "<module rename-to=\"v";
    private static final String VERSIONED_MODULE_SUFFIX = "\">";
    private static final String JAVA_HOME = "JAVA_HOME";

    public Project( ProjectParameters pParameters )
    {
        super( pParameters.validate() );
        applyDefaults();
    }

    public String getTargetJavaVersion()
    {
        return "1.6";
    }

    protected void packageClean()
    {
        delete( getAppDirPath() );
        delete( getOneJarPath() );
        delete( getWarPath() );
    }

    protected boolean packageIt()
    {
        return appDir() | oneJAR() | war(); // Note: SINGLE '|' ORs to force full execution!
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
     * @return True if the "OneJAR" was created / updated or false if no OneJar is NOT requested for this project or it was not needed.
     */
    public boolean oneJAR( String... pExcludeJARs )
    {
        File zOneJarPath = getOneJarPathFile();

        if ( zOneJarPath == null )
        {
            return false;
        }

        File zJarPath = getJarPathFile();
        if ( !zJarPath.isFile() )
        {
            throw new IllegalStateException( "One JAR: " + this + " requested, BUT NO jar File produced at: " + zJarPath.getPath() );
        }

        if ( zOneJarPath.isFile() && (zOneJarPath.lastModified() >= zJarPath.lastModified()) )
        {
            progress( "One JAR: " + this + " NOT Needed!" );
            return false;
        }

        Paths zClasspath = classpath();
        if ( zClasspath.isEmpty() )
        {
            progress( "One JAR: " + this + " No supporting Jars!  Simply Copying to: " + zOneJarPath.getPath() );
            copyFile( zJarPath, zOneJarPath );
            return true;
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
        innerJar( "'ONE' JAR", zOneJarPath.getPath(), new Paths( zOnejarDir.getPath() ) );
        return true;
    }

    /**
     * Collects the distribution files using the "dist" property, the project's JAR file, and everything on the project's classpath
     * (including dependency project classpaths) and places them into the specified directory. This is also done for depenency projects,
     * recursively. This is everything the application needs to be run from JAR files.
     *
     * @return True if the AppDir was populated or false if no distribution (App Dir) is requested for this project.
     */
    public boolean appDir()
    {
        String zAppDir = getAppDirPath();
        if ( zAppDir == null )
        {
            return false;
        }
        File zJarPath = getJarPathFile();
        if ( !zJarPath.isFile() )
        {
            progress( "AppDir: " + this + " BUT NO jar File produced at: " + zJarPath.getPath() );
            return false;
        }
        Paths zPaths = new Paths();
        addDependantProjectsDistPaths( zPaths );
        zPaths.add( classpath() );
        zPaths.add( FilePath.canonicalize( getJarPathFile() ) );

        File zAppDirFile = new File( zAppDir );
        if ( zAppDirFile.exists() )
        {
            if ( zAppDirFile.lastModified() >= zPaths.getGreatestLastModified() )
            {
                progress( "AppDir: " + this + " NOT Needed!" );
                return false;
            }
            delete( zAppDirFile );
        }

        progress( "AppDir: " + this + " -> " + zAppDir );
        String distDir = mkdir( zAppDir ); // Give it a new Timestamp
        zPaths.copyTo( distDir );
        return true;
    }

    /**
     * Produce either a 'war' directory or a '.war' file, in theory ready to deploy to a servlet/web container.
     *
     * @return true if the 'war' was created.
     */
    public boolean war()
    {
        String zWar = getWar();
        if ( zWar == null )
        {
            return false;
        }
        File zJarPath = getJarPathFile();
        if ( !zJarPath.isFile() )
        {
            progress( "WAR: " + this + " BUT NO jar File produced at: " + zJarPath.getPath() );
            return false;
        }

        Paths zDistPaths = new Paths();
        addDependantProjectsDistPaths( zDistPaths );
        if ( null != getGWT() )
        {
            zDistPaths.add( new Paths( getGWTwarPath() ) );
        }

        Paths zClassPath = new Paths();
        zClassPath.add( FilePath.canonical( getGWTatDir(), GWT_SERVLET ) );
        zClassPath.add( classpath() );
        zClassPath.add( FilePath.canonicalize( getJarPathFile() ) );

        File zWarPathFile = getWarPathFile();
        if ( zWarPathFile.exists() )
        {
            if ( zWarPathFile.lastModified() >= zClassPath.getGreatestLastModified() )
            {
                progress( "WAR: " + this + " NOT Needed!" );
                return false;
            }
            delete( zWarPathFile );
        }

        boolean zWarIt = zWar.endsWith( ".war" );

        File zWarDir = zWarPathFile;
        if ( zWarIt )
        {
            zWarDir = new File( path( "$target$/war/" ) );
            delete( zWarDir );
        }

        progress( "WAR: " + this + " -> " + zWarDir.getPath() );

        File zWarDirLibPath = new File( zWarDir, "WEB-INF/lib" );
        mkdir( zWarDirLibPath );

        zDistPaths.copyTo( zWarDir.getPath() );
        zClassPath.copyTo( zWarDirLibPath.getPath() );

        if ( zWarIt )
        {
            innerJar( "WAR", zWarPathFile.getPath(), new Paths( zWarDir.getPath() ) );
        }
        return true;
    }

    /**
     * Computes the classpath for all the dependencies of the specified project, recursively.
     */
    protected void addDependantProjectsDistPaths( Paths pPathsToAddTo )
    {
        for ( Project zProject : mDependantProjects )
        {
            zProject.addDependantProjectsDistPaths( pPathsToAddTo );
        }
        pPathsToAddTo.add( getDist() );
    }

    protected boolean GWTcompileIt()
    {
        String[] args = // 
                { //
                  getPathJavaJRE(), //
                  "-Xmx" + getGWTmx(), //
                  "-cp", //
                  buildGWTcompileClassPath(), //
                  "com.google.gwt.dev.Compiler", //
                  "-logLevel", //
                  getGWTlogging(), //
                  "-war", //
                  getGWTwarPath(), //
                  "-style", //
                  getGWTstyle(), //
                  getGWT() //
                };

        Utils.shell( args );
        return true;
    }

    private String buildGWTcompileClassPath()
    {
        Paths zGWTclassPath = new Paths();
        File zGWTatDir = getGWTatDir();
        zGWTclassPath.add( FilePath.canonical( zGWTatDir, GWT_DEV ) );
        zGWTclassPath.add( FilePath.canonical( zGWTatDir, GWT_USER ) );
        zGWTclassPath.add( FilePath.canonicalize( getJarPathFile() ) );
        zGWTclassPath.add( classpath() );
        return zGWTclassPath.toString( File.pathSeparator );
    }

    protected String getPathJavaJRE()
    {
        File zJavaHomeDir = assertIsDirectory( JAVA_HOME, new File( assertNotEmpty( JAVA_HOME, System.getenv( JAVA_HOME ) ) ) );
        File zJavaDir = new File( zJavaHomeDir, "jre/bin" );
        if ( !zJavaDir.isDirectory() )
        {
            if ( !(zJavaDir = new File( zJavaHomeDir, "bin" )).isDirectory() )
            {
                throw new IllegalStateException( "Unable to find JAVA_HOME bin directory under: " + zJavaHomeDir );
            }
        }
        File zJavaExecutable = new File( zJavaDir, isWindows ? "java.exe" : "java" );
        if ( zJavaExecutable.isFile() )
        {
            return getCanonicalFile( zJavaExecutable ).getPath();
        }
        throw new IllegalStateException( "Unable to find JAVA_HOME based executable at: " + zJavaExecutable );
    }

    protected File getGeneratedGWT_nocache_jsFile()
    {
        String zGWTxmlRelativeFilePath = getGWT().replace( '.', '/' ) + ".gwt.xml"; // e.g. org.litesoft.sandbox.csapp.CSapp
        RootedPaths[] zRootedPaths = getSource().getRootedPaths();
        for ( RootedPaths zPath : zRootedPaths )
        {
            File zFile = new File( zPath.getCanonicalRootDirectory(), zGWTxmlRelativeFilePath );
            if ( zFile.isFile() )
            {
                String moduleName = extractModuleNameFrom( getGWT(), fileContents( zFile ) );
                return new File( getGWTwarPath(), moduleName + "/" + moduleName + ".nocache.js" );
            }
        }
        throw new IllegalArgumentException( "Unable to locate GWT module file: " + getGWT() );
    }

    private String extractModuleNameFrom( String pGWTmoduleReference, String pGWTmoduleFileContents )
    {
        int at = pGWTmoduleFileContents.indexOf( " rename-to" );
        if ( at != -1 ) // . . . . . . . . . . . .01234567890
        {
            int upTo = pGWTmoduleFileContents.indexOf( '>', at += 10 );
            if ( upTo != -1 )
            {
                String stuff = pGWTmoduleFileContents.substring( at, upTo ).trim();
                if ( stuff.startsWith( "=" ) )
                {
                    if ( (stuff = stuff.substring( 1 ).trim()).length() > 2 )
                    {
                        char c = stuff.charAt( 0 );
                        if ( (c == '"') || (c == '\'') )
                        {
                            if ( -1 != (at = stuff.indexOf( c, 1 )) )
                            {
                                return stuff.substring( 1, at );
                            }
                        }
                    }
                }
            }
        }
        String s = "." + pGWTmoduleReference;
        return s.substring( s.lastIndexOf( '.' ) + 1 );
    }

    protected boolean needToCompileGWT()
    {
        File zJarFile = getJarPathFile();
        if ( !zJarFile.isFile() )
        {
            throw new IllegalStateException( this + " Jar Not Built?" );
        }
        long zJarTimestamp = zJarFile.lastModified();
        File zGeneratedGWT_nocache_jsFile = getGeneratedGWT_nocache_jsFile();
        return (zGeneratedGWT_nocache_jsFile == null) || !zGeneratedGWT_nocache_jsFile.isFile() || (zGeneratedGWT_nocache_jsFile.lastModified() < zJarTimestamp);
    }

    public boolean GWTcompile()
    {
        String zGWT = getGWT();
        if ( zGWT == null )
        {
            return false;
        }
        if ( !needToCompileGWT() )
        {
            progress( "GWT Compile: " + this + " NOT Needed!" );
            return false;
        }
        progress( "GWT Compile: " + this );
        return GWTcompileIt();
    }

    /**
     * Assert that this project is currently a 'Versioned' GWT project, and then rev the version number by 1
     */
    public void versionGWT()
    {
        File zWarWebXmlFile = new File( mCanonicalProjectDir, "war/WEB-INF/web.xml" );
        String zWarWebXml = fileContents( assertIsFile( "web.xml", zWarWebXmlFile ) );

        int zCurVersion = extractVersionFromUrlPattern( zWarWebXml );

        String zWarResourceRelativePathCurrent = "warResources/v" + zCurVersion;

        File zIndexHtmlFile = new File( mCanonicalProjectDir, zWarResourceRelativePathCurrent + "/index.html" );
        String zIndexHtml = assertVersionedIndexHtml( zIndexHtmlFile, zCurVersion );

        List<File> zVersionedGwtXmlFiles = findVersionedGwtXmlFiles( zCurVersion );

        int zNewVersion = zCurVersion + 1;

        String zWarResourceRelativePathNew = "warResources/v" + zNewVersion;
        if ( new File( mCanonicalProjectDir, zWarResourceRelativePathNew ).exists() )
        {
            throw new IllegalStateException( "Project already contains a 'warResources/v" + zNewVersion + "' directory?" );
        }

        progress( "versionGWT: " + this + " | " + zCurVersion + " -> " + (zCurVersion + 1) );
        progress( "    " + zWarWebXmlFile.getPath() );
        progress( "    " + zIndexHtmlFile.getPath() );
        for ( File zFile : zVersionedGwtXmlFiles )
        {
            progress( "    " + zFile.getPath() );
        }
        progress( "    " + zWarResourceRelativePathCurrent + " -> " + zWarResourceRelativePathNew );

        new Paths( zWarResourceRelativePathCurrent ).copyTo( zWarResourceRelativePathNew );

        updateFileContents( new File( mCanonicalProjectDir, zWarResourceRelativePathNew + "/index.html" ), updateVersionedIndexHtml( zIndexHtml, zCurVersion, zNewVersion ) );

        updateFileContents( zWarWebXmlFile, updateVersionedWebXml( zWarWebXml, zCurVersion, zNewVersion ) );

        for ( File zFile : zVersionedGwtXmlFiles )
        {
            updateFileContents( zFile, updateVersionedGwtXmlFile( fileContents( zFile ), zCurVersion, zNewVersion ) );
        }
        // Update/Create the Current Version's redirect JavaScript file
        String zCurPathVersion = "/v" + zCurVersion;
        String redirectScript = "var loc = window.location.href;\n" + //
                                "var at = loc.indexOf( '" + zCurPathVersion + "' );\n" + //
                                "window.location.href = loc.substring(0, at) + '/v" + zNewVersion + "' + loc.substring(at + " + zCurPathVersion.length() + ");\n";
        updateFileContents( new File( mCanonicalProjectDir, zWarResourceRelativePathCurrent + "/v" + zCurVersion + ".nocache.js" ), redirectScript );

        // Update the "root" html (if it exists)
        File zRootHtmlFile = new File( mCanonicalProjectDir, "warResources/index.html" );
        if ( zRootHtmlFile.isFile() )
        {
            updateFileContents( zRootHtmlFile, updateRootHTML( fileContents( zRootHtmlFile ), zCurVersion, zNewVersion ) );
        }
    }

    protected String updateRootHTML( String pFileContents, int pCurVersion, int pNewVersion )
    {
        // <!DOCTYPE html>
        // <html>
        //     <head>
        //         <meta http-equiv="Refresh" content="1; url=v1/">
        //     </head>
        //     <body>
        //         <script>window.location.href = 'v1/';</script>
        //     </body>
        // </html>
        String zCurPathVersion = "v" + pCurVersion + "/";
        String zNewPathVersion = "v" + pNewVersion + "/";
        for ( int at; -1 != (at = pFileContents.indexOf( zCurPathVersion )); )
        {
            pFileContents = pFileContents.substring( 0, at ) + zNewPathVersion + pFileContents.substring( at + zCurPathVersion.length() );
        }
        return pFileContents;
    }

    protected String updateVersionedGwtXmlFile( String pFileContents, int pCurVersion, int pNewVersion )
    {
        String zCurVersionedModule = VERSIONED_MODULE_PREFIX + pCurVersion + VERSIONED_MODULE_SUFFIX;
        String zNewVersionedModule = VERSIONED_MODULE_PREFIX + pNewVersion + VERSIONED_MODULE_SUFFIX;
        int at = pFileContents.indexOf( zCurVersionedModule );
        return pFileContents.substring( 0, at ) + zNewVersionedModule + pFileContents.substring( at + zCurVersionedModule.length() );
    }

    protected List<File> findVersionedGwtXmlFiles( int pVersion )
    {
        String zVersionedModule = VERSIONED_MODULE_PREFIX + pVersion + VERSIONED_MODULE_SUFFIX;

        ArrayList<File> zVersionedFiles = new ArrayList<File>();

        String zSourceString = get( SOURCE.getName() ) + "|";
        Paths zGwtXml = new Paths( zSourceString.substring( 0, zSourceString.indexOf( '|' ) ), "**.gwt.xml" );
        for ( File zFile : zGwtXml.getFiles() )
        {
            if ( fileContents( zFile ).contains( zVersionedModule ) )
            {
                zVersionedFiles.add( zFile );
            }
        }
        if ( zVersionedFiles.isEmpty() )
        {
            throw new IllegalStateException( "Project does not appear to contain a 'gwt.xml' file with the current version module definition of: " + zVersionedModule );
        }
        return zVersionedFiles;
    }

    protected String updateVersionedIndexHtml( String pFileContents, int pCurVersion, int pNewVersion )
    {
        String zCurVersionedScript = VERSIONED_SCRIPT_PREFIX + pCurVersion + VERSIONED_SCRIPT_SUFFIX;
        String zNewVersionedScript = VERSIONED_SCRIPT_PREFIX + pNewVersion + VERSIONED_SCRIPT_SUFFIX;
        int at = pFileContents.indexOf( zCurVersionedScript );
        return pFileContents.substring( 0, at ) + zNewVersionedScript + pFileContents.substring( at + zCurVersionedScript.length() );
    }

    protected String assertVersionedIndexHtml( File pIndexHtmlFile, int pVersion )
    {
        String zVersionedScript = VERSIONED_SCRIPT_PREFIX + pVersion + VERSIONED_SCRIPT_SUFFIX;

        String zContents = fileContents( assertIsFile( "Versioned index.html", pIndexHtmlFile ) );
        if ( !zContents.contains( zVersionedScript ) )
        {
            throw new IllegalStateException( "Project's current versioned index.html file (" + pIndexHtmlFile.getPath() + ") does not contain a 'versioned' script element of: " + zVersionedScript );
        }
        return zContents;
    }

    protected String updateVersionedWebXml( String pFileContents, int pCurVersion, int pNewVersion )
    {
        String zCurVersionedUrlPattern = VERSIONED_URL_PATTERN_PREFIX + pCurVersion + "/";
        String zNewVersionedUrlPattern = VERSIONED_URL_PATTERN_PREFIX + pNewVersion + "/";
        for ( int at; -1 != (at = pFileContents.indexOf( zCurVersionedUrlPattern )); )
        {
            pFileContents = pFileContents.substring( 0, at ) + zNewVersionedUrlPattern + pFileContents.substring( at + zCurVersionedUrlPattern.length() );
        }
        return pFileContents;
    }

    protected int extractVersionFromUrlPattern( String pWarWebXml )
    {
        for ( int at, from = 0; -1 != (at = pWarWebXml.indexOf( VERSIONED_URL_PATTERN_PREFIX, from )); from = at + 1 )
        {
            int slashAt = pWarWebXml.indexOf( '/', at += VERSIONED_URL_PATTERN_PREFIX.length() );
            if ( slashAt > 0 )
            {
                try
                {
                    return Integer.parseInt( pWarWebXml.substring( at, slashAt ) );
                }
                catch ( NumberFormatException acceptable )
                {
                    // path starts w/ a 'v' but is not of pattern "v####"
                }
            }
        }
        throw new IllegalStateException( "Project's war/WEB-INF/web.xml does not appear to contain a 'versioned' <url-pattern>." );
    }

    /**
     * Executes the buildDependencies, clean, compile, jar, [GWTcompile], and then "packageIt" utility methods.
     */
    public synchronized boolean build()
    {
        if ( mBuilt )
        {
            return false;
        }
        mBuilt = true;
        boolean zAnythingBuilt = false;
        if ( !buildDependencies() && !needToBuild() )
        {
            progress( "Build: " + this + " NOT Needed!" );
        }
        else
        {
            progress( "Build: " + this );
            clean();
            compile();
            jar();
            zAnythingBuilt = true;
        }
        zAnythingBuilt |= GWTcompile();
        zAnythingBuilt |= packageIt();
        return zAnythingBuilt;
    }

    protected boolean needToBuild()
    {
        File zJarFile = getJarPathFile();
        if ( !zJarFile.isFile() )
        {
            return true;
        }
        long zJarTimestamp = zJarFile.lastModified();
        return (mProjectFileLastModified > zJarTimestamp) || //
               checkNewer( zJarTimestamp, compileClasspath() ) || //
               checkNewer( zJarTimestamp, getSource() ) || //
               checkNewer( zJarTimestamp, getResources() );
    }

    protected boolean checkNewer( long pJarTimestamp, Paths pPaths )
    {
        Long zLastModified = pPaths.getGreatestLastModified();
        return (zLastModified != null) && (zLastModified > pJarTimestamp);
    }

    /**
     * Deletes the "target" directory and all files and directories under it.
     */
    public void clean()
    {
        progress( "Clean: " + this );
        packageClean();
        delete( getGWTwarPath() );
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
        Paths classpath = compileClasspath();

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
            args.add( pClasspath.toString( File.pathSeparator ) );
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
     * Encodes the specified paths into a JAR file.
     *
     * @return The path to the JAR file.
     */
    public String jar( String jarFile, Paths paths )
    {
        return innerJar( "JAR", jarFile, paths );
    }

    /**
     * Encodes the specified paths into a JAR/WAR file.
     *
     * @return The path to the JAR/WAR file.
     */
    protected String innerJar( String pType, String jarFile, Paths paths )
    {
        Util.assertNotNull( "jarFile", jarFile );
        Util.assertNotNull( "paths", paths );

        progress( "Creating " + pType + " (" + paths.count() + " entries): " + jarFile );

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
    protected Paths compileClasspath()
    {
        Paths classpath = getCompileClasspath();
        classpath.add( getClasspath() );
        for ( Project zProject : mDependantProjects )
        {
            zProject.addDependantProjectsCompileClassPaths( classpath );
        }
        return classpath;
    }

    /**
     * Computes the classpath for all the dependencies of the specified project, recursively.
     */
    protected void addDependantProjectsCompileClassPaths( Paths pPathsToAddTo )
    {
        addDependentProjectJar( pPathsToAddTo );
        pPathsToAddTo.add( compileClasspath() );
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
        addDependentProjectJar( pPathsToAddTo );
        pPathsToAddTo.add( classpath() );
    }

    protected void addDependentProjectJar( Paths pPathsToAddTo )
    {
        File zJarFile = getJarPathFile();
        if ( !zJarFile.isFile() )
        {
            throw new RuntimeException( "Dependency (" + this + ") Jar not found, not built?" );
        }
        pPathsToAddTo.add( new FilePath( zJarFile.getParentFile(), zJarFile.getName() ) );
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
            String zStrKey = noEmpty( pKey.toString().toLowerCase() );
            if ( Parameter.reservedNames().contains( zStrKey ) )
            {
                throw new IllegalArgumentException( zStrKey + " not updatable!" );
            }
            pKey = zStrKey;
        }
        Util.assertNotNull( "key", pKey );
        return pKey;
    }

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
