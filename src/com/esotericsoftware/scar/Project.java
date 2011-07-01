package com.esotericsoftware.scar;

import java.io.*;
import java.util.*;

import org.litesoft.logger.*;

import com.esotericsoftware.scar.support.*;
import com.esotericsoftware.utils.*;

/**
 * Generic Data structure that contains information needed to perform tasks.
 */
@SuppressWarnings("UnusedDeclaration")
public class Project extends ProjectParameters
{
    protected static final Logger LOGGER = LoggerFactory.getLogger( Project.class );

    public Project( ProjectParameters pParameters )
    {
        super( pParameters.getName(), pParameters.getDirectory(), pParameters.mData );
    }

    public synchronized void set( Object key, Object object )
    {
        mData.put( updatableKey( key ), object );
    }

    public synchronized void remove( Object key )
    {
        mData.remove( updatableKey( key ) );
    }

    /**
     * Removes an item from a list or map. If the mData under the specified key is a list, the entry equal to the specified value is
     * removed. If the mData under the specified key is a map, the entry with the key specified by value is removed.
     */
    public synchronized void remove( Object key, Object value )
    {
        Object object = mData.get( key = updatableKey( key ) );
        if ( object instanceof Map )
        {
            ((Map) object).remove( value );
        }
        else if ( object instanceof List )
        {
            ((List) object).remove( value );
        }
        else
        {
            mData.remove( key );
        }
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
     * Executes the buildDependencies, clean, compile, jar, and dist utility metshods.
     */
    public synchronized void build()
            throws IOException
    {
        if ( !mBuilt )
        {
            buildDependencies();
            System.out.println( "build: " + this );
//        clean( project );
//        compile( project );
//        jar( project );
//        dist( project );
//
//        builtProjects.add( project.getName() );
            mBuilt = true;
        }
    }

    /**
     * Calls {@link #build(Project)} for each dependency project in the specified project.
     */
    public void buildDependencies()
            throws IOException
    {
        for ( Project zProject : mDependantProjects )
        {
            zProject.build();
        }
//        for ( String dependency : project.getDependencies() )
//        {
//            Project dependencyProject = project( project.path( dependency ) );
//
//            if ( builtProjects.contains( dependencyProject.getName() ) )
//            {
//                LOGGER.debug.log( "Dependency project already built: ", dependencyProject );
//                return;
//            }
//
//            String jarFile;
//            if ( dependencyProject.hasVersion() )
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
            throws IOException
    {
        List<String> zDependencies = getDependencies();
        if ( zDependencies != null )
        {
            for ( String zDependency : zDependencies )
            {
                mDependantProjects.add( pProjectFactory.project( mDirectory, zDependency ) );
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
