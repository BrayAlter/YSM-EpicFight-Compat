package net.okitsu.ysmepicfightcompat.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.okitsu.ysmepicfightcompat.CompatMod;
import net.okitsu.ysmepicfightcompat.network.HeldItemModelDisplayState;
import net.okitsu.ysmepicfightcompat.network.HeldItemPreferenceBroadcaster;

/** Client-to-server update containing only the sender's resolved display state. */
public record HeldItemPreferenceUpdateMessage(boolean mainHandYsm,
                                              boolean offHandYsm,
                                              boolean mainHandYsmSwitchAnimation,
                                              boolean offHandYsmSwitchAnimation)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HeldItemPreferenceUpdateMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    CompatMod.MOD_ID, "held_item_preference_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HeldItemPreferenceUpdateMessage>
            STREAM_CODEC = StreamCodec.of(
                    (output, message) -> write(message, output), HeldItemPreferenceUpdateMessage::read);

    public HeldItemPreferenceUpdateMessage(HeldItemModelDisplayState state) {
        this(state.mainHandYsm(), state.offHandYsm(),
                state.mainHandYsmSwitchAnimation(),
                state.offHandYsmSwitchAnimation());
    }

    public static void write(HeldItemPreferenceUpdateMessage message,
                             FriendlyByteBuf output) {
        output.writeBoolean(message.mainHandYsm());
        output.writeBoolean(message.offHandYsm());
        output.writeBoolean(message.mainHandYsmSwitchAnimation());
        output.writeBoolean(message.offHandYsmSwitchAnimation());
    }

    public static HeldItemPreferenceUpdateMessage read(FriendlyByteBuf input) {
        return new HeldItemPreferenceUpdateMessage(
                input.readBoolean(), input.readBoolean(),
                input.readBoolean(), input.readBoolean());
    }

    public static void receive(HeldItemPreferenceUpdateMessage message, IPayloadContext context) {
        ServerPlayer sender = context.player() instanceof ServerPlayer serverPlayer
                ? serverPlayer : null;
        if (sender != null) {
            context.enqueueWork(() -> HeldItemPreferenceBroadcaster.accept(sender,
                    new HeldItemModelDisplayState(message.mainHandYsm(),
                            message.offHandYsm(),
                            message.mainHandYsmSwitchAnimation(),
                            message.offHandYsmSwitchAnimation())));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
