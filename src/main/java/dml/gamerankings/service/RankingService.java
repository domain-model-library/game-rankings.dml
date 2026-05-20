package dml.gamerankings.service;

import dml.gamerankings.entity.Leaderboard;
import dml.gamerankings.entity.PlayerRank;
import dml.gamerankings.entity.PlayerRankUpdateTask;
import dml.gamerankings.entity.PlayerRankUpdateTaskSegment;
import dml.gamerankings.entity.PlayerRankingItem;
import dml.gamerankings.repository.LeaderboardRepository;
import dml.gamerankings.repository.PlayerRankRepository;
import dml.gamerankings.repository.PlayerRankUpdateTaskRepository;
import dml.gamerankings.repository.PlayerRankUpdateTaskSegmentRepository;
import dml.gamerankings.service.repositoryset.RankingServiceRepositorySet;
import dml.gamerankings.service.result.CalculateRankingResult;
import dml.largescaletaskmanagement.entity.ResetSegmentToProcessIfTimeout;
import dml.largescaletaskmanagement.repository.LargeScaleTaskRepository;
import dml.largescaletaskmanagement.repository.LargeScaleTaskSegmentRepository;
import dml.largescaletaskmanagement.repository.SegmentProcessingTimeoutHandlingStrategyRepository;
import dml.largescaletaskmanagement.service.LargeScaleTaskService;
import dml.largescaletaskmanagement.service.repositoryset.LargeScaleTaskServiceRepositorySet;
import dml.largescaletaskmanagement.service.result.TakeTaskSegmentToExecuteResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RankingService {
    public static PlayerRank createPlayerRank(RankingServiceRepositorySet repositorySet,
                                              PlayerRank playerRank, Object playerId) {
        PlayerRankRepository<PlayerRank, Object> playerRankRepository = repositorySet.getPlayerRankRepository();

        playerRank.setPlayerId(playerId);
        PlayerRank exists = playerRankRepository.putIfAbsent(playerRank);
        if (exists != null) {
            return exists;
        }
        return playerRank;
    }

    public static CalculateRankingResult calculateRanking(RankingServiceRepositorySet repositorySet,
                                                          List<PlayerRankingItem> allRankingItems,
                                                          Leaderboard newLeaderboard,
                                                          int topN, boolean rankingFromHighToLow,
                                                          long currentTime, int itemsUpdateBatchSize) {
        PlayerRankRepository<PlayerRank, Object> playerRankRepository = repositorySet.getPlayerRankRepository();

        PlayerRankingItem[] items = allRankingItems.toArray(new PlayerRankingItem[0]);
        int n = items.length;

        // Compute how many bits are needed for array indices (low bits) and metric values (high bits).
        // maxMetricValue determines metricBits; n determines indexBits.
        // Encoding: (metricValue << indexBits) | arrayIndex — sorts correctly as a signed long
        // as long as metricBits + indexBits <= 63 (sign bit unused).
        int indexBits = (n <= 1) ? 0 : (Long.SIZE - Long.numberOfLeadingZeros(n - 1));

        long[] encoded = new long[n];
        for (int i = 0; i < n; i++) {
            encoded[i] = (items[i].getMetricValue() << indexBits) | i;
        }

        Arrays.sort(encoded);

        // Walk the sorted array assigning ranks 1..n.
        // rankingFromHighToLow=true  → highest encoded value (= highest metric) is rank 1, iterate backwards.
        // rankingFromHighToLow=false → lowest encoded value (= lowest metric) is rank 1, iterate forwards.
        long indexMask = (1L << indexBits) - 1;
        List<PlayerRank> allRankedItems = new ArrayList<>(n);
        List<PlayerRank> topItems = new ArrayList<>(Math.min(topN, n));

        for (int rank = 1; rank <= n; rank++) {
            int sortedPos = rankingFromHighToLow ? (n - rank) : (rank - 1);
            int itemIndex = (int) (encoded[sortedPos] & indexMask);
            PlayerRankingItem item = items[itemIndex];
            PlayerRank playerRank = playerRankRepository.take(item.getPlayerId());
            if (playerRank != null) {
                playerRank.setRank(rank);
                allRankedItems.add(playerRank);
                if (rank <= topN) {
                    topItems.add(playerRank);
                }
            }
        }

        // Take existing leaderboard or create it for the first time.
        LeaderboardRepository<Leaderboard> leaderboardRepository = repositorySet.getLeaderboardRepository();
        Leaderboard leaderboard = leaderboardRepository.takeOrPutIfAbsent(newLeaderboard);
        leaderboard.setItemList(topItems);

        PlayerRankUpdateTaskRepository playerRankUpdateTaskRepository = repositorySet.getPlayerRankUpdateTaskRepository();
        PlayerRankUpdateTaskSegmentRepository playerRankUpdateTaskSegmentRepository = repositorySet.getPlayerRankUpdateTaskSegmentRepository();
        submitPlayerRankUpdateTask(playerRankUpdateTaskRepository, playerRankUpdateTaskSegmentRepository,
                allRankedItems, currentTime, itemsUpdateBatchSize);

        return new CalculateRankingResult(leaderboard, allRankedItems);
    }

    private static void submitPlayerRankUpdateTask(PlayerRankUpdateTaskRepository playerRankUpdateTaskRepository,
                                                   PlayerRankUpdateTaskSegmentRepository playerRankUpdateTaskSegmentRepository,
                                                   List<PlayerRank> allRankedItems, long currentTime,
                                                   int itemsUpdateBatchSize) {
        if (allRankedItems == null || allRankedItems.isEmpty()) {
            return;
        }
        String taskName = "PlayerRank-update-" + currentTime;
        PlayerRankUpdateTask task = new PlayerRankUpdateTask();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankUpdateTaskRepository, playerRankUpdateTaskSegmentRepository);
        LargeScaleTaskService.createTask(largeScaleTaskServiceRepositorySet, taskName, task, currentTime);

        for (int i = 0; i < allRankedItems.size(); i += itemsUpdateBatchSize) {
            int endIdx = Math.min(i + itemsUpdateBatchSize, allRankedItems.size());
            List<PlayerRank> batch = allRankedItems.subList(i, endIdx);
            String segmentId = taskName + "-" + i;
            PlayerRankUpdateTaskSegment segment = new PlayerRankUpdateTaskSegment(segmentId);
            segment.setPlayerRankList(new ArrayList<>(batch));
            LargeScaleTaskService.addTaskSegment(largeScaleTaskServiceRepositorySet, taskName, segment);
        }
        LargeScaleTaskService.setTaskReadyToProcess(largeScaleTaskServiceRepositorySet, taskName);
    }

    private static LargeScaleTaskServiceRepositorySet getLargeScaleTaskServiceRepositorySet(
            PlayerRankUpdateTaskRepository playerRankUpdateTaskRepository,
            PlayerRankUpdateTaskSegmentRepository playerRankUpdateTaskSegmentRepository) {
        return new LargeScaleTaskServiceRepositorySet() {

            @Override
            public LargeScaleTaskRepository getLargeScaleTaskRepository() {
                return playerRankUpdateTaskRepository;
            }

            @Override
            public LargeScaleTaskSegmentRepository getLargeScaleTaskSegmentRepository() {
                return playerRankUpdateTaskSegmentRepository;
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

    public static PlayerRank getPlayerRank(RankingServiceRepositorySet repositorySet,
                                           Object playerId) {
        PlayerRankRepository<PlayerRank, Object> playerRankRepository = repositorySet.getPlayerRankRepository();
        return playerRankRepository.find(playerId);
    }

    public static String takePlayerRankUpdateSegmentToExecute(RankingServiceRepositorySet repositorySet,
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

    private static LargeScaleTaskServiceRepositorySet getLargeScaleTaskServiceRepositorySet(RankingServiceRepositorySet repositorySet) {
        PlayerRankUpdateTaskRepository playerRankUpdateTaskRepository = repositorySet.getPlayerRankUpdateTaskRepository();
        PlayerRankUpdateTaskSegmentRepository playerRankUpdateTaskSegmentRepository = repositorySet.getPlayerRankUpdateTaskSegmentRepository();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankUpdateTaskRepository, playerRankUpdateTaskSegmentRepository);
        return largeScaleTaskServiceRepositorySet;
    }

    public static int executePlayerRankUpdateSegment(RankingServiceRepositorySet repositorySet,
                                                     String segmentId) {
        PlayerRankUpdateTaskSegmentRepository playerRankUpdateTaskSegmentRepository = repositorySet.getPlayerRankUpdateTaskSegmentRepository();
        PlayerRankRepository<PlayerRank, Object> playerRankRepository = repositorySet.getPlayerRankRepository();

        PlayerRankUpdateTaskSegment segment = playerRankUpdateTaskSegmentRepository.find(segmentId);
        if (segment == null) {
            return 0;
        }
        List<PlayerRank> playerRankList = segment.getPlayerRankList();
        if (playerRankList == null || playerRankList.isEmpty()) {
            return 0;
        }
        for (PlayerRank item : playerRankList) {
            PlayerRank existing = playerRankRepository.take(item.getPlayerId());
            if (existing == null) {
                playerRankRepository.put(item);
            } else {
                existing.setRank(item.getRank());
            }
        }
        return playerRankList.size();
    }

    public static void completePlayerRankUpdateSegment(RankingServiceRepositorySet repositorySet,
                                                       String segmentId) {
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet = getLargeScaleTaskServiceRepositorySet(repositorySet);

        LargeScaleTaskService.completeTaskSegment(largeScaleTaskServiceRepositorySet, segmentId);
    }


    public static Leaderboard getLeaderboard(RankingServiceRepositorySet repositorySet) {
        LeaderboardRepository<Leaderboard> leaderboardRepository = repositorySet.getLeaderboardRepository();
        return leaderboardRepository.get();
    }
}
