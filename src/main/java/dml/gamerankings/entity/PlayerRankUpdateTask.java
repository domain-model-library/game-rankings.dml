package dml.gamerankings.entity;

import dml.largescaletaskmanagement.entity.LargeScaleTaskBase;

/**
 * PlayerRank 批量更新任务实体。
 * 代表一个需要分段执行的 PlayerRank 批量更新任务。
 */
public class PlayerRankUpdateTask extends LargeScaleTaskBase {
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
