package com.chong.expandabletextview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;


/**
 * 按行数进行折叠带过渡动画的TextView
 */
public class ExpandableTextView extends LinearLayout implements OnClickListener {

    /**
     * TextView
     */
    private TextView textView;

    /**
     * 收起/全部TextView
     */
    private TextView tvState;

    /**
     * 点击进行折叠/展开的图片
     */
    private ImageView ivExpandOrShrink;

    /**
     * 底部是否折叠/收起的父类布局
     */
    private RelativeLayout rlToggleLayout;

    /**
     * 提示折叠的图片资源
     */
    private Drawable drawableShrink;
    /**
     * 提示显示全部的图片资源
     */
    private Drawable drawableExpand;

    /**
     * 全部/收起文本的字体颜色
     */
    private int textViewStateColor;
    /**
     * 展开提示文本
     */
    private String textExpand;
    /**
     * 收缩提示文本
     */
    private String textShrink;

    /**
     * 是否是折叠状态
     */
    private boolean isShrink = false;

    /**
     * 是否需要折叠
     */
    private boolean isExpandNeeded = false;

    /**
     * 是否初始化TextView
     */
    private boolean isInitTextView = true;

    /**
     * 折叠显示的行数
     */
    private int expandLines;

    /**
     * 文本的行数
     */
    private int textLines;

    /**
     * 显示的文本
     */
    private CharSequence textContent;

    /**
     * 显示的文本颜色
     */
    private int textContentColor;

    /**
     * 显示的文本字体大小
     */
    private float textContentSize;

    /**
     * 动画过度间隔
     */
    private int sleepTime = 30;

    /**
     * handler信号
     */
    private static final int WHAT = 2;
    /**
     * 动画结束信号
     */
    private static final int WHAT_ANIMATION_END = 3;

    /**
     * 动画结束，只是改变图标，并不隐藏
     */
    private static final int WHAT_EXPAND_ONLY = 4;

