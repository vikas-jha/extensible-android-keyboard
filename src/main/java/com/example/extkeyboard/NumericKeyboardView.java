package com.example.extkeyboard;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;

import com.example.extkeyboard.internal.Configuration;
import com.example.extkeyboard.internal.Constants;

public class NumericKeyboardView extends GenericKeyboardView {
	

	public NumericKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
	}
	
	public NumericKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        configuration = Configuration.getInstance();
    }
	
	@Override
    public void onDraw(Canvas canvas){
		
		List<Key> keys = getKeyboard().getKeys();
		
		if(((GenericKeyboard)getKeyboard()).getSymbol().equals("keypad")
    			/*((GenericKeyboard)getKeyboard()).getBoardType() == EBoardTypes.Numeric*/){
			paint.setColor(configuration.getKeyTextColor());
    		for(Key key: keys) {
    			drawKey(key, canvas, paint);
    		}
		}else{
			super.onDraw(canvas);
		}
		
		paint.setTextSize(12 * deviceDensity);
		paint.setColor(configuration.getKeyHintTextColor());
		paint.setTextAlign(Paint.Align.LEFT);
		
		drawKeyBoardFeatures(canvas, paint);
		
		
	}
	
	
	protected void drawKey(Key key, Canvas canvas, Paint paint) {
		if(key.pressed){
			int previousColor = paint.getColor();
			paint.setColor(configuration.getTouchBgColor());
			canvas.drawRoundRect(key.x, key.y, key.x + key.width, key.y + key.height, 10, 10, paint);
			paint.setColor(previousColor);
		}
		
		if(key.label != null){
			int primaryCode = 0;
			if(key.codes.length > 0){
				primaryCode = key.codes[0];
			}
			if(primaryCode == Constants.KEYCODE_ENTER){
				paint.setTextSize(15 * deviceDensity);
			}else{
				paint.setTextSize(25 * deviceDensity);
				
			}
			
			paint.setFakeBoldText(true);
			int x = key.x + key.width/4;
			if(key.edgeFlags == Keyboard.EDGE_RIGHT ){
				int textWidth = (int) paint.measureText(key.label.toString());
				x = key.x + (key.width - textWidth)/2;
				paint.setFakeBoldText(false);
			}
			canvas.drawText(key.label.toString(), x, key.y + key.height/2 + 10*deviceDensity, paint);
		}
		if(key.icon != null){
			int x = key.x + (key.width - key.icon.getIntrinsicWidth())/2;
			int y = key.y + (key.height - key.icon.getIntrinsicHeight())/2;
			key.icon.setBounds(x, y, x + key.icon.getIntrinsicWidth(), y + key.icon.getIntrinsicHeight());
			key.icon.draw(canvas);
		}

	}
	
	protected void drawKeyFeatures(Key key, Canvas canvas, Paint paint) {

		if (key.popupCharacters != null) {

			String extraText = key.popupCharacters.toString();

			if (extraText.length() > 4) {
				extraText = extraText.substring(0, 3);
			}

			canvas.drawText(extraText, key.x + key.width / 2, key.y
					+ key.height / 2 + 8 * deviceDensity, paint);

		}

	}

}
