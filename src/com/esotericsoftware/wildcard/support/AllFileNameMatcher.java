package com.esotericsoftware.wildcard.support;

import com.esotericsoftware.wildcard.*;

public class AllFileNameMatcher implements FileNameMatcher
{
    public static final FileNameMatcher INSTANCE = new AllFileNameMatcher();

    private AllFileNameMatcher()
    {
    }

    /**
     * return True if the file name specified with <code>fileName</code> is acceptable (NO path may be specified)
     *
     * @param filePath !null and !empty and trim()'d (NO path may be specified)
     */
    @Override
    public boolean acceptable( String fileName )
    {
        return true;
    }
}
