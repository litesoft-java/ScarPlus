package com.esotericsoftware.wildcard.support;

public interface FilePathPartMatcher
{
    public static final java.util.regex.Pattern SLASH = java.util.regex.Pattern.compile( "/" );

    public boolean acceptsAnyNumberOfParts();

    public boolean acceptsAnything();

    public boolean acceptable( String pFilePathPart );
}
