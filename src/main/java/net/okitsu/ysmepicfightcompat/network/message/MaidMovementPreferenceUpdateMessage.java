package net.okitsu.ysmepicfightcompat.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.okitsu.ysmepicfightcompat.CompatMod;
import net.okitsu.ysmepicfightcompat.network.MaidPreferenceBroadcaster;

import java.util.UUID;

/** Owner response containing one movement decision, never its local rule. */
public record MaidMovementPreferenceUpdateMessage(
        UUID queryId,
        int entityId,
        UUID entityUuid,
        UUID policyEpoch,
        long revision,
        boolean ysmMovement)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MaidMovementPreferenceUpdateMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    CompatMod.MOD_ID, "maid_movement_preference_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaidMovementPreferenceUpdateMessage>
            STREAM_CODEC = StreamCodec.of(
                    (output, message) -> write(message, output), MaidMovementPreferenceUpdateMessage::read);

    public MaidMovementPreferenceUpdateMessage {
        if (queryId == null || entityId < 0 || entityUuid == null
                || policyEpoch == null || revision <= 0L) {
            throw new IllegalArgumentException("Invalid maid movement response");
        }
    }

    public static void write(MaidMovementPreferenceUpdateMessage message,
                             FriendlyByteBuf output) {
        output.writeUUID(message.queryId());
        output.writeVarInt(message.entityId());
        output.writeUUID(message.entityUuid());
        output.writeUUID(message.policyEpoch());
        output.writeVarLong(message.revision());
        output.writeBoolean(message.ysmMovement());
    }

    public static MaidMovementPreferenceUpdateMessage read(FriendlyByteBuf input) {
        return new MaidMovementPreferenceUpdateMessage(
                input.readUUID(), input.readVarInt(), input.readUUID(),
                input.readUUID(), input.readVarLong(), input.readBoolean());
    }

    public static void receive(MaidMovementPreferenceUpdateMessage message, IPayloadContext context) {
        ServerPlayer sender = context.player() instanceof ServerPlayer serverPlayer
                ? serverPlayer : null;
        if (sender != null && context.flow().isServerbound()) {
            context.enqueueWork(() -> MaidPreferenceBroadcaster.accept(sender, message));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
