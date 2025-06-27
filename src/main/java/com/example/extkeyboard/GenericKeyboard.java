/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.extkeyboard;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.IBinder;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import com.example.extkeyboard.internal.Constants;

public class GenericKeyboard extends Keyboard {

	private String name;

	private Key mSwitchKey;

	private Key mEnterKey;
	
	private Key mSpaceKey;
	
	private Key mShiftKey;

	private Keyboard mShiftedKeyBoard;
	
	private Keyboard baseKeyBoard;
	
	private String shiftedKeyBoardId;

    private String font;

	/**
	 * Stores the current state of the mode change key. Its width will be
	 * dynamically updated to match the region of {@link #mModeChangeKey} when
	 * {@link #mModeChangeKey} becomes invisible.
	 */
	private Key mModeChangeKey;
	/**
	 * Stores the current state of the language switch key (a.k.a. globe key).
	 * This should be visible while
	 * {@link InputMethodManager#shouldOfferSwitchingToNextInputMethod(IBinder)}
	 * returns true. When this key becomes invisible, its width will be shrunk
	 * to zero.
	 */
	private Key mLanguageSwitchKey;
	/**
	 * Stores the size and other information of {@link #mModeChangeKey} when
	 * {@link #mLanguageSwitchKey} is visible. This should be immutable and will
	 * be used only as a reference size when the visibility of
	 * {@link #mLanguageSwitchKey} is changed.
	 */
	private Key mSavedModeChangeKey;
	/**
	 * Stores the size and other information of {@link #mLanguageSwitchKey} when
	 * it is visible. This should be immutable and will be used only as a
	 * reference size when the visibility of {@link #mLanguageSwitchKey} is
	 * changed.
	 */
	private Key mSavedLanguageSwitchKey;
	
	private String language, symbol;
    private boolean customDraw = false;
	private EBoardTypes boardType;

	protected List<Row> rows;

	public GenericKeyboard(Context context, int xmlLayoutResId) {
		super(context, xmlLayoutResId);
		XmlResourceParser parser = context.getResources()
				.getXml(xmlLayoutResId);
		try {
			int eventType = parser.getEventType();
			while (eventType != XmlResourceParser.END_DOCUMENT) {
				if (eventType == XmlResourceParser.START_DOCUMENT) {
					
				} else if (eventType == XmlResourceParser.START_TAG) {
					if(parser.getName().equals("Keyboard")){
						language = parser.getAttributeValue(null, "lang");
						symbol = parser.getAttributeValue(null,   "symbol");
						String bt = parser.getAttributeValue(null,   "boardType");
						if(bt != null){
							boardType = EBoardTypes.valueOf(bt);
						}
                        customDraw = parser.getAttributeBooleanValue(null,  "customDraw", false);
						shiftedKeyBoardId = parser.getAttributeValue(null,   "shiftedKeyBoard");
                        font = parser.getAttributeValue(null,   "font");
					}
				} else if (eventType == XmlResourceParser.END_TAG) {
				} else if (eventType == XmlResourceParser.TEXT) {
				}
				eventType = parser.next();
			}
		} catch (XmlPullParserException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}
	
	public GenericKeyboard(Context context, int layoutTemplateResId,
			CharSequence characters, int columns, int horizontalPadding) {
		super(context, layoutTemplateResId, characters, columns,
				horizontalPadding);
	}

	@Override
	protected Key createKeyFromXml(Resources res, Row parent, int x, int y,
			XmlResourceParser parser) {
		Key key = new GenericKey(res, parent, x, y, parser);
		if (key.codes[0] == 10) {
			mEnterKey = key;
		} else if (key.codes[0] == ' ') {
			mSpaceKey = key;
		} else if (key.codes[0] == Constants.KEYCODE_BOARD_SWITCH) {
			mSwitchKey = key;
			mModeChangeKey = key;
			mSavedModeChangeKey = new GenericKey(res, parent, x, y, parser);
		} else if (key.codes[0] == Keyboard.KEYCODE_MODE_CHANGE) {
			mModeChangeKey = key;
			mSavedModeChangeKey = new GenericKey(res, parent, x, y, parser);
		} else if (key.codes[0] == Constants.KEYCODE_LANGUAGE_SWITCH) {
			mLanguageSwitchKey = key;
			mSavedLanguageSwitchKey = new GenericKey(res, parent, x, y, parser);
		}else if (key.codes[0] == Constants.KEYCODE_SHIFT) {
			mShiftKey = key;
		}
		return key;
	}

	/**
	 * Dynamically change the visibility of the language switch key (a.k.a.
	 * globe key).
	 * 
	 * @param visible
	 *            True if the language switch key should be visible.
	 */
	void setLanguageSwitchKeyVisibility(boolean visible) {
		if(mLanguageSwitchKey == null){
			return;
		}
		if (visible) {
			// The language switch key should be visible. Restore the size of
			// the mode change key
			// and language switch key using the saved layout.
			mModeChangeKey.width = mSavedModeChangeKey.width;
			mModeChangeKey.x = mSavedModeChangeKey.x;
			mLanguageSwitchKey.width = mSavedLanguageSwitchKey.width;
			mLanguageSwitchKey.icon = mSavedLanguageSwitchKey.icon;
			mLanguageSwitchKey.iconPreview = mSavedLanguageSwitchKey.iconPreview;
		} else {
			// The language switch key should be hidden. Change the width of the
			// mode change key
			// to fill the space of the language key so that the user will not
			// see any strange gap.
			mModeChangeKey.width = mSavedModeChangeKey.width
					+ mSavedLanguageSwitchKey.width;
			mLanguageSwitchKey.width = 0;
			mLanguageSwitchKey.icon = null;
			mLanguageSwitchKey.iconPreview = null;
		}
	}

	/**
	 * This looks at the ime options given by the current editor, to set the
	 * appropriate label on the keyboard's enter key (if it has one).
	 */
	void setImeOptions(Resources res, int options) {
		if (mEnterKey == null) {
			return;
		}

		switch (options
				& (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
		case EditorInfo.IME_ACTION_GO:
			mEnterKey.iconPreview = null;
			mEnterKey.icon = null;
			mEnterKey.label = res.getText(R.string.label_go_key);
			break;
		case EditorInfo.IME_ACTION_NEXT:
			mEnterKey.iconPreview = null;
			mEnterKey.icon = null;
			mEnterKey.label = res.getText(R.string.label_next_key);
			break;
		case EditorInfo.IME_ACTION_SEARCH:
			mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_search);
			mEnterKey.label = null;
			break;
		case EditorInfo.IME_ACTION_SEND:
			mEnterKey.iconPreview = null;
			mEnterKey.icon = null;
			mEnterKey.label = res.getText(R.string.label_send_key);
			break;
		default:
			mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_return);
			mEnterKey.label = null;
			break;
		}
	}

