package com.heaven7.android.StickyLayout;


import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.OverScroller;

/**
 * sticky navigation layout：similar to google+ app.
 */
public class StickyNavigationLayout extends LinearLayout {

    private static final String TAG = "StickyNavLayout";
    /**
     * the top view
     */
    private View mTop;
    /**
     * the navigation view
     */
    private View mNav;
    /**
     * the child view which will be intercept
     */
    private View mWillInterceptChildView;

    private int mTopViewHeight;
    private boolean mTopHide = false;

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMaximumVelocity, mMinimumVelocity;

    private int mLastY;
    private boolean mDragging;

    private final int mTopViewId;
    private final int mIndicatorId;
    private final int mContentId;

    private IStickyDelegate mStickyDelegate;
    private boolean mNeedIntercept;

    public StickyNavigationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);

        mScroller = new OverScroller(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();//触摸阙值
        mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StickyNavigationLayout);
        mTopViewId = a.getResourceId(a.getIndex(R.styleable.StickyNavigationLayout_stickyLayout_top_id), 0);
        mIndicatorId = a.getResourceId(a.getIndex(R.styleable.StickyNavigationLayout_stickyLayout_indicator_id), 0);
        mContentId = a.getResourceId(a.getIndex(R.styleable.StickyNavigationLayout_stickyLayout_content_id), 0);
        a.recycle();

        //getWindowVisibleDisplayFrame(mExpectTopRect);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTop = findViewById(mTopViewId);
        mNav = findViewById(mIndicatorId);
        mWillInterceptChildView = findViewById(mContentId);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 设置view的高度 (将mViewPager。的高度设置为  整个 Height - 导航的高度) - 被拦截的child view
        ViewGroup.LayoutParams params = mWillInterceptChildView.getLayoutParams();
        params.height = getMeasuredHeight() - mNav.getMeasuredHeight();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mTopViewHeight = mTop.getMeasuredHeight();
    }

    /**
     * set the sticky delegate
     * @param delegate the delegate
     */
    public void setStickyDelegate(IStickyDelegate delegate) {
        this.mStickyDelegate = delegate;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /**
         * 1, 上拉的时候，停靠后分发给child滑动. max = mTopViewHeight
         * 2, 下拉时，先拉上面的，拉完后分发给child.
         */
        int action = ev.getAction();
        int y = (int) (ev.getY() + 0.5f);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                int dy = y - mLastY;

                if (Math.abs(dy) > mTouchSlop) {
                    if(mNeedIntercept){
                        return true;
                    }
                    if (dy > 0) {
                        return getScrollY() == mTopViewHeight;
                    }
                    if (mStickyDelegate != null && mStickyDelegate.shouldIntercept(this, dy,
                            mTopHide ? VIEW_STATE_HIDE : VIEW_STATE_SHOW)) {
                        mDragging = true;
                        return true;
                    }
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        int action = event.getAction();
        int y = (int) (event.getY() + 0.5f);

        switch (action) {

            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();
                mVelocityTracker.addMovement(event);
                mLastY = y;
                return true;

            case MotionEvent.ACTION_MOVE:
                int dy = y - mLastY;

                if (!mDragging && Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                }
                if (mDragging) {
                    //手向下滑动， dy >0 否则 <0.
                    final int scrollY = getScrollY();
                    //Logger.i(TAG, "onTouchEvent", "ScrollY = " + scrollY + " ,dy = " + dy + " , mTopViewHeight = " + mTopViewHeight);
                    mLastY = y;

                    if (dy < 0) {
                        //手势向上滑动 ,view down
                        /**
                         *  called [ onTouchEvent() ]: ScrollY = 666 ,dy = -7.4692383 , mTopViewHeight = 788
                         *  called [ onTouchEvent() ]: ScrollY = 673 ,dy = -3.748291 , mTopViewHeight = 788
                         */
                        if (scrollY == mTopViewHeight) {
                            //分发给child
                            if(mStickyDelegate != null) {
                                mStickyDelegate.scrollBy(dy);
                                // return mStickyDelegate.dispatchTouchEventToChild(event);
                            }
                        } else if (scrollY - dy > mTopViewHeight) {
                            //top height is the max scroll height
                            scrollTo(getScrollX(), mTopViewHeight);
                        } else {
                            scrollBy(0, -dy);
                        }
                    } else {
                        //手势向下
                        if (scrollY == 0) {
                            //分发事件给child
                            if(mStickyDelegate != null) {
                                mStickyDelegate.scrollBy(dy);
                                // return mStickyDelegate.dispatchTouchEventToChild(event);
                            }
                        } else {
                            if (scrollY - dy < 0) {
                                dy = scrollY;
                            }
                            scrollBy(0, -dy);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                if (!mScroller.isFinished()) {//如果手离开了,就终止滑动
                    mScroller.abortAnimation();
                }
                break;

            case MotionEvent.ACTION_UP:
                mDragging = false;
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity); //1000表示像素/秒
                int velocityY = (int) mVelocityTracker.getYVelocity();
                if (Math.abs(velocityY) > mMinimumVelocity) {
                    fling(-velocityY);
                }
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    public void fling(int velocityY) {
        //使得当前对象只滑动到mTopViewHeight
        mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, 0, mTopViewHeight);
        invalidate();
    }

    @Override //限定滑动的y范围
    public void scrollTo(int x, int y) {
       // Logger.i(TAG, "scrollTo", "x = " + x + ", y = " + y);
        if (y < 0) {
            y = 0;
        }
        //maxY =  mTopViewHeight
        if (y > mTopViewHeight) {
            y = mTopViewHeight;
        }
        if (y == 0 || y == mTopViewHeight) {
            mNeedIntercept = false;
        }else{
            mNeedIntercept = true;
        }
        super.scrollTo(x, y);
        mTopHide = getScrollY() == mTopViewHeight;
    }

    @Override
    public void computeScroll() {
        //super.computeScroll();
        if (mScroller.computeScrollOffset()) {//true滑动尚未完成
            scrollTo(0, mScroller.getCurrY());
            invalidate();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return new SaveState(super.onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if(state != null) {
            SaveState ss = (SaveState) state;
            this.mNeedIntercept = ss.mNeedIntercept;
            this.mTopHide = ss.mTopHide;
        }
    }

    private static class SaveState extends BaseSavedState{

        boolean mNeedIntercept;
        boolean mTopHide;

        public SaveState(Parcel source) {
            super(source);
            mNeedIntercept = source.readByte() == 1;
            mTopHide = source.readByte() == 1;
        }
        public SaveState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (mNeedIntercept ? 1 : 0));
            out.writeByte((byte) (mTopHide ? 1 : 0));
        }

        public static final Parcelable.Creator<SaveState> CREATOR
                = new Parcelable.Creator<SaveState>() {
            @Override
            public SaveState createFromParcel(Parcel in) {
                return new SaveState(in);
            }
            @Override
            public SaveState[] newArray(int size) {
                return new SaveState[size];
            }
        };
    }

    /**
     * the sticky delegate
     */
    public interface IStickyDelegate {
        /**
         * called when you should intercept child's touch event.
         *
         * @param snv          the {@link StickyNavigationLayout}
         * @param dy           the delta y distance
         * @param topViewState the view state of top view. {@link #VIEW_STATE_SHOW} or {@link #VIEW_STATE_HIDE}
         * @return true to intercept
         */
        boolean shouldIntercept(StickyNavigationLayout snv, int dy, int topViewState);

        /**
         * scroll by the target delta y distance.
         * @param dy the delta y
         */
        void scrollBy(int dy);
    }

    /**
     * the view state is shown.
      */
    public static final int VIEW_STATE_SHOW = 1;
    /**
     * the view state is hide
     */
    public static final int VIEW_STATE_HIDE = 2;
}
