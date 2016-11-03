package com.heaven7.android.sticky_navigation_layout.demo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

public class RadioButtonPoint extends RadioButton {
    public static final int STATUS_UNREAD_NUMBER = 1;
    public static final int STATUS_RED_POINT = 2;
    public static final int STATUS_CLEAR = 3;
    private int mStatus = STATUS_CLEAR;
    private int mUnreadNumber;

    private Paint mCirclePaint;
    private int mCircleLeft;
    private int mRadius;
    private int mColor;

    private TextView mTextView;

    public RadioButtonPoint(Context context) {
        super(context);
        init(null, 0);
    }

    public RadioButtonPoint(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public RadioButtonPoint(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }


    private void init(AttributeSet attrs, int defStyle) {
      /*  final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.RadioButtonPoint, defStyle, 0);
        mRadius = a.getDimensionPixelSize(R.styleable.RadioButtonPoint_circle_radius, DimenUtil.dip2px(4));
        mColor = a.getColor(R.styleable.RadioButtonPoint_circle_color, 0xfffe412d);
        a.recycle();*/

        mCirclePaint = new Paint();
        mCirclePaint.setColor(mColor);
        mCirclePaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final Drawable[] compoundDrawables = getCompoundDrawables();
        int drawableWidth = 0;
        if (null != compoundDrawables) {
            drawableWidth = compoundDrawables[1].getIntrinsicWidth();
        }
        mCircleLeft = (getMeasuredWidth() + drawableWidth) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        switch (mStatus) {
            case STATUS_UNREAD_NUMBER:
                if (mUnreadNumber > 0 && mTextView != null) {
                    mTextView.setVisibility(View.VISIBLE);
                    mTextView.setText(mUnreadNumber + "");
                }
                break;

            case STATUS_RED_POINT:
                canvas.drawCircle(mCircleLeft, mRadius, mRadius, mCirclePaint);

                if (mTextView != null) {
                    mTextView.setVisibility(View.GONE);
                }
                break;

            case STATUS_CLEAR:
                if (mTextView != null) {
                    mTextView.setVisibility(View.GONE);
                }
                break;

            default:
                if (mTextView != null) {
                    mTextView.setVisibility(View.GONE);
                }
                break;

        }
    }

    public void updateStatus(int status, int unreadNumber) {
        if (unreadNumber > 99) {
            unreadNumber = 99;
        }
        mStatus = status;
        mUnreadNumber = unreadNumber;
        invalidate();
    }

    public void updateStatus(int status) {
        updateStatus(status, 0);
    }

    public void setUnreadNumberTextView(TextView textView) {
        mTextView = textView;
    }
}
