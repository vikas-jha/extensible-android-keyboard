package com.example.dictionary;

public class WordNode {
	
	private String word;
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

	public WordNode(String word) {
		super();
		if(word == null){
			this.word = null;
		}else{
			this.word = word + "";
		}
	}
	
	@Override
	public String toString(){
		return this.word;
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
}
