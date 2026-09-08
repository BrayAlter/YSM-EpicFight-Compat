package net.okitsu.ysmepicfightcompat.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.okitsu.ysmepicfightcompat.CompatMod;
import net.okitsu.ysmepicfightcompat.animation.ClientShieldBlockState;

import java.util.UUID;

/** A server-observed successful shield block for one exact entity identity. */
public record ShieldBlockMessage(int entityId, UUID entityUuid)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShieldBlockMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    CompatMod.MOD_ID, "shield_block"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldBlockMessage>
            STREAM_CODEC = StreamCodec.of(
                    (output, message) -> write(message, output), ShieldBlockMessage::read);

    public ShieldBlockMessage {
        if (entityId < 0 || entityUuid == null) {
            throw new IllegalArgumentException("Invalid shield block identity");
        }
    }

    public boolean matchesIdentity(int candidateId, UUID candidateUuid) {
        return entityId == candidateId && entityUuid.equals(candidateUuid);
    }

    public static void write(ShieldBlockMessage message, FriendlyByteBuf output) {
        output.writeVarInt(message.entityId());
        output.writeUUID(message.entityUuid());
    }

    public static ShieldBlockMessage read(FriendlyByteBuf input) {
        return new ShieldBlockMessage(input.readVarInt(), input.readUUID());
    }

    public static void receive(ShieldBlockMessage message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> ClientShieldBlockState.receive(message));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
