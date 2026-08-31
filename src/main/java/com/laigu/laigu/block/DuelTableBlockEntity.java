package com.laigu.laigu.block;

import com.laigu.laigu.container.DeckBoxContainer;
import com.laigu.laigu.container.DuelTableMenu;
import com.laigu.laigu.duel.DuelActions;
import com.laigu.laigu.duel.DuelAi;
import com.laigu.laigu.duel.DuelGame;
import com.laigu.laigu.item.CardItem;
import com.laigu.laigu.item.DeckBoxItem;
import com.laigu.laigu.network.DuelEmojiS2CPacket;
import com.laigu.laigu.network.DuelStateS2CPacket;
import com.laigu.laigu.network.ModPackets;
import com.laigu.laigu.registry.ModBlockEntities;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 对战方块实体：承载一局 {@link DuelGame} 状态机 + 双人侧位登记 + AI 驱动 + 黑暗对决。
 * <ul>
 *   <li>登记：右键打开登记界面，放入 16 张来古牌点「确定」提交；第一个提交者为主机。</li>
 *   <li>设置：主机进入战斗设置界面，可开关黑暗对决、补充 AI 对手，点「开始」开打。</li>
 *   <li>黑暗对决：胜者从败者真实卡组匣中抢走一张卡，署名改为胜者。</li>
 *   <li>对局状态经 {@link DuelStateS2CPacket} 全量广播；完整持久化到方块 NBT。</li>
 *   <li>方块被不可抗力移除：对局作废，登记卡组副本返还给玩家。</li>
 * </ul>
 */
public class DuelTableBlockEntity extends BlockEntity
{
    private final RandomSource rnd = RandomSource.create();
    private DuelGame game;
    private int aiCooldown = 0;

    private final UUID[] owner = new UUID[2];
    private final String[] ownerName = new String[2];
    private final boolean[] isAi = new boolean[2];
    /** 主机侧（第一个提交卡组的玩家）。 */
    private int hostSide = -1;
    private boolean rewardDone = false;
    /** 本局是否已结算「参战胜利次数」（每局只结算一次）。 */
    private boolean winsCounted = false;

    /** 观战者（对决开始后点击方块加入；只观战不操作，随房间解散一并退出）。 */
    private final List<UUID> spectators = new ArrayList<>();

    /** 每位玩家打开登记界面时的卡组草稿（提交后清除，不消耗本体）。 */
    private final Map<UUID, DeckBoxContainer> pendingDecks = new HashMap<>();

