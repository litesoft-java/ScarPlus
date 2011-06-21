package com.esotericsoftware.scar;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.regex.Pattern;

import org.litesoft.logger.*;

import com.esotericsoftware.scar.support.*;
import com.esotericsoftware.utils.*;
import com.esotericsoftware.wildcard.*;

/**
 * Generic Data structure that contains information needed to perform tasks.
 */
@SuppressWarnings("UnusedDeclaration")
public class Project implements ProjectParameters
{
    public static class Parameters
    {
        private final String mName;
        private final File mDirectory;
        private final Map<Object, Object> mData = new HashMap<Object, Object>();

        public Parameters( String pName, File pDirectory, Map<Object, Object> pData )
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

        public String getName()
        {
            return mName;
        }

        public File getDirectory()
        {
            return mDirectory;
        }

        public Map<Object, Object> getData()
        {
            return mData;
        }
    }

    protected static final Logger LOGGER = LoggerFactory.getLogger( Project.class );

    protected final String mName;
    protected final File mDirectory;
    protected final Map<Object, Object> mData;

    public Project( Parameters pParameters )
    {
        mName = pParameters.getName();
        mDirectory = pParameters.getDirectory();
        mData = pParameters.getData();
    }

    public synchronized String getName()
    {
        return mName;
    }

    public synchronized File getDirectory()
    {
        return mDirectory;
    }

    public List<String> getDependencies()
    {
        return getList( DEPENDENCIES.getName() );
    }

    public synchronized void set( Object key, Object object )
    {
        if ( key instanceof String )
        {
            key = Util.noEmpty( key.toString().toLowerCase() );
            if ( NAME.getName().equals( key ) )
            {
                throw new IllegalArgumentException( NAME.getName() + " not updateable!" );
            }
        }
        Util.assertNotNull( "key", key );
        mData.put( key, object );
    }

    public synchronized void remove( Object key )
    {
        mData.remove( key );
    }

    /**
     * Removes an item from a list or map. If the mData under the specified key is a list, the entry equal to the specified value is
     * removed. If the mData under the specified key is a map, the entry with the key specified by value is removed.
     */
    public synchronized void remove( Object key, Object value )
    {
        Object object = mData.get( key );
        if ( object instanceof Map )
        {
            ((Map) object).remove( object );
        }
        else if ( object instanceof List )
        {
            ((List) object).remove( object );
        }
        else
        {
            mData.remove( key );
        }
    }

    public synchronized void clear()
    {
        mData.clear();
        mData.put( NAME.getName(), getName() );
    }

    public synchronized Object[] keys()
    {
        return mData.keySet().toArray();
    }

    @Override
    public synchronized String toString()
    {
        return getName();
    }

    public synchronized boolean has( Object key )
    {
        Util.assertNotNull( "key", key );
        return mData.get( key ) != null;
    }

