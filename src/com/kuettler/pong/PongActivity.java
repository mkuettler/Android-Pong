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
	private InfiniteTimer timer;
	private long timer_tick_length = 50;
	private long timer_run_length = 1000;
	private final float max_player_velocity = 1000f;
	private final float player_radius = 10f;
	private Player player1;
	private Player player2;
	private Ball ball;

	public PlayAreaView(Context context) {
	    super(context);
	    timer = null;
	    player1 = new HumanPlayer(0xFFFF0000, player_radius, 
				 max_player_velocity);
	    player2 = new Player(0xFF0000FF, player_radius, 
				 max_player_velocity);
	    gestures = new GestureDetector
		(PongActivity.this,new GestureListener
		 (this, (HumanPlayer)player1));
	    setTimer();
	}

	@Override
	public void onDraw(Canvas canvas) {
	    //canvas.drawColor(0xFF00FF00);
	    player1.draw(canvas);
	    player2.draw(canvas);
	}

	public void onSizeChanged(int w, int h, int oldw, int oldh) {
	    super.onSizeChanged(w,h,oldw,oldh);
	    Rect boundary = new Rect(0, h/2, w, h);
	    player1.setBoundary(boundary);
	    player1.setPosition(w/2f, 3f*h/4);
	    boundary = new Rect(0, 0, w, h/2);
	    player2.setBoundary(boundary);
	    player2.setPosition(w/2f, h/4f);
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
	    player1.doMove(timer_tick_length);
	    player2.doMove(timer_tick_length);
	    invalidate();
	}

	public void timerFinished() {
	    setTimer();
	}
    }

    /*
     * Common Super-class
     */
    private abstract class Ball {
	private int color;
	protected float posX;
	protected float posY;
	protected float radius;

	public Ball(int color, float radius) {
	    this.color = color;
	    this.radius = radius;
	}

	public void setPosition(float posX, float posY) {
	    this.posX = posX;
	    this.posY = posY;
	}

	public void setBoundary(Rect b) {
	    this.boundary = b;
	}

	public void draw(Canvas canvas) {
	    Paint p = new Paint();
	    p.setColor(color);
	    p.setAntiAlias(true);
	    canvas.drawCircle(posX, posY, radius, p);
	}

	abstract public void doMove(long time);

    }

    /*
     * Player class
     */

    private abstract class Player
    {

	private int color;
	public float posX;
	public float posY;
	public float radius;
	protected float targetX;
	protected float targetY;
	protected Rect boundary;
	protected float max_velocity;
	

	public Player(int color, float radius, float max_velocity) {
	    super(color, radius);
	    this.max_velocity = max_velocity;
	}

	@Override
	public void setPosition(float posX, float posY) {
	    super(posX, posY);
	    this.targetX = posX;
	    this.targetY = posY;
	}

    }

    /*
     * Human Player
     */

    class HumanPlayer extends Player {

	private boolean fling_mode;
	private float fling_vX;
	private float fling_vY;
	private long fling_time;

	public HumanPlayer(int color, float radius, float max_velocity) {
	    super(color, radius, max_velocity);
	    fling_mode = false;
	}

	@Override
	public void doMove(long time) {
	    if (!fling_mode) {
		moveToTarget( max_velocity * time / 1000f);
	    } else {
		flingMove(time);
	    }
	    
	}

	public void moveTo(final float x, final float y) {
	    fling_mode = false;
	    targetX = x;
	    targetY = y;
	    clipTarget();
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
		return true;
	    } else {
		posX = posX + (targetX-posX) * max_dist/dist;
		posY = posY + (targetY-posY) * max_dist/dist;
		return false;
	    }
	}

	private boolean flingMove(long time) {
	    posX = posX + fling_vX * time / 1000;
	    posY = posY + fling_vY * time / 1000;
	    reflectPosition();
	    fling_vX = fling_vX * 0.8f;
	    fling_vY = fling_vY * 0.8f;
	    fling_time = fling_time - time;
	    if (fling_time <= 0) {
		fling_mode = false;
		targetX = posX;
		targetY = posY;
	    }
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
	}

    }

    /*
     * PongBall
     */

    private class PongBall extends Ball {
	
	private float velX;
	private float velY;
	private Rect boundary;
	private Player player1;
	private Player player2;
	
	
	public PongBall(int color, float radius) {
	    super(color, radius);
	    velX = 0;
	    velY = 0;
	}

	public void setPosition(float x, float y) {
	    super(x,y);
	}

	public void setSpeed(float x, float y) {
	    velX = x;
	    velY = y;
	}

	public void setBoundary(final Rect boundary) {
	    this.boundary = boundary;
	}

	@Override
	public void doMove(long time) {
	    float dx = velX * time / 1000f;
	    float dy = velY * time / 1000f;
	    float newX = posX + dx;
	    float newY = posY + dy;
	    boolean change;
	    do {
		change = false;
		if ((player1.newX-newX)*(player1.newX-newX) + 
		    (player1.newY-newY)*(player1.newY-newY) <=
		    player1.radius + radius) {
		    /* some smart code here... */
		} else if ((player2.newX-newX)*(player2.newX-newX) + 
		    (player2.newY-newY)*(player2.newY-newY) <=
		    player2.radius + radius) {
		    /* and here... */
		} else if (newX < boundary.left) {
		    newX = 2*boundary.left - newX;
		    fling_vX = -fling_vX;
		    change = true;
		} else if (newX > boundary.right) {
		    newX = 2*boundary.right - newX;
		    fling_vX = -fling_vX;
		    change = true;
		} else if (newY > boundary.bottom) {
		    newY = 2*boundary.bottom - newY;
		    fling_vY = -fling_vY;
		    change = true;
		} else if (newY < boundary.top) {
		    newY = 2*boundary.top - newY;
		    fling_vY = -fling_vY;
		    change = true;
		}
	    } while (change);
	}
    }

    private class GestureListener implements GestureDetector.OnGestureListener,
					     GestureDetector.OnDoubleTapListener
    {
	PlayAreaView view;
	HumanPlayer player;

	    public GestureListener(PlayAreaView view, HumanPlayer player) {
	    this.view = view;
	    this.player = player;
	}

	@Override
	public boolean onDown(MotionEvent e) {
	    /* We should not always return true here. */
	    player.moveTo(e.getX(), e.getY());
	    return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, 
				float dX, float dY) {
	    player.moveTo(e2.getX(), e2.getY());
	    return true;
	}

	@Override
	public boolean onFling (MotionEvent e1, MotionEvent e2,
				final float vX, final float vY) {
	    player.flingBy(vX, vY);
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
