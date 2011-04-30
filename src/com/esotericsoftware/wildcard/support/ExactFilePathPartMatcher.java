package com.esotericsoftware.wildcard.support;

public class ExactFilePathPartMatcher implements FilePathPartMatcher
{
    private final String mPart;

    public ExactFilePathPartMatcher( String pPart )
    {
        if ( (mPart = pPart.trim()).length() == 0 )
        {
            throw new IllegalArgumentException( "Exact Part May NOT be empty!" );
        }
    }

    @Override
    public boolean acceptsAnyNumberOfParts()
    {
        return false;
    }

    @Override
    public boolean acceptable( String pFilePathPart )
    {
        return mPart.equals( pFilePathPart );
    }
}
