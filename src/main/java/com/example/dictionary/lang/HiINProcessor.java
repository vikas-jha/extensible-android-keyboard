package com.example.dictionary.lang;

import java.util.HashMap;
import java.util.Map;

public class HiINProcessor extends LanguageProcessor {
	
	private static Map<Long, Long> diacriticMap = new HashMap<>();
    private static Map<Long, Long> reverseDiacriticMap = new HashMap<>();
	static{
		mapChars(0x093E, 0x0906);
		mapChars(0x093F, 0x0907);
		mapChars(0x0940, 0x0908);
		mapChars(0x0941, 0x0909);
		mapChars(0x0942, 0x090A);
		mapChars(0x0947, 0x090F);
		mapChars(0x0948, 0x0910);
		mapChars(0x094B, 0x0913);
		mapChars(0x094C, 0x0914);
		mapChars(0x093C, 0);
		mapChars(0x094D, 0);
	}

	private static String halant = String.valueOf(Character.toChars(0x094D));
	
	private static void mapChars(long _initial, long _final){

        diacriticMap.put(_initial, _final);
        if(_initial != 0 && _final != 0){
            reverseDiacriticMap.put(_final,_initial);
        }
	}

	public HiINProcessor() {
		langauge = "hi-IN";
	}



    @Override
    public String[] getLanguages() {
        return new String[]{langauge, "hi","sa-IN"};
    }

    @Override
	public boolean isWordCharacter(int codePoint) {
		if((codePoint >= 0x0900 && codePoint <= 0x0963)
				|| codePoint == 0x0970 || codePoint == 46){
			return true;
		}
		return false;
	}
	
	@Override
	public String processKey(CharSequence csq, int keyCode, int position){
		String text = csq.toString();
		Long longCode = Long.valueOf(keyCode);
        int preCharCode = 0;
        boolean done = false;
        if(position > 0){
            preCharCode = text.codePointAt(position - 1);
        }

		if(diacriticMap.get(longCode) != null){
			int value = diacriticMap.get(longCode).intValue();;
				if((position == 0 || csq.length() == 0 )){
					keyCode = value;
				}else if((preCharCode >= 0x0915 && preCharCode <= 0x0939)
						|| preCharCode == 0x093C ){
					
				}else {
                    keyCode = value;
				}

		}else if((preCharCode >= 0x0915 && preCharCode <= 0x0939)){
            if(reverseDiacriticMap.get(longCode) != null){
                keyCode =  reverseDiacriticMap.get(longCode).intValue();
            }else if(longCode == 0x905){
                keyCode =  0x93E;
            }
        }

        StringBuilder sb = new StringBuilder(csq);
        while(true){
           int index = sb.indexOf(".");
            if(index > 0 ){
                int preCode = sb.codePointAt(index - 1);
                if(preCode >= 0x0915 && preCode <= 0x0939){
                    sb.replace(index,index+1, halant);
                }else{
                    sb.replace(index,index+1, "");
                }

            }else{
                break;
            }
        }

        sb.insert(position,String.valueOf(Character.toChars(keyCode)));

        return sb.toString() ;
    }

}
