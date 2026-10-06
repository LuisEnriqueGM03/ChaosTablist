package com.chaos.tablist.text;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chaos.tablist.ChaosTablist;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;

/**
 * Anchos y codepoints de la fuente de iconos y badges (los genera tools/gen_font.py en badge_meta.json).
 * El servidor los necesita para montar los badges sin tener la fuente cargada.
 */
public final class Icons {

	/** 11 px de alto: menús. */
	public static final ResourceLocation FONT = ChaosTablist.id("badge");
	/** Mismos codepoints en 9 px: el Tab (sus filas miden 9 px). */
	public static final ResourceLocation FONT_COMPACT = ChaosTablist.id("badge_compact");

	public static final int MAX_LABEL = 24;

	public record Letter(char glyph, int width, char knockout, char boldGlyph, int boldWidth, char boldKnockout) {}

	public record Icon(String id, char glyph, char knockout, int width, String fallback) {}

	private static final Map<Character, Letter> LABEL = new HashMap<>();
	private static final Map<String, Icon> ICONS = new LinkedHashMap<>();
	private static int labelSpace = 3;

	static {
		try (InputStream in = Icons.class.getResourceAsStream("/assets/chaostablist/badge_meta.json")) {
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			labelSpace = root.get("labelSpace").getAsInt();
			for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("label").entrySet()) {
				JsonObject o = e.getValue().getAsJsonObject();
				char c = e.getKey().charAt(0);
				LABEL.put(c, new Letter(c, o.get("w").getAsInt(), (char) o.get("ko").getAsInt(),
						(char) o.get("b").getAsInt(), o.get("bw").getAsInt(), (char) o.get("bko").getAsInt()));
			}
			for (JsonElement el : root.getAsJsonArray("icons")) {
				JsonObject o = el.getAsJsonObject();
				Icon icon = new Icon(o.get("id").getAsString(), (char) o.get("char").getAsInt(),
						(char) o.get("ko").getAsInt(), o.get("width").getAsInt(), o.get("fb").getAsString());
				ICONS.put(icon.id(), icon);
			}
		} catch (Exception e) {
			throw new IllegalStateException("Falta assets/chaostablist/badge_meta.json", e);
		}
	}

	private Icons() {}

	public static int labelSpace() {
		return labelSpace;
	}

	@Nullable
	public static Letter letter(char c) {
		return LABEL.get(c);
	}

	@Nullable
	public static Icon icon(@Nullable String id) {
		return id != null ? ICONS.get(id) : null;
	}

	public static List<Icon> icons() {
		return Collections.unmodifiableList(new ArrayList<>(ICONS.values()));
	}

	/** Mayúsculas, sin tildes y solo caracteres que tiene la fuente. */
	public static String sanitize(String text) {
		String plain = Normalizer.normalize(text.toUpperCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		StringBuilder sb = new StringBuilder();
		for (char c : plain.toCharArray()) {
			if (sb.length() >= MAX_LABEL) {
				break;
			}
			if (c == ' ' || LABEL.containsKey(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
