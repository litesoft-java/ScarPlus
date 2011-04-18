import java.io.*;
import java.util.*;

import com.esotericsoftware.scar.*;

import static com.esotericsoftware.minlog.Log.*;

public class Builder
{
    // Magic Property Values:

    private static final String NAME = "name"; //. . . . . . . . . The name of the project. Used to name the JAR. Default: The name of the directory containing the project YAML file.
    private static final String TARGET = "target"; //. . . . . . . The directory to output build artifacts.   Default: The directory containing the project YAML file, plus "../target/name".
    private static final String VERSION = "version"; //. . . . . . The version of the project. If available, used to name the JAR.   Default: blank
    private static final String RESOURCES = "resources"; //. . . . Wildcard patterns for the files to include in the JAR.   Default: resources and src/main/resources.
    private static final String DIST = "dist"; //. . . . . . . . . Wildcard patterns for the files to include in the distribution, outside the JAR.   Default: dist.
    private static final String SOURCE = "source"; //. . . . . . . Wildcard patterns for the Java files to compile.   Default: src **/*.java and src/main/java **/*.java.
    private static final String CLASSPATH = "classpath"; //. . . . Wildcard patterns for the files to include on the classpath.   Default: lib **/*.jar.
    private static final String DEPENDENCIES = "dependencies"; //. Relative or absolute paths to dependency project directories or YAML files.   Default: blank
    private static final String INCLUDE = "include"; //. . . . . . Relative or absolute paths to project files to inherit properties from.   Default: blank
    private static final String MAIN = "main"; //. . . . . . . . . Name of the main class.   Default: blank

    private static final String DEPENDS = "depends"; //. . . . . . List of "Functional" dependencies (e.g. litesoft).   Default: blank
    private static final String DEV_ROOT_DIR = "DevRootDir"; //. . Relative Path to the Dev Root Dir (where litesoft and zGlobal) can be found.   Default: blank    Only required if "depends"="litesoft" / mode="GWT" is specified.
    private static final String MODE = "mode"; //. . . . . . . . . Mode of this Project.   Default: JAR   for options, see below...
    // Mode options are (case ignored):
    //      make regular JAR
    //      mode=1JAR
    //      mode=War
    //      mode=GWT
    //      mode=GWT:Detail
    //      mode=GWT:DEBUG
    //      mode=GWT:OBF,WARN




    private boolean mBuilt = false;
    private Builder mParent = null;
    private List<Builder> mDependents = new ArrayList<Builder>();
    private Project mProject = new Project();
    private String mName;

    public Builder( Builder pParent, String pYamlPath )
            throws IOException
    {
        mParent = pParent;
        mProject.load( pYamlPath );
        mName = forceName();
        processProject();
    }

    private void processProject()
    {
        //To change body of created methods use File | Settings | File Templates.
    }

    private String forceName()
    {
        String zName = mProject.get( NAME, "" ).trim();
        if ( zName.length() == 0 )
        {
            zName = new File( mProject.getDirectory() ).getName();
            mProject.set( NAME, zName );
        }
        return zName;
    }

    public void build()
    {
        System.out.println( "build: " + mName );
    }

    public static void main( String[] args )
            throws IOException
    {
        Scar.args = new Arguments( args );

        if ( Scar.args.has( "trace" ) )
        {
            TRACE();
        }
        else if ( Scar.args.has( "debug" ) )
        {
            DEBUG();
        }
        else if ( Scar.args.has( "info" ) )
        {
            INFO();
        }
        else if ( Scar.args.has( "warn" ) )
        {
            WARN();
        }
        else if ( Scar.args.has( "error" ) ) //
        {
            ERROR();
        }

        new Builder( null, Scar.args.get( "file", "." ) ).build();
    }

    @Override
    public String toString()
    {
        return "Project: " + mName;
    }
}
