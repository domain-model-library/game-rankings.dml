import dml.common.repository.TestCommonRepository;
import dml.common.repository.TestCommonSingletonRepository;
import dml.gamerankings.entity.PlayerRankingChangeItem;
import dml.gamerankings.entity.PlayerRankingItem;
import dml.gamerankings.repository.*;
import dml.gamerankings.service.RankingChangeResetService;
import dml.gamerankings.service.RankingChangeService;
import dml.gamerankings.service.RankingResetService;
import dml.gamerankings.service.RankingService;
import dml.gamerankings.service.repositoryset.RankingChangeResetServiceRepositorySet;
import dml.gamerankings.service.repositoryset.RankingChangeServiceRepositorySet;
import dml.gamerankings.service.repositoryset.RankingResetServiceRepositorySet;
import dml.gamerankings.service.repositoryset.RankingServiceRepositorySet;
import dml.gamerankings.service.result.CalculateRankingResult;

import java.util.ArrayList;
import java.util.List;

public class Test {

    @org.junit.Test
    public void rankingAndRankChangeAndReset() {
        //准备10个玩家id
        long playerId1 = 1;
        long playerId2 = 2;
        long playerId3 = 3;
        long playerId4 = 4;
        long playerId5 = 5;
        long playerId6 = 6;
        long playerId7 = 7;
        long playerId8 = 8;
        long playerId9 = 9;
        long playerId10 = 10;

        //给10个玩家创建排行项
        RankingService.createRankingItem(rankingServiceRepositorySet,
                new TestPlayerRankingItem(), playerId1);
        RankingService.createRankingItem(rankingServiceRepositorySet,
                new TestPlayerRankingItem(), playerId2);
        RankingService.createRankingItem(rankingServiceRepositorySet,
                new TestPlayerRankingItem(), playerId3);
        RankingService.createRankingItem(rankingServiceRepositorySet,
                new TestPlayerRankingItem(), playerId4);
        RankingService.createRankingItem(rankingServiceRepositorySet,
                new TestPlayerRankingItem(), playerId5);
        RankingService.createRankingItem(rankingServiceRepositorySet,
                new TestPlayerRankingItem(), playerId6);
        RankingService.createRankingItem(rankingServiceRepositorySet,
                new TestPlayerRankingItem(), playerId7);
        RankingService.createRankingItem(rankingServiceRepositorySet,
                new TestPlayerRankingItem(), playerId8);
        RankingService.createRankingItem(rankingServiceRepositorySet,
                new TestPlayerRankingItem(), playerId9);
        RankingService.createRankingItem(rankingServiceRepositorySet,
                new TestPlayerRankingItem(), playerId10);

        //给10个玩家的排行项设置排行指标值
        RankingService.setRankingMetricValue(rankingServiceRepositorySet,
                playerId1, 10);
        RankingService.setRankingMetricValue(rankingServiceRepositorySet,
                playerId2, 20);
        RankingService.setRankingMetricValue(rankingServiceRepositorySet,
                playerId3, 30);
        RankingService.setRankingMetricValue(rankingServiceRepositorySet,
                playerId4, 40);
        RankingService.setRankingMetricValue(rankingServiceRepositorySet,
                playerId5, 50);
        RankingService.setRankingMetricValue(rankingServiceRepositorySet,
                playerId6, 60);
        RankingService.setRankingMetricValue(rankingServiceRepositorySet,
                playerId7, 70);
        RankingService.setRankingMetricValue(rankingServiceRepositorySet,
                playerId8, 80);
        RankingService.setRankingMetricValue(rankingServiceRepositorySet,
                playerId9, 90);
        RankingService.setRankingMetricValue(rankingServiceRepositorySet,
                playerId10, 100);

        //准备计算排行榜需要的参数：榜单取top几，从高到低还是从低到高排名
        int topN = 5;
        boolean rankingFromHighToLow = true;

        //计算排行榜
        long currentTime = 0;
        List<PlayerRankingItem> allRankingItems = getAllPlayerRankingItems(playerId1, playerId2, playerId3, playerId4, playerId5, playerId6, playerId7, playerId8, playerId9, playerId10);
        CalculateRankingResult result = RankingService.calculateRanking(rankingServiceRepositorySet,
                allRankingItems,
                new TestLeaderboard(),
                topN, rankingFromHighToLow,
                currentTime, 10);
        PlayerRankingItem topItem = result.getLeaderboard().getItemList().get(0);
        assert topItem.getPlayerId().equals(playerId10);
        assert topItem.getRank() == 1;

        //给10个玩家创建排行变化项
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId1);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId2);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId3);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId4);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId5);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId6);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId7);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId8);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId9);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId10);

        //记录玩家排名变化
        List<PlayerRankingItem> allRankedItems = result.getAllRankedItems();
        RankingChangeService.recordRankingChange(rankingChangeServiceRepositorySet,
                allRankedItems, topN, currentTime, 10);

        //异步更新RankingItem
        long segmentTimeoutMs = 60000;
        long maxTimeToReadyMs = 120000;
        String taskName = "PlayerRankingItem-update-" + currentTime;
        String segmentId = RankingService.takePlayerRankingItemUpdateSegmentToExecute(rankingServiceRepositorySet,
                taskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert segmentId != null;
        int count = RankingService.executePlayerRankingItemUpdateSegment(rankingServiceRepositorySet,
                segmentId);
        assert count == 10;
        RankingService.completePlayerRankingItemUpdateSegment(rankingServiceRepositorySet,
                segmentId);

        //验证第5名是否更新
        PlayerRankingItem playerRankingItem6 = RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId6);
        assert playerRankingItem6.getRank() == 5;

        //异步更新PlayerRankingChangeItem
        taskName = "PlayerRankingChangeItem-update-" + currentTime;
        segmentId = RankingChangeService.takePlayerRankingChangeItemUpdateSegmentToExecute(rankingChangeServiceRepositorySet,
                taskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert segmentId != null;
        count = RankingChangeService.executePlayerRankingChangeItemUpdateSegment(rankingChangeServiceRepositorySet,
                segmentId);
        assert count == 5;
        RankingChangeService.completePlayerRankingChangeItemUpdateSegment(rankingChangeServiceRepositorySet,
                segmentId);

        //第二名的排行指标增加
        RankingService.setRankingMetricValue(rankingServiceRepositorySet,
                playerId9, 110);

        //重新计算排行榜
        currentTime += 1000;
        allRankingItems = getAllPlayerRankingItems(playerId1, playerId2, playerId3, playerId4, playerId5, playerId6, playerId7, playerId8, playerId9, playerId10);
        result = RankingService.calculateRanking(
                rankingServiceRepositorySet,
                allRankingItems,
                new TestLeaderboard(),
                topN, rankingFromHighToLow,
                currentTime, 10);
        topItem = result.getLeaderboard().getItemList().get(0);
        assert topItem.getPlayerId().equals(playerId9);
        assert topItem.getRank() == 1;

        //记录玩家排名变化
        allRankedItems = result.getAllRankedItems();
        RankingChangeService.recordRankingChange(rankingChangeServiceRepositorySet,
                allRankedItems, topN, currentTime, 10);

        //异步更新RankingItem
        taskName = "PlayerRankingItem-update-" + currentTime;
        segmentId = RankingService.takePlayerRankingItemUpdateSegmentToExecute(rankingServiceRepositorySet,
                taskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert segmentId != null;
        count = RankingService.executePlayerRankingItemUpdateSegment(rankingServiceRepositorySet,
                segmentId);
        assert count == 10;
        RankingService.completePlayerRankingItemUpdateSegment(rankingServiceRepositorySet,
                segmentId);

        //异步更新PlayerRankingChangeItem
        taskName = "PlayerRankingChangeItem-update-" + currentTime;
        segmentId = RankingChangeService.takePlayerRankingChangeItemUpdateSegmentToExecute(rankingChangeServiceRepositorySet,
                taskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert segmentId != null;
        count = RankingChangeService.executePlayerRankingChangeItemUpdateSegment(rankingChangeServiceRepositorySet,
                segmentId);
        assert count == 5;
        RankingChangeService.completePlayerRankingChangeItemUpdateSegment(rankingChangeServiceRepositorySet,
                segmentId);

        //验证现在的第一名是从第二名变化过来的
        PlayerRankingChangeItem playerRankingChangeItem9 = RankingChangeService.getPlayerRankingChangeItem(rankingChangeServiceRepositorySet, playerId9);
        assert playerRankingChangeItem9.getPlayerId().equals(String.valueOf(playerId9));
        assert playerRankingChangeItem9.getCurrentRank() == 1;
        assert playerRankingChangeItem9.getLastRank() == 2;

        //重置排行榜
        currentTime += 1000;
        List<Object> allPlayerIdsForReset = new ArrayList<>();
        allPlayerIdsForReset.add(playerId1);
        allPlayerIdsForReset.add(playerId2);
        allPlayerIdsForReset.add(playerId3);
        allPlayerIdsForReset.add(playerId4);
        allPlayerIdsForReset.add(playerId5);
        allPlayerIdsForReset.add(playerId6);
        allPlayerIdsForReset.add(playerId7);
        allPlayerIdsForReset.add(playerId8);
        allPlayerIdsForReset.add(playerId9);
        allPlayerIdsForReset.add(playerId10);

        RankingResetService.resetRanking(rankingResetServiceRepositorySet,
                allPlayerIdsForReset, currentTime, 10);

        //验证Leaderboard已被删除
        assert RankingService.getLeaderboard(rankingServiceRepositorySet) == null;

        //异步执行PlayerRankingItem重置任务段
        String resetTaskName = "PlayerRankingItem-reset-" + currentTime;
        String resetSegmentId = RankingResetService.takePlayerRankingItemResetSegmentToExecute(rankingResetServiceRepositorySet,
                resetTaskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert resetSegmentId != null;
        int resetCount = RankingResetService.executePlayerRankingItemResetSegment(rankingResetServiceRepositorySet,
                resetSegmentId);
        assert resetCount == 10;
        RankingResetService.completePlayerRankingItemResetSegment(rankingResetServiceRepositorySet,
                resetSegmentId);

        //验证PlayerRankingItem的排行指标和排名都已置零
        PlayerRankingItem resetItem9 = RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId9);
        assert resetItem9.getMetricValue() == 0;
        assert resetItem9.getRank() == 0;
        PlayerRankingItem resetItem10 = RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId10);
        assert resetItem10.getMetricValue() == 0;
        assert resetItem10.getRank() == 0;

        //重置PlayerRankingChangeItem
        List<String> allPlayerIdStringsForReset = new ArrayList<>();
        allPlayerIdStringsForReset.add(String.valueOf(playerId1));
        allPlayerIdStringsForReset.add(String.valueOf(playerId2));
        allPlayerIdStringsForReset.add(String.valueOf(playerId3));
        allPlayerIdStringsForReset.add(String.valueOf(playerId4));
        allPlayerIdStringsForReset.add(String.valueOf(playerId5));
        allPlayerIdStringsForReset.add(String.valueOf(playerId6));
        allPlayerIdStringsForReset.add(String.valueOf(playerId7));
        allPlayerIdStringsForReset.add(String.valueOf(playerId8));
        allPlayerIdStringsForReset.add(String.valueOf(playerId9));
        allPlayerIdStringsForReset.add(String.valueOf(playerId10));

        RankingChangeResetService.resetRankingChange(rankingChangeResetServiceRepositorySet,
                allPlayerIdStringsForReset, currentTime, 10);

        //异步执行PlayerRankingChangeItem重置任务段
        String changeResetTaskName = "PlayerRankingChangeItem-reset-" + currentTime;
        String changeResetSegmentId = RankingChangeResetService.takePlayerRankingChangeItemResetSegmentToExecute(rankingChangeResetServiceRepositorySet,
                changeResetTaskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert changeResetSegmentId != null;
        int changeResetCount = RankingChangeResetService.executePlayerRankingChangeItemResetSegment(rankingChangeResetServiceRepositorySet,
                changeResetSegmentId);
        assert changeResetCount == 10;
        RankingChangeResetService.completePlayerRankingChangeItemResetSegment(rankingChangeResetServiceRepositorySet,
                changeResetSegmentId);

        //验证PlayerRankingChangeItem的上次排名和当前排名都已置零
        PlayerRankingChangeItem resetChangeItem9 = RankingChangeService.getPlayerRankingChangeItem(rankingChangeServiceRepositorySet, playerId9);
        assert resetChangeItem9.getLastRank() == 0;
        assert resetChangeItem9.getCurrentRank() == 0;
        PlayerRankingChangeItem resetChangeItem10 = RankingChangeService.getPlayerRankingChangeItem(rankingChangeServiceRepositorySet, playerId10);
        assert resetChangeItem10.getLastRank() == 0;
        assert resetChangeItem10.getCurrentRank() == 0;

    }

    private List<PlayerRankingItem> getAllPlayerRankingItems(long playerId1, long playerId2, long playerId3, long playerId4, long playerId5, long playerId6, long playerId7, long playerId8, long playerId9, long playerId10) {
        List<PlayerRankingItem> allRankingItems = new ArrayList<>();
        allRankingItems.add(RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId1));
        allRankingItems.add(RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId2));
        allRankingItems.add(RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId3));
        allRankingItems.add(RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId4));
        allRankingItems.add(RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId5));
        allRankingItems.add(RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId6));
        allRankingItems.add(RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId7));
        allRankingItems.add(RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId8));
        allRankingItems.add(RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId9));
        allRankingItems.add(RankingService.getPlayerRankingItem(rankingServiceRepositorySet, playerId10));
        return allRankingItems;
    }

    PlayerRankingItemRepository playerRankingItemRepository = TestCommonRepository.instance(PlayerRankingItemRepository.class);
    LeaderboardRepository leaderboardRepository = TestCommonSingletonRepository.instance(LeaderboardRepository.class);
    PlayerRankingItemUpdateTaskRepository playerRankingItemUpdateTaskRepository = TestCommonRepository.instance(PlayerRankingItemUpdateTaskRepository.class);
    PlayerRankingItemUpdateTaskSegmentRepository playerRankingItemUpdateTaskSegmentRepository = TestCommonRepository.instance(PlayerRankingItemUpdateTaskSegmentRepository.class);
    PlayerRankingChangeItemRepository playerRankingChangeItemRepository = TestCommonRepository.instance(PlayerRankingChangeItemRepository.class);
    PlayerRankingChangeItemUpdateTaskRepository playerRankingChangeItemUpdateTaskRepository = TestCommonRepository.instance(PlayerRankingChangeItemUpdateTaskRepository.class);
    PlayerRankingChangeItemUpdateTaskSegmentRepository playerRankingChangeItemUpdateTaskSegmentRepository = TestCommonRepository.instance(PlayerRankingChangeItemUpdateTaskSegmentRepository.class);
    PlayerRankingItemResetTaskRepository playerRankingItemResetTaskRepository = TestCommonRepository.instance(PlayerRankingItemResetTaskRepository.class);
    PlayerRankingItemResetTaskSegmentRepository playerRankingItemResetTaskSegmentRepository = TestCommonRepository.instance(PlayerRankingItemResetTaskSegmentRepository.class);
    PlayerRankingChangeItemResetTaskRepository playerRankingChangeItemResetTaskRepository = TestCommonRepository.instance(PlayerRankingChangeItemResetTaskRepository.class);
    PlayerRankingChangeItemResetTaskSegmentRepository playerRankingChangeItemResetTaskSegmentRepository = TestCommonRepository.instance(PlayerRankingChangeItemResetTaskSegmentRepository.class);

    RankingServiceRepositorySet rankingServiceRepositorySet = new RankingServiceRepositorySet() {
        @Override
        public PlayerRankingItemRepository getPlayerRankingItemRepository() {
            return playerRankingItemRepository;
        }

        @Override
        public LeaderboardRepository getLeaderboardRepository() {
            return leaderboardRepository;
        }

        @Override
        public PlayerRankingItemUpdateTaskRepository getPlayerRankingItemUpdateTaskRepository() {
            return playerRankingItemUpdateTaskRepository;
        }

        @Override
        public PlayerRankingItemUpdateTaskSegmentRepository getPlayerRankingItemUpdateTaskSegmentRepository() {
            return playerRankingItemUpdateTaskSegmentRepository;
        }
    };

    RankingChangeServiceRepositorySet rankingChangeServiceRepositorySet = new RankingChangeServiceRepositorySet() {

        @Override
        public PlayerRankingChangeItemRepository getPlayerRankingChangeItemRepository() {
            return playerRankingChangeItemRepository;
        }

        @Override
        public PlayerRankingChangeItemUpdateTaskRepository getPlayerRankingChangeItemUpdateTaskRepository() {
            return playerRankingChangeItemUpdateTaskRepository;
        }

        @Override
        public PlayerRankingChangeItemUpdateTaskSegmentRepository getPlayerRankingChangeItemUpdateTaskSegmentRepository() {
            return playerRankingChangeItemUpdateTaskSegmentRepository;
        }
    };

    RankingResetServiceRepositorySet rankingResetServiceRepositorySet = new RankingResetServiceRepositorySet() {

        @Override
        public PlayerRankingItemRepository getPlayerRankingItemRepository() {
            return playerRankingItemRepository;
        }

        @Override
        public LeaderboardRepository getLeaderboardRepository() {
            return leaderboardRepository;
        }

        @Override
        public PlayerRankingItemResetTaskRepository getPlayerRankingItemResetTaskRepository() {
            return playerRankingItemResetTaskRepository;
        }

        @Override
        public PlayerRankingItemResetTaskSegmentRepository getPlayerRankingItemResetTaskSegmentRepository() {
            return playerRankingItemResetTaskSegmentRepository;
        }
    };

    RankingChangeResetServiceRepositorySet rankingChangeResetServiceRepositorySet = new RankingChangeResetServiceRepositorySet() {

        @Override
        public PlayerRankingChangeItemRepository getPlayerRankingChangeItemRepository() {
            return playerRankingChangeItemRepository;
        }

        @Override
        public PlayerRankingChangeItemResetTaskRepository getPlayerRankingChangeItemResetTaskRepository() {
            return playerRankingChangeItemResetTaskRepository;
        }

        @Override
        public PlayerRankingChangeItemResetTaskSegmentRepository getPlayerRankingChangeItemResetTaskSegmentRepository() {
            return playerRankingChangeItemResetTaskSegmentRepository;
        }
    };

}
