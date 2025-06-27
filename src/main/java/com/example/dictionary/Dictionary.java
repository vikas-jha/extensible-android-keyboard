package com.example.dictionary;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.example.extkeyboard.internal.Constants;

public class Dictionary {
	
	private Locale locale;
	private String id;
	private TextNode root = new TextNode("");
	private boolean modified = false;
	private boolean loading = false;
	
	//Annoying empty unicode character
	private static final Character CHAR_EMPTY = Character.valueOf((char) 65279);
	
	public Dictionary (Locale locale){
		this.locale = locale;
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
		String[] words = text.split("[\\s+:;/\\)\\(\\[\\]{}]");
			for(String word: words){
				word = word.replace(CHAR_EMPTY.toString(),"");
				if(word.length() == 0 || word.matches(".*[0-9_].*")){
					continue;
				}
				word = word.replaceAll("[,\"&\\.:]", "");
				if(word.endsWith(".")&& word.indexOf('.') < word.length() - 1){
					word = word.substring(0,word.length() - 1);
				}
				root.insert(word);
			}
	}

    private void insert(String text, long frequncy ){

        WordNode node = root.insert(text);
        if(node != null){
            node.setCount(frequncy);
        }

    }
	
	public List<String> predict(String text){
        List<WordNode> nodes = root.predict(text);
        Collections.sort(nodes, new Comparator<WordNode>() {
            @Override
            public int compare(WordNode o1, WordNode o2) {
                return o1.getCount() == o2.getCount() ? 0 : (o1.getCount() > o2.getCount() ? -1 : 1);
            }
        });

        List<String> predictions = new ArrayList<>(32);

        for(int i = 0; i < nodes.size(); i++){
            predictions.add(nodes.get(i).getWord());
        }
        return predictions;
	}

    public List<String> predict(List<String> texts){
        List<WordNode> nodes = new ArrayList<>();
        for(String text :texts){
            nodes.addAll(root.predict(text));
        }
        Collections.sort(nodes, new Comparator<WordNode>() {
            @Override
            public int compare(WordNode o1, WordNode o2) {
                return o1.getCount() == o2.getCount() ? 0 : (o1.getCount() > o2.getCount() ? -1 : 1);
            }
        });

        List<String> predictions = new ArrayList<>(32);

        for(int i = 0; i < nodes.size(); i++){
            predictions.add(nodes.get(i).getWord());
        }
        return predictions;
    }
	
	public boolean contains(String word){
		
		synchronized (this) {
			if(word == null || word.length() < Constants.DICT_MIN_WORD_LENGTH){
				return false;
			}else if(root.findWord(word) != null){
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
	
	public void load(String data){
		synchronized (this) {
			loadWords(data);
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
                    if(c == '{'){
                        int end = data.indexOf('}', i+1);
                        String value = data.substring(i + 1,end);
                        if(value.matches("\\d+")){
                            long frequency = Long.parseLong(value);
                            insert(word, frequency);
                            i = end;
                            continue;
                        }else{
                            insert(word);
                        }
                    }else{
                        insert(word);
                    }
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
                String word = "";
                while(!isDictSymbol(data.charAt(i + 1))){
                    i++;
                    word += data.charAt(i);
                }
                if(data.charAt(i+1) == '{'){
                    int end = data.indexOf('}', i+2);
                    String value = data.substring(i + 2,end);
                    if(value.matches("\\d+")){
                        long frequency = Long.parseLong(value);
                        insert(word, frequency);
                        i = end;
                    }else{
                        insert(word);
                    }
                }else{
                    insert(word);
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

	public Locale getLocale() {
		return locale;
	}

	public boolean isModified() {
		return modified;
	}

	public boolean onSaved(){
        modified = false;
        return true;
    }

	public boolean isLoading() {
		return loading;
	}

}
