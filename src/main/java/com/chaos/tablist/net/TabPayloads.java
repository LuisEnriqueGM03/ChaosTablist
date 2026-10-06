package com.chaos.tablist.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.chaos.tablist.ChaosTablist;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Paquetes del mod. La configuración y los valores viajan como JSON comprimido con gzip: la configuración puede
 * ser grande y del cliente al servidor solo caben 32 KB.
 */
public final class TabPayloads {

	/** Límite de un paquete del cliente al servidor (menos un margen para la cabecera). */
	public static final int MAX_C2S = 32_000;
	private static final int MAX_S2C = 1_000_000;

	private TabPayloads() {}

	public static byte[] gzip(String json) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
			out.write(json.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return bytes.toByteArray();
	}

	public static String gunzip(byte[] data) {
		try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
			// Tope de 4 MB descomprimidos: que nadie mande una bomba de gzip.
			byte[] out = in.readNBytes(4 * 1024 * 1024);
			return new String(out, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Servidor -> cliente: la configuración del Tab (al entrar y cada vez que cambia). */
	public record SyncConfig(byte[] data) implements CustomPacketPayload {

		public static final Type<SyncConfig> TYPE = new Type<>(ChaosTablist.id("sync_config"));
		public static final StreamCodec<ByteBuf, SyncConfig> CODEC =
				ByteBufCodecs.byteArray(MAX_S2C).map(SyncConfig::new, SyncConfig::data);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor -> cliente: valores de los placeholders (globales y por jugador), una vez por segundo. */
	public record SyncValues(byte[] data) implements CustomPacketPayload {

		public static final Type<SyncValues> TYPE = new Type<>(ChaosTablist.id("sync_values"));
		public static final StreamCodec<ByteBuf, SyncValues> CODEC =
				ByteBufCodecs.byteArray(MAX_S2C).map(SyncValues::new, SyncValues::data);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor -> cliente: abre el editor con la configuración actual. */
	public record OpenEditor(byte[] data, boolean chaosRanks) implements CustomPacketPayload {

		public static final Type<OpenEditor> TYPE = new Type<>(ChaosTablist.id("open_editor"));
		public static final StreamCodec<ByteBuf, OpenEditor> CODEC = StreamCodec.composite(
				ByteBufCodecs.byteArray(MAX_S2C), OpenEditor::data,
				ByteBufCodecs.BOOL, OpenEditor::chaosRanks,
				OpenEditor::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor -> cliente: un icono PNG propio. Nombre "*" y sin datos = borrar todos. */
	public record IconData(String name, byte[] png) implements CustomPacketPayload {

		public static final Type<IconData> TYPE = new Type<>(ChaosTablist.id("icon_data"));
		public static final StreamCodec<ByteBuf, IconData> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(64), IconData::name,
				ByteBufCodecs.byteArray(MAX_S2C), IconData::png,
				IconData::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente -> servidor: guardar la configuración desde el editor. */
	public record SaveConfig(byte[] data) implements CustomPacketPayload {

		public static final Type<SaveConfig> TYPE = new Type<>(ChaosTablist.id("save_config"));
		public static final StreamCodec<ByteBuf, SaveConfig> CODEC =
				ByteBufCodecs.byteArray(MAX_C2S).map(SaveConfig::new, SaveConfig::data);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
