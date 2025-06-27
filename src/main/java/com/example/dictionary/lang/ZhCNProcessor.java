package com.example.dictionary.lang;

import com.example.extkeyboard.internal.Constants;

/**
 * Created by vijha on 10/9/2017.
 */

public class ZhCNProcessor extends LanguageProcessor {

    public ZhCNProcessor() {
        langauge = "zh-CN";
    }

    @Override
    public boolean isWordCharacter(int codePoint) {
        if(codePoint <= 0xff){
            return false;
        }
        return true;
    }
}
