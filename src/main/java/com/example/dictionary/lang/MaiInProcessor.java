package com.example.dictionary.lang;

/**
 * Created by vijha on 10/17/2017.
 */

public class MaiInProcessor extends LanguageProcessor {

    public MaiInProcessor() {
        langauge = "mai-IN";
    }

    @Override
    public boolean isWordCharacter(int codePoint) {
        if((codePoint >= 0x0900 && codePoint <= 0x0963)
                || codePoint == 0x0970 || codePoint == 46){
            return true;
        }
        return false;
    }
}
