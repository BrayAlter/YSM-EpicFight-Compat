package net.okitsu.ysmepicfightcompat.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.okitsu.ysmepicfightcompat.CompatMod;
import net.okitsu.ysmepicfightcompat.network.ConfigurationVariableBroadcaster;
import net.okitsu.ysmepicfightcompat.network.ConfigurationVariableValues;

import java.util.Map;

/** Client delta for ordinary v.* values written by an official YSM configuration action. */
public record ConfigurationVariableUpdateMessage(Map<String, Double> changes)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConfigurationVariableUpdateMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    CompatMod.MOD_ID, "configuration_variable_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigurationVariableUpdateMessage>
            STREAM_CODEC = StreamCodec.of(
                    (output, message) -> write(message, output), ConfigurationVariableUpdateMessage::read);

    public ConfigurationVariableUpdateMessage {
        changes = ConfigurationVariableValues.validate(changes);
    }

    public static void write(ConfigurationVariableUpdateMessage message,
                             FriendlyByteBuf output) {
        ConfigurationVariableValues.write(output, message.changes());
    }

    public static ConfigurationVariableUpdateMessage read(FriendlyByteBuf input) {
        return new ConfigurationVariableUpdateMessage(ConfigurationVariableValues.read(input));
    }

    public static void receive(ConfigurationVariableUpdateMessage message, IPayloadContext context) {
        ServerPlayer sender = context.player() instanceof ServerPlayer serverPlayer
                ? serverPlayer : null;
        if (sender != null && context.flow().isServerbound()) {
            context.enqueueWork(() -> ConfigurationVariableBroadcaster.accept(
                    sender, message.changes()));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
