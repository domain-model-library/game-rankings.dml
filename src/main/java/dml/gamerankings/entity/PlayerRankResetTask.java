package dml.gamerankings.entity;

import dml.largescaletaskmanagement.entity.LargeScaleTaskBase;

public class PlayerRankResetTask extends LargeScaleTaskBase {
    private String name;

    @Override
    public void setName(String taskName) {
        this.name = taskName;
    }

    @Override
    public String getName() {
        return name;
    }
}
