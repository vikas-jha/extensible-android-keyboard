package com.example.extkeyboard.views;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.extkeyboard.ExtensibleKeyboard;
import com.example.extkeyboard.GenericKeyboard;
import com.example.extkeyboard.GraphicsUtils;
import com.example.extkeyboard.R;
import com.example.extkeyboard.Util;
import com.example.extkeyboard.internal.Configuration;
import com.example.extkeyboard.internal.Constants;

/**
 * Created by vijha on 10/2/2017.
 */

public class KeyPopup extends PopupActivity {

    private Context context;
    private CloseMessageHandler handler = new CloseMessageHandler();
    private LinearLayout layout;
    private TextView tv;
    private View parent;
    private WindowManager wm;
    //private GenericKeyboard.GenericKey key;

    private final int minHeight, minWidth;
    private long showTime = -1;
    private boolean longPress = false;
    private int x,y, keyWidth, lastSelected = Constants.OUT_OF_BOUNDS;
    private CharSequence selectedText = null;
    private int mColorTransparent;
    protected float deviceDensity;

    public KeyPopup(Context context) {
        super(context);
        this.context = context;

        setSplitTouchEnabled(true);
        setTouchable(true);

        wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        deviceDensity = Configuration.getInstance().getDeviceDensity();

        minWidth = (int) (40 * deviceDensity);
        minHeight = (int) (100 * deviceDensity);


        mColorTransparent = context.getResources().getColor(android.R.color.transparent);

        layout = new LinearLayout(context);

        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1,1,1,1);
        layout.setLayoutParams(layoutParams);
        layout.setPadding(2,2,2,2);
        layout.setBackgroundColor(Constants.COLOR_WHITE);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.TOP);
        setContentView(layout);


        tv = new TextView(context);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(tv);

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
    }

    public void show(View parent, Rect keyRect, String text){

        if(isShowing()){
            dismiss();
        }

        if(tv.getParent() == null){
            layout.addView(tv);
        }

        this.parent = parent;
        this.selectedText = null;
        this.lastSelected = Constants.OUT_OF_BOUNDS;

        boolean isFullScreen = false;

        if(context instanceof  ExtensibleKeyboard){
            isFullScreen = ((ExtensibleKeyboard)context).isFullscreenMode();
        }

        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        int x, y;
        if(text == null){
            return;
        }

        int bgColor = Configuration.getInstance().getKeyboardBgColor();
        float brightness = GraphicsUtils.getBrightness(bgColor);
        if(brightness > 0.5){
            layout.setBackground(context.getDrawable(R.drawable.preview_border));
            layout.getBackground().setColorFilter(bgColor/* | Constants.COLOR_NOAPLPHA_OR*/, PorterDuff.Mode.MULTIPLY);
        }else{
            layout.setBackground(context.getDrawable(R.drawable.preview_border_light));
            layout.getBackground().setColorFilter(bgColor/* | Constants.COLOR_NOAPLPHA_OR*/, PorterDuff.Mode.LIGHTEN);
        }
        setBackgroundDrawable(context.getDrawable(R.drawable.preview_border));

        int height = (int)((keyRect.top - keyRect.bottom) * 1.5);
        height = height > minHeight ? minHeight : minHeight;
        setHeight(height);

        int width = (keyRect.right - keyRect.left) > minWidth ? (keyRect.right - keyRect.left) : minWidth;
        setWidth(width);

        if(isFullScreen){

            Display display = wm.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            y = metrics.heightPixels - parentHeight + keyRect.top - height - (keyRect.top - keyRect.bottom)/2;
        }else{
            y = keyRect.top - height + (keyRect.top - keyRect.bottom);
        }


        /*if(text.length() > 2){
            tv.setTextSize(16);
        }else{
            tv.setTextSize(26);
        }
        tv.setText(text);*/

        setText(text);

        //tv.setPadding(0,20,0,0);

        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setTextColor(Configuration.getInstance().getKeyTextColor());



        x = keyRect.left + (keyRect.right - keyRect.left)/2 - getWidth()/2;
        if(keyRect.left + getWidth() > parentWidth){
            x = parentWidth - getWidth();
        }else if(x < 0){
            x = 0;
        }
        showAtLocation(parent, Gravity.NO_GRAVITY, x, y);
        showTime = SystemClock.uptimeMillis();
        this.x = x;
        this.y = y;

    }

    public void show(View parent, GenericKeyboard.GenericKey key){

        if(isShowing()){
            dismiss();
        }

        if(tv.getParent() == null){
            layout.addView(tv);
        }

        this.parent = parent;
        this.selectedText = null;
        this.lastSelected = Constants.OUT_OF_BOUNDS;

        boolean isFullScreen = false;

        if(context instanceof  ExtensibleKeyboard){
            isFullScreen = ((ExtensibleKeyboard)context).isFullscreenMode();
        }

        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        int x, y;
        if(key.label == null){
            return;
        }

        int bgColor = Configuration.getInstance().getKeyboardBgColor();
        float brightness = GraphicsUtils.getBrightness(bgColor);
        if(brightness > 0.8){
            layout.setBackground(context.getDrawable(R.drawable.preview_border));
            layout.getBackground().setColorFilter(bgColor, PorterDuff.Mode.MULTIPLY);
        }else{
            layout.setBackground(context.getDrawable(R.drawable.preview_border_light));
            layout.getBackground().setColorFilter(bgColor, PorterDuff.Mode.LIGHTEN);
        }
        setBackgroundDrawable(context.getDrawable(R.drawable.preview_border));

        int height = (int)(key.height * 1.5);
        height = height > minHeight ? minHeight : minHeight;
        setHeight(height);

        int width = key.width > minWidth ? key.width : minWidth;
        setWidth(width);

        if(isFullScreen){

            Display display = wm.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            y = metrics.heightPixels - parentHeight + key.y - height - key.height/2;
        }else{
            y = key.y - height + key.height;
        }

        setText(key.label.toString());
        /*float textWidth = tv.getPaint().measureText(key.label.toString());
        if(textWidth >  tv.getWidth() - 10){
            tv.getPaint().setTextScaleX((tv.getWidth() - 10)/textWidth);
        }else{
            tv.getPaint().setTextScaleX(1);
        }

        if(key.label.length() > 2){
            tv.setTextSize(16);
        }else{
            tv.setTextSize(26);
        }
        tv.setText(key.label);*/

        //tv.setPadding(0,20,0,0);

        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setTextColor(Configuration.getInstance().getKeyTextColor());
        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });


        x = key.x + key.width/2 - getWidth()/2;
        if(key.x + getWidth() > parentWidth){
            x = parentWidth - getWidth();
        }else if(x < 0){
            x = 0;
        }
        showAtLocation(parent, Gravity.NO_GRAVITY, x, y);
        showTime = SystemClock.uptimeMillis();
        this.x = x;
        this.y = y;
        /*
        setting m = new setting();
        m.obj = showTime;
        handler.sendMessageDelayed(new setting(), 2000);*/

    }

    public void setText(String text){
        if(text.length() > 2){
            tv.setTextSize(18);
        }else{
            tv.setTextSize(26);
        }

        float textWidth = tv.getPaint().measureText(text);
        if(textWidth >  tv.getWidth() - 20){
            tv.setTextSize(22);
        }


        tv.setText(text);
    }

    public void onLongPress(GenericKeyboard.GenericKey key){
        if(isShowing() && key != null
                && key.keyHints != null && key.popupResId != 0){
            longPress = true;
            int length = key.keyHints.length;
            int keyWidth;
            int width = getWidth();
            if(width > length * minWidth){
                keyWidth = width / length;
            }else{
                keyWidth = minWidth;
                int newWidth = keyWidth * length;
                update(this.x - (newWidth - width)/2, this.y, newWidth, getHeight(), true);
            }

            this.keyWidth = keyWidth;

            layout.removeAllViews();
            layout.setOrientation(LinearLayout.HORIZONTAL);

            for(String text : key.keyHints){
                TextView tv = new TextView(context);
                tv.setGravity(Gravity.CENTER_HORIZONTAL);

                LinearLayout.LayoutParams layoutParams =
                        new LinearLayout.LayoutParams(keyWidth, getHeight()-2);
                tv.setLayoutParams(layoutParams);

                if(text.length() > 2){
                    tv.setTextSize(18);
                }else{
                    tv.setTextSize(26);
                }
                tv.setText(text);
                tv.setTextColor(Configuration.getInstance().getKeyTextColor());

                tv.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return false;
                    }
                });

                layout.addView(tv);
            }


        }

    }

    public void updatePosition(Rect keyRect, String text){
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        int x, y;
        if(text == null){
            this.dismiss();
        }

        int bgColor = Configuration.getInstance().getKeyboardBgColor();
        float brightness = GraphicsUtils.getBrightness(bgColor);
        if(brightness > 0.8){
            layout.setBackground(context.getDrawable(R.drawable.preview_border));
            layout.getBackground().setColorFilter(bgColor, PorterDuff.Mode.MULTIPLY);
        }else{
            layout.setBackground(context.getDrawable(R.drawable.preview_border_light));
            layout.getBackground().setColorFilter(bgColor, PorterDuff.Mode.LIGHTEN);
        }
        setBackgroundDrawable(context.getDrawable(R.drawable.preview_border));

        int height = (int)((keyRect.top - keyRect.bottom) * 1.5);
        height = height > minHeight ? minHeight : minHeight;
        setHeight(height);

        int width = (keyRect.right - keyRect.left) > minWidth ? (keyRect.right - keyRect.left) : minWidth;
        setWidth(width);

        boolean isFullScreen = false;
        if(context instanceof  ExtensibleKeyboard){
            isFullScreen = ((ExtensibleKeyboard)context).isFullscreenMode();
        }

        if(isFullScreen){

            Display display = wm.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            y = metrics.heightPixels - parentHeight + keyRect.top - height - (keyRect.top - keyRect.bottom)/2;
        }else{
            y = keyRect.top - height + (keyRect.top - keyRect.bottom);
        }

        setText(text);

        /*if(text.length() > 2){
            tv.setTextSize(16);
        }else{
            tv.setTextSize(26);
        }
        tv.setText(text);*/

        x = keyRect.left + (keyRect.right - keyRect.left)/2 - getWidth()/2;
        if(keyRect.left + getWidth() > parentWidth){
            x = parentWidth - getWidth();
        }else if(x < 0){
            x = 0;
        }

        update(x, y, width, height, true);
    }

    public boolean onTouchEvent(MotionEvent me){
        if(longPress){
            ViewGroup group = (ViewGroup)layout;

            int[] position = new int[2], parentPosition = new int[2];
            group.getLocationOnScreen(position);
            this.parent.getLocationOnScreen(parentPosition);
            float x = me.getRawX() - position[0] , y = me.getRawY() - position[1];

            Rect r = new Rect(0, 0, getWidth(), getHeight());

            if(r.contains((int)x,(int)y)){
                int index = (int)x / keyWidth;

                if(lastSelected != Constants.OUT_OF_BOUNDS && lastSelected != index){
                    TextView lastTv = (TextView) group.getChildAt(lastSelected);
                    lastTv.setBackgroundColor(mColorTransparent);
                    lastTv.requestLayout();
                }

                if(group.getChildAt(index) instanceof  TextView){
                    TextView tv = (TextView) group.getChildAt(index);
                   if(GraphicsUtils.getBrightness(Configuration.getInstance().getKeyboardBgColor()) > 0.4) {
                        tv.setBackground(context.getDrawable(R.drawable.pressed));
                    }else{
                        tv.setBackground(context.getDrawable(R.drawable.pressed_light));
                    }

                    tv.invalidate();
                    selectedText = tv.getText();
                    lastSelected = index;
                }


            }else{
                if(me.getAction() == MotionEvent.ACTION_UP){
                    selectedText = null;
                }
                if(lastSelected != Constants.OUT_OF_BOUNDS){
                    TextView lastTv = (TextView) group.getChildAt(lastSelected);
                    lastTv. setBackgroundColor(mColorTransparent);
                    lastTv.requestLayout();
                }
            }


            return false;
        }
        return false;
    }

    @Override
    public void dismiss(){
        if(longPress){
            layout.removeAllViews();
            longPress = false;
        }
        this.parent = null;
        super.dismiss();
    }

    public void dismiss(int delay){
        Message m = new Message();
        m.obj = showTime;
        handler.sendMessageDelayed(m, 50);
    }

    public String getSelectedText(){
        if(selectedText != null){
            return selectedText.toString();
        }
        return null;
    }

    public boolean getLongPress(){
        return  longPress;
    }



    private class CloseMessageHandler extends Handler{

        @Override
        public void handleMessage(Message msg) {
            long t = (long)msg.obj;
            if(showTime == t){
                dismiss();
            }

        }
    }
}
