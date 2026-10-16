package com.chaos.tablist.text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import com.chaos.tablist.config.TabConfig;

/** Non-destructive feedback: unknown markup stays literal, with a warning in the editor. */
public final class TemplateDiagnostics {
    public record Issue(int column, String key, String detail) {}
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_.:-]+)}");
    private TemplateDiagnostics() {}

    public static Set<String> placeholders(TabConfig config) {
        Set<String> out = new HashSet<>();
        var matcher = PLACEHOLDER.matcher(config.toJson());
        while (matcher.find()) out.add(matcher.group(1).toLowerCase(java.util.Locale.ROOT));
        return Set.copyOf(out);
    }

    public static List<Issue> inspect(String source) {
        List<Issue> issues = new ArrayList<>();
        var stack = new ArrayDeque<String>();
        for (int i = 0; i < source.length() && issues.size() < 8; i++) {
            char ch = source.charAt(i);
            if (ch == '\\') { i++; continue; }
            if (ch == '{') {
                if (source.indexOf('}', i + 1) < 0) issues.add(new Issue(i + 1, "placeholder", "}"));
                continue;
            }
            if (ch != '<') continue;
            int end = source.indexOf('>', i + 1);
            if (end < 0) { issues.add(new Issue(i + 1, "tag_end", ">")); break; }
            String body = source.substring(i + 1, end);
            boolean closing = body.startsWith("/");
            String name = (closing ? body.substring(1) : body).split(":", 2)[0].toLowerCase(java.util.Locale.ROOT);
            name = ChaosText.alias(name);
            if (name.startsWith("#") || ColorUtil.isNamed(name) && !ChaosText.CONTAINERS.contains(name)) name = "color";
            if (closing) {
                if (stack.isEmpty() || !name.isEmpty() && !stack.contains(name)) {
                    issues.add(new Issue(i + 1, "closing", name));
                } else if (name.isEmpty()) stack.pop();
                else { while (!stack.pop().equals(name)) {} }
            } else if (name.equals("reset") || name.equals("r")) stack.clear();
            else if (ChaosText.CONTAINERS.contains(name)) {
                stack.push(name);
                if (stack.size() > ChaosText.MAX_NESTING) { issues.add(new Issue(i + 1, "depth", name)); break; }
            } else if (!ChaosText.SELF_CLOSING.contains(name)) issues.add(new Issue(i + 1, "unknown", name));
            i = end;
        }
        return List.copyOf(issues);
    }

    public static void validate(TabConfig config) {
        List<String> templates = new ArrayList<>(config.header);
        templates.addAll(config.footer);
        templates.add(config.row.prefix + config.row.name + config.row.suffix);
        templates.add(config.ping.format);
        templates.add(config.ping.vanillaSuffix);
        config.players.values().forEach(p -> {
            if (p.prefix != null) templates.add(p.prefix);
            if (p.format != null) templates.add(p.format);
            if (p.suffix != null) templates.add(p.suffix);
        });
        config.animations.values().forEach(a -> templates.addAll(a.frames));
        templates.forEach(ChaosText::parse);
    }
}
