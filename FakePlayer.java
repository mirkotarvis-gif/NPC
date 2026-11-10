package shieldbot;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * A server-side bot player. Adapted from Carpet's EntityPlayerMPFake (MIT),
 * plus a knockback setting so the bot doesn't hop when you hit it.
 */
@SuppressWarnings("EntityConstructor")
public class FakePlayer extends ServerPlayer
{
    public enum KnockbackMode
    {
        /** Vanilla: pushed back and lifted ~0.4 blocks/tick (the little hop). */
        NORMAL,
        /** Pushed back sideways like vanilla, but never lifted off the ground. Default. */
        NOJUMP,
        /** Doesn't move at all when hit. */
        NONE
    }

    public Runnable fixStartingPosition = () -> {};
    public KnockbackMode knockbackMode = KnockbackMode.NOJUMP;
    /** When true, the bot turns to face the nearest real player every tick. */
    public boolean tracking = false;

    private final ActionPack actionPack;

    public static FakePlayer spawn(String name, MinecraftServer server, Vec3 pos, float yaw, float pitch,
                                   ResourceKey<Level> dimension, GameType gameMode)
    {
        ServerLevel level = server.getLevel(dimension);
        GameProfile profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
        FakePlayer bot = new FakePlayer(server, level, profile, ClientInformation.createDefault());
        bot.fixStartingPosition = () -> bot.snapTo(pos.x, pos.y, pos.z, yaw, pitch);

        server.getPlayerList().placeNewPlayer(new FakeConnection(PacketFlow.SERVERBOUND), bot,
                new CommonListenerCookie(profile, 0, bot.clientInformation(), false));

        bot.stopRiding();
        bot.teleportTo(level, pos.x, pos.y, pos.z, Set.of(), yaw, pitch, true);
        bot.setYHeadRot(yaw);
        bot.setHealth(bot.getMaxHealth());
        bot.unsetRemoved();
        bot.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
        bot.gameMode.changeGameModeForPlayer(gameMode);
        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(bot, (byte) (bot.yHeadRot * 256 / 360)), dimension);
        server.getPlayerList().broadcastAll(ClientboundEntityPositionSyncPacket.of(bot), dimension);
        bot.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f); // show all skin layers
        bot.getAbilities().flying = false;
        return bot;
    }

    private FakePlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation info)
    {
        super(server, level, profile, info);
        this.actionPack = new ActionPack(this);
    }

    public ActionPack getActionPack()
    {
        return actionPack;
    }

    // ---------------------------------------------------------------- knockback

    @Override
    public void knockback(double strength, double x, double z)
    {
        switch (knockbackMode)
        {
            case NONE -> {
                // ignore knockback completely
            }
            case NOJUMP -> {
                double oldY = this.getDeltaMovement().y;
                super.knockback(strength, x, z);
                Vec3 v = this.getDeltaMovement();
                this.setDeltaMovement(v.x, oldY, v.z);
            }
            default -> super.knockback(strength, x, z);
        }
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick()
    {
        if (tracking)
        {
            Player target = nearestRealPlayer();
            if (target != null)
            {
                this.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                this.setYHeadRot(this.getYRot());
            }
        }

        actionPack.onUpdate();

        if (this.level().getServer().getTickCount() % 10 == 0)
        {
            this.connection.resetPosition();
            this.level().getChunkSource().move(this);
        }
        try
        {
            super.tick();
            this.doTick();
        }
        catch (NullPointerException ignored)
        {
        }
    }

    private Player nearestRealPlayer()
    {
        Player best = null;
        double bestDist = 64 * 64;
        for (ServerPlayer p : this.level().players())
        {
            if (p == this || p instanceof FakePlayer || p.isSpectator()) continue;
            double d = p.distanceToSqr(this);
            if (d < bestDist)
            {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void onEquipItem(EquipmentSlot slot, ItemStack previous, ItemStack stack)
    {
        if (!isUsingItem()) super.onEquipItem(slot, previous, stack);
    }

    @Override
    public void kill(ServerLevel level)
    {
        kill(Component.literal("Killed"));
    }

    public void kill(Component reason)
    {
        MinecraftServer server = this.level().getServer();
        server.schedule(new TickTask(server.getTickCount(),
                () -> this.connection.onDisconnect(new DisconnectionDetails(reason))));
    }

    @Override
    public void die(DamageSource cause)
    {
        super.die(cause);
        setHealth(20);
        kill(this.getCombatTracker().getDeathMessage());
    }

    @Override
    public String getIpAddress()
    {
        return "127.0.0.1";
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos)
    {
        doCheckFallDamage(0.0, y, 0.0, onGround);
    }
}
