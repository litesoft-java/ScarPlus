package com.esotericsoftware.scar;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.esotericsoftware.filesystem.*;
import com.esotericsoftware.scar.support.*;
import com.esotericsoftware.utils.*;

import static com.esotericsoftware.scar.support.Parameter.*;

@SuppressWarnings({"UnusedDeclaration"})
public class ProjectParameters extends FileUtil
{
    public static final Parameter NAME = def( "name", Form.STRING, "The name of the project. Used to name the JAR.", //
                                              "Default:\n" + //
                                              "  The name of the directory containing the project YAML file, or\n" + //
                                              "  the name of the YAML file if it is not 'build'." );

    public static final Parameter VERSION = def( "version", Form.STRING, "The version of the project. If available, used to name the 'default' named JAR." );

    public static final Parameter MAIN = def( "main", Form.STRING, "Name of the main class." );

    public static final Parameter TARGET = def( "target", Form.STRING, "The directory to output build artifacts.", //
                                                "Default: The directory containing the project YAML file, plus 'build'." );

    public static final Parameter DEPENDENCIES = def( "dependencies", Form.STRING_LIST, "Relative or absolute path(s) to dependency project directories or YAML files." );

    public static final Parameter COMPILECLASSPATH = def( "compileclasspath", Form.PATHS, "Wildcard patterns for the file(s) to include on the 'compile' classpath.", //
                                                          "Note: automatically includes the ClassPath(s)." );

    public static final Parameter CLASSPATH = def( "classpath", Form.PATHS, "Wildcard patterns for the file(s) to include on the classpath (both, compile & runtime/deployment).", //
                                                   "Default: 'lib|**.jar'." );

    public static final Parameter SOURCE = def( "source", Form.PATHS, "Wildcard patterns for the Java file(s) to compile.", //
                                                "Default: 'src|**.java' or 'src/main/java|**.java'." );

    public static final Parameter RESOURCES = def( "resources", Form.PATHS, "Wildcard patterns for the file(s) to include in the JAR.", //
                                                   "Default: 'resources' or 'src/main/resources'." );

    public static final Parameter JAR = def( "jar", Form.STRING, "JAR name w/ optional path for the JAR ('.jar' added to the end if does not end with 'jar', case insensitive).", //
                                             "Default: '$target$[-$version$]'." );

    public static final Parameter DIST = def( "dist", Form.PATHS, "Wildcard patterns for the file(s) to include in the distribution, outside the JAR.", //
                                              "Default: 'war' (if there)." );

    // ------------------------------- Packaging Options, these are mutually exclusive ---------------------------------

    public static final Parameter APPDIR = def( "appdir", Form.STRING, "Directory path to bring together all files (both JARs and 'dist', for this project " + "and all dependencies, recursively) the application needs to be run from JAR files." );

    public static final Parameter ONEJAR = def( "onejar", Form.STRING, "JAR name w/ optional path for the JAR ('.jar' added to the end if does not end with 'jar', case insensitive), that all 'exploded' dependendend JARs and dist files will be JAR'd into (this should make a single JAR application)." );

    public static final Parameter WAR = def( "war", Form.STRING, "Target WAR directory or WAR name (if ends w/ '.war'), produces a war directory (as specified, or a default one if a '.war' is requested) then if a '.war' is requested, packages the war directory into the specified '.war' file." );

    // ------------------------------------------------ GWT Parameters -------------------------------------------------

    public static final Parameter GWT = def( "GWT", Form.STRING, "The 'package' Name of the 'root' '.gwt.xml' file. e.g. 'org.sample.MyGwtApplication'" );

    public static final Parameter GWTat = def( "GWTat", Form.STRING, "The directory to find the GWT JARs.  Required if 'GWT' indicated" );

    public static final Parameter GWTwar = def( "GWTwar", Form.STRING, "The directory to put the GWT Compiler's output in.", //
                                                "Default: '$target$/GWTCompilerOutput'." );

