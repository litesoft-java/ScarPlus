package com.esotericsoftware.scar.support;

public class Parameter
{
    public static Parameter def( String pName, Class<?> pType, String pDescription )
    {
        return def( pName, pType, pDescription, null );
    }

    public static Parameter def( String pName, Class<?> pType, String pDescription, String pDescriptionForDefaulting )
    {
        return new Parameter( pName, pType, pDescription, pDescriptionForDefaulting );
    }

    public String getName()
    {
        return mName;
    }

    public Class<?> getType()
    {
        return mType;
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
    private final Class<?> mType;
    private final String mDescription;
    private final String mDescriptionForDefaulting;

    private Parameter( String pName, Class<?> pType, String pDescription, String pDescriptionForDefaulting )
    {
        mName = pName;
        mType = pType;
        mDescription = pDescription;
        mDescriptionForDefaulting = pDescriptionForDefaulting;
    }
}
