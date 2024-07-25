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

    public static void main(String[] args) {
        // Test the getNextChar method
        char testChar1 = '0'; // Should return '1'
        char testChar2 = '9'; // Should return 'a'
        char testChar3 = 'z'; // Should return 'A'
        char testChar4 = 'Z'; // Should return 'Z' (no wrap around)

        System.out.println("Next char for '0': " + getNextChar(testChar1));
        System.out.println("Next char for '9': " + getNextChar(testChar2));
        System.out.println("Next char for 'z': " + getNextChar(testChar3));
        System.out.println("Next char for 'Z': " + getNextChar(testChar4));
    }
}