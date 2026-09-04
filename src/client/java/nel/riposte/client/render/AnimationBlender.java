package nel.riposte.client.render;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnimationBlender {


    private static final Map<UUID, PlayerBlendState> STATES = new HashMap<>();

    public static PlayerBlendState get(UUID uuid) {
        return STATES.computeIfAbsent(uuid, k -> new PlayerBlendState());
    }

    public static class PlayerBlendState {
        public boolean wasActive = false;
        public long blendStartTime = 0;
        public final Pose lastFrame = new Pose();
        public final Pose blendStart = new Pose();
        public final Pose capturedVanillaPose = new Pose();

        public void applyBlend(PlayerEntityModel<?> model, float progress) {

            blendStart.rightArm.blend(model.rightArm, progress);
            blendStart.rightSleeve.blend(model.rightSleeve, progress);
            blendStart.leftArm.blend(model.leftArm, progress);
            blendStart.leftSleeve.blend(model.leftSleeve, progress);
        }
    }

    public static class Pose {
        public final PartPose head = new PartPose();
        public final PartPose hat = new PartPose();
        public final PartPose body = new PartPose();
        public final PartPose jacket = new PartPose();
        public final PartPose rightArm = new PartPose();
        public final PartPose rightSleeve = new PartPose();
        public final PartPose leftArm = new PartPose();
        public final PartPose leftSleeve = new PartPose();
        public final PartPose rightLeg = new PartPose();
        public final PartPose rightPants = new PartPose();
        public final PartPose leftLeg = new PartPose();
        public final PartPose leftPants = new PartPose();

        public void copyFrom(PlayerEntityModel<?> model) {
            head.copy(model.head);
            hat.copy(model.hat);
            body.copy(model.body);
            jacket.copy(model.jacket);
            rightArm.copy(model.rightArm);
            rightSleeve.copy(model.rightSleeve);
            leftArm.copy(model.leftArm);
            leftSleeve.copy(model.leftSleeve);
            rightLeg.copy(model.rightLeg);
            rightPants.copy(model.rightPants);
            leftLeg.copy(model.leftLeg);
            leftPants.copy(model.leftPants);
        }


        public void copyFrom(Pose other) {
            head.copy(other.head);
            hat.copy(other.hat);
            body.copy(other.body);
            jacket.copy(other.jacket);
            rightArm.copy(other.rightArm);
            rightSleeve.copy(other.rightSleeve);
            leftArm.copy(other.leftArm);
            leftSleeve.copy(other.leftSleeve);
            rightLeg.copy(other.rightLeg);
            rightPants.copy(other.rightPants);
            leftLeg.copy(other.leftLeg);
            leftPants.copy(other.leftPants);
        }
    }

    public static class PartPose {
        public float pitch, yaw, roll, x, y, z;

        public void copy(ModelPart part) {
            this.pitch = part.pitch;
            this.yaw = part.yaw;
            this.roll = part.roll;
            this.x = part.pivotX;
            this.y = part.pivotY;
            this.z = part.pivotZ;
        }


        public void copy(PartPose other) {
            this.pitch = other.pitch;
            this.yaw = other.yaw;
            this.roll = other.roll;
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
        }

        public void blend(ModelPart target, float progress) {
            target.pitch = angleLerp(progress, this.pitch, target.pitch);
            target.yaw = angleLerp(progress, this.yaw, target.yaw);
            target.roll = angleLerp(progress, this.roll, target.roll);
            target.pivotX = MathHelper.lerp(progress, this.x, target.pivotX);
            target.pivotY = MathHelper.lerp(progress, this.y, target.pivotY);
            target.pivotZ = MathHelper.lerp(progress, this.z, target.pivotZ);
        }


        private float angleLerp(float delta, float start, float end) {
            float f = (end - start) % ((float) Math.PI * 2F);
            if (f < -(float) Math.PI) f += ((float) Math.PI * 2F);
            if (f >= (float) Math.PI) f -= ((float) Math.PI * 2F);
            return start + delta * f;
        }
    }
}