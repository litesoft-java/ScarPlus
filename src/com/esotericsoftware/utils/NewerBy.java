package com.esotericsoftware.utils;

public class NewerBy
{
    public static final long MILLISECS_IN_SEC = 1000;
    public static final long MILLISECS_IN_MIN = 60 * MILLISECS_IN_SEC;
    public static final long MILLISECS_IN_HOUR = 60 * MILLISECS_IN_MIN;
    public static final long MILLISECS_IN_DAY = 24 * MILLISECS_IN_HOUR;
    public static final long MILLISECS_IN_SHORT_MONTH = 28 * MILLISECS_IN_DAY;
    public static final long MILLISECS_IN_LONG_MONTH = 31 * MILLISECS_IN_DAY;
    public static final long MILLISECS_IN_YEAR = (long)(365.242 * MILLISECS_IN_DAY);

    public static final long MILLISECS_IN_AVERAGE_MONTH = MILLISECS_IN_YEAR / 12;
    public static final long MILLISECS_IN_TWO_AVERAGE_MONTHS = MILLISECS_IN_AVERAGE_MONTH + MILLISECS_IN_AVERAGE_MONTH;

    private final Long mFromTimeMillis;
    private final Long mToTimeMillis;
    private final Boolean mNewer;

    public NewerBy( Long pFromTimeMillis, Long pToTimeMillis )
    {
        mFromTimeMillis = pFromTimeMillis;
        mToTimeMillis = pToTimeMillis;
        if ( (pFromTimeMillis == null) || (pToTimeMillis == null) )
        {
            mNewer = null;
        }
        else
        {
            mNewer = (pToTimeMillis > pFromTimeMillis);
        }
    }

    public boolean isNewer()
    {
        return Boolean.TRUE.equals( mNewer );
    }

    public Long getFromTimeMillis()
    {
        return mFromTimeMillis;
    }

    public Long getToTimeMillis()
    {
        return mToTimeMillis;
    }

    @Override
    public String toString()
    {
        if ( mNewer == null )
        {
            return "???";
        }
        if ( mToTimeMillis.equals( mFromTimeMillis ) )
        {
            return "No Difference";
        }
        StringBuilder sb = new StringBuilder();
        long zDelta;
        if ( mNewer )
        {
            sb.append( "Newer" );
            zDelta = mToTimeMillis - mFromTimeMillis;
        }
        else
        {
            sb.append( "Older" );
            zDelta = mFromTimeMillis - mToTimeMillis;
        }
        sb.append( " by " );
        if ( (zDelta < 0) || (MILLISECS_IN_YEAR < zDelta) )
        {
            sb.append( "more than a year" );
        }
        else if ( MILLISECS_IN_TWO_AVERAGE_MONTHS <= zDelta )
        {
            check( sb, zDelta, MILLISECS_IN_AVERAGE_MONTH, "month" );
        }
        else if ( MILLISECS_IN_LONG_MONTH < zDelta )
        {
            sb.append( "more than a month" );
        }
        else if ( MILLISECS_IN_SHORT_MONTH < zDelta )
        {
            sb.append( "about a month" );
        }
        else if ( !check( sb, zDelta, MILLISECS_IN_DAY, "day" ) )
        {
            if ( !check( sb, zDelta, MILLISECS_IN_HOUR, "hour" ) )
            {
                if ( !check( sb, zDelta, MILLISECS_IN_MIN, "minute" ) )
                {
                    if ( !check( sb, zDelta, MILLISECS_IN_SEC, "second" ) )
                    {
                        sb.append( zDelta ).append( "ms" );
                    }
                }
            }
        }
        return sb.toString();
    }

    private boolean check( StringBuilder sb, long pDelta, long pDenominator, String pWhat )
    {
        if ( (pDenominator + pDenominator) <= pDelta )
        {
            sb.append( "about " ).append( roundUp( pDelta, pDenominator ) ).append( " " + pWhat + "s" );
            return true;
        }
        if ( pDenominator < pDelta )
        {
            sb.append( "more than a " + pWhat );
            return true;
        }
        return false;
    }

    private long roundUp( long pNumerator, long pDenominator )
    {
        return (pNumerator + (pDenominator / 2)) / pDenominator;
    }
}
