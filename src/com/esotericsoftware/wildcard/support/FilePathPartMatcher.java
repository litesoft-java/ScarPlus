package com.esotericsoftware.wildcard.support;

public interface FilePathPartMatcher
{
    public boolean acceptsAnyNumberOfParts();

    public boolean acceptsAnything();

    public boolean acceptable( String pFilePathPart );
}
