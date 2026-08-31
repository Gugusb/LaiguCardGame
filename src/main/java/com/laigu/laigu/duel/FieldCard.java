package com.laigu.laigu.duel;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 场上槽位中的一张卡。槽位为固定 5 格数组（null=空槽），
 * 这样【消耗】移出后空位保留、相邻关系不因列表压缩而改变。
 */
public class FieldCard
{
    public static final int MAX_DICE_PER_CARD = 5;

    /** 场上这张卡的物品栈（对战卡副本）。 */
    public final ItemStack card;
    /** 布置到该卡的骰面（1-6）。 */
    public final List<Integer> dice = new ArrayList<>();

    /** 本轮新部署、尚未揭示（卡面朝下）。揭示后置 false。 */
    public boolean faceDown = true;
    /** 上一轮结算时仍在场上（跨轮加成依据）。 */
    public boolean lastedLastRound = false;
    /** 连续在场轮数（新部署计 1，每幸存一轮 +1；老兵类效果依据）。 */
    public int roundsOnField = 1;
    /** 本轮回结算后是否被【消耗】移除。 */
    public boolean consumed = false;
    /** 【激活x】进度：被其他卡效果激活 +1；达到 x 触发奖励并清零；每轮结束清零。 */
    public int activation = 0;
    /** 【固有消耗】：部署即携带【消耗】词条，本轮结束必然移出（见 玩法设计 第 6 条）。 */
    public boolean intrinsicConsume = false;
    /** 【伏击·睡莲】被无效化的骰子数：前 N 颗骰不计入计分/牌型。 */
    public int invalidatedCount = 0;
    /** 金卡破阵/伏击造成的持续状态。 */
    public boolean locked = false;
    public boolean poZhenAlwaysSuccess = false;
    /** 回合结算后由伏击焕章摧毁。 */
    public boolean destroyAtRoundEnd = false;
    /** 永久强化：后续回合持续增加基础分。 */
    public int persistentBaseBonus = 0;
    /** 本轮临时状态。 */
    public int roundExtraBonus = 0;
    public int roundMultBonus = 0;
    public boolean parityOverrideOdd = false;
    public boolean parityOverrideEven = false;

    /** 未被无效化的骰子（用于计分/牌型；前 invalidatedCount 颗被睡莲无效化）。 */
    public List<Integer> activeDice()
    {
        if (invalidatedCount <= 0) return dice;
        List<Integer> out = new ArrayList<>();
        for (int k = invalidatedCount; k < dice.size(); k++) out.add(dice.get(k));
        return out;
    }

    public FieldCard(ItemStack card)
    {
        this.card = card;
    }

    public int diceCount()
    {
        return dice.size();
    }

    public boolean canAddDie()
    {
        return dice.size() < MAX_DICE_PER_CARD;
    }
}
