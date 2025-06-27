package com.example.transliterator;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vijha on 10/7/2017.
 */

public class TransliteratorManager {

    private static Map<String, String> resourceMap = new HashMap<>();
    private static Map<String, Transliterator> tlMap = new HashMap<>();
    private static TransliteratorManager instance;
    private Transliterator tl;

    private Context context;


    public static TransliteratorManager init(Context context){
        instance = new TransliteratorManager();
        instance.context = context;
        return  instance;
    }

    public static TransliteratorManager getInstance(){
        return instance;
    }

    public boolean load(String id){
        try {
            String[] parts = id.split("\\.");
           InputStream is = context.getResources().getAssets().open("tl/transliterator_" + parts[0]);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            load(br, id);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  false;
    }

    private boolean load(BufferedReader br, String id) throws IOException {
        Transliterator dict = null;
        boolean compiled = false;
        StringBuffer compiledText = new StringBuffer();
        String tagetCodes = id.substring(id.indexOf('.') + 1);

        String line;
        while((line = br.readLine()) != null){
            line = line.trim();
            if(line.length() == 0 || line.startsWith("//")){
                //do nothing. its a comment or empty line.
            }else if(line.startsWith("[")){
                if(compiledText.length() > 0 && dict != null){
                    break;
                }
                compiledText.setLength(0);
                int index = 1;
                if(line.charAt(1)== '#'){
                    compiled = true;
                    compiledText = new StringBuffer();
                    index++;
                }else{
                    compiled = false;
                }
                String codes = line.substring(index, line.indexOf(']'));
                if(codes.equals(tagetCodes)){
                    dict = new Transliterator(codes);
                    tlMap.put(id,dict);
                }

            }else if(dict != null){
                if(compiled){
                    compiledText.append(line.trim());
                }else{
                    compiledText.append(","+line.trim());
                }
            }

        }
        if(compiledText.length() > 0 && dict != null){
            dict.load(compiledText.toString(),compiled);
        }
        compiledText.setLength(0);

        br.close();

        return true;
    }


    public void setLangs(String langs){
        if(langs == null){
            tl = null;
        }else{
            tl = tlMap.get(langs);
        }

    }

    public String getTransliterations(int primaryCode, CharSequence preText){
        if(tl != null){
           return tl.transliterate(primaryCode, preText.toString());
        }
        return preText + String.valueOf(Character.toChars(primaryCode));
    }

    public List<String> getTransliterations(String text){
        List<String> texts = new ArrayList<>();
        if(tl != null){
            texts.addAll(tl.transliterate(text));
        }else{
            texts.add(text);
        }
        return texts;
    }



}
