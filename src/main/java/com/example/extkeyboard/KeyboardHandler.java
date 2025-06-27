package com.example.extkeyboard;

public class KeyboardHandler {
	private String name;
	private String symbol;
	private String configuration;
	private String baseBoard;
	private String locale;
    private String language;
	private String shiftedKb;
    private String transliterator;
	private String id;
    private String textSize;
    private boolean hidden;
    private String font;
	
	private KeyboardHandler shiftedProvider;
	private KeyboardHandler baseProvider;
	
	public String getName() {
		return name;
	}
	public String getSymbol() {
		return symbol;
	}
	public String getConfiguration() {
		return configuration;
	}
	public String getBaseBoard() {
		return baseBoard;
	}
	public String getLanguage() {
        if(language != null){
            return  language;
        }
		return getLocale();
	}
	public String getShiftedKb() {
		return shiftedKb;
	}
	public String getId() {
		return id;
	}
	public KeyboardHandler getShiftedKbProvider() {
		return shiftedProvider;
	}
	public void setShiftedProvider(KeyboardHandler shiftedProvider) {
		this.shiftedProvider = shiftedProvider;
	}
	public KeyboardHandler getBaseProvider() {
		return baseProvider;
	}
	public void setBaseProvider(KeyboardHandler baseProvider) {
		this.baseProvider = baseProvider;
	}

    public String getTransliterator() {
        return transliterator;
    }

    public String getLocale() {
        return locale;
    }

    public String getTextSize() {
        return textSize;
    }

    public void setTextSize(String textSize) {
        this.textSize = textSize;
    }


    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getFont() {
        return font;
    }

    public void setFont(String font) {
        this.font = font;
    }
}
