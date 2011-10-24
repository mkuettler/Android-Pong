package com.kuettler.pong;

import android.content.Context;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Point;
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
        setContentView(R.layout.main);

	//setContentView(new PlayAreaView(this));
	FrameLayout frame = (FrameLayout) findViewById(R.id.frame_layout);
	PlayAreaView play_area = new PlayAreaView(this);
	frame.addView(play_area);
	frame.bringChildToFront(play_area);
    }

    public class PlayAreaView extends View
    {
	
	private GestureDetector gestures;
	private float posX;
	private float posY;
	private float targetX;
	private float targetY;
	private Rect boundary;
	private InfiniteTimer timer;
	private long timer_tick_length = 50;
	private long timer_run_length = 1000;
	private float max_velocity = 1000f;
	private boolean fling_mode;
	private float fling_vX;
	private float fling_vY;
	private long fling_time;

	public PlayAreaView(Context context) {
	    super(context);
	    posX = 100f;
	    posY = 100f;
	    targetX = posX;
	    targetY = posY;
	    fling_mode = false;
	    timer = null;
	    gestures = new GestureDetector(PongActivity.this,
					   new GestureListener(this));
	}

	@Override
	public void onDraw(Canvas canvas) {
	    //canvas.drawColor(0xFF00FF00);
	    Paint p = new Paint();
	    p.setColor(0xFFFF0000);
	    p.setAntiAlias(true);
	    canvas.drawCircle(posX, posY, 10.0f, p);
	}

	public void onSizeChanged(int w, int h, int oldw, int oldh) {
	    super.onSizeChanged(w,h,oldw,oldh);
	    boundary = new Rect(0, h/2, w, h);
	    posX = w/2;
	    posY = 3*h/4;
	    targetX = posX;
	    targetY = posY;
	    Toast.makeText(PongActivity.this, "New size = "+
			   Integer.toString(w)+" "+Integer.toString(h),
			   Toast.LENGTH_LONG).show();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    /*int action = event.getAction();
	    if (action == MotionEvent.ACTION_DOWN) {
		moveTo(event.getX(), event.getY());
		} else if (action == MotionEvent.ACTION*/
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

	/*public void moveBy(final float dx, final float dy) {
	    fling_mode = false;
	    targetX = targetX + dx;
	    targetY = targetY + dy;
	    clipTarget();
	    if (timer == null) {
		setTimer();
	    }
	    }*/

	public void moveTo(final float x, final float y) {
	    fling_mode = false;
	    targetX = x;
	    targetY = y;
	    clipTarget();
	    if (timer == null) {
		setTimer();
	    }
	}

	private void clipTarget() {
	    if (targetX < boundary.left) {
		targetX = boundary.left;
	    } else if (targetX > boundary.right) {
		targetX = boundary.right;
	    }
	    if (targetY > boundary.bottom) {
		targetY = boundary.bottom;
	    } else if (targetY < boundary.top) {
		targetY = boundary.top;
	    }
	}

	private void reflectPosition() {
	    boolean change;
	    do {
		change = false;
		if (posX < boundary.left) {
		    posX = 2*boundary.left - posX;
		    fling_vX = -fling_vX;
		    change = true;
		} else if (posX > boundary.right) {
		    posX = 2*boundary.right - posX;
		    fling_vX = -fling_vX;
		    change = true;
		}
		if (posY > boundary.bottom) {
		    posY = 2*boundary.bottom - posY;
		    fling_vY = -fling_vY;
		    change = true;
		} else if (posY < boundary.top) {
		    posY = 2*boundary.top - posY;
		    fling_vY = -fling_vY;
		    change = true;
		}
	    } while (change);
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
		posX = posX + (targetX-posX) * max_dist/dist;
		posY = posY + (targetY-posY) * max_dist/dist;
		invalidate();
		return false;
	    }
	}

	private boolean flingMove() {
	    posX = posX + fling_vX * timer_tick_length / 1000;
	    posY = posY + fling_vY * timer_tick_length / 1000;
	    reflectPosition();
	    fling_vX = fling_vX * 0.8f;
	    fling_vY = fling_vY * 0.8f;
	    fling_time = fling_time - timer_tick_length;
	    if (fling_time <= 0) {
		fling_mode = false;
		targetX = posX;
		targetY = posY;
	    }
	    invalidate();
	    return !fling_mode;
	}

	public void flingBy(final float vX, final float vY) {
	    fling_mode = true;
	    fling_vX = vX;
	    fling_vY = vY;
	    if (fling_vX*fling_vX + fling_vY*fling_vY > 
		max_velocity*max_velocity) {
		final float fv = FloatMath.sqrt(fling_vX*fling_vX + 
						fling_vY*fling_vY);
		fling_vX = fling_vX * max_velocity/fv;
		fling_vY = fling_vY * max_velocity/fv;
	    }
	    fling_time = 800;
	    if (timer == null) {
		setTimer();
	    }
	}
	
    }

    /*private class Player
    {

	int color;
	Rect boundary;
	

	public Player() {
	    
	}
	}*/

    private class GestureListener implements GestureDetector.OnGestureListener,
					     GestureDetector.OnDoubleTapListener
    {
	PlayAreaView view;

	public GestureListener(PlayAreaView view) {
	    this.view = view;
	}

	@Override
	public boolean onDown(MotionEvent e) {
	    /* We should not always return true here. */
	    view.moveTo(e.getX(), e.getY());
	    return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, 
				float dX, float dY) {
	    view.moveTo(e2.getX(), e2.getY());
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
