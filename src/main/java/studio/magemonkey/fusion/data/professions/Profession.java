package studio.magemonkey.fusion.data.professions;

import lombok.Getter;
import lombok.Setter;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.sql.SQLManager;

import java.util.UUID;

@Getter

public class Profession {

    private final long    id;
    private final UUID    uuid;
    private final String  name;
    private       long    exp;
    private       boolean mastered;
    private       boolean joined;

    public Profession(long id, UUID uuid, String name, long exp, boolean mastered, boolean joined) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.exp = exp;
        this.mastered = mastered;
        this.joined = joined;
    }

    public void addExp(long exp) {
        SQLManager.professions().incrementExperience(this, exp);
        this.exp += exp;
    }

    public void removeExp(long exp) {
        addExp(-exp);
    }

    public void resetExp() {
        setExp(0);
    }

    public void resetMastered() {
        setMastered(false);
    }

    public void resetJoined() {
        setJoined(false);
    }

    public void reset() {
        resetExp();
        resetMastered();
        resetJoined();
    }

    public void update() {
        SQLManager.professions().setProfession(uuid, this);
    }

    public void setExp(long exp) {
        SQLManager.professions().setValue(this, "Experience", exp);
        this.exp = exp;
    }

    public void setMastered(boolean mastered) {
        SQLManager.professions().setValue(this, "Mastered", mastered);
        this.mastered = mastered;
    }

    public void setJoined(boolean joined) {
        SQLManager.professions().setValue(this, "Joined", joined);
        this.joined = joined;
    }

    public int getLevel() {
        return ProfessionsCfg.getGuiMap().containsKey(name) ? ProfessionsCfg.getTable(name).getLevelFunction().getLevel(exp) : 0;
    }

    public void setLevel(int level) {
        if (level < getLevel()) {
            long diffExp = (long) (exp - ProfessionsCfg.getTable(name).getLevelFunction().getXP(level));
            removeExp(diffExp);
        } else {
            long requiredExp = (long) (ProfessionsCfg.getTable(name).getLevelFunction().getXP(level) - exp);
            addExp(requiredExp);
        }
    }
}
