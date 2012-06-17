package org.litesoft.droid;

import org.apache.cordova.*;
import android.content.res.AssetManager;
import android.os.Bundle;

public class PhoneGapActivity extends DroidGap
{
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        loadUrl( "file:///android_asset/www/" + findIndexHtml( getValue("language", "en") ) );
	}

	private String getValue( String key, String defaultValue )
	{
		return getSharedPreferences( getApplicationInfo().packageName, MODE_PRIVATE ).getString( key, defaultValue );
    }

    private String findIndexHtml( String language )
    {
        AssetManager assetManager = getAssets();
        try
        {
            String[] files = assetManager.list( "www" );
            if ( files != null )
            {
                String toFind = "index-" + language + ".html";
                for ( String file : files )
                {
                    if ( toFind.equals( file ) )
                    {
                        return file;
                    }
                }
            }
        }
        catch ( Exception e )
        {
            // Fall Thru
        }
        return "index.html";
    }
}
