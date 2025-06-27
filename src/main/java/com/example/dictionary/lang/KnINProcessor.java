package com.example.dictionary.lang;

public class KnINProcessor extends LanguageProcessor {

	
	public KnINProcessor() {
		langauge = "kn-IN";
	}
	
	@Override
	public boolean isWordCharacter(int codePoint) {
		if((codePoint >= 0x0C80 && codePoint <= 0x0CE3)){
			return true;
		}
		return false;
	}

}
