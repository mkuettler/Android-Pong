package com.kuettler.pong;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.FloatMath;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;


public class PongActivity extends Activity
{
    private static final String TAG = "PongActivity";

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

    /*
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

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

    public class PlayAreaView extends View
    {

	private GestureDetector gestures;
	private InfiniteTimer timer;
	private long timer_tick_length = 50;
	private long timer_run_length = 1000;
	private final float max_player_velocity = 1000f;
	private final float player_radius = 30f;
	private final float max_ball_velocity = 2000f;
	private final float ball_radius = 15f;
	private Player player1;
	private Player player2;
	private PongBall ball;

	public PlayAreaView(Context context) {
	    super(context);
	    timer = null;
	    player1 = new HumanPlayer(0xFFFF0000, player_radius,
				 max_player_velocity);
	    player2 = new HumanPlayer(0xFF0000FF, player_radius,
				 max_player_velocity);
	    ball = new PongBall(0xFFFFFFFF, ball_radius,
				player1, player2, max_ball_velocity);
	    gestures = new GestureDetector
		(PongActivity.this,new GestureListener
		 (this, (HumanPlayer)player1));
	}

	@Override
	public void onDraw(Canvas canvas) {
	    //canvas.drawColor(0xFF00FF00);
	    player1.draw(canvas);
	    player2.draw(canvas);
	    ball.draw(canvas);
	}

	public void onSizeChanged(int w, int h, int oldw, int oldh) {
	    super.onSizeChanged(w,h,oldw,oldh);
	    final float r = player_radius;
	    final float rb = ball_radius;
	    Rect boundary = new Rect((int)r, (int)(h/2+r),
				     (int)(w-r), (int)(h-r));
	    player1.setBoundary(boundary);
	    player1.setPosition(w/2f, 3f*h/4);
	    boundary = new Rect((int)r, (int)r, (int)(w-r), (int)(h/2-r));
	    player2.setBoundary(boundary);
	    player2.setPosition(w/2f, h/4f);
	    boundary = new Rect((int)rb, (int)rb, (int)(w-rb), (int)(h-rb));
	    ball.setBoundary(boundary);
	    ball.setPosition(w/2f, 5*h/8f);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    if (timer == null)
		setTimer();
	    return gestures.onTouchEvent(event);
	}

	private void setTimer() {
	    timer = new InfiniteTimer(this, timer_run_length, timer_tick_length);
	    timer.start();
	}

	public void timerTicked() {
	    player1.doMove(timer_tick_length);
	    player2.doMove(timer_tick_length);
	    ball.doMove(timer_tick_length);
	    invalidate();
	}

	public void timerFinished() {
	    setTimer();
	}

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            super.onWindowFocusChanged(hasFocus);
            Log.d(TAG, "PlayAreaView.onWindowFocusChanged: " + hasFocus);
            if (hasFocus)
                setTimer();
            else
                timer.cancel();
        }
    }

    /*
     * Common Super-class
     */
    private abstract class Ball {
	private int color;
	protected float posX;
	protected float posY;
	protected float velX;
	protected float velY;
	protected float radius;
	protected float mass;
	protected float max_velocity;
	protected Rect boundary;

        protected final long vibrate_length = 50;
        protected final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

	public Ball(int color, float radius, float max_velocity) {
	    this.color = color;
	    this.radius = radius;
	    this.max_velocity = max_velocity;
	}

	public void setPosition(float posX, float posY) {
	    this.posX = posX;
	    this.posY = posY;
	    velX = 0;
	    velY = 0;
	}

	public void setSpeed(float x, float y) {
	    velX = x;
	    velY = y;
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

	protected void clapSpeed() {
	    final float s = FloatMath.sqrt(velX*velX+velY*velY);
	    if (s > max_velocity) {
		velX = velX * max_velocity/s;
		velY = velY * max_velocity/s;
	    }
	}

	abstract public void doMove(long time);

        @Override
        public String toString() {
            return getClass().getName() + ": Color=" + color;
        }

    }

    /*
     * Player class
     */

    private abstract class Player extends Ball
    {

	protected float targetX;
	protected float targetY;

	public Player(int color, float radius, float max_velocity) {
	    super(color, radius, max_velocity);
	    mass = 10.0f;
	}

	@Override
	public void setPosition(float posX, float posY) {
	    super.setPosition(posX, posY);
	    this.targetX = posX;
	    this.targetY = posY;
	}

    }

