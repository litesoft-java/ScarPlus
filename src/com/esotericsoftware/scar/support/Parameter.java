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

    public static class Manager
    {
        private final Map<Object, Object> mData = new HashMap<Object, Object>();
        private final Map<Object, Object> mCachedResponses = new HashMap<Object, Object>();

        public Manager( Manager them )
        {
            this( them.mData );
        }

        public Manager( Map<Object, Object> pData )
        {
            if ( pData != null )
            {
                Object zValue;
                for ( Object key : pData.keySet() )
                {
                    if ( null != (zValue = normalizeValue( pData.get( key ) )) )
                    {
                        mData.put( normalizeKey( key ), zValue );
                    }
                }
            }
        }

        public Object normalizeKey( Object pKey )
        {
            if ( pKey instanceof String )
            {
                pKey = Util.noEmpty( pKey.toString().toLowerCase() );
            }
            Util.assertNotNull( "key", pKey );
            return pKey;
        }

        public Object normalizeValue( Object pValue )
        {
            return (pValue instanceof String) ? Util.noEmpty( pValue.toString() ) : pValue;
        }

        public synchronized <T> T getCachedResponse( Object pKey )
        {
            //noinspection unchecked
            return (T) mCachedResponses.get( pKey );
        }

        public synchronized void addCachedResponse( Object pKey, Object pValue )
        {
            mCachedResponses.put( pKey, pValue );
        }

        public synchronized void put( Object pKey, Object pValue )
        {
            if ( pValue != null )
            {
                mData.put( pKey, pValue );
            }
            else if ( null == mData.remove( pKey ) )
            {
                return; // Nothing Changed
            }
            mCachedResponses.remove( pKey );
        }

        /**
         * Removes an item from a list or map. If the mData under the specified key is a list, the entry equal to the specified value is
         * removed. If the mData under the specified key is a map, the entry with the key specified by value is removed.
         */
        public synchronized void remove( Object pKey, Object pValue )
        {
            boolean zUpdate;
            Object object = mData.get( pKey );
            if ( object instanceof Map )
            {
                zUpdate = (null != ((Map) object).remove( pValue ));
            }
            else if ( object instanceof List )
            {
                zUpdate = ((List) object).remove( pValue );
            }
            else
            {
                zUpdate = (null != mData.remove( pKey ));
            }
            if ( zUpdate )
            {
                mCachedResponses.remove( pKey );
            }
        }

        public synchronized Object get( Object pKey )
        {
            return mData.get( pKey );
        }

        public synchronized Object[] keys()
        {
            return mData.keySet().toArray();
        }
    }
}
