package com.canyinghao.canrecyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created by canyinghao on 15/12/17..
 * Copyright 2016 canyinghao
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class CanScaleRecyclerView extends RecyclerViewEmpty {


    private static final int INVALID_POINTER_ID = -1;


    private int mMainPointerId = INVALID_POINTER_ID;


    private float mCurrentScaleFactor;

    private float mLastTouchX;
    private float mLastTouchY;

    private float mOffsetX;
    private float mOffsetY;

    private float centerX;
    private float centerY;

    private float mMinScaleFactor = 1.0f;
    private float mMidScaleFactor = 2.5f;
    private float mMaxScaleFactor = 4f;

    private int mAutoTime = 5;
    private float mAutoBigger = 1.08f;
    private float mAutoSmall = 0.92f;

    private boolean isScale;


    private GestureDetector mGestureDetector;

    private ScaleGestureDetector mScaleGestureDetector;

    private OnGestureListener mOnGestureListener;

    public interface OnGestureListener {
        boolean onScale(ScaleGestureDetector detector);

        boolean onSingleTapConfirmed(MotionEvent e);

        boolean onDoubleTap(MotionEvent e);
    }


    /**
     * 双击缩放时平滑缩放
     */
    private class AutoScaleRunnable implements Runnable {

        private float mTargetScaleFactor ;

        private float mGrad;


        private AutoScaleRunnable(float TargetScale, float grad) {
            mTargetScaleFactor = TargetScale;
            mGrad = grad;
        }

        @Override
        public void run() {
            if ((mGrad > 1.0f && mCurrentScaleFactor < mTargetScaleFactor)
                    || (mGrad < 1.0f && mCurrentScaleFactor > mTargetScaleFactor)) {

                mCurrentScaleFactor *= mGrad;

                if (mGrad > 1.0f) {

                    if (mCurrentScaleFactor >= mTargetScaleFactor) {
                        mCurrentScaleFactor = mTargetScaleFactor;
                    }

                } else {

                    if (mCurrentScaleFactor <= mTargetScaleFactor) {
                        mCurrentScaleFactor = mTargetScaleFactor;
                    }

                }
                postDelayed(this, mAutoTime);
            } else {
                mCurrentScaleFactor = mTargetScaleFactor;
            }

            checkBorder();
            invalidate();
        }
    }

    private class CanLinearLayoutManager extends LinearLayoutManager {
        public CanLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        @Override
        public int scrollVerticallyBy(int dy, Recycler recycler, State state) {


            int result = super.scrollVerticallyBy((int) Math.ceil(dy / mCurrentScaleFactor), recycler, state);
            if (result == Math.ceil(dy / mCurrentScaleFactor)) {
                return dy;
            }
            return result;
        }
    }


    public CanScaleRecyclerView(Context context) {
        this(context, null);
    }

    public CanScaleRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CanScaleRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutManager(new CanLinearLayoutManager(context, VERTICAL, false));

        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.CanScaleRecyclerView);
        for (int i = 0; i < ta.getIndexCount(); i++) {
            int attr = ta.getIndex(i);
            if (attr == R.styleable.CanScaleRecyclerView_minScaleFactor) {
                mMinScaleFactor = ta.getFloat(attr, 1.0f);
            } else if (attr == R.styleable.CanScaleRecyclerView_maxScaleFactor) {
                mMaxScaleFactor = ta.getFloat(attr, mMinScaleFactor * 4);
            } else if (attr == R.styleable.CanScaleRecyclerView_autoScaleTime) {
                mAutoTime = ta.getInt(attr, 5);
            }
        }
        ta.recycle();

        mMidScaleFactor = (mMinScaleFactor + mMaxScaleFactor) / 2;
        mCurrentScaleFactor = mMinScaleFactor;



        initDetector();
    }






    /**
     * 设置手势监听
     */
    private void initDetector() {
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {



                // 获取缩放的中心点
                centerX = detector.getFocusX();
                centerY = detector.getFocusY();
                isScale = true;

                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {


                mCurrentScaleFactor *= detector.getScaleFactor();
                mCurrentScaleFactor = Math.max(mMinScaleFactor, Math.min(mCurrentScaleFactor, mMaxScaleFactor));

                CanScaleRecyclerView.this.invalidate();

                if (mOnGestureListener != null) {
                    mOnGestureListener.onScale(detector);
                }
                return true;
            }


            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

                isScale = false;
                super.onScaleEnd(detector);
            }
        });

        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {


            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {

                //点击
                return mOnGestureListener != null && mOnGestureListener.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {

                //双击缩放
                centerX = e.getRawX();
                centerY = e.getRawY();

                if (mCurrentScaleFactor < mMidScaleFactor) {
                    postDelayed(new AutoScaleRunnable(mMidScaleFactor, mAutoBigger), mAutoTime);
                } else if (mCurrentScaleFactor < mMaxScaleFactor) {
                    postDelayed(new AutoScaleRunnable(mMaxScaleFactor, mAutoBigger), mAutoTime);
                } else {
                    postDelayed(new AutoScaleRunnable(mMinScaleFactor, mAutoSmall), mAutoTime);
                }

                if (mOnGestureListener != null) {
                    mOnGestureListener.onDoubleTap(e);
                }
                return true;
            }
        });
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        if (mCurrentScaleFactor == 1.0f) {
            mOffsetX = 0.0f;
            mOffsetY = 0.0f;
        }

        canvas.translate(mOffsetX, mOffsetY);//偏移

        canvas.scale(mCurrentScaleFactor, mCurrentScaleFactor, centerX, centerY);//缩放
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (mGestureDetector.onTouchEvent(event)) {
            mMainPointerId = event.getPointerId(0);
            return true;
        }

        mScaleGestureDetector.onTouchEvent(event);


        //缩放时不偏移
        if (isScale) {
            return true;
        }


        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                mMainPointerId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                int mainPointIndex = event.findPointerIndex(mMainPointerId);
                float mainPointX = event.getX(mainPointIndex);
                float mainPointY = event.getY(mainPointIndex);


                //滑动时偏移
                mOffsetX += (mainPointX - mLastTouchX);
                mOffsetY += (mainPointY - mLastTouchY);


                mLastTouchX = mainPointX;
                mLastTouchY = mainPointY;


                checkBorder();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                mMainPointerId = INVALID_POINTER_ID;
                break;
            case MotionEvent.ACTION_CANCEL:
                mMainPointerId = INVALID_POINTER_ID;
                break;
            case MotionEvent.ACTION_POINTER_UP: {

                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mMainPointerId) {

                    int newPointerIndex = (pointerIndex == 0 ? 1 : 0);
                    mLastTouchX = event.getX(newPointerIndex);
                    mLastTouchY = event.getY(newPointerIndex);
                    mMainPointerId = event.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }


    /**
     * 保证在边界内,通过缩放中心距左右、上下边界的比例确定
     */
    private void checkBorder() {


        float sumOffsetX = getWidth() * (mCurrentScaleFactor - 1.0f);
        float sumOffsetY = getHeight() * (mCurrentScaleFactor - 1.0f);


        float numX = (getWidth() - centerX) / centerX + 1;


        float offsetLeftX = sumOffsetX / numX;


        float offsetRightX = ((getWidth() - centerX) / centerX) * offsetLeftX;


        float numY = (getHeight() - centerY) / centerY + 1;


        float offsetTopY = sumOffsetY / numY;


        float offsetBottomY = ((getHeight() - centerY) / centerY) * offsetTopY;


        if (mOffsetX > offsetLeftX) {
            mOffsetX = offsetLeftX;

        }

        if (mOffsetX < -offsetRightX) {

            mOffsetX = -offsetRightX;
        }


        if (mOffsetY > offsetTopY) {
            mOffsetY = offsetTopY;

        }

        if (mOffsetY < -offsetBottomY) {

            mOffsetY = -offsetBottomY;
        }


    }


    public int getAutoTime() {
        return mAutoTime;
    }

    public void setAutoTime(int autoTime) {
        this.mAutoTime = autoTime;
    }

    public float getMinScaleFactor() {
        return mMinScaleFactor;
    }

    public void setMinScaleFactor(float initScaleFactor) {
        this.mMinScaleFactor = initScaleFactor;
    }

    public float getMaxScaleFactor() {
        return mMaxScaleFactor;
    }

    public void setMaxScaleFactor(float maxScaleFactor) {
        this.mMaxScaleFactor = maxScaleFactor;
    }


    public OnGestureListener getOnGestureListener() {
        return mOnGestureListener;
    }

    public void setOnGestureListener(OnGestureListener onGestureListener) {
        this.mOnGestureListener = onGestureListener;
    }


}