    public static final Parameter GWTstyle = def( "GWTstyle", Form.STRING, "GWT Compiler's output 'style'.  Options are: OBF, PRETTY, or DETAILED.  Note: OBF == Obfuscated.", //
                                                  "Default: 'OBF'." );

    public static final Parameter GWTlogging = def( "GWTlogging", Form.STRING, "Logging level for the GWT Compiler.  Options are: ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL.", //
                                                    "Default: 'INFO'." );

    public static final Parameter GWTmx = def( "GWTmx", Form.STRING, "the -Xmx value for the GWT Compiler.", //
                                               "Default: '128m'." );

    public static final String GWT_DEV = "gwt-dev.jar";
    public static final String GWT_USER = "gwt-user.jar";
    public static final String GWT_SERVLET = "gwt-servlet.jar";

    private static final String[] GWT_JARS = {GWT_DEV, GWT_USER, GWT_SERVLET};

    // -------------------------------------------------- Validation ---------------------------------------------------

    public ProjectParameters validate()
    {
        int zHas = intForNotNull( 1, getAppDir() ) + //
                   intForNotNull( 2, getOneJar() ) + //
                   intForNotNull( 4, getWar() );
        switch ( zHas )
        {
            default:
            case 1:
            case 2:
            case 4:
                break; // Happy Cases
            case 3:
                throw new IllegalArgumentException( "May not specify both: AppDir & OneJAR" );
            case 5:
                throw new IllegalArgumentException( "May not specify both: AppDir & WAR" );
            case 6:
                throw new IllegalArgumentException( "May not specify both: OneJAR & WAR" );
            case 7:
                throw new IllegalArgumentException( "May only specify one of: AppDir, OneJAR, or WAR" );
        }
        return this;
    }

    // ------------------------------------------------ Default Support ------------------------------------------------

    protected synchronized void applyDefaults()
    {
        defaultTARGET();
        defaultCLASSPATH();
        defaultSOURCE();
        defaultRESOURCES();
        defaultJAR();
        defaultDIST();
        defaultGWT();
        defaultOneJar();
    }

    protected void defaultOneJar()
    {
        enforceJarExtension( ONEJAR.getName() );
    }

    protected void defaultDIST()
    {
        defaultSubDirOptional( DIST, "war" );
    }

    protected void defaultJAR()
    {
        if ( null == enforceJarExtension( JAR.getName() ) )
        {
            mManager.put( JAR.getName(), "$target$/$name$" + (hasVersion() ? "-$version$" : "") + ".jar" );
        }
    }

    protected void defaultTARGET()
    {
        defaultKey( TARGET, "build" );
    }

    protected void defaultCLASSPATH()
    {
        defaultSubDirOptional( CLASSPATH, "lib|**.jar" );
    }

    protected void defaultRESOURCES()
    {
        defaultSubDirOptional( RESOURCES, "src/main/resources", "resources" );
    }

    protected void defaultSOURCE()
    {
        defaultSubDirOptional( SOURCE, "src/main/java|**.java", "src|**.java" );
    }

    protected void defaultGWT()
    {
        String zGWT = getGWT();
        if ( null != zGWT )
        {
            verifyGWTlibs( getGWTatDir() );
            defaultKey( GWTwar, "$target$/GWTCompilerOutput" );
            defaultKey( GWTstyle, "OBF" );
            defaultKey( GWTlogging, "INFO" );
            defaultKey( GWTmx, "512m" );
        }
    }

    protected void verifyGWTlibs( File pGWTdir )
    {
        if ( pGWTdir == null )
        {
            throw new IllegalStateException( "GWT specified, but NO GWTat!" );
        }
        if ( !pGWTdir.isDirectory() )
        {
            throw new IllegalStateException( "GWTat '" + pGWTdir + "' is not a directory!" );
        }
        for ( String zGwtJar : GWT_JARS )
        {
            if ( !new File( pGWTdir, zGwtJar ).isFile() )
            {
                throw new IllegalStateException( "GWTat '" + pGWTdir.getPath() + "' did not contain: " + zGwtJar );
            }
        }
    }

