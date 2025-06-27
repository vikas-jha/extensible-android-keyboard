package com.example.dictionary.lang;

public class JaJPProcessor extends LanguageProcessor{
	
	public JaJPProcessor() {
		langauge = "ja_JP";
	}

    @Override
    public String[] getLanguages() {
        return new String[]{langauge, "ja", "ja_JP#Hiragana", "ja_JP#Katakana", "ja_JP#Kanji"};
    }

    @Override
	public boolean isWordCharacter(int codePoint) {
		if (codePoint >= 0x3040 && codePoint <= 0x309F) {
            //Hiragana characters
			return true;
		}if (codePoint >= 0x30A0 && codePoint <= 0x30FF) {
            //Katakana characters
            return true;
        }else /*Condition for Kanji characters*/{

        }
		return false;
	}

}
