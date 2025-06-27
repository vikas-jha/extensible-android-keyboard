package com.example.dictionary.lang;

public class EnUSProcessor extends LanguageProcessor{
	
	public EnUSProcessor() {
		langauge = "en-US";
	}

    @Override
    public String[] getLanguages() {
        return new String[]{langauge, "en"};
    }

    @Override
	public boolean isWordCharacter(int codePoint){
		if(codePoint < 0xFF && codePoint > 0){
			char c = (char) codePoint;
			if((c >= 'a' && c <= 'z')
					|| (c >= 'A' && c <= 'Z') || c == '\'') {
				return true;
			}
		}

		return false;
	}

}
