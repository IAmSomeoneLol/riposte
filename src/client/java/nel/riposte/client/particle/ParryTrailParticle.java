package nel.riposte.client.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.LinkedList;

public class ParryTrailParticle extends SpriteBillboardParticle {

    private final LinkedList<Vec3d> trailHistory = new LinkedList<>();
    private final int maxTrailLength = 80;
    private final float baseSize = 0.035f;
    private final double dotSpacing = 0.15;
    private final boolean isHeavy;

    protected ParryTrailParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, boolean isHeavy) {
        super(world, x, y, z, vx, vy, vz);
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;
        this.isHeavy = isHeavy;

        this.maxAge = 25 + this.random.nextInt(10);
        this.collidesWithWorld = true;
        this.trailHistory.add(new Vec3d(x, y, z));
    }

    @Override
    public void tick() {
        double oldVx = this.velocityX;
        double oldVy = this.velocityY;
        double oldVz = this.velocityZ;

        this.velocityY -= 0.025;
        this.velocityX *= 0.95;
        this.velocityZ *= 0.95;
        this.velocityY *= 0.95;

        super.move(this.velocityX, this.velocityY, this.velocityZ);

        if (this.velocityX == 0.0D && oldVx != 0.0D) this.velocityX = -oldVx * 0.8;
        if (this.onGround || (this.velocityY == 0.0D && oldVy != 0.0D)) this.velocityY = -oldVy * 0.8;
        if (this.velocityZ == 0.0D && oldVz != 0.0D) this.velocityZ = -oldVz * 0.8;

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
        float minU = this.sprite.getMinU();
        float maxU = this.sprite.getMaxU();
        float minV = this.sprite.getMinV();
        float maxV = this.sprite.getMaxV();
        int light = 15728880;

        Quaternionf quaternion = camera.getRotation();

        float lifeRatio = (float) this.age / this.maxAge;
        float ageFade = lifeRatio > 0.5f ? (1.0f - lifeRatio) * 2.0f : 1.0f;
        if (ageFade < 0) ageFade = 0;

        for (int i = 0; i < this.trailHistory.size() - 1; i++) {
            Vec3d current = this.trailHistory.get(i);
            Vec3d next = this.trailHistory.get(i + 1);

            double dist = current.distanceTo(next);
            int steps = Math.max(1, (int) Math.ceil(dist / dotSpacing));

            for (int step = 0; step < steps; step++) {
                float stepFraction = (float) step / steps;

                double ix = MathHelper.lerp(stepFraction, current.x, next.x);
                double iy = MathHelper.lerp(stepFraction, current.y, next.y);
                double iz = MathHelper.lerp(stepFraction, current.z, next.z);

                float baseProgress = (float) i / (this.trailHistory.size() - 1);
                float nextProgress = (float) (i + 1) / (this.trailHistory.size() - 1);
                float progress = MathHelper.lerp(stepFraction, baseProgress, nextProgress);

                float currentSize = this.baseSize * (1.0f - (progress * 0.7f));

                float r, g, b;
                if (this.isHeavy) {
                    if (progress < 0.1f) {
                        r = 1.0f; g = 1.0f; b = 1.0f;
                    } else if (progress < 0.3f) {
                        float p = (progress - 0.1f) / 0.2f;
                        r = MathHelper.lerp(p, 1.0f, 0.4f);
                        g = MathHelper.lerp(p, 1.0f, 0.9f);
                        b = 1.0f;
                    } else {
                        float p = (progress - 0.3f) / 0.7f;
                        r = MathHelper.lerp(p, 0.4f, 0.1f);
                        g = MathHelper.lerp(p, 0.9f, 0.5f);
                        b = MathHelper.lerp(p, 1.0f, 0.8f);
                    }
                } else {
                    if (progress < 0.1f) {
                        r = 1.0f; g = 1.0f; b = 1.0f;
                    } else if (progress < 0.3f) {
                        float p = (progress - 0.1f) / 0.2f;
                        r = 1.0f;
                        g = MathHelper.lerp(p, 1.0f, 0.7f);
                        b = MathHelper.lerp(p, 1.0f, 0.1f);
                    } else {
                        float p = (progress - 0.3f) / 0.7f;
                        r = MathHelper.lerp(p, 1.0f, 0.8f);
                        g = MathHelper.lerp(p, 0.7f, 0.2f);
                        b = MathHelper.lerp(p, 0.1f, 0.0f);
                    }
                }

                float alpha = ageFade * (1.0f - progress);

                Vector3f[] corners = new Vector3f[]{
                        new Vector3f(-1.0F, -1.0F, 0.0F),
                        new Vector3f(-1.0F,  1.0F, 0.0F),
                        new Vector3f( 1.0F,  1.0F, 0.0F),
                        new Vector3f( 1.0F, -1.0F, 0.0F)
                };

                float cx = (float)(ix - camPos.x);
                float cy = (float)(iy - camPos.y);
                float cz = (float)(iz - camPos.z);

                for (int j = 0; j < 4; ++j) {
                    corners[j].rotate(quaternion);
                    corners[j].mul(currentSize);
                    corners[j].add(cx, cy, cz);
                }

                vertexConsumer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).texture(maxU, maxV).color(r, g, b, alpha).light(light);
                vertexConsumer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).texture(maxU, minV).color(r, g, b, alpha).light(light);
                vertexConsumer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).texture(minU, minV).color(r, g, b, alpha).light(light);
                vertexConsumer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).texture(minU, maxV).color(r, g, b, alpha).light(light);
            }
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class HeavyFactory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;
        public HeavyFactory(SpriteProvider spriteProvider) { this.spriteProvider = spriteProvider; }
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            ParryTrailParticle particle = new ParryTrailParticle(world, x, y, z, vx, vy, vz, true);
            particle.setSprite(this.spriteProvider);
            return particle;
        }
    }

    public static class LightFactory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;
        public LightFactory(SpriteProvider spriteProvider) { this.spriteProvider = spriteProvider; }
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            ParryTrailParticle particle = new ParryTrailParticle(world, x, y, z, vx, vy, vz, false);
            particle.setSprite(this.spriteProvider);
            return particle;
        }
    }
}