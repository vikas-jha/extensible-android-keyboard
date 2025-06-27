package com.example.transliterator;

public class SymbolNode {

    private String word;
    private String condition;
    private long count;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        if(word == null){
            this.word = null;
        }else{
            this.word = word + "";
        }
    }

	/*public SymbolNode(String word) {
		this.word = word;
	}*/

    public SymbolNode(String word, String condition) {
        this.word = word;
        this.condition = condition;
    }

    @Override
    public String toString(){
        if(this.condition != null){
            return word + ":" + condition;
        }else{
            return this.word;
        }
    }

    public long getCount() {
        return count;
    }

    public void changeCount(long changeCount) {
        this.count += changeCount;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
