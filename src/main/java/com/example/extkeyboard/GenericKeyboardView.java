/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.extkeyboard;

import java.lang.reflect.Field;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodSubtype;

import com.example.extkeyboard.GenericKeyboard.GenericKey;
import com.example.extkeyboard.internal.Configuration;
import com.example.extkeyboard.internal.Constants;
import com.example.extkeyboard.views.KeyPopup;

public class GenericKeyboardView extends KeyboardView {
	
	
	private int mTouchX = Constants.OUT_OF_BOUNDS, mTouchY = Constants.OUT_OF_BOUNDS, keyIndex = Constants.OUT_OF_BOUNDS;
	
	private int nTap = 0;
	private long lastTapTime = Constants.OUT_OF_BOUNDS, keyDownTime;
	private GenericKey lastKey;
	private KeyMessageHandler handler = new KeyMessageHandler();
	
	protected final static int UP = 2, RIGHT = 3, DOWN = 4, LEFT = 1, TAP_TIMEOUT = 800;
	private final static int MSG_KEYPRESS = 12000, MSG_CLOSEPOPUP = 12001;

	protected Configuration configuration;
    protected KeyBoardsManager km;
	protected Paint paint;
	protected KeyPopup keyPopup;
	protected boolean popupSupported = true, customFont = false;
    protected Bitmap bitmap;
    protected boolean reDraw = true;
    protected boolean customDraw = false;
    protected Typeface kbFont;
    protected  int popupTextSize = 0;

    protected String fontSize;

    protected float deviceDensity;

    protected Drawable keyBg;


