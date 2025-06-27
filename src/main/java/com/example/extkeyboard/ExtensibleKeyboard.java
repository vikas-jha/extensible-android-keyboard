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

import android.app.ActionBar.LayoutParams;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.inputmethod.InputMethodSubtype.InputMethodSubtypeBuilder;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextServicesManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.dictionary.DictionaryManager;
import com.example.dictionary.lang.LanguageProcessor;
import com.example.extkeyboard.GenericKeyboard.GenericKey;
import com.example.extkeyboard.SettingsView.OnPopCloseListener;
import com.example.extkeyboard.internal.Configuration;
import com.example.extkeyboard.internal.Constants;
import com.example.extkeyboard.internal.Literals;
import com.example.extkeyboard.views.PopupActivity;
import com.example.transliterator.TransliteratorManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;


public class ExtensibleKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener, SpellCheckerSession.SpellCheckerSessionListener {
    static final boolean DEBUG = true;

    private Map<Integer, Key> keyMap = new HashMap<>();
    private Map<String, PopupActivity> popups = new WeakHashMap<>();

    private Configuration configuration;

    static final boolean PROCESS_HARD_KEYS = false;

    private InputMethodManager inputMethodManager;
    private AudioManager am;
    private Vibrator vb;
    private LanguageProcessor lp;

    private DictionaryManager dm;
    private KeyBoardsManager km;
    private TransliteratorManager tlm;

    private GenericKeyboardView inputView, numericInputView, genericInputView, kanaInputView;
    private EmojiKeyBoardView emojiView;
    private CandidateView candidateView;
    private CompletionInfo[] mCompletions;

    private StringBuilder mComposing = new StringBuilder();
    private Literals literals = new Literals();

    private boolean predictionOn, predictable, completionOn;
    private boolean capsLock;
    private boolean emojiBoard = false, transliterating = false;
    private boolean bgProcessed = false;

    private long lastKeyTime;
    private int lastKeyCode;
    private long mMetaState;

    private SpellCheckerSession mScs;
    private List<String> mSuggestions;

    private ESuggestionTypes suggestionType;

    private ViewGroup container;
    private int /*oldSelStart, oldSelEnd, newSelStart, newSelEnd,*/ candidatesStart, candidatesEnd;
    private int wordStart = Constants.OUT_OF_BOUNDS;
    private boolean updatingCursor = false;

    private int currentKey;

    private Executor executor = new ScheduledThreadPoolExecutor(4);

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        //Check if configuration files are present i.e. first run
        Util.checkFirstRun(this);

        configuration = Configuration.load(this);

        KeyBoardsManager.init(this);
        tlm = TransliteratorManager.init(this);
        km = KeyBoardsManager.getInstance();
        am = (AudioManager) getSystemService(AUDIO_SERVICE);
        vb = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        suggestionType = ESuggestionTypes.controlKeys;

        inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        final TextServicesManager tsm = (TextServicesManager) getSystemService(
                Context.TEXT_SERVICES_MANAGER_SERVICE);
        mScs = tsm.newSpellCheckerSession(null, null, this, true);

        dm = new DictionaryManager(getApplicationContext());

    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override
    public void onInitializeInterface() {
        dismissPopups();
        km.loadKeyboards();
        emojiView = null;
    }


    private boolean dismissPopups() {
        for (PopupActivity popupWindow : popups.values()) {
            if (popupWindow.isShowing()) {
                popupWindow.dismiss();
                return true;
            }
        }
        return false;
    }

