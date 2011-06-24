package com.esotericsoftware.scar.support;

import java.util.*;

import com.esotericsoftware.utils.*;

@SuppressWarnings({"UnusedDeclaration"})
public class Parameter
{
    public enum Form
    {
        STRING, STRING_LIST, PATHS
    }

    public static Parameter def( String pName, Form pForm, String pDescription, String pDescriptionForDefaulting )
    {
        return new Parameter( pName, pForm, pDescription, pDescriptionForDefaulting );
    }

    public static Parameter def( String pName, Form pForm, String pDescription )
    {
        return def( pName, pForm, pDescription, null );
    }

    public static Set<String> reservedNames()
    {
        return Collections.unmodifiableSet( RESERVED_NAMES );
    }

    public String getName()
    {
        return mName;
    }

    public Form getForm()
    {
        return mForm;
    }

    public String getDescription()
    {
        return mDescription;
    }

    public String getDescriptionForDefaulting()
    {
        return mDescriptionForDefaulting;
    }

    private final String mName;
    private final Form mForm;
    private final String mDescription;
    private final String mDescriptionForDefaulting;

    private Parameter( String pName, Form pForm, String pDescription, String pDescriptionForDefaulting )
    {
        mName = Util.assertNotEmpty( "Name", pName );
        mForm = pForm;
        mDescription = pDescription;
        mDescriptionForDefaulting = pDescriptionForDefaulting;

        if ( !RESERVED_NAMES.add( mName ) )
        {
            throw new IllegalArgumentException( "Duplicate Parameter declared with name: " + mName );
        }
    }

    private static final Set<String> RESERVED_NAMES = new HashSet<String>();
}
