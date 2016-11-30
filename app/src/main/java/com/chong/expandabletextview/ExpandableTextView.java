package com.chong.expandabletextview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 可以展开折叠的带过渡动画的TextView
 */
public class ExpandableTextView extends LinearLayout implements View.OnClickListener {

    private static final String TAG = ExpandableTextView.class.getSimpleName();
    /**
     * 默认最大折叠最大行数
     */
    private static final int MAX_COLLAPSED_LINES = 8;
    /**
     * 内容文本
     */
    protected TextView mTvContent;
    /**
     * 展开/折叠按钮
     */
    protected ImageButton mBtnState;
    /**
     * 展开/折叠文本
     */
    protected TextView mTvState;
    /**
     * 是否需要重新布局
     */
    private boolean isRelayout;
    /**
     * 默认TextView处于折叠状态
     */
    private boolean mCollapsed = true;
    /**
     * 折叠最大显示行数
     */
    private int mMaxCollapsedLines;
    /**
     * 向下展开的图标
     */
    private Drawable mExpandDrawable;
    /**
     * 向上折叠的图标
     */
    private Drawable mCollapseDrawable;
    /**
     * 是否展示展开/折叠按钮
     */
    private boolean isNeedDrawable = true;
    /**
     * 是否展示展开/折叠文本
     */
    private boolean isNeedText = false;
    /**
     * 向下展开文本
     */
    private CharSequence mExpandText;
    /**
     * 向上折叠文本
     */
    private CharSequence mCollapseText;
    /**
     * 在列表中，保存状态
     */
    private SparseBooleanArray mCollapsedStatus;
    /**
     * 列表中位置
     */
    private int mPosition;
    /**
     * 只需要展开，不需要折叠
     */
    private boolean isOnlyExpand;

    public ExpandableTextView(Context context) {
        this(context, null);
    }

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ExpandableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    @Override
    public void setOrientation(int orientation) {
        if (LinearLayout.HORIZONTAL == orientation) {
            throw new IllegalArgumentException("ExpandableTextView only supports Vertical Orientation.");
        }
        super.setOrientation(orientation);
    }

    @Override
    public void onClick(View view) {
        if (isNeedDrawable) {
            if (mBtnState.getVisibility() != View.VISIBLE) {
                return;
            }
        }
        if (isNeedText) {
            if (mTvState.getVisibility() != View.VISIBLE) {
                return;
            }
        }

        mCollapsed = !mCollapsed;
        if (isNeedDrawable) {
            if (isOnlyExpand && !mCollapsed) {
                mBtnState.setVisibility(GONE);
            } else {
                mBtnState.setVisibility(VISIBLE);
            }
            isRelayout = true;
            mBtnState.setImageDrawable(mCollapsed ? mExpandDrawable : mCollapseDrawable);
        }
        if (isNeedText) {
            if (isOnlyExpand && !mCollapsed) {
                mTvState.setVisibility(GONE);
            } else {
                mTvState.setVisibility(VISIBLE);
            }
            isRelayout = true;
            mTvState.setText(mCollapsed ? mExpandText : mCollapseText);
        }
        requestLayout();

        // 按位置保存展开/折叠状态
        if (mCollapsedStatus != null) {
            mCollapsedStatus.put(mPosition, mCollapsed);
        }
    }

    @Override
    protected void onFinishInflate() {
        findViews();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 如果没有改变显示内容，或者显示内容为空，执行super.onMeasure()并返回
        if (!isRelayout || getVisibility() == View.GONE) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        isRelayout = false;
        // 先隐藏状态按钮，将文字最大显示行数设置到最大，后面再根据测量情况修改
        if (isNeedDrawable) {
            mBtnState.setVisibility(View.GONE);
        }
        if (isNeedText) {
            mTvState.setVisibility(View.GONE);
        }
        mTvContent.setMaxLines(Integer.MAX_VALUE);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 未超过最大折叠行数
        if (mTvContent.getLineCount() <= mMaxCollapsedLines) {
            return;
        }

        // 需要折叠
        if (mCollapsed) {
            mTvContent.setMaxLines(mMaxCollapsedLines);
        }
        if (isNeedDrawable) {
            if (isOnlyExpand && !mCollapsed) {
                mBtnState.setVisibility(GONE);
            } else {
                mBtnState.setVisibility(VISIBLE);
            }
        }
        if (isNeedText) {
            if (isOnlyExpand && !mCollapsed) {
                mTvState.setVisibility(GONE);
            } else {
                mTvState.setVisibility(VISIBLE);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setText(@Nullable CharSequence text) {
        isRelayout = true;
        mTvContent.setText(text);
        setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    // 在列表中使用时，设置文本
    public void setText(@Nullable CharSequence text, @NonNull SparseBooleanArray collapsedStatus, int position) {
        mCollapsedStatus = collapsedStatus;
        mPosition = position;
        boolean isCollapsed = collapsedStatus.get(position, true);
        clearAnimation();
        mCollapsed = isCollapsed;
        if (isNeedDrawable) {
            mBtnState.setImageDrawable(mCollapsed ? mExpandDrawable : mCollapseDrawable);
        }
        if (isNeedText) {
            mTvState.setText(mCollapsed ? mExpandText : mCollapseText);
        }
        setText(text);
        getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        requestLayout();
    }

    public CharSequence getText() {
        if (mTvContent == null) {
            return "";
        }
        return mTvContent.getText();
    }

    private void init(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ExpandableTextView);
        mMaxCollapsedLines = typedArray.getInt(R.styleable.ExpandableTextView_max_collapsed_lines, MAX_COLLAPSED_LINES);
        mExpandDrawable = typedArray.getDrawable(R.styleable.ExpandableTextView_expand_drawable);
        mCollapseDrawable = typedArray.getDrawable(R.styleable.ExpandableTextView_collapse_drawable);
        isNeedDrawable = typedArray.getBoolean(R.styleable.ExpandableTextView_need_drawable, true);
        isNeedText = typedArray.getBoolean(R.styleable.ExpandableTextView_need_text, false);
        mExpandText = typedArray.getString(R.styleable.ExpandableTextView_expand_text);
        mCollapseText = typedArray.getString(R.styleable.ExpandableTextView_collapse_text);
        isOnlyExpand = typedArray.getBoolean(R.styleable.ExpandableTextView_only_expand, false);

        if (isNeedDrawable) {
            if (mExpandDrawable == null) {
                mExpandDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_expand_more_black_12dp);
            }
            if (mCollapseDrawable == null) {
                mCollapseDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_expand_less_black_12dp);
            }
        }
        if (isNeedText) {
            if (mExpandText == null) {
                mExpandText = "展开";
            }
            if (mCollapseText == null) {
                mCollapseText = "收起";
            }
        }
        typedArray.recycle();

        setOrientation(LinearLayout.VERTICAL);
        setVisibility(GONE);
    }

    private void findViews() {
        mTvContent = (TextView) findViewById(R.id.tv_expandable_text);
        if (isNeedDrawable) {
            mBtnState = (ImageButton) findViewById(R.id.expand_collapse);
            mBtnState.setImageDrawable(mCollapsed ? mExpandDrawable : mCollapseDrawable);
            mBtnState.setOnClickListener(this);
        }
        if (isNeedText) {
            mTvState = (TextView) findViewById(R.id.tv_expand_collapse);
            mTvState.setText(mCollapsed ? mExpandText : mCollapseText);
            mTvState.setOnClickListener(this);
        }
    }
}