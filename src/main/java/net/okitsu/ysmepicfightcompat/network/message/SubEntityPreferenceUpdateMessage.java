package net.okitsu.ysmepicfightcompat.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.okitsu.ysmepicfightcompat.CompatMod;
import net.okitsu.ysmepicfightcompat.network.SubEntityModelKind;
import net.okitsu.ysmepicfightcompat.network.SubEntityPreferenceBroadcaster;

import java.util.UUID;

/** Owner-to-server response containing one result, never the owner's local rules. */
public record SubEntityPreferenceUpdateMessage(
        UUID queryId,
        int entityId,
        UUID entityUuid,
        UUID ownerUuid,
        UUID policyEpoch,
        long revision,
        SubEntityModelKind kind,
        boolean ysm)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SubEntityPreferenceUpdateMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    CompatMod.MOD_ID, "sub_entity_preference_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SubEntityPreferenceUpdateMessage>
            STREAM_CODEC = StreamCodec.of(
                    (output, message) -> write(message, output), SubEntityPreferenceUpdateMessage::read);

    public SubEntityPreferenceUpdateMessage {
        if (queryId == null || entityId < 0 || entityUuid == null
                || ownerUuid == null || policyEpoch == null || revision <= 0L
                || kind == null) {
            throw new IllegalArgumentException("Invalid sub-entity preference response");
        }
    }

    public static void write(SubEntityPreferenceUpdateMessage message,
                             FriendlyByteBuf output) {
        output.writeUUID(message.queryId());
        output.writeVarInt(message.entityId());
        output.writeUUID(message.entityUuid());
        output.writeUUID(message.ownerUuid());
        output.writeUUID(message.policyEpoch());
        output.writeVarLong(message.revision());
        output.writeByte(message.kind().ordinal());
        output.writeBoolean(message.ysm());
    }

    public static SubEntityPreferenceUpdateMessage read(FriendlyByteBuf input) {
        return new SubEntityPreferenceUpdateMessage(
                input.readUUID(), input.readVarInt(), input.readUUID(),
                input.readUUID(), input.readUUID(), input.readVarLong(),
                SubEntityModelKind.fromNetworkId(input.readUnsignedByte()),
                input.readBoolean());
    }

    public static void receive(SubEntityPreferenceUpdateMessage message, IPayloadContext context) {
        ServerPlayer sender = context.player() instanceof ServerPlayer serverPlayer
                ? serverPlayer : null;
        if (sender != null && context.flow().isServerbound()) {
            context.enqueueWork(() -> SubEntityPreferenceBroadcaster.accept(sender, message));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