    /*
     * Human Player
     */

    class HumanPlayer extends Player {

	/* these variables should be static,
	 * but java doesn't allow them to be.*/
	private long TARGET_MODE = 1;
	private long FLING_MODE = 2;
	private long STILL_MODE = 3;
	private long fling_sqr_threshold = 2;

	private long move_mode;

	public HumanPlayer(int color, float radius, float max_velocity) {
	    super(color, radius, max_velocity);
	    move_mode = STILL_MODE;
	}

	@Override
	public void setSpeed(final float vx, final float vy) {
	    if (move_mode == STILL_MODE)
		move_mode = FLING_MODE;

	    if (move_mode == FLING_MODE)
		super.setSpeed(vx, vy);
	}
	
	@Override
	public void doMove(long time) {
	    if (move_mode == TARGET_MODE) {
		moveToTarget(time);
	    } else if (move_mode == FLING_MODE) {
		flingMove(time);
	    }

	}

	public void moveTo(final float x, final float y) {
	    move_mode = TARGET_MODE;
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
		    velX = -velX;
		    change = true;
		} else if (posX > boundary.right) {
		    posX = 2*boundary.right - posX;
		    velX = -velX;
		    change = true;
		}
		if (posY > boundary.bottom) {
		    posY = 2*boundary.bottom - posY;
		    velY = -velY;
		    change = true;
		} else if (posY < boundary.top) {
		    posY = 2*boundary.top - posY;
		    velY = -velY;
		    change = true;
		}
                if (change) {
                    //Log.d(TAG, "HumanPlayer.reflectPosition() -- " +
                    //      posX + ", " + posY + "; " + velX + ", " + velY);
                    vibrator.vibrate(vibrate_length);
                }
	    } while (change);
	}

	private boolean moveToTarget(long time) {
	    final float max_dist = max_velocity * time / 1000f;
	    final float dist = FloatMath.sqrt((posX-targetX)*(posX-targetX) +
					      (posY-targetY)*(posY-targetY));
	    if (dist <= max_dist) {
		velX = (targetX - posX)*1000f/time;
		velY = (targetY - posY)*1000f/time;
		posX = targetX;
		posY = targetY;
		move_mode = STILL_MODE;
		return true;
	    } else {
		float newX = posX + (targetX-posX) * max_dist/dist;
		float newY = posY + (targetY-posY) * max_dist/dist;
		velX = (newX - posX)*1000f/time;
		velY = (newY - posY)*1000f/time;
		posX = newX;
		posY = newY;
		return false;
	    }
	}

	private boolean flingMove(long time) {
	    posX = posX + velX * time / 1000;
	    posY = posY + velY * time / 1000;
	    reflectPosition();
	    targetX = posX;
	    targetY = posY;
	    velX = velX * 0.8f;
	    velY = velY * 0.8f;
	    if (velX*velX + velY*velY <= fling_sqr_threshold) {
		move_mode = STILL_MODE;
		velX = 0.0f;
		velY = 0.0f;
	    }
	    return !(move_mode == FLING_MODE);
	}