    public GenericKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setProperties(context);
    }

    public GenericKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setProperties(context);

    }

    public void updateProperties(){
        setProperties(getContext());
    }

    private void setProperties(Context context){
    	configuration = Configuration.getInstance();
        km = KeyBoardsManager.getInstance();
        this.setBackgroundColor( configuration.getKeyboardBgColor());
        setKeyTextColor(configuration.getKeyTextColor());

        deviceDensity = configuration.getDeviceDensity();
        if(keyPopup == null){
            keyPopup = new KeyPopup(context);
        }

		paint = new Paint();
		paint.setAntiAlias(true);


    }

    public void setKeyTextColor(int color){
		try {

			Class<?> claz = this.getClass();
			while(claz != KeyboardView.class){
				claz = claz.getSuperclass();
			}

			Field textColorField = claz.getDeclaredField("mKeyTextColor");
			textColorField.setAccessible(true);
			textColorField.set(this, color);
			textColorField.setAccessible(false);

            Field keyBackGround = claz.getDeclaredField("mKeyBackground");
            keyBackGround.setAccessible(true);
            if(GraphicsUtils.getBrightness(configuration.getKeyboardBgColor()) > 0.5){
                keyBg = getContext().getResources().getDrawable(R.drawable.key_background);

            }else{
                keyBg = getContext().getResources().getDrawable(R.drawable.key_background_light);
            }

            keyBackGround.set(this, keyBg);

            keyBackGround.setAccessible(false);
		} catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

    public void setKeyTextSize(int size){
        try {

            Class<?> claz = this.getClass();
            while(claz != KeyboardView.class){
                claz = claz.getSuperclass();
            }

            Field textSize = claz.getDeclaredField("mKeyTextSize");
            textSize.setAccessible(true);
            textSize.set(this, size);
            textSize.setAccessible(false);


        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected boolean onLongPress(Key key) {
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(Constants.KEYCODE_OPTIONS, key.codes);
            return true;
        }else if (key.codes[0] == Constants.KEYCODE_BOARD_SWITCH) {
            getOnKeyboardActionListener().onKey(Constants.KEYCODE_KB_OPTIONS_VIEW, key.codes);
            return true;
        }else if (key instanceof GenericKey && ((GenericKey)key).keyHints != null && key.repeatable == false) {
        	if(key.codes.length > 1){
        		getOnKeyboardActionListener().onKey(key.codes[1], key.codes);
        	}else{
        		getOnKeyboardActionListener().onKey(key.codes[0], key.codes);
        	}
            return true;
        }else if(key instanceof GenericKey && 
        		key.popupCharacters != null && key.popupCharacters.length()  == 1 ){
        	int[] keyCodes = new int[2];
        	keyCodes[0] = -10;
        	keyCodes[1] = key.popupCharacters.charAt(0);
			keyPopup.setText(String.copyValueOf(Character.toChars(keyCodes[1])));
        	getOnKeyboardActionListener().onKey(keyCodes[1], keyCodes);
        	return true;
        } else if(key.popupResId == 0){
        	getOnKeyboardActionListener().onKey(key.codes[0], key.codes);
        	return true;
        }else {
            return super.onLongPress(key);
        }
    }

    void setSubtypeOnSpaceKey(final InputMethodSubtype subtype) {
        /*final IndicKeyboard keyboard = (IndicKeyboard)getKeyboard();
        keyboard.setSpaceIcon(getResources().getDrawable(subtype.getIconResId()));
        invalidateAllKeys();*/
    }
    
    public boolean handleBack() {
        if (((ExtensibleKeyboard)getOnKeyboardActionListener()).handleBack()) {
            return true;
        }
        return super.handleBack();
    }

    @Override
    public void setKeyboard(Keyboard keyboard) {
        super.setKeyboard(keyboard);
        customDraw = ((GenericKeyboard) keyboard).isCustomDraw();
        String fontName = ((GenericKeyboard) keyboard).getFont();
        if(fontName != null){
            kbFont = configuration.getFont(fontName);
        }else {
            kbFont = null;
        }

        reDraw = true;
    }

    public void invalidate(){
        super.invalidate();
        reDraw = true;
    }


    @Override
    public void onDraw(Canvas canvas) {

        if(reDraw){
            List<Key> keys = getKeyboard().getKeys();
            for(Key key : keys){
                if(key.icon != null){
                    key.icon.setColorFilter(configuration.getKeyTextColor(), PorterDuff.Mode.MULTIPLY);
                }
            }
        }

        customDraw = ((GenericKeyboard) getKeyboard()).isCustomDraw();
        if(!customDraw){
            super.onDraw(canvas);
        }

        if(bitmap == null){
            bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            reDraw = true;
        }

        if (reDraw) {
            Canvas c = new Canvas(bitmap);
            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
            drawBitmap(c);
        }
        canvas.drawBitmap(bitmap,0,0, paint);
    }

    private void drawBitmap(Canvas canvas){

        List<Key> keys = getKeyboard().getKeys();
        /*
        for(Key key : keys){
        	if(key.icon != null){
        		key.icon.setColorFilter(configuration.getKeyTextColor(), PorterDuff.Mode.MULTIPLY);
        	}
        }*/
		Paint paint = new Paint();
        paint.setAntiAlias(true);


        //If kbFont is provided draw keys using custom kbFont

		if (customDraw) {

            if(keyIndex > Constants.OUT_OF_BOUNDS){
                GenericKey key = (GenericKey) getKeyboard().getKeys().get(keyIndex);
                keyBg.setBounds(key.x, key.y, key.x + key.width, key.y + key.height);
                keyBg.draw(canvas);
            }

            String textSize = km.getCurrentHandler().getTextSize();
            fontSize = textSize;
            if(textSize != null && textSize.equals("small")){
                paint.setTextSize(22 * deviceDensity);
                popupTextSize = -6;
            }else{
                paint.setTextSize(20 * deviceDensity);
                popupTextSize = 0;
            }

            if(kbFont != null){
                paint.setTypeface(kbFont);
                paint.setColor(getContext().getResources().getColor(R.color.teal));
            }

            for (Key key : keys) {
                drawKey(key, canvas, paint);
            }

		}else{
			super.onDraw(canvas);
		}
		
		drawKeyBoardFeatures(canvas, paint);
        
    }

    
    protected void drawKeyBoardFeatures(Canvas canvas, Paint paint){
    	Typeface restoreToFace = paint.getTypeface();
		boolean isBold = paint.isFakeBoldText();

        if(kbFont != null){
            paint.setTypeface(kbFont);

        }


		for (Key key : getKeyboard().getKeys()) {
			drawKeyFeatures(key, canvas, paint);
		}
		
		if (restoreToFace != null) {
			paint.setTypeface(restoreToFace);
			paint.setFakeBoldText(isBold);
		}
    }
    
	protected void drawKeyFeatures(Key key, Canvas canvas, Paint paint) {

		//paint.setTextSize(12 * deviceDensity);
        if(kbFont == null){
            paint.setColor(configuration.getKeyTextColor());
        }else{
            paint.setColor(getContext().getResources().getColor(R.color.teal));
        }
		paint.setTextAlign(Paint.Align.RIGHT);

		if (key.label != null) {
			if (key.popupCharacters != null) {

				// Draw emoji symbol on keyboards
				if (key.popupCharacters.toString().codePointAt(0) == Constants.SYMBOL_EMOJI) {
					Typeface symbolaFont = Configuration.getInstance()
							.getSymbolFont();
					paint.setTypeface(symbolaFont);
                    paint.setFakeBoldText(true);
				}

				String superText = key.popupCharacters.toString();

				if (superText.length() > 4) {
					superText = superText.substring(0, 3);
                    paint.setTextSize(10 * deviceDensity);
				}else{
                    paint.setTextSize(12 * deviceDensity);
                }
				
				canvas.drawText(superText, key.x + (key.width - 5 * deviceDensity),
						key.y + 12*deviceDensity, paint);

			}

		}
		if (key.codes[0] == Constants.KEYCODE_SPACE && key.label == null) {
			int defaultColor = paint.getColor();
			paint.setColor(0x22000000 | (configuration.getKeyTextColor() & 0x00ffffff));
			canvas.drawRoundRect(key.x + 10, key.y + key.height / 4, key.x
					+ key.width - 10 , key.y + key.height * 3 / 4, 10, 10, paint);
			paint.setColor(defaultColor);
		}
	}
	
	protected void drawKey(Key key, Canvas canvas, Paint paint){
		paint.setTextAlign(Paint.Align.LEFT);



        if(key.icon != null){
            int x = key.x + (key.width - key.icon.getIntrinsicWidth())/2;
            int y = key.y + (key.height - key.icon.getIntrinsicHeight())/2;
            key.icon.setColorFilter(paint.getColor(), PorterDuff.Mode.MULTIPLY);

            key.icon.setBounds(x, y, x + key.icon.getIntrinsicWidth(), y + key.icon.getIntrinsicHeight());
            key.icon.draw(canvas);

        }else if (key.label != null && key.label.length() > 0) {
				String label = key.label.toString();
                float textSize = paint.getTextSize();
                if(label.length() > Character.toChars(label.codePointAt(0)).length){
                    paint.setTextSize(textSize - 8 * deviceDensity);
                    paint.setFakeBoldText(true);
                }else{
                    paint.setFakeBoldText(false);
                }
                float textWidth = paint.measureText(label);
            if(textWidth > key.width - 20){

                paint.setTextScaleX((key.width - 30)/textWidth);
                textWidth = paint.measureText(label);
                canvas.drawText(label, key.x + (key.width  - textWidth)/ 2, key.y
                        + key.height / 2 + 5*deviceDensity, paint);
                paint.setTextScaleX(1);

            }else{
                canvas.drawText(label, key.x + (key.width - textWidth) / 2, key.y
                        + key.height / 2 + 5 * deviceDensity, paint);
            }

                if(label.length() > Character.toChars(label.codePointAt(0)).length){
                    paint.setTextSize(textSize );
                }
			}
		}
	
	
	@Override
	public boolean performClick(){
		return super.performClick();
	}


	@Override
    public boolean onTouchEvent(MotionEvent me){
        int action = me.getAction();
		if(keyPopup.isShowing()){
			keyPopup.onTouchEvent(me);
		}

        switch (action) {
        case MotionEvent.ACTION_DOWN:
        	mTouchX = (int) me.getX();
        	mTouchY = (int) me.getY();
        	int[] indices = getKeyboard().getNearestKeys(mTouchX, mTouchY);
    		if(indices.length > 0){
    			for(int index : indices){
    				if(getKeyboard().getKeys().get(index).isInside(mTouchX, mTouchY)){
    					GenericKey key = (GenericKey) getKeyboard().getKeys().get(index);
    					if(key.codes[0] == 0){
    						return false;
    					}
    					key.onPressed();
    					keyIndex = index;
						if(popupSupported && kbFont == null && isPreviewEnabled()
								&& configuration.isPreviewKeyPress()){
							keyPopup.show(this, key);
						}

    					invalidateKey(keyIndex);
    					Message msg = new Message();
    					msg.obj = keyDownTime = me.getEventTime();
						msg.arg1 = MSG_KEYPRESS;
    					msg.arg2 = index;
    					if(key.repeatable){
    						handler.sendMessageDelayed(msg, 200);
    					}else{
    						handler.sendMessageDelayed(msg, 500);
    					}
    				}
    			}
    		}
    		
            break;
        case MotionEvent.ACTION_MOVE:
            onTouchMove(me);
            break;
        case MotionEvent.ACTION_UP:
			if(keyPopup.isShowing()){
				keyPopup.dismiss(50);
			}
        	if(keyIndex > Constants.OUT_OF_BOUNDS){
        		handleKeyUp(me);
        	}
            break;
        }
        return true;
    
	}
	
	protected class KeyMessageHandler extends Handler{
		@Override
		public void handleMessage(Message msg) {
			switch (msg.arg1){
				case MSG_KEYPRESS:

					long msgTime = (long) msg.obj;

					if(keyDownTime == msgTime && keyIndex == msg.arg2){
						GenericKey key = (GenericKey) getKeyboard().getKeys().get(keyIndex);
						if(key.repeatable){
							getOnKeyboardActionListener().onKey(key.codes[0], key.codes);
							Message m = new Message();
							m.obj = msgTime;
							m.arg1 = MSG_KEYPRESS;
							m.arg2 = msg.arg2;
							this.sendMessageDelayed(m, 25);
						}else{
							updateKeyPopup(key);
						}
					}
					break;
				case MSG_CLOSEPOPUP:
			}

	    }
	}

	private void updateKeyPopup(Key key){
		if(keyPopup.isShowing()){
			if(key instanceof GenericKey &&
					key.popupCharacters != null && key.popupCharacters.length()  > 0 ){
				if(key.popupResId == 0){
					keyPopup.setText(key.popupCharacters.toString());
				}else{
					keyPopup.onLongPress((GenericKey) key);
				}

			}
		}
	}


	public void onTouchMove(MotionEvent me){
        GenericKey key = (GenericKey) getKeyboard().getKeys().get(keyIndex);

        float distanceX = me.getX() - mTouchX, distanceY = me.getY() - mTouchY;
        float absX = Math.abs(distanceX), absY = Math.abs(distanceY);
        boolean swipe = absX > getWidth()/2 || absY > getHeight()/2;

        int minDistance = 20;
        float ratio = absX > absY ? absX/absY : absY/absX;
        if( ratio < 2 || (absX < minDistance && absY < minDistance)){
            return;
        }

        int direction = absX > absY ? (distanceX > 0 ? RIGHT : LEFT) : (distanceY > 0 ? DOWN : UP);

        if(swipe){

        }else if(keyDownTime + Constants.LONG_PRESS_TIME <  me.getEventTime()){
            onFlicking(key, 0, false);
        }else{
            onFlicking(key, direction, false);
        }


    }
    
	
	private boolean handleKeyUp(MotionEvent me){
		GenericKey key = (GenericKey) getKeyboard().getKeys().get(keyIndex);
		
		float distanceX = me.getX() - mTouchX, distanceY = me.getY() - mTouchY;
		float absX = Math.abs(distanceX), absY = Math.abs(distanceY);
		
		/*************************
		 * release the key
		 * ************************/
		releaseKey(0);

		boolean swipe = absX > getWidth()/2 || absY > getHeight()/2;

		if(!swipe && keyDownTime + Constants.LONG_PRESS_TIME <  me.getEventTime()){
			String selectedText = keyPopup.getSelectedText();
			if(keyPopup.getLongPress()){
				if(selectedText != null && selectedText.length() > 0){
					if(selectedText.length() > 1){
						getOnKeyboardActionListener().onText(selectedText);
					}else{
						int codes[] = new int[1];
						codes[0] = selectedText.codePointAt(0);
						getOnKeyboardActionListener().onKey(codes[0], codes);
					}
				}else{
					getOnKeyboardActionListener().onKey(key.codes[0], key.codes);
				}
				return true;
			}else{
				return onLongPress(key);
			}

		}

		int minDistance = 20;

		if(absX < minDistance && absY < minDistance){
			//if(key.isInside((int)me.getX(), (int) me.getY())){
				if(key == lastKey && key.codes.length > 1 && lastTapTime + TAP_TIMEOUT >= me.getEventTime()){
					nTap = (nTap + 1)%key.codes.length;
					getOnKeyboardActionListener().onKey(Constants.KEY_DELETE[0], Constants.KEY_DELETE);
				}else{
					nTap = 0;
				}
				lastTapTime = me.getEventTime();
				
				getOnKeyboardActionListener().onKey(key.codes[nTap], key.codes);
				lastKey = key;
				return true;
		}
		
		float ratio = absX > absY ? absX/absY : absY/absX; 
		if( ratio < 2 || (absX < minDistance && absY < minDistance)){
			return false;
		}
		
		lastKey = key;
		
		int direction = absX > absY ? (distanceX > 0 ? RIGHT : LEFT) : (distanceY > 0 ? DOWN : UP);
		
		if(direction > 0){
			return onFlick(key, direction, swipe);
		}
		
		return false;
		
	}


	protected  void onFlicking(GenericKey key, int direction, boolean isSwipe){

	}

	protected boolean onFlick(GenericKey key, int direction){
        switch (direction) {
            case UP:
                if(configuration.isFlickUpShift() ){
                    int [] keycodes = {key.codes[0], Constants.KEYCODE_FLICK_UP};
                    getOnKeyboardActionListener().onKey(keycodes[0], keycodes);
                }else{
                    getOnKeyboardActionListener().onKey(key.codes[0], key.codes);
                }
                return true;
            default:
                getOnKeyboardActionListener().onKey(key.codes[0], key.codes);
                return true;

        }
    }
	
	protected boolean onFlick(GenericKey key, int direction, boolean isSwipe) {
		boolean processed = false;
		if (isSwipe) {
			switch (direction) {
			case UP:
				swipeUp();
				break;

			case DOWN:
				swipeDown();
				break;

			case LEFT:
				swipeLeft();
				break;

			case RIGHT:
				swipeRight();
				break;
			}

		} else {
            onFlick(key, direction);


		}
		return true;
	}
	
	private void releaseKey(int delay) {
		if (keyIndex > Constants.OUT_OF_BOUNDS) {
			GenericKey key = (GenericKey) getKeyboard().getKeys().get(keyIndex);
			key.onReleased(false);
			invalidateKey(keyIndex);
		}

		keyIndex = mTouchX = mTouchY = Constants.OUT_OF_BOUNDS;
	}


}
