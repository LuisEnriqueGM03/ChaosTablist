package com.chaos.tablist.command;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.config.TabStore;
import com.chaos.tablist.server.ServerContext;
import com.chaos.tablist.server.ServerValues;
import com.chaos.tablist.server.TabServer;
import com.chaos.tablist.Lang;
import com.chaos.tablist.text.Evaluator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /tablist                 abre el editor (necesita el mod en el cliente)
 * /tablist reload          vuelve a leer tablist.json y los iconos
 * /tablist toggle          activa o desactiva el Tab personalizado
 * /tablist preview &lt;texto&gt; evalúa una plantilla y la muestra en el chat
 * /tablist player &lt;jugadores&gt; prefix|format|suffix &lt;texto&gt; · reset · hide · show
 */
public final class TabCommand {

	private TabCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tablist")
				.requires(source -> source.hasPermission(TabServer.PERMISSION))
				.executes(ctx -> {
					TabServer.open(ctx.getSource().getPlayerOrException());
					return 1;
				})
				.then(Commands.literal("reload").executes(ctx -> {
					TabServer.reload(ctx.getSource().getServer());
					ctx.getSource().sendSuccess(() -> Lang.tr("msg.reloaded"), true);
					return 1;
				}))
				.then(Commands.literal("toggle").executes(ctx -> {
					TabConfig config = TabStore.config().copy();
					config.enabled = !config.enabled;
					TabServer.apply(ctx.getSource().getServer(), config);
					ctx.getSource().sendSuccess(() -> Lang.tr("msg.toggled", Lang.yesNo(config.enabled)), true);
					return 1;
				}))
				.then(Commands.literal("preview")
						.then(Commands.argument("text", StringArgumentType.greedyString()).executes(TabCommand::preview)))
				.then(Commands.literal("player").then(Commands.argument("targets", EntityArgument.players())
						.then(field("prefix", (o, text) -> o.prefix = text))
						.then(field("format", (o, text) -> o.format = text))
						.then(field("suffix", (o, text) -> o.suffix = text))
						.then(Commands.literal("hide").executes(ctx -> edit(ctx, o -> o.hidden = true)))
						.then(Commands.literal("show").executes(ctx -> edit(ctx, o -> o.hidden = false)))
						.then(Commands.literal("reset").executes(ctx -> reset(ctx))))));
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> field(String name,
			BiConsumer<TabConfig.PlayerOverride, String> setter) {
		return Commands.literal(name).then(Commands.argument("text", StringArgumentType.greedyString())
				.executes(ctx -> edit(ctx, o -> setter.accept(o, StringArgumentType.getString(ctx, "text")))));
	}

	private static int edit(CommandContext<CommandSourceStack> ctx, java.util.function.Consumer<TabConfig.PlayerOverride> change)
			throws CommandSyntaxException {
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
		TabConfig config = TabStore.config().copy();
		for (ServerPlayer p : targets) {
			TabConfig.PlayerOverride o = config.players.computeIfAbsent(p.getStringUUID(), k -> new TabConfig.PlayerOverride());
			o.name = p.getScoreboardName();
			change.accept(o);
		}
		TabServer.apply(ctx.getSource().getServer(), config);
		ctx.getSource().sendSuccess(() -> Lang.tr("msg.player_updated", targets.size()), true);
		return targets.size();
	}

	private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
		TabConfig config = TabStore.config().copy();
		targets.forEach(p -> config.players.remove(p.getStringUUID()));
		TabServer.apply(ctx.getSource().getServer(), config);
		ctx.getSource().sendSuccess(() -> Lang.tr("msg.player_updated", targets.size()), true);
		return targets.size();
	}

	private static int preview(CommandContext<CommandSourceStack> ctx) {
		String text = StringArgumentType.getString(ctx, "text");
		ServerPlayer player = ctx.getSource().getPlayer();
		Map<String, String> values = player == null ? Map.of() : ServerValues.player(player.getUUID());
		ServerContext context = new ServerContext(TabStore.config(), values, values,
				ctx.getSource().getServer().registryAccess(), ServerContext.now());
		Component result = Evaluator.toComponent(Evaluator.eval(text, context));
		ctx.getSource().sendSuccess(() -> result, false);
		return 1;
	}
}
