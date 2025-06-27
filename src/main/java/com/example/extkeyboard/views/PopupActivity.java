package com.example.extkeyboard.views;

import android.content.Context;
import android.widget.PopupWindow;

/**
 * Created by vijha on 10/2/2017.
 */

public class PopupActivity extends PopupWindow{
    public PopupActivity(Context context){
        super(context);
    }

    public boolean onBack(){
        this.dismiss();
        return true;
    }
}
