package com.kuettler.pong;

import android.content.Context;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.util.FloatMath;

import android.widget.Toast;

public class PongActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);

	setContentView(new PlayAreaView(this));
	/*FrameLayout frame = (FrameLayout) findViewById(R.id.frame_layout);
	PlayAreaView play_area = new PlayAreaView(this);
	frame.addView(play_area);*/
    }

    public class PlayAreaView extends View
    {
	
	private GestureDetector gestures;
	private float posX;
	private float posY;
	private float targetX;
	private float targetY;
	private InfiniteTimer timer;
	private long timer_tick_length = 100;
	private long timer_run_length = 1000;
	private float max_velocity = 1000f;
	private boolean fling_mode;
	private float fling_vX;
	private float fling_vY;
	private long fling_time;

	public PlayAreaView(Context context) {
	    super(context);
	    posX = 50f;
	    posY = 70f;
	    targetX = posX;
	    targetY = posY;
	    fling_mode = false;
	    timer = null;
	    gestures = new GestureDetector(PongActivity.this,
					   new GestureListener(this));
	}

	@Override
	public void onDraw(Canvas canvas) {
	    Toast.makeText(PongActivity.this,
			   "onDraw " + Float.toString(canvas.getHeight())+" "
			   +Float.toString(canvas.getWidth()),
			   Toast.LENGTH_SHORT).show();
	    Paint p = new Paint();
	    p.setColor(0x00FFFFFF);
	    p.setAntiAlias(true);
	    canvas.drawCircle(posX, posY, 10.0f, p);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    return gestures.onTouchEvent(event);
	}

	private void setTimer() {
	    timer = new InfiniteTimer(this, timer_run_length, timer_tick_length);
	    timer.start();
	}

	public void timerTicked() {
	    if (!fling_mode) {
		moveToTarget( timer_tick_length * max_velocity / 1000f );
	    } else {
		flingMove();
	    }
	}

	public void timerFinished() {
	    Toast.makeText(PongActivity.this,
			   Float.toString(posX)+" "+Float.toString(targetX)+" "
			   +Float.toString(posY)+" "+Float.toString(targetY),
			   Toast.LENGTH_LONG).show();
	    if (posX != targetX || posY != targetY || fling_mode) {
		setTimer();
	    } else {
		timer = null;
	    }
	}

	/*
	 * The movement probably would benefit from the usage of an
	 * Interpolator. Yet I don't find that very convenient.
	 */

	public void moveBy(final float dx, final float dy) {
	    fling_mode = false;
	    targetX = targetX + dx;
	    targetY = targetY + dy;
	    if (timer == null) {
		setTimer();
	    }
	}

	public void moveTo(final float x, final float y) {
	    fling_mode = false;
	    targetX = x;
	    targetY = y;
	    if (timer == null) {
		setTimer();
	    }
	}

	private boolean moveToTarget(final float max_dist) {
	    final float dist = FloatMath.sqrt((posX-targetX)*(posX-targetX) + 
					      (posY-targetY)*(posY-targetY));
	    if (dist <= max_dist) {
		posX = targetX;
		posY = targetY;
		invalidate();
		return true;
	    } else {
		posX = posX + (targetX-posX) * dist/max_dist;
		posY = posY + (targetY-posY) * dist/max_dist;
		invalidate();
		return false;
	    }
	}

	private boolean flingMove() {
	    posX = posX + fling_vX * timer_tick_length / 1000;
	    posY = posY + fling_vY * timer_tick_length / 1000;
	    fling_vX = fling_vX * 0.8f;
	    fling_vY = fling_vY * 0.8f;
	    fling_time = fling_time - timer_tick_length;
	    if (fling_time <= 0) {
		fling_mode = false;
	    }
	    invalidate();
	    return !fling_mode;
	}

	public void flingBy(final float vX, final float vY) {
	    fling_mode = true;
	    fling_vX = vX;
	    fling_vY = vY;
	    fling_time = 400;
	    if (timer == null) {
		setTimer();
	    }
	}
	
    }

    private class GestureListener implements GestureDetector.OnGestureListener,
					     GestureDetector.OnDoubleTapListener
    {
	PlayAreaView view;

	public GestureListener(PlayAreaView view) {
	    this.view = view;
	}

	@Override
	public boolean onDown(MotionEvent e) {
	    Toast.makeText(PongActivity.this,
			   Float.toString(e.getX())+", "
			   +Float.toString(e.getY()),
			   Toast.LENGTH_SHORT).show();
	    /* We should not always return true here. */
	    //view.moveTo(e.getX(), e.getY());
	    return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, 
				float dX, float dY) {
	    view.moveBy(dX,dY);
	    return true;
	}

	@Override
	public boolean onFling (MotionEvent e1, MotionEvent e2,
				final float vX, final float vY) {
	    view.flingBy(vX, vY);
	    return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
	    return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	    return;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	    ;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
	    return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
	    return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
	    return true;
	}
    }

    private class InfiniteTimer extends CountDownTimer
    {
	PlayAreaView view;

	InfiniteTimer(PlayAreaView view, long run_time, long tick_time) {
	    super(run_time, tick_time);
	    this.view = view;
	}

	@Override
	public void onTick(long remainingMS) {
	    view.timerTicked();
	}

	@Override
	public void onFinish() {
	    view.timerFinished();
	}

    }
}
