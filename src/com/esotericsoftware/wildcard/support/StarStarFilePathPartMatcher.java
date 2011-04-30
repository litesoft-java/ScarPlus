package com.esotericsoftware.wildcard.support;

public class StarStarFilePathPartMatcher implements FilePathPartMatcher
{
    public static final FilePathPartMatcher INSTANCE = new StarStarFilePathPartMatcher();

    private StarStarFilePathPartMatcher()
    {
    }

    @Override
    public boolean acceptsAnyNumberOfParts()
    {
        return true;
    }

    @Override
    public boolean acceptable( String pFilePathPart )
    {
        return true;
    }
}
