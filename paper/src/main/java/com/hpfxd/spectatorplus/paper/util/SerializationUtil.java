package com.hpfxd.spectatorplus.paper.util;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class SerializationUtil {
    private SerializationUtil() {
    }

    public static UUID readUuid(ByteArrayDataInput buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    public static void writeUuid(ByteArrayDataOutput out, UUID uuid) {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    public static void writeVarInt(ByteArrayDataOutput out, int value) {
        while((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        out.writeByte(value);
    }

    public static void writeItems(ByteArrayDataOutput buf, ItemStack[] items) {
        buf.writeInt(items.length);

        for (final ItemStack item : items) {
            buf.writeBoolean(item != null);

            if (item != null) {
                writeItem(buf, item);
            }
        }
    }

    public static void writeItem(ByteArrayDataOutput buf, ItemStack item) {
        try {
            buf.write(ReflectionUtil.encodeItem(item == null ? ItemStack.empty() : item));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to encode item " + item, e);
        }
    }
}
