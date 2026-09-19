package nel.riposte.network;

import nel.riposte.Riposte;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class RipostePayloads {
    public record ParrySyncPayload() implements CustomPayload {
        public static final CustomPayload.Id<ParrySyncPayload> ID = new CustomPayload.Id<>(Identifier.of(Riposte.MOD_ID, "parry_sync"));
        public static final PacketCodec<RegistryByteBuf, ParrySyncPayload> CODEC = CustomPayload.codecOf(
                (payload, buf) -> {},
                buf -> new ParrySyncPayload()
        );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ExecuteFinisherPayload(int targetId) implements CustomPayload {
        public static final CustomPayload.Id<ExecuteFinisherPayload> ID = new CustomPayload.Id<>(Identifier.of(Riposte.MOD_ID, "execute_finisher"));
        public static final PacketCodec<RegistryByteBuf, ExecuteFinisherPayload> CODEC = CustomPayload.codecOf(
                (payload, buf) -> buf.writeInt(payload.targetId()),
                buf -> new ExecuteFinisherPayload(buf.readInt())
        );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }
    public record ParrySuccessPayload(boolean isFallParry, int targetId, float gauge) implements CustomPayload {
        public static final CustomPayload.Id<ParrySuccessPayload> ID = new CustomPayload.Id<>(Identifier.of(Riposte.MOD_ID, "parry_success"));
        public static final PacketCodec<RegistryByteBuf, ParrySuccessPayload> CODEC = CustomPayload.codecOf(
                (payload, buf) -> {
                    buf.writeBoolean(payload.isFallParry());
                    buf.writeInt(payload.targetId());
                    buf.writeFloat(payload.gauge());
                },
                buf -> new ParrySuccessPayload(buf.readBoolean(), buf.readInt(), buf.readFloat())
        );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ComboSuccessPayload() implements CustomPayload {
        public static final CustomPayload.Id<ComboSuccessPayload> ID = new CustomPayload.Id<>(Identifier.of(Riposte.MOD_ID, "combo_success"));
        public static final PacketCodec<RegistryByteBuf, ComboSuccessPayload> CODEC = CustomPayload.codecOf(
                (payload, buf) -> {},
                buf -> new ComboSuccessPayload()
        );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ParryVfxPayload(double x, double y, double z, float yaw, boolean isWeapon, boolean isHeavyDamage) implements CustomPayload {
        public static final CustomPayload.Id<ParryVfxPayload> ID = new CustomPayload.Id<>(Identifier.of(Riposte.MOD_ID, "parry_vfx"));
        public static final PacketCodec<RegistryByteBuf, ParryVfxPayload> CODEC = CustomPayload.codecOf(
                (payload, buf) -> {
                    buf.writeDouble(payload.x());
                    buf.writeDouble(payload.y());
                    buf.writeDouble(payload.z());
                    buf.writeFloat(payload.yaw());
                    buf.writeBoolean(payload.isWeapon());
                    buf.writeBoolean(payload.isHeavyDamage());
                },
                buf -> new ParryVfxPayload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readBoolean(), buf.readBoolean())
        );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record FallParryVfxPayload(double x, double y, double z) implements CustomPayload {
        public static final CustomPayload.Id<FallParryVfxPayload> ID = new CustomPayload.Id<>(Identifier.of(Riposte.MOD_ID, "fall_parry_vfx"));
        public static final PacketCodec<RegistryByteBuf, FallParryVfxPayload> CODEC = CustomPayload.codecOf(
                (payload, buf) -> {
                    buf.writeDouble(payload.x());
                    buf.writeDouble(payload.y());
                    buf.writeDouble(payload.z());
                },
                buf -> new FallParryVfxPayload(buf.readDouble(), buf.readDouble(), buf.readDouble())
        );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record LethalVfxPayload() implements CustomPayload {
        public static final CustomPayload.Id<LethalVfxPayload> ID = new CustomPayload.Id<>(Identifier.of(Riposte.MOD_ID, "lethal_vfx"));
        public static final PacketCodec<RegistryByteBuf, LethalVfxPayload> CODEC = CustomPayload.codecOf(
                (payload, buf) -> {},
                buf -> new LethalVfxPayload()
        );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record StartFinisherAnimPayload(UUID playerUuid, int targetId, String finisherId) implements CustomPayload {
        public static final CustomPayload.Id<StartFinisherAnimPayload> ID = new CustomPayload.Id<>(Identifier.of(Riposte.MOD_ID, "start_finisher_anim"));
        public static final PacketCodec<RegistryByteBuf, StartFinisherAnimPayload> CODEC = CustomPayload.codecOf(
                (payload, buf) -> {
                    buf.writeUuid(payload.playerUuid());
                    buf.writeInt(payload.targetId());
                    buf.writeString(payload.finisherId());
                },
                buf -> new StartFinisherAnimPayload(buf.readUuid(), buf.readInt(), buf.readString())
        );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SyncFinisherGaugePayload(int targetId, float gauge, int parryCount) implements CustomPayload {
        public static final CustomPayload.Id<SyncFinisherGaugePayload> ID = new CustomPayload.Id<>(Identifier.of(Riposte.MOD_ID, "sync_finisher_gauge"));
        public static final PacketCodec<RegistryByteBuf, SyncFinisherGaugePayload> CODEC = CustomPayload.codecOf(
                (payload, buf) -> {
                    buf.writeInt(payload.targetId());
                    buf.writeFloat(payload.gauge());
                    buf.writeInt(payload.parryCount());
                },
                buf -> new SyncFinisherGaugePayload(buf.readInt(), buf.readFloat(), buf.readInt())
        );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void registerAll() {        PayloadTypeRegistry.playC2S().register(ParrySyncPayload.ID, ParrySyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ExecuteFinisherPayload.ID, ExecuteFinisherPayload.CODEC);        PayloadTypeRegistry.playS2C().register(ParrySuccessPayload.ID, ParrySuccessPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ComboSuccessPayload.ID, ComboSuccessPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ParryVfxPayload.ID, ParryVfxPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FallParryVfxPayload.ID, FallParryVfxPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LethalVfxPayload.ID, LethalVfxPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StartFinisherAnimPayload.ID, StartFinisherAnimPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncFinisherGaugePayload.ID, SyncFinisherGaugePayload.CODEC);
    }
}