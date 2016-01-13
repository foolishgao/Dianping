package android.support.v7.widget;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class StaggeredGridLayoutManager extends RecyclerView.LayoutManager
{
  private static final boolean DEBUG = false;

  @Deprecated
  public static final int GAP_HANDLING_LAZY = 1;
  public static final int GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS = 2;
  public static final int GAP_HANDLING_NONE = 0;
  public static final int HORIZONTAL = 0;
  private static final int INVALID_OFFSET = -2147483648;
  public static final String TAG = "StaggeredGridLayoutManager";
  public static final int VERTICAL = 1;
  private final AnchorInfo mAnchorInfo = new AnchorInfo(null);
  private final Runnable mCheckForGapsRunnable = new Runnable()
  {
    public void run()
    {
      StaggeredGridLayoutManager.this.checkForGaps();
    }
  };
  private int mFullSizeSpec;
  private int mGapStrategy = 2;
  private int mHeightSpec;
  private boolean mLaidOutInvalidFullSpan = false;
  private boolean mLastLayoutFromEnd;
  private boolean mLastLayoutRTL;
  private LayoutState mLayoutState;
  LazySpanLookup mLazySpanLookup = new LazySpanLookup();
  private int mOrientation;
  private SavedState mPendingSavedState;
  int mPendingScrollPosition = -1;
  int mPendingScrollPositionOffset = -2147483648;
  OrientationHelper mPrimaryOrientation;
  private BitSet mRemainingSpans;
  private boolean mReverseLayout = false;
  OrientationHelper mSecondaryOrientation;
  boolean mShouldReverseLayout = false;
  private int mSizePerSpan;
  private boolean mSmoothScrollbarEnabled = true;
  private int mSpanCount = -1;
  private Span[] mSpans;
  private final Rect mTmpRect = new Rect();
  private int mWidthSpec;

  public StaggeredGridLayoutManager(int paramInt1, int paramInt2)
  {
    this.mOrientation = paramInt2;
    setSpanCount(paramInt1);
  }

  public StaggeredGridLayoutManager(Context paramContext, AttributeSet paramAttributeSet, int paramInt1, int paramInt2)
  {
    paramContext = getProperties(paramContext, paramAttributeSet, paramInt1, paramInt2);
    setOrientation(paramContext.orientation);
    setSpanCount(paramContext.spanCount);
    setReverseLayout(paramContext.reverseLayout);
  }

  private void appendViewToAllSpans(View paramView)
  {
    int i = this.mSpanCount - 1;
    while (i >= 0)
    {
      this.mSpans[i].appendToSpan(paramView);
      i -= 1;
    }
  }

  private void applyPendingSavedState(AnchorInfo paramAnchorInfo)
  {
    if (this.mPendingSavedState.mSpanOffsetsSize > 0)
      if (this.mPendingSavedState.mSpanOffsetsSize == this.mSpanCount)
      {
        int j = 0;
        if (j < this.mSpanCount)
        {
          this.mSpans[j].clear();
          int k = this.mPendingSavedState.mSpanOffsets[j];
          int i = k;
          if (k != -2147483648)
            if (!this.mPendingSavedState.mAnchorLayoutFromEnd)
              break label102;
          label102: for (i = k + this.mPrimaryOrientation.getEndAfterPadding(); ; i = k + this.mPrimaryOrientation.getStartAfterPadding())
          {
            this.mSpans[j].setLine(i);
            j += 1;
            break;
          }
        }
      }
      else
      {
        this.mPendingSavedState.invalidateSpanInfo();
        this.mPendingSavedState.mAnchorPosition = this.mPendingSavedState.mVisibleAnchorPosition;
      }
    this.mLastLayoutRTL = this.mPendingSavedState.mLastLayoutRTL;
    setReverseLayout(this.mPendingSavedState.mReverseLayout);
    resolveShouldLayoutReverse();
    if (this.mPendingSavedState.mAnchorPosition != -1)
      this.mPendingScrollPosition = this.mPendingSavedState.mAnchorPosition;
    for (paramAnchorInfo.mLayoutFromEnd = this.mPendingSavedState.mAnchorLayoutFromEnd; ; paramAnchorInfo.mLayoutFromEnd = this.mShouldReverseLayout)
    {
      if (this.mPendingSavedState.mSpanLookupSize > 1)
      {
        this.mLazySpanLookup.mData = this.mPendingSavedState.mSpanLookup;
        this.mLazySpanLookup.mFullSpanItems = this.mPendingSavedState.mFullSpanItems;
      }
      return;
    }
  }

  private void attachViewToSpans(View paramView, LayoutParams paramLayoutParams, LayoutState paramLayoutState)
  {
    if (paramLayoutState.mLayoutDirection == 1)
    {
      if (paramLayoutParams.mFullSpan)
      {
        appendViewToAllSpans(paramView);
        return;
      }
      paramLayoutParams.mSpan.appendToSpan(paramView);
      return;
    }
    if (paramLayoutParams.mFullSpan)
    {
      prependViewToAllSpans(paramView);
      return;
    }
    paramLayoutParams.mSpan.prependToSpan(paramView);
  }

  private int calculateScrollDirectionForPosition(int paramInt)
  {
    int i = -1;
    if (getChildCount() == 0)
    {
      if (this.mShouldReverseLayout)
        return 1;
      return -1;
    }
    int j;
    if (paramInt < getFirstChildPosition())
    {
      j = 1;
      if (j == this.mShouldReverseLayout)
        break label47;
    }
    label47: for (paramInt = i; ; paramInt = 1)
    {
      return paramInt;
      j = 0;
      break;
    }
  }

  private boolean checkForGaps()
  {
    if ((getChildCount() == 0) || (this.mGapStrategy == 0) || (!isAttachedToWindow()))
      return false;
    int j;
    if (this.mShouldReverseLayout)
      j = getLastChildPosition();
    for (int i = getFirstChildPosition(); (j == 0) && (hasGapsToFix() != null); i = getLastChildPosition())
    {
      this.mLazySpanLookup.clear();
      requestSimpleAnimationsInNextLayout();
      requestLayout();
      return true;
      j = getFirstChildPosition();
    }
    if (!this.mLaidOutInvalidFullSpan)
      return false;
    if (this.mShouldReverseLayout);
    StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem localFullSpanItem1;
    for (int k = -1; ; k = 1)
    {
      localFullSpanItem1 = this.mLazySpanLookup.getFirstFullSpanItemInRange(j, i + 1, k, true);
      if (localFullSpanItem1 != null)
        break;
      this.mLaidOutInvalidFullSpan = false;
      this.mLazySpanLookup.forceInvalidateAfter(i + 1);
      return false;
    }
    StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem localFullSpanItem2 = this.mLazySpanLookup.getFirstFullSpanItemInRange(j, localFullSpanItem1.mPosition, k * -1, true);
    if (localFullSpanItem2 == null)
      this.mLazySpanLookup.forceInvalidateAfter(localFullSpanItem1.mPosition);
    while (true)
    {
      requestSimpleAnimationsInNextLayout();
      requestLayout();
      return true;
      this.mLazySpanLookup.forceInvalidateAfter(localFullSpanItem2.mPosition + 1);
    }
  }

  private boolean checkSpanForGap(Span paramSpan)
  {
    if (this.mShouldReverseLayout)
    {
      if (paramSpan.getEndLine() >= this.mPrimaryOrientation.getEndAfterPadding());
    }
    else
      do
        return true;
      while (paramSpan.getStartLine() > this.mPrimaryOrientation.getStartAfterPadding());
    return false;
  }

  private int computeScrollExtent(RecyclerView.State paramState)
  {
    boolean bool2 = false;
    if (getChildCount() == 0)
      return 0;
    ensureOrientationHelper();
    OrientationHelper localOrientationHelper = this.mPrimaryOrientation;
    if (!this.mSmoothScrollbarEnabled);
    for (boolean bool1 = true; ; bool1 = false)
    {
      View localView = findFirstVisibleItemClosestToStart(bool1, true);
      bool1 = bool2;
      if (!this.mSmoothScrollbarEnabled)
        bool1 = true;
      return ScrollbarHelper.computeScrollExtent(paramState, localOrientationHelper, localView, findFirstVisibleItemClosestToEnd(bool1, true), this, this.mSmoothScrollbarEnabled);
    }
  }

  private int computeScrollOffset(RecyclerView.State paramState)
  {
    boolean bool2 = false;
    if (getChildCount() == 0)
      return 0;
    ensureOrientationHelper();
    OrientationHelper localOrientationHelper = this.mPrimaryOrientation;
    if (!this.mSmoothScrollbarEnabled);
    for (boolean bool1 = true; ; bool1 = false)
    {
      View localView = findFirstVisibleItemClosestToStart(bool1, true);
      bool1 = bool2;
      if (!this.mSmoothScrollbarEnabled)
        bool1 = true;
      return ScrollbarHelper.computeScrollOffset(paramState, localOrientationHelper, localView, findFirstVisibleItemClosestToEnd(bool1, true), this, this.mSmoothScrollbarEnabled, this.mShouldReverseLayout);
    }
  }

  private int computeScrollRange(RecyclerView.State paramState)
  {
    boolean bool2 = false;
    if (getChildCount() == 0)
      return 0;
    ensureOrientationHelper();
    OrientationHelper localOrientationHelper = this.mPrimaryOrientation;
    if (!this.mSmoothScrollbarEnabled);
    for (boolean bool1 = true; ; bool1 = false)
    {
      View localView = findFirstVisibleItemClosestToStart(bool1, true);
      bool1 = bool2;
      if (!this.mSmoothScrollbarEnabled)
        bool1 = true;
      return ScrollbarHelper.computeScrollRange(paramState, localOrientationHelper, localView, findFirstVisibleItemClosestToEnd(bool1, true), this, this.mSmoothScrollbarEnabled);
    }
  }

  private StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem createFullSpanItemFromEnd(int paramInt)
  {
    StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem localFullSpanItem = new StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem();
    localFullSpanItem.mGapPerSpan = new int[this.mSpanCount];
    int i = 0;
    while (i < this.mSpanCount)
    {
      localFullSpanItem.mGapPerSpan[i] = (paramInt - this.mSpans[i].getEndLine(paramInt));
      i += 1;
    }
    return localFullSpanItem;
  }

  private StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem createFullSpanItemFromStart(int paramInt)
  {
    StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem localFullSpanItem = new StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem();
    localFullSpanItem.mGapPerSpan = new int[this.mSpanCount];
    int i = 0;
    while (i < this.mSpanCount)
    {
      localFullSpanItem.mGapPerSpan[i] = (this.mSpans[i].getStartLine(paramInt) - paramInt);
      i += 1;
    }
    return localFullSpanItem;
  }

  private void ensureOrientationHelper()
  {
    if (this.mPrimaryOrientation == null)
    {
      this.mPrimaryOrientation = OrientationHelper.createOrientationHelper(this, this.mOrientation);
      this.mSecondaryOrientation = OrientationHelper.createOrientationHelper(this, 1 - this.mOrientation);
      this.mLayoutState = new LayoutState();
    }
  }

  private int fill(RecyclerView.Recycler paramRecycler, LayoutState paramLayoutState, RecyclerView.State paramState)
  {
    this.mRemainingSpans.set(0, this.mSpanCount, true);
    int j;
    int k;
    label58: label61: View localView;
    LayoutParams localLayoutParams;
    int i3;
    int i1;
    label123: Span localSpan;
    label144: label155: label176: label208: int i2;
    int m;
    int n;
    StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem localFullSpanItem;
    if (paramLayoutState.mLayoutDirection == 1)
    {
      j = paramLayoutState.mEndLine + paramLayoutState.mAvailable;
      updateAllRemainingSpans(paramLayoutState.mLayoutDirection, j);
      if (!this.mShouldReverseLayout)
        break label427;
      k = this.mPrimaryOrientation.getEndAfterPadding();
      i = 0;
      if ((!paramLayoutState.hasMore(paramState)) || (this.mRemainingSpans.isEmpty()))
        break label737;
      localView = paramLayoutState.next(paramRecycler);
      localLayoutParams = (LayoutParams)localView.getLayoutParams();
      i3 = localLayoutParams.getViewLayoutPosition();
      i = this.mLazySpanLookup.getSpan(i3);
      if (i != -1)
        break label439;
      i1 = 1;
      if (i1 == 0)
        break label455;
      if (!localLayoutParams.mFullSpan)
        break label445;
      localSpan = this.mSpans[0];
      this.mLazySpanLookup.setSpan(i3, localSpan);
      localLayoutParams.mSpan = localSpan;
      if (paramLayoutState.mLayoutDirection != 1)
        break label467;
      addView(localView);
      measureChildWithDecorationsAndMargin(localView, localLayoutParams);
      if (paramLayoutState.mLayoutDirection != 1)
        break label489;
      if (!localLayoutParams.mFullSpan)
        break label477;
      i = getMaxEnd(k);
      i2 = i + this.mPrimaryOrientation.getDecoratedMeasurement(localView);
      m = i;
      n = i2;
      if (i1 != 0)
      {
        m = i;
        n = i2;
        if (localLayoutParams.mFullSpan)
        {
          localFullSpanItem = createFullSpanItemFromEnd(i);
          localFullSpanItem.mGapDir = -1;
          localFullSpanItem.mPosition = i3;
          this.mLazySpanLookup.addFullSpanItem(localFullSpanItem);
          n = i2;
          m = i;
        }
      }
      if ((localLayoutParams.mFullSpan) && (paramLayoutState.mItemDirection == -1))
      {
        if (i1 == 0)
          break label601;
        this.mLaidOutInvalidFullSpan = true;
      }
      attachViewToSpans(localView, localLayoutParams, paramLayoutState);
      if (!localLayoutParams.mFullSpan)
        break label679;
      i = this.mSecondaryOrientation.getStartAfterPadding();
      label341: i1 = i + this.mSecondaryOrientation.getDecoratedMeasurement(localView);
      if (this.mOrientation != 1)
        break label702;
      layoutDecoratedWithMargins(localView, i, m, i1, n);
      label377: if (!localLayoutParams.mFullSpan)
        break label719;
      updateAllRemainingSpans(this.mLayoutState.mLayoutDirection, j);
    }
    while (true)
    {
      recycle(paramRecycler, this.mLayoutState);
      i = 1;
      break label61;
      j = paramLayoutState.mStartLine - paramLayoutState.mAvailable;
      break;
      label427: k = this.mPrimaryOrientation.getStartAfterPadding();
      break label58;
      label439: i1 = 0;
      break label123;
      label445: localSpan = getNextSpan(paramLayoutState);
      break label144;
      label455: localSpan = this.mSpans[i];
      break label155;
      label467: addView(localView, 0);
      break label176;
      label477: i = localSpan.getEndLine(k);
      break label208;
      label489: if (localLayoutParams.mFullSpan);
      for (i = getMinStart(k); ; i = localSpan.getStartLine(k))
      {
        i2 = i - this.mPrimaryOrientation.getDecoratedMeasurement(localView);
        m = i2;
        n = i;
        if (i1 == 0)
          break;
        m = i2;
        n = i;
        if (!localLayoutParams.mFullSpan)
          break;
        localFullSpanItem = createFullSpanItemFromStart(i);
        localFullSpanItem.mGapDir = 1;
        localFullSpanItem.mPosition = i3;
        this.mLazySpanLookup.addFullSpanItem(localFullSpanItem);
        m = i2;
        n = i;
        break;
      }
      label601: if (paramLayoutState.mLayoutDirection == 1)
      {
        if (!areAllEndsEqual());
        for (i = 1; ; i = 0)
        {
          label619: if (i == 0)
            break label671;
          localFullSpanItem = this.mLazySpanLookup.getFullSpanItem(i3);
          if (localFullSpanItem != null)
            localFullSpanItem.mHasUnwantedGapAfter = true;
          this.mLaidOutInvalidFullSpan = true;
          break;
        }
      }
      if (!areAllStartsEqual());
      for (i = 1; ; i = 0)
      {
        break label619;
        label671: break;
      }
      label679: i = localSpan.mIndex * this.mSizePerSpan + this.mSecondaryOrientation.getStartAfterPadding();
      break label341;
      label702: layoutDecoratedWithMargins(localView, m, i, n, i1);
      break label377;
      label719: updateRemainingSpans(localSpan, this.mLayoutState.mLayoutDirection, j);
    }
    label737: if (i == 0)
      recycle(paramRecycler, this.mLayoutState);
    if (this.mLayoutState.mLayoutDirection == -1)
      i = getMinStart(this.mPrimaryOrientation.getStartAfterPadding());
    for (int i = this.mPrimaryOrientation.getStartAfterPadding() - i; i > 0; i = getMaxEnd(this.mPrimaryOrientation.getEndAfterPadding()) - this.mPrimaryOrientation.getEndAfterPadding())
      return Math.min(paramLayoutState.mAvailable, i);
    return 0;
  }

  private int findFirstReferenceChildPosition(int paramInt)
  {
    int j = getChildCount();
    int i = 0;
    while (i < j)
    {
      int k = getPosition(getChildAt(i));
      if ((k >= 0) && (k < paramInt))
        return k;
      i += 1;
    }
    return 0;
  }

  private int findLastReferenceChildPosition(int paramInt)
  {
    int i = getChildCount() - 1;
    while (i >= 0)
    {
      int j = getPosition(getChildAt(i));
      if ((j >= 0) && (j < paramInt))
        return j;
      i -= 1;
    }
    return 0;
  }

  private void fixEndGap(RecyclerView.Recycler paramRecycler, RecyclerView.State paramState, boolean paramBoolean)
  {
    int i = getMaxEnd(this.mPrimaryOrientation.getEndAfterPadding());
    i = this.mPrimaryOrientation.getEndAfterPadding() - i;
    if (i > 0)
    {
      i -= -scrollBy(-i, paramRecycler, paramState);
      if ((paramBoolean) && (i > 0))
        this.mPrimaryOrientation.offsetChildren(i);
    }
  }

  private void fixStartGap(RecyclerView.Recycler paramRecycler, RecyclerView.State paramState, boolean paramBoolean)
  {
    int i = getMinStart(this.mPrimaryOrientation.getStartAfterPadding()) - this.mPrimaryOrientation.getStartAfterPadding();
    if (i > 0)
    {
      i -= scrollBy(i, paramRecycler, paramState);
      if ((paramBoolean) && (i > 0))
        this.mPrimaryOrientation.offsetChildren(-i);
    }
  }

  private int getFirstChildPosition()
  {
    if (getChildCount() == 0)
      return 0;
    return getPosition(getChildAt(0));
  }

  private int getLastChildPosition()
  {
    int i = getChildCount();
    if (i == 0)
      return 0;
    return getPosition(getChildAt(i - 1));
  }

  private int getMaxEnd(int paramInt)
  {
    int j = this.mSpans[0].getEndLine(paramInt);
    int i = 1;
    while (i < this.mSpanCount)
    {
      int m = this.mSpans[i].getEndLine(paramInt);
      int k = j;
      if (m > j)
        k = m;
      i += 1;
      j = k;
    }
    return j;
  }

  private int getMaxStart(int paramInt)
  {
    int j = this.mSpans[0].getStartLine(paramInt);
    int i = 1;
    while (i < this.mSpanCount)
    {
      int m = this.mSpans[i].getStartLine(paramInt);
      int k = j;
      if (m > j)
        k = m;
      i += 1;
      j = k;
    }
    return j;
  }

  private int getMinEnd(int paramInt)
  {
    int j = this.mSpans[0].getEndLine(paramInt);
    int i = 1;
    while (i < this.mSpanCount)
    {
      int m = this.mSpans[i].getEndLine(paramInt);
      int k = j;
      if (m < j)
        k = m;
      i += 1;
      j = k;
    }
    return j;
  }

  private int getMinStart(int paramInt)
  {
    int j = this.mSpans[0].getStartLine(paramInt);
    int i = 1;
    while (i < this.mSpanCount)
    {
      int m = this.mSpans[i].getStartLine(paramInt);
      int k = j;
      if (m < j)
        k = m;
      i += 1;
      j = k;
    }
    return j;
  }

  private Span getNextSpan(LayoutState paramLayoutState)
  {
    int i;
    int k;
    int j;
    if (preferLastSpan(paramLayoutState.mLayoutDirection))
    {
      i = this.mSpanCount - 1;
      k = -1;
      j = -1;
    }
    Object localObject;
    while (true)
    {
      int i1;
      int n;
      if (paramLayoutState.mLayoutDirection == 1)
      {
        paramLayoutState = null;
        m = 2147483647;
        i2 = this.mPrimaryOrientation.getStartAfterPadding();
        while (true)
        {
          localObject = paramLayoutState;
          if (i == k)
            break;
          localObject = this.mSpans[i];
          i1 = ((Span)localObject).getEndLine(i2);
          n = m;
          if (i1 < m)
          {
            paramLayoutState = (LayoutState)localObject;
            n = i1;
          }
          i += j;
          m = n;
        }
        i = 0;
        k = this.mSpanCount;
        j = 1;
        continue;
      }
      paramLayoutState = null;
      int m = -2147483648;
      int i2 = this.mPrimaryOrientation.getEndAfterPadding();
      while (i != k)
      {
        localObject = this.mSpans[i];
        i1 = ((Span)localObject).getStartLine(i2);
        n = m;
        if (i1 > m)
        {
          paramLayoutState = (LayoutState)localObject;
          n = i1;
        }
        i += j;
        m = n;
      }
      localObject = paramLayoutState;
    }
    return (Span)localObject;
  }

  private int getSpecForDimension(int paramInt1, int paramInt2)
  {
    if (paramInt1 < 0)
      return paramInt2;
    return View.MeasureSpec.makeMeasureSpec(paramInt1, 1073741824);
  }

  private void handleUpdate(int paramInt1, int paramInt2, int paramInt3)
  {
    int k;
    int j;
    int i;
    if (this.mShouldReverseLayout)
    {
      k = getLastChildPosition();
      if (paramInt3 != 3)
        break label100;
      if (paramInt1 >= paramInt2)
        break label89;
      j = paramInt2 + 1;
      i = paramInt1;
      label31: this.mLazySpanLookup.invalidateAfter(i);
      switch (paramInt3)
      {
      case 2:
      default:
        label72: if (j > k)
          break;
      case 0:
      case 1:
      case 3:
      }
    }
    while (true)
    {
      return;
      k = getFirstChildPosition();
      break;
      label89: j = paramInt1 + 1;
      i = paramInt2;
      break label31;
      label100: i = paramInt1;
      j = paramInt1 + paramInt2;
      break label31;
      this.mLazySpanLookup.offsetForAddition(paramInt1, paramInt2);
      break label72;
      this.mLazySpanLookup.offsetForRemoval(paramInt1, paramInt2);
      break label72;
      this.mLazySpanLookup.offsetForRemoval(paramInt1, 1);
      this.mLazySpanLookup.offsetForAddition(paramInt2, 1);
      break label72;
      if (this.mShouldReverseLayout);
      for (paramInt1 = getFirstChildPosition(); i <= paramInt1; paramInt1 = getLastChildPosition())
      {
        requestLayout();
        return;
      }
    }
  }

  private void layoutDecoratedWithMargins(View paramView, int paramInt1, int paramInt2, int paramInt3, int paramInt4)
  {
    LayoutParams localLayoutParams = (LayoutParams)paramView.getLayoutParams();
    layoutDecorated(paramView, paramInt1 + localLayoutParams.leftMargin, paramInt2 + localLayoutParams.topMargin, paramInt3 - localLayoutParams.rightMargin, paramInt4 - localLayoutParams.bottomMargin);
  }

  private void measureChildWithDecorationsAndMargin(View paramView, int paramInt1, int paramInt2)
  {
    calculateItemDecorationsForChild(paramView, this.mTmpRect);
    LayoutParams localLayoutParams = (LayoutParams)paramView.getLayoutParams();
    paramView.measure(updateSpecWithExtra(paramInt1, localLayoutParams.leftMargin + this.mTmpRect.left, localLayoutParams.rightMargin + this.mTmpRect.right), updateSpecWithExtra(paramInt2, localLayoutParams.topMargin + this.mTmpRect.top, localLayoutParams.bottomMargin + this.mTmpRect.bottom));
  }

  private void measureChildWithDecorationsAndMargin(View paramView, LayoutParams paramLayoutParams)
  {
    if (paramLayoutParams.mFullSpan)
    {
      if (this.mOrientation == 1)
      {
        measureChildWithDecorationsAndMargin(paramView, this.mFullSizeSpec, getSpecForDimension(paramLayoutParams.height, this.mHeightSpec));
        return;
      }
      measureChildWithDecorationsAndMargin(paramView, getSpecForDimension(paramLayoutParams.width, this.mWidthSpec), this.mFullSizeSpec);
      return;
    }
    if (this.mOrientation == 1)
    {
      measureChildWithDecorationsAndMargin(paramView, this.mWidthSpec, getSpecForDimension(paramLayoutParams.height, this.mHeightSpec));
      return;
    }
    measureChildWithDecorationsAndMargin(paramView, getSpecForDimension(paramLayoutParams.width, this.mWidthSpec), this.mHeightSpec);
  }

  private boolean preferLastSpan(int paramInt)
  {
    int i;
    if (this.mOrientation == 0)
      if (paramInt == -1)
      {
        i = 1;
        if (i == this.mShouldReverseLayout)
          break label29;
      }
    label29: label63: label66: 
    while (true)
    {
      return true;
      i = 0;
      break;
      return false;
      if (paramInt == -1)
      {
        i = 1;
        if (i != this.mShouldReverseLayout)
          break label63;
      }
      for (i = 1; ; i = 0)
      {
        if (i == isLayoutRTL())
          break label66;
        return false;
        i = 0;
        break;
      }
    }
  }

  private void prependViewToAllSpans(View paramView)
  {
    int i = this.mSpanCount - 1;
    while (i >= 0)
    {
      this.mSpans[i].prependToSpan(paramView);
      i -= 1;
    }
  }

  private void recycle(RecyclerView.Recycler paramRecycler, LayoutState paramLayoutState)
  {
    if (paramLayoutState.mAvailable == 0)
    {
      if (paramLayoutState.mLayoutDirection == -1)
      {
        recycleFromEnd(paramRecycler, paramLayoutState.mEndLine);
        return;
      }
      recycleFromStart(paramRecycler, paramLayoutState.mStartLine);
      return;
    }
    if (paramLayoutState.mLayoutDirection == -1)
    {
      i = paramLayoutState.mStartLine - getMaxStart(paramLayoutState.mStartLine);
      if (i < 0);
      for (i = paramLayoutState.mEndLine; ; i = paramLayoutState.mEndLine - Math.min(i, paramLayoutState.mAvailable))
      {
        recycleFromEnd(paramRecycler, i);
        return;
      }
    }
    int i = getMinEnd(paramLayoutState.mEndLine) - paramLayoutState.mEndLine;
    if (i < 0);
    for (i = paramLayoutState.mStartLine; ; i = paramLayoutState.mStartLine + Math.min(i, paramLayoutState.mAvailable))
    {
      recycleFromStart(paramRecycler, i);
      return;
    }
  }

  private void recycleFromEnd(RecyclerView.Recycler paramRecycler, int paramInt)
  {
    int i = getChildCount() - 1;
    while (true)
    {
      View localView;
      LayoutParams localLayoutParams;
      int j;
      if (i >= 0)
      {
        localView = getChildAt(i);
        if (this.mPrimaryOrientation.getDecoratedStart(localView) >= paramInt)
        {
          localLayoutParams = (LayoutParams)localView.getLayoutParams();
          if (!localLayoutParams.mFullSpan)
            break label119;
          j = 0;
          if (j >= this.mSpanCount)
            break label88;
          if (this.mSpans[j].mViews.size() != 1)
            break label79;
        }
      }
      label79: label88: label119: 
      do
      {
        return;
        j += 1;
        break;
        j = 0;
        while (j < this.mSpanCount)
        {
          this.mSpans[j].popEnd();
          j += 1;
        }
      }
      while (localLayoutParams.mSpan.mViews.size() == 1);
      localLayoutParams.mSpan.popEnd();
      removeAndRecycleView(localView, paramRecycler);
      i -= 1;
    }
  }

  private void recycleFromStart(RecyclerView.Recycler paramRecycler, int paramInt)
  {
    while (true)
    {
      View localView;
      LayoutParams localLayoutParams;
      int i;
      if (getChildCount() > 0)
      {
        localView = getChildAt(0);
        if (this.mPrimaryOrientation.getDecoratedEnd(localView) <= paramInt)
        {
          localLayoutParams = (LayoutParams)localView.getLayoutParams();
          if (!localLayoutParams.mFullSpan)
            break label112;
          i = 0;
          if (i >= this.mSpanCount)
            break label81;
          if (this.mSpans[i].mViews.size() != 1)
            break label72;
        }
      }
      label72: label81: label112: 
      do
      {
        return;
        i += 1;
        break;
        i = 0;
        while (i < this.mSpanCount)
        {
          this.mSpans[i].popStart();
          i += 1;
        }
      }
      while (localLayoutParams.mSpan.mViews.size() == 1);
      localLayoutParams.mSpan.popStart();
      removeAndRecycleView(localView, paramRecycler);
    }
  }

  private void resolveShouldLayoutReverse()
  {
    boolean bool = true;
    if ((this.mOrientation == 1) || (!isLayoutRTL()))
    {
      this.mShouldReverseLayout = this.mReverseLayout;
      return;
    }
    if (!this.mReverseLayout);
    while (true)
    {
      this.mShouldReverseLayout = bool;
      return;
      bool = false;
    }
  }

  private void setLayoutStateDirection(int paramInt)
  {
    int i = 1;
    this.mLayoutState.mLayoutDirection = paramInt;
    LayoutState localLayoutState = this.mLayoutState;
    boolean bool2 = this.mShouldReverseLayout;
    boolean bool1;
    if (paramInt == -1)
    {
      bool1 = true;
      if (bool2 != bool1)
        break label50;
    }
    label50: for (paramInt = i; ; paramInt = -1)
    {
      localLayoutState.mItemDirection = paramInt;
      return;
      bool1 = false;
      break;
    }
  }

  private void updateAllRemainingSpans(int paramInt1, int paramInt2)
  {
    int i = 0;
    if (i < this.mSpanCount)
    {
      if (this.mSpans[i].mViews.isEmpty());
      while (true)
      {
        i += 1;
        break;
        updateRemainingSpans(this.mSpans[i], paramInt1, paramInt2);
      }
    }
  }

  private boolean updateAnchorFromChildren(RecyclerView.State paramState, AnchorInfo paramAnchorInfo)
  {
    if (this.mLastLayoutFromEnd);
    for (int i = findLastReferenceChildPosition(paramState.getItemCount()); ; i = findFirstReferenceChildPosition(paramState.getItemCount()))
    {
      paramAnchorInfo.mPosition = i;
      paramAnchorInfo.mOffset = -2147483648;
      return true;
    }
  }

  private void updateLayoutState(int paramInt, RecyclerView.State paramState)
  {
    boolean bool1 = false;
    this.mLayoutState.mAvailable = 0;
    this.mLayoutState.mCurrentPosition = paramInt;
    int k = 0;
    int m = 0;
    int i = m;
    int j = k;
    if (isSmoothScrolling())
    {
      int n = paramState.getTargetScrollPosition();
      i = m;
      j = k;
      if (n != -1)
      {
        boolean bool2 = this.mShouldReverseLayout;
        if (n < paramInt)
          bool1 = true;
        if (bool2 != bool1)
          break label133;
        i = this.mPrimaryOrientation.getTotalSpace();
        j = k;
      }
    }
    while (getClipToPadding())
    {
      this.mLayoutState.mStartLine = (this.mPrimaryOrientation.getStartAfterPadding() - j);
      this.mLayoutState.mEndLine = (this.mPrimaryOrientation.getEndAfterPadding() + i);
      return;
      label133: j = this.mPrimaryOrientation.getTotalSpace();
      i = m;
    }
    this.mLayoutState.mEndLine = (this.mPrimaryOrientation.getEnd() + i);
    this.mLayoutState.mStartLine = (-j);
  }

  private void updateRemainingSpans(Span paramSpan, int paramInt1, int paramInt2)
  {
    int i = paramSpan.getDeletedSize();
    if (paramInt1 == -1)
      if (paramSpan.getStartLine() + i <= paramInt2)
        this.mRemainingSpans.set(paramSpan.mIndex, false);
    do
      return;
    while (paramSpan.getEndLine() - i < paramInt2);
    this.mRemainingSpans.set(paramSpan.mIndex, false);
  }

  private int updateSpecWithExtra(int paramInt1, int paramInt2, int paramInt3)
  {
    if ((paramInt2 == 0) && (paramInt3 == 0));
    int i;
    do
    {
      return paramInt1;
      i = View.MeasureSpec.getMode(paramInt1);
    }
    while ((i != -2147483648) && (i != 1073741824));
    return View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(paramInt1) - paramInt2 - paramInt3, i);
  }

  boolean areAllEndsEqual()
  {
    int j = this.mSpans[0].getEndLine(-2147483648);
    int i = 1;
    while (i < this.mSpanCount)
    {
      if (this.mSpans[i].getEndLine(-2147483648) != j)
        return false;
      i += 1;
    }
    return true;
  }

  boolean areAllStartsEqual()
  {
    int j = this.mSpans[0].getStartLine(-2147483648);
    int i = 1;
    while (i < this.mSpanCount)
    {
      if (this.mSpans[i].getStartLine(-2147483648) != j)
        return false;
      i += 1;
    }
    return true;
  }

  public void assertNotInLayoutOrScroll(String paramString)
  {
    if (this.mPendingSavedState == null)
      super.assertNotInLayoutOrScroll(paramString);
  }

  public boolean canScrollHorizontally()
  {
    return this.mOrientation == 0;
  }

  public boolean canScrollVertically()
  {
    return this.mOrientation == 1;
  }

  public boolean checkLayoutParams(RecyclerView.LayoutParams paramLayoutParams)
  {
    return paramLayoutParams instanceof LayoutParams;
  }

  public int computeHorizontalScrollExtent(RecyclerView.State paramState)
  {
    return computeScrollExtent(paramState);
  }

  public int computeHorizontalScrollOffset(RecyclerView.State paramState)
  {
    return computeScrollOffset(paramState);
  }

  public int computeHorizontalScrollRange(RecyclerView.State paramState)
  {
    return computeScrollRange(paramState);
  }

  public int computeVerticalScrollExtent(RecyclerView.State paramState)
  {
    return computeScrollExtent(paramState);
  }

  public int computeVerticalScrollOffset(RecyclerView.State paramState)
  {
    return computeScrollOffset(paramState);
  }

  public int computeVerticalScrollRange(RecyclerView.State paramState)
  {
    return computeScrollRange(paramState);
  }

  public int[] findFirstCompletelyVisibleItemPositions(int[] paramArrayOfInt)
  {
    int[] arrayOfInt;
    if (paramArrayOfInt == null)
      arrayOfInt = new int[this.mSpanCount];
    while (true)
    {
      int i = 0;
      while (true)
        if (i < this.mSpanCount)
        {
          arrayOfInt[i] = this.mSpans[i].findFirstCompletelyVisibleItemPosition();
          i += 1;
          continue;
          arrayOfInt = paramArrayOfInt;
          if (paramArrayOfInt.length >= this.mSpanCount)
            break;
          throw new IllegalArgumentException("Provided int[]'s size must be more than or equal to span count. Expected:" + this.mSpanCount + ", array size:" + paramArrayOfInt.length);
        }
    }
    return arrayOfInt;
  }

  View findFirstVisibleItemClosestToEnd(boolean paramBoolean1, boolean paramBoolean2)
  {
    ensureOrientationHelper();
    int j = this.mPrimaryOrientation.getStartAfterPadding();
    int k = this.mPrimaryOrientation.getEndAfterPadding();
    Object localObject1 = null;
    int i = getChildCount() - 1;
    if (i >= 0)
    {
      View localView = getChildAt(i);
      int m = this.mPrimaryOrientation.getDecoratedStart(localView);
      int n = this.mPrimaryOrientation.getDecoratedEnd(localView);
      Object localObject2 = localObject1;
      if (n > j)
      {
        if (m < k)
          break label99;
        localObject2 = localObject1;
      }
      while (true)
      {
        i -= 1;
        localObject1 = localObject2;
        break;
        label99: if ((n <= k) || (!paramBoolean1))
          return localView;
        localObject2 = localObject1;
        if (!paramBoolean2)
          continue;
        localObject2 = localObject1;
        if (localObject1 != null)
          continue;
        localObject2 = localView;
      }
    }
    return (View)localObject1;
  }

  View findFirstVisibleItemClosestToStart(boolean paramBoolean1, boolean paramBoolean2)
  {
    ensureOrientationHelper();
    int j = this.mPrimaryOrientation.getStartAfterPadding();
    int k = this.mPrimaryOrientation.getEndAfterPadding();
    int m = getChildCount();
    Object localObject1 = null;
    int i = 0;
    if (i < m)
    {
      View localView = getChildAt(i);
      int n = this.mPrimaryOrientation.getDecoratedStart(localView);
      Object localObject2 = localObject1;
      if (this.mPrimaryOrientation.getDecoratedEnd(localView) > j)
      {
        if (n < k)
          break label98;
        localObject2 = localObject1;
      }
      while (true)
      {
        i += 1;
        localObject1 = localObject2;
        break;
        label98: if ((n >= j) || (!paramBoolean1))
          return localView;
        localObject2 = localObject1;
        if (!paramBoolean2)
          continue;
        localObject2 = localObject1;
        if (localObject1 != null)
          continue;
        localObject2 = localView;
      }
    }
    return (View)localObject1;
  }

  int findFirstVisibleItemPositionInt()
  {
    if (this.mShouldReverseLayout);
    for (View localView = findFirstVisibleItemClosestToEnd(true, true); localView == null; localView = findFirstVisibleItemClosestToStart(true, true))
      return -1;
    return getPosition(localView);
  }

  public int[] findFirstVisibleItemPositions(int[] paramArrayOfInt)
  {
    int[] arrayOfInt;
    if (paramArrayOfInt == null)
      arrayOfInt = new int[this.mSpanCount];
    while (true)
    {
      int i = 0;
      while (true)
        if (i < this.mSpanCount)
        {
          arrayOfInt[i] = this.mSpans[i].findFirstVisibleItemPosition();
          i += 1;
          continue;
          arrayOfInt = paramArrayOfInt;
          if (paramArrayOfInt.length >= this.mSpanCount)
            break;
          throw new IllegalArgumentException("Provided int[]'s size must be more than or equal to span count. Expected:" + this.mSpanCount + ", array size:" + paramArrayOfInt.length);
        }
    }
    return arrayOfInt;
  }

  public int[] findLastCompletelyVisibleItemPositions(int[] paramArrayOfInt)
  {
    int[] arrayOfInt;
    if (paramArrayOfInt == null)
      arrayOfInt = new int[this.mSpanCount];
    while (true)
    {
      int i = 0;
      while (true)
        if (i < this.mSpanCount)
        {
          arrayOfInt[i] = this.mSpans[i].findLastCompletelyVisibleItemPosition();
          i += 1;
          continue;
          arrayOfInt = paramArrayOfInt;
          if (paramArrayOfInt.length >= this.mSpanCount)
            break;
          throw new IllegalArgumentException("Provided int[]'s size must be more than or equal to span count. Expected:" + this.mSpanCount + ", array size:" + paramArrayOfInt.length);
        }
    }
    return arrayOfInt;
  }

  public int[] findLastVisibleItemPositions(int[] paramArrayOfInt)
  {
    int[] arrayOfInt;
    if (paramArrayOfInt == null)
      arrayOfInt = new int[this.mSpanCount];
    while (true)
    {
      int i = 0;
      while (true)
        if (i < this.mSpanCount)
        {
          arrayOfInt[i] = this.mSpans[i].findLastVisibleItemPosition();
          i += 1;
          continue;
          arrayOfInt = paramArrayOfInt;
          if (paramArrayOfInt.length >= this.mSpanCount)
            break;
          throw new IllegalArgumentException("Provided int[]'s size must be more than or equal to span count. Expected:" + this.mSpanCount + ", array size:" + paramArrayOfInt.length);
        }
    }
    return arrayOfInt;
  }

  public RecyclerView.LayoutParams generateDefaultLayoutParams()
  {
    return new LayoutParams(-2, -2);
  }

  public RecyclerView.LayoutParams generateLayoutParams(Context paramContext, AttributeSet paramAttributeSet)
  {
    return new LayoutParams(paramContext, paramAttributeSet);
  }

  public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams paramLayoutParams)
  {
    if ((paramLayoutParams instanceof ViewGroup.MarginLayoutParams))
      return new LayoutParams((ViewGroup.MarginLayoutParams)paramLayoutParams);
    return new LayoutParams(paramLayoutParams);
  }

  public int getColumnCountForAccessibility(RecyclerView.Recycler paramRecycler, RecyclerView.State paramState)
  {
    if (this.mOrientation == 1)
      return this.mSpanCount;
    return super.getColumnCountForAccessibility(paramRecycler, paramState);
  }

  public int getGapStrategy()
  {
    return this.mGapStrategy;
  }

  public int getOrientation()
  {
    return this.mOrientation;
  }

  public boolean getReverseLayout()
  {
    return this.mReverseLayout;
  }

  public int getRowCountForAccessibility(RecyclerView.Recycler paramRecycler, RecyclerView.State paramState)
  {
    if (this.mOrientation == 0)
      return this.mSpanCount;
    return super.getRowCountForAccessibility(paramRecycler, paramState);
  }

  public int getSpanCount()
  {
    return this.mSpanCount;
  }

  View hasGapsToFix()
  {
    int i = getChildCount() - 1;
    BitSet localBitSet = new BitSet(this.mSpanCount);
    localBitSet.set(0, this.mSpanCount, true);
    int j;
    int k;
    if ((this.mOrientation == 1) && (isLayoutRTL()))
    {
      j = 1;
      if (!this.mShouldReverseLayout)
        break label129;
      k = 0 - 1;
      label60: if (i >= k)
        break label145;
    }
    int n;
    View localView;
    LayoutParams localLayoutParams;
    label129: label145: for (int m = 1; ; m = -1)
    {
      n = i;
      if (n == k)
        break label355;
      localView = getChildAt(n);
      localLayoutParams = (LayoutParams)localView.getLayoutParams();
      if (!localBitSet.get(localLayoutParams.mSpan.mIndex))
        break label162;
      if (!checkSpanForGap(localLayoutParams.mSpan))
        break label151;
      label121: return localView;
      j = -1;
      break;
      m = 0;
      k = i + 1;
      i = m;
      break label60;
    }
    label151: localBitSet.clear(localLayoutParams.mSpan.mIndex);
    label162: if (localLayoutParams.mFullSpan);
    label169: label341: label343: label349: label353: 
    while (true)
    {
      n += m;
      break;
      if (n + m == k)
        continue;
      Object localObject = getChildAt(n + m);
      i = 0;
      int i2;
      if (this.mShouldReverseLayout)
      {
        i1 = this.mPrimaryOrientation.getDecoratedEnd(localView);
        i2 = this.mPrimaryOrientation.getDecoratedEnd((View)localObject);
        if (i1 < i2)
          break label121;
        if (i1 == i2)
          i = 1;
        label248: if (i == 0)
          break label341;
        localObject = (LayoutParams)((View)localObject).getLayoutParams();
        if (localLayoutParams.mSpan.mIndex - ((LayoutParams)localObject).mSpan.mIndex >= 0)
          break label343;
        i = 1;
        if (j >= 0)
          break label349;
      }
      for (int i1 = 1; ; i1 = 0)
      {
        if (i == i1)
          break label353;
        return localView;
        i1 = this.mPrimaryOrientation.getDecoratedStart(localView);
        i2 = this.mPrimaryOrientation.getDecoratedStart((View)localObject);
        if (i1 > i2)
          break;
        if (i1 != i2)
          break label248;
        i = 1;
        break label248;
        break label169;
        i = 0;
        break label285;
      }
    }
    label285: label355: return (View)null;
  }

  public void invalidateSpanAssignments()
  {
    this.mLazySpanLookup.clear();
    requestLayout();
  }

  boolean isLayoutRTL()
  {
    return getLayoutDirection() == 1;
  }

  public void offsetChildrenHorizontal(int paramInt)
  {
    super.offsetChildrenHorizontal(paramInt);
    int i = 0;
    while (i < this.mSpanCount)
    {
      this.mSpans[i].onOffset(paramInt);
      i += 1;
    }
  }

  public void offsetChildrenVertical(int paramInt)
  {
    super.offsetChildrenVertical(paramInt);
    int i = 0;
    while (i < this.mSpanCount)
    {
      this.mSpans[i].onOffset(paramInt);
      i += 1;
    }
  }

  public void onDetachedFromWindow(RecyclerView paramRecyclerView, RecyclerView.Recycler paramRecycler)
  {
    removeCallbacks(this.mCheckForGapsRunnable);
    int i = 0;
    while (i < this.mSpanCount)
    {
      this.mSpans[i].clear();
      i += 1;
    }
  }

  public void onInitializeAccessibilityEvent(AccessibilityEvent paramAccessibilityEvent)
  {
    super.onInitializeAccessibilityEvent(paramAccessibilityEvent);
    View localView1;
    View localView2;
    if (getChildCount() > 0)
    {
      paramAccessibilityEvent = AccessibilityEventCompat.asRecord(paramAccessibilityEvent);
      localView1 = findFirstVisibleItemClosestToStart(false, true);
      localView2 = findFirstVisibleItemClosestToEnd(false, true);
      if ((localView1 != null) && (localView2 != null));
    }
    else
    {
      return;
    }
    int i = getPosition(localView1);
    int j = getPosition(localView2);
    if (i < j)
    {
      paramAccessibilityEvent.setFromIndex(i);
      paramAccessibilityEvent.setToIndex(j);
      return;
    }
    paramAccessibilityEvent.setFromIndex(j);
    paramAccessibilityEvent.setToIndex(i);
  }

  public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler paramRecycler, RecyclerView.State paramState, View paramView, AccessibilityNodeInfoCompat paramAccessibilityNodeInfoCompat)
  {
    paramRecycler = paramView.getLayoutParams();
    if (!(paramRecycler instanceof LayoutParams))
    {
      super.onInitializeAccessibilityNodeInfoForItem(paramView, paramAccessibilityNodeInfoCompat);
      return;
    }
    paramRecycler = (LayoutParams)paramRecycler;
    if (this.mOrientation == 0)
    {
      j = paramRecycler.getSpanIndex();
      if (paramRecycler.mFullSpan);
      for (i = this.mSpanCount; ; i = 1)
      {
        paramAccessibilityNodeInfoCompat.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(j, i, -1, -1, paramRecycler.mFullSpan, false));
        return;
      }
    }
    int j = paramRecycler.getSpanIndex();
    if (paramRecycler.mFullSpan);
    for (int i = this.mSpanCount; ; i = 1)
    {
      paramAccessibilityNodeInfoCompat.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(-1, -1, j, i, paramRecycler.mFullSpan, false));
      return;
    }
  }

  public void onItemsAdded(RecyclerView paramRecyclerView, int paramInt1, int paramInt2)
  {
    handleUpdate(paramInt1, paramInt2, 0);
  }

  public void onItemsChanged(RecyclerView paramRecyclerView)
  {
    this.mLazySpanLookup.clear();
    requestLayout();
  }

  public void onItemsMoved(RecyclerView paramRecyclerView, int paramInt1, int paramInt2, int paramInt3)
  {
    handleUpdate(paramInt1, paramInt2, 3);
  }

  public void onItemsRemoved(RecyclerView paramRecyclerView, int paramInt1, int paramInt2)
  {
    handleUpdate(paramInt1, paramInt2, 1);
  }

  public void onItemsUpdated(RecyclerView paramRecyclerView, int paramInt1, int paramInt2, Object paramObject)
  {
    handleUpdate(paramInt1, paramInt2, 2);
  }

  public void onLayoutChildren(RecyclerView.Recycler paramRecycler, RecyclerView.State paramState)
  {
    int j = 1;
    ensureOrientationHelper();
    AnchorInfo localAnchorInfo = this.mAnchorInfo;
    localAnchorInfo.reset();
    if (((this.mPendingSavedState != null) || (this.mPendingScrollPosition != -1)) && (paramState.getItemCount() == 0))
    {
      removeAndRecycleAllViews(paramRecycler);
      return;
    }
    if (this.mPendingSavedState != null)
      applyPendingSavedState(localAnchorInfo);
    while (true)
    {
      updateAnchorInfoForLayout(paramState, localAnchorInfo);
      if ((this.mPendingSavedState == null) && ((localAnchorInfo.mLayoutFromEnd != this.mLastLayoutFromEnd) || (isLayoutRTL() != this.mLastLayoutRTL)))
      {
        this.mLazySpanLookup.clear();
        localAnchorInfo.mInvalidateOffsets = true;
      }
      if ((getChildCount() <= 0) || ((this.mPendingSavedState != null) && (this.mPendingSavedState.mSpanOffsetsSize >= 1)))
        break;
      if (localAnchorInfo.mInvalidateOffsets)
      {
        i = 0;
        while (i < this.mSpanCount)
        {
          this.mSpans[i].clear();
          if (localAnchorInfo.mOffset != -2147483648)
            this.mSpans[i].setLine(localAnchorInfo.mOffset);
          i += 1;
        }
        resolveShouldLayoutReverse();
        localAnchorInfo.mLayoutFromEnd = this.mShouldReverseLayout;
        continue;
      }
      i = 0;
      while (i < this.mSpanCount)
      {
        this.mSpans[i].cacheReferenceLineAndClear(this.mShouldReverseLayout, localAnchorInfo.mOffset);
        i += 1;
      }
    }
    detachAndScrapAttachedViews(paramRecycler);
    this.mLaidOutInvalidFullSpan = false;
    updateMeasureSpecs();
    updateLayoutState(localAnchorInfo.mPosition, paramState);
    if (localAnchorInfo.mLayoutFromEnd)
    {
      setLayoutStateDirection(-1);
      fill(paramRecycler, this.mLayoutState, paramState);
      setLayoutStateDirection(1);
      this.mLayoutState.mCurrentPosition = (localAnchorInfo.mPosition + this.mLayoutState.mItemDirection);
      fill(paramRecycler, this.mLayoutState, paramState);
      if (getChildCount() > 0)
      {
        if (!this.mShouldReverseLayout)
          break label504;
        fixEndGap(paramRecycler, paramState, true);
        fixStartGap(paramRecycler, paramState, false);
      }
      label352: if (!paramState.isPreLayout())
      {
        if ((this.mGapStrategy == 0) || (getChildCount() <= 0))
          break label521;
        i = j;
        if (!this.mLaidOutInvalidFullSpan)
          if (hasGapsToFix() == null)
            break label521;
      }
    }
    label521: for (int i = j; ; i = 0)
    {
      if (i != 0)
      {
        removeCallbacks(this.mCheckForGapsRunnable);
        postOnAnimation(this.mCheckForGapsRunnable);
      }
      this.mPendingScrollPosition = -1;
      this.mPendingScrollPositionOffset = -2147483648;
      this.mLastLayoutFromEnd = localAnchorInfo.mLayoutFromEnd;
      this.mLastLayoutRTL = isLayoutRTL();
      this.mPendingSavedState = null;
      return;
      setLayoutStateDirection(1);
      fill(paramRecycler, this.mLayoutState, paramState);
      setLayoutStateDirection(-1);
      this.mLayoutState.mCurrentPosition = (localAnchorInfo.mPosition + this.mLayoutState.mItemDirection);
      fill(paramRecycler, this.mLayoutState, paramState);
      break;
      label504: fixStartGap(paramRecycler, paramState, true);
      fixEndGap(paramRecycler, paramState, false);
      break label352;
    }
  }

  public void onRestoreInstanceState(Parcelable paramParcelable)
  {
    if ((paramParcelable instanceof SavedState))
    {
      this.mPendingSavedState = ((SavedState)paramParcelable);
      requestLayout();
    }
  }

  public Parcelable onSaveInstanceState()
  {
    Object localObject;
    if (this.mPendingSavedState != null)
      localObject = new SavedState(this.mPendingSavedState);
    SavedState localSavedState;
    int i;
    label124: int j;
    label158: int k;
    while (true)
    {
      return localObject;
      localSavedState = new SavedState();
      localSavedState.mReverseLayout = this.mReverseLayout;
      localSavedState.mAnchorLayoutFromEnd = this.mLastLayoutFromEnd;
      localSavedState.mLastLayoutRTL = this.mLastLayoutRTL;
      if ((this.mLazySpanLookup != null) && (this.mLazySpanLookup.mData != null))
      {
        localSavedState.mSpanLookup = this.mLazySpanLookup.mData;
        localSavedState.mSpanLookupSize = localSavedState.mSpanLookup.length;
        localSavedState.mFullSpanItems = this.mLazySpanLookup.mFullSpanItems;
        if (getChildCount() <= 0)
          break label282;
        ensureOrientationHelper();
        if (!this.mLastLayoutFromEnd)
          break label236;
        i = getLastChildPosition();
        localSavedState.mAnchorPosition = i;
        localSavedState.mVisibleAnchorPosition = findFirstVisibleItemPositionInt();
        localSavedState.mSpanOffsetsSize = this.mSpanCount;
        localSavedState.mSpanOffsets = new int[this.mSpanCount];
        j = 0;
        localObject = localSavedState;
        if (j >= this.mSpanCount)
          continue;
        if (!this.mLastLayoutFromEnd)
          break label244;
        k = this.mSpans[j].getEndLine(-2147483648);
        i = k;
        if (k == -2147483648)
          break;
        i = k - this.mPrimaryOrientation.getEndAfterPadding();
      }
    }
    while (true)
    {
      localSavedState.mSpanOffsets[j] = i;
      j += 1;
      break label158;
      localSavedState.mSpanLookupSize = 0;
      break;
      label236: i = getFirstChildPosition();
      break label124;
      label244: k = this.mSpans[j].getStartLine(-2147483648);
      i = k;
      if (k == -2147483648)
        continue;
      i = k - this.mPrimaryOrientation.getStartAfterPadding();
    }
    label282: localSavedState.mAnchorPosition = -1;
    localSavedState.mVisibleAnchorPosition = -1;
    localSavedState.mSpanOffsetsSize = 0;
    return (Parcelable)localSavedState;
  }

  public void onScrollStateChanged(int paramInt)
  {
    if (paramInt == 0)
      checkForGaps();
  }

  int scrollBy(int paramInt, RecyclerView.Recycler paramRecycler, RecyclerView.State paramState)
  {
    ensureOrientationHelper();
    int i;
    int j;
    if (paramInt > 0)
    {
      i = 1;
      j = getLastChildPosition();
      updateLayoutState(j, paramState);
      setLayoutStateDirection(i);
      this.mLayoutState.mCurrentPosition = (this.mLayoutState.mItemDirection + j);
      j = Math.abs(paramInt);
      this.mLayoutState.mAvailable = j;
      i = fill(paramRecycler, this.mLayoutState, paramState);
      if (j >= i)
        break label112;
    }
    while (true)
    {
      this.mPrimaryOrientation.offsetChildren(-paramInt);
      this.mLastLayoutFromEnd = this.mShouldReverseLayout;
      return paramInt;
      i = -1;
      j = getFirstChildPosition();
      break;
      label112: if (paramInt < 0)
      {
        paramInt = -i;
        continue;
      }
      paramInt = i;
    }
  }

  public int scrollHorizontallyBy(int paramInt, RecyclerView.Recycler paramRecycler, RecyclerView.State paramState)
  {
    return scrollBy(paramInt, paramRecycler, paramState);
  }

  public void scrollToPosition(int paramInt)
  {
    if ((this.mPendingSavedState != null) && (this.mPendingSavedState.mAnchorPosition != paramInt))
      this.mPendingSavedState.invalidateAnchorPositionInfo();
    this.mPendingScrollPosition = paramInt;
    this.mPendingScrollPositionOffset = -2147483648;
    requestLayout();
  }

  public void scrollToPositionWithOffset(int paramInt1, int paramInt2)
  {
    if (this.mPendingSavedState != null)
      this.mPendingSavedState.invalidateAnchorPositionInfo();
    this.mPendingScrollPosition = paramInt1;
    this.mPendingScrollPositionOffset = paramInt2;
    requestLayout();
  }

  public int scrollVerticallyBy(int paramInt, RecyclerView.Recycler paramRecycler, RecyclerView.State paramState)
  {
    return scrollBy(paramInt, paramRecycler, paramState);
  }

  public void setGapStrategy(int paramInt)
  {
    assertNotInLayoutOrScroll(null);
    if (paramInt == this.mGapStrategy)
      return;
    if ((paramInt != 0) && (paramInt != 2))
      throw new IllegalArgumentException("invalid gap strategy. Must be GAP_HANDLING_NONE or GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS");
    this.mGapStrategy = paramInt;
    requestLayout();
  }

  public void setOrientation(int paramInt)
  {
    if ((paramInt != 0) && (paramInt != 1))
      throw new IllegalArgumentException("invalid orientation.");
    assertNotInLayoutOrScroll(null);
    if (paramInt == this.mOrientation)
      return;
    this.mOrientation = paramInt;
    if ((this.mPrimaryOrientation != null) && (this.mSecondaryOrientation != null))
    {
      OrientationHelper localOrientationHelper = this.mPrimaryOrientation;
      this.mPrimaryOrientation = this.mSecondaryOrientation;
      this.mSecondaryOrientation = localOrientationHelper;
    }
    requestLayout();
  }

  public void setReverseLayout(boolean paramBoolean)
  {
    assertNotInLayoutOrScroll(null);
    if ((this.mPendingSavedState != null) && (this.mPendingSavedState.mReverseLayout != paramBoolean))
      this.mPendingSavedState.mReverseLayout = paramBoolean;
    this.mReverseLayout = paramBoolean;
    requestLayout();
  }

  public void setSpanCount(int paramInt)
  {
    assertNotInLayoutOrScroll(null);
    if (paramInt != this.mSpanCount)
    {
      invalidateSpanAssignments();
      this.mSpanCount = paramInt;
      this.mRemainingSpans = new BitSet(this.mSpanCount);
      this.mSpans = new Span[this.mSpanCount];
      paramInt = 0;
      while (paramInt < this.mSpanCount)
      {
        this.mSpans[paramInt] = new Span(paramInt, null);
        paramInt += 1;
      }
      requestLayout();
    }
  }

  public void smoothScrollToPosition(RecyclerView paramRecyclerView, RecyclerView.State paramState, int paramInt)
  {
    paramRecyclerView = new LinearSmoothScroller(paramRecyclerView.getContext())
    {
      public PointF computeScrollVectorForPosition(int paramInt)
      {
        paramInt = StaggeredGridLayoutManager.this.calculateScrollDirectionForPosition(paramInt);
        if (paramInt == 0)
          return null;
        if (StaggeredGridLayoutManager.this.mOrientation == 0)
          return new PointF(paramInt, 0.0F);
        return new PointF(0.0F, paramInt);
      }
    };
    paramRecyclerView.setTargetPosition(paramInt);
    startSmoothScroll(paramRecyclerView);
  }

  public boolean supportsPredictiveItemAnimations()
  {
    return this.mPendingSavedState == null;
  }

  boolean updateAnchorFromPendingData(RecyclerView.State paramState, AnchorInfo paramAnchorInfo)
  {
    boolean bool = false;
    if ((paramState.isPreLayout()) || (this.mPendingScrollPosition == -1))
      return false;
    if ((this.mPendingScrollPosition < 0) || (this.mPendingScrollPosition >= paramState.getItemCount()))
    {
      this.mPendingScrollPosition = -1;
      this.mPendingScrollPositionOffset = -2147483648;
      return false;
    }
    if ((this.mPendingSavedState == null) || (this.mPendingSavedState.mAnchorPosition == -1) || (this.mPendingSavedState.mSpanOffsetsSize < 1))
    {
      paramState = findViewByPosition(this.mPendingScrollPosition);
      if (paramState != null)
      {
        if (this.mShouldReverseLayout)
          i = getLastChildPosition();
        while (true)
        {
          paramAnchorInfo.mPosition = i;
          if (this.mPendingScrollPositionOffset == -2147483648)
            break;
          if (paramAnchorInfo.mLayoutFromEnd)
          {
            paramAnchorInfo.mOffset = (this.mPrimaryOrientation.getEndAfterPadding() - this.mPendingScrollPositionOffset - this.mPrimaryOrientation.getDecoratedEnd(paramState));
            return true;
            i = getFirstChildPosition();
            continue;
          }
          paramAnchorInfo.mOffset = (this.mPrimaryOrientation.getStartAfterPadding() + this.mPendingScrollPositionOffset - this.mPrimaryOrientation.getDecoratedStart(paramState));
          return true;
        }
        if (this.mPrimaryOrientation.getDecoratedMeasurement(paramState) > this.mPrimaryOrientation.getTotalSpace())
        {
          if (paramAnchorInfo.mLayoutFromEnd);
          for (i = this.mPrimaryOrientation.getEndAfterPadding(); ; i = this.mPrimaryOrientation.getStartAfterPadding())
          {
            paramAnchorInfo.mOffset = i;
            return true;
          }
        }
        int i = this.mPrimaryOrientation.getDecoratedStart(paramState) - this.mPrimaryOrientation.getStartAfterPadding();
        if (i < 0)
        {
          paramAnchorInfo.mOffset = (-i);
          return true;
        }
        i = this.mPrimaryOrientation.getEndAfterPadding() - this.mPrimaryOrientation.getDecoratedEnd(paramState);
        if (i < 0)
        {
          paramAnchorInfo.mOffset = i;
          return true;
        }
        paramAnchorInfo.mOffset = -2147483648;
        return true;
      }
      paramAnchorInfo.mPosition = this.mPendingScrollPosition;
      if (this.mPendingScrollPositionOffset == -2147483648)
      {
        if (calculateScrollDirectionForPosition(paramAnchorInfo.mPosition) == 1)
          bool = true;
        paramAnchorInfo.mLayoutFromEnd = bool;
        paramAnchorInfo.assignCoordinateFromPadding();
      }
      while (true)
      {
        paramAnchorInfo.mInvalidateOffsets = true;
        return true;
        paramAnchorInfo.assignCoordinateFromPadding(this.mPendingScrollPositionOffset);
      }
    }
    paramAnchorInfo.mOffset = -2147483648;
    paramAnchorInfo.mPosition = this.mPendingScrollPosition;
    return true;
  }

  void updateAnchorInfoForLayout(RecyclerView.State paramState, AnchorInfo paramAnchorInfo)
  {
    if (updateAnchorFromPendingData(paramState, paramAnchorInfo));
    do
      return;
    while (updateAnchorFromChildren(paramState, paramAnchorInfo));
    paramAnchorInfo.assignCoordinateFromPadding();
    paramAnchorInfo.mPosition = 0;
  }

  void updateMeasureSpecs()
  {
    this.mSizePerSpan = (this.mSecondaryOrientation.getTotalSpace() / this.mSpanCount);
    this.mFullSizeSpec = View.MeasureSpec.makeMeasureSpec(this.mSecondaryOrientation.getTotalSpace(), 1073741824);
    if (this.mOrientation == 1)
    {
      this.mWidthSpec = View.MeasureSpec.makeMeasureSpec(this.mSizePerSpan, 1073741824);
      this.mHeightSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
      return;
    }
    this.mHeightSpec = View.MeasureSpec.makeMeasureSpec(this.mSizePerSpan, 1073741824);
    this.mWidthSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
  }

  private class AnchorInfo
  {
    boolean mInvalidateOffsets;
    boolean mLayoutFromEnd;
    int mOffset;
    int mPosition;

    private AnchorInfo()
    {
    }

    void assignCoordinateFromPadding()
    {
      if (this.mLayoutFromEnd);
      for (int i = StaggeredGridLayoutManager.this.mPrimaryOrientation.getEndAfterPadding(); ; i = StaggeredGridLayoutManager.this.mPrimaryOrientation.getStartAfterPadding())
      {
        this.mOffset = i;
        return;
      }
    }

    void assignCoordinateFromPadding(int paramInt)
    {
      if (this.mLayoutFromEnd)
      {
        this.mOffset = (StaggeredGridLayoutManager.this.mPrimaryOrientation.getEndAfterPadding() - paramInt);
        return;
      }
      this.mOffset = (StaggeredGridLayoutManager.this.mPrimaryOrientation.getStartAfterPadding() + paramInt);
    }

    void reset()
    {
      this.mPosition = -1;
      this.mOffset = -2147483648;
      this.mLayoutFromEnd = false;
      this.mInvalidateOffsets = false;
    }
  }

  public static class LayoutParams extends RecyclerView.LayoutParams
  {
    public static final int INVALID_SPAN_ID = -1;
    boolean mFullSpan;
    StaggeredGridLayoutManager.Span mSpan;

    public LayoutParams(int paramInt1, int paramInt2)
    {
      super(paramInt2);
    }

    public LayoutParams(Context paramContext, AttributeSet paramAttributeSet)
    {
      super(paramAttributeSet);
    }

    public LayoutParams(RecyclerView.LayoutParams paramLayoutParams)
    {
      super();
    }

    public LayoutParams(ViewGroup.LayoutParams paramLayoutParams)
    {
      super();
    }

    public LayoutParams(ViewGroup.MarginLayoutParams paramMarginLayoutParams)
    {
      super();
    }

    public final int getSpanIndex()
    {
      if (this.mSpan == null)
        return -1;
      return this.mSpan.mIndex;
    }

    public boolean isFullSpan()
    {
      return this.mFullSpan;
    }

    public void setFullSpan(boolean paramBoolean)
    {
      this.mFullSpan = paramBoolean;
    }
  }

  static class LazySpanLookup
  {
    private static final int MIN_SIZE = 10;
    int[] mData;
    List<FullSpanItem> mFullSpanItems;

    private int invalidateFullSpansAfter(int paramInt)
    {
      if (this.mFullSpanItems == null)
        return -1;
      FullSpanItem localFullSpanItem = getFullSpanItem(paramInt);
      if (localFullSpanItem != null)
        this.mFullSpanItems.remove(localFullSpanItem);
      int k = -1;
      int m = this.mFullSpanItems.size();
      int i = 0;
      while (true)
      {
        int j = k;
        if (i < m)
        {
          if (((FullSpanItem)this.mFullSpanItems.get(i)).mPosition >= paramInt)
            j = i;
        }
        else
        {
          if (j == -1)
            break;
          localFullSpanItem = (FullSpanItem)this.mFullSpanItems.get(j);
          this.mFullSpanItems.remove(j);
          return localFullSpanItem.mPosition;
        }
        i += 1;
      }
    }

    private void offsetFullSpansForAddition(int paramInt1, int paramInt2)
    {
      if (this.mFullSpanItems == null)
        return;
      int i = this.mFullSpanItems.size() - 1;
      label21: FullSpanItem localFullSpanItem;
      if (i >= 0)
      {
        localFullSpanItem = (FullSpanItem)this.mFullSpanItems.get(i);
        if (localFullSpanItem.mPosition >= paramInt1)
          break label58;
      }
      while (true)
      {
        i -= 1;
        break label21;
        break;
        label58: localFullSpanItem.mPosition += paramInt2;
      }
    }

    private void offsetFullSpansForRemoval(int paramInt1, int paramInt2)
    {
      if (this.mFullSpanItems == null)
        return;
      int i = this.mFullSpanItems.size() - 1;
      label21: FullSpanItem localFullSpanItem;
      if (i >= 0)
      {
        localFullSpanItem = (FullSpanItem)this.mFullSpanItems.get(i);
        if (localFullSpanItem.mPosition >= paramInt1)
          break label58;
      }
      while (true)
      {
        i -= 1;
        break label21;
        break;
        label58: if (localFullSpanItem.mPosition < paramInt1 + paramInt2)
        {
          this.mFullSpanItems.remove(i);
          continue;
        }
        localFullSpanItem.mPosition -= paramInt2;
      }
    }

    public void addFullSpanItem(FullSpanItem paramFullSpanItem)
    {
      if (this.mFullSpanItems == null)
        this.mFullSpanItems = new ArrayList();
      int j = this.mFullSpanItems.size();
      int i = 0;
      while (i < j)
      {
        FullSpanItem localFullSpanItem = (FullSpanItem)this.mFullSpanItems.get(i);
        if (localFullSpanItem.mPosition == paramFullSpanItem.mPosition)
          this.mFullSpanItems.remove(i);
        if (localFullSpanItem.mPosition >= paramFullSpanItem.mPosition)
        {
          this.mFullSpanItems.add(i, paramFullSpanItem);
          return;
        }
        i += 1;
      }
      this.mFullSpanItems.add(paramFullSpanItem);
    }

    void clear()
    {
      if (this.mData != null)
        Arrays.fill(this.mData, -1);
      this.mFullSpanItems = null;
    }

    void ensureSize(int paramInt)
    {
      if (this.mData == null)
      {
        this.mData = new int[Math.max(paramInt, 10) + 1];
        Arrays.fill(this.mData, -1);
      }
      do
        return;
      while (paramInt < this.mData.length);
      int[] arrayOfInt = this.mData;
      this.mData = new int[sizeForPosition(paramInt)];
      System.arraycopy(arrayOfInt, 0, this.mData, 0, arrayOfInt.length);
      Arrays.fill(this.mData, arrayOfInt.length, this.mData.length, -1);
    }

    int forceInvalidateAfter(int paramInt)
    {
      if (this.mFullSpanItems != null)
      {
        int i = this.mFullSpanItems.size() - 1;
        while (i >= 0)
        {
          if (((FullSpanItem)this.mFullSpanItems.get(i)).mPosition >= paramInt)
            this.mFullSpanItems.remove(i);
          i -= 1;
        }
      }
      return invalidateAfter(paramInt);
    }

    public FullSpanItem getFirstFullSpanItemInRange(int paramInt1, int paramInt2, int paramInt3, boolean paramBoolean)
    {
      Object localObject;
      if (this.mFullSpanItems == null)
      {
        localObject = null;
        return localObject;
      }
      int j = this.mFullSpanItems.size();
      int i = 0;
      while (true)
      {
        if (i >= j)
          break label117;
        FullSpanItem localFullSpanItem = (FullSpanItem)this.mFullSpanItems.get(i);
        if (localFullSpanItem.mPosition >= paramInt2)
          return null;
        if (localFullSpanItem.mPosition >= paramInt1)
        {
          localObject = localFullSpanItem;
          if (paramInt3 == 0)
            break;
          localObject = localFullSpanItem;
          if (localFullSpanItem.mGapDir == paramInt3)
            break;
          if (paramBoolean)
          {
            localObject = localFullSpanItem;
            if (localFullSpanItem.mHasUnwantedGapAfter)
              break;
          }
        }
        i += 1;
      }
      label117: return null;
    }

    public FullSpanItem getFullSpanItem(int paramInt)
    {
      Object localObject;
      if (this.mFullSpanItems == null)
      {
        localObject = null;
        return localObject;
      }
      int i = this.mFullSpanItems.size() - 1;
      while (true)
      {
        if (i < 0)
          break label63;
        FullSpanItem localFullSpanItem = (FullSpanItem)this.mFullSpanItems.get(i);
        localObject = localFullSpanItem;
        if (localFullSpanItem.mPosition == paramInt)
          break;
        i -= 1;
      }
      label63: return null;
    }

    int getSpan(int paramInt)
    {
      if ((this.mData == null) || (paramInt >= this.mData.length))
        return -1;
      return this.mData[paramInt];
    }

    int invalidateAfter(int paramInt)
    {
      if (this.mData == null);
      do
        return -1;
      while (paramInt >= this.mData.length);
      int i = invalidateFullSpansAfter(paramInt);
      if (i == -1)
      {
        Arrays.fill(this.mData, paramInt, this.mData.length, -1);
        return this.mData.length;
      }
      Arrays.fill(this.mData, paramInt, i + 1, -1);
      return i + 1;
    }

    void offsetForAddition(int paramInt1, int paramInt2)
    {
      if ((this.mData == null) || (paramInt1 >= this.mData.length))
        return;
      ensureSize(paramInt1 + paramInt2);
      System.arraycopy(this.mData, paramInt1, this.mData, paramInt1 + paramInt2, this.mData.length - paramInt1 - paramInt2);
      Arrays.fill(this.mData, paramInt1, paramInt1 + paramInt2, -1);
      offsetFullSpansForAddition(paramInt1, paramInt2);
    }

    void offsetForRemoval(int paramInt1, int paramInt2)
    {
      if ((this.mData == null) || (paramInt1 >= this.mData.length))
        return;
      ensureSize(paramInt1 + paramInt2);
      System.arraycopy(this.mData, paramInt1 + paramInt2, this.mData, paramInt1, this.mData.length - paramInt1 - paramInt2);
      Arrays.fill(this.mData, this.mData.length - paramInt2, this.mData.length, -1);
      offsetFullSpansForRemoval(paramInt1, paramInt2);
    }

    void setSpan(int paramInt, StaggeredGridLayoutManager.Span paramSpan)
    {
      ensureSize(paramInt);
      this.mData[paramInt] = paramSpan.mIndex;
    }

    int sizeForPosition(int paramInt)
    {
      int i = this.mData.length;
      while (i <= paramInt)
        i *= 2;
      return i;
    }

    static class FullSpanItem
      implements Parcelable
    {
      public static final Parcelable.Creator<FullSpanItem> CREATOR = new Parcelable.Creator()
      {
        public StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem createFromParcel(Parcel paramParcel)
        {
          return new StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem(paramParcel);
        }

        public StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem[] newArray(int paramInt)
        {
          return new StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem[paramInt];
        }
      };
      int mGapDir;
      int[] mGapPerSpan;
      boolean mHasUnwantedGapAfter;
      int mPosition;

      public FullSpanItem()
      {
      }

      public FullSpanItem(Parcel paramParcel)
      {
        this.mPosition = paramParcel.readInt();
        this.mGapDir = paramParcel.readInt();
        if (paramParcel.readInt() == 1);
        while (true)
        {
          this.mHasUnwantedGapAfter = bool;
          int i = paramParcel.readInt();
          if (i > 0)
          {
            this.mGapPerSpan = new int[i];
            paramParcel.readIntArray(this.mGapPerSpan);
          }
          return;
          bool = false;
        }
      }

      public int describeContents()
      {
        return 0;
      }

      int getGapForSpan(int paramInt)
      {
        if (this.mGapPerSpan == null)
          return 0;
        return this.mGapPerSpan[paramInt];
      }

      public void invalidateSpanGaps()
      {
        this.mGapPerSpan = null;
      }

      public String toString()
      {
        return "FullSpanItem{mPosition=" + this.mPosition + ", mGapDir=" + this.mGapDir + ", mHasUnwantedGapAfter=" + this.mHasUnwantedGapAfter + ", mGapPerSpan=" + Arrays.toString(this.mGapPerSpan) + '}';
      }

      public void writeToParcel(Parcel paramParcel, int paramInt)
      {
        paramParcel.writeInt(this.mPosition);
        paramParcel.writeInt(this.mGapDir);
        if (this.mHasUnwantedGapAfter);
        for (paramInt = 1; ; paramInt = 0)
        {
          paramParcel.writeInt(paramInt);
          if ((this.mGapPerSpan == null) || (this.mGapPerSpan.length <= 0))
            break;
          paramParcel.writeInt(this.mGapPerSpan.length);
          paramParcel.writeIntArray(this.mGapPerSpan);
          return;
        }
        paramParcel.writeInt(0);
      }
    }
  }

  static class SavedState
    implements Parcelable
  {
    public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator()
    {
      public StaggeredGridLayoutManager.SavedState createFromParcel(Parcel paramParcel)
      {
        return new StaggeredGridLayoutManager.SavedState(paramParcel);
      }

      public StaggeredGridLayoutManager.SavedState[] newArray(int paramInt)
      {
        return new StaggeredGridLayoutManager.SavedState[paramInt];
      }
    };
    boolean mAnchorLayoutFromEnd;
    int mAnchorPosition;
    List<StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem> mFullSpanItems;
    boolean mLastLayoutRTL;
    boolean mReverseLayout;
    int[] mSpanLookup;
    int mSpanLookupSize;
    int[] mSpanOffsets;
    int mSpanOffsetsSize;
    int mVisibleAnchorPosition;

    public SavedState()
    {
    }

    SavedState(Parcel paramParcel)
    {
      this.mAnchorPosition = paramParcel.readInt();
      this.mVisibleAnchorPosition = paramParcel.readInt();
      this.mSpanOffsetsSize = paramParcel.readInt();
      if (this.mSpanOffsetsSize > 0)
      {
        this.mSpanOffsets = new int[this.mSpanOffsetsSize];
        paramParcel.readIntArray(this.mSpanOffsets);
      }
      this.mSpanLookupSize = paramParcel.readInt();
      if (this.mSpanLookupSize > 0)
      {
        this.mSpanLookup = new int[this.mSpanLookupSize];
        paramParcel.readIntArray(this.mSpanLookup);
      }
      if (paramParcel.readInt() == 1)
      {
        bool1 = true;
        this.mReverseLayout = bool1;
        if (paramParcel.readInt() != 1)
          break label152;
        bool1 = true;
        label113: this.mAnchorLayoutFromEnd = bool1;
        if (paramParcel.readInt() != 1)
          break label157;
      }
      label152: label157: for (boolean bool1 = bool2; ; bool1 = false)
      {
        this.mLastLayoutRTL = bool1;
        this.mFullSpanItems = paramParcel.readArrayList(StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem.class.getClassLoader());
        return;
        bool1 = false;
        break;
        bool1 = false;
        break label113;
      }
    }

    public SavedState(SavedState paramSavedState)
    {
      this.mSpanOffsetsSize = paramSavedState.mSpanOffsetsSize;
      this.mAnchorPosition = paramSavedState.mAnchorPosition;
      this.mVisibleAnchorPosition = paramSavedState.mVisibleAnchorPosition;
      this.mSpanOffsets = paramSavedState.mSpanOffsets;
      this.mSpanLookupSize = paramSavedState.mSpanLookupSize;
      this.mSpanLookup = paramSavedState.mSpanLookup;
      this.mReverseLayout = paramSavedState.mReverseLayout;
      this.mAnchorLayoutFromEnd = paramSavedState.mAnchorLayoutFromEnd;
      this.mLastLayoutRTL = paramSavedState.mLastLayoutRTL;
      this.mFullSpanItems = paramSavedState.mFullSpanItems;
    }

    public int describeContents()
    {
      return 0;
    }

    void invalidateAnchorPositionInfo()
    {
      this.mSpanOffsets = null;
      this.mSpanOffsetsSize = 0;
      this.mAnchorPosition = -1;
      this.mVisibleAnchorPosition = -1;
    }

    void invalidateSpanInfo()
    {
      this.mSpanOffsets = null;
      this.mSpanOffsetsSize = 0;
      this.mSpanLookupSize = 0;
      this.mSpanLookup = null;
      this.mFullSpanItems = null;
    }

    public void writeToParcel(Parcel paramParcel, int paramInt)
    {
      int i = 1;
      paramParcel.writeInt(this.mAnchorPosition);
      paramParcel.writeInt(this.mVisibleAnchorPosition);
      paramParcel.writeInt(this.mSpanOffsetsSize);
      if (this.mSpanOffsetsSize > 0)
        paramParcel.writeIntArray(this.mSpanOffsets);
      paramParcel.writeInt(this.mSpanLookupSize);
      if (this.mSpanLookupSize > 0)
        paramParcel.writeIntArray(this.mSpanLookup);
      if (this.mReverseLayout)
      {
        paramInt = 1;
        paramParcel.writeInt(paramInt);
        if (!this.mAnchorLayoutFromEnd)
          break label120;
        paramInt = 1;
        label87: paramParcel.writeInt(paramInt);
        if (!this.mLastLayoutRTL)
          break label125;
      }
      label120: label125: for (paramInt = i; ; paramInt = 0)
      {
        paramParcel.writeInt(paramInt);
        paramParcel.writeList(this.mFullSpanItems);
        return;
        paramInt = 0;
        break;
        paramInt = 0;
        break label87;
      }
    }
  }

  class Span
  {
    static final int INVALID_LINE = -2147483648;
    int mCachedEnd = -2147483648;
    int mCachedStart = -2147483648;
    int mDeletedSize = 0;
    final int mIndex;
    private ArrayList<View> mViews = new ArrayList();

    private Span(int arg2)
    {
      int i;
      this.mIndex = i;
    }

    void appendToSpan(View paramView)
    {
      StaggeredGridLayoutManager.LayoutParams localLayoutParams = getLayoutParams(paramView);
      localLayoutParams.mSpan = this;
      this.mViews.add(paramView);
      this.mCachedEnd = -2147483648;
      if (this.mViews.size() == 1)
        this.mCachedStart = -2147483648;
      if ((localLayoutParams.isItemRemoved()) || (localLayoutParams.isItemChanged()))
        this.mDeletedSize += StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedMeasurement(paramView);
    }

    void cacheReferenceLineAndClear(boolean paramBoolean, int paramInt)
    {
      int i;
      if (paramBoolean)
      {
        i = getEndLine(-2147483648);
        clear();
        if (i != -2147483648)
          break label32;
      }
      label32: 
      do
      {
        return;
        i = getStartLine(-2147483648);
        break;
      }
      while (((paramBoolean) && (i < StaggeredGridLayoutManager.this.mPrimaryOrientation.getEndAfterPadding())) || ((!paramBoolean) && (i > StaggeredGridLayoutManager.this.mPrimaryOrientation.getStartAfterPadding())));
      int j = i;
      if (paramInt != -2147483648)
        j = i + paramInt;
      this.mCachedEnd = j;
      this.mCachedStart = j;
    }

    void calculateCachedEnd()
    {
      Object localObject = (View)this.mViews.get(this.mViews.size() - 1);
      StaggeredGridLayoutManager.LayoutParams localLayoutParams = getLayoutParams((View)localObject);
      this.mCachedEnd = StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedEnd((View)localObject);
      if (localLayoutParams.mFullSpan)
      {
        localObject = StaggeredGridLayoutManager.this.mLazySpanLookup.getFullSpanItem(localLayoutParams.getViewLayoutPosition());
        if ((localObject != null) && (((StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem)localObject).mGapDir == 1))
          this.mCachedEnd += ((StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem)localObject).getGapForSpan(this.mIndex);
      }
    }

    void calculateCachedStart()
    {
      Object localObject = (View)this.mViews.get(0);
      StaggeredGridLayoutManager.LayoutParams localLayoutParams = getLayoutParams((View)localObject);
      this.mCachedStart = StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedStart((View)localObject);
      if (localLayoutParams.mFullSpan)
      {
        localObject = StaggeredGridLayoutManager.this.mLazySpanLookup.getFullSpanItem(localLayoutParams.getViewLayoutPosition());
        if ((localObject != null) && (((StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem)localObject).mGapDir == -1))
          this.mCachedStart -= ((StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem)localObject).getGapForSpan(this.mIndex);
      }
    }

    void clear()
    {
      this.mViews.clear();
      invalidateCache();
      this.mDeletedSize = 0;
    }

    public int findFirstCompletelyVisibleItemPosition()
    {
      if (StaggeredGridLayoutManager.this.mReverseLayout)
        return findOneVisibleChild(this.mViews.size() - 1, -1, true);
      return findOneVisibleChild(0, this.mViews.size(), true);
    }

    public int findFirstVisibleItemPosition()
    {
      if (StaggeredGridLayoutManager.this.mReverseLayout)
        return findOneVisibleChild(this.mViews.size() - 1, -1, false);
      return findOneVisibleChild(0, this.mViews.size(), false);
    }

    public int findLastCompletelyVisibleItemPosition()
    {
      if (StaggeredGridLayoutManager.this.mReverseLayout)
        return findOneVisibleChild(0, this.mViews.size(), true);
      return findOneVisibleChild(this.mViews.size() - 1, -1, true);
    }

    public int findLastVisibleItemPosition()
    {
      if (StaggeredGridLayoutManager.this.mReverseLayout)
        return findOneVisibleChild(0, this.mViews.size(), false);
      return findOneVisibleChild(this.mViews.size() - 1, -1, false);
    }

    int findOneVisibleChild(int paramInt1, int paramInt2, boolean paramBoolean)
    {
      int k = -1;
      int m = StaggeredGridLayoutManager.this.mPrimaryOrientation.getStartAfterPadding();
      int n = StaggeredGridLayoutManager.this.mPrimaryOrientation.getEndAfterPadding();
      int i;
      if (paramInt2 > paramInt1)
        i = 1;
      while (true)
      {
        int j = k;
        View localView;
        if (paramInt1 != paramInt2)
        {
          localView = (View)this.mViews.get(paramInt1);
          j = StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedStart(localView);
          int i1 = StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedEnd(localView);
          if ((j >= n) || (i1 <= m))
            break label147;
          if (paramBoolean)
          {
            if ((j < m) || (i1 > n))
              break label147;
            j = StaggeredGridLayoutManager.this.getPosition(localView);
          }
        }
        else
        {
          return j;
          i = -1;
          continue;
        }
        return StaggeredGridLayoutManager.this.getPosition(localView);
        label147: paramInt1 += i;
      }
    }

    public int getDeletedSize()
    {
      return this.mDeletedSize;
    }

    int getEndLine()
    {
      if (this.mCachedEnd != -2147483648)
        return this.mCachedEnd;
      calculateCachedEnd();
      return this.mCachedEnd;
    }

    int getEndLine(int paramInt)
    {
      if (this.mCachedEnd != -2147483648)
        paramInt = this.mCachedEnd;
      do
        return paramInt;
      while (this.mViews.size() == 0);
      calculateCachedEnd();
      return this.mCachedEnd;
    }

    StaggeredGridLayoutManager.LayoutParams getLayoutParams(View paramView)
    {
      return (StaggeredGridLayoutManager.LayoutParams)paramView.getLayoutParams();
    }

    int getNormalizedOffset(int paramInt1, int paramInt2, int paramInt3)
    {
      if (this.mViews.size() == 0)
        paramInt2 = 0;
      while (true)
      {
        return paramInt2;
        if (paramInt1 >= 0)
          break;
        paramInt3 = getEndLine() - paramInt3;
        if (paramInt3 <= 0)
          return 0;
        paramInt2 = paramInt1;
        if (-paramInt1 > paramInt3)
          return -paramInt3;
      }
      paramInt2 -= getStartLine();
      if (paramInt2 <= 0)
        return 0;
      if (paramInt2 < paramInt1)
        paramInt1 = paramInt2;
      while (true)
        return paramInt1;
    }

    int getStartLine()
    {
      if (this.mCachedStart != -2147483648)
        return this.mCachedStart;
      calculateCachedStart();
      return this.mCachedStart;
    }

    int getStartLine(int paramInt)
    {
      if (this.mCachedStart != -2147483648)
        paramInt = this.mCachedStart;
      do
        return paramInt;
      while (this.mViews.size() == 0);
      calculateCachedStart();
      return this.mCachedStart;
    }

    void invalidateCache()
    {
      this.mCachedStart = -2147483648;
      this.mCachedEnd = -2147483648;
    }

    boolean isEmpty(int paramInt1, int paramInt2)
    {
      int j = this.mViews.size();
      int i = 0;
      while (i < j)
      {
        View localView = (View)this.mViews.get(i);
        if ((StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedStart(localView) < paramInt2) && (StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedEnd(localView) > paramInt1))
          return false;
        i += 1;
      }
      return true;
    }

    void onOffset(int paramInt)
    {
      if (this.mCachedStart != -2147483648)
        this.mCachedStart += paramInt;
      if (this.mCachedEnd != -2147483648)
        this.mCachedEnd += paramInt;
    }

    void popEnd()
    {
      int i = this.mViews.size();
      View localView = (View)this.mViews.remove(i - 1);
      StaggeredGridLayoutManager.LayoutParams localLayoutParams = getLayoutParams(localView);
      localLayoutParams.mSpan = null;
      if ((localLayoutParams.isItemRemoved()) || (localLayoutParams.isItemChanged()))
        this.mDeletedSize -= StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedMeasurement(localView);
      if (i == 1)
        this.mCachedStart = -2147483648;
      this.mCachedEnd = -2147483648;
    }

    void popStart()
    {
      View localView = (View)this.mViews.remove(0);
      StaggeredGridLayoutManager.LayoutParams localLayoutParams = getLayoutParams(localView);
      localLayoutParams.mSpan = null;
      if (this.mViews.size() == 0)
        this.mCachedEnd = -2147483648;
      if ((localLayoutParams.isItemRemoved()) || (localLayoutParams.isItemChanged()))
        this.mDeletedSize -= StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedMeasurement(localView);
      this.mCachedStart = -2147483648;
    }

    void prependToSpan(View paramView)
    {
      StaggeredGridLayoutManager.LayoutParams localLayoutParams = getLayoutParams(paramView);
      localLayoutParams.mSpan = this;
      this.mViews.add(0, paramView);
      this.mCachedStart = -2147483648;
      if (this.mViews.size() == 1)
        this.mCachedEnd = -2147483648;
      if ((localLayoutParams.isItemRemoved()) || (localLayoutParams.isItemChanged()))
        this.mDeletedSize += StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedMeasurement(paramView);
    }

    void setLine(int paramInt)
    {
      this.mCachedStart = paramInt;
      this.mCachedEnd = paramInt;
    }
  }
}

/* Location:           C:\Users\xuetong\Desktop\dazhongdianping7.9.6\ProjectSrc\classes-dex2jar.jar
 * Qualified Name:     android.support.v7.widget.StaggeredGridLayoutManager
 * JD-Core Version:    0.6.0
 */