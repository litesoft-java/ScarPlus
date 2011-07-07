package com.esotericsoftware.scar;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.regex.*;
import javax.tools.*;

import com.esotericsoftware.utils.*;

public class Utils extends Util
{
    /**
     * The Scar installation directory. The value comes from the SCAR_HOME environment variable, if it exists. Alternatively, the
     * "scar.home" System property can be defined.
     */
    static public final String SCAR_HOME;

    static
    {
        if ( System.getProperty( "scar.home" ) != null )
        {
            SCAR_HOME = System.getProperty( "scar.home" );
        }
        else
        {
            SCAR_HOME = System.getenv( "SCAR_HOME" );
        }
    }

    /**
     * The Java installation directory.
     */
    static public final String JAVA_HOME = System.getProperty( "java.home" );

    /**
     * Returns the full path for the specified file name in the current working directory, the {@link #SCAR_HOME}, and the bin
     * directory of {@link #JAVA_HOME}.
     */
    static public String resolvePath( String fileName )
    {
        if ( fileName == null )
        {
            return null;
        }

        String foundFile = lowLevelResolve( fileName );
        LOGGER.trace.log( "Path \"", fileName, "\" resolved to: ", foundFile );
        return foundFile;
    }

    private static String lowLevelResolve( String fileName )
    {
        String foundFile;
        if ( fileExists( foundFile = canonical( fileName ) ) )
        {
            return foundFile;
        }
        if ( fileExists( foundFile = new File( SCAR_HOME, fileName ).getPath() ) )
        {
            return foundFile;
        }
        if ( fileExists( foundFile = new File( JAVA_HOME, "bin/" + fileName ).getPath() ) )
        {
            return foundFile;
        }
        return fileName;
    }

    /**
     * Returns the canonical path for the specified path. Eg, if "." is passed, this will resolve the actual path and return it.
     */
    static public String canonical( String path )
    {
        path = assertNotEmpty( "path", path );

        File file = new File( path );
        try
        {
            return file.getCanonicalPath();
        }
        catch ( IOException ex )
        {
            file = file.getAbsoluteFile();
            if ( file.getName().equals( "." ) )
            {
                file = file.getParentFile();
            }
            return file.getPath();
        }
    }

    /**
     * Returns the canonical path for the specified path. Eg, if "." is passed, this will resolve the actual path and return it.
     */
    static public File canonical( File path )
    {
        assertNotNull( "path", path );

        try
        {
            return path.getCanonicalFile();
        }
        catch ( IOException ex )
        {
            return path.getAbsoluteFile();
        }
    }

    /**
     * Creates the directories in the specified path.
     */
    static public String mkdir( String path )
    {
        if ( new File( path = assertNotEmpty( "path", path ) ).mkdirs() )
        {
            LOGGER.trace.log( "Created directory: ", path );
        }
        return path;
    }

    /**
     * Returns true if the file exists.
     */
    static public boolean fileExists( String path )
    {
        return new File( assertNotEmpty( "path", path ) ).exists();
    }

    /**
     * Reads to the end of the input stream and writes the bytes to the output stream.
     */
    static public void copyStream( InputStream input, OutputStream output )
            throws IOException
    {
        assertNotNull( "input", input );
        assertNotNull( "output", output );
        Closeable zCloseable = output;
        try
        {
            byte[] buffer = new byte[4096];
            while ( true )
            {
                int length = input.read( buffer );
                if ( length == -1 )
                {
                    break;
                }
                output.write( buffer, 0, length );
            }
            zCloseable = null;
            output.close();
        }
        finally
        {
            dispose( zCloseable );
            dispose( input );
        }
    }

    /**
     * Copies a file, overwriting any existing file at the destination.
     */
    static public String copyFile( String in, String out )
            throws IOException
    {
        assertNotNull( "in", in );
        assertNotNull( "out", out );
        LOGGER.trace.log( "Copying file: ", in, " -> ", out );

        FileChannel sourceChannel = null;
        FileChannel destinationChannel = null;
        Closeable zCloseable = null;
        try
        {
            sourceChannel = new FileInputStream( in ).getChannel();
            zCloseable = (destinationChannel = new FileOutputStream( out ).getChannel());
            sourceChannel.transferTo( 0, sourceChannel.size(), destinationChannel );
            zCloseable = null;
            destinationChannel.close();
        }
        finally
        {
            dispose( zCloseable );
            dispose( sourceChannel );
        }
        return out;
    }

    /**
     * Moves a file, overwriting any existing file at the destination.
     */
    static public String moveFile( String in, String out )
            throws IOException
    {
        assertNotNull( "in", in );
        assertNotNull( "out", out );
        copyFile( in, out );
        delete( in );
        return out;
    }

