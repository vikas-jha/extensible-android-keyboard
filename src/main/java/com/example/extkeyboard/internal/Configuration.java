package com.example.extkeyboard.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;

import com.example.extkeyboard.ExtensibleKeyboard;
import com.example.extkeyboard.InputProvider;
import com.example.extkeyboard.R;
import com.example.extkeyboard.Util;
import com.example.extkeyboard.serde.JSON;
import com.example.extkeyboard.serde.JsonSettings;

public class Configuration {
	private static Configuration instance = new Configuration();
	private Context context;
	private InputProvider[] providers;
	private InputProvider[] hiddenProviders;

    private static JsonSettings settings;

    private Float deviceDensity;
    private Integer statusBarHeight;
	
	
	private HashMap<String, Typeface> fontMap = new HashMap<>();

	public static synchronized Configuration load(Context context){
		instance.context = context;
		try {
			instance.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return instance;
	}
	
	public static Configuration getInstance(){
		return instance;
	}
	private void load() throws IOException{

        String metadata;
        metadata = Util.readStream(context.openFileInput(Constants.STR_SETTINGS_FILENAME));
        settings =  new JSON<>(metadata, JsonSettings.class).toObject();

		metadata = Util.readStream(context.getAssets().open("defaults/input_providers.json"));
		providers =  new JSON<>(metadata, InputProvider[].class).toObject();
		
		metadata = Util.readStream(context.getAssets().open("defaults/system_providers.json"));
		hiddenProviders =  new JSON<>(metadata, InputProvider[].class).toObject();

        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
	}

    public boolean isCompletionOn(){
        return getSettings().getSubSetting("input").getSubSetting("completion_on").getOn();
    }

	public boolean isPredictionOn(){
       return getSettings().getSubSetting("input").getSubSetting("prediction_on").getOn();
	}

	public boolean isVibrationOn(){
        return getSettings().getSubSetting("input").getSubSetting("vibration_on").getOn();
	}
	
	public boolean isAudioOn(){
        return getSettings().getSubSetting("input").getSubSetting("audio_on").getOn();
	}
	
	public boolean isDoubleSpaceToFullStop()
    {
        return getSettings().getSubSetting("input").getSubSetting("double_space_period").getOn();
	}
	
	public boolean isFlickUpShift(){

        return getSettings().getSubSetting("input").getSubSetting("flick_up_shift").getOn();
	}

	public boolean isPreviewKeyPress(){
        return getSettings().getSubSetting("input").getSubSetting("preview_key_press").getOn();
	}
	
	public Typeface getSymbolFont(){
		return getFont("NotoEmoji-Regular.ttf");
	}
	
	public Typeface getFont(String name){
		Typeface font = fontMap.get(name);
		if( font == null && context != null){
			font = Typeface.createFromAsset(context.getAssets(), "fonts/" + name);
			fontMap.put(name, font);
		}
		return font;
	}
	
	public long getDoubleTapTime(){
		return 500;
	}

	public long getLongPressTime(){
		return 500;
	}
	
	public int getDisplayWidth(){
		return ((ExtensibleKeyboard) context).getMaxWidth();
	}
	
    private int getColorValue(String hexColor, int defaultColor){
        try{
            long longCode = Util.parserLong(hexColor);
            int alpha = (int) ((longCode & 0xff000000 ) >> 24);
            int rgb = (int)(longCode & 0x00ffffff );
            int color = (alpha << 24) | rgb ;
            if(hexColor.length()  < 10){
                color = color | 0xff000000;
            }
            return color;
        }catch (Exception e){
            e.printStackTrace();
            return defaultColor;
        }

    }
	
	
	
	/*
	 * Color configurations
	 */
	public int getKeyTextColor(){
        String value = getSettings().getSubSetting("display").getSubSetting("kbs_color_key_txt").getValue();
        return (getColorValue(value, Constants.COLOR_DARK_GREY) | 0xff000000);
	}

	public int getKeyHintTextColor(){
		return 0x88000000 | (0x00ffffff & getKeyTextColor());
	}
	
	public int getKeyboardBgColor(){
        String value = getSettings().getSubSetting("display").getSubSetting("kbs_color_kb_bg").getValue();
        return getColorValue(value, Constants.COLOR_LIGHT_GREY);
	}


	
	public int getKeyBoardHeaderColor(){
        String value = getSettings().getSubSetting("display").getSubSetting("kbs_color_kb_head").getValue();
		return getColorValue(value, Constants.COLOR_WHITE_GREY);
	}
	
	public int getTouchBgColor(){

        return 0x22000000;
	}
	
	public int getControlColor(){
		return context.getResources().getColor(R.color.teal);
	}
	
	public int getControlBgColor(){
		return Constants.COLOR_GREY;
	}
	
	public int getKeyHintTextSize(){
		return 12;
	}

	public InputProvider[] getProviders() {
		return providers;
	}

	public InputProvider[] getHiddenProviders() {
		return hiddenProviders;
	}

	public JsonSettings getSettings(){
        return settings;
    }

    public float getDeviceDensity(){
        if(deviceDensity == null){
            deviceDensity = context.getResources().getDisplayMetrics().density;
        }
        return  deviceDensity;

    }

    public Integer getStatusBarHeight() {
        return statusBarHeight;
    }

    public BitmapDrawable getBackgroundImage(){
        BitmapDrawable bitmapDrawable = (BitmapDrawable) context.getDrawable(R.drawable.sunrise);
        return bitmapDrawable;
    }
}
