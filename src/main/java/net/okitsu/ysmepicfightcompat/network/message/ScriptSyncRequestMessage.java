package net.okitsu.ysmepicfightcompat.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.okitsu.ysmepicfightcompat.CompatMod;
import net.okitsu.ysmepicfightcompat.network.ScriptSyncValues;
import net.okitsu.ysmepicfightcompat.network.ServerScriptEvents;

/** A client may emit numeric data for its own selected model, not choose an event owner. */
public record ScriptSyncRequestMessage(String modelId, double[] arguments)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ScriptSyncRequestMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    CompatMod.MOD_ID, "script_sync_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScriptSyncRequestMessage>
            STREAM_CODEC = StreamCodec.of(
                    (output, message) -> write(message, output), ScriptSyncRequestMessage::read);

    public ScriptSyncRequestMessage {
        modelId = ScriptSyncValues.modelId(modelId);
        arguments = ScriptSyncValues.arguments(arguments);
    }

    @Override
    public double[] arguments() {
        return arguments.clone();
    }

    public static void write(ScriptSyncRequestMessage message, FriendlyByteBuf output) {
        output.writeUtf(message.modelId(), ScriptSyncValues.MAX_MODEL_ID);
        ScriptSyncValues.write(output, message.arguments);
    }

    public static ScriptSyncRequestMessage read(FriendlyByteBuf input) {
        return new ScriptSyncRequestMessage(input.readUtf(ScriptSyncValues.MAX_MODEL_ID),
                ScriptSyncValues.read(input));
    }

    public static void receive(ScriptSyncRequestMessage message, IPayloadContext context) {
        ServerPlayer sender = context.player() instanceof ServerPlayer serverPlayer
                ? serverPlayer : null;
        if (sender != null && context.flow().isServerbound()) {
            context.enqueueWork(() -> ServerScriptEvents.accept(
                    sender, message.modelId(), message.arguments()));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