    protected String enforceJarExtension( String pEntryName )
    {
        String zJar = get( pEntryName );
        if ( null != zJar )
        {
            int at = zJar.lastIndexOf( '.' );
            if ( at == -1 || !".jar".equalsIgnoreCase( zJar.substring( at ) ) )
            {
                mManager.put( pEntryName, zJar += ".jar" );
            }
        }
        return zJar;
    }

    // ------------------------------------------- Special Property Accessors ------------------------------------------

    public boolean hasVersion()
    {
        return (null != getVersion());
    }

    public String getVersion()
    {
        return get( VERSION.getName() );
    }

    public boolean hasMain()
    {
        return (null != getMain());
    }

    public String getMain()
    {
        return get( MAIN.getName() );
    }

    public String getTarget()
    {
        return get( TARGET.getName() );
    }

    public String getTargetPath()
    {
        return getPath( TARGET.getName() );
    }

    public List<String> getDependencies()
    {
        return getListNotNull( DEPENDENCIES.getName() );
    }

    public Paths getCompileClasspath()
    {
        return getPaths( COMPILECLASSPATH.getName() );
    }

    public Paths getClasspath()
    {
        return getPaths( CLASSPATH.getName() );
    }

    public Paths getSource()
    {
        return getPaths( SOURCE.getName() );
    }

    public Paths getResources()
    {
        return getPaths( RESOURCES.getName() );
    }

    public String getJar()
    {
        return get( JAR.getName() );
    }

    public String getJarPath()
    {
        return getPath( JAR.getName() );
    }

    public File getJarPathFile()
    {
        return new File( getJarPath() );
    }

    public Paths getDist()
    {
        return getPaths( DIST.getName() );
    }

    public String getAppDir()
    {
        return get( APPDIR.getName() );
    }

    public String getAppDirPath()
    {
        return getPath( APPDIR.getName() );
    }

    public String getOneJar()
    {
        return get( ONEJAR.getName() );
    }

    public String getOneJarPath()
    {
        return getPath( ONEJAR.getName() );
    }

    public File getOneJarPathFile()
    {
        String zJar = getOneJarPath();
        if ( zJar == null )
        {
            return null;
        }
        int at = zJar.lastIndexOf( '.' );
        if ( at == -1 || !".jar".equalsIgnoreCase( zJar.substring( at ) ) )
        {
            zJar += ".jar";
        }
        return new File( zJar );
    }

    public String getWar()
    {
        return get( WAR.getName() );
    }

    public String getWarPath()
    {
        return getPath( WAR.getName() );
    }

    public File getWarPathFile()
    {
        return new File( getWarPath() );
    }

    public String getGWT()
    {
        return get( GWT.getName() );
    }

    public String getGWTat()
    {
        return get( GWTat.getName() );
    }

    public File getGWTatDir()
    {
        return new File( getPath( GWTat.getName() ) );
    }

    public String getGWTwar()
    {
        return get( GWTwar.getName() );
    }

    public String getGWTwarPath()
    {
        return getPath( GWTwar.getName() );
    }

    public String getGWTstyle()
    {
        return get( GWTstyle.getName() );
    }

    public String getGWTlogging()
    {
        return get( GWTlogging.getName() );
    }

    public String getGWTmx()
    {
        return get( GWTmx.getName() );
    }

    // ---------------------------------- Generic accessors for the underlying Data (map) ------------------------------

    public boolean has( Object key )
    {
        return null != getObject( key );
    }

    public Object getObject( Object key )
    {
        return mManager.get( mManager.normalizeKey( key ) );
    }

    public Object getObject( Object key, Object defaultValue )
    {
        return Util.deNull( getObject( key ), defaultValue );
    }

    public String get( Object key )
    {
        Object zValue = getObject( key );
        return (zValue != null) ? zValue.toString() : null;
    }

    public String get( Object key, String defaultValue )
    {
        return Util.deNull( get( key ), defaultValue );
    }

    public int get_int( Object key )
    {
        return get_int( key, 0 );
    }

    public int get_int( Object key, int defaultValue )
    {
        return getInteger( key, defaultValue );
    }

