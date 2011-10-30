
package com.kuettler.pong;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import com.kuettler.pong.PongView;
import com.kuettler.pong.PongView.PongThread;


public class PongActivity extends Activity
{
    private static final String TAG = "PongActivity";

    private PongThread mPongThread;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.main);

	//setContentView(new PlayAreaView(this));
	FrameLayout frame = (FrameLayout) findViewById(R.id.frame_layout);
	PongView play_area = new PongView(this);
	frame.addView(play_area);
	frame.bringChildToFront(play_area);

        mPongThread = play_area.getThread();
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mPongThread.pause();
    }

    /*
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    // @Override
    // public void onWindowFocusChanged (boolean hasFocus) {
    //     super.onWindowFocusChanged(hasFocus);
    //     Log.d(TAG, "onWindowFocusChanged: " + hasFocus);
    // }
    */

}
