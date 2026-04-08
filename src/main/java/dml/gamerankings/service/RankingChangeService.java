package dml.gamerankings.service;

import dml.gamerankings.entity.PlayerRankingChangeItem;
import dml.gamerankings.entity.PlayerRankingChangeItemUpdateTask;
import dml.gamerankings.entity.PlayerRankingChangeItemUpdateTaskSegment;
import dml.gamerankings.entity.PlayerRankingItem;
import dml.gamerankings.repository.PlayerRankingChangeItemRepository;
import dml.gamerankings.repository.PlayerRankingChangeItemUpdateTaskRepository;
import dml.gamerankings.repository.PlayerRankingChangeItemUpdateTaskSegmentRepository;
import dml.gamerankings.service.repositoryset.RankingChangeServiceRepositorySet;
import dml.largescaletaskmanagement.entity.ResetSegmentToProcessIfTimeout;
import dml.largescaletaskmanagement.repository.LargeScaleTaskRepository;
import dml.largescaletaskmanagement.repository.LargeScaleTaskSegmentRepository;
import dml.largescaletaskmanagement.repository.SegmentProcessingTimeoutHandlingStrategyRepository;
import dml.largescaletaskmanagement.service.LargeScaleTaskService;
import dml.largescaletaskmanagement.service.repositoryset.LargeScaleTaskServiceRepositorySet;
import dml.largescaletaskmanagement.service.result.TakeTaskSegmentToExecuteResult;

import java.util.ArrayList;
import java.util.List;

public class RankingChangeService {

    public static PlayerRankingChangeItem createRankingChangeItem(RankingChangeServiceRepositorySet repositorySet,
                                                                  Object playerId) {
        PlayerRankingChangeItemRepository playerRankingChangeItemRepository = repositorySet.getPlayerRankingChangeItemRepository();

        PlayerRankingChangeItem playerRankingChangeItem = new PlayerRankingChangeItem();
        playerRankingChangeItem.setPlayerId(playerId.toString());
        PlayerRankingChangeItem exists = playerRankingChangeItemRepository.putIfAbsent(playerRankingChangeItem);
        if (exists != null) {
            return exists;
        }
        return playerRankingChangeItem;
    }

    public static void recordRankingChange(RankingChangeServiceRepositorySet repositorySet,
                                           List<PlayerRankingItem> allRankedItems, int topN, long currentTime,
                                           int itemsUpdateBatchSize) {
        PlayerRankingChangeItemRepository playerRankingChangeItemRepository = repositorySet.getPlayerRankingChangeItemRepository();
        PlayerRankingChangeItemUpdateTaskRepository playerRankingChangeItemUpdateTaskRepository = repositorySet.getPlayerRankingChangeItemUpdateTaskRepository();
        PlayerRankingChangeItemUpdateTaskSegmentRepository playerRankingChangeItemUpdateTaskSegmentRepository = repositorySet.getPlayerRankingChangeItemUpdateTaskSegmentRepository();

        if (allRankedItems == null || allRankedItems.isEmpty()) {
            return;
        }
        for (int i = 0; i < allRankedItems.size(); i++) {
            PlayerRankingItem playerRankingItem = allRankedItems.get(i);
            if (i < topN) {
                PlayerRankingChangeItem playerRankingChangeItem = playerRankingChangeItemRepository.take(playerRankingItem.getPlayerId().toString());
                playerRankingChangeItem.update(playerRankingItem.getRank());
            } else {
                break;
            }
        }
        //从allRankedItems中刨去topN个
        List<PlayerRankingItem> rankedItemsToUpdate = allRankedItems.subList(Math.min(topN, allRankedItems.size()), allRankedItems.size());

        submitRankingChangeItemUpdateTask(
                playerRankingChangeItemUpdateTaskRepository,
                playerRankingChangeItemUpdateTaskSegmentRepository,
                rankedItemsToUpdate, currentTime, itemsUpdateBatchSize);
    }

    private static void submitRankingChangeItemUpdateTask(
            PlayerRankingChangeItemUpdateTaskRepository playerRankingChangeItemUpdateTaskRepository,
            PlayerRankingChangeItemUpdateTaskSegmentRepository playerRankingChangeItemUpdateTaskSegmentRepository,
            List<PlayerRankingItem> rankedItemsToUpdate, long currentTime,
            int itemsUpdateBatchSize) {
        if (rankedItemsToUpdate == null || rankedItemsToUpdate.isEmpty()) {
            return;
        }
        String taskName = "PlayerRankingChangeItem-update-" + currentTime;
        PlayerRankingChangeItemUpdateTask task = new PlayerRankingChangeItemUpdateTask();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankingChangeItemUpdateTaskRepository, playerRankingChangeItemUpdateTaskSegmentRepository);
        LargeScaleTaskService.createTask(largeScaleTaskServiceRepositorySet, taskName, task, currentTime);