    public Integer getInteger( Object key, int defaultValue )
    {
        return Util.deNull( getInteger( key ), defaultValue );
    }

    public Integer getInteger( Object key )
    {
        return getCachedWithConvertion( key, INTEGER, null );
    }

    public float get_float( Object key )
    {
        return get_float( key, 0 );
    }

    public float get_float( Object key, float defaultValue )
    {
        return getFloat( key, defaultValue );
    }

    public Float getFloat( Object key, float defaultValue )
    {
        return Util.deNull( getFloat( key ), defaultValue );
    }

    public Float getFloat( Object key )
    {
        return getCachedWithConvertion( key, FLOAT, null );
    }

    public boolean get_boolean( Object key )
    {
        return get_boolean( key, false );
    }

    public boolean get_boolean( Object key, boolean defaultValue )
    {
        return getBoolean( key, defaultValue );
    }

    public Boolean getBoolean( Object key, boolean defaultValue )
    {
        return Util.deNull( getBoolean( key ), defaultValue );
    }

    public Boolean getBoolean( Object key )
    {
        return getCachedWithConvertion( key, BOOLEAN, null );
    }

    /**
     * Returns a list of objects under the specified key. If the key is a single value, it is placed in a list and returned. If the
     * key does not exist, a list with the defaultValues is returned.
     */
    public List<?> getObjectListNotNull( Object key, Object... defaultValues )
    {
        List<?> zList = getObjectList( key );
        return (zList != null) ? zList : (defaultValues == null) ? Collections.emptyList() : Arrays.asList( defaultValues );
    }

    /**
     * Returns a list of objects under the specified key. If the key is a single value, it is placed in a list and returned. If the
     * key does not exist, a 'null' is returned.
     */
    public List<?> getObjectList( Object key )
    {
        return getCachedWithConvertion( key, LIST_OBJECT, null );
    }

    /**
     * Returns a list of strings under the specified key. If the key is a single value, it is placed in a list and returned. If the
     * key does not exist, a list with the defaultValues is returned.
     */
    public List<String> getListNotNull( Object key, String... defaultValues )
    {
        List<String> zList = getList( key );
        return (zList != null) ? zList : (defaultValues == null) ? Collections.<String>emptyList() : Arrays.asList( defaultValues );
    }

    /**
     * Returns a list of strings under the specified key. If the key is a single value, it is placed in a list and returned. If the
     * key does not exist, a 'null' is returned.
     */
    public List<String> getList( Object key )
    {
        return getCachedWithConvertion( key, LIST_STRING, null );
    }

    /**
     * Returns a Map<Object,Object> under the specified key. If the key does not exist, a Map with the defaultValues (as Key/Value pairs) is returned.
     */
    public Map<?, ?> getObjectMapNotNull( Object key, Object... defaultValues )
    {
        Util.assertPairedEntries( "defaultValues", defaultValues );
        Map<?, ?> map = getObjectMap( key );
        return (map != null) ? map : createMap( defaultValues );
    }

    /**
     * Returns a Map<Object,Object> under the specified key. If the key does not exist, a 'null' is returned.
     */
    public Map<?, ?> getObjectMap( Object key )
    {
        return getCachedWithConvertion( key, MAP_OBJECT, null );
    }

    /**
     * Returns a Map<String,String> under the specified key. If the key does not exist, a Map with the defaultValues (as Key/Value pairs) is returned.
     */
    public Map<String, String> getMapNotNull( Object key, String... defaultValues )
    {
        Util.assertPairedEntries( "defaultValues", defaultValues );
        Map<String, String> map = getMap( key );
        return (map != null) ? map : createMap( defaultValues );
    }

    /**
     * Returns a Map<String, String> under the specified key. If the key does not exist, a 'null' is returned.
     */
    public Map<String, String> getMap( Object key )
    {
        return getCachedWithConvertion( key, MAP_STRING, null );
    }

    /**
     * Uses the strings under the specified key to {@link Paths#glob(String, String...) glob} paths.
     */
    public Paths getPaths( String key )
    {
        return getCachedWithConvertion( key, PATHS, PATHS_DEFAULTER );
    }

