package com.example.transliterator;

import com.example.dictionary.WordNode;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Transliterator {
	
	private String locales;
	private String id;
	private CharsNode root = new CharsNode("");
	private boolean modified = false;
	private boolean loading = false;
	
	//Annoying empty unicode character
	private static final Character CHAR_EMPTY = Character.valueOf((char) 65279);
	
	public Transliterator (String locales){
		this.locales = locales;
	}

	/* this assumes clean words*/
	public void addWord(String word){
		synchronized(this){
			insert(word);
			if(modified == false){
				modified = true;
			}
		}
	}


	private void insert(String text){
		text = text.replace(String.valueOf(CHAR_EMPTY), "");
		if(text.length() >= 2 && text.contains("=")){
			root.insert(text);
		}
	}

    public String transliterate(int primaryCode, String text){
        text = text + String.valueOf(Character.toChars(primaryCode));
        for(int i = 0; i < text.length(); i++){
            CharsNode node = root.findWord(text.substring(i));
            if(node != null && node.getWordNodes().size() > 0){
                for(SymbolNode wordNode: node.getWordNodes()){
                    String newWord = text.substring(0,i) + wordNode.getWord();
                    if((wordNode.getCondition() != null &&
                            !newWord.matches(".*" + wordNode.getCondition() + "$"))
                            || newWord.contains("~")
                            ){
                        continue;
                    }
                    else{
                        text = newWord;
                        break;
                    }
                }

            }
        }
        return text;
    }
	
	public List<String> transliterate(String text){
		final Map<String,Integer> predictions = new HashMap<>();
		
		transliterate(text, "", 0, predictions, 0);
		List<String> strings = new ArrayList<>(predictions.keySet());
		Collections.sort(strings, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return predictions.get(o2) - predictions.get(o1); 
			}

		});
		return strings;
	}


	
	private void transliterate(String text, String str, int index, Map<String,Integer> predictions, int score) {
        for (int i = text.length() - 1; i >= index; i--) {
            CharsNode node = root.findWord(text.substring(index, i + 1));
            if (node != null) {
                for (SymbolNode wordNode : node.getWordNodes()) {
                    String newWord = str + wordNode.getWord();

                    if (wordNode.getCondition() != null) {
                        if (!newWord.matches(".*" + wordNode.getCondition() + "$")) {
                            continue;
                        }
                    }

                    score += (i - index + 1) * (i - index + 1);
                    if (i == text.length() - 1) {
                        if (!predictions.containsKey(newWord)
                                || predictions.get(newWord) < score) {
                            newWord = newWord.replace("~", "");
                            if (newWord.length() > 0) {
                                predictions.put(newWord, score);
                            }

                        }
                    } else {
                        transliterate(text, newWord, i + 1, predictions, score);
                    }
                }
                return;
            }
        }

        if(text.length() > index){
            transliterate(text, text.substring(0, index + 1), index + 1, predictions, score);
        }


    }

    public boolean contains(String word){
		
		synchronized (this) {
			if(root.findWord(word) != null){
				return true;
			}
			return false;
		}
		
	}
	
	
	
	public String serialize(){
		synchronized (this) {
			return root.serialize();
		}
	}
	
	/*public void load(String data){
		synchronized (this) {
			loadWords(data);
		}
	}*/
	
	public void load(String data, boolean comiled){
		synchronized (this) {
			if(comiled){
				loadWords(data);
			}else{
				String[] mappings = data.split(",");
				for(String s : mappings){
					insert(s);
				}
			}
		}
	}
	
	private void loadWords(String data){
		Deque<String> stack = new ArrayDeque<>(128);
		String token = "";
		Character symbol = null;
		for(int i = 0; i < data.length() ; i++ ){
			char c =  data.charAt(i);
			if(isDictSymbol(c)){
				if(token.length() > 0){
					stack.push(token);
					token = "";
				}
				if(symbol != null && symbol == '(' && c != '+'){
					Iterator<String> iter = stack.descendingIterator();
					String word = "";
					while(iter.hasNext()){
						String t = iter.next();
						if( t.length() > 1 || !isDictSymbol(t.charAt(0))){
							word += t;
						}
						
					}
					insert(word);
				}
				symbol = c;
				
			}
			if(c == '{' || c == '[' || c =='('){
				stack.push(c + "");
			}else if(c == ')'){
				popBrackets(stack, '(');
			}else if(c == '}'){
				popBrackets(stack, '{');
			}else if(c == ']'){
				popBrackets(stack, '[');
			}else if(c == '+'){
				Iterator<String> iter = stack.descendingIterator();
				String word = "";
				while(iter.hasNext()){
					String t = iter.next();
					if( t.length() > 1 || !isDictSymbol(t.charAt(0))){
						word += t;
					}
					
				}
				
				if(word.length() > 0){
					int start = data.indexOf("[",i);
					int end = data.indexOf("]",i);
					String[] symbols = data.substring(start + 1, end).split(",");
					for(String s : symbols){
						insert(word + "=" + s);
					}	
				}
				
				
			}else if(c == ',' || c == CHAR_EMPTY ){
				
			}else{
				token += c;
			}
		}
		
	}
	
	private boolean isDictSymbol(char c){
		if(c == '{' || c == '[' || 
				c == '}' || c == ']' ||
				c == ')' || c == '(' ||
				c == ',' || c == '+'){
			return true;
		}else{
			return false;
		}
	}
	
	private void popBrackets(Deque<String> stack, char tc){
		while(!stack.isEmpty()){
			String token = stack.pop();
			
			if(token.length() == 1){
				char c = token.charAt(0);
				if(c == tc){
					return;
				}else if(c == '{' || c == '[' || 
						c == '}' || c == ']' ||
						c == ')' || c == '('){
					System.err.println("unmatched braces found");
					return;
				}
			}
		}
		
	}

	public String getLocales() {
		return locales;
	}

	public boolean isModified() {
		return modified;
	}

	public boolean isLoading() {
		return loading;
	}

}
