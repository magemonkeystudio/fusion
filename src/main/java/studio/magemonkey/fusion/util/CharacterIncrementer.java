package studio.magemonkey.fusion.util;

import java.util.HashMap;
import java.util.Map;

public class CharacterIncrementer {

    private static final String CUSTOM_ORDER = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Map<Character, Character> nextCharMap = new HashMap<>();

    static {
        for (int i = 0; i < CUSTOM_ORDER.length() - 1; i++) {
            nextCharMap.put(CUSTOM_ORDER.charAt(i), CUSTOM_ORDER.charAt(i + 1));
        }
    }

    public static char getNextChar(char c) {
        return nextCharMap.getOrDefault(c, c);
    }
}