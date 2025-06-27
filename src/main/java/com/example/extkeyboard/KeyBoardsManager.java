package com.example.extkeyboard;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.inputmethodservice.Keyboard.Key;

import com.example.extkeyboard.GenericKeyboard.GenericKey;
import com.example.extkeyboard.internal.Configuration;
import com.example.extkeyboard.internal.Constants;
import com.example.extkeyboard.serde.JSON;
import com.example.extkeyboard.serde.JsonKey;
import com.example.extkeyboard.serde.JsonKeyBoard;
import com.example.extkeyboard.serde.JsonSettings;
import com.example.transliterator.TransliteratorManager;

public class KeyBoardsManager {

    private static KeyBoardsManager instance;
    private Map<KeyboardHandler, GenericKeyboard> keyBoardMap = new WeakHashMap<>();

    private static Map<String, JsonKeyBoard> jkbMap = new HashMap<>();

    private static Map<String, InputProvider> providersMap = new LinkedHashMap<>();
    private static List<InputProvider> providers = new ArrayList<>();

    private InputProvider defaultProvider = null, currentProvider, langProvider;
    private Context context;

    public static void init(Context context) {
        try {
            instance = new KeyBoardsManager(context);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        instance.loadKeyboards();
    }

    private KeyBoardsManager(Context context) throws XmlPullParserException, IOException {
        this.context = context;
    }

    public void loadKeyboards() {

        providersMap.clear();
        providers.clear();

        keyBoardMap.clear();
        langProvider = currentProvider = null;

        InputProvider symbolsKeyBoard = null;


        JsonSettings settings = Configuration.getInstance().getSettings().getSubSetting("keyboards");

        for (InputProvider provider : Configuration.getInstance().getProviders()) {
            if (provider.isEnabled()) {
                providersMap.put(provider.getName(), provider);
                JsonSettings js = settings.getSubSetting(provider.getName());
                if (js != null) {
                    if (js.getOn()) {
                        providers.add(provider);
                    }
                }else{
                    js = new JsonSettings();
                    js.setId(provider.getName());
                    js.setLabel(provider.getName());
                    js.setOn(false);
                    settings.getSettings().add(js);
                }
            }
        }

        for (InputProvider provider : Configuration.getInstance()
                .getHiddenProviders()) {
            providersMap.put(provider.getName(), provider);
        }

        List<JsonSettings> deleted = new ArrayList<>();
        for(JsonSettings setting : settings.getSettings()){
            if(providersMap.get(setting.getId()) == null){
                deleted.add(setting);
            }
        }
        settings.getSettings().removeAll(deleted);

        for (InputProvider ip : providers) {
            for (KeyboardHandler kbProvider : ip.getKbProviders()) {
                if (langProvider == null && kbProvider.getLanguage() != null) {
                    langProvider = ip;
                    if (currentProvider == null) {
                        currentProvider = ip;
                    }
                } else if (symbolsKeyBoard == null && kbProvider.getName().equals(Constants.STR_KB_SYMBOL)) {
                    symbolsKeyBoard = ip;
                    providersMap.put(Constants.STR_KB_SYMBOL, ip);
                }

                if (kbProvider.getShiftedKb() != null) {
                    setShiftedKeyBoard(ip, kbProvider);
                }
            }
        }

        if (currentProvider == null) {
            currentProvider = providers.get(0);
        }

    }

    private Properties getSettings() {
        InputStream is = null;
        try {
            is = context.openFileInput(Constants.STR_SETTINGS_FILENAME);
        } catch (FileNotFoundException e) {
            try {
                is = getResources().getAssets().open("defaults/settings");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        Properties properties = new Properties();

        if (is != null) {
            try {
                properties.load(is);
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    private Resources getResources() {
        return context.getResources();
    }

    private void setShiftedKeyBoard(InputProvider ip, KeyboardHandler kbProvider) {
        if (kbProvider.getShiftedKb() != null &&
                kbProvider.getShiftedKbProvider() == null) {
            KeyboardHandler shiftedKbProvider = ip.getKbHandler(kbProvider.getShiftedKb());
            if (shiftedKbProvider != null) {

                kbProvider.setShiftedProvider(shiftedKbProvider);
                if (kbProvider.getBaseProvider() != null) {
                    shiftedKbProvider.setBaseProvider(kbProvider.getBaseProvider());
                } else {
                    shiftedKbProvider.setBaseProvider(kbProvider);
                }

                if (shiftedKbProvider.getShiftedKb() == null) {
                    //do nothing
                } else if (shiftedKbProvider.getShiftedKb().equals(shiftedKbProvider.getBaseProvider())) {
                    //do nothing
                } else {
                    setShiftedKeyBoard(ip, shiftedKbProvider);
                }
            }
        }
         /*if(keyboard.getShiftedKeyBoardId()!= null &&
	    			keyboard.getShiftedKeyBoard() == null){
	    		GenericKeyboard shiftedKeyBoard = keyBoardMap.get(keyboard.getShiftedKeyBoardId());
	    		if(shiftedKeyBoard == null){
	    			shiftedKeyBoard = new GenericKeyboard(context, (int) R.xml.class.getField(keyboard.getShiftedKeyBoardId()).get(null));
	    			keyBoardMap.put(keyboard.getShiftedKeyBoardId(), shiftedKeyBoard);
	    		}
	    		keyboard.setShiftedKeyBoard(shiftedKeyBoard);
	    		if(keyboard.getBaseKeyBoard() == null){
	    			shiftedKeyBoard.setBaseKeyBoard(keyboard);
	    		}else{
	    			shiftedKeyBoard.setBaseKeyBoard(keyboard.getBaseKeyBoard());
	    		}
	    		if(shiftedKeyBoard.getShiftedKeyBoardId() != null){
	    			setShiftedKeyBoard(shiftedKeyBoard);
	    		}
	    	}*/
    }

    public static KeyBoardsManager getInstance() {
        return instance;
    }

    public GenericKeyboard getKeyBoard(InputProvider provider) {
        GenericKeyboard keyboard = null;
        KeyboardHandler kbProvider = provider.getCurrentKbHandler();
        if (provider != null) {
            keyboard = keyBoardMap.get(kbProvider);
            if (keyboard == null) {
                String configuration = kbProvider.getConfiguration();
                if (configuration != null) {
                    try {
                        keyboard = buildKeyboard(provider.getCurrentKbHandler());
                        keyBoardMap.put(kbProvider, keyboard);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    String baseBoard = kbProvider.getBaseBoard();
                    try {
                        keyboard = new GenericKeyboard(context, (int) R.xml.class.getField(baseBoard).get(null));
                        keyBoardMap.put(kbProvider, keyboard);
                    } catch (IllegalAccessException | IllegalArgumentException
                            | NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(provider.getCurrentKbHandler().getTransliterator() != null){
                TransliteratorManager.getInstance().load(provider.getCurrentKbHandler().getTransliterator());
            }
        }
        return keyboard;
    }

    private GenericKeyboard buildKeyboard(KeyboardHandler kbHandler) throws Exception {
        String configuration = kbHandler.getConfiguration();
        JsonKeyBoard jkb = jkbMap.get(configuration);
        if (jkb == null) {
            String metadata = Util.readStream(context.getAssets().open("kb/" + configuration));
            jkb = new JSON<>(metadata, JsonKeyBoard.class).toObject();
        }
        GenericKeyboard keyboard = new GenericKeyboard(context, (int) R.xml.class.getField(jkb.getBaseBoard()).get(null));
        /***************************************************************************
         * Modify the keyboard as per configuration
         * ************************************************************************/
        Map<String, GenericKey> keyMap = new HashMap<>();
        keyboard.setName(jkb.getName());
        if(jkb.getBoardType() != null){
            keyboard.setBoardType(jkb.getBoardType());
        }
        if(kbHandler.getLanguage() != null){
            keyboard.setLanguage(kbHandler.getLanguage() );
        }

        kbHandler.setTextSize(jkb.getTextSize());
        keyboard.setCustomDraw(jkb.isCustomDraw());
        kbHandler.setHidden(jkb.isHidden());

        for (Key key : keyboard.getKeys()) {
            String name = "";
            for (int code : key.codes) {
                if (name.length() > 0) {
                    name += ",";
                }
                name += code;
            }
            keyMap.put(name, (GenericKey) key);
        }
        for (JsonKey jKey : jkb.getKeys()) {
            GenericKey key = keyMap.get(jKey.getId());
            if (key != null) {
                if (jKey.getCodes() != null) {
                    key.codes = jKey.getCodes();
                }
                if (jKey.getLabel() != null) {
                    key.label = jKey.getLabel();
                }
                if (jKey.getPopupCharacters() != null) {
                    if (jKey.getPopupCharacters().isEmpty()) {
                        key.popupCharacters = null;
                    } else {
                        key.popupCharacters = jKey.getPopupCharacters();
                    }
                }

                if(jKey.isPopupKeyboard()){
                    key.popupResId = R.layout.keyboard_popup_layout;
                }

                if (jKey.getKeyHints() != null) {
                    if (jKey.getKeyHints().length() == 0) {
                        key.keyHints = null;
                    } else {
                        key.keyHints = jKey.getKeyHints().split(",");
                    }
                }
                if (jKey.getDoubleTapCode() != Constants.OUT_OF_BOUNDS) {
                    key.doubleTapCode = jKey.getDoubleTapCode();
                }

            }
        }
        /**************************************************************************/

        return keyboard;
    }

    public GenericKeyboard getDefaultKeyBoard() {
        if (defaultProvider != null) {
            return getKeyBoard(defaultProvider);
        } else {
            return getKeyBoard(providers.get(0));
        }
    }

    public List<InputProvider> getKbProvider() {
        return providers;
    }

    public int getCurrentBoardIndex() {
        for (int i = 0; i < providers.size(); i++) {
            InputProvider keyboard = providers.get(i);
            if (keyboard == currentProvider /*||
    				(currentKeyboard.getBaseKeyBoard() != null 
    				&& currentKeyboard.getBaseKeyBoard() == keyboard)*/) {
                return i;
            }
        }
        return -1;
    }

    public GenericKeyboard getNextBoard() {
        InputProvider kb = currentProvider;
    	/*if(currentKeyboard.getBaseKeyBoard() != null){
    		kb = currentKeyboard.getBaseKeyBoard();
    	}*/
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i) == kb) {
                return getKeyBoard(providers.get((i + 1) % providers.size()));
            }
        }
        return null;
    }

    public InputProvider getNextProvider() {
        InputProvider kb = currentProvider;
    	/*if(currentKeyboard.getBaseKeyBoard() != null){
    		kb = currentKeyboard.getBaseKeyBoard();
    	}*/
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i) == kb) {
                return providers.get((i + 1) % providers.size());
            }
        }
        return null;
    }

    public GenericKeyboard getPreviousBoard() {
        InputProvider kb = currentProvider;
    	/*if(currentKeyboard.getBaseKeyBoard() != null){
    		kb = currentKeyboard.getBaseKeyBoard();
    	}*/
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i) == kb) {
                return getKeyBoard(providers.get((i - 1) % providers.size()));
            }
        }
        return null;
    }

    public GenericKeyboard getCurrentKeyboard() {
        return getKeyBoard(getCurrentProvider());
    }

    public InputProvider getCurrentProvider() {
        if (currentProvider != null) {
            return currentProvider;
        } else if (langProvider != null) {
            return langProvider;
        } else {
            return providers.get(0);
        }
    }

    public KeyboardHandler getCurrentHandler() {
        return getCurrentProvider().getCurrentKbHandler();
    }

    public void setCurrentKeyboard(InputProvider currentKeyboard) {
        if (currentKeyboard.getCurrentKbHandler().getLanguage() != null) {
            langProvider = currentKeyboard;
        }
        this.currentProvider = currentKeyboard;
    }

    public String getCurrentKeyBoardName() {
        return currentProvider.getName();
    }

    public EBoardTypes getCurrentKeyBoardType() {
        return getKeyBoard(currentProvider).getBoardType();
    }

    public Map<String, InputProvider> getAllProviders() {
        return providersMap;
    }

    public InputProvider getNumericProvider() {
        InputProvider ip = providersMap.get(Constants.STR_KB_NUMERIC);
        if (ip != null) {
            return ip;
        } else {
            return getCurrentProvider();
        }
    }

    public InputProvider getKeypad() {
        InputProvider ip = providersMap.get(Constants.STR_KB_KEYPAD);
        if (ip != null) {
            return ip;
        } else {
            return getCurrentProvider();
        }
    }

    public GenericKeyboard getLanguageKeyboard() {
        if (langProvider != null) {
            return getKeyBoard(providersMap.get(langProvider.getName()));
        } else {
            return getDefaultKeyBoard();
        }
    }

}
