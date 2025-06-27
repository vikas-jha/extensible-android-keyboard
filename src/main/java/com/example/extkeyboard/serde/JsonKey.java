package com.example.extkeyboard.serde;

import com.example.extkeyboard.Util;
import com.example.extkeyboard.internal.Constants;


public class JsonKey {
	private String id;
	private String keyHints;
	private String popupCharacters;
    private boolean popupKeyboard;
	private String label;
	private String text;
	
	private String codes;
	
	private int doubleTapCode = Constants.KEYCODE_NONE;
	
	private boolean modifier;
	private boolean sticky;
	private int width = Constants.KEYCODE_NONE;
	private int height = Constants.KEYCODE_NONE;
	
	
	public String getId() {
		if(id.contains("0x")){
			String s = "";
			String[] str = id.split(",");
			for(int i = 0 ; i < str.length; i++){
				String string = str[i].trim();
				s += Util.parserInt(string);
			}
			id = s;
		}
		return id;
	}
	public String getKeyHints() {
		return keyHints;
	}
	public String getPopupCharacters() {
		return popupCharacters;
	}
	public String getLabel() {
		return label;
	}
	public String getText() {
		return text;
	}
	public int[] getCodes() {
		if(codes != null){
			String[] str = codes.split(",");
			int []keyCodes = new int[str.length];
			for(int i = 0 ; i < str.length; i++){
				String string = str[i].trim();
				keyCodes[i] = Util.parserInt(string);
			}
			return keyCodes;
		}
		return null;
	}
	public int getDoubleTapCode() {
		return doubleTapCode;
	}
	public boolean isModifier() {
		return modifier;
	}
	public boolean isSticky() {
		return sticky;
	}


    public boolean isPopupKeyboard() {
        return popupKeyboard;
    }
}
