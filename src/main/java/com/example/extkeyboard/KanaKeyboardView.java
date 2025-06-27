package com.example.extkeyboard;

import java.lang.reflect.Field;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.example.extkeyboard.GenericKeyboard.GenericKey;

public class KanaKeyboardView extends GenericKeyboardView {
	
	public KanaKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public KanaKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void onDraw(Canvas canvas) {

		List<Key> keys = getKeyboard().getKeys();
		super.onDraw(canvas);
		paint.setStrokeWidth(4);
		paint.setColor( configuration.getKeyHintTextColor());
		paint.setTextSize(configuration.getKeyHintTextSize() * deviceDensity);
		
		for(int i = 0; i < keys.size();i ++){
			GenericKey key = (GenericKey) keys.get(i);
			if(key.keyHints != null){
				drawKeyHints(key, canvas, paint);
			}
		}
	}
	
	private void drawKeyHints(GenericKey key, Canvas canvas, Paint paint){
		for(int i = 1; i <= key.keyHints.length ;i++){
			String text = key.keyHints[i - 1];
			if(text.equals("0")){
				continue;
			}
			int vc = (int) (5 * deviceDensity);
			int textWidth = (int) paint.measureText(text);
			switch (i) {
			case UP:
				canvas.drawText(text, key.x + (key.width - textWidth)/2 , key.y + key.height/4, paint);
				break;
			case RIGHT:
				canvas.drawText(text, key.x + key.width*3/4 - textWidth/2 , key.y + key.height/2 + vc, paint);
				break;
			case DOWN:
				canvas.drawText(text, key.x + (key.width - textWidth)/2 , key.y + key.height *3/4 + vc*2, paint);
				break;
			case LEFT:
				canvas.drawText(text, key.x + key.width/4 - textWidth/2 , key.y + key.height/2 + vc, paint);

			default:
				break;
			}
		}
	}


    @Override
    protected void onFlicking(GenericKey key, int direction, boolean isSwipe) {
        if(configuration.isPreviewKeyPress()){
            if(direction == 0){
                if(!keyPopup.isShowing()){
                    keyPopup.show(this, key);
                }
                keyPopup.setText(String.valueOf(Character.toChars(key.codes[0])));

            }else if(key.keyHints != null && key.keyHints.length >= direction){
                String hint = key.keyHints[direction - 1];
                if(hint.equals("0")){
                    keyPopup.dismiss();
                }else{
                    if(!keyPopup.isShowing()){
                        keyPopup.show(this, key);
                    }
                    keyPopup.setText(hint);
                }
            }
        }

    }

    protected boolean onFlick(GenericKey key, int direction){
		if(key.keyHints != null && key.keyHints.length >= direction){
			 String hint = key.keyHints[direction - 1];
			 if(!hint.equals("0")){
				 getOnKeyboardActionListener().onKey(hint.codePointAt(0), key.codes);
				 return true;
			 }
		 }
		return false;
	}
	
			
}
