package com.esotericsoftware.wildcard;

import org.junit.*;

import static org.junit.Assert.*;

public class PatternTest
{
    @Test
    public void clean()
    {
        assertEquals( "*", Pattern.clean( "" ) );
        assertEquals( "*", Pattern.clean( "*" ) );
        assertEquals( "**/*", Pattern.clean( "**" ) );
        assertEquals( "**/*TA", Pattern.clean( "**TA" ) );
        assertEquals( "**/*TA*/**/*", Pattern.clean( "**TA**" ) );
        assertEquals( "**/*TA/**/*", Pattern.clean( "** / ** / *TA /**/**" ) );
        assertEquals( "**/*TA/**/*", Pattern.clean( "**? / ** / ?*TA /**?/??*?" ) );
    }

    @Test
    public void excludeDirFileSupport()
    {
        Pattern zPattern = new Pattern( "**/be/**/*.java" );
        new ExcludeAnswers( "Dude.java", false, false ).test( zPattern );
        new ExcludeAnswers( "when/ever/Dude.java", false, false, false ).test( zPattern );
        new ExcludeAnswers( "what/ever/Dude.java", false, false, false ).test( zPattern );
        new ExcludeAnswers( "too/be/or/not/too/be/Dude.java", false, false, false, false, false, false, true ).test( zPattern );
        new ExcludeAnswers( "be/too/what/Dude.java", false, false, false, true ).test( zPattern );
        new ExcludeAnswers( "bad/Dude.java", false, false ).test( zPattern );
    }

    @Test
    public void excludeFileSupport()
    {
        Pattern zPattern = new Pattern( "**/*.java" );
        new ExcludeAnswers( "Dude.java", false, true ).test( zPattern );
        new ExcludeAnswers( "when/ever/Dude.java", false, false, true ).test( zPattern );
        new ExcludeAnswers( "what/ever/Dude.java", false, false, true ).test( zPattern );
        new ExcludeAnswers( "too/be/or/not/too/be/Dude.java", false, false, false, false, false, false, true ).test( zPattern );
        new ExcludeAnswers( "be/too/what/Dude.java", false, false, false, true ).test( zPattern );
        new ExcludeAnswers( "bad/Dude.java", false, true ).test( zPattern );
    }

    @Test
    public void excludeDirSupport()
    {
        Pattern zPattern = new Pattern( "**/be/**" );
        new ExcludeAnswers( "Dude.java", false, false ).test( zPattern );
        new ExcludeAnswers( "when/ever/Dude.java", false, false, false ).test( zPattern );
        new ExcludeAnswers( "what/ever/Dude.java", false, false, false ).test( zPattern );
        new ExcludeAnswers( "too/be/or/not/too/be/Dude.java", false, true ).test( zPattern );
        new ExcludeAnswers( "be/too/what/Dude.java", true ).test( zPattern );
        new ExcludeAnswers( "bad/Dude.java", false, false ).test( zPattern );
    }

    @Test
    public void matchesEveryThing()
    {
        Pattern zPattern = new Pattern( "**/*" );
        new Answers( "Dude.java", null, true, true ).test( zPattern );
        new Answers( "when/ever/Dude.java", true, true, true ).test( zPattern );
        new Answers( "what/ever/Dude.java", true, true, true ).test( zPattern );
        new Answers( "too/be/or/not/too/be/Dude.java", true, true, true ).test( zPattern );
        new Answers( "be/too/what/Dude.java", true, true, true ).test( zPattern );
        new Answers( "bad/Dude.java", null, true, true ).test( zPattern );
    }

    @Test
    public void matchesJava()
    {
        Pattern zPattern = new Pattern( "**/*.java" );
        new Answers( "Dude.java", null, true, true ).test( zPattern );
        new Answers( "when/ever/Dude.java", true, true, true ).test( zPattern );
        new Answers( "what/ever/Dude.java", true, true, true ).test( zPattern );
        new Answers( "too/be/or/not/too/be/Dude.java", true, true, true ).test( zPattern );
        new Answers( "be/too/what/Dude.java", true, true, true ).test( zPattern );
        new Answers( "bad/Dude.java", null, true, true ).test( zPattern );
    }

    @Test
    public void matchesBaStar_Java()
    {
        Pattern zPattern = new Pattern( "ba*/*.java" );
        new Answers( "Dude.java", null, false, false ).test( zPattern );
        new Answers( "when/ever/Dude.java", false, false, false ).test( zPattern );
        new Answers( "what/ever/Dude.java", false, false, false ).test( zPattern );
        new Answers( "too/be/or/not/too/be/Dude.java", false, false, false ).test( zPattern );
        new Answers( "be/too/what/Dude.java", false, false, false ).test( zPattern );
        new Answers( "bad/Dude.java", null, true, true ).test( zPattern );
    }

    @Test
    public void matchesBa_Java()
    {
        Pattern zPattern = new Pattern( "ba?/*.java" );
        new Answers( "Dude.java", null, false, false ).test( zPattern );
        new Answers( "when/ever/Dude.java", false, false, false ).test( zPattern );
        new Answers( "what/ever/Dude.java", false, false, false ).test( zPattern );
        new Answers( "too/be/or/not/too/be/Dude.java", false, false, false ).test( zPattern );
        new Answers( "be/too/what/Dude.java", false, false, false ).test( zPattern );
        new Answers( "bad/Dude.java", null, true, true ).test( zPattern );
    }

