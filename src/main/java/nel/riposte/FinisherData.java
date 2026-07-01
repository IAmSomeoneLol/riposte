package nel.riposte;

public interface FinisherData {
    boolean isExecutingFinisher();
    int getFinisherTargetId();
    int getFinisherTick();
    String getActiveFinisherId(); // NEW
    void setActiveFinisherId(String id); // NEW

    void startFinisher(int targetId);
    void cancelFinisher();

    int getPostFinisherInvuln();
    void setPostFinisherInvuln(int ticks);

    float getFinisherGauge(int targetId);
    void addFinisherGauge(int targetId, float amount);
    void consumeFinisherGauge(int targetId, float amount);
    void setFinisherGauge(int targetId, float amount);

    int getParryCount(int targetId);
    void addParryCount(int targetId);
    void setParryCount(int targetId, int amount);
    void clearParryCount(int targetId);

    long getGaugeReadyTimestamp(int targetId);
    void setGaugeReadyTimestamp(int targetId, long time);

    long getParryReadyTimestamp(int targetId);
    void setParryReadyTimestamp(int targetId, long time);

    void markProjectileDeflected(int projectileId);
    boolean isProjectileDeflected(int projectileId);
}