    /**
     * Uses the strings under the specified key to {@link Paths#glob(String, String...) glob} paths.
     */
    public String getPath( String key )
    {
        return getCachedWithConvertion( key, PATH, null );
    }

    /**
     * Returns a canonicalizePath built from the specified path.
     * If the specified path is a relative path, it is made absolute relative to this project's directory.
     */
    public String path( String path )
    {
        if ( path == null )
        {
            return null;
        }
        path = format( path );
        String zSuffix = "";
        int pipeIndex = path.indexOf( '|' );
        if ( pipeIndex > -1 )
        {
            // Handle wildcard search patterns.
            zSuffix = path.substring( pipeIndex );
            path = path.substring( 0, pipeIndex );
        }
        return canonicalizePath( getCanonicalProjectDir(), path ).getPath() + zSuffix;
    }

    /**
     * Replaces property names surrounded by dollar-signs ('$') with the value from this project.
     */
    public String format( String text )
    {
        Matcher matcher = formatPattern.matcher( text );
        StringBuilder buffer = new StringBuilder( 128 );
        while ( matcher.find() )
        {
            buffer.append( matcher.group( 1 ) );
            String name = matcher.group( 2 );
            Object value = getObject( name );
            if ( value instanceof String )
            {
                buffer.append( format( (String) value ) );
            }
            else if ( value != null )
            {
                buffer.append( value );
            }
            else
            {
                buffer.append( name );
            }
            buffer.append( matcher.group( 3 ) );
        }
        if ( buffer.length() == 0 )
        {
            return text;
        }
        return buffer.toString();
    }

    public Object[] keys()
    {
        return mManager.keys();
    }

    public File getCanonicalProjectDir()
    {
        return mCanonicalProjectDir;
    }

    public String getName()
    {
        return mName;
    }

    public ProjectParameters( File pProjectFile, String pName, File pCanonicalProjectDir, Map<Object, Object> pData )
    {
        this( pProjectFile.lastModified(), pName, pCanonicalProjectDir, new Manager( pData ) );
    }

    // ---------------------------------------------------- Support ----------------------------------------------------

    protected ProjectParameters( ProjectParameters pParameters )
    {
        this( pParameters.mProjectFileLastModified, pParameters.getName(), pParameters.getCanonicalProjectDir(), pParameters.mManager );
    }

    private ProjectParameters( long pProjectFileLastModified, String pName, File pCanonicalProjectDir, Manager pManager )
    {
        mProjectFileLastModified = pProjectFileLastModified;
        mCanonicalProjectDir = pCanonicalProjectDir;
        mManager = pManager;
        Object zName = mManager.get( NAME.getName() );
        mManager.put( NAME.getName(), mName = (zName != null) ? zName.toString() : pName );
    }

    protected final long mProjectFileLastModified;
    protected final File mCanonicalProjectDir;
    protected final Manager mManager;
    protected final String mName;

    static private final java.util.regex.Pattern formatPattern = java.util.regex.Pattern.compile( "([^\\$]*)\\$([^\\$]+)\\$([^\\$]*)" );

