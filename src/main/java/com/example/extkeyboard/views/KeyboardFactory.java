package com.example.extkeyboard.views;

import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.view.View;
import android.view.ViewGroup;

import com.example.extkeyboard.EBoardTypes;
import com.example.extkeyboard.EmojiKeyBoardView;
import com.example.extkeyboard.ExtensibleKeyboard;
import com.example.extkeyboard.GenericKeyboardView;
import com.example.extkeyboard.R;

/**
 * Created by vijha on 10/3/2017.
 */

public class KeyboardFactory {
    private static Context context;

    public static void init(Context context){
        KeyboardFactory.context = context;
    }

    public View getKeyBoardView(EBoardTypes boardType, KeyboardView.OnKeyboardActionListener l){
        View kbView = null;
        switch (boardType){
            case Kana:
                break;
            case Emoji:
                EmojiKeyBoardView emojiView = new EmojiKeyBoardView(context);
                emojiView.setOnKeyboardActionListener(l);
                kbView = emojiView;
                break;
            default:
            GenericKeyboardView genericInputView = (GenericKeyboardView)infalteLayout(R.layout.input, null);
            genericInputView.setOnKeyboardActionListener(l);
            genericInputView.setPreviewEnabled(true);

        }
        return kbView;
    }

    private View infalteLayout(int id, ViewGroup root){
        return ((ExtensibleKeyboard)context).getLayoutInflater().inflate(id, root);
    }

}
