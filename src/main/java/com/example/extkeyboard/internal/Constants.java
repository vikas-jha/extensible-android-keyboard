package com.example.extkeyboard.internal;

public class Constants {

	public static final int ZERO		 			= 0;
	public static final int OUT_OF_BOUNDS 			= -1;
	public static final int KEYCODE_NONE 			= -1;
	public static final int KEYCODE_LONG_PRESS		= -10;
	public static final int KEYCODE_FLICK_UP		= -11;
	public static final int SUGGESTION_KEY_CODE		= -1000;
	public static final int LONG_PRESS_TIME			= 500;
	
	public static final int DICT_MIN_WORD_LENGTH	= 2;
	public static final int DICT_MAX_SUGGESTIONS	= 16;
	public static final int NUM_MIN_KEYBOARDS		= 2;
	public static final int NUM_MAX_KEYBOARDS		= 6;
	
	//Character symbols
	public static final int SYMBOL_EMOJI  			= 0x263A;
	public static final int SYMBOL_HELP 			= 0xFE16;
	public static final int SYMBOL_RECENT_EMOJI		= 0x1F551;


	
	//Special key codes
	public static final int KEYCODE_SHIFT	 		= -1;
	public static final int KEYCODE_OPTIONS 		= -100;
	public static final int KEYCODE_LANGUAGE_SWITCH = -101;
	public static final int KEYCODE_BOARD_SWITCH 	= -104;
	public static final int KEYCODE_SETTINGS 	 	= -105;
	public static final int KEYCODE_KB_OPTIONS_VIEW	= -106;	//show default candidate view i.e. keyboards options view
	public static final int KEYCODE_EMOJI_BOARD		= -9786;
	public static final int KEYCODE_SPACE			= 32;
	public static final int KEYCODE_DOUBLE_SPACE	= -32;
	public static final int KEYCODE_ENTER			= 10;
	public static final int KEYCODE_DELETE			= -5;
    public static final int KEYCODE_AUTO_COMPLETE	= -107;
    public static final int KEYCODE_EXPAND_SYMBOL   = -1032;
    public static final int KEYCODE_EXPAND_1     	= -1033;
    public static final int KEYCODE_EXPAND_2     	= -1034;

    //Keys
    public static final int[] KEY_DELETE = { KEYCODE_DELETE };
	
	//String constants
	public static final String STR_SETTINGS_SYMBOL 	= "\u2699";
    public static final String STR_HELP_SYMBOL 		= "\uFE16";
    public static final String STR_EMOJI_SYMBOL  	= "\u263A";
    public static final String STR_SETTINGS_ASSET	= "defaults/settings.json";
    public static final String STR_HELP_CONTEXT		= "helpContext";
    
    public static final String STR_KB_NUMERIC		= "Numeric";
    public static final String STR_KB_KEYPAD		= "Keypad";
    public static final String STR_KB_SYMBOL		= "symbols";
    public static final String STR_KB_LANG			= "lang";
    
    public static final String STR_SETTINGS_FILENAME		= "settings.json";
    public static final String STR_RECENT_EMOJI_FILENAME	= "recentEmojis";

	//Popups keys
	public static final String POPUP_NAME_SETTINGS 			= "SETTINGS_POPUP";
	public static final String POPUP_NAME_HELP 				= "HELP_POPUP";
	public static final String POPUP_NAME_INPUT_SELECTOR	= "INPUT_SELECTOR_POPUP";
    
    //Colors
    static final int COLOR_CANDIDATE_NORMAL	= 0;
    public static final int COLOR_WHITE 	= 0xFFFFFFFF;
    public static final int COLOR_BLACK 	= 0xFF000000;
    public static final int COLOR_NOAPLPHA_OR 	= 0xFF000000;

    static final int COLOR_OFF_WHITE 		= 0xFFEEEEEE;
    static final int COLOR_WHITE_GREY 		= 0xFFDDDDDD;
    static final int COLOR_LIGHT_GREY 		= 0xFFCCCCCC;
    static final int COLOR_GREY 			= 0xFFAAAAAA;
    static final int COLOR_DARK_GREY 		= 0xFF444444;

}
