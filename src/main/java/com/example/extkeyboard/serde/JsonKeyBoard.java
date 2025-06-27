package com.example.extkeyboard.serde;

import com.example.extkeyboard.EBoardTypes;

import java.util.List;

public class JsonKeyBoard {
	private String name;
	private String symbol;
	private String baseBoard;
	private String language;
    private String locale;
	private String shiftedKb;
	private String id;
    private String boardType;
    private String textSize;
    private boolean customDraw;
    private boolean hidden;
    private List<JsonKey> keys;

	
	public String getName() {
		return name;
	}
	public String getSymbol() {
		return symbol;
	}
	public String getBaseBoard() {
		return baseBoard;
	}
	public String getLanguage() {
		return language;
	}
	public String getShiftedKb() {
		return shiftedKb;
	}
	public String getId() {
		return id;
	}
	public String getLocale(){
        return locale;
    }
	public List<JsonKey> getKeys() {
		return keys;
	}


    public EBoardTypes getBoardType() {
        if(boardType != null){
           return EBoardTypes.valueOf(boardType);
        }
        return null;
    }

    public String getTextSize() {
        return textSize;
    }

    public boolean isCustomDraw() {
        return (customDraw || textSize != null);
    }

    public boolean isHidden() {
        return hidden;
    }
}
