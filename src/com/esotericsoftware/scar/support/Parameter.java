package com.esotericsoftware.scar.support;

public class Parameter
{
    public enum Form
    {
        STRING, STRING_LIST
    }

    public static Parameter def( String pName, Form pForm, String pDescription, String pDescriptionForDefaulting )
    {
        return new Parameter( pName, pForm, pDescription, pDescriptionForDefaulting );
    }

    public static Parameter def( String pName, Form pForm, String pDescription )
    {
        return def( pName, pForm, pDescription, null );
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
        mName = pName;
        mForm = pForm;
        mDescription = pDescription;
        mDescriptionForDefaulting = pDescriptionForDefaulting;
    }
}