    /**
     * Returns the textual contents of the specified file.
     */
    static public String fileContents( String path )
            throws IOException
    {
        StringBuilder stringBuffer = new StringBuilder( 4096 );
        FileReader reader = new FileReader( path );
        try
        {
            char[] buffer = new char[2048];
            while ( true )
            {
                int length = reader.read( buffer );
                if ( length == -1 )
                {
                    break;
                }
                stringBuffer.append( buffer, 0, length );
            }
        }
        finally
        {
            dispose( reader );
        }
        return stringBuffer.toString();
    }

    /**
     * Returns only the filename portion of the specified path.
     */
    static public String fileName( String path )
    {
        return new File( canonical( path ) ).getName();
    }

    /**
     * Returns the parent directory of the specified path.
     */
    static public String parent( String path )
    {
        return new File( canonical( path ) ).getParent();
    }

    /**
     * Returns only the extension portion of the specified path, or an empty string if there is no extension.
     */
    static public String fileExtension( String file )
    {
        file = assertNotEmpty( "file", file );
        int commaIndex = file.indexOf( '.' );
        return (commaIndex == -1) ? "" : file.substring( commaIndex + 1 );
    }

    /**
     * Returns only the filename portion of the specified path, without the extension, if any.
     */
    static public String fileWithoutExtension( String file )
    {
        file = assertNotEmpty( "file", file );
        int commaIndex = file.indexOf( '.' );
        if ( commaIndex == -1 )
        {
            commaIndex = file.length();
        }
        int slashIndex = file.replace( '\\', '/' ).lastIndexOf( '/' );
        if ( slashIndex == -1 )
        {
            slashIndex = 0;
        }
        else
        {
            slashIndex++;
        }
        return file.substring( slashIndex, commaIndex );
    }

    /**
     * Returns a substring of the specified text.
     *
     * @param end The end index of the substring. If negative, the index used will be "text.length() + end".
     */
    static public String substring( String text, int start, int end )
    {
        assertNotNull( "text", text );
        assertNotNegative( "start", start );
        return text.substring( start, (end >= 0) ? end : text.length() + end );
    }

    /**
     * Splits the specified command at spaces that are not surrounded by quotes and passes the result to {@link #shell(String...)}.
     */
    static public void shell( String command )
            throws IOException
    {
        List<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile( "[^\\s\"']+|\"([^\"]*)\"|'([^']*)'" );
        Matcher regexMatcher = regex.matcher( command );
        while ( regexMatcher.find() )
        {
            if ( regexMatcher.group( 1 ) != null )
            {
                matchList.add( regexMatcher.group( 1 ) );
            }
            else if ( regexMatcher.group( 2 ) != null )
            {
                matchList.add( regexMatcher.group( 2 ) );
            }
            else
            {
                matchList.add( regexMatcher.group() );
            }
        }
        shell( matchList.toArray( new String[matchList.size()] ) );
    }

    /**
     * Executes the specified shell command. {@link #resolvePath(String)} is used to locate the file to execute. If not found, on
     * Windows the same filename with an "exe" extension is also tried.
     */
    static public void shell( String... command )
            throws IOException
    {
        assertNotEmpty( "command", command );

        String originalCommand = (command[0] = assertNotEmpty( "shell Command", command[0] ));
        command[0] = resolvePath( command[0] );
        if ( !fileExists( command[0] ) && isWindows )
        {
            command[0] = resolvePath( command[0] + ".exe" );
            if ( !fileExists( command[0] ) )
            {
                command[0] = originalCommand;
            }
        }

        if ( LOGGER.trace.isEnabled() )
        {
            StringBuilder buffer = new StringBuilder( 256 );
            for ( String text : command )
            {
                buffer.append( text );
                buffer.append( ' ' );
            }
            LOGGER.trace.log( "Executing command: ", buffer );
        }

        Process process = new ProcessBuilder( command ).start();
        // try {
        // process.waitFor();
        // } catch (InterruptedException ignored) {
        // }
        BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
        while ( true )
        {
            String line = reader.readLine();
            if ( line == null )
            {
                break;
            }
            System.out.println( line );
        }
        reader.close();
        reader = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
        while ( true )
        {
            String line = reader.readLine();
            if ( line == null )
            {
                break;
            }
            System.out.println( line );
        }
        if ( process.exitValue() != 0 )
        {
            StringBuilder buffer = new StringBuilder( 256 );
            for ( String text : command )
            {
                buffer.append( text );
                buffer.append( ' ' );
            }
            throw new RuntimeException( "Error executing command: " + buffer );
        }
    }

