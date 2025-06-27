package com.example.transliterator;

import com.example.transliterator.SymbolNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class CharsNode {

    private String text;
    //private String word;
    private List<SymbolNode> wordNode = new ArrayList<>();

    private ConcurrentHashMap<String, CharsNode> textNodes;

    public CharsNode(String text) {
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

    public Map<String, CharsNode> getTextNodes() {
        return textNodes;
    }

    public List<SymbolNode> getWordNodes(){
        return this.wordNode;
    }

    public void insert(String word) {
        insert(word, 0);
    }

    private boolean insert(String tl, int index) {

        String[] parts = tl.split("=");
        String word = parts[0];
        String symbol = parts[1];
        String condition = null;
        if(symbol.contains(":")){
            parts = symbol.split(":");
            symbol = parts[0];
            condition = parts[1];
        }
        int i = 0;
        String text = word;
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
            CharsNode splitNode = new CharsNode(getText().substring(i)
                    .toLowerCase());
            if (this.wordNode != null) {
                splitNode.wordNode = this.wordNode;
                this.wordNode = new ArrayList<>();
            }
            splitNode.textNodes = this.textNodes;

            this.textNodes = new ConcurrentHashMap<>();
            this.textNodes.put(getText().substring(i, i + 1), splitNode);

            CharsNode childNode = new CharsNode(word.substring(index)
                    .toLowerCase());
            childNode.wordNode.add(new SymbolNode(symbol, condition));
            this.textNodes.put(word.substring(index, index + 1), childNode);

            this.setText(getText().substring(0, i));

        } else if (i < getText().length()) {
            /*new word is part of node word
            *old - abcdjhik
            *new - abcd
            */
            CharsNode splitNode = new CharsNode(getText().substring(i)
                    .toLowerCase());
            splitNode.wordNode = this.wordNode;
            this.wordNode = new ArrayList<>();
            this.wordNode.add(new SymbolNode(symbol, condition));
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
                textNodes = new ConcurrentHashMap<String, CharsNode>();
            }
            CharsNode childNode = textNodes.get(key);
            if (childNode == null) {
                childNode = new CharsNode(word.substring(index).toLowerCase());
                childNode.wordNode.add(new SymbolNode(symbol, condition));
                textNodes.put(word.substring(index, index + 1).toLowerCase(),
                        childNode);
                return true;
            } else {
                //textNodes.remove(key);
                //textNodes.put(key, childNode);
                childNode.insert(tl, index);
            }
        }else if ( index == word.length()){
            /*new word and node word are same
            *old - abcd
            *new - abcd
            */

			/*if(getWordNode() != null &&
					getWordNode().getWord(). equalsIgnoreCase(word)){
                 if(!getWordNode().getWord().equals(word)){
                     getWordNode().setWord(word.toLowerCase());
                 }
                 getWordNode().changeCount(1);

			}else{*/
            this.wordNode.add(new SymbolNode(symbol, condition));
            //}
        }
        return false;
    }

    public List<SymbolNode> transliterate(String text){
        List<SymbolNode> predictions = new ArrayList<>();
        transliterate(text, 0, predictions);
        return predictions;
    }

    private void transliterate(String text, int index, List<SymbolNode> predictions){

        if(text.length() - index > getText().length()){
            if(textNodes == null){
                return;
            }
            index += getText().length();
            String indexChar = text.substring(index ,index + 1).toLowerCase();
            CharsNode childNode = textNodes.get(indexChar);
            if(childNode != null){
                childNode.transliterate(text, index, predictions);
            }
        }else{
            if(text.length() - index == getText().length()){
                if(!text.substring(index).equalsIgnoreCase(getText())){
                    return;
                }
            }
            if(this.wordNode != null){
                predictions.addAll(this.wordNode);
            }

            if(textNodes != null){
                for(Entry<String, CharsNode> e : textNodes.entrySet()){
                    CharsNode textNode = e.getValue();
                    textNode.transliterate("", 0, predictions);
                }
            }


        }

    }

    public CharsNode findWord(String word){
        return findWord(word, 0);
    }

    private CharsNode findWord(String word, int index){
        if(word.length() - index > getText().length()){
            if(textNodes == null){
                return null;
            }
            index += getText().length();
            String indexChar = word.substring(index ,index + 1).toLowerCase();
            CharsNode childNode = textNodes.get(indexChar);
            if(childNode != null){
                return childNode.findWord(word, index);
            }
        }else if(word.length() - index == getText().length()){
            if(word.substring(index).equals(getText())&& this.wordNode != null && this.wordNode.size() > 0 ){
                return this;
            }
        }
        return null;
    }

    public String serialize(){
        String s;
        if(this.wordNode != null && this.wordNode.size() > 0){
            String wordNodeStr = wordNode.toString();
            s = "(" + this.getText() + "+" + "<" + wordNodeStr.substring(1, wordNodeStr.length() - 1) + ">";
        }else{
            s = "{" + this.getText();
        }
        if(textNodes != null){
            s += "[";
            List<Entry<String,CharsNode>> list = new ArrayList<Entry<String,CharsNode>>(textNodes.entrySet());
            for(int i = 0 ; i < list.size(); i++){
                CharsNode textNode = list.get(i).getValue();

                s += textNode.serialize();
                if(i < list.size() - 1){
                    s += ",";
                }
            }

            s += "]";
        }

        s += this.wordNode == null || this.wordNode.size() == 0 ?"}" : ")";

        return s;


    }

}
