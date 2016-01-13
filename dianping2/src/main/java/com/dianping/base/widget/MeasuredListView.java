package com.dianping.base.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import com.dianping.widget.view.NovaListView;

public class MeasuredListView extends NovaListView
{
  public MeasuredListView(Context paramContext)
  {
    super(paramContext);
  }

  public MeasuredListView(Context paramContext, AttributeSet paramAttributeSet)
  {
    super(paramContext, paramAttributeSet);
  }

  public MeasuredListView(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
  {
    super(paramContext, paramAttributeSet, paramInt);
  }

  public void onMeasure(int paramInt1, int paramInt2)
  {
    super.onMeasure(paramInt1, View.MeasureSpec.makeMeasureSpec(536870911, -2147483648));
  }
}

/* Location:           C:\Users\xuetong\Desktop\dazhongdianping7.9.6\ProjectSrc\classes-dex2jar.jar
 * Qualified Name:     com.dianping.base.widget.MeasuredListView
 * JD-Core Version:    0.6.0
 */