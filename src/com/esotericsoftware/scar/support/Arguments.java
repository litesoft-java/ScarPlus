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
    public static class NameValuePair
    {
        private String mName, mValue;

        public NameValuePair( String pName, String pValue )
        {
            mName = pName;
            mValue = pValue;
        }

        public String getName()
        {
            return mName;
        }

        public String getValue()
        {
            return mValue;
        }
    }

    private final Map<String, String> mParameters = new LinkedHashMap<String, String>();

    public Arguments()
    {
    }

    public Arguments( String[] pArgs )
    {
        for ( String zArg : pArgs )
        {
            int at = zArg.indexOf( '=' );
            if ( at == -1 )
            {
                set( zArg, "" );
            }
            else
            {
                set( zArg.substring( 0, at ), zArg.substring( at + 1 ).trim() );
            }
        }
    }

    private void set( String pName, String pValue )
    {
        mParameters.put( normalizeName( pName ), pValue );
    }

    /**
     * Get (and remove if there) the 'Next' Name/Value.
     *
     * Returns null means no more.
     */
    public NameValuePair getNext()
    {
        if ( mParameters.isEmpty() )
        {
            return null;
        }
        String zName = mParameters.keySet().iterator().next();
        return new NameValuePair( zName, get( zName ) );
    }

    /**
     * Get (and remove if there) the value assocciated w/ pName.
     *
     * Returns the value of the argument with the specified Name,
     *      or "" if the argument was specified without a value,
     *      or null if it was not specified.
     */
    public String get( String pName )
    {
        return mParameters.remove( normalizeName( pName ) );
    }

    /**
     * Get (and remove if there) the value assocciated w/ pName.
     *
     * Returns the value of the argument with the specified Name,
     *      or "" if the argument was specified without a value,
     *      or pDefaultValue if it was not specified.
     */
    public String get( String pName, String pDefaultValue )
    {
        String zValue = get( pName );
        return (zValue != null) ? zValue : pDefaultValue;
    }

    public int count()
    {
        return mParameters.size();
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
            if ( "".equals( value ) )
            {
                buffer.append( '=' );
                buffer.append( value );
            }
        }
        return buffer.toString();
    }
}
