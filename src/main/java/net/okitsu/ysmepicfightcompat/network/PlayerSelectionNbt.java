package net.okitsu.ysmepicfightcompat.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Reads the stable serialized contract of official YSM's player capability. */
public final class PlayerSelectionNbt {
    /** NeoForge 1.21.1 serializes data attachments under this root key. */
    private static final String NEOFORGE_ATTACHMENTS = "neoforge:attachments";
    /** Legacy 1.20.1 Forge capability root, kept as a fallback for migrated data. */
    private static final String FORGE_CAPS = "ForgeCaps";
    private static final String YSM_SELECTION = "yes_steve_model:model_id";

    public record Selection(String modelId, String textureName) {
    }

    private PlayerSelectionNbt() {
    }

    public static Selection read(Player player) {
        if (player == null) {
            return null;
        }
        try {
            return parse(player.saveWithoutId(new CompoundTag()));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    static Selection parse(CompoundTag root) {
        if (root == null) {
            return null;
        }
        Selection selection = parseContainer(root, NEOFORGE_ATTACHMENTS);
        return selection != null ? selection : parseContainer(root, FORGE_CAPS);
    }

    private static Selection parseContainer(CompoundTag root, String containerKey) {
        if (!root.contains(containerKey, CompoundTag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag capabilities = root.getCompound(containerKey);
        if (!capabilities.contains(YSM_SELECTION, CompoundTag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag selected = capabilities.getCompound(YSM_SELECTION);
        if (selected.getBoolean("disabled")
                || !selected.contains("model_id", CompoundTag.TAG_STRING)
                || !selected.contains("select_texture", CompoundTag.TAG_STRING)) {
            return null;
        }
        String model = selected.getString("model_id");
        return model.isEmpty() ? null
                : new Selection(model, selected.getString("select_texture"));
    }
}
