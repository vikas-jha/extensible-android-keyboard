package com.example.extkeyboard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.example.extkeyboard.internal.Constants;

import android.content.Context;

public class Util {
	public static void copy(InputStream is, OutputStream os, boolean close){
		if(os == null || is == null){
			return;
		}
		byte[] buffer = new byte[1024 * 10];
		int length = 0;
		try {
			while( (length = is.read(buffer)) > 0){
				os.write(buffer, 0 ,length);
			}
			
			if(close){
				is.close();
				os.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static String readStream(InputStream is) throws IOException{
		StringBuilder sb = new StringBuilder();
		byte[] buffer = new byte [10 * 1024];
   		int length = 0;
   		while((length = is.read(buffer))> 0){
   			sb.append(new String(buffer, 0, length));
   		}
   		return sb.toString();
	}
	
	public static boolean checkFirstRun(Context context){
    	try {
    		InputStream is = context.openFileInput(Constants.STR_SETTINGS_FILENAME);
    		is.close();
    		return false;
    	} catch (IOException e) {
    		try {
				Util.copy(context.getResources().getAssets().open(Constants.STR_SETTINGS_ASSET), 
						context.openFileOutput(Constants.STR_SETTINGS_FILENAME, Context.MODE_PRIVATE), true);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return true;
    	} 
    	
    }
	
	public static boolean isRTL(String symbol) {
		int codePoint = symbol.codePointAt(0);
		if((codePoint >= 0x0600 && codePoint <= 0x06FF)//Arabic
                || (codePoint >= 0x0590 && codePoint <= 0x05FF)//Hebrew /**Should merge them? */

                ){
			return true;
		}
		return false;
	}
	
	public static int parserInt(String string){
		if(string.startsWith("0x")){
			return Integer.parseInt(string.substring(2), 16);
		}else{
			return Integer.parseInt(string);
		}
	}

    public static long parserLong(String string){
        if(string.startsWith("0x")){
            return Long.parseLong(string.substring(2), 16);
        }else{
            return Long.parseLong(string);
        }
    }

    public static int getColorValue(String hexColor){
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
        }
        return  0;
    }
}
