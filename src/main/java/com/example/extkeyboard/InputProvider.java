package com.example.extkeyboard;

import java.util.Collections;
import java.util.List;

public class InputProvider {
	private String symbol;
	private String name;
	private boolean enabled;
	private String current;
	private List<KeyboardHandler> handlers;
	
	public String getSymbol() {
		return symbol;
	}
	public String getName() {
		return name;
	}
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setCurrentHandler(KeyboardHandler kbHandler){
		this.current = kbHandler.getId();
	}

	public void setCurrentKbProvider(String id){
		this.current = id;
	}
	
	public KeyboardHandler getCurrentKbHandler(){
		if(handlers.size() == 1 || (current == null && handlers.size() > 0)){
			return handlers.get(0);
		}else{
			for(KeyboardHandler kbProvider : handlers){
				if(kbProvider.getId().equals(current)){
					return kbProvider;
				}
			}
		}
		return null;
	}
	
	public KeyboardHandler getKbHandler(String id) {
		for (KeyboardHandler kbProvider : handlers) {
			if (kbProvider.getId().equals(id)) {
				return kbProvider;
			}
		}
		return null;
	}
	
	public List<KeyboardHandler> getKbProviders() {
		return Collections.unmodifiableList(handlers);
	}

}
