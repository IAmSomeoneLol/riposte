package nel.riposte;

import java.util.List;

public class FinisherDefinition {
    public String id;
    public String animation_id;
    public String target_size;
    public boolean requires_grounded;
    public String weapon_requirement;
    public int total_duration_ticks;
    public List<TimelineEvent> timeline;

    public static class TimelineEvent {
        public int tick;
        public String action; // "sound", "particle", "damage", "camera_shake", "camera_pitch"
        public String value;
        public float amount;
        public float x_offset;
        public float y_offset;
        public float z_offset;
        public int count;
    }
}