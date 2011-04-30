package com.esotericsoftware.wildcard.support;

import org.junit.*;

import static org.junit.Assert.*;

public class WildMultiDirMatcherTest
{
    @Test
    public void matcher_dir_be()
    {
        WildMultiDirMatcher zMatcher = new WildMultiDirMatcher( "**", "be", "**" );
        assertFalse( zMatcher.acceptable( "" ) );
        assertFalse( zMatcher.acceptable( "what/ever" ) );
        assertFalse( zMatcher.acceptable( "when/ever" ) );
        assertFalse( zMatcher.acceptable( "bad" ) );
        assertTrue( zMatcher.acceptable( "too/be/or/not/too/be" ) );
        assertTrue( zMatcher.acceptable( "be/too/what" ) );
    }

    @Test
    public void matcher_dir_be_too()
    {
        WildMultiDirMatcher zMatcher = new WildMultiDirMatcher( "**", "be", "**", "too", "**" );
        assertFalse( zMatcher.acceptable( "" ) );
        assertFalse( zMatcher.acceptable( "what/ever" ) );
        assertFalse( zMatcher.acceptable( "when/ever" ) );
        assertFalse( zMatcher.acceptable( "bad" ) );
        assertTrue( zMatcher.acceptable( "too/be/or/not/too/be" ) );
        assertTrue( zMatcher.acceptable( "be/too/what" ) );
    }
}