    private MyHandler handler;

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initValue(context, attrs);
        initView(context);
        initClick();
    }

    private void initValue(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs,
                R.styleable.ExpandableTextView);

        expandLines = ta.getInteger(
                R.styleable.ExpandableTextView_tv_expandLines, 5);

        drawableShrink = ta
                .getDrawable(R.styleable.ExpandableTextView_tv_shrinkBitmap);
        drawableExpand = ta
                .getDrawable(R.styleable.ExpandableTextView_tv_expandBitmap);

        textViewStateColor = ta.getColor(R.styleable.ExpandableTextView_tv_textStateColor,
                ContextCompat.getColor(context, R.color.colorPrimary));

        textShrink = ta.getString(R.styleable.ExpandableTextView_tv_textShrink);
        textExpand = ta.getString(R.styleable.ExpandableTextView_tv_textExpand);

        if (null == drawableShrink) {
            drawableShrink = ContextCompat.getDrawable(context, R.drawable.icon_green_arrow_up);
        }

        if (null == drawableExpand) {
            drawableExpand = ContextCompat.getDrawable(context, R.drawable.icon_green_arrow_down);
        }

        if (TextUtils.isEmpty(textShrink)) {
            textShrink = context.getString(R.string.shrink);
        }

        if (TextUtils.isEmpty(textExpand)) {
            textExpand = context.getString(R.string.expand);
        }


        textContentColor = ta.getColor(R.styleable.ExpandableTextView_tv_textContentColor, ContextCompat.getColor(context, R.color.color_gray_light_content_text));
        textContentSize = ta.getDimension(R.styleable.ExpandableTextView_tv_textContentSize, 14);

        ta.recycle();
    }

    private void initView(Context context) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.layout_expandable_textview, this);

        rlToggleLayout = (RelativeLayout) findViewById(R.id.rl_expandable);

        textView = (TextView) findViewById(R.id.tv_expandable);
        textView.setTextColor(textContentColor);
        textView.getPaint().setTextSize(textContentSize);

        ivExpandOrShrink = (ImageView) findViewById(R.id.iv_expandable);

        tvState = (TextView) findViewById(R.id.tv_expandable_hint);
        tvState.setTextColor(textViewStateColor);

        handler = new MyHandler(this, textView);
    }

    private void initClick() {
        textView.setOnClickListener(this);
        rlToggleLayout.setOnClickListener(this);
    }

    public void setText(CharSequence charSequence) {

        textContent = charSequence;

        textView.setText(charSequence.toString());

        ViewTreeObserver viewTreeObserver = textView.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new OnPreDrawListener() {

            @Override
            public boolean onPreDraw() {
                if (!isInitTextView) {
                    return true;
                }
                textLines = textView.getLineCount();
                isExpandNeeded = textLines > expandLines;
                isInitTextView = false;
                if (isExpandNeeded) {
                    isShrink = true;
                    textView.setMaxLines(expandLines);
                    setExpandState(expandLines);
                } else {
                    isShrink = false;
                    doNotExpand();
                }
                return true;
            }
        });

    }

    private static class MyHandler extends Handler {
        WeakReference<ExpandableTextView> mViewWeakReference;
        WeakReference<TextView> mTextViewWeakReference;

        public MyHandler(ExpandableTextView view, TextView textView) {
            mViewWeakReference = new WeakReference<>(view);
            mTextViewWeakReference = new WeakReference<>(textView);
        }

        @Override
        public void handleMessage(Message msg) {
            ExpandableTextView view = mViewWeakReference.get();
            TextView textView = mTextViewWeakReference.get();
            if (view != null && textView != null) {
                if (WHAT == msg.what) {
                    textView.setMaxLines(msg.arg1);
                } else if (WHAT_ANIMATION_END == msg.what) {
                    view.setExpandState(msg.arg1);
                } else if (WHAT_EXPAND_ONLY == msg.what) {
                    view.changeExpandState(msg.arg1);
                }
            }
        }
    }

    /**
     * @param startIndex 开始动画的起点行数
     * @param endIndex   结束动画的终点行数
     * @param what       动画结束后的handler信号标示
     */
    private void doAnimation(final int startIndex, final int endIndex, final int what) {

        // 动画线程
        new Thread(new Runnable() {

            @Override
            public void run() {

                if (startIndex < endIndex) {
                    // 如果起止行数小于结束行数，那么往下一行一行展开至结束行数
                    int count = startIndex;
                    while (count++ < endIndex) {
                        Message msg = handler.obtainMessage(WHAT, count, 0);

                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        handler.sendMessage(msg);
                    }
                } else if (startIndex > endIndex) {
                    // 如果起止行数大于结束行数，那么往上一行一行折叠至结束行数
                    int count = startIndex;
                    while (count-- > endIndex) {
                        Message msg = handler.obtainMessage(WHAT, count, 0);
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        handler.sendMessage(msg);
                    }
                }

                // 动画结束后发送结束的信号
                Message msg = handler.obtainMessage(what, endIndex, 0);
                handler.sendMessage(msg);

            }

        }).start();

    }

    /**
     * 确定收起折叠状态
     *
     * @param endIndex
     */
    @SuppressWarnings("deprecation")
    private void changeExpandState(int endIndex) {
        rlToggleLayout.setVisibility(View.VISIBLE);
        if (endIndex < textLines) {
            ivExpandOrShrink.setBackgroundDrawable(drawableExpand);
            tvState.setText(textExpand);
        } else {
            ivExpandOrShrink.setBackgroundDrawable(drawableShrink);
            tvState.setText(textShrink);
        }

    }

    /**
     * 设置折叠状态（如果折叠行数设定大于文本行数，那么折叠/展开图片布局将会隐藏,文本将一直处于展开状态）
     *
     * @param endIndex
     */
    @SuppressWarnings("deprecation")
    private void setExpandState(int endIndex) {

        if (endIndex < textLines) {
            isShrink = true;
            rlToggleLayout.setVisibility(View.VISIBLE);
            ivExpandOrShrink.setBackgroundDrawable(drawableExpand);
            textView.setOnClickListener(this);
            tvState.setText(textExpand);
        } else {
            isShrink = false;
            rlToggleLayout.setVisibility(View.GONE);
            ivExpandOrShrink.setBackgroundDrawable(drawableShrink);
            textView.setOnClickListener(null);
            tvState.setText(textShrink);
        }

    }

    /**
     * 无需折叠
     */
    private void doNotExpand() {
        textView.setMaxLines(expandLines);
        rlToggleLayout.setVisibility(View.GONE);
        textView.setOnClickListener(null);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.rl_expandable || v.getId() == R.id.tv_expandable) {
            clickImageToggle();
        }

    }

    private void clickImageToggle() {
        if (isShrink) {
            // 如果是已经折叠，那么进行非折叠处理
            doAnimation(expandLines, textLines, WHAT_EXPAND_ONLY);
        } else {
            // 如果是非折叠，那么进行折叠处理
            doAnimation(textLines, expandLines, WHAT_EXPAND_ONLY);
        }

        // 切换状态
        isShrink = !isShrink;
    }

    public Drawable getDrawableShrink() {
        return drawableShrink;
    }

    public void setDrawableShrink(Drawable drawableShrink) {
        this.drawableShrink = drawableShrink;
    }

    public Drawable getDrawableExpand() {
        return drawableExpand;
    }

    public void setDrawableExpand(Drawable drawableExpand) {
        this.drawableExpand = drawableExpand;
    }

    public int getExpandLines() {
        return expandLines;
    }

    public void setExpandLines(int newExpandLines) {
        int start = isShrink ? this.expandLines : textLines;
        int end = textLines < newExpandLines ? textLines : newExpandLines;
        doAnimation(start, end, WHAT_ANIMATION_END);
        this.expandLines = newExpandLines;
    }

    /**
     * 取得显示的文本内容
     *
     * @return content text
     */
    public CharSequence getTextContent() {
        return textContent;
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

}
