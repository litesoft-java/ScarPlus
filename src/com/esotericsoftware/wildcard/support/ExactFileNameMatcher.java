package com.esotericsoftware.wildcard.support;

import com.esotericsoftware.wildcard.*;

public class ExactFileNameMatcher implements FileNameMatcher
{
    private String mFileNameToMatch;

    public ExactFileNameMatcher( String pFileNameToMatch )
    {
        if ( (mFileNameToMatch = pFileNameToMatch).contains( "/" ) )
        {
            throw new IllegalArgumentException( "FileNames may NOT contain a '/'" );
        }
    }

    /**
     * return True if the file name specified with <code>fileName</code> is acceptable (NO path may be specified)
     *
     * @param filePath !null and !empty and trim()'d (NO path may be specified)
     */
    @Override
    public boolean acceptable( String fileName )
    {
        return mFileNameToMatch.equals( fileName );
    }
}
