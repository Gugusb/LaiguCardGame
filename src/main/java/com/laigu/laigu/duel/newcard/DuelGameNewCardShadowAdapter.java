package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.DuelGame;
import com.laigu.laigu.duel.FieldCard;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

/**
 * 旧 DuelGame 的只读影子适配器：把真实 ItemStack 映射到新核心，但不修改旧对局结果。
 * 用于逐槽位验证新规则接入，正式切换前由旧 DuelGame 继续作为权威状态。
 */
public final class DuelGameNewCardShadowAdapter
{
    private final NewCardBattle battle;

    public DuelGameNewCardShadowAdapter()
    {
        this(new NewCardBattle());
    }

    public DuelGameNewCardShadowAdapter(NewCardBattle battle)
    {
        this.battle = Objects.requireNonNull(battle);
    }

    public Optional<DuelCard> observePlacement(int side, int slot, ItemStack stack)
    {
        Optional<DuelCard> mapped = CardItemAdapter.create(stack);
        mapped.ifPresent(card -> observeMappedPlacement(side, slot, card));
        return mapped;
    }

    public void synchronizeFrom(DuelGame game)
    {
        Objects.requireNonNull(game);
        for (int side = 0; side < BattleState.SIDES; side++)
        {
            for (int slot = 0; slot < BattleState.SLOTS; slot++)
            {
                FieldCard fieldCard = game.field(side).get(slot);
                if (fieldCard == null) battle.synchronizeEmpty(side, slot);
                else observePlacement(side, slot, fieldCard.card);
            }
        }
    }

    /**
     * 阶段18：事件桥接基线——同步实态手牌数/行动力/行动力上限到影子。
     * 派发前同步，派发后影子与实态的差值即新核心写入量，由钩子实现回写实态。
     */
    public void syncEphemeralFrom(DuelGame game)
    {
        Objects.requireNonNull(game);
        BattleState s = battle.state();
        for (int side = 0; side < BattleState.SIDES; side++)
        {
            s.setHandSizeForPersistence(side, game.hand(side).size());
            s.setActionPoints(side, game.actionPoints(side));
        }
        s.setMaxActionPoints(DuelGame.maxAp(game.round()));
        // 观星金焕章：同步本轮已消耗行动力。
        for (int side2 = 0; side2 < BattleState.SIDES; side2++)
            s.setActionPointsSpent(side2, game.actionPointsSpentThisRound(side2));
    }

    public ScoreComparison compareScore(DuelGame game)
    {
        return BattleScoreAdapter.compare(battle.state(), game);
    }

    public NewCardBattle battle()
    {
        return battle;
    }

    private void observeMappedPlacement(int side, int slot, DuelCard card)
    {
        Optional<CardPlacement> existing = battle.placements().stream()
                .filter(p -> p.side() == side && p.slot() == slot)
                .findFirst();
        if (existing.isEmpty())
        {
            battle.placeCard(side, slot, card);
        }
        else if (existing.get().card().getClass() != card.getClass())
        {
            battle.replaceCard(side, slot, card);
        }
    }
}