        for (int i = 0; i < rankedItemsToUpdate.size(); i += itemsUpdateBatchSize) {
            int endIdx = Math.min(i + itemsUpdateBatchSize, rankedItemsToUpdate.size());
            List<PlayerRankingItem> batch = rankedItemsToUpdate.subList(i, endIdx);
            String segmentId = taskName + "-" + i;
            PlayerRankingChangeItemUpdateTaskSegment segment = new PlayerRankingChangeItemUpdateTaskSegment(segmentId);
            segment.setPlayerRankingItemList(new ArrayList<>(batch));
            LargeScaleTaskService.addTaskSegment(largeScaleTaskServiceRepositorySet, taskName, segment);
        }
        LargeScaleTaskService.setTaskReadyToProcess(largeScaleTaskServiceRepositorySet, taskName);
    }

    private static LargeScaleTaskServiceRepositorySet getLargeScaleTaskServiceRepositorySet(
            PlayerRankingChangeItemUpdateTaskRepository playerRankingChangeItemUpdateTaskRepository,
            PlayerRankingChangeItemUpdateTaskSegmentRepository playerRankingChangeItemUpdateTaskSegmentRepository) {
        return new LargeScaleTaskServiceRepositorySet() {

            @Override
            public LargeScaleTaskRepository getLargeScaleTaskRepository() {
                return playerRankingChangeItemUpdateTaskRepository;
            }

            @Override
            public LargeScaleTaskSegmentRepository getLargeScaleTaskSegmentRepository() {
                return playerRankingChangeItemUpdateTaskSegmentRepository;
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

    public static String takePlayerRankingChangeItemUpdateSegmentToExecute(RankingChangeServiceRepositorySet repositorySet,
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

    private static LargeScaleTaskServiceRepositorySet getLargeScaleTaskServiceRepositorySet(RankingChangeServiceRepositorySet repositorySet) {
        PlayerRankingChangeItemUpdateTaskRepository playerRankingChangeItemUpdateTaskRepository = repositorySet.getPlayerRankingChangeItemUpdateTaskRepository();
        PlayerRankingChangeItemUpdateTaskSegmentRepository playerRankingChangeItemUpdateTaskSegmentRepository = repositorySet.getPlayerRankingChangeItemUpdateTaskSegmentRepository();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankingChangeItemUpdateTaskRepository, playerRankingChangeItemUpdateTaskSegmentRepository);
        return largeScaleTaskServiceRepositorySet;
    }

    public static int executePlayerRankingChangeItemUpdateSegment(RankingChangeServiceRepositorySet repositorySet,
                                                                  String segmentId) {
        PlayerRankingChangeItemUpdateTaskSegmentRepository playerRankingChangeItemUpdateTaskSegmentRepository = repositorySet.getPlayerRankingChangeItemUpdateTaskSegmentRepository();
        PlayerRankingChangeItemRepository playerRankingChangeItemRepository = repositorySet.getPlayerRankingChangeItemRepository();

        PlayerRankingChangeItemUpdateTaskSegment segment = playerRankingChangeItemUpdateTaskSegmentRepository.find(segmentId);
        if (segment == null) {
            return 0;
        }
        List<PlayerRankingItem> playerRankingItemList = segment.getPlayerRankingItemList();
        if (playerRankingItemList == null || playerRankingItemList.isEmpty()) {
            return 0;
        }
        for (PlayerRankingItem item : playerRankingItemList) {
            PlayerRankingChangeItem existing = playerRankingChangeItemRepository.take(item.getPlayerId().toString());
            if (existing == null) {
                PlayerRankingChangeItem newItem = new PlayerRankingChangeItem();
                newItem.setPlayerId(item.getPlayerId().toString());
                newItem.update(item.getRank());
                playerRankingChangeItemRepository.put(newItem);
            } else {
                existing.update(item.getRank());
            }
        }
        return playerRankingItemList.size();
    }

    public static void completePlayerRankingChangeItemUpdateSegment(RankingChangeServiceRepositorySet repositorySet,
                                                                    String segmentId) {
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet = getLargeScaleTaskServiceRepositorySet(repositorySet);

        LargeScaleTaskService.completeTaskSegment(largeScaleTaskServiceRepositorySet, segmentId);
    }


    public static PlayerRankingChangeItem getPlayerRankingChangeItem(RankingChangeServiceRepositorySet repositorySet,
                                                                     Object playerId) {
        PlayerRankingChangeItemRepository playerRankingChangeItemRepository = repositorySet.getPlayerRankingChangeItemRepository();
        return playerRankingChangeItemRepository.find(playerId.toString());
    }
}
