package shieldbot;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import shieldbot.ActionPack.Action;
import shieldbot.ActionPack.ActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class PlayerCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> root = literal("player")
                // Shortcut: /player spawn <name> [in <gamemode>]
                .then(literal("spawn")
                        .then(argument("player", StringArgumentType.word())
                                .executes(PlayerCommand::spawn)
                                .then(literal("in").then(argument("gamemode", GameModeArgument.gameMode())
                                        .executes(PlayerCommand::spawn)))))
                // Carpet style: /player <name> <action>
                .then(argument("player", StringArgumentType.word())
                        .suggests((c, b) -> suggest(botNames(c.getSource()), b))
                        .then(literal("spawn").executes(PlayerCommand::spawn)
                                .then(literal("in").then(argument("gamemode", GameModeArgument.gameMode())
                                        .executes(PlayerCommand::spawn)))
                                .then(literal("at").then(argument("position", Vec3Argument.vec3())
                                        .executes(PlayerCommand::spawn)
                                        .then(literal("facing").then(argument("direction", RotationArgument.rotation())
                                                .executes(PlayerCommand::spawn))))))
                        .then(literal("kill").executes(PlayerCommand::kill))
                        .then(literal("stop").executes(manipulation(ActionPack::stopAll)))
                        .then(makeActionCommand("use", ActionType.USE))
                        .then(makeActionCommand("attack", ActionType.ATTACK))
                        .then(makeActionCommand("jump", ActionType.JUMP))
                        .then(makeActionCommand("drop", ActionType.DROP_ITEM))
                        .then(makeActionCommand("dropStack", ActionType.DROP_STACK))
                        .then(makeActionCommand("swapHands", ActionType.SWAP_HANDS))
                        .then(literal("hotbar").then(argument("slot", IntegerArgumentType.integer(1, 9))
                                .executes(c -> manipulate(c, ap -> ap.setSlot(IntegerArgumentType.getInteger(c, "slot"))))))
                        .then(literal("sneak").executes(manipulation(ap -> ap.setSneaking(true))))
                        .then(literal("unsneak").executes(manipulation(ap -> ap.setSneaking(false))))
                        .then(literal("sprint").executes(manipulation(ap -> ap.setSprinting(true))))
                        .then(literal("unsprint").executes(manipulation(ap -> ap.setSprinting(false))))
                        .then(literal("move").executes(manipulation(ActionPack::stopMovement))
                                .then(literal("forward").executes(manipulation(ap -> ap.setForward(1))))
                                .then(literal("backward").executes(manipulation(ap -> ap.setForward(-1))))
                                .then(literal("left").executes(manipulation(ap -> ap.setStrafing(1))))
                                .then(literal("right").executes(manipulation(ap -> ap.setStrafing(-1)))))
                        .then(literal("look")
                                .then(literal("north").executes(look(ap -> ap.look(Direction.NORTH))))
                                .then(literal("south").executes(look(ap -> ap.look(Direction.SOUTH))))
                                .then(literal("east").executes(look(ap -> ap.look(Direction.EAST))))
                                .then(literal("west").executes(look(ap -> ap.look(Direction.WEST))))
                                .then(literal("up").executes(look(ap -> ap.look(Direction.UP))))
                                .then(literal("down").executes(look(ap -> ap.look(Direction.DOWN))))
                                .then(literal("me").executes(PlayerCommand::lookAtMe))
                                .then(literal("at").then(argument("position", Vec3Argument.vec3())
                                        .executes(c -> lookCmd(c, ap -> ap.lookAt(Vec3Argument.getVec3(c, "position"))))))
                                .then(argument("direction", RotationArgument.rotation())
                                        .executes(c -> lookCmd(c, ap -> ap.look(RotationArgument.getRotation(c, "direction").getRotation(c.getSource()))))))
                        .then(literal("turn")
                                .then(literal("left").executes(look(ap -> ap.turn(-90, 0))))
                                .then(literal("right").executes(look(ap -> ap.turn(90, 0))))
                                .then(literal("back").executes(look(ap -> ap.turn(180, 0))))
                                .then(argument("rotation", RotationArgument.rotation())
                                        .executes(c -> lookCmd(c, ap -> ap.turn(RotationArgument.getRotation(c, "rotation").getRotation(c.getSource()))))))
                        .then(literal("track").executes(c -> setTracking(c, true)))
                        .then(literal("untrack").executes(c -> setTracking(c, false)))
                        .then(literal("knockback")
                                .then(literal("normal").executes(c -> setKnockback(c, FakePlayer.KnockbackMode.NORMAL)))
                                .then(literal("nojump").executes(c -> setKnockback(c, FakePlayer.KnockbackMode.NOJUMP)))
                                .then(literal("none").executes(c -> setKnockback(c, FakePlayer.KnockbackMode.NONE))))
                        .then(literal("heal").executes(PlayerCommand::heal))
                );
        dispatcher.register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeActionCommand(String name, ActionType type)
    {
        return literal(name)
                .executes(manipulation(ap -> ap.start(type, Action.once())))
                .then(literal("once").executes(manipulation(ap -> ap.start(type, Action.once()))))
                .then(literal("continuous").executes(manipulation(ap -> ap.start(type, Action.continuous()))))
                .then(literal("interval").then(argument("ticks", IntegerArgumentType.integer(1))
                        .executes(c -> manipulate(c, ap -> ap.start(type, Action.interval(IntegerArgumentType.getInteger(c, "ticks")))))));
    }

    // ------------------------------------------------------------------ helpers

    private static List<String> botNames(CommandSourceStack source)
    {
        List<String> names = new ArrayList<>();
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers())
        {
            if (p instanceof FakePlayer) names.add(p.getScoreboardName());
        }
        if (names.isEmpty()) names.add("Bot");
        return names;
    }

    private static FakePlayer getBot(CommandContext<CommandSourceStack> c)
    {
        String name = StringArgumentType.getString(c, "player");
        ServerPlayer p = c.getSource().getServer().getPlayerList().getPlayerByName(name);
        if (p instanceof FakePlayer bot) return bot;
        c.getSource().sendFailure(Component.literal(p == null
                ? "No bot called " + name + ". Spawn one with /player spawn " + name
                : name + " is a real player, not a bot"));
        return null;
    }

    private static void say(CommandSourceStack source, String msg)
    {
        source.sendSuccess(() -> Component.literal(msg), false);
    }

    private static int manipulate(CommandContext<CommandSourceStack> c, Consumer<ActionPack> action)
    {
        FakePlayer bot = getBot(c);
        if (bot == null) return 0;
        action.accept(bot.getActionPack());
        return 1;
    }

    private static Command<CommandSourceStack> manipulation(Consumer<ActionPack> action)
    {
        return c -> manipulate(c, action);
    }

    /** Manual look commands switch tracking off so they actually stick. */
    private static int lookCmd(CommandContext<CommandSourceStack> c, Consumer<ActionPack> action)
    {
        FakePlayer bot = getBot(c);
        if (bot == null) return 0;
        bot.tracking = false;
        action.accept(bot.getActionPack());
        return 1;
    }

    private static Command<CommandSourceStack> look(Consumer<ActionPack> action)
    {
        return c -> lookCmd(c, action);
    }

    // ------------------------------------------------------------------ commands

    private static int spawn(CommandContext<CommandSourceStack> c) throws CommandSyntaxException
    {
        CommandSourceStack source = c.getSource();
        MinecraftServer server = source.getServer();
        String name = StringArgumentType.getString(c, "player");

        if (name.length() > 16)
        {
            source.sendFailure(Component.literal("Name is too long (max 16 characters)"));
            return 0;
        }
        if (server.getPlayerList().getPlayerByName(name) != null)
        {
            source.sendFailure(Component.literal(name + " is already in the world"));
            return 0;
        }

        Vec3 pos = source.getPosition();
        Vec2 rot = source.getRotation();
        try { pos = Vec3Argument.getVec3(c, "position"); } catch (IllegalArgumentException ignored) {}
        try { rot = RotationArgument.getRotation(c, "direction").getRotation(source); } catch (IllegalArgumentException ignored) {}

        GameType mode = GameType.SURVIVAL; // survival so it can take hits and block with a shield
        try { mode = GameModeArgument.getGameMode(c, "gamemode"); } catch (IllegalArgumentException ignored) {}

        if (!Level.isInSpawnableBounds(BlockPos.containing(pos)))
        {
            source.sendFailure(Component.literal("Can't spawn a bot outside the world"));
            return 0;
        }

        FakePlayer.spawn(name, server, pos, rot.y, rot.x, source.getLevel().dimension(), mode);
        say(source, "Spawned " + name + ". Drop it a shield, then: /player " + name + " use continuous");
        return 1;
    }

    private static int kill(CommandContext<CommandSourceStack> c)
    {
        FakePlayer bot = getBot(c);
        if (bot == null) return 0;
        bot.kill(bot.level());
        return 1;
    }

    private static int lookAtMe(CommandContext<CommandSourceStack> c)
    {
        FakePlayer bot = getBot(c);
        if (bot == null) return 0;
        Entity me = c.getSource().getEntity();
        if (me == null)
        {
            c.getSource().sendFailure(Component.literal("Only a player can use 'look me'"));
            return 0;
        }
        bot.tracking = false;
        bot.getActionPack().lookAt(me.getEyePosition());
        return 1;
    }

    private static int setTracking(CommandContext<CommandSourceStack> c, boolean on)
    {
        FakePlayer bot = getBot(c);
        if (bot == null) return 0;
        bot.tracking = on;
        say(c.getSource(), on ? bot.getScoreboardName() + " will keep facing you" : bot.getScoreboardName() + " stopped tracking");
        return 1;
    }

    private static int setKnockback(CommandContext<CommandSourceStack> c, FakePlayer.KnockbackMode mode)
    {
        FakePlayer bot = getBot(c);
        if (bot == null) return 0;
        bot.knockbackMode = mode;
        say(c.getSource(), bot.getScoreboardName() + " knockback: " + mode.name().toLowerCase());
        return 1;
    }

    private static int heal(CommandContext<CommandSourceStack> c)
    {
        FakePlayer bot = getBot(c);
        if (bot == null) return 0;
        bot.setHealth(bot.getMaxHealth());
        bot.getFoodData().setFoodLevel(20);
        say(c.getSource(), "Healed " + bot.getScoreboardName());
        return 1;
    }
}
