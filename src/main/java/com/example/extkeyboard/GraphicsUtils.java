package com.example.extkeyboard;

/**
 * Created by vijha on 10/2/2017.
 */

public class GraphicsUtils {

    public static float getBrightness(int color){
        int r = (color & 0xff0000) >> 16, g = (color & 0x00ff00)>> 8, b = (color & 0x0000ff);
        float brightness = (float) ((r * 0.3 + g * 0.59 + b * 0.11)/255);
        return brightness;
    }

    public static int getComplementaryShade(int color){
       if(getBrightness(color) > 0.5){
           return 0xff333333;
       }else{
           return 0xffcccccc;
       }
    }

    public static int getMiddleColor(int color1, int color2){
        int r = (color1 & 0xff) + (color2 & 0xff);
        int g = (color1 >> 8 & 0xff) + (color2 >> 8 & 0xff);
        int b = (color1 >> 16 & 0xff) + (color2 >> 16 & 0xff);
        int a = (color1 >> 24 & 0xff) + (color2 >> 24 & 0xff);
        int color = a << 23 | b << 15 | g << 7 | r;
        return color;
    }

}