    @SuppressWarnings({"unchecked"})
    private <T> Map<T, T> getAsMap( Object pKey )
    {
        Object zValue = getObject( pKey );
        return (zValue instanceof Map) ? (Map<T, T>) zValue : null;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    protected void defaultSubDirOptional( Parameter pParameter, String... pOptions )
    {
        Object o = mManager.get( pParameter.getName() );
        if ( o == null )
        {
            for ( String zOption : pOptions )
            {
                int at = zOption.indexOf( '|' );
                if ( dirExists( (at == -1) ? zOption : zOption.substring( 0, at ) ) )
                {
                    mManager.put( pParameter.getName(), zOption );
                    return;
                }
            }
        }
    }

    protected boolean dirExists( String pPath )
    {
        int pipeAt = pPath.indexOf( '|' );
        if ( pipeAt != -1 )
        {
            pPath = pPath.substring( pipeAt );
        }
        return new File( getCanonicalProjectDir(), pPath ).isDirectory();
    }

    protected void defaultKey( Parameter pParameter, String pDefault )
    {
        if ( null == get( pParameter.getName() ) )
        {
            mManager.put( pParameter.getName(), pDefault );
        }
    }

    protected <T> Map<T, T> createMap( T[] defaultValues )
    {
        Map<T, T> map = new HashMap<T, T>();
        if ( defaultValues != null )
        {
            for ( int i = 0; i < defaultValues.length; )
            {
                T defaultKey = defaultValues[i++];
                T defaultValue = defaultValues[i++];
                map.put( defaultKey, defaultValue );
            }
        }
        return map;
    }

    protected <T> T getCachedWithConvertion( Object pKey, DataConverter<T> pConverter, DataDefaulter<T> pDefaulter )
    {
        Object zValue = mManager.getCachedResponse( pKey = mManager.normalizeKey( pKey ) );
        if ( zValue != null )
        {
            //noinspection unchecked
            return (T) zValue;
        }
        zValue = mManager.get( pKey );
        T zConverted = null;
        if ( zValue != null )
        {
            zConverted = pConverter.convert( zValue );
        }
        else if ( pDefaulter != null )
        {
            zConverted = pDefaulter.createDefault();
        }
        if ( zConverted != null )
        {
            mManager.addCachedResponse( pKey, zConverted );
        }
        return zConverted;
    }

    private static final DataConverter<Integer> INTEGER = new DataConverter<Integer>()
    {
        @Override
        public Integer convert( Object pValue )
        {
            return (pValue instanceof Number) ? ((Number) pValue).intValue() : Integer.parseInt( pValue.toString() );
        }
    };

    private static final DataConverter<Float> FLOAT = new DataConverter<Float>()
    {
        @Override
        public Float convert( Object pValue )
        {
            return (pValue instanceof Number) ? ((Number) pValue).floatValue() : Float.parseFloat( pValue.toString() );
        }
    };

    private static final DataConverter<Boolean> BOOLEAN = new DataConverter<Boolean>()
    {
        @Override
        public Boolean convert( Object pValue )
        {
            return (pValue instanceof Boolean) ? (Boolean) pValue : Boolean.parseBoolean( pValue.toString() );
        }
    };

    private static final DataConverter<List<?>> LIST_OBJECT = new DataConverter<List<?>>()
    {
        @Override
        public List<?> convert( Object pValue )
        {
            return (pValue instanceof List) ? (List<?>) pValue : Arrays.asList( pValue );
        }
    };

    private static final DataConverter<List<String>> LIST_STRING = new DataConverter<List<String>>()
    {
        @Override
        public List<String> convert( Object pValue )
        {
            //noinspection unchecked
            return (pValue instanceof List) ? (List<String>) pValue : Arrays.asList( pValue.toString() );
        }
    };

    private static final DataConverter<Map<?, ?>> MAP_OBJECT = new DataConverter<Map<?, ?>>()
    {
        @Override
        public Map<?, ?> convert( Object pValue )
        {
            return (Map<?, ?>) pValue;
        }
    };

    private static final DataConverter<Map<String, String>> MAP_STRING = new DataConverter<Map<String, String>>()
    {
        @Override
        public Map<String, String> convert( Object pValue )
        {
            //noinspection unchecked
            return (Map<String, String>) pValue;
        }
    };

    private final DataConverter<String> PATH = new DataConverter<String>()
    {
        @Override
        public String convert( Object pValue )
        {
            return path( pValue.toString() );
        }
    };

    private final DataConverter<Paths> PATHS = new DataConverter<Paths>()
    {
        @Override
        public Paths convert( Object pValue )
        {
            Paths paths = new Paths();
            List<String> zList = LIST_STRING.convert( pValue );
            for ( String dirPattern : zList )
            {
                paths.glob( path( dirPattern ) );
            }
            return paths;
        }
    };

    private final DataDefaulter<Paths> PATHS_DEFAULTER = new DataDefaulter<Paths>()
    {
        @Override public Paths createDefault()
        {
            return new Paths();
        }
    };
}