    private boolean sendBackToPpups() {
        for (PopupActivity popupWindow : popups.values()) {
            if (popupWindow.isShowing()) {
                popupWindow.onBack();
                return true;
            }
        }
        return false;
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override
    public View onCreateInputView() {

        if (container != null) {
            container.removeAllViews();
            ViewParent v = container.getParent();
            if(v instanceof  ViewGroup){
                ((ViewGroup) v).removeView(container);
            }
        }else{
            container = (ViewGroup) getLayoutInflater().inflate(
                    R.layout.keyboard, null);
        }


        bgProcessed = false;

        /*container.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if(!bgProcessed){
                    setBackground();
                    bgProcessed = true;
                }
            }
        });*/




        candidateView = new CandidateView(this);
        candidateView.setKeyboards(km.getKbProvider());
        candidateView.setService(this);

        genericInputView = (GenericKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        genericInputView.setOnKeyboardActionListener(this);
        genericInputView.setPreviewEnabled(true);
        return container;
    }

    private void setBackground(){


        /*BitmapDrawable bitmapDrawable = configuration.getBackgroundImage();
        if(bitmapDrawable != null && container.getHeight() > 0 && container.getWidth() > 0 ){

            int ep = 10;

            Bitmap bitmap = Bitmap.createBitmap(container.getWidth(), container.getHeight(), Bitmap.Config.ARGB_8888);
            int h = bitmapDrawable.getIntrinsicHeight(), w = bitmapDrawable.getIntrinsicWidth();
            float scale = (float)w/container.getWidth();

            if(h/scale < container.getHeight()){
                scale = (float) h/container.getHeight();
            }

            float height = container.getHeight() * scale;
            float width = container.getWidth() * scale;

            Rect src = new Rect((int)(w - width)/2 , (int)(h - height)/2, (int)(w + width)/2, (int)(h + height)/2),
                    trg = new Rect(0, 0, container.getWidth(), container.getHeight() );
            Canvas c = new Canvas(bitmap);
            c.drawBitmap(bitmapDrawable.getBitmap(), src, trg, new Paint());

            container.setBackground(new BitmapDrawable(getResources(), bitmap));

        }*/

    }

    private void showEmojiBoard() {
        commitTyped(getCurrentInputConnection());
        container.removeAllViews();
        if(emojiView == null){
            emojiView = new EmojiKeyBoardView(this);
            emojiView.setOnKeyboardActionListener(this);
        }
        container.addView(emojiView);
        updateComposingText();
        emojiBoard = true;
        emojiView.setNextKeyBoardSymbol(km.getCurrentProvider().getSymbol());
        bgProcessed = false;
    }

    private void setKeyboard(InputProvider nextProvider) {
        commitTyped(getCurrentInputConnection());
        GenericKeyboard nextKeyboard = km.getKeyBoard(nextProvider);
        /**/
        container.removeAllViews();
        if (container.getParent() == null) {

            setInputView(container);
        }
        emojiBoard = false;
        if (nextKeyboard.getBoardType() == EBoardTypes.Numeric) {
            if (numericInputView == null) {
                numericInputView = (GenericKeyboardView) getLayoutInflater().inflate(R.layout.input_numeric, null);
                numericInputView.setOnKeyboardActionListener(this);
                numericInputView.setPreviewEnabled(false);
            }
            inputView = numericInputView;
            container.addView(numericInputView);

        } else if (nextKeyboard.getBoardType() == EBoardTypes.Kana) {

            if (kanaInputView == null) {
                kanaInputView = (GenericKeyboardView) getLayoutInflater().inflate(R.layout.input_kana, null);
                kanaInputView.setOnKeyboardActionListener(this);
                //kanaInputView.setPreviewEnabled(false);
            }

            inputView = kanaInputView;
            container.addView(candidateView);
            container.addView(kanaInputView);
        } else {
            inputView = genericInputView;
            container.addView(candidateView);
            container.addView(genericInputView);
        }

        inputView.setShifted(false);

        if (nextProvider.getCurrentKbHandler().getTransliterator() != null) {
            tlm.setLangs(nextProvider.getCurrentKbHandler().getTransliterator());
            transliterating = true;
        } else {
            transliterating = false;
        }

        dm.getDictionary(nextProvider.getCurrentKbHandler().getLanguage());
        lp = dm.getLanguageProcessor(nextProvider.getCurrentKbHandler().getLanguage());


        if (nextKeyboard.getSwitchKey() != null) {
            final boolean shouldSupportLanguageSwitchKey =
                    inputMethodManager.shouldOfferSwitchingToNextInputMethod(getToken());
            nextKeyboard.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey);
        }


        nextKeyboard.setImeOptions(getResources(), getCurrentInputEditorInfo().imeOptions);

        if (nextKeyboard.getBoardType() != EBoardTypes.Numeric) {
            km.setCurrentKeyboard(nextProvider);
            if (candidateView != null) {
                candidateView.setCurrentBoardIndex(km.getCurrentBoardIndex());
            }
            switchSubType();
            updateComposingText();
            //updateCandidates();
        }

        keyMap.clear();
        for (Key key : nextKeyboard.getKeys()) {
            for (int code : key.codes) {
                keyMap.put(code, key);
            }
            if (key.codes[0] == Constants.KEYCODE_BOARD_SWITCH
                    && nextKeyboard.getSwitchKey() != null) {
                if (km.getNextBoard() != null) {
                    key.label = km.getNextProvider().getSymbol();
                }
            }
        }
        setSuggestions(null, false, false, ESuggestionTypes.controlKeys);
        suggestionType = ESuggestionTypes.controlKeys;
        inputView.setKeyboard(nextKeyboard);
        inputView.invalidateAllKeys();
        literals.clear();

    }


    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override
    public View onCreateCandidatesView() {
        return null;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.

        mComposing.setLength(0);

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }

        predictable = false;
        completionOn = false;
        mCompletions = null;

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).


