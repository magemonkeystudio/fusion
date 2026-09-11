package studio.magemonkey.fusion.gui.recipe;

import java.util.*;
import studio.magemonkey.fusion.data.professions.pattern.InventoryPattern;

public final class PatternCompiler {
    private static final Map<Character, SlotRole> DEFAULTS = Map.of(
            'o', SlotRole.RECIPE_LIST, '=', SlotRole.RECIPE_LIST, '-', SlotRole.QUEUE_LIST,
            '<', SlotRole.PREVIOUS_PAGE, '>', SlotRole.NEXT_PAGE,
            '{', SlotRole.PREVIOUS_QUEUE_PAGE, '}', SlotRole.NEXT_QUEUE_PAGE);
    private PatternCompiler() { }

    public static CompiledPattern compile(InventoryPattern pattern) {
        return compile(pattern.getPattern(), pattern.getRoles());
    }

    public static CompiledPattern compile(String[] rows, Map<Character, SlotRole> overrides) {
        if (rows == null || rows.length < 1 || rows.length > 6)
            throw new IllegalArgumentException("Recipe pattern must contain 1 to 6 rows");
        Map<Character, SlotRole> bindings = new HashMap<>(DEFAULTS);
        bindings.putAll(overrides);
        char[] symbols = new char[rows.length * 9];
        List<SlotRole> roles = new ArrayList<>();
        for (int row = 0; row < rows.length; row++) {
            if (rows[row] == null || rows[row].length() != 9)
                throw new IllegalArgumentException("Pattern row " + (row + 1) + " must contain exactly 9 characters");
            for (int column = 0; column < 9; column++) {
                char symbol = rows[row].charAt(column);
                symbols[row * 9 + column] = symbol;
                roles.add(bindings.getOrDefault(symbol, SlotRole.DECORATION));
            }
        }
        if (!roles.contains(SlotRole.RECIPE_LIST))
            throw new IllegalArgumentException("Recipe pattern needs at least one recipe-list slot");
        if (!roles.contains(SlotRole.QUEUE_LIST) && (roles.contains(SlotRole.PREVIOUS_QUEUE_PAGE)
                || roles.contains(SlotRole.NEXT_QUEUE_PAGE)))
            throw new IllegalArgumentException("Queue navigation requires queue-list slots");
        return new CompiledPattern(symbols, roles);
    }
}
