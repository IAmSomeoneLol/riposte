package nel.riposte.network;

import nel.riposte.Riposte;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class RipostePackets {

    public record ParrySyncPayload() implements CustomPayload {
        public static final Id<ParrySyncPayload> ID = new Id<>(Identifier.of(Riposte.MOD_ID, "parry_sync"));
        public static final PacketCodec<RegistryByteBuf, ParrySyncPayload> CODEC = PacketCodec.unit(new ParrySyncPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ExecuteFinisherPayload(int targetId) implements CustomPayload {
        public static final Id<ExecuteFinisherPayload> ID = new Id<>(Identifier.of(Riposte.MOD_ID, "execute_finisher"));
        public static final PacketCodec<RegistryByteBuf, ExecuteFinisherPayload> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeInt(value.targetId()),
                buf -> new ExecuteFinisherPayload(buf.readInt())
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record FallParryVfxPayload(double x, double y, double z) implements CustomPayload {
        public static final Id<FallParryVfxPayload> ID = new Id<>(Identifier.of(Riposte.MOD_ID, "fall_parry_vfx"));
        public static final PacketCodec<RegistryByteBuf, FallParryVfxPayload> CODEC = PacketCodec.of(
                (value, buf) -> { buf.writeDouble(value.x()); buf.writeDouble(value.y()); buf.writeDouble(value.z()); },
                buf -> new FallParryVfxPayload(buf.readDouble(), buf.readDouble(), buf.readDouble())
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ParryVfxPayload(double x, double y, double z, float yaw, boolean isWeapon, boolean isHeavyDamage) implements CustomPayload {
        public static final Id<ParryVfxPayload> ID = new Id<>(Identifier.of(Riposte.MOD_ID, "parry_vfx"));
        public static final PacketCodec<RegistryByteBuf, ParryVfxPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeDouble(value.x()); buf.writeDouble(value.y()); buf.writeDouble(value.z());
                    buf.writeFloat(value.yaw()); buf.writeBoolean(value.isWeapon()); buf.writeBoolean(value.isHeavyDamage());
                },
                buf -> new ParryVfxPayload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readBoolean(), buf.readBoolean())
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record LethalVfxPayload() implements CustomPayload {
        public static final Id<LethalVfxPayload> ID = new Id<>(Identifier.of(Riposte.MOD_ID, "lethal_vfx"));
        public static final PacketCodec<RegistryByteBuf, LethalVfxPayload> CODEC = PacketCodec.unit(new LethalVfxPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SyncFinisherGaugePayload(int targetId, float gauge, int parryCount) implements CustomPayload {
        public static final Id<SyncFinisherGaugePayload> ID = new Id<>(Identifier.of(Riposte.MOD_ID, "sync_finisher_gauge"));
        public static final PacketCodec<RegistryByteBuf, SyncFinisherGaugePayload> CODEC = PacketCodec.of(
                (value, buf) -> { buf.writeInt(value.targetId()); buf.writeFloat(value.gauge()); buf.writeInt(value.parryCount()); },
                buf -> new SyncFinisherGaugePayload(buf.readInt(), buf.readFloat(), buf.readInt())
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ParrySuccessPayload(boolean isFallParry, int ownerId, float gauge) implements CustomPayload {
        public static final Id<ParrySuccessPayload> ID = new Id<>(Identifier.of(Riposte.MOD_ID, "parry_success"));
        public static final PacketCodec<RegistryByteBuf, ParrySuccessPayload> CODEC = PacketCodec.of(
                (value, buf) -> { buf.writeBoolean(value.isFallParry()); buf.writeInt(value.ownerId()); buf.writeFloat(value.gauge()); },
                buf -> new ParrySuccessPayload(buf.readBoolean(), buf.readInt(), buf.readFloat())
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ComboSuccessPayload() implements CustomPayload {
        public static final Id<ComboSuccessPayload> ID = new Id<>(Identifier.of(Riposte.MOD_ID, "combo_success"));
        public static final PacketCodec<RegistryByteBuf, ComboSuccessPayload> CODEC = PacketCodec.unit(new ComboSuccessPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record StartFinisherAnimPayload(UUID playerUuid, int targetId, String finisherId) implements CustomPayload {
        public static final Id<StartFinisherAnimPayload> ID = new Id<>(Identifier.of(Riposte.MOD_ID, "start_finisher_anim"));
        public static final PacketCodec<RegistryByteBuf, StartFinisherAnimPayload> CODEC = PacketCodec.of(
                (value, buf) -> { buf.writeUuid(value.playerUuid()); buf.writeInt(value.targetId()); buf.writeString(value.finisherId()); },
                buf -> new StartFinisherAnimPayload(buf.readUuid(), buf.readInt(), buf.readString())
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void registerC2S() {
        PayloadTypeRegistry.playC2S().register(ParrySyncPayload.ID, ParrySyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ExecuteFinisherPayload.ID, ExecuteFinisherPayload.CODEC);
    }

    public static void registerS2C() {
        PayloadTypeRegistry.playS2C().register(FallParryVfxPayload.ID, FallParryVfxPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ParryVfxPayload.ID, ParryVfxPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LethalVfxPayload.ID, LethalVfxPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncFinisherGaugePayload.ID, SyncFinisherGaugePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ParrySuccessPayload.ID, ParrySuccessPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ComboSuccessPayload.ID, ComboSuccessPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StartFinisherAnimPayload.ID, StartFinisherAnimPayload.CODEC);
    }
}