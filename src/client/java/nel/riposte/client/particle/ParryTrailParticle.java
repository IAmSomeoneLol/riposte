package nel.riposte.client.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.LinkedList;

public class ParryTrailParticle extends SpriteBillboardParticle {

    private final LinkedList<Vec3d> trailHistory = new LinkedList<>();
    private final int maxTrailLength = 15;
    private final float trailWidth = 0.15f;

    protected ParryTrailParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;
        this.maxAge = 40 + this.random.nextInt(20);
        this.collidesWithWorld = true;
        this.trailHistory.add(new Vec3d(x, y, z));
    }

    @Override
    public void tick() {
        double oldVx = this.velocityX;
        double oldVy = this.velocityY;
        double oldVz = this.velocityZ;

        this.velocityY -= 0.04;
        this.velocityX *= 0.98;
        this.velocityZ *= 0.98;

        super.move(this.velocityX, this.velocityY, this.velocityZ);

        // Fixed Bounce Physics: If Minecraft sets velocity to 0, it hit a wall. Bounce it!
        if (this.velocityX == 0.0D && oldVx != 0.0D) this.velocityX = -oldVx * 0.6;
        if (this.onGround || (this.velocityY == 0.0D && oldVy != 0.0D)) this.velocityY = -oldVy * 0.6;
        if (this.velocityZ == 0.0D && oldVz != 0.0D) this.velocityZ = -oldVz * 0.6;

        this.trailHistory.addFirst(new Vec3d(this.x, this.y, this.z));
        if (this.trailHistory.size() > maxTrailLength) {
            this.trailHistory.removeLast();
        }

        if (this.age++ >= this.maxAge) {
            this.markDead();
        }
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        if (this.trailHistory.size() < 2) return;

        Vec3d camPos = camera.getPos();

        // Grab the bounds of our blank texture from the atlas
        float minU = this.sprite.getMinU();
        float maxU = this.sprite.getMaxU();
        float minV = this.sprite.getMinV();
        float maxV = this.sprite.getMaxV();

        for (int i = 0; i < this.trailHistory.size() - 1; i++) {
            Vec3d current = this.trailHistory.get(i);
            Vec3d next = this.trailHistory.get(i + 1);

            float progress1 = (float) i / (this.trailHistory.size() - 1);
            float progress2 = (float) (i + 1) / (this.trailHistory.size() - 1);

            float r1 = MathHelper.lerp(progress1, 0.1f, 0.4f);
            float g1 = MathHelper.lerp(progress1, 0.2f, 0.8f);
            float b1 = MathHelper.lerp(progress1, 1.0f, 1.0f);
            float alpha1 = MathHelper.lerp(progress1, 1.0f, 0.0f);

            float r2 = MathHelper.lerp(progress2, 0.1f, 0.4f);
            float g2 = MathHelper.lerp(progress2, 0.2f, 0.8f);
            float b2 = MathHelper.lerp(progress2, 1.0f, 1.0f);
            float alpha2 = MathHelper.lerp(progress2, 1.0f, 0.0f);

            Vector3f dir = new Vector3f((float)(next.x - current.x), (float)(next.y - current.y), (float)(next.z - current.z)).normalize();
            Vector3f toCam = new Vector3f((float)(camPos.x - current.x), (float)(camPos.y - current.y), (float)(camPos.z - current.z)).normalize();
            Vector3f right = new Vector3f(dir).cross(toCam).normalize().mul(trailWidth);

            vertexConsumer.vertex((float)(current.x - camPos.x) + right.x(), (float)(current.y - camPos.y) + right.y(), (float)(current.z - camPos.z) + right.z())
                    .texture(minU, minV).color(r1, g1, b1, alpha1).light(255).next();

            vertexConsumer.vertex((float)(current.x - camPos.x) - right.x(), (float)(current.y - camPos.y) - right.y(), (float)(current.z - camPos.z) - right.z())
                    .texture(maxU, minV).color(r1, g1, b1, alpha1).light(255).next();

            vertexConsumer.vertex((float)(next.x - camPos.x) - right.x(), (float)(next.y - camPos.y) - right.y(), (float)(next.z - camPos.z) - right.z())
                    .texture(maxU, maxV).color(r2, g2, b2, alpha2).light(255).next();

            vertexConsumer.vertex((float)(next.x - camPos.x) + right.x(), (float)(next.y - camPos.y) + right.y(), (float)(next.z - camPos.z) + right.z())
                    .texture(minU, maxV).color(r2, g2, b2, alpha2).light(255).next();
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;
        public Factory(SpriteProvider spriteProvider) { this.spriteProvider = spriteProvider; }
        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            ParryTrailParticle particle = new ParryTrailParticle(world, x, y, z, vx, vy, vz);
            particle.setSprite(this.spriteProvider); // Now works correctly!
            return particle;
        }
    }
}