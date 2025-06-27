package com.example.dictionary.lang;

import java.util.ArrayList;
import java.util.List;

import com.example.extkeyboard.internal.Constants;

public abstract class LanguageProcessor {
	
	protected static String langauge;

    public String[] getLanguages(){
        return new String[]{langauge};
    }

	public abstract boolean isWordCharacter(int codePoint);

    public boolean isWordCharacter(int codePoint, String lang){
        return isWordCharacter(codePoint);
    }
	
	public String processKey(CharSequence csq, int primayCode, int position){
		return csq + String.valueOf(Character.toChars(primayCode));
	}
	
	public List<String> processText(CharSequence csq, int primayCode, int position){
		List<String> texts = new ArrayList<>();
		 texts.add(processKey(csq, primayCode, position));
		return texts;
	}

}
