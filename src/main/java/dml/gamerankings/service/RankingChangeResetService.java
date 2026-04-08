package dml.gamerankings.service;

import dml.gamerankings.entity.PlayerRankingChangeItem;
import dml.gamerankings.entity.PlayerRankingChangeItemResetTask;
import dml.gamerankings.entity.PlayerRankingChangeItemResetTaskSegment;
import dml.gamerankings.repository.PlayerRankingChangeItemRepository;
import dml.gamerankings.repository.PlayerRankingChangeItemResetTaskRepository;
import dml.gamerankings.repository.PlayerRankingChangeItemResetTaskSegmentRepository;
import dml.gamerankings.service.repositoryset.RankingChangeResetServiceRepositorySet;
import dml.largescaletaskmanagement.entity.ResetSegmentToProcessIfTimeout;
import dml.largescaletaskmanagement.repository.LargeScaleTaskRepository;
import dml.largescaletaskmanagement.repository.LargeScaleTaskSegmentRepository;
import dml.largescaletaskmanagement.repository.SegmentProcessingTimeoutHandlingStrategyRepository;
import dml.largescaletaskmanagement.service.LargeScaleTaskService;
import dml.largescaletaskmanagement.service.repositoryset.LargeScaleTaskServiceRepositorySet;
import dml.largescaletaskmanagement.service.result.TakeTaskSegmentToExecuteResult;

import java.util.ArrayList;
import java.util.List;

public class RankingChangeResetService {

    public static void resetRankingChange(RankingChangeResetServiceRepositorySet repositorySet,
                                          List<String> allPlayerIds,
                                          long currentTime, int itemsUpdateBatchSize) {
        submitRankingChangeItemResetTask(repositorySet, allPlayerIds, currentTime, itemsUpdateBatchSize);
    }

    private static void submitRankingChangeItemResetTask(RankingChangeResetServiceRepositorySet repositorySet,
                                                         List<String> allPlayerIds, long currentTime,
                                                         int itemsUpdateBatchSize) {
        if (allPlayerIds == null || allPlayerIds.isEmpty()) {
            return;
        }
        PlayerRankingChangeItemResetTaskRepository playerRankingChangeItemResetTaskRepository = repositorySet.getPlayerRankingChangeItemResetTaskRepository();
        PlayerRankingChangeItemResetTaskSegmentRepository playerRankingChangeItemResetTaskSegmentRepository = repositorySet.getPlayerRankingChangeItemResetTaskSegmentRepository();

        String taskName = "PlayerRankingChangeItem-reset-" + currentTime;
        PlayerRankingChangeItemResetTask task = new PlayerRankingChangeItemResetTask();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankingChangeItemResetTaskRepository, playerRankingChangeItemResetTaskSegmentRepository);
        LargeScaleTaskService.createTask(largeScaleTaskServiceRepositorySet, taskName, task, currentTime);

