package net.okitsu.ysmepicfightcompat.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.okitsu.ysmepicfightcompat.CompatMod;
import net.okitsu.ysmepicfightcompat.animation.OfficialConfigurationVariables;
import net.okitsu.ysmepicfightcompat.network.ConfigurationVariableValues;

import java.util.Map;
import java.util.UUID;

/** Server-authoritative session snapshot for one player's ordinary configuration values. */
public record ConfigurationVariableSnapshotMessage(UUID playerId, String modelId,
                                                   Map<String, Double> values)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConfigurationVariableSnapshotMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    CompatMod.MOD_ID, "configuration_variable_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigurationVariableSnapshotMessage>
            STREAM_CODEC = StreamCodec.of(
                    (output, message) -> write(message, output), ConfigurationVariableSnapshotMessage::read);

    private static final int MAX_MODEL_ID_LENGTH = 4096;

    public ConfigurationVariableSnapshotMessage {
        if (playerId == null || modelId == null
                || modelId.length() > MAX_MODEL_ID_LENGTH) {
            throw new IllegalArgumentException("Invalid configuration-variable snapshot");
        }
        values = ConfigurationVariableValues.validate(values);
    }

    public static void write(ConfigurationVariableSnapshotMessage message,
                             FriendlyByteBuf output) {
        output.writeUUID(message.playerId());
        output.writeUtf(message.modelId(), MAX_MODEL_ID_LENGTH);
        ConfigurationVariableValues.write(output, message.values());
    }

    public static ConfigurationVariableSnapshotMessage read(FriendlyByteBuf input) {
        return new ConfigurationVariableSnapshotMessage(input.readUUID(),
                input.readUtf(MAX_MODEL_ID_LENGTH), ConfigurationVariableValues.read(input));
    }

    public static void receive(ConfigurationVariableSnapshotMessage message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> OfficialConfigurationVariables.acceptSnapshot(
                    message.playerId(), message.modelId(), message.values()));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
