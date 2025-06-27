package com.example.dictionary;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;

import com.example.dictionary.lang.EnUSProcessor;
import com.example.dictionary.lang.HiINProcessor;
import com.example.dictionary.lang.KnINProcessor;
import com.example.dictionary.lang.LanguageProcessor;
import com.example.dictionary.lang.KoKRProcessor;
import com.example.dictionary.lang.MaiInProcessor;
import com.example.dictionary.lang.ZhCNProcessor;
import com.example.extkeyboard.KeyBoardsManager;
import com.example.extkeyboard.Util;
import com.example.extkeyboard.internal.Constants;

public class DictionaryManager {
	
	private Map<String, Dictionary> dictionaries = new HashMap<>();
	private static Map<String, LanguageProcessor> langProcessors = new HashMap<>(); 
	static{
		registerLanguageProcessor(EnUSProcessor.class);
		registerLanguageProcessor(HiINProcessor.class);
		registerLanguageProcessor(KnINProcessor.class);
		registerLanguageProcessor(KoKRProcessor.class);
        registerLanguageProcessor(ZhCNProcessor.class);
        registerLanguageProcessor(MaiInProcessor.class);
	}
	
	@SuppressLint("DefaultLocale") 
	private static void registerLanguageProcessor(Class<? extends LanguageProcessor> lpClass){
		try {
			LanguageProcessor lp = lpClass.newInstance();
            for(String language: lp.getLanguages()){
                if(langProcessors.get(language) == null){
                    langProcessors.put(language, lp);
                }
            }

		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	private Context context;

	public DictionaryManager(Context context) {
		super();
		this.context = context;
	}
	
	
	public Dictionary getDictionary(String lang){
		if(lang != null){
			Dictionary dictionary = dictionaries.get(lang);
			if(dictionary != null){
				return dictionary;
			}else{
				return loadDictionary(lang);
			}
		}
		return null;
	}
	
	public synchronized void saveDictionaries(){
		for(String lang : dictionaries.keySet()){
			Dictionary dictionary = dictionaries.get(lang);
			if(dictionary.isModified()){
				String text = dictionary.serialize();
				String fileName = getDictionaryFileName(dictionary);
				try {
					OutputStream os = context.openFileOutput(fileName, Context.MODE_PRIVATE);
					os.write(text.getBytes());
                    dictionary.onSaved();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
		}
	}
	
	public LanguageProcessor getLanguageProcessor(String lang){
		if(lang != null){
			lang = lang.replace("_", "-");
			return langProcessors.get(lang);
		}
		return null;
	}
	
	public void insertWord(String word, String lang){
		if(word == null || word.length() < Constants.DICT_MIN_WORD_LENGTH || lang == null){
			return;
		}

		lang = lang.replace("_", "-");
		
		Dictionary dictionary = dictionaries.get(lang);
		
		LanguageProcessor lp;
		if(dictionary != null && !dictionary.isLoading()){
			if(dictionary.contains(word)){
				dictionary.addWord(word);
			}else if((lp = getLanguageProcessor(lang)) != null){
				for(int i = 0 ; i < word.length() ; i++){
					if(!lp.isWordCharacter(word.codePointAt(i))){
						return;
					}
					dictionary.addWord(word);
				}
			}
				
		}
	}
	
	private synchronized Dictionary loadDictionary(String lang){
        Dictionary dictionary = dictionaries.get(lang);
        if(dictionary == null){
            dictionary = new Dictionary(new Locale(lang));
            dictionaries.put(lang, dictionary);
            new Thread(new DictionaryLoader(dictionary)).start();
        }

		return dictionary;
	}

    private String getDictionaryFileName(Dictionary dictionary){
        return "dictionary_" + dictionary.getLocale().getLanguage().toLowerCase().replace("-","_");
    }
	
	private class DictionaryLoader implements Runnable{

		private final Dictionary dictionary;
		
		public DictionaryLoader(Dictionary dictionary) {
			this.dictionary = dictionary;
		}

		@Override
		public void run() {
			try {
				byte[] buffer = new byte[10*1024];
				StringBuilder sb = new StringBuilder();
				
				InputStream is = getDictionaryFile(dictionary);
                int length = 0;
				if(is != null){

					while((length = is.read(buffer)) > -1){
						sb.append(new String(buffer, 0, length, "UTF-8"));
					}
					is.close();
					dictionary.load(sb.toString());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		private InputStream getDictionaryFile(Dictionary dictionary) throws IOException{
			String fileName = getDictionaryFileName(dictionary);
			InputStream is = null;
			try {
				is = context.openFileInput(fileName);
			} catch (FileNotFoundException e) {
				is = context.getResources().getAssets().open(fileName);
				OutputStream os = context.openFileOutput(fileName, Context.MODE_PRIVATE); 
				Util.copy(is, os, true);
				is = context.openFileInput(fileName);
			}
			
			return is;
		}
		
	}


	
	public List<String> getSuggestions(String composing){
		Dictionary dictionary = getDictionary(KeyBoardsManager.getInstance().getCurrentHandler().getLanguage());
		if (dictionary != null) {
			return dictionary.predict(composing);
		}else{
			return new ArrayList<>();
		}
	}

    public List<String> getSuggestions(List<String> texts){
        Dictionary dictionary = getDictionary(KeyBoardsManager.getInstance().getCurrentHandler().getLanguage());
        if (dictionary != null) {
            return dictionary.predict(texts);
        }else{
            return new ArrayList<>();
        }
    }

}
