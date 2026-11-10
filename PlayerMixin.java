package shieldbot.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import shieldbot.FakePlayer;

@Mixin(Player.class)
public abstract class PlayerMixin
{
    /**
     * Vanilla undoes the server-side knockback of a hit player and lets the client apply it instead.
     * Bots have no client, so keep the knockback on the server (FakePlayer.knockback decides how much).
     */
    @Redirect(method = "causeExtraKnockback", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/Entity;hurtMarked:Z", ordinal = 0))
    private boolean shieldbot$keepKnockbackOnBots(Entity target)
    {
        return target.hurtMarked && !(target instanceof FakePlayer);
    }
}