    @Test
    public void matchesEver()
    {
        Pattern zPattern = new Pattern( "**/ever/**" );
        new Answers( "Dude.java", null, false, false ).test( zPattern );
        new Answers( "when/ever/Dude.java", true, true, true ).test( zPattern );
        new Answers( "what/ever/Dude.java", true, true, true ).test( zPattern );
        new Answers( "too/be/or/not/too/be/Dude.java", true, false, false ).test( zPattern );
        new Answers( "be/too/what/Dude.java", true, false, false ).test( zPattern );
        new Answers( "bad/Dude.java", null, false, false ).test( zPattern );
    }

    @Test
    public void matchesWhen_Java()
    {
        Pattern zPattern = new Pattern( "when/**/*.java" );
        new Answers( "Dude.java", null, false, false ).test( zPattern );
        new Answers( "when/ever/Dude.java", true, true, true ).test( zPattern );
        new Answers( "what/ever/Dude.java", false, false, false ).test( zPattern );
        new Answers( "too/be/or/not/too/be/Dude.java", false, false, false ).test( zPattern );
        new Answers( "be/too/what/Dude.java", false, false, false ).test( zPattern );
        new Answers( "bad/Dude.java", null, false, false ).test( zPattern );
    }

    @Test
    public void matchesWhenEverJava()
    {
        Pattern zPattern = new Pattern( "when/ever/*.java" );
        new Answers( "Dude.java", null, false, false ).test( zPattern );
        new Answers( "when/ever/Dude.java", true, true, true ).test( zPattern );
        new Answers( "what/ever/Dude.java", false, false, false ).test( zPattern );
        new Answers( "too/be/or/not/too/be/Dude.java", false, false, false ).test( zPattern );
        new Answers( "be/too/what/Dude.java", false, false, false ).test( zPattern );
        new Answers( "bad/Dude.java", null, false, false ).test( zPattern );
    }

    @Test
    public void matchesMultiWildJava()
    {
        Pattern zPattern = new Pattern( "**/be/**/too/*/*.java" );
        new Answers( "Dude.java", null, false, false ).test( zPattern );
        new Answers( "when/ever/Dude.java", true, false, false ).test( zPattern );
        new Answers( "what/ever/Dude.java", true, false, false ).test( zPattern );
        new Answers( "too/be/or/not/too/be/Dude.java", true, true, true ).test( zPattern );
        new Answers( "be/too/what/Dude.java", true, true, true ).test( zPattern );
        new Answers( "bad/Dude.java", null, false, false ).test( zPattern );
    }

    private static class Answers
    {
        private final Boolean mParentDirsPathAnswer;
        private final boolean mDirPathAnswer;
        private final boolean mFileNameAnswer;
        private final String[] mFilePathParts;

        public Answers( String pFilePath, Boolean pParentDirsPathAnswer, boolean pDirPathAnswer, boolean pFileNameAnswer )
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
                        fail( i + " Expected " + mParentDirsPathAnswer + ", but got " + zActual + " w/ Pattern( \"" + zPattern + "\" ).acceptableParentDirPath( \"" + filePath + "\" )" );
                    }
                }
            }
            filePath = append( filePath, mFilePathParts[i++] );
            boolean zActual = zPattern.acceptableDirPath( filePath );
            if ( mDirPathAnswer != zActual )
            {
                fail( i + " Expected " + mDirPathAnswer + ", but got " + zActual + " w/ Pattern( \"" + zPattern + "\" ).acceptableDirPath( \"" + filePath + "\" )" );
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

    private static class ExcludeAnswers
    {
        private final String[] mFilePathParts;
        private final boolean[] mPathAndOptionalFileAnswer;

        public ExcludeAnswers( String pFilePath, boolean... pPathAndOptionalFileAnswer )
        {
            mFilePathParts = pFilePath.contains( "/" ) ? pFilePath.split( "/" ) : new String[]{"", pFilePath};
            mPathAndOptionalFileAnswer = pPathAndOptionalFileAnswer;
        }

        public void test( Pattern zPattern )
        {
            boolean zActual, zExpected;
            String filePath = "";
            int i = 0;
            for (; i < (mFilePathParts.length - 1); i++ )
            {
                zExpected = mPathAndOptionalFileAnswer[i];
                filePath = append( filePath, mFilePathParts[i] );
                zActual = zPattern.matchesDirPathAndChildren( filePath );
                if ( zExpected != zActual )
                {
                    fail( i + " Expected " + zExpected + ", but got " + zActual + " w/ Pattern( \"" + zPattern + "\" ).matchesDirPathAndChildren( \"" + filePath + "\" )" );
                }
                if ( zExpected )
                {
                    return;
                }
            }
            zExpected = mPathAndOptionalFileAnswer[i];
            filePath = append( filePath, mFilePathParts[i] );
            zActual = zPattern.matchesFilePath( filePath );
            if ( zExpected != zActual )
            {
                fail( "Expected " + zExpected + ", but got " + zActual + " w/ Pattern( \"" + zPattern + "\" ).matchesFilePath( \"" + filePath + "\" )" );
            }
        }

        private String append( String pPath, String pPart )
        {
            return (pPath.length() == 0) ? pPart : pPath + "/" + pPart;
        }
    }
}
