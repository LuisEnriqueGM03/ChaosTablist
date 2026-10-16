package com.chaos.tablist.client.editor;

import java.util.ArrayDeque;
import java.util.Deque;

/** Bounded undo/redo with a single entry for a continuous burst of typing. */
public final class EditorHistory {
    private static final int MAX = 100;
    private final Deque<String> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private String stable;
    private boolean burst;
    private long lastChange;
    public EditorHistory(String json) { stable = json; }

    private static void push(Deque<String> stack, String json) {
        if (!json.equals(stack.peek())) {
            stack.push(json);
            while (stack.size() > MAX) stack.removeLast();
        }
    }
    public void changed(long now) {
        if (!burst) { push(undo, stable); redo.clear(); burst = true; }
        lastChange = now;
    }
    public void checkpoint(String current) {
        push(undo, burst ? stable : current);
        if (burst) push(undo, current);
        redo.clear();
    }
    public void settle(String current) { stable = current; burst = false; }
    public boolean settling(long now) { return burst && now - lastChange > 700; }
    public String undo(String current, boolean forward) {
        Deque<String> from = forward ? redo : undo, to = forward ? undo : redo;
        while (!from.isEmpty()) {
            String json = from.pop();
            if (!json.equals(current)) { push(to, current); settle(json); return json; }
        }
        return null;
    }
}
