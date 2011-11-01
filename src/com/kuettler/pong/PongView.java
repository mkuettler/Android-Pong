
package com.kuettler.pong;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.FloatMath;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PongView extends SurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = "PongActivity";

    class PongThread extends Thread
    {
        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;

        private PongView mView;

        ///** Message handler used by thread to interact with TextView */
        //private Handler mHandler;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        /** Used to figure out elapsed time between frames */
        private long mLastTime;

        /*
         * State-tracking constants
         */
        public static final int STATE_LOSE = 1;
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;
        public static final int STATE_WIN = 5;

        private static final float max_player_velocity = 1000f;
        private static final float player_radius = 30f;
        private static final float max_ball_velocity = 2000f;
        private static final float ball_radius = 15f;

        private Ball ball;

        /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
        private int mMode;

        public PongThread(SurfaceHolder holder, PongView view, Context context) {
            mSurfaceHolder = holder;
            mView = view;

            ball = new Ball(Color.WHITE, ball_radius, max_ball_velocity);
        }

        public void setRunning(boolean b) {
            mRun = b;
        }

        @Override
        public void run() {
            while (mRun) {
                Canvas canvas = null;
                try {
                    canvas = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mMode == STATE_RUNNING) updatePhysics();
                        canvas.drawColor(Color.BLACK);
                        ball.draw(canvas);
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (canvas != null) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }

        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                mMode = mode;
            }
        }

        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) setState(STATE_PAUSE);
            }
        }

        /**
         * Resumes from a pause.
         */
        public void unpause() {
            // Move the real time clock up to now
            synchronized (mSurfaceHolder) {
                mLastTime = System.currentTimeMillis() + 100;
            }
            setState(STATE_RUNNING);
        }

        private void updatePhysics() {
            long now = System.currentTimeMillis();

            if (mLastTime > now)
                return;

            long elapsed = now - mLastTime;

            ball.integrate(now/1000f, elapsed/1000f);
            mLastTime = now;
        }

        /**
         * Starts the game, setting parameters for the current difficulty.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
                mLastTime = System.currentTimeMillis() + 100;
                setState(STATE_RUNNING);
            }
        }

        public void setSurfaceSize(int w, int h) {
            synchronized (mSurfaceHolder) {
                final float r = player_radius;
                final float rb = ball_radius;
                Rect boundary = new Rect((int)r, (int)(h/2+r),
                                         (int)(w-r), (int)(h-r));
                ball.setBoundary(boundary);
                ball.setPosition(w/2f, 5*h/8f);
            }
        }

	public boolean doSingleTap(MotionEvent e) {
            ball.setGoal(e.getX(), e.getY());
            return true;
	}

	public boolean doScroll(MotionEvent e1, MotionEvent e2,
				float dX, float dY) {
	    //player.moveTo(e2.getX(), e2.getY());
            //Log.d(TAG, "onScroll: " + e1.toString());
            ball.setGoal(e2.getX(), e2.getY());
	    return true;
	}
    }

    private GestureDetector gestures;
    private PongThread thread;
    private Context mContext;

    public PongView(Context context) {
        super(context);
        mContext = context;

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new PongThread(holder, this, context);

        gestures = new GestureDetector
            (context,
             new GestureListener(thread));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();
        thread.doStart();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        thread.setSurfaceSize(width, height);
    }

    public PongThread getThread() {
        return thread;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestures.onTouchEvent(event);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG, "PongView.onWindowFocusChanged: " + hasFocus);
        if (hasFocus)
            thread.unpause();
        else
            thread.pause();
    }

    /*
     * Common Super-class
     */
    class Ball {
        protected class State
        {
            public float x, y, dx, dy;

            public State() {}

            public State(float x, float y, float dx, float dy) {
                this.x = x;
                this.y = y;
                this.dx = dx;
                this.dy = dy;
            }

            public String toString() {
                return "State(" + x + ", " + y + "); (" +
                    dx + ", " + dy + ")";
            }

            public State difference(State other) {
                State result = new State();
                result.x = this.x - other.x;
                result.y = this.y - other.y;
                result.dx = this.dx - other.dx;
                result.dy = this.dy - other.dy;
                return result;
            }
        }

        protected class Derivative
        {
            public float dx, dy, ddx, ddy;

            public Derivative() {}

            public Derivative(float dx, float dy, float ddx, float ddy) {
                this.dx = dx;
                this.dy = dy;
                this.ddx = ddx;
                this.ddy = ddy;
            }

            public String toString() {
                return "Derivative(" + dx + ", " + dy + "); (" +
                    ddx + ", " + ddy + ")";
            }
        }

	private int color;

	protected float radius;
	protected float mass;
	protected float max_velocity;
	protected Rect boundary;

        protected State state;
        protected State goal;

        protected final long vibrate_length = 50;
        protected final Vibrator vibrator =
            (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);

	public Ball(int color, float radius, float max_velocity) {
	    this.color = color;
	    this.radius = radius;
	    this.max_velocity = max_velocity;

            state = new State(0,0,0,0);
            goal = new State(0,0,0,0);
	}

	public void setPosition(float x, float y) {
            Log.d(TAG, "Ball: setPosition=" + x + ", " + y);
            state.x = goal.x = x;
            state.y = goal.y = y;
            state.dx = goal.dx = 0;
            state.dy = goal.dy = 0;
	}

	public void setGoal(float x, float y) {
            Log.d(TAG, "Ball: setGoal=" + x + ", " + y);
            goal.x = x;
            goal.y = y;
            goal.dx = goal.dy = 0;
	}


	public void setBoundary(Rect b) {
	    this.boundary = b;
	}

	public void draw(Canvas canvas) {
	    Paint p = new Paint();
	    p.setColor(color);
	    p.setAntiAlias(true);
	    canvas.drawCircle(state.x, state.y, radius, p);
	}

        protected Derivative evaluate(float t, float dt, Derivative D) {
            State S = new State();
            S.x = state.x + D.dx*dt;
            S.y = state.y + D.dy*dt;
            S.dx = state.dx + D.ddx*dt;
            S.dy = state.dy + D.ddy*dt;

            Derivative result = new Derivative();
            result.dx = S.dx;
            result.dy = S.dy;

            // //result.ddy = acceleration(S, t+dt);
            // result.ddx = 0;
            // result.ddy = -10 * (S.y - 200) - 0.01f*S.dy;

            State difference = S.difference(goal);
            float k = 10;
            float b = 2;
            result.ddx = -k * difference.x - b*S.dx;
            result.ddy = -k * difference.y - b*S.dy;

            return result;
        }

        protected void integrate(float t, float dt) {
            Derivative a = evaluate(t, 0.0f, new Derivative());
            Derivative b = evaluate(t+dt*0.5f, dt*0.5f, a);
            Derivative c = evaluate(t+dt*0.5f, dt*0.5f, b);
            Derivative d = evaluate(t+dt, dt, c);

            float dxdt = 1.0f/6.0f * (a.dx + 2.0f*(b.dx + c.dx) + d.dx);
            float dydt = 1.0f/6.0f * (a.dy + 2.0f*(b.dy + c.dy) + d.dy);
            float ddxdt = 1.0f/6.0f * (a.ddx + 2.0f*(b.ddx + c.ddx) + d.ddx);
            float ddydt = 1.0f/6.0f * (a.ddy + 2.0f*(b.ddy + c.ddy) + d.ddy);

            state.x = state.x + dxdt * dt;
            state.y = state.y + dydt * dt;
            state.dx = state.dx + ddxdt * dt;
            state.dy = state.dy + ddydt * dt;
        }

        @Override
        public String toString() {
            return getClass().getName() + ": Color=" + color;
        }

    }

    private class GestureListener implements GestureDetector.OnGestureListener,
					     GestureDetector.OnDoubleTapListener
    {
	PongThread thread;

        public GestureListener(PongThread thread) {
	    this.thread = thread;
	}

	@Override
	public boolean onDown(MotionEvent e) {
	    /* We should not always return true here. */
	    //player.moveTo(e.getX(), e.getY());
            //Log.d(TAG, "onDown: " + e.toString());
            return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float dX, float dY) {
	    //player.moveTo(e2.getX(), e2.getY());
            //Log.d(TAG, "onScroll: " + e1.toString());
            thread.doScroll(e1, e2, dX, dY);
	    return true;
	}

	@Override
	public boolean onFling (MotionEvent e1, MotionEvent e2,
				final float vX, final float vY) {
	    //player.flingBy(vX, vY);
            Log.d(TAG, "onFling" + e1.toString());
	    return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap" + e.toString());
	    return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
            //Log.d(TAG, "onLongPress" + e.toString());
	}

	@Override
	public void onShowPress(MotionEvent e) {
            //Log.d(TAG, "onShowPress" + e.toString());
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
            //Log.d(TAG, "onSingleTapUp: " + e.toString());
            thread.doSingleTap(e);
	    return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
            Log.d(TAG, "onDoubleTapEvent: " + e.toString());
	    return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
            //Log.d(TAG, "onSingleTapConfirmed: " + e.toString());
            return true;
	}
    }
}
