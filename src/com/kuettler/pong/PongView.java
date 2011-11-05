
package com.kuettler.pong;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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
        private Ball other_ball;

        /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
        private int mMode;

        public PongThread(SurfaceHolder holder, PongView view, Context context) {
            mSurfaceHolder = holder;
            mView = view;

            ball = new Ball(Color.WHITE, ball_radius, 500000);
            ball.setFreeConstants(0.25f, 0.2f);
            ball.setCollisionConstants(500, 1);
            other_ball = new Ball(Color.RED, ball_radius, 500);
            other_ball.setFreeConstants(0.0f, 0.1f);
            other_ball.setCollisionConstants(500, 0);
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
                        other_ball.draw(canvas);
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

            if (ball.detectOtherBallCollision(other_ball)) {
                Log.d(TAG, "Ball collision detected");
                ball.setMode(Ball.MODE_COLLISION);
                other_ball.setMode(Ball.MODE_COLLISION);
                other_ball.lastBallContact = ball.lastBallContact.scale(-1);
            } else {
                if (ball.mode == Ball.MODE_COLLISION)
                    ball.setMode(Ball.MODE_FREE);
                if (other_ball.mode == Ball.MODE_COLLISION)
                    other_ball.setMode(Ball.MODE_FREE);
            }
            ball.integrate(now/1000f, elapsed/1000f);
            other_ball.integrate(now/1000f, elapsed/1000f);
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
                RectF boundary = new RectF(0, 0, w, h);
                //boundary.inset(30, 30);
                ball.setBoundary(boundary);
                ball.setPosition(w/2f, 5*h/8f);

                other_ball.setBoundary(boundary);
                other_ball.setPosition(w/2f, h/2f);
            }
        }

	public boolean doDown(MotionEvent e) {
            ball.setGoal(e.getX(), e.getY());
            ball.setConstants(800, 50);
            return true;
	}

	public boolean doScroll(MotionEvent e1, MotionEvent e2,
				float dX, float dY) {
            ball.setGoal(e2.getX(), e2.getY());
            ball.setConstants(10000, 100);
	    return true;
	}

	public boolean doFling (MotionEvent e1, MotionEvent e2,
				float vX, float vY) {
            //ball.setGoal(e2.getX(), e2.getY(), vX, vY);
            //ball.setConstants(10, 10);
            ball.setMode(Ball.MODE_FREE);
            ball.state.x = e2.getX();
            ball.state.y = e2.getY();
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

            public State(State S) {
                this(S.x, S.y, S.dx, S.dy);
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

            public State scale(float a) {
                State result = new State(this);
                result.x *= a;
                result.y *= a;
                result.dx *= a;
                result.dy *= a;
                return result;
            }

            public float norm() {
                return norm(x, y);
            }

            public float dnorm() {
                return norm(dx, dy);
            }

            private float norm(float u, float v) {
                return FloatMath.sqrt(u*u + v*v);
            }
        }

        protected class Derivative {
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

            public float dnorm() {
                return norm(dx, dy);
            }

            public float ddnorm() {
                return norm(ddx, ddy);
            }

            private float norm(float u, float v) {
                return FloatMath.sqrt(u*u + v*v);
            }
        }

        protected class Contact {
            public float nx, ny, depth;
            public State difference;

            public Contact scale(float a) {
                Contact result = new Contact();
                result.nx = a*nx;
                result.ny = a*ny;
                result.depth = depth;
                result.difference = difference.scale(a);
                return result;
            }

            public String toString() {
                return "n=(" + nx + "," + ny + "); depth=" + depth +
                    "; difference=" + difference.toString();
            }
        }

	private int color;

	protected float radius;
	protected float inverseMass;
	protected RectF boundary;
	protected RectF inset;

        protected final State state;

        protected final State goal;

        public static final int MODE_FREE = 1;
        public static final int MODE_FORCED = 2;
        public static final int MODE_COLLISION = 3;
        protected int mode;

        /** Spring tightness */
        protected float k;

        /** Damping coefficient */
        protected float b;

        protected float maxV;

        protected final long vibrate_length = 50;
        protected final Vibrator vibrator =
            (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);

	public Ball(int color, float radius, float maxV) {
	    this.color = color;
	    this.radius = radius;
	    this.inverseMass = 1.0f;
            this.maxV = maxV;

            state = new State(0,0,0,0);
            goal = new State(0,0,0,0);

            mode = MODE_FREE;

            setFreeConstants(0.005f, 10);
            // k = 800;
            // b = 50;
	}

	public void setPosition(float x, float y) {
            Log.d(TAG, "Ball: setPosition=" + x + ", " + y);
            state.x = goal.x = x;
            state.y = goal.y = y;
            state.dx = goal.dx = 0;
            state.dy = goal.dy = 0;
	}

	public void setGoal(float x, float y) {
            //Log.d(TAG, "Ball: setGoal=" + x + ", " + y);
            if (!inset.contains(x,y))
                return;
            goal.x = x;
            goal.y = y;
            goal.dx = goal.dy = 0;
            setMode(MODE_FORCED);
	}

	public void setGoal(float x, float y, float dx, float dy) {
            //Log.d(TAG, "Ball: setGoal=" + x + ", " + y);
            goal.x = x;
            goal.y = y;
            goal.dx = dx;
            goal.dy = dy;
            setMode(MODE_FREE);
	}

        public void setMode(int m) {
            mode = m;
            // if (mode == MODE_FORCED) {
            //     Log.d(TAG, "Set mode to forced");
            // } else if (mode == MODE_FREE) {
            //     Log.d(TAG, "Set mode to free");
            // }
        }

        public void setConstants(float k, float b) {
            this.k = k;
            this.b = b;
        }

        protected float k1, k2;
        public void setFreeConstants(float k1, float k2) {
            this.k1 = k1;
            this.k2 = k2;
        }

        protected float k3, k4;
        public void setCollisionConstants(float k3, float k4) {
            this.k3 = k3;
            this.k4 = k4;
        }


	public void setBoundary(RectF b) {
            boundary = b;
            inset = new RectF(b);
            inset.inset(radius, radius);
	}

	public void draw(Canvas canvas) {
	    Paint p = new Paint();
	    p.setColor(color);
	    p.setAntiAlias(true);
	    canvas.drawCircle(state.x, state.y, radius, p);
	}

        protected Derivative evaluate(float t, float dt, Derivative D) {
            final State S = new State();
            S.x = state.x + D.dx*dt;
            S.y = state.y + D.dy*dt;
            S.dx = state.dx + D.ddx*dt;
            S.dy = state.dy + D.ddy*dt;

            Derivative result = new Derivative();
            result.dx = S.dx;
            result.dy = S.dy;

            setAcceleration(result, S, t);
            return result;
        }

        protected void setAcceleration(Derivative D, State S, float time) {
            if (mode == MODE_FORCED) {
                State difference = S.difference(goal);
                D.ddx = -k * difference.x - b*difference.dx;
                D.ddy = -k * difference.y - b*difference.dy;
            } else if (mode == MODE_FREE) {
                D.ddx = - k1*FloatMath.sqrt(Math.abs(state.dx))*state.dx
                    - k2*state.dx;
                D.ddy = - k1*FloatMath.sqrt(Math.abs(state.dy))*state.dy
                    - k2*state.dy;;
            } else if (mode == MODE_COLLISION) {
                //Log.d(TAG, "Collision mode, contact=" + lastBallContact.toString());
                float sprod = lastBallContact.nx*lastBallContact.difference.dx +
                    lastBallContact.ny*lastBallContact.difference.dy;
                setConstants(k3*lastBallContact.depth, k4*sprod);
                D.ddx = (k*lastBallContact.depth - b)*lastBallContact.nx;
                D.ddy = (k*lastBallContact.depth - b)*lastBallContact.ny;
            }
            else {
                Log.e(TAG, "Unknown mode " + mode);
            }

            //D.ddx *= inverseMass;
            //D.ddy *= inverseMass;
        }

        // private int mycounter = 0;
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

            float velocity = state.dnorm();
            if (velocity > maxV) {
                state.dx = maxV*state.dx/velocity;
                state.dy = maxV*state.dy/velocity;
            }

            fixBoundaryCollision();

            // if (++mycounter > 20) {
            //     Log.d(TAG, "New state is " + state.toString());
            //     mycounter = 0;
            // }
        }

        protected final RectF box = new RectF();

        private final Contact lastContact = new Contact();
        protected boolean fixBoundaryCollision() {
            box.set(state.x-radius, state.y-radius,
                    state.x+radius, state.y+radius);
            box.union(boundary);

            Contact c = lastContact;
            c.nx = c.ny = 0;

            c.depth = box.width() - boundary.width();
            if (c.depth > 0) {
                c.nx = Math.signum(boundary.centerX() - state.x);
                state.x += c.depth*c.nx;
                state.y += c.depth*c.ny*state.dy/state.dx;
                state.dx *= -1;
                return true;
            }

            c.depth = box.height() - boundary.height();
            if (c.depth > 0) {
                c.ny = Math.signum(boundary.centerY() - state.y);
                state.x += c.depth*c.ny*state.dx/state.dy;
                state.y += c.depth*c.ny;
                state.dy *= -1;
                return true;
            }

            return false;
        }

        protected Contact lastBallContact = new Contact();
        protected boolean detectOtherBallCollision(Ball other) {
            State difference = state.difference(other.state);
            float distance = difference.norm();
            float depth = this.radius + other.radius - distance;
            if (depth < 0)
                return false;

            lastBallContact.nx = difference.x/distance;
            lastBallContact.ny = difference.y/distance;
            lastBallContact.depth = depth;
            lastBallContact.difference = difference;
            return true;
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
            return thread.doDown(e);
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float dX, float dY) {
	    //player.moveTo(e2.getX(), e2.getY());
            //Log.d(TAG, "onScroll: " + e2.toString());
            return thread.doScroll(e1, e2, dX, dY);
	}

	@Override
	public boolean onFling (MotionEvent e1, MotionEvent e2,
                                float vX, float vY) {
            //Log.d(TAG, "onFling" + e1.toString());
            return thread.doFling(e1, e2, vX, vY);
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap: " + e.toString());
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
            //thread.doSingleTap(e);
	    return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
            //Log.d(TAG, "onDoubleTapEvent: " + e.toString());
	    return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
            //Log.d(TAG, "onSingleTapConfirmed: " + e.toString());
            return true;
	}
    }
}