                predictable = true;

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    predictable = false;
                    completionOn = false;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER
                        || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    predictable = false;
                }

                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    predictable = false;
                    completionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                predictable = true;
                completionOn = true;
                updateShiftKeyState(attribute);
        }

        predictionOn = predictable && configuration.isCompletionOn();

        if (predictionOn) {
            updateComposingText();
            updateCandidates();
        }

    }

    private InputProvider getKeyboardForInput(EditorInfo attribute) {

        InputProvider keyboard = km.getCurrentProvider();

        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {

            case InputType.TYPE_CLASS_NUMBER:
                keyboard = km.getNumericProvider();
                break;
            case InputType.TYPE_CLASS_DATETIME:
                keyboard = km.getCurrentProvider();
                break;

            case InputType.TYPE_CLASS_PHONE:
                keyboard = km.getKeypad();
                break;

            case InputType.TYPE_CLASS_TEXT:
            default:
                keyboard = km.getCurrentProvider();
        }

        return keyboard;
    }

    @Override
    public void requestHideSelf(int flags) {
        super.requestHideSelf(flags);
        dm.saveDictionaries();
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override
    public void onFinishInput() {
        super.onFinishInput();
        // Clear current composing text and candidates.
        if (mComposing != null) {
            mComposing.setLength(0);
            updateCandidates();
        }

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);
        completionOn = false;

        if (inputView != null) {
            inputView.closing();
        }
        dm.saveDictionaries();
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        InputProvider curKeyboard = getKeyboardForInput(attribute);
        setKeyboard(curKeyboard);
        if (inputView != null) {
            inputView.closing();
        }

        setBackground();

        //final InputMethodSubtype subtype = inputMethodManager.getCurrentInputMethodSubtype();
        //inputView.setSubtypeOnSpaceKey(subtype);
        updateComposingText();
        updateCandidates();
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        inputView.setSubtypeOnSpaceKey(subtype);
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                  int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        /*this.oldSelStart = oldSelStart;
        this.oldSelEnd = oldSelEnd;
        this.newSelStart = newSelStart;
        this.newSelEnd = newSelEnd;*/

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        /*if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }*/

        if (!updatingCursor) {
            this.candidatesStart = candidatesStart;
            this.candidatesEnd = candidatesEnd;
        }

        if (!updatingCursor && newSelStart == newSelEnd && currentKey <= -1 && newSelStart != oldSelStart) {
            updatingCursor = true;
            if (!(candidatesStart <= newSelEnd && candidatesEnd >= newSelEnd)) {
                getCurrentInputConnection().finishComposingText();
                //getCurrentInputConnection().setComposingRegion(newSelStart, newSelEnd);
                updateComposingText();

            }
        }
        updatingCursor = false;

    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (completionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false, ESuggestionTypes.controlKeys);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for (int i = 0; i < completions.length; i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true, ESuggestionTypes.autoCompletion);
        }
    }

    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() - 1);
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length() - 1);
            }
        }

        onKey(c, null);

        return true;
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (sendBackToPpups()) {
                    return true;
                }
                if (inputView != null) {
                    inputView.handleBack();
                }

                break;

            case KeyEvent.KEYCODE_DEL:

                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;
            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                /*
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState()&KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }*/
        }

        boolean result = super.onKeyDown(keyCode, event);

        return result;
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (predictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }


        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private synchronized void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            mComposing.setLength(0);
            inputConnection.finishComposingText();
            updateCandidates();
        }
    }

    private synchronized void commitComposing(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            String text = mComposing.toString();
            mComposing.setLength(0);
            inputConnection.commitText(text, 1);
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        GenericKeyboard curKeyboard = km.getCurrentKeyboard();
        if (curKeyboard != null
                && curKeyboard.getBoardType() != null && curKeyboard.getBoardType() == EBoardTypes.Latin) {
            if (attr != null
                    && inputView != null) {
                int caps = 0;
                EditorInfo ei = getCurrentInputEditorInfo();
                if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                    caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
                }
                inputView.setShifted(capsLock || caps != 0);
            }
        }

    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) {

        playAudioAndVibrate(primaryCode);

        currentKey = primaryCode;
        if (primaryCode < 0 || primaryCode == Constants.KEYCODE_ENTER) {
            handleControlKeys(primaryCode, keyCodes);
        } else if (keyCodes.length == 2 && keyCodes[0] == Constants.KEYCODE_LONG_PRESS
                && primaryCode == Constants.SYMBOL_EMOJI) {
            //if (primaryCode == Constants.SYMBOL_EMOJI) {
                showEmojiBoard();
            /*}else{
                handleCharacter(primaryCode, keyCodes);
            }*/
        } else {
            handleCharacter(primaryCode, keyCodes);
        }

        lastKeyTime = SystemClock.uptimeMillis();
        lastKeyCode = currentKey;
        currentKey = Constants.KEYCODE_NONE;

    }

    private void handleControlKeys(int primaryCode, int[] keyCodes) {
        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                handleBackspace();
                break;
            case Keyboard.KEYCODE_SHIFT:
                handleShift();
                break;
            case Keyboard.KEYCODE_CANCEL:
                handleClose();
                break;
            case Constants.KEYCODE_SETTINGS:
            case Constants.KEYCODE_OPTIONS:
                showSettingsDialog();
                break;
            case Constants.KEYCODE_LANGUAGE_SWITCH:
                handleLanguageSwitch();
                break;
            case Constants.KEYCODE_BOARD_SWITCH:
                if (km.getCurrentKeyboard().getBoardType() == EBoardTypes.Custom || emojiBoard) {
                    setKeyboard(km.getCurrentProvider());
                } else {
                    setKeyboard(km.getNextProvider());
                }
                break;
            case Constants.KEYCODE_EMOJI_BOARD:
                showEmojiBoard();
                break;
            case Keyboard.KEYCODE_MODE_CHANGE:
                if (inputView != null) {/*
		        Keyboard current = inputView.getKeyboard();
		        if (current == keyboards.get(0) || current == keyboards.get(0)) {
		            setKeyboard(curLangboard);
		        } else {
		            setKeyboard(keyboards.get(0));
		            curKeyboard.setShifted(false);
		        }*/
                }
                break;
            case Constants.KEYCODE_KB_OPTIONS_VIEW:
                setSuggestions(null, false, false, ESuggestionTypes.controlKeys);
                break;
            case Constants.KEYCODE_ENTER:
                sendKey(primaryCode);
                break;
            case Constants.KEYCODE_DOUBLE_SPACE:
                sendKey(46);
                break;

            default:
                //less -1000 are for suggestion key codes
                if (primaryCode < Constants.SUGGESTION_KEY_CODE) {
                    GenericKey key = (GenericKey) keyMap.get(primaryCode);
                    if (key.keyHints != null) {
                        setSuggestions(Arrays.asList(key.keyHints), true, false, ESuggestionTypes.keySuggestions);
                    }
                }
                break;
        }


    }

    public boolean handleBack() {
        for (PopupActivity popupActivity : popups.values()) {
            if (popupActivity.isShowing()) {
                popupActivity.onBack();
                return true;
            }
        }
        requestHideSelf(0);
        return true;

    }

    public void onText(CharSequence text) {

        playAudioAndVibrate(0);

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void playAudioAndVibrate(int keyCode) {
        /********************************************************
         * Key press audio and vibration
         *********************************************************/
        if (configuration.isVibrationOn()) {
            vb.vibrate(50);
        }
        if (configuration.isAudioOn()) {
            float volume = 1f;
            int audioType;

            switch (keyCode) {
                case Constants.KEYCODE_SPACE:
                    audioType = AudioManager.FX_KEYPRESS_SPACEBAR;
                    break;
                case Constants.KEYCODE_ENTER:
                    audioType = AudioManager.FX_KEYPRESS_RETURN;
                    break;
                case Constants.KEYCODE_DELETE:
                    audioType = AudioManager.FX_KEYPRESS_DELETE;
                    break;
                default:
                    audioType = AudioManager.FX_KEYPRESS_STANDARD;
                    break;
            }

            am.playSoundEffect(audioType, volume);

        }
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        Set<String> set = new LinkedHashSet<String>(16);

        if (predictionOn) {
            if (mComposing.length() > 0) {
                List<String> suggestions = dm.getSuggestions(mComposing.toString());
                if (mComposing.length() >= Constants.DICT_MIN_WORD_LENGTH
                        && !(suggestions.contains(mComposing.toString()) || suggestions.contains(mComposing.toString().toLowerCase()))
                        ) {
                    set.add("^" + mComposing.toString());
                }
                if (suggestions.size() > 0) {
                    suggestions = processSuggestions(suggestions, mComposing.toString(), Constants.DICT_MAX_SUGGESTIONS);
                    set.addAll(suggestions);
                }

            }
        }

        if (set.size() == 0) {
            setSuggestions(null, false, false, ESuggestionTypes.controlKeys);
        } else {
            List<String> list = new ArrayList<String>();
            list.addAll(set);
            setSuggestions(list, true, true, ESuggestionTypes.autoCompletion);
        }
    }

    private void updateCandidates(List<String> inputs) {
        Set<String> set = new LinkedHashSet<String>(16);
        Set<String> texts = new LinkedHashSet<>();
        texts.addAll(inputs);

        Set<String> predictions = new LinkedHashSet<>();

        if (predictionOn) {

            for (String text : texts) {
                if (!text.equals(mComposing.toString())) {
                    predictions.add(text);
                }
            }

            List<String> suggestions = dm.getSuggestions(inputs);

            predictions.addAll(suggestions);

            if (!hasInvalidCharacters(mComposing.toString())) {
                if (mComposing.length() >= Constants.DICT_MIN_WORD_LENGTH
                        && !(predictions.contains(mComposing.toString()) || predictions.contains(mComposing.toString().toLowerCase()))
                        ) {
                    set.add("^" + mComposing.toString());
                } else if (transliterating) {
                    set.add(mComposing.toString());
                }
            }


            set.addAll(predictions);
        }

        if (set.size() == 0) {
            setSuggestions(null, false, false, ESuggestionTypes.controlKeys);
        } else {
            List<String> list = new ArrayList<String>();
            list.addAll(set);
            setSuggestions(list, true, true, ESuggestionTypes.autoCompletion);
        }
    }

    private List<String> processSuggestions(List<String> suggestions, String composing, int max) {
        if (km.getCurrentKeyboard().getBoardType() == EBoardTypes.Latin) {

            List<String> processed = new ArrayList<>(max);
            for (String suggestion : suggestions) {
                if (capsLock) {
                    processed.add(suggestion.toUpperCase());
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < suggestion.length(); i++) {
                        if (i < composing.length() && Character.isUpperCase(composing.charAt(i))) {
                            sb.append(Character.toUpperCase(suggestion.charAt(i)));
                        } else {
                            sb.append(suggestion.charAt(i));
                        }
                    }
                    processed.add(sb.toString());
                }

                if (processed.size() >= max) {
                    return processed;
                }
            }
            return processed;
        } else {
            if (suggestions.size() > max) {
                return suggestions.subList(0, max);
            } else {
                return suggestions;
            }
        }
    }

    private boolean hasInvalidCharacters(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!isWordCharacter(str.codePointAt(i))) {
                return true;
            }
        }
        return false;
    }

    public void setMessage(String message) {
        this.suggestionType = ESuggestionTypes.setting;
        List<String> messages = new ArrayList<>();
        messages.add(message);
        mSuggestions = messages;
        if (candidateView != null) {
            candidateView.setSuggestions(mSuggestions, false,
                    false, suggestionType);
        }
    }

    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid, ESuggestionTypes suggestionType) {
        this.suggestionType = suggestionType;
        mSuggestions = suggestions;
        if (candidateView != null) {
            candidateView.setSuggestions(suggestions, completions,
                    typedWordValid, suggestionType);
        }
    }

    private void handleBackspace() {
        ExtractedText et = getCurrentInputConnection().getExtractedText(new ExtractedTextRequest(), 0);
        mComposing.setLength(0);
        getCurrentInputConnection().finishComposingText();

        if (transliterating) {
            int start = et.selectionStart - this.wordStart, end = et.selectionEnd - this.wordStart;
            if(et.selectionStart != et.selectionEnd){
                start = et.selectionStart;
                end = et.selectionEnd;
            }

            literals.remove(start - 1, end);

        }

        //handle non BMP characters

        if (et.selectionStart == et.selectionEnd) {

            String text = et.text.toString();
            int size = 1;
            if (et.selectionEnd > 1) {
                int codePoint = text.codePointAt(et.selectionEnd - 2);
                size = Character.charCount(codePoint);
                //Check for flag symbols
                if (size == 2 && et.selectionEnd >= 4) {
                    if (codePoint >= 0x0001F1E6 && codePoint <= 0x0001F1FF) {
                        int preCodePoint = text.codePointAt(et.selectionEnd - 4);
                        if (Character.charCount(preCodePoint) == 2 && preCodePoint >= 0x0001F1E6 && preCodePoint <= 0x0001F1FF) {
                            size = 4;
                        }
                    }
                }

            }


            getCurrentInputConnection().deleteSurroundingText(size, 0);
        } else {
            getCurrentInputConnection().setSelection(et.selectionEnd, et.selectionEnd);
            getCurrentInputConnection().deleteSurroundingText(et.selectionEnd - et.selectionStart, 0);
        }

        updateComposingText();

        updateShiftKeyState(getCurrentInputEditorInfo());
        if (suggestionType != ESuggestionTypes.keySuggestions) {
            updateCandidates();
        }

    }

    private synchronized void updateComposingText() {

        mComposing.setLength(0);
        literals.clear();

        EBoardTypes currentBoardType = km.getCurrentKeyBoardType();

        if (currentBoardType == null ||
                currentBoardType == EBoardTypes.Custom || currentBoardType == EBoardTypes.Symbol) {
            return;
        }
        if (!predictionOn || getCurrentInputConnection() == null) {
            return;
        }

        updatingCursor = true;

        ExtractedText et = getCurrentInputConnection().getExtractedText(new ExtractedTextRequest(), 0);

        if (et == null) {
            return;
        }

        CharSequence charSequence = et.text;

        if(et.selectionStart != et.selectionEnd){
            mComposing.append(charSequence.subSequence(et.selectionStart,et.selectionEnd));
            return;
        }

        charSequence = charSequence.subSequence(0, charSequence.length());
        String text = charSequence.toString();
        int pos = et.text.length() > et.selectionStart ? et.selectionStart : et.text.length();
        int start = pos, end = pos, i = 0;
        StringBuilder sb = new StringBuilder();
        for (i = pos - 1; i >= 0; i--) {
            if (isWordCharacter(text.charAt(i)) || (transliterating && isTransliteratingCharacter(text.charAt(i)))) {
                sb.insert(0, text.charAt(i));
            } else {
                break;
            }
        }
        start = i + 1;
        for (i = pos; i < text.length(); i++) {
            if (isWordCharacter(text.codePointAt(i))) {
                sb.append(text.charAt(i));
            } else {
                break;
            }
        }

        end = i;


        if (sb.length() > 0) {
            mComposing.append(sb.toString());
            literals.append(sb.toString());
            this.wordStart = start;
            this.candidatesStart = start;

            getCurrentInputConnection().setComposingRegion(start, end);
        } else {
            this.wordStart = Constants.OUT_OF_BOUNDS;
        }

        getCurrentInputConnection().setSelection(et.selectionStart, et.selectionEnd);

        updatingCursor = false;
        if (suggestionType != ESuggestionTypes.keySuggestions) {
            updateCandidates();
        }

    }

    private boolean isWordCharacter(int codePoint) {
        if (km.getCurrentKeyBoardType() == EBoardTypes.Custom) {
            return false;
        }
        LanguageProcessor lp = dm.getLanguageProcessor(km.getCurrentHandler().getLanguage());
        if (lp != null) {
            return lp.isWordCharacter(codePoint);
        }
        return false;
    }

    private boolean isTransliteratingCharacter(int codePoint) {
        if (km.getCurrentKeyBoardType() == EBoardTypes.Custom) {
            return false;
        }
        LanguageProcessor lp = dm.getLanguageProcessor(km.getCurrentHandler().getLanguage());
        if (lp != null && lp.isWordCharacter(codePoint)) {
            return true;
        }
        String source = km.getCurrentHandler().getTransliterator();
        if (source != null) {
            source = source.substring(source.indexOf('.') + 1, source.indexOf('='));
            lp = dm.getLanguageProcessor(source);
            if (lp != null) {
                return lp.isWordCharacter(codePoint, source);
            }
        }
        return false;
    }

    private void handleShift() {
        if (inputView == null) {
            return;
        }

        GenericKeyboard currentKeyboard = (GenericKeyboard) inputView.getKeyboard();
        if (currentKeyboard.getBoardType() != null && currentKeyboard.getBoardType() == EBoardTypes.Latin) {
            checkToggleCapsLock();
            inputView.setShifted(capsLock || !inputView.isShifted());
        } else {
            if (km.getCurrentHandler().getShiftedKb() != null || km.getCurrentHandler().getBaseProvider() != null) {
                if (km.getCurrentHandler().getShiftedKb() != null) {
                    km.getCurrentProvider().setCurrentKbProvider(km.getCurrentHandler().getShiftedKb());
                } else {
                    km.getCurrentProvider().setCurrentKbProvider(km.getCurrentHandler().getBaseProvider().getId());
                }
                setKeyboard(/*(GenericKeyboard) currentKeyboard.getShiftedKeyBoard()*/km.getCurrentProvider());
            } else if (currentKeyboard.getLanguage() != null) {

                if (currentKeyboard.isShifted()) {
                    currentKeyboard.setShifted(false);
                } else {
                    currentKeyboard.setShifted(true);
                }
                if (currentKeyboard.getBoardType() != EBoardTypes.Latin) {

                    for (Key key : currentKeyboard.getKeys()) {
                        if (key.label != null && key.popupCharacters != null &&
                                key.popupCharacters.length() >= 1
                                && key.popupCharacters.length() == Character.charCount(key.popupCharacters.toString().codePointAt(0))) {
                            if (key.popupCharacters.charAt(0) == (char) Constants.SYMBOL_EMOJI) {
                                continue;
                            }
                            CharSequence t = key.label;
                            key.label = key.popupCharacters;
                            key.popupCharacters = t;
                        }
                    }
                }
            }
        }

        inputView.invalidateAllKeys();
    }

    /*private void handleCharacterAsync(int primaryCode, int[] keyCodes) {

    }

    private class characterKeyHandler implements Runnable{

        private int key;


        @Override
        public void run() {

        }
    }*/

    private void handleCharacter(int primaryCode, int[] keyCodes) {

       /* executor.execute(new Runnable() {
            @Override
            public void run() {

            }
        });*/

        ExtractedText et = getCurrentInputConnection().getExtractedText(new ExtractedTextRequest(), 0);

        if (et.selectionEnd != et.selectionStart) {
            getCurrentInputConnection().setSelection(et.selectionEnd, et.selectionEnd);
            getCurrentInputConnection().deleteSurroundingText(et.selectionEnd - et.selectionStart, 0);
            updateComposingText();
        }

        if (inputView != null && isInputViewShown() && !emojiBoard) {
            int codes[];
            codes = processCharacterKey(primaryCode, keyCodes);
            if (codes[0] < 0) {
                getCurrentInputConnection().deleteSurroundingText(-codes[0], 0);
            }
            primaryCode = codes[1];
        }

        if (emojiBoard == true) {
            handleEmoji(primaryCode, keyCodes);
        } else if (transliterating) {
            if (configuration.isCompletionOn()) {
                handleTransliteration(primaryCode, keyCodes);
            } else {
                setMessage("Enable '\u2699 > Input > Suggest completion'");
            }

        } else {
            handleTyping(primaryCode, keyCodes);
        }
    }

    private void handleTyping(int primaryCode, int[] keyCodes) {
        {

            int charSize = Character.charCount(primaryCode);
            ExtractedText et = getCurrentInputConnection().getExtractedText(new ExtractedTextRequest(), 0);

            if (lp != null && et.selectionStart == et.selectionEnd) {
                if (predictionOn && this.candidatesStart > -1) {
                    int position = et.selectionEnd - this.candidatesStart;
                    int remaining = this.candidatesEnd - et.selectionEnd;
                    String expected = mComposing.toString().substring(0, position) + String.valueOf(Character.toChars(primaryCode));
                    String processed = lp.processKey(mComposing, primaryCode, position);
                    String modification = processed.substring(0, position);
                    if (!processed.equals(expected) /*&& !mComposing.substring(0, mComposing.length() - position).equals(modification)*/) {
                        mComposing.replace(0, position, modification);
                        //getCurrentInputConnection().commitText(mComposing, 1);
                        //updateComposingText();
                        et = getCurrentInputConnection().getExtractedText(new ExtractedTextRequest(), 0);
                        primaryCode = processed.codePointAt(processed.length() - 1);
                    }

                } else {
                    CharSequence text;
                    int position = et.selectionStart > 3 ? 3 : et.selectionStart;
                    text = et.text.subSequence(et.selectionStart - position, et.selectionStart);
                    String processed = lp.processKey(text, primaryCode, position);
                    primaryCode = processed.codePointAt(processed.length() - charSize);
                }
            }


            if (primaryCode == Constants.ZERO) {
                return;
            }

            if (!isWordCharacter(primaryCode)) {
                commitTyped(getCurrentInputConnection());
                literals.clear();
                //mLiterating.setLength(0);
            }

            EBoardTypes currentBoardType = km.getCurrentKeyBoardType();

            char[] c = Character.toChars(primaryCode);
            if (predictionOn &&
                    !(currentBoardType == EBoardTypes.Custom || currentBoardType == EBoardTypes.Symbol) && isWordCharacter(primaryCode)) {


                if (this.candidatesStart > -1) {
                    mComposing.insert(et.selectionStart - this.candidatesStart, c);
                } else {
                    mComposing.append(c);
                }
                getCurrentInputConnection().setComposingText(mComposing, 0);
                int newPosition = et.selectionStart + c.length;
                getCurrentInputConnection().setSelection(newPosition, newPosition);
                updateShiftKeyState(getCurrentInputEditorInfo());
                updateCandidates();
            } else {
                getCurrentInputConnection().commitText(
                        String.copyValueOf(c), c.length);
            }
        }

    }

    private void handleEmoji(int primaryCode, int[] keyCodes) {
        char[] c = Character.toChars(primaryCode);
        getCurrentInputConnection().commitText(
                String.copyValueOf(c), 0);

    }

    private void handleTransliteration(int primaryCode, int[] keyCodes) {

        ExtractedText et = getCurrentInputConnection().getExtractedText(new ExtractedTextRequest(), 0);
        if (this.wordStart == Constants.OUT_OF_BOUNDS) {
            this.wordStart = et.selectionStart;
        }

        //updateComposingText();
        //int candidateStart = this.candidatesStart >= 0 ? this.candidatesStart : et.selectionStart;
        String keyString = String.valueOf(Character.toChars(primaryCode));
        int position = et.selectionStart - this.wordStart;
        if (position <= literals.size()) {
            literals.insert(et.selectionStart - this.wordStart, primaryCode);
            //mLiterating.insert(et.selectionStart - this.wordStart, keyString);
        }

        String tl;
        /*if(this.wordStart < 0){
            int nChars = mComposing.length() > 3 ? 3 : mComposing.length();
            tl = tlm.getTransliterations(primaryCode, mComposing.substring(mComposing.length() - nChars));
            primaryCode = tl.codePointAt(tl.length() - 1);
            mComposing.append(tl);
        }else{*/

        CharSequence text = mComposing.subSequence(0, et.selectionStart - this.wordStart);
        String defaultText = text + keyString;
        String remainingText = mComposing.toString().substring(position);
        tl = tlm.getTransliterations(primaryCode, text);
        if (!tl.contentEquals(defaultText)) {
            int from = tl.length() - 1, to = defaultText.length() - 1;
            literals.replace(from, to, tl.codePointAt(tl.length() - 1));
        }


        mComposing.setLength(0);
        mComposing.append(tl);
        mComposing.append(remainingText);
        primaryCode = tl.codePointAt(tl.length() - 1);
        //}

        List<String> tls = tlm.getTransliterations(literals.getSource());

        /*if (tl.length() > 0 && !text.equals(modification)) {
            mComposing.replace(0, position, tl);
            //getCurrentInputConnection().setComposingText(mComposing, 0);
            //getCurrentInputConnection().commitText(mComposing,1);
            //int newPosition  = et.selectionStart - position + modification.length() + 1;
            //getCurrentInputConnection().setComposingRegion(candidateStart, newPosition);
            //
        }*/


        if (primaryCode == Constants.ZERO) {
            return;
        }

        if (!isTransliteratingCharacter(primaryCode)) {
            commitTyped(getCurrentInputConnection());
            //mLiterating.setLength(0);
            mComposing.setLength(0);
            this.wordStart = Constants.OUT_OF_BOUNDS;
            literals.clear();

            char[] c = Character.toChars(primaryCode);
            getCurrentInputConnection().commitText(
                    String.copyValueOf(c), c.length);

        } else {
            getCurrentInputConnection().setComposingText(mComposing, 0);
            int newPosition = et.selectionStart - position + tl.length();
            getCurrentInputConnection().setSelection(newPosition, newPosition);
            updateShiftKeyState(getCurrentInputEditorInfo());
            updateCandidates(tls);
        }
    }


    private int[] processCharacterKey(int primaryCode, int[] keyCodes) {
        int pre = 0, post = 0;

        boolean processShift = true;
        GenericKey key = (GenericKey) keyMap.get(primaryCode);
        if (key == null) {
            return new int[]{pre, primaryCode, post};
        }


        int charSize = 1;
        if (Character.isValidCodePoint(primaryCode)) {
            charSize = Character.charCount(primaryCode);
        }

        //check for double tap
        boolean doubleTap = isDoubleTap(primaryCode);

        if (primaryCode == Constants.KEYCODE_SPACE
                && configuration.isDoubleSpaceToFullStop()
                && key.doubleTapCode != Constants.KEYCODE_NONE
                && (doubleTap ||
                (isDoubleSapce()))) {
            pre = -1;
            //getCurrentInputConnection().deleteSurroundingText(1, 0);
            primaryCode = key.doubleTapCode;
            processShift = false;
        } else if (doubleTap &&
                key.doubleTapCode != Constants.KEYCODE_NONE) {
            if (primaryCode == Constants.KEYCODE_SPACE) {
                if (configuration.isDoubleSpaceToFullStop()) {
                    CharSequence txt = getCurrentInputConnection().getTextBeforeCursor(2, 0);
                    if (txt.length() < 2 || txt.toString().codePointAt(0) != Constants.KEYCODE_SPACE) {
                        pre = -1;
                        //getCurrentInputConnection().deleteSurroundingText(1, 0);
                        primaryCode = key.doubleTapCode;
                    }
                }
                processShift = false;
            } else if (!key.repeatable) {
                getCurrentInputConnection().deleteSurroundingText(1, 0);
                primaryCode = key.doubleTapCode;
                processShift = false;
            }
        }

        if (processShift) {
            boolean flickedUp = keyCodes.length == 2 && keyCodes[1] == Constants.KEYCODE_FLICK_UP
                    && configuration.isFlickUpShift();

            if (km.getCurrentKeyBoardType() == EBoardTypes.Latin || primaryCode <= 0xff) {
                if (inputView.isShifted() && !flickedUp || flickedUp && !inputView.isShifted()) {
                    primaryCode = Character.toUpperCase(primaryCode);
                }
            } else if (km.getCurrentKeyBoardType() == EBoardTypes.Kana) {
                //do nothing
            } else {

                if ((keyCodes.length > 1 && keyCodes[0] == Constants.KEYCODE_LONG_PRESS) || flickedUp) {
                    if (key != null && key.popupCharacters != null
                            && key.popupCharacters.length() == charSize) {
                        primaryCode = key.popupCharacters.toString().codePointAt(0);
                    }
                } else {
                    if (key != null && key.label != null
                            && key.label.length() == charSize) {
                        primaryCode = key.label.toString().codePointAt(0);
                    }

                }


            }
        }

        return new int[]{pre, primaryCode, post};
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        inputView.closing();
    }

    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    private void handleLanguageSwitch() {
        inputMethodManager.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */);

    }

    private void switchSubType() {
        String locale = km.getCurrentKeyboard().getLanguage();
        if (locale == null) {
            locale = "en_US";
        }
        InputMethodSubtype subtype = new InputMethodSubtypeBuilder().
                setSubtypeLocale(locale).setIsAuxiliary(false)
                .setOverridesImplicitlyEnabledSubtype(false).setIsAsciiCapable(locale.startsWith("en")).build();

        if(getInputMethodId() != null){
            inputMethodManager.setInputMethodAndSubtype(getToken(), getInputMethodId(), subtype);
        }
        if (km.getCurrentKeyboard().getLanguage() != null) {
            final TextServicesManager tsm = (TextServicesManager) getSystemService(
                    Context.TEXT_SERVICES_MANAGER_SERVICE);
            mScs = tsm.newSpellCheckerSession(null, null, this, true);
        }

    }

    private String getInputMethodId() {
        for (InputMethodInfo i : inputMethodManager.getEnabledInputMethodList()) {
            if (i.getPackageName().equals(getPackageName())) {
                return i.getId();
            }
        }
        return null;
    }

    private void checkToggleCapsLock() {
        if (isDoubleTap(Constants.KEYCODE_SHIFT)) {
            capsLock = true;
        } else if (capsLock) {
            capsLock = false;
        }
    }

    private boolean isDoubleSapce() {
        int primaryCode = Constants.KEYCODE_SPACE;
        GenericKey key = (GenericKey) keyMap.get(primaryCode);
        if (isDoubleTap(primaryCode) ||
                lastKeyCode == Constants.KEYCODE_AUTO_COMPLETE &&
                        lastKeyTime + configuration.getDoubleTapTime() * 2 > SystemClock.uptimeMillis()) {
            return true;
        }
        return false;
    }

    private boolean isDoubleTap(int primaryCode) {
        GenericKey key = (GenericKey) keyMap.get(primaryCode);
        if (key != null && !key.pressed
                && lastKeyCode == primaryCode
                && lastKeyTime + configuration.getDoubleTapTime() > SystemClock.uptimeMillis()) {
            return true;
        }
        return false;
    }


    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }

    public void pickSuggestionManually(int index) {
        playAudioAndVibrate(0);

        updatingCursor = true;
        ExtractedText et = getCurrentInputConnection().getExtractedText(new ExtractedTextRequest(), 0);
        if (suggestionType == ESuggestionTypes.setting) {
            showSettingsDialog();
        } else if (suggestionType == ESuggestionTypes.controlKeys) {
            if (index == 0) {
                showSettingsDialog();
            } else if (candidateView.getBoardSymbols().size() - 1 == index) {
                showHelp("");
            } else {
                if (km.getCurrentBoardIndex() != index - 1) {
                    km.setCurrentKeyboard(km.getKbProvider().get(index - 1));
                    GenericKeyboard keyboard = km.getCurrentKeyboard();
                    if (keyboard != null) {
                        setKeyboard(km.getCurrentProvider());
                    }
                }

            }
        } else if (suggestionType == ESuggestionTypes.keySuggestions) {
            String text = mSuggestions.get(index);
            if (predictionOn) {
                mComposing.append(text);
                getCurrentInputConnection().setComposingText(mComposing, text.length());
                if (!isWordCharacter(text.codePointAt(text.length() - 1))) {
                    getCurrentInputConnection().finishComposingText();
                    mComposing.setLength(0);

                }
            } else {
                getCurrentInputConnection().commitText(text, 1);
            }


        } else if (completionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (candidateView != null) {
                candidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {

            if (predictionOn && mSuggestions != null && index >= 0) {
                String text = mSuggestions.get(index).toString().replace("^", "");
                if (et.text.length() == this.candidatesEnd) {
                    text += " ";
                    lastKeyTime = SystemClock.uptimeMillis();
                    lastKeyCode = Constants.KEYCODE_AUTO_COMPLETE;
                }
                mComposing.replace(0, mComposing.length(), text);
                if (km.getCurrentKeyboard() != null && km.getCurrentHandler().getLanguage() != null) {
                    dm.insertWord(text, km.getCurrentHandler().getLanguage());
                }
                commitComposing(getCurrentInputConnection());
                //mLiterating.setLength(0);
                literals.clear();
                this.wordStart = Constants.OUT_OF_BOUNDS;
                this.candidatesStart = this.candidatesEnd = Constants.OUT_OF_BOUNDS;
                updateCandidates();
            }

        }

        updatingCursor = false;
    }

    public void swipeRight() {
        if (completionOn || predictionOn) {
            pickDefaultCandidate();
        }
    }

    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
        getCurrentInputConnection().finishComposingText();
        setSuggestions(null, false, false, ESuggestionTypes.controlKeys);
    }

    public void onPress(int primaryCode) {

    }

    public void onRelease(int primaryCode) {

    }

    /**
     * http://www.tutorialspoint.com/android/android_spelling_checker.htm
     *
     * @param results results
     */
    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < results.length; ++i) {
            // Returned suggestions are contained in SuggestionsInfo
            final int len = results[i].getSuggestionsCount();
            sb.append('\n');

            for (int j = 0; j < len; ++j) {
                sb.append("," + results[i].getSuggestionAt(j));
            }

            sb.append(" (" + len + ")");
        }
    }

    private void dumpSuggestionsInfoInternal(
            final List<String> sb, final SuggestionsInfo si, final int length, final int offset) {
        // Returned suggestions are contained in SuggestionsInfo
        final int len = si.getSuggestionsCount();
        for (int j = 0; j < len; ++j) {
            sb.add(si.getSuggestionAt(j));
        }
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        final List<String> sb = new ArrayList<>();
        for (int i = 0; i < results.length; ++i) {
            final SentenceSuggestionsInfo ssi = results[i];
            for (int j = 0; j < ssi.getSuggestionsCount(); ++j) {
                dumpSuggestionsInfoInternal(
                        sb, ssi.getSuggestionsInfoAt(j), ssi.getOffsetAt(j), ssi.getLengthAt(j));
            }
        }
        setSuggestions(sb, true, true, ESuggestionTypes.controlKeys);
    }

    /*@Override
    public void onComputeInsets(InputMethodService.Insets outInsets) {
        super.onComputeInsets(outInsets);
        if (!isFullscreenMode()) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets;
        }
    }*/


    private void showSettingsDialog() {

        PopupActivity settingsPopup = popups.get(Constants.POPUP_NAME_SETTINGS);
        if (settingsPopup == null) {
            settingsPopup = new SettingsView(this);
            popups.put(Constants.POPUP_NAME_SETTINGS, settingsPopup);
        }

        getCurrentInputConnection().finishComposingText();

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);


        settingsPopup.showAtLocation(container, Gravity.NO_GRAVITY, 0, 0);
        ((SettingsView) settingsPopup)
                .setOnPopCloseListener(new OnPopCloseListener() {

                    @Override
                    public void onPopupClose() {
                        //container.setMinimumHeight(0);
                        if (genericInputView != null) {
                            genericInputView.setBackgroundColor(configuration.getKeyboardBgColor());
                            genericInputView.invalidate();
                            genericInputView.setKeyTextColor(configuration.getKeyTextColor());
                            genericInputView.invalidateAllKeys();
                        }

                        if (kanaInputView != null) {
                            kanaInputView.setBackgroundColor(configuration.getKeyboardBgColor());
                            kanaInputView.invalidate();
                            kanaInputView.setKeyTextColor(configuration.getKeyTextColor());
                            kanaInputView.invalidateAllKeys();
                        }
                        if (numericInputView != null) {
                            numericInputView.setBackgroundColor(configuration.getKeyboardBgColor());
                            numericInputView.invalidate();
                            numericInputView.setKeyTextColor(configuration.getKeyTextColor());
                            numericInputView.invalidateAllKeys();

                        }

                        candidateView.setBackgroundColor(configuration.getKeyBoardHeaderColor());

                        predictionOn = predictable && configuration.isCompletionOn();
                        km.loadKeyboards();
                        candidateView.setKeyboards(km.getKbProvider());
                        setKeyboard(km.getCurrentProvider());
                        container.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showHelp(String id) {
		/*WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);*/

        PopupActivity helpPopup = popups.get(Constants.POPUP_NAME_HELP);
        if (helpPopup == null) {
            helpPopup = new HelpView(this);
            popups.put(Constants.POPUP_NAME_HELP, helpPopup);
        }
        helpPopup.showAtLocation(container, Gravity.NO_GRAVITY, 0, 0);
    }

    public void onHeaderDoubleTap(int index) {
        if (suggestionType == ESuggestionTypes.controlKeys) {
            final InputProvider ip = km.getCurrentProvider();
            final List<KeyboardHandler> handlers = new ArrayList<>();//ip.getKbProviders();
            for(KeyboardHandler kbh : ip.getKbProviders() ){
                if(!kbh.isHidden()){
                    handlers.add(kbh);
                }
            }

            PopupActivity methodSelectorPopup = popups.get(Constants.POPUP_NAME_INPUT_SELECTOR);
            if (methodSelectorPopup == null) {
                methodSelectorPopup = new PopupActivity(this);
                popups.put(Constants.POPUP_NAME_INPUT_SELECTOR, methodSelectorPopup);
            }

            methodSelectorPopup.setOutsideTouchable(true);
            methodSelectorPopup.setBackgroundDrawable(new BitmapDrawable());

            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);

            LinearLayout containerLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.empty_list, null);
            methodSelectorPopup.setContentView(containerLayout);

            LinearLayout layout = (LinearLayout) containerLayout.findViewById(R.id.layout_container);
            layout.removeAllViews();


            methodSelectorPopup.setWidth((int) (metrics.widthPixels * 0.99));
            methodSelectorPopup.setHeight(metrics.heightPixels);

            int fontSize = getResources().getDimensionPixelSize(R.dimen.kbs_default_font_size);

            TextView header = new TextView(this);
            header.setText("Select input method");

            header.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            int padding = getResources().getDimensionPixelSize(R.dimen.kbs_switch_padding);
            header.setPadding(padding, padding, padding, padding);
            header.setTextColor(getResources().getColor(R.color.teal));
            header.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);

            layout.addView(header);


            final RadioGroup radioGroup = new RadioGroup(this);
            layout.addView(radioGroup);

            final PopupWindow popupWindow = methodSelectorPopup;
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = radioGroup.indexOfChild(v);
                    ip.setCurrentHandler(handlers.get(index));
                    setKeyboard(ip);
                    popupWindow.dismiss();
                }
            };


            for (KeyboardHandler handler : handlers) {
                //AppCompatRadioButton tv = new AppCompatRadioButton(this);
                RadioButton tv = new RadioButton(this);
                tv.setText(handler.getName());

                tv.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                tv.setPadding(padding, padding, padding, padding);
                tv.setBackground(getResources().getDrawable(R.drawable.settings_border));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);

                if (ip.getCurrentKbHandler() == handler) {
                    tv.setChecked(true);
                }
                if(!tv.getPaint().hasGlyph(handler.getName()) && handler.getFont() != null){
                    tv.setTypeface(configuration.getFont(handler.getFont()));
                }

                if(Util.isRTL(handler.getName())){
                    tv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                }

                tv.setOnClickListener(clickListener);
                radioGroup.addView(tv);

            }

            methodSelectorPopup.showAtLocation(container, Gravity.NO_GRAVITY, 0, (int) (-metrics.heightPixels * 0.9 + container.getHeight()));

        }
    }

}
