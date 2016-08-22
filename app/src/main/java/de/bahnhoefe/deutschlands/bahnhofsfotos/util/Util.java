package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

/**
 * Created by android_oma on 29.05.16.
 */

public class Util {
    public static boolean isNumeric(String str){
        for(char c : str.toCharArray()){
            if(!Character.isDigit(c))
                return false;
        }
        return true;
    }
}
