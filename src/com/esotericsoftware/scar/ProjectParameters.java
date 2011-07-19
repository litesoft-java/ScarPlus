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

    public static final Parameter CLASSPATH = def( "classpath", Form.PATHS, "Wildcard patterns for the file(s) to include on the classpath.", //
                                                   "Default: 'lib|**.jar'." );

    public static final Parameter SOURCE = def( "source", Form.PATHS, "Wildcard patterns for the Java file(s) to compile.", //
                                                "Default: 'src|**.java' or 'src/main/java|**.java'." );

    public static final Parameter RESOURCES = def( "resources", Form.PATHS, "Wildcard patterns for the file(s) to include in the JAR.", //
                                                   "Default: 'resources' or 'src/main/resources'." );

    public static final Parameter JAR = def( "jar", Form.STRING, "JAR name w/ optional path for the JAR ('.jar' added to the end if does not end with 'jar', case insensitive).", //
                                             "Default: '$target$[-$version$]'." );

    public static final Parameter DIST = def( "dist", Form.PATHS, "Wildcard patterns for the file(s) to include in the distribution, outside the JAR." );

    public static final Parameter APPDIR = def( "appdir", Form.STRING, "Directory path to bring together all files (both JARs and 'dist', for this project " + "and all dependencies, recursively) the application needs to be run from JAR files." );

    // ------------------------------------------------ Default Support ------------------------------------------------

    protected synchronized void applyDefaults()
    {
        defaultTARGET();
        defaultCLASSPATH();
        defaultSOURCE();
        defaultRESOURCES();
        defaultJAR();
    }

    protected void defaultJAR()
    {
        String zJar = getJar();
        if ( null == zJar )
        {
            mManager.put( JAR.getName(), "$target$/$name$" + (hasVersion() ? "-$version$" : "") + ".jar" );
        }
        else
        {
            int at = zJar.lastIndexOf( '.' );
            if ( at == -1 || !".jar".equalsIgnoreCase( zJar.substring( at ) ) )
            {
                mManager.put( JAR.getName(), zJar + ".jar" );
            }
        }
    }

    protected void defaultTARGET()
    {
        defaultKey( TARGET, "build" );
    }

    private void defaultCLASSPATH()
    {
        defaultSubDirOptional( CLASSPATH, "lib|**.jar" );
    }

    private void defaultRESOURCES()
    {
        defaultSubDirOptional( RESOURCES, "src/main/resources", "resources" );
    }

    private void defaultSOURCE()
    {
        defaultSubDirOptional( SOURCE, "src/main/java|**.java", "src|**.java" );
    }

    // ------------------------------------------- Special Property Accessors ------------------------------------------

    public boolean hasVersion()
    {
        return (null != getVersion());
    }

    public String getVersion()
    {
        return get( JAR.getName() );
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

    public List<String> getDependencies()
    {
        return getListNotNull( DEPENDENCIES.getName() );
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

    public Paths getDist()
    {
        return getPaths( DIST.getName() );
    }

    public String getAppDir()
    {
        return get( APPDIR.getName() );
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
        return getCachedWithConvertion( key, INTEGER );
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
        return getCachedWithConvertion( key, FLOAT );
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
        return getCachedWithConvertion( key, BOOLEAN );
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
        return getCachedWithConvertion( key, LIST_OBJECT );
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
        return getCachedWithConvertion( key, LIST_STRING );
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
        return getCachedWithConvertion( key, MAP_OBJECT );
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
        return getCachedWithConvertion( key, MAP_STRING );
    }

    /**
     * Uses the strings under the specified key to {@link Paths#glob(String, String...) glob} paths.
     */
    public Paths getPaths( String key )
    {
        Paths zPaths = getCachedWithConvertion( key, PATHS );
        return (zPaths != null) ? zPaths : new Paths();
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

    public ProjectParameters( String pName, File pCanonicalProjectDir, Map<Object, Object> pData )
    {
        this( pName, pCanonicalProjectDir, new Manager( pData ) );
    }

    // ---------------------------------------------------- Support ----------------------------------------------------

    protected ProjectParameters( ProjectParameters pParameters )
    {
        this( pParameters.getName(), pParameters.getCanonicalProjectDir(), pParameters.mManager );
    }

    private ProjectParameters( String pName, File pCanonicalProjectDir, Manager pManager )
    {
        mCanonicalProjectDir = pCanonicalProjectDir;
        mManager = pManager;
        Object zName = mManager.get( NAME.getName() );
        mManager.put( NAME.getName(), mName = (zName != null) ? zName.toString() : pName );
    }

    protected final String mName;
    protected final File mCanonicalProjectDir;
    protected final Manager mManager;

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

    protected void deletePath( String pProjectRelativePath )
    {
        String zPath = path( pProjectRelativePath );
        if ( zPath != null )
        {
            delete( zPath );
        }
    }

    protected void defaultSubDirOptional( Parameter pParameter, String... pOptions )
    {
        Object o = mManager.get( pParameter.getName() );
        if ( o == null )
        {
            for ( String zOption : pOptions )
            {
                if ( dirExists( zOption ) )
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

    protected <T> T getCachedWithConvertion( Object pKey, DataConverter<T> pConverter )
    {
        Object zValue = mManager.getCachedResponse( pKey = mManager.normalizeKey( pKey ) );
        if ( zValue != null )
        {
            //noinspection unchecked
            return (T) zValue;
        }
        zValue = mManager.get( pKey );
        if ( zValue == null )
        {
            return null;
        }
        T zConverted = pConverter.convert( zValue );
        mManager.addCachedResponse( pKey, zConverted );
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
}
