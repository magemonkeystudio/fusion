package studio.magemonkey.fusion.gui.recipe;

import java.util.*;

/** Immutable layout shared by both recipe GUIs. Contains no player state. */
public final class CompiledPattern {
    private final char[] symbols;
    private final List<SlotRole> roles;
    public CompiledPattern(char[] symbols, List<SlotRole> roles) {
        this.symbols = symbols.clone();
        this.roles = List.copyOf(roles);
    }
    public int size() { return symbols.length; }
    public char symbol(int slot) { return symbols[slot]; }
    public SlotRole role(int slot) { return roles.get(slot); }
    public List<Integer> slots(SlotRole role) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size(); i++) if (roles.get(i) == role) result.add(i);
        return List.copyOf(result);
    }
}
