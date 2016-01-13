package com.dianping.base.widget.wheel.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class WheelScroller
{
  private static final int MESSAGE_JUSTIFY = 1;
  private static final int MESSAGE_SCROLL = 0;
  public static final int MIN_DELTA_FOR_SCROLLING = 1;
  private static final int SCROLLING_DURATION = 400;
  Handler animationHandler = new Handler()
  {
    public void handleMessage(Message paramMessage)
    {
      WheelScroller.this.scroller.computeScrollOffset();
      int i = WheelScroller.this.scroller.getCurrY();
      int j = WheelScroller.this.lastScrollY - i;
      WheelScroller.this.lastScrollY = i;
      if (j != 0)
        WheelScroller.this.listener.onScroll(j);
      if (Math.abs(i - WheelScroller.this.scroller.getFinalY()) < 1)
      {
        WheelScroller.this.scroller.getFinalY();
        WheelScroller.this.scroller.forceFinished(true);
      }
      if (!WheelScroller.this.scroller.isFinished())
      {
        WheelScroller.this.animationHandler.sendEmptyMessage(paramMessage.what);
        return;
      }
      if (paramMessage.what == 0)
      {
        WheelScroller.this.justify();
        return;
      }
      WheelScroller.this.finishScrolling();
    }
  };
  private Context context;
  private GestureDetector gestureDetector = new GestureDetector(paramContext, this.gestureListener);
  private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener()
  {
    public boolean onFling(MotionEvent paramMotionEvent1, MotionEvent paramMotionEvent2, float paramFloat1, float paramFloat2)
    {
      WheelScroller.this.lastScrollY = 0;
      WheelScroller.this.scroller.fling(0, WheelScroller.this.lastScrollY, 0, (int)(-paramFloat2), 0, 0, -2147483647, 2147483647);
      WheelScroller.this.setNextMessage(0);
      return true;
    }

    public boolean onScroll(MotionEvent paramMotionEvent1, MotionEvent paramMotionEvent2, float paramFloat1, float paramFloat2)
    {
      return true;
    }
  };
  private boolean isScrollingPerformed;
  int lastScrollY;
  private float lastTouchedY;
  ScrollingListener listener;
  Scroller scroller;

  public WheelScroller(Context paramContext, ScrollingListener paramScrollingListener)
  {
    this.gestureDetector.setIsLongpressEnabled(false);
    this.scroller = new Scroller(paramContext);
    this.listener = paramScrollingListener;
    this.context = paramContext;
  }

  private void clearMessages()
  {
    this.animationHandler.removeMessages(0);
    this.animationHandler.removeMessages(1);
  }

  private void startScrolling()
  {
    if (!this.isScrollingPerformed)
    {
      this.isScrollingPerformed = true;
      this.listener.onStarted();
    }
  }

  void finishScrolling()
  {
    if (this.isScrollingPerformed)
    {
      this.listener.onFinished();
      this.isScrollingPerformed = false;
    }
  }

  void justify()
  {
    this.listener.onJustify();
    setNextMessage(1);
  }

  public boolean onTouchEvent(MotionEvent paramMotionEvent)
  {
    switch (paramMotionEvent.getAction())
    {
    case 1:
    default:
    case 0:
    case 2:
    }
    while (true)
    {
      if ((!this.gestureDetector.onTouchEvent(paramMotionEvent)) && (paramMotionEvent.getAction() == 1))
        justify();
      return true;
      this.lastTouchedY = paramMotionEvent.getY();
      this.scroller.forceFinished(true);
      clearMessages();
      continue;
      int i = (int)(paramMotionEvent.getY() - this.lastTouchedY);
      if (i == 0)
        continue;
      startScrolling();
      this.listener.onScroll(i);
      this.lastTouchedY = paramMotionEvent.getY();
    }
  }

  public void scroll(int paramInt1, int paramInt2)
  {
    this.scroller.forceFinished(true);
    this.lastScrollY = 0;
    Scroller localScroller = this.scroller;
    if (paramInt2 != 0);
    while (true)
    {
      localScroller.startScroll(0, 0, 0, paramInt1, paramInt2);
      setNextMessage(0);
      startScrolling();
      return;
      paramInt2 = 400;
    }
  }

  public void setInterpolator(Interpolator paramInterpolator)
  {
    this.scroller.forceFinished(true);
    this.scroller = new Scroller(this.context, paramInterpolator);
  }

  void setNextMessage(int paramInt)
  {
    clearMessages();
    this.animationHandler.sendEmptyMessage(paramInt);
  }

  public void stopScrolling()
  {
    this.scroller.forceFinished(true);
  }

  public static abstract interface ScrollingListener
  {
    public abstract void onFinished();

    public abstract void onJustify();

    public abstract void onScroll(int paramInt);

    public abstract void onStarted();
  }
}

/* Location:           C:\Users\xuetong\Desktop\dazhongdianping7.9.6\ProjectSrc\classes-dex2jar.jar
 * Qualified Name:     com.dianping.base.widget.wheel.widget.WheelScroller
 * JD-Core Version:    0.6.0
 */