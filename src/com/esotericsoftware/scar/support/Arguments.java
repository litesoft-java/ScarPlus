package com.esotericsoftware.scar.support;

import java.util.*;

import com.esotericsoftware.utils.*;

/**
 * Stores command line arguments as 'String' name/value pairs. Arguments containing an equals sign are considered a name/value pair. All
 * other arguments are stored as a name/value pair with a null value.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class Arguments
{
    private final Map<String, String> mParameters = new LinkedHashMap<String, String>();

    public Arguments()
    {
    }

    public Arguments( String[] pArgs )
    {
        this( pArgs, 0 );
    }

    public Arguments( String[] pArgs, int pIndex )
    {
        while ( pIndex < pArgs.length )
        {
            String zArg = pArgs[pIndex++];
            int at = zArg.indexOf( '=' );
            if ( at == -1 )
            {
                set( zArg );
            }
            else
            {
                set( zArg.substring( 0, at ), zArg.substring( at + 1 ).trim() );
            }
        }
    }

    /**
     * Returns true if the argument was specified.
     */
    public boolean has( String pName )
    {
        return mParameters.containsKey( normalizeName( pName ) );
    }

    /**
     * Returns the value of the argument with the specified Name, or null if the argument was specified without a value or was not
     * specified.
     */
    public String get( String pName )
    {
        return mParameters.get( normalizeName( pName ) );
    }

    public void set( String pName, String pValue )
    {
        mParameters.put( normalizeName( pName ), pValue );
    }

    public String remove( String pName )
    {
        return mParameters.remove( normalizeName( pName ) );
    }

    public int count()
    {
        return mParameters.size();
    }

    public void clear()
    {
        mParameters.clear();
    }

    /**
     * Returns the value of the argument with the specified Name, or the specified default value if the argument was specified
     * without a value or was not specified.
     */
    public String get( String pName, String pDefaultValue )
    {
        String zValue = get( pName );
        return (zValue != null) ? zValue : pDefaultValue;
    }

    public void set( String pName )
    {
        set( pName, null );
    }

    private String normalizeName( String pName )
    {
        return Util.assertNotEmpty( "Name", pName ).toLowerCase();
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 100 );
        for ( String param : mParameters.keySet() )
        {
            if ( buffer.length() > 1 )
            {
                buffer.append( ' ' );
            }
            buffer.append( param );
            String value = get( param );
            if ( value != null )
            {
                buffer.append( '=' );
                buffer.append( value );
            }
        }
        return buffer.toString();
    }
}