	void setSpaceIcon(final Drawable icon) {
		if (mSpaceKey != null) {
			mSpaceKey.icon = icon;
		}
	}

    public boolean isCustomDraw() {
        return customDraw;
    }

    public void setCustomDraw(boolean customDraw) {
        this.customDraw = customDraw;
    }

    public String getFont() {
        return font;
    }

    public static class GenericKey extends Keyboard.Key {

		public int[] keyCodes;
		public String[] keyHints;
		public int doubleTapCode = Constants.KEYCODE_NONE;

		public GenericKey(Resources res, Keyboard.Row parent, int x, int y,
				XmlResourceParser parser) {
			super(res, parent, x, y, parser);
			int size = this.codes.length;
			if(this.popupCharacters != null){
				size += this.popupCharacters.length();
			}
			keyCodes = new int[size];
			keyCodes[0] = this.codes[0];
			if (keyCodes.length >= 1 && this.popupCharacters != null && this.popupCharacters.length() > 0) {
				keyCodes[1] = this.popupCharacters.charAt(0);
			}
			try {
				if(parser.getEventType() == XmlResourceParser.START_TAG){
					String suggestions = parser.getAttributeValue(null, "suggestions");
					if(suggestions != null){
						keyHints = suggestions.split(",");
					}
					doubleTapCode = parser.getAttributeIntValue(null, "doubleTapCode", Constants.KEYCODE_NONE);
				}
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public GenericKey(Row parent){
			super(parent);
		}

		/**
		 * Overriding this method so that we can reduce the target area for the
		 * key that closes the keyboard.
		 */
		@Override
		public boolean isInside(int x, int y) {
			return super.isInside(x, codes[0] == KEYCODE_CANCEL ? y - 10 : y);
		}
	}

	public Key getSwitchKey() {
		return mSwitchKey;
	}

	public Keyboard getShiftedKeyBoard() {
		return mShiftedKeyBoard;
	}

	public void setShiftedKeyBoard(Keyboard mShiftedKeyBoard) {
		this.mShiftedKeyBoard = mShiftedKeyBoard;
	}

	public String getLanguage() {
		return language;
	}
    public void setLanguage(String language)
    {
        this.language = language;
    }

	public String getSymbol() {
		return symbol;
	}

	public String getShiftedKeyBoardId() {
		return shiftedKeyBoardId;
	}

	public EBoardTypes getBoardType() {
		return boardType;
	}

    public void setBoardType(EBoardTypes boardType) {
        this.boardType = boardType;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Keyboard getBaseKeyBoard() {
		return baseKeyBoard;
	}

	public void setBaseKeyBoard(Keyboard baseKeyBoard) {
		this.baseKeyBoard = baseKeyBoard;
	}

	public Key getShiftKey() {
		return mShiftKey;
	}

}
