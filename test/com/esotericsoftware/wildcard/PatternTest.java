package com.esotericsoftware.wildcard;

import org.junit.*;

import static org.junit.Assert.*;

public class PatternTest
{
    private static final String[] DIRS = {"when/ever", //
                                          "what/ever", //
                                          "too/be/or/not/too/be", //
                                          "be/too/what", //
                                          "bad"};

    @Test
    public void clean()
    {
        assertEquals( "*", Pattern.clean( "*" ) );
        assertEquals( "**/*", Pattern.clean( "**" ) );
        assertEquals( "**/*TA", Pattern.clean( "**TA" ) );
        assertEquals( "**/*TA*/**/*", Pattern.clean( "**TA**" ) );
        assertEquals( "**/*TA/**/*", Pattern.clean( "** / ** / *TA /**/**" ) );
    }

    @Test
    public void matchesEveryThing()
    {
        testPattern( "**/*", justFile( true, true ), //
                     dir0( true, true, true ), //
                     dir1( true, true, true ), //
                     dir2( true, true, true ), //
                     dir3( true, true, true ), //
                     dir4( true, true ) );
    }

    @Test
    public void matchesJava()
    {
        testPattern( "**/*.java", justFile( true, true ), //
                     dir0( true, true, true ), //
                     dir1( true, true, true ), //
                     dir2( true, true, true ), //
                     dir3( true, true, true ), //
                     dir4( true, true ));
    }

    @Test
    public void matchesBaStar_Java()
    {
        testPattern( "ba*/*.java", justFile( false, false ), //
                     dir0( false, false, false ), //
                     dir1( false, false, false ), //
                     dir2( false, false, false ), //
                     dir3( false, false, false ), //
                     dir4( true, true ));
    }

    @Test
    public void matchesBa_Java()
    {
        testPattern( "ba?/*.java", justFile( false, false ), //
                     dir0( false, false, false ), //
                     dir1( false, false, false ), //
                     dir2( false, false, false ), //
                     dir3( false, false, false ), //
                     dir4( true, true ));
    }

    @Test
    public void matchesEver()
    {
        testPattern( "**/ever/**", justFile( false, false ), //
                     dir0( true, true, true ), //
                     dir1( true, true, true ), //
                     dir2( true, false, false ), //
                     dir3( true, false, false ), //
                     dir4( false, false ) );
    }

    @Test
    public void matchesWhen_Java()
    {
        testPattern( "when/**/*.java", justFile( false, false ), //
                     dir0( true, true, true ), //
                     dir1( false, false, false ), //
                     dir2( false, false, false ), //
                     dir3( false, false, false ), //
                     dir4( false, false ) );
    }

    @Test
    public void matchesWhenEverJava()
    {
        testPattern( "when/ever/*.java", justFile( false, false ), //
                     dir0( true, true, true ), //
                     dir1( false, false, false ), //
                     dir2( false, false, false ), //
                     dir3( false, false, false ), //
                     dir4( false, false ) );
    }

    @Test
    public void matchesMultiWildJava()
    {
        testPattern( "**/be/**/too/*/*.java", justFile( false, false ), //
                     dir0( true, false, false ), //
                     dir1( true, false, false ), //
                     dir2( true, true, true ), //
                     dir3( true, true, true ), //
                     dir4( false, false ) );
    }

    private Answers justFile( boolean pDirPathAnswer, boolean pFileNameAnswer )
    {
        return new Answers( null, pDirPathAnswer, pFileNameAnswer, "Dude.java" );
    }

    private Answers dir0( boolean pParentDirsPathAnswer, boolean pDirPathAnswer, boolean pFileNameAnswer )
    {
        return new Answers( pParentDirsPathAnswer, pDirPathAnswer, pFileNameAnswer, DIRS[0] + "/Dude.java" );
    }

    private Answers dir1( boolean pParentDirsPathAnswer, boolean pDirPathAnswer, boolean pFileNameAnswer )
    {
        return new Answers( pParentDirsPathAnswer, pDirPathAnswer, pFileNameAnswer, DIRS[1] + "/Dude.java" );
    }

    private Answers dir2( boolean pParentDirsPathAnswer, boolean pDirPathAnswer, boolean pFileNameAnswer )
    {
        return new Answers( pParentDirsPathAnswer, pDirPathAnswer, pFileNameAnswer, DIRS[2] + "/Dude.java" );
    }

    private Answers dir3( boolean pParentDirsPathAnswer, boolean pDirPathAnswer, boolean pFileNameAnswer )
    {
        return new Answers( pParentDirsPathAnswer, pDirPathAnswer, pFileNameAnswer, DIRS[3] + "/Dude.java" );
    }

    private Answers dir4( boolean pDirPathAnswer, boolean pFileNameAnswer )
    {
        return new Answers( null, pDirPathAnswer, pFileNameAnswer, DIRS[4] + "/Dude.java" );
    }

    private void testPattern( String pPattern, Answers... pAnswers )
    {
        Pattern zPattern = new Pattern( pPattern );
        for ( Answers zAnswer : pAnswers )
        {
            zAnswer.test( zPattern );
        }
    }

    private static class Answers
    {
        private final Boolean mParentDirsPathAnswer;
        private final boolean mDirPathAnswer;
        private final boolean mFileNameAnswer;
        private final String[] mFilePathParts;

        public Answers( Boolean pParentDirsPathAnswer, boolean pDirPathAnswer, boolean pFileNameAnswer, String pFilePath )
        {
            mParentDirsPathAnswer = pParentDirsPathAnswer;
            mDirPathAnswer = pDirPathAnswer;
            mFileNameAnswer = pFileNameAnswer;
            mFilePathParts = pFilePath.contains( "/" ) ? pFilePath.split( "/" ) : new String[]{"", pFilePath};
            if ( (mParentDirsPathAnswer == null) && (mFilePathParts.length > 2) )
            {
                fail( "No Parent Dir Check, but were Parent Dirs: " + pFilePath );
            }
        }

        public void test( Pattern zPattern )
        {
            String filePath = "";
            int i = 0;
            if ( mParentDirsPathAnswer != null )
            {
                while ( i < (mFilePathParts.length - 2) )
                {
                    filePath = append( filePath, mFilePathParts[i++] );
                    boolean zActual = zPattern.acceptableParentDirPath( filePath );
                    if ( mParentDirsPathAnswer != zActual )
                    {
                        fail( "Expected " + mParentDirsPathAnswer + ", but got " + zActual + " w/ Pattern( \"" + zPattern + "\" ).acceptableParentDirPath( \"" + filePath + "\" )" );
                    }
                }
            }
            filePath = append( filePath, mFilePathParts[i++] );
            boolean zActual = zPattern.acceptableDirPath( filePath );
            if ( mDirPathAnswer != zActual )
            {
                fail( "Expected " + mDirPathAnswer + ", but got " + zActual + " w/ Pattern( \"" + zPattern + "\" ).acceptableDirPath( \"" + filePath + "\" )" );
            }
            filePath = append( filePath, mFilePathParts[i] );
            zActual = zPattern.matchesFilePath( filePath );
            if ( mFileNameAnswer != zActual )
            {
                fail( "Expected " + mFileNameAnswer + ", but got " + zActual + " w/ Pattern( \"" + zPattern + "\" ).matchesFilePath( \"" + filePath + "\" )" );
            }
        }

        private String append( String pPath, String pPart )
        {
            return (pPath.length() == 0) ? pPart : pPath + "/" + pPart;
        }
    }
}