    public synchronized Object getObject( Object key )
    {
        Util.assertNotNull( "key", key );
        return mData.get( key );
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
        if ( defaultValues != null )
        {
            return Arrays.asList( defaultValues );
        }
        return null;
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
        if ( defaultValues != null )
        {
            return Arrays.asList( defaultValues );
        }
        return null;
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

    @SuppressWarnings({"unchecked"})
    private <T> Map<T, T> getAsMap( Object pKey )
    {
        Object zValue = getObject( pKey );
        return (zValue instanceof Map) ? (Map<T, T>) zValue : null;
    }

    /**
     * Uses the strings under the specified key to {@link Paths#glob(String, String...) glob} paths.
     */
    public Paths getPaths( String key )
    {
        Paths paths = new Paths();
        Object object = getObject( key );
        if ( object instanceof List )
        {
            for ( Object dirPattern : (List) object )
            {
                paths.glob( path( (String) dirPattern ) );
            }
        }
        else if ( object instanceof String )
        {
            paths.glob( path( (String) object ) );
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

    static private final Pattern formatPattern = Pattern.compile( "([^\\$]*)\\$([^\\$]+)\\$([^\\$]*)" );

    /**
     * Executes the buildDependencies, clean, compile, jar, and dist utility metshods.
     */
    public void build()
            throws IOException
    {
        System.out.println( "build: " + this );
        buildDependencies();
//        clean( project );
//        compile( project );
//        jar( project );
//        dist( project );
//
//        builtProjects.add( project.get( "name" ) );
    }

    /**
     * Calls {@link #build(Project)} for each dependency project in the specified project.
     */
    public void buildDependencies()
            throws IOException
    {
//        for ( String dependency : project.getDependencies() )
//        {
//            Project dependencyProject = project( project.path( dependency ) );
//
//            if ( builtProjects.contains( dependencyProject.get( "name" ) ) )
//            {
//                LOGGER.debug.log( "Dependency project already built: ", dependencyProject );
//                return;
//            }
//
//            String jarFile;
//            if ( dependencyProject.has( "version" ) )
//            {
//                jarFile = dependencyProject.path( "$target$/$name$-$version$.jar" );
//            }
//            else
//            {
//                jarFile = dependencyProject.path( "$target$/$name$.jar" );
//            }
//
//            LOGGER.debug.log( "Building dependency: ", dependencyProject );
//            if ( !executeDocument( dependencyProject ) )
//            {
//                build( dependencyProject );
//            }
//        }
    }

    /**
     * Creates an empty project, without any default properties, and then loads the specified YAML files.
     */
//    public Project( String path, ProjectFactory pFactory, String... paths )
//            throws IOException
//    {
//        Util.assertNotNull( "path", path );
//
//        load( path );
//        if ( paths != null )
//        {
//            for ( String mergePath : paths )
//            {
//                replace( new Project( mergePath, pFactory ) );
//            }
//        }
//    }

    /**
     * Clears the Data in this project and replaces it with the contents of the specified YAML file. The project directory is set
     * to the directory containing the YAML file.
     *
     * @param path Path to a YAML project file, or a directory containing a "project.yaml" file.
     */
//    public void load( String path )
//            throws IOException
//    {
//        File file = new File( path );
//        if ( !file.exists() && !path.endsWith( ".yaml" ) )
//        {
//            path += ".yaml";
//            file = new File( path );
//        }
//        Util.assertExists( "Project", file );
//        if ( file.isDirectory() )
//        {
//            file = new File( file, "project.yaml" );
//            if ( file.exists() )
//            {
//                load( file.getPath() );
//            }
//            else
//            {
//                dir = Utils.canonical( path );
//            }
//            return;
//        }
//        dir = new File( Utils.canonical( path ) ).getParent().replace( '\\', '/' );
//
//        BufferedReader fileReader = new BufferedReader( new FileReader( path ) );
//        try
//        {
//            StringBuffer buffer = new StringBuffer( 2048 );
//            while ( true )
//            {
//                String line = fileReader.readLine();
//                if ( line == null || line.trim().equals( "---" ) )
//                {
//                    break;
//                }
//                buffer.append( line );
//                buffer.append( '\n' );
//            }
//
//            YamlReader yamlReader = new YamlReader( new StringReader( buffer.toString() ) )
//            {
//                @Override
//                protected Object readValue( Class type, Class elementType, Class defaultType )
//                        throws YamlException, ParserException, TokenizerException
//                {
//                    Object value = super.readValue( type, elementType, defaultType );
//                    if ( value instanceof String )
//                    {
//                        value = ((String) value).replaceAll( "\\$dir\\$", dir );
//                    }
//                    return value;
//                }
//            };
//            try
//            {
//                mData = yamlReader.read( HashMap.class );
//                yamlReader.close();
//            }
//            catch ( YamlException ex )
//            {
//                throw new IOException( "Error reading YAML file: " + new File( path ).getAbsolutePath(), ex );
//            }
//            if ( mData == null )
//            {
//                mData = new HashMap();
//            }
//
//            buffer.setLength( 0 );
//            while ( true )
//            {
//                String line = fileReader.readLine();
//                if ( line == null )
//                {
//                    break;
//                }
//                buffer.append( line );
//                buffer.append( '\n' );
//            }
//            document = buffer.toString();
//        }
//        finally
//        {
//            fileReader.close();
//        }
//    }

    /**
     * Replaces the Data in this project with the contents of the specified YAML file. If the specified project has Data with the
     * same key as this project, the value is overwritten. Keys in this project that are not in the specified project are not
     * affected.
     */
//    public void replace( Project project )
//            throws IOException
//    {
//        Util.assertNotNull( "project", project );
//        mData.putAll( project.mData );
//        document = project.document;
//        dir = project.dir;
//    }
    public synchronized void initialize( ProjectFactory pProjectFactory )
    {
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
//            String dependencyName = project( project.path( dependency ) ).get( "name" );
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
}
