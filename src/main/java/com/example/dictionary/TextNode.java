package com.example.dictionary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TextNode {

	private String text;
	//private String word;
	private WordNode wordNode;

	private ConcurrentHashMap<String, TextNode> textNodes;

	public TextNode(String text) {
		super();
		this.text = text.toLowerCase();
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text.toLowerCase();
	}

	@Override
	public String toString() {
		String s = "{" + this.text + ",";
		if(this.wordNode != null){
			s += this.wordNode.toString();
		}else{
			s += "null";
		}
		s += "}";
		return s;
	}

	public Map<String, TextNode> getWordNodes() {
		return textNodes;
	}

	public WordNode insert(String word) {
		return insert(word, 0);
	}

    private WordNode insert(String word, int index) {

        WordNode wordNode = null;

        int i = 0;
        String text = word.toLowerCase();
        if (getText().length() > 0) {
            for (i = 0; i + index < text.length() && i < getText().length(); i++) {
                if (text.charAt(index + i) != getText().charAt(i)) {
                    break;
                }
            }
        }

        index += i;

        if (index < word.length() && i < getText().length()) {
            /*Common region is smaller than both node and new word
            * old - abcdjhik
            * new - abcdwxyz
            */
            TextNode splitNode = new TextNode(getText().substring(i)
                    .toLowerCase());
            if (this.wordNode != null) {
                splitNode.setWordNode(this.wordNode);
                this.setWordNode(null);
            }
            splitNode.textNodes = this.textNodes;

            this.textNodes = new ConcurrentHashMap<>();
            this.textNodes.put(getText().substring(i, i + 1), splitNode);

            TextNode childNode = new TextNode(word.substring(index)
                    .toLowerCase());
            wordNode = new WordNode(word);
            childNode.setWordNode(wordNode);
            this.textNodes.put(word.substring(index, index + 1), childNode);

            this.setText(getText().substring(0, i));

        } else if (i < getText().length()) {
            /*new word is part of node word
            *old - abcdjhik
            *new - abcd
            */
            TextNode splitNode = new TextNode(getText().substring(i)
                    .toLowerCase());
            splitNode.setWordNode(this.wordNode);
            wordNode = new WordNode(word);
            this.setWordNode(wordNode);
            splitNode.textNodes = this.textNodes;

            this.textNodes = new ConcurrentHashMap<>();
            this.textNodes.put(getText().substring(i, i + 1), splitNode);

            this.setText(getText().substring(0, i));

        } else if (index < word.length()) {
            /*new word is part of node word
            *old - abcd
            *new - abcdwxyz
            */
            String key = word.toLowerCase().substring(index, index + 1);
            if(textNodes == null){
                textNodes = new ConcurrentHashMap<String, TextNode>();
            }
            TextNode childNode = textNodes.get(key);
            if (childNode == null) {
                childNode = new TextNode(word.substring(index).toLowerCase());
                wordNode = new WordNode(word);
                childNode.setWordNode(wordNode);
                textNodes.put(word.substring(index, index + 1).toLowerCase(),
                        childNode);
            } else {
                //textNodes.remove(key);
                //textNodes.put(key, childNode);
                wordNode = childNode.insert(word, index);
            }
        }else if ( index == word.length()){
            /*new word and node word are same
            *old - abcd
            *new - abcd
            */

            if(getWordNode() != null &&
                    getWordNode().getWord(). equalsIgnoreCase(word)){
                if(!getWordNode().getWord().equals(word)){
                    getWordNode().setWord(word.toLowerCase());
                }
                getWordNode().changeCount(1);

            }else{
                wordNode = new WordNode(word);
                this.setWordNode(wordNode);
            }
        }
        return wordNode;
    }
	
	public List<WordNode> predict(String text){
		List<WordNode> predications = new ArrayList<>();
		predict(text, 0, predications);
		return predications;
	}
	
	private void predict(String text, int index, List<WordNode> predications){
		if(text.length() - index > getText().length()){
			if(textNodes == null){
				return;
			}
			index += getText().length();
			String indexChar = text.substring(index ,index + 1).toLowerCase();
			TextNode childNode = textNodes.get(indexChar);
			if(childNode != null){
				childNode.predict(text, index, predications);
			}
		}else{
			if(text.length() - index == getText().length()){
				if(!text.substring(index).equalsIgnoreCase(getText())){
					return;
				}
			}
			if(getWordNode() != null){
				predications.add(getWordNode());
			}
			
			if(textNodes != null){
				for(Entry<String, TextNode> e : textNodes.entrySet()){
					TextNode textNode = e.getValue();
					textNode.predict("", 0, predications);
				}
			}
			
			
		}
		
	}

	public WordNode getWordNode() {
		return wordNode;
	}

	public void setWordNode(WordNode wordNode) {
		this.wordNode = wordNode;
	}
	
	public TextNode findWord(String word){
		return findWord(word, 0);
	}
	
	private TextNode findWord(String word, int index){
		if(word.length() - index > getText().length()){
			if(textNodes == null){
				return null; 
			}
			index += getText().length();
			String indexChar = word.substring(index ,index + 1).toLowerCase();
			TextNode childNode = textNodes.get(indexChar);
			if(childNode != null){
				return childNode.findWord(word, index);
			}
		}else if(word.length() - index == getText().length()){
			if(this.wordNode != null && this.wordNode.getWord().equals(word)){
				return this;
			}
		}
		return null;
	}
	
	public String serialize(){
        String s;
        if(this.wordNode != null){
            s = "(" + this.getText();
            if(!wordNode.getWord().toLowerCase().equals(wordNode.getWord())){
                s += "+" + wordNode.getWord();
            }

            s += "{" + wordNode.getCount() + "}";

        }else{
            s = "{" + this.getText();
        }
        if(textNodes != null){
            s += "[";
            List<Entry<String,TextNode>> list = new ArrayList<Entry<String,TextNode>>(textNodes.entrySet());
            for(int i = 0 ; i < list.size(); i++){
                TextNode textNode = list.get(i).getValue();

                s += textNode.serialize();
                if(i < list.size() - 1){
                    s += ",";
                }
            }

            s += "]";
        }

        s += this.wordNode == null ?"}" : ")";

        return s;


    }

}