        for (int i = 0; i < allPlayerIds.size(); i += itemsUpdateBatchSize) {
            int endIdx = Math.min(i + itemsUpdateBatchSize, allPlayerIds.size());
            List<String> batch = allPlayerIds.subList(i, endIdx);
            String segmentId = taskName + "-" + i;
            PlayerRankingChangeItemResetTaskSegment segment = new PlayerRankingChangeItemResetTaskSegment(segmentId);
            segment.setPlayerIdList(new ArrayList<>(batch));
            LargeScaleTaskService.addTaskSegment(largeScaleTaskServiceRepositorySet, taskName, segment);
        }
        LargeScaleTaskService.setTaskReadyToProcess(largeScaleTaskServiceRepositorySet, taskName);
    }

    private static LargeScaleTaskServiceRepositorySet getLargeScaleTaskServiceRepositorySet(
            PlayerRankingChangeItemResetTaskRepository playerRankingChangeItemResetTaskRepository,
            PlayerRankingChangeItemResetTaskSegmentRepository playerRankingChangeItemResetTaskSegmentRepository) {
        return new LargeScaleTaskServiceRepositorySet() {

            @Override
            public LargeScaleTaskRepository getLargeScaleTaskRepository() {
                return playerRankingChangeItemResetTaskRepository;
            }

            @Override
            public LargeScaleTaskSegmentRepository getLargeScaleTaskSegmentRepository() {
                return playerRankingChangeItemResetTaskSegmentRepository;
            }

            @Override
            public SegmentProcessingTimeoutHandlingStrategyRepository getSegmentProcessingTimeoutHandlingStrategyRepository() {
                return new SegmentProcessingTimeoutHandlingStrategyRepository() {

                    @Override
                    public Object get() {
                        return new ResetSegmentToProcessIfTimeout();
                    }

                    @Override
                    public Object take() {
                        return new ResetSegmentToProcessIfTimeout();
                    }

                    @Override
                    public void put(Object o) {
                    }

                    @Override
                    public Object putIfAbsent(Object o) {
                        return new ResetSegmentToProcessIfTimeout();
                    }

                    @Override
                    public Object takeOrPutIfAbsent(Object o) {
                        return new ResetSegmentToProcessIfTimeout();
                    }

                    @Override
                    public Object remove() {
                        return new ResetSegmentToProcessIfTimeout();
                    }
                };
            }
        };
    }

    public static String takePlayerRankingChangeItemResetSegmentToExecute(RankingChangeResetServiceRepositorySet repositorySet,
                                                                           String taskName, long currentTime, long segmentTimeoutMs, long maxTimeToReadyMs) {
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet = getLargeScaleTaskServiceRepositorySet(repositorySet);

        TakeTaskSegmentToExecuteResult result = LargeScaleTaskService.takeTaskSegmentToExecute(
                largeScaleTaskServiceRepositorySet, taskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        if (result.isTaskCompleted()) {
            LargeScaleTaskService.removeTask(largeScaleTaskServiceRepositorySet, taskName);
        }
        if (result.getTaskSegment() == null) {
            return null;
        }
        return String.valueOf(result.getTaskSegment().getId());
    }

    private static LargeScaleTaskServiceRepositorySet getLargeScaleTaskServiceRepositorySet(RankingChangeResetServiceRepositorySet repositorySet) {
        PlayerRankingChangeItemResetTaskRepository playerRankingChangeItemResetTaskRepository = repositorySet.getPlayerRankingChangeItemResetTaskRepository();
        PlayerRankingChangeItemResetTaskSegmentRepository playerRankingChangeItemResetTaskSegmentRepository = repositorySet.getPlayerRankingChangeItemResetTaskSegmentRepository();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankingChangeItemResetTaskRepository, playerRankingChangeItemResetTaskSegmentRepository);
        return largeScaleTaskServiceRepositorySet;
    }

    public static int executePlayerRankingChangeItemResetSegment(RankingChangeResetServiceRepositorySet repositorySet,
                                                                  String segmentId) {
        PlayerRankingChangeItemResetTaskSegmentRepository playerRankingChangeItemResetTaskSegmentRepository = repositorySet.getPlayerRankingChangeItemResetTaskSegmentRepository();
        PlayerRankingChangeItemRepository playerRankingChangeItemRepository = repositorySet.getPlayerRankingChangeItemRepository();

        PlayerRankingChangeItemResetTaskSegment segment = playerRankingChangeItemResetTaskSegmentRepository.find(segmentId);
        if (segment == null) {
            return 0;
        }
        List<String> playerIdList = segment.getPlayerIdList();
        if (playerIdList == null || playerIdList.isEmpty()) {
            return 0;
        }
        for (String playerId : playerIdList) {
            PlayerRankingChangeItem existing = playerRankingChangeItemRepository.take(playerId);
            if (existing != null) {
                existing.reset();
            }
        }
        return playerIdList.size();
    }

    public static void completePlayerRankingChangeItemResetSegment(RankingChangeResetServiceRepositorySet repositorySet,
                                                                    String segmentId) {
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet = getLargeScaleTaskServiceRepositorySet(repositorySet);

        LargeScaleTaskService.completeTaskSegment(largeScaleTaskServiceRepositorySet, segmentId);
    }
}
