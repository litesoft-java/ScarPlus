package com.esotericsoftware.wildcard.support;

import com.esotericsoftware.wildcard.*;

public class WildFileNameMatcher implements FileNameMatcher
{
    private final FilePathPartMatcher mFilePathPartMatcher;

    public WildFileNameMatcher( String pFileNamePattern )
    {
        mFilePathPartMatcher = new WildCardPatternFilePathPartMatcher( pFileNamePattern );
    }

    /**
     * return True if the file name specified with <code>fileName</code> is acceptable (NO path may be specified)
     *
     * @param filePath !null and !empty and trim()'d (NO path may be specified)
     */
    @Override
    public boolean acceptable( String fileName )
    {
        return !fileName.contains( "/" ) && mFilePathPartMatcher.acceptable( fileName );
    }
}
