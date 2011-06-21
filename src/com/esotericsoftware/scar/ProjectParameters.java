package com.esotericsoftware.scar;

import com.esotericsoftware.scar.support.*;

import static com.esotericsoftware.scar.support.Parameter.*;

@SuppressWarnings({"UnusedDeclaration"})
public interface ProjectParameters
{
    public static final Parameter NAME = def( "name", Form.STRING, "The name of the project. Used to name the JAR.", //
                                              "Default:\n" + //
                                              "  The name of the directory containing the project YAML file, or\n" + //
                                              "  the name of the YAML file if it is not 'build'." );

    public static final Parameter TARGET = def( "target", null, "The directory to output build artifacts.", //
                                                "Default: The directory containing the project YAML file, plus '../target/name'." );

    public static final Parameter VERSION = def( "version", Form.STRING, "The version of the project. If available, used to name the JAR." );

    public static final Parameter RESOURCES = def( "resources", null, "Wildcard patterns for the files to include in the JAR.", //
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
}
