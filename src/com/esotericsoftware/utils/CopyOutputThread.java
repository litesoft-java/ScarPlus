package com.esotericsoftware.utils;

import java.io.*;

public class CopyOutputThread extends Thread
{
    private BufferedReader reader;
    private PrintStream mOut;

    public CopyOutputThread( InputStream pInputStream, PrintStream pOut )
    {
        reader = new BufferedReader( new InputStreamReader( pInputStream ) );
        mOut = pOut;
        start();
    }

    @Override
    public void run()
    {
        try
        {
            for ( String line; null != (line = reader.readLine()); )
            {
                mOut.println( line );
            }
        }
        catch ( IOException e )
        {
            e.printStackTrace( mOut );
        }
        finally
        {
            FileUtil.dispose( reader );
        }
    }
}