    public DuelTableBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.DUEL_TABLE.get(), pos, state);
    }

    // ================= 方块交互 =================

    /** 右键处理（服务端）。返回是否成功（成功则调用方广播状态给该玩家）。 */
    public boolean handleUse(Player player)
    {
        if (level == null || level.isClientSide) return false;
        // 对局进行中或已结束（认输/分出胜负）：玩家可重新进入查看结算 / 再来一局
        if (game != null && (game.isStarted() || game.isFinished()))
        {
            if (sideOf(player) >= 0) return true; // 重新进入对局
            if (game.isStarted())
            {
                joinSpectator(player); // 观战：点击方块参加（默认主机视角，双方透明）
                return false;          // joinSpectator 已直接推送观战视角
            }
            sendMsg(player, "message.laigu.duel_ongoing");
            return false;
        }
        // 已登记（等待主机开始）：主机再右键可重新打开设置界面（ESC/E 关掉后能回来）；
        // 客人（非主机）提示等待主机
        int mySide = sideOf(player);
        if (mySide >= 0)
        {
            if (game != null && !game.isStarted() && mySide == hostSide)
            {
                sendUi(player, 1); // 重新打开主机设置界面
                return false;
            }
            sendMsg(player, "message.laigu.duel_waiting");
            return false;
        }
        // 登记 = 提交预设好的卡组包：必须手持卡组匣（含已编排的卡组）
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof DeckBoxItem))
        {
            sendMsg(player, "message.laigu.duel_need_box");
            return false;
        }
        boolean creative = held.getItem() instanceof DeckBoxItem dc && dc.creative;
        // 创造卡组：恒合法，直接进；普通卡组：必须已确认构建（卡组合法）才进，否则提示并进不去
        if (!creative && !DeckBoxContainer.isBuilt(held))
        {
            sendMsg(player, "message.laigu.deck_not_built");
            return false;
        }
        openRegisterMenu(player, held);
        return false;
    }

    private void openRegisterMenu(Player player, ItemStack boxStack)
    {
        DeckBoxContainer container = pendingDecks.computeIfAbsent(player.getUUID(),
                k -> new DeckBoxContainer(ItemStack.EMPTY));
        container.loadFrom(boxStack); // 同步卡组包最新内容（预设卡组草稿，不消耗本体）
        // 必须用 NetworkHooks.openScreen 并写入方块坐标：
        // Player.openMenu 不写额外数据，客户端 DuelTableMenu.fromNetwork 读不到 BlockPos 会 NPE。
        NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider()
        {
            @Override
            public Component getDisplayName()
            {
                return Component.translatable("block.laigu.duel_table");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p)
            {
                return new DuelTableMenu(containerId, inv, container, worldPosition,
                        boxStack.getItem() instanceof DeckBoxItem di && di.creative);
            }
        }, buf -> buf.writeBlockPos(worldPosition));
    }

    // ================= 操作分发（含登记/设置） =================

    public void handleAction(Player player, int action, int a, int b)
    {
        switch (action)
        {
            case DuelActions.REGISTER_CONFIRM -> doRegister(player);
            case DuelActions.HOST_SETTINGS -> doHostSettings(player, a == 1);
            case DuelActions.ADD_AI -> doAddAi(player);
            case DuelActions.LEAVE_ROOM -> doLeaveRoom(player);
            case DuelActions.SPECTATE_LEAVE -> doSpectateLeave(player);
            case DuelActions.EMOJI -> doEmoji(player, a);
            default -> doGameAction(player, action, a, b);
        }
    }

    private void doGameAction(Player player, int action, int a, int b)
    {
        if (game == null) return;
        // 对局未开始且未结束 → 忽略（避免误触）
        if (!game.isStarted() && !game.isFinished()) return;
        int side = sideOf(player);
        if (side < 0) return;
        game.applyAction(side, action, a, b);
        if (game.lastMsg != null && game.lastMsg.length() > 0)
        {
            sendMsg(player, "message.laigu.duel_msg", game.lastMsg);
        }
        // 认输/逃跑：视为离开房间 → 双方立即退出房间、房间解散，
        // 不再广播结算，对方也不会再被「再来一局」拉回。
        if (action == DuelActions.FORFEIT)
        {
            // 认输也算「游戏失败」：黑暗对决下照常被夺卡（关界面/逃跑躲不掉），
            // 必须在 leaveRoom（game 置空）之前结算奖励。
            checkReward();
            countWins();
            leaveRoom(side, true);
            return;
        }
        // 再来一局重新开局后，重置本局奖励标记（每局可抢一次 / 每局胜利结算一次）
        if (game.isStarted() && game.phase() == DuelGame.Phase.DEPLOY)
        {
            rewardDone = false;
            winsCounted = false;
        }
        broadcastAll();
        checkReward();
        countWins();
    }

    // ---- 离开房间 / 房间解散 ----

    /**
     * 玩家离开房间（关闭界面/逃跑/认输/掉线）。规则：离开 = 退出房间，
     * 对方也立即退出（关闭对战界面），房间解散、可重新登记；不会有人被拉回。
     *
     * @param forfeit true=认输（对方提示「对方认输，你获胜」）；false=普通离开
     */
    private void leaveRoom(int leavingSide, boolean forfeit)
    {
        if (level == null || level.isClientSide) return;
        for (int s = 0; s < 2; s++)
        {
            if (isAi[s] || owner[s] == null) continue;
            ServerPlayer p = ((ServerLevel) level).getServer().getPlayerList().getPlayer(owner[s]);
            if (p == null) continue;
            if (s != leavingSide)
            {
                sendMsg(p, forfeit ? "message.laigu.duel_forfeit_win" : "message.laigu.duel_other_left");
            }
            sendUi(p, 4); // 关闭对战界面（含离开者自己）
        }
        // 房间解散：观战者一并被踢出
        for (UUID su : spectators)
        {
            ServerPlayer sp = ((ServerLevel) level).getServer().getPlayerList().getPlayer(su);
            if (sp != null) sendUi(sp, 4);
        }
        resetRoom();
    }

    /** 关闭界面/退出房间：对局进行中按认输处理，正常结束后直接解散房间。 */
    private void doLeaveRoom(Player player)
    {
        int side = sideOf(player);
        if (side < 0) return;
        if (game != null && game.isStarted() && !game.isFinished())
        {
            game.forfeit(side);
            leaveRoom(side, true);
        }
        else
        {
            leaveRoom(side, false);
        }
    }

    /** 退出观战：只把该玩家移出观战名单并关闭其观战界面，不影响房间与对局。 */
    private void doSpectateLeave(Player player)
    {
        if (!spectators.remove(player.getUUID())) return;
        sendUi(player, 4); // 关闭观战界面（房间不解散）
    }

    /** 表情：仅对局进行中有效；发送者=登记双方其一，或观战者（side=-1）。广播给双方与全部观战者。 */
    private void doEmoji(Player player, int index)
    {
        if (game == null || (!game.isStarted() && !game.isFinished())) return;
        if (index < 0 || index >= DuelActions.EMOJI_COUNT) return;
        int side = sideOf(player);
        if (side < 0 && !spectators.contains(player.getUUID())) return; // 不在对局内
        DuelEmojiS2CPacket pkt = new DuelEmojiS2CPacket(worldPosition, side, index);
        for (int s = 0; s < 2; s++)
        {
            if (isAi[s] || owner[s] == null) continue;
            ServerPlayer p = ((ServerLevel) level).getServer().getPlayerList().getPlayer(owner[s]);
            if (p != null)
            {
                ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
            }
        }
        for (UUID su : spectators)
        {
            ServerPlayer sp = ((ServerLevel) level).getServer().getPlayerList().getPlayer(su);
            if (sp != null)
            {
                ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), pkt);
            }
        }
    }

    /** 房间解散：清空对局与登记，方块恢复空闲（可重新登记开新局）。 */
    private void resetRoom()
    {
        game = null;
        for (int s = 0; s < 2; s++)
        {
            owner[s] = null;
            ownerName[s] = null;
            isAi[s] = false;
        }
        hostSide = -1;
        rewardDone = false;
        winsCounted = false;
        pendingDecks.clear();
        spectators.clear();
        setChanged();
    }

    /** 掉线检测：某方登记玩家已离线 → 视为离开房间，解散房间；掉线观战者只移除。 */
    private void checkOfflineOwners()
    {
        if (level == null || level.isClientSide) return;
        // 掉线的观战者直接从名单移除（不影响对局与房间）
        spectators.removeIf(su -> ((ServerLevel) level).getServer().getPlayerList().getPlayer(su) == null);
        if (game == null)
        {
            // 登记阶段（尚未开局）也可能有登记者掉线，同样清空释放
            for (int s = 0; s < 2; s++)
            {
                if (owner[s] != null && !isAi[s]
                        && ((ServerLevel) level).getServer().getPlayerList().getPlayer(owner[s]) == null)
                {
                    leaveRoom(s, false);
                    return;
                }
            }
            return;
        }
        for (int s = 0; s < 2; s++)
        {
            if (isAi[s] || owner[s] == null) continue;
            if (((ServerLevel) level).getServer().getPlayerList().getPlayer(owner[s]) == null)
            {
                leaveRoom(s, false);
                return;
            }
        }
    }

    // ---- 登记 ----

    private void doRegister(Player player)
    {
        if (game != null && game.isStarted())
        {
            sendMsg(player, "message.laigu.duel_ongoing");
            return;
        }
        // 只有已确认构建的卡组包才能参与对战（防止玩家退出构建界面后改卡）
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof DeckBoxItem) || !DeckBoxContainer.isBuilt(held))
        {
            sendMsg(player, "message.laigu.deck_not_built");
            return;
        }
        if (game != null && game.isFinished()) game = null;
        if (game == null) game = new DuelGame(rnd);

        DeckBoxContainer container = pendingDecks.remove(player.getUUID());
        boolean creative = held.getItem() instanceof DeckBoxItem di2 && di2.creative;
        // 创造卡组或未走登记菜单时：直接读卡组包 NBT 里的卡（避免 pendingDecks 为空导致空卡组）
        List<ItemStack> deck = (container != null && (creative || container.snapshot().size() > 0))
                ? containerToDeck(container) : DeckBoxContainer.readDeck(held);
        if (!creative && deck.size() != DuelGame.DECK_SIZE)
        {
            sendMsg(player, "message.laigu.deck_not_full", deck.size());
            return;
        }
        if (!creative && !DuelGame.isDeckLegal(deck))
        {
            sendMsg(player, "message.laigu.deck_illegal");
            return;
        }

        int side = sideOf(player);
        if (side < 0)
        {
            if (owner[0] == null && !isAi[0]) side = 0;
            else if (owner[1] == null && !isAi[1]) side = 1;
            else
            {
                sendMsg(player, "message.laigu.duel_full");
                return;
            }
        }
        owner[side] = player.getUUID();
        ownerName[side] = player.getGameProfile().getName();
        isAi[side] = false;
        game.creative = creative;
        game.setDeck(side, deck);
        setChanged();
        if (hostSide < 0) hostSide = side;

        if (side == hostSide)
        {
            sendMsg(player, "message.laigu.duel_registered_host");
            sendUi(player, 1); // 打开主机设置界面
        }
        else
        {
            sendMsg(player, "message.laigu.duel_registered_wait");
            sendUi(player, 2); // 关闭登记界面，等待主机
        }
        notifyHostStatus(); // 通知主机刷新设置界面（第二侧是否已就绪）
    }

    private static List<ItemStack> containerToDeck(DeckBoxContainer c)
    {
        List<ItemStack> deck = new ArrayList<>();
        if (c == null) return deck;
        for (int i = 0; i < DeckBoxContainer.SLOT_COUNT; i++)
        {
            if (!c.getItem(i).isEmpty()) deck.add(c.getItem(i));
        }
        return deck;
    }

    // ---- 主机设置 / AI ----

    private void doHostSettings(Player player, boolean dark)
    {
        if (sideOf(player) != hostSide)
        {
            sendMsg(player, "message.laigu.duel_host_only");
            return;
        }
        if (game == null || game.isStarted()) return;
        if (!game.sideReady(0) || !game.sideReady(1))
        {
            sendMsg(player, "message.laigu.duel_waiting");
            return;
        }
        game.darkMode = dark;
        game.start();
        broadcastAll();
        checkReward();
        countWins();
    }

    private void doAddAi(Player player)
    {
        if (sideOf(player) != hostSide)
        {
            sendMsg(player, "message.laigu.duel_host_only");
            return;
        }
        if (game == null) game = new DuelGame(rnd);
        if (game.isStarted()) return;
        int aiSide = -1;
        for (int s = 0; s < 2; s++)
        {
            if (owner[s] == null && !isAi[s]) aiSide = s;
        }
        if (aiSide < 0)
        {
            sendMsg(player, "message.laigu.duel_full");
            return;
        }
        isAi[aiSide] = true;
        ownerName[aiSide] = DuelAi.AI_NAME;
        owner[aiSide] = null;
        game.setDeck(aiSide, DuelAi.presetDeck());
        setChanged();
        sendMsg(player, "message.laigu.ai_added", DuelAi.AI_NAME);
        notifyHostStatus(); // AI 已就绪 → 主机设置界面点亮「开始对战」
    }

    // ================= 黑暗对决奖励 =================

    private void checkReward()
    {
        if (rewardDone || game == null || !game.isFinished() || !game.darkMode) return;
        int winner = game.winnerLast();
        if (winner < 0) return;
        int loser = 1 - winner;

        // 从败者「登记卡组」（存储在方块内的副本）随机移除一张，交给胜者。
        // 同步从败者背包真实卡组匣移除同一张卡，让损失在实物上也可见。
        ItemStack stolen = removeRandomFromStoredDeck(loser);
        if (stolen == null || stolen.isEmpty()) { rewardDone = true; return; }
        syncRemoveFromLoserBox(loser, stolen);

        CardNbt.setOwnerBy(stolen, ownerName[winner], owner[winner]);
        if (isAi[winner])
        {
            // AI 胜者没有背包收卡：被夺的卡掉落在对战桌旁（损失依然真实发生）
            ServerLevel sl = (ServerLevel) level;
            sl.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(sl,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, stolen));
        }
        else if (owner[winner] != null)
        {
            ServerPlayer sp = ((ServerLevel) level).getServer().getPlayerList().getPlayer(owner[winner]);
            if (sp != null)
            {
                boolean ok = sp.addItem(stolen);
                if (!ok) sp.drop(stolen, false);
                sendMsg(sp, "message.laigu.dark_reward", stolen.getDisplayName().getString());
            }
        }
        rewardDone = true;
        setChanged();
    }

    /** 每局结束时，为胜者本局实际部署过的卡牌实例（背包卡组匣内或散落的）累加「参战胜利次数」。 */
    private void countWins()
    {
        if (winsCounted || game == null || !game.isFinished()) return;
        winsCounted = true; // 一局只结算一次：无论是否有胜者，先锁住再判断
        if (level == null || level.isClientSide) return;
        int winner = game.winnerLast();
        if (winner < 0) return;
        Set<String> uids = game.deployedUids(winner);
        if (uids == null || uids.isEmpty()) return;
        if (owner[winner] == null) return;
        ServerPlayer sp = ((ServerLevel) level).getServer().getPlayerList().getPlayer(owner[winner]);
        if (sp == null) return;

        for (int i = 0; i < sp.getInventory().getContainerSize(); i++)
        {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof DeckBoxItem)
            {
                DeckBoxContainer c = new DeckBoxContainer(s);
                boolean changed = false;
                for (int si = 0; si < DeckBoxContainer.SLOT_COUNT; si++)
                {
                    ItemStack card = c.getItem(si);
                    if (!card.isEmpty() && uids.contains(CardNbt.uidOf(card)))
                    {
                        CardNbt.addWin(card);
                        changed = true;
                    }
                }
                if (changed)
                {
                    c.save();
                    sp.getInventory().setItem(i, s); // NBT 写回背包
                }
            }
            else if (s.getItem() instanceof CardItem && uids.contains(CardNbt.uidOf(s)))
            {
                CardNbt.addWin(s);
                sp.getInventory().setItem(i, s);
            }
        }
        setChanged();
    }

    /** 从败者「登记卡组」（存储的副本）随机移除一张卡并返回；卡组已空返回空。 */
    private ItemStack removeRandomFromStoredDeck(int side)
    {
        if (game == null) return ItemStack.EMPTY;
        List<ItemStack> deck = game.deckOriginalCopy(side);
        if (deck.isEmpty()) return ItemStack.EMPTY;
        int idx = rnd.nextInt(deck.size());
        ItemStack stolen = deck.get(idx).copy();
        game.removeDeckCard(side, idx); // 登记卡组少一张（再来一局/存档也随之少一张）
        return stolen;
    }

    /** 败者背包真实卡组匣同步移除同一张卡（损失在实物上可见）；无匣/匣空则只损登记卡组。 */
    private void syncRemoveFromLoserBox(int loserSide, ItemStack stolen)
    {
        if (level == null || isAi[loserSide]) return;
        ServerPlayer loser = ((ServerLevel) level).getServer().getPlayerList().getPlayer(owner[loserSide]);
        if (loser == null) return;
        for (int i = 0; i < loser.getInventory().getContainerSize(); i++)
        {
            ItemStack s = loser.getInventory().getItem(i);
            if (s.getItem() instanceof DeckBoxItem)
            {
                DeckBoxContainer c = new DeckBoxContainer(s);
                int target = -1;
                for (int si = 0; si < DeckBoxContainer.SLOT_COUNT; si++)
                {
                    if (ItemStack.isSameItemSameTags(c.getItem(si), stolen)) { target = si; break; }
                }
                if (target < 0)
                {
                    // 匣里没有同一张（登记后改过）→ 随机删一张，保持「少一张」
                    for (int si = 0; si < DeckBoxContainer.SLOT_COUNT; si++)
                    {
                        if (!c.getItem(si).isEmpty()) { target = si; break; }
                    }
                }
                if (target >= 0)
                {
                    c.removeItem(target, 1);
                    c.save();
                    loser.getInventory().setItem(i, s); // 写回容器（NBT 已改）
                    sendMsg(loser, "message.laigu.dark_stolen", stolen.getDisplayName().getString());
                }
                return;
            }
        }
    }

    // ================= AI 驱动 =================

    public static void serverTick(Level level, BlockPos pos, BlockState state, DuelTableBlockEntity be)
    {
        be.tickAi();
        be.checkReward();
        be.countWins();
    }

    private void tickAi()
    {
        if (level == null || level.isClientSide) return;
        checkOfflineOwners();   // 掉线 = 离房，解散房间
        if (game == null || !game.isStarted() || game.isFinished()) return;
        if (--aiCooldown > 0) return;
        for (int s = 0; s < 2; s++)
        {
            if (isAi[s] && needsAiAction(s))
            {
                aiCooldown = 8;
                DuelAi.act(game, s);
                broadcastAll();
                checkReward();
                countWins();
                return;
            }
        }
    }

    private boolean needsAiAction(int side)
    {
        if (game == null) return false;
        return switch (game.phase())
        {
            case DEPLOY -> !game.deployDone(side);
            case DRAFT -> game.currentPicker() == side && game.pickTarget(side) > 0;
            case PLACE -> !game.placeDone(side);
            case ROUND_END -> !game.roundEndDone(side);
            default -> false;
        };
    }

    // ================= 状态广播 =================

    public void broadcastAll()
    {
        if (level == null || level.isClientSide) return;
        if (game == null) return; // FINISHED（含认输）也要广播，让玩家看到结算
        for (int s = 0; s < 2; s++)
        {
            if (isAi[s] || owner[s] == null) continue;
            ServerPlayer p = ((ServerLevel) level).getServer().getPlayerList().getPlayer(owner[s]);
            if (p != null)
            {
                ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                        new DuelStateS2CPacket(worldPosition, stateOf(s)));
            }
        }
        // 观战者：实时推送主机视角（双方透明）
        for (UUID su : spectators)
        {
            ServerPlayer sp = ((ServerLevel) level).getServer().getPlayerList().getPlayer(su);
            if (sp != null)
            {
                ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new DuelStateS2CPacket(worldPosition, spectateState()));
            }
        }
    }

    public void broadcastTo(Player player)
    {
        if (game == null) return; // 含已结束（FINISHED）：仍发送结算状态供重新进入查看
        int side = sideOf(player);
        if (side < 0) return;
        if (player instanceof ServerPlayer sp)
        {
            ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new DuelStateS2CPacket(worldPosition, stateOf(side)));
        }
    }

    /** 某方视角状态 + 双方名字（进入战斗后用真实名字替代 A/B 标签）。 */
    private CompoundTag stateOf(int side)
    {
        CompoundTag st = game.serializeState(side);
        st.putString("myName", nameOf(side));
        st.putString("oppName", nameOf(1 - side));
        return st;
    }

    /** 观战视角：默认主机视角（mySide=hostSide），双方操作透明（spectate=true）。 */
    private CompoundTag spectateState()
    {
        CompoundTag st = game.serializeState(hostSide, true);
        st.putString("myName", nameOf(hostSide));
        st.putString("oppName", nameOf(1 - hostSide));
        return st;
    }

    /** 加入观战并立即推送观战视角。 */
    private void joinSpectator(Player player)
    {
        if (spectators.contains(player.getUUID())) return;
        spectators.add(player.getUUID());
        if (player instanceof ServerPlayer sp)
        {
            ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new DuelStateS2CPacket(worldPosition, spectateState()));
        }
    }

    private String nameOf(int side)
    {
        if (ownerName[side] != null && !ownerName[side].isEmpty()) return ownerName[side];
        return isAi[side] ? DuelAi.AI_NAME : "对手";
    }

    /** 发送 UI 提示状态包（1=主机设置界面，2=等待中）。 */
    private void sendUi(Player player, int ui)
    {
        if (!(player instanceof ServerPlayer sp)) return;
        CompoundTag t = new CompoundTag();
        t.putInt("ui", ui);
        ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                new DuelStateS2CPacket(worldPosition, t));
    }

    /** 通知主机刷新设置界面：第二侧（AI 或真人）是否已就绪，用于点亮「开始对战」。 */
    private void notifyHostStatus()
    {
        if (hostSide < 0 || level == null || level.isClientSide) return;
        ServerPlayer host = ((ServerLevel) level).getServer().getPlayerList().getPlayer(owner[hostSide]);
        if (host == null) return;
        int other = 1 - hostSide;
        CompoundTag t = new CompoundTag();
        t.putInt("ui", 3);
        t.putBoolean("aiReady", isAi[other]);
        t.putBoolean("humanJoined", owner[other] != null);
        ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> host),
                new DuelStateS2CPacket(worldPosition, t));
    }

    // ================= 方块被移除：对局作废，返还卡组 =================

    public void handleBlockRemoved()
    {
        if (level == null || level.isClientSide) return;
        if (game != null)
        {
            ServerLevel sl = (ServerLevel) level;
            // 对局进行中（未分出胜负）被移除：对局作废，双方登记卡组副本返还
            if (game.isStarted() && !game.isFinished())
            {
                for (int s = 0; s < 2; s++)
                {
                    if (isAi[s] || owner[s] == null) continue;
                    ServerPlayer p = sl.getServer().getPlayerList().getPlayer(owner[s]);
                    if (p != null && game.sideReady(s))
                    {
                        for (ItemStack stack : game.deckOriginalCopy(s))
                        {
                            boolean ok = p.addItem(stack);
                            if (!ok) p.drop(stack, false);
                        }
                        sendMsg(p, "message.laigu.duel_cancelled");
                    }
                }
            }
            // 无论对局是否结束，双方与观战者都关闭对战界面：否则 resetRoom 后 game==null，
            // 客户端残留的界面里「认输/再来一局」发包被静默忽略，按钮点了没反应 → 卡死。
            for (int s = 0; s < 2; s++)
            {
                if (isAi[s] || owner[s] == null) continue;
                ServerPlayer p = sl.getServer().getPlayerList().getPlayer(owner[s]);
                if (p != null) sendUi(p, 4);
            }
            for (UUID su : spectators)
            {
                ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(su);
                if (sp != null) sendUi(sp, 4);
            }
        }
        resetRoom();
    }

    // ================= 侧位 =================

    public int sideOf(Player player)
    {
        for (int s = 0; s < 2; s++)
        {
            if (!isAi[s] && owner[s] != null && owner[s].equals(player.getUUID())) return s;
        }
        return -1;
    }

    private void sendMsg(Player player, String key, Object... args)
    {
        if (player instanceof ServerPlayer sp)
        {
            sp.displayClientMessage(Component.translatable(key, args), true);
        }
    }

    // ================= 持久化 =================

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        if (game != null)
        {
            tag.put("game", game.toNbt());
        }
        tag.putInt("hostSide", hostSide);
        tag.putBoolean("rewardDone", rewardDone);
        for (int s = 0; s < 2; s++)
        {
            if (owner[s] != null)
            {
                tag.putUUID("owner" + s, owner[s]);
                tag.putString("ownerName" + s, ownerName[s] == null ? "" : ownerName[s]);
            }
            tag.putBoolean("ai" + s, isAi[s]);
        }
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        for (int s = 0; s < 2; s++)
        {
            owner[s] = tag.contains("owner" + s) ? tag.getUUID("owner" + s) : null;
            ownerName[s] = tag.contains("ownerName" + s) ? tag.getString("ownerName" + s) : null;
            isAi[s] = tag.getBoolean("ai" + s);
        }
        hostSide = tag.getInt("hostSide");
        rewardDone = tag.getBoolean("rewardDone");
        if (tag.contains("game"))
        {
            game = DuelGame.fromNbt(tag.getCompound("game"), rnd);
        }
    }
}
