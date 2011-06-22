package com.esotericsoftware.scar;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.esotericsoftware.scar.support.*;
import com.esotericsoftware.utils.*;
import com.esotericsoftware.wildcard.*;

import static com.esotericsoftware.scar.support.Parameter.*;

@SuppressWarnings({"UnusedDeclaration"})
public class ProjectParameters
{
    public static final Parameter NAME = def( "name", Form.STRING, "The name of the project. Used to name the JAR.", //
                                              "Default:\n" + //
                                              "  The name of the directory containing the project YAML file, or\n" + //
                                              "  the name of the YAML file if it is not 'build'." );

    public static final Parameter TARGET = def( "target", Form.STRING, "The directory to output build artifacts.", //
                                                "Default: The directory containing the project YAML file, plus '../target/name'." );

    public static final Parameter VERSION = def( "version", Form.STRING, "The version of the project. If available, used to name the JAR." );

    public static final Parameter RESOURCES = def( "resources", Form.PATHS, "Wildcard patterns for the files to include in the JAR.", //
                                                   "Default: 'resources' or 'src/main/resources'." );

    public static final Parameter DIST = def( "dist", null, "Wildcard patterns for the files to include in the distribution, outside the JAR.", //
                                              "Default: 'dist'." );

    public static final Parameter SOURCE = def( "source", null, "Wildcard patterns for the Java files to compile.", //
                                                "Default: 'src|**/*.java' or 'src/main/java|**/*.java'." );

    public static final Parameter CLASSPATH = def( "classpath", null, "Wildcard patterns for the files to include on the classpath.", //
                                                   "Default: 'lib|**/*.jar'." );

    public static final Parameter DEPENDENCIES = def( "dependencies", Form.STRING_LIST, "Relative or absolute paths to dependency project directories or YAML files." );

    public static final Parameter INCLUDE = def( "include", null, "Relative or absolute paths to project files to inherit properties from." );

    public static final Parameter MAIN = def( "main", Form.STRING, "Name of the main class." );

    protected final String mName;
    protected final File mDirectory;
    protected final Map<Object, Object> mData = new HashMap<Object, Object>();

    public ProjectParameters( String pName, File pDirectory, Map<Object, Object> pData )
    {
        mDirectory = pDirectory;
        if ( pData != null )
        {
            for ( Object key : pData.keySet() )
            {
                Object zValue = pData.get( key );
                if ( key instanceof String )
                {
                    key = key.toString().toLowerCase();
                }
                mData.put( key, zValue );
            }
        }
        Object zObjName = mData.get( NAME.getName() );
        String zName = (zObjName == null) ? null : Util.noEmpty( zObjName.toString() );
        mData.put( NAME.getName(), mName = (zName != null) ? zName : pName );
    }

    @Override
    public String toString()
    {
        return getName();
    }

    protected Object valueFor( Object pKey )
    {
        if ( pKey instanceof String )
        {
            pKey = Util.noEmpty( pKey.toString().toLowerCase() );
        }
        Util.assertNotNull( "key", pKey );
        synchronized ( this )
        {
            return mData.get( pKey );
        }
    }

    public synchronized Object[] keys()
    {
        return mData.keySet().toArray();
    }

    public synchronized File getDirectory()
    {
        return mDirectory;
    }

    public synchronized String getName()
    {
        return mName;
    }

    public String getTarget()
    {
        return get( TARGET.getName() );
    }

    public boolean hasVersion()
    {
        return (null != getVersion());
    }

    public String getVersion()
    {
        return get( VERSION.getName() );
    }

    public Paths getResources()
    {
        return getPaths( RESOURCES.getName() );
    }

    public String getDist()
    {

    }

    public String getSource()
    {

    }

    public String getClasspath()
    {

    }

    public List<String> getDependencies()
    {
        return getList( DEPENDENCIES.getName() );
    }

    public String getInclude()
    {

    }

    public String getMain()
    {

    }

    // Below here are the accessors for the underlying Data (map)

    public boolean has( Object key )
    {
        return null != valueFor( key );
    }

    public Object getObject( Object key )
    {
        return valueFor( key );
    }

    public Object getObject( Object key, Object defaultValue )
    {
        return Util.deNull( getObject( key ), defaultValue );
    }

    public String get( Object key )
    {
        return get( key, null );
    }

    public String get( Object key, String defaultValue )
    {
        Object value = getObject( key );
        return (value != null) ? value.toString() : defaultValue;
    }

    public int getInt( Object key )
    {
        return getInt( key, 0 );
    }