	public void flingBy(final float vX, final float vY) {
	    move_mode = FLING_MODE;
	    velX = vX;
	    velY = vY;
	    if (velX*velX + velY*velY >
		max_velocity*max_velocity) {
		final float fv = FloatMath.sqrt(velX*velX +
						velY*velY);
		velX = velX * max_velocity/fv;
		velY = velY * max_velocity/fv;
	    }
	}

    }

    /*
     * PongBall
     */

    private class PongBall extends Ball {

	private Player player1;
	private Player player2;

	public PongBall(int color, float radius, Player p1, Player p2,
			float max_velocity) {
	    super(color, radius, max_velocity);
	    velX = 0;
	    velY = 0;
	    player1 = p1;
	    player2 = p2;
	    mass = 2.0f;
	}

	private Pair<Float,Float> collideTime(float time, Ball ba) {
	    final float vx = velX;
	    final float vy = velY;
	    final float px = posX;
	    final float py = posY;
	    final float bx = ba.posX;
	    final float by = ba.posY;
	    final float r = radius + ba.radius;
	    final float a = vy*vy + vx*vx;
	    final float b = -2*(bx-px)*vx - 2*(by-py)*vy;
	    final float c = (bx-px)*(bx-px) + (by-py)*(by-py) - r*r;
	    final float D = b*b - 4*a*c;
	    if (a == 0) {
		if ((px-bx)*(px-bx)+(py-by)*(py-by) <= r*r)
		    return new Pair<Float, Float>(0.0f, 0.0f);
		return new Pair<Float, Float>(-1.0f, -1.0f);
	    }
	    if (D < 0)
		return new Pair<Float, Float>(-1.0f, -1.0f);
	    final float t1 = (-b-FloatMath.sqrt(D))/(2*a);
	    final float t2 = (-b+FloatMath.sqrt(D))/(2*a);
	    /* Since a > 0 we have t1 <= t2 */
	    if (t2 < 0 || t1 > time)
		return new Pair<Float, Float>(-1.0f, -1.0f);
	    if (t1 < 0.0f)
		return new Pair<Float, Float>(0.0f, t2);
	    return new Pair<Float, Float>(t1, t2);
	}

	private void collideWithBall(Ball b) {
	    final float m = mass;
	    final float bm = b.mass;
	    float dx = b.posX - posX;
	    float dy = b.posY - posY;
	    float ld = FloatMath.sqrt(dx*dx + dy*dy);
	    dx = dx/ld;
	    dy = dy/ld;
	    final float vd = dx*velX + dy*velY;
	    final float vo = -dy*velX + dx*velY;
	    final float bvd = dx*b.velX + dy*b.velY;
	    final float bvo = -dy*b.velX + dx*b.velY;
	    final float nvd = 2*(bm*bvd + m*vd)/(m+bm) - vd;
	    final float bnvd = 2*(bm*bvd + m*vd)/(m+bm) - bvd;

	    velX = dx*nvd - dy*vo;
	    velY = dy*nvd + dx*vo;
	    /* This will not change the Players behavior, unless
	     * he is in fling_mode */
	    b.setSpeed(dx*bnvd - dy*bvo, dy*bnvd + dx*bvo);

            vibrator.vibrate(vibrate_length);
	}

	@Override
	public void doMove(long time) {
	    float realtime = time / 1000f;
	    clapSpeed();
	    float newX = posX + velX * realtime;
	    float newY = posY + velY * realtime;
	    float hittime;
	    float hittime2;
	    Player player;
	    boolean change;
	    do {
		change = false;
		player = null;
		hittime = collideTime(realtime, player1).first;
		hittime2 = collideTime(realtime, player2).first;
		if (hittime >= 0 && (hittime2 < 0 || hittime <= hittime2)) {
		    player = player1;
		} else if (hittime2 >= 0) {
		    hittime = hittime2;
		    player = player2;
		}
		if (player != null) {
                    Log.d(TAG, "PongBall.doMove(): " +
                          //"Player=" + player.toString() + "; " +
                          "pos=(" + posX + ", " + posY +
                          "); vel=(" + velX + ", " + velY + ")");
		    /* Move to the hit-position */
		    posX = posX + hittime*velX;
		    posY = posY + hittime*velY;
		    realtime = realtime - hittime;
		    collideWithBall(player);
		    clapSpeed();
		    /* Move away from this ball */
		    hittime = collideTime(1000000.0f /*a lot*/, player).second;
		    if (hittime > 0.0f) {
			posX = posX + (hittime+0.01f)*velX;
			posY = posY + (hittime+0.01f)*velY;
			realtime = realtime - hittime;
			if (realtime < 0.0f)
			    realtime = 0.0f;
		    }
		    newX = posX + velX * realtime;
		    newY = posY + velY * realtime;
		    change = true;
                    Log.d(TAG, "PongBall.doMove(): " +
                          //"Player=" + player.toString() + "; " +
                          "new=(" + newX + ", " + newY +
                          "); vel=(" + velX + ", " + velY + ")");

		}
		/* Get the order right! */
		else if (newX < boundary.left) {
		    newX = 2*boundary.left - newX;
		    velX = -velX;
		    change = true;
		} else if (newX > boundary.right) {
		    newX = 2*boundary.right - newX;
		    velX = -velX;
		    change = true;
		} else if (newY > boundary.bottom) {
		    newY = 2*boundary.bottom - newY;
		    velY = -velY;
		    change = true;
		} else if (newY < boundary.top) {
		    newY = 2*boundary.top - newY;
		    velY = -velY;
		    change = true;
		}
                if (change) {
                    vibrator.vibrate(vibrate_length);
                }
	    } while (change);
	    posX = newX;
	    posY = newY;
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
