package dml.gamerankings.entity;

import dml.largescaletaskmanagement.entity.LargeScaleTaskBase;

/**
 * PlayerRankingItem 批量更新任务实体。
 * 代表一个需要分段执行的 PlayerRankingItem 批量更新任务。
 */
public class PlayerRankingItemUpdateTask extends LargeScaleTaskBase {
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