    public int getInt( Object key, int defaultValue )
    {
        Object value = getObject( key );
        if ( value == null )
        {
            return defaultValue;
        }
        if ( value instanceof Number )
        {
            return ((Number) value).intValue();
        }
        return Integer.parseInt( value.toString() );
    }

    public float getFloat( Object key )
    {
        return getFloat( key, 0 );
    }

    public float getFloat( Object key, float defaultValue )
    {
        Object value = getObject( key );
        if ( value == null )
        {
            return defaultValue;
        }
        if ( value instanceof Number )
        {
            return ((Number) value).floatValue();
        }
        return Float.parseFloat( value.toString() );
    }

    public boolean getBoolean( Object key )
    {
        return getBoolean( key, false );
    }

    public boolean getBoolean( Object key, boolean defaultValue )
    {
        Object value = getObject( key );
        if ( value == null )
        {
            return defaultValue;
        }
        if ( value instanceof Boolean )
        {
            return (Boolean) value;
        }
        return Boolean.parseBoolean( value.toString() );
    }

    /**
     * Returns a list of strings under the specified key. If the key is a single value, it is placed in a list and returned. If the
     * key does not exist, an empty list is returned.
     */
    public List<String> getList( Object key, String... defaultValues )
    {
        Object object = getObject( key );
        if ( object instanceof List )
        {
            List src = (List) object;
            List<String> rv = new ArrayList<String>( src.size() );
            for ( Object entry : src )
            {
                rv.add( entry.toString() );
            }
            return rv;
        }
        if ( object != null )
        {
            return Arrays.asList( object.toString() );
        }
        return (defaultValues == null) ? null : Arrays.asList( defaultValues );
    }

    /**
     * Returns a list of objects under the specified key. If the key is a single value, it is placed in a list and returned. If the
     * key does not exist, an empty list is returned.
     */
    public List getObjectList( Object key, Object... defaultValues )
    {
        Object object = getObject( key );
        if ( object instanceof List )
        {
            return (List) object;
        }
        if ( object != null )
        {
            return Arrays.asList( object );
        }
        return (defaultValues == null) ? null : Arrays.asList( defaultValues );
    }

    public Map<String, String> getMap( Object key, String... defaultValues )
    {
        Util.assertPairedEntries( "defaultValues", defaultValues );
        Map<String, String> map = getAsMap( key );
        if ( map == null )
        {
            if ( defaultValues != null )
            {
                map = new HashMap<String, String>();
                for ( int i = 0; i < defaultValues.length; )
                {
                    String defaultKey = defaultValues[i++];
                    String defaultValue = defaultValues[i++];
                    map.put( defaultKey, defaultValue );
                }
            }
        }
        return map;
    }

    public Map getObjectMap( Object key, Object... defaultValues )
    {
        Util.assertPairedEntries( "defaultValues", defaultValues );
        Map<Object, Object> map = getAsMap( key );
        if ( map == null )
        {
            if ( defaultValues != null )
            {
                map = new HashMap<Object, Object>();
                for ( int i = 0; i < defaultValues.length; )
                {
                    Object defaultKey = defaultValues[i++];
                    Object defaultValue = defaultValues[i++];
                    map.put( defaultKey, defaultValue );
                }
            }
        }
        return map;
    }

    /**
     * Uses the strings under the specified key to {@link Paths#glob(String, String...) glob} paths.
     */
    public Paths getPaths( String key )
    {
        Paths paths = new Paths();
        for ( String dirPattern : getList( key ) )
        {
            paths.glob( path( dirPattern ) );
        }
        return paths;
    }

    /**
     * Returns the specified path if it is an absolute path, otherwise returns the path relative to this project's directory.
     */
    public String path( String path )
    {
        path = format( path );
        int pipeIndex = path.indexOf( '|' );
        if ( pipeIndex > -1 )
        {
            // Handle wildcard search patterns.
            return path( path.substring( 0, pipeIndex ) ) + path.substring( pipeIndex );
        }
        if ( new File( path ).isAbsolute() )
        {
            return path;
        }
        return new File( getDirectory(), path ).getAbsolutePath();
    }

    /**
     * Replaces property names surrounded by curly braces with the value from this project.
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

    static private final java.util.regex.Pattern formatPattern = java.util.regex.Pattern.compile( "([^\\$]*)\\$([^\\$]+)\\$([^\\$]*)" );

    @SuppressWarnings({"unchecked"})
    private <T> Map<T, T> getAsMap( Object pKey )
    {
        Object zValue = getObject( pKey );
        return (zValue instanceof Map) ? (Map<T, T>) zValue : null;
    }
}
