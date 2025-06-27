package com.example.dictionary.lang;

import java.util.HashMap;
import java.util.Map;


public class KoKRProcessor extends LanguageProcessor{
	
	private static Map<Long, Long> intialToFinalMap = new HashMap<>();
	static{
		mapChars(0x1100, 0x11A8);
		mapChars(0x1101, 0x11A9);
		mapChars(0x1102, 0x11AB);
		mapChars(0x1103, 0x11AE);
		mapChars(0x1105, 0x11AF);
		mapChars(0x1106, 0x11B7);
		mapChars(0x1107, 0x11B8);
		mapChars(0x1109, 0x11BA);
		mapChars(0x110B, 0x11BC);
		mapChars(0x110C, 0x11BD);
		mapChars(0x110E, 0x11BE);
		mapChars(0x110F, 0x11BF);
		mapChars(0x1110, 0x11C0);
		mapChars(0x1111, 0x11C1);
		mapChars(0x1112, 0x11C2);
	}
	
	private static void mapChars(long _initial, long _final){
		intialToFinalMap.put(_initial, _final);
	}
	
	public KoKRProcessor() {
		langauge = "ko-KR";
	}
	
	@Override
	public boolean isWordCharacter(int codePoint){
		if(codePoint >= 0x1100 && codePoint <= 0x11FF){
				return true;
		}
		return false;
	}
	
	@Override
	public String processKey(CharSequence csq, int keyCode, int position){
		String text = csq.toString();
    		if(text.length() >= 2 && position >= 2 &&
    				keyCode >= 0x1100 && keyCode <= 0x1112){
    			int previousCode = text.codePointAt(position - 1);
    			if(previousCode >= 0x1161 && previousCode <= 0x1175){
    				Long mappedCode = intialToFinalMap.get(Long.valueOf(keyCode));
    				if(mappedCode != null){
    					keyCode = mappedCode.intValue();
    				}
    			}
    		}
    		return csq + String.valueOf(Character.toChars(keyCode));
	}

}
