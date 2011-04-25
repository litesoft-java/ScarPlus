package com.esotericsoftware.wildcard;

public interface DirMatcher
{
    /**
     * return True if the directory specified with <code>dirPath</code> <i>could possibly</i> host directories that <i>could</i> host files acceptable to this Pattern
     *
     * @param dirPath !null and path separators converted to '/'
     */
    public boolean acceptableParentDir( String dirPath );

    /**
     * return True if the directory specified with <code>dirPath</code> is acceptable
     * @param dirPath !null and path separators converted to '/'
     */
    public boolean acceptable( String dirPath );
}