    /**
     * Creates a new keystore for signing JARs. If the keystore file already exists, no action will be taken.
     *
     * @return The path to the keystore file.
     */
    static public String keystore( String keystoreFile, String alias, String password, String company, String title )
            throws IOException
    {
        if ( fileExists( keystoreFile = assertNotEmpty( "keystoreFile", keystoreFile ) ) )
        {
            return keystoreFile;
        }
        alias = assertNotEmpty( "alias", alias );
        company = assertNotEmpty( "company", company );
        title = assertNotEmpty( "title", title );
        if ( (password = assertNotEmpty( "password", password )).length() < 6 )
        {
            throw new IllegalArgumentException( "password must be 6 or more characters." );
        }
        LOGGER.debug.log( "Creating keystore (", alias, ":", password, ", ", company, ", ", title, "): ", keystoreFile );

        File file = new File( keystoreFile );
        file.delete();
        Process process = Runtime.getRuntime().exec( new String[]{resolvePath( "keytool" ), "-genkey", "-keystore", keystoreFile, "-alias", alias} );
        OutputStreamWriter writer = new OutputStreamWriter( process.getOutputStream() );
        writer.write( password + "\n" ); // Enter keystore password:
        writer.write( password + "\n" ); // Re-enter new password:
        writer.write( company + "\n" ); // What is your first and last name?
        writer.write( title + "\n" ); // What is the name of your organizational unit?
        writer.write( title + "\n" ); // What is the name of your organization?
        writer.write( "\n" ); // What is the name of your City or Locality? [Unknown]
        writer.write( "\n" ); // What is the name of your State or Province? [Unknown]
        writer.write( "\n" ); // What is the two-letter country code for this unit? [Unknown]
        writer.write( "yes\n" ); // Correct?
        writer.write( "\n" ); // Return if same alias key password as keystore.
        writer.flush();
        process.getOutputStream().close();
        process.getInputStream().close();
        process.getErrorStream().close();
        try
        {
            process.waitFor();
        }
        catch ( InterruptedException ignored )
        {
        }
        if ( !file.exists() )
        {
            throw new RuntimeException( "Error creating keystore." );
        }
        return keystoreFile;
    }

    public static Class compileDynamicCodeToClass( int pOverheadStartLines, final String pCode, final URL... pClasspathURLs )
            throws ClassNotFoundException
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if ( compiler == null )
        {
            throw new RuntimeException( "No compiler available. Ensure you are running from a 1.6+ JDK, and not a JRE." );
        }

        final ByteArrayOutputStream output = new ByteArrayOutputStream( 32 * 1024 );
        final SimpleJavaFileObject javaObject = new SimpleJavaFileObject( URI.create( "Generated.java" ), JavaFileObject.Kind.SOURCE )
        {
            @Override
            public OutputStream openOutputStream()
            {
                return output;
            }

            @Override
            public CharSequence getCharContent( boolean ignoreEncodingErrors )
            {
                return pCode;
            }
        };
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        //noinspection unchecked
        compiler.getTask( null, new ForwardingJavaFileManager( compiler.getStandardFileManager( null, null, null ) )
        {
            @Override
            public JavaFileObject getJavaFileForOutput( Location location, String className, JavaFileObject.Kind kind, FileObject sibling )
            {
                return javaObject;
            }
        }, diagnostics, null, null, Arrays.asList( javaObject ) ).call();

        if ( !diagnostics.getDiagnostics().isEmpty() )
        {
            StringBuilder buffer = new StringBuilder( 1024 );
            for ( Diagnostic diagnostic : diagnostics.getDiagnostics() )
            {
                if ( buffer.length() > 0 )
                {
                    buffer.append( "\n" );
                }
                buffer.append( "Line " );
                buffer.append( diagnostic.getLineNumber() - pOverheadStartLines );
                buffer.append( ": " );
                buffer.append( diagnostic.getMessage( null ).replaceAll( "^Generated.java:\\d+:\\d* ", "" ) );
            }
            throw new RuntimeException( "Compilation errors:\n" + buffer );
        }

        // Load class.
        return new URLClassLoader( pClasspathURLs, Scar.class.getClassLoader() )
        {
            @Override
            protected synchronized Class<?> loadClass( String name, boolean resolve )
                    throws ClassNotFoundException
            {
                // Look in this classloader before the parent.
                Class c = findLoadedClass( name );
                if ( c == null )
                {
                    try
                    {
                        c = findClass( name );
                    }
                    catch ( ClassNotFoundException e )
                    {
                        return super.loadClass( name, resolve );
                    }
                }
                if ( resolve )
                {
                    resolveClass( c );
                }
                return c;
            }

            @Override
            protected Class<?> findClass( String name )
                    throws ClassNotFoundException
            {
                if ( name.equals( "Generated" ) )
                {
                    byte[] bytes = output.toByteArray();
                    return defineClass( name, bytes, 0, bytes.length );
                }
                return super.findClass( name );
            }
        }.loadClass( "Generated" );
    }
}
