package com.esotericsoftware.wildcard;

public interface FileNameMatcher
{
    /**
     * return True if the file name specified with <code>fileName</code> is acceptable (NO path may be specified)
     *
     * @param filePath !null and !empty and trim()'d (NO path may be specified)
     */
    public boolean acceptable( String fileName );
}
