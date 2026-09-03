package com.laigu.laigu.duel.newcard;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** 新版战斗状态的 NBT 持久化；字段采用独立命名空间，兼容旧存档缺失字段。 */
public final class BattleStatePersistence
{
    private static final String ROOT = "laigu_new_battle";
    private BattleStatePersistence() {}

    public static void save(BattleState state, CompoundTag parent)
    {
        CompoundTag root = new CompoundTag();
        root.putInt("round", state.round());
        for (int side = 0; side < BattleState.SIDES; side++)
        {
            CompoundTag s = new CompoundTag();
            s.putInt("hand", state.handSize(side));
            ListTag slots = new ListTag();
            for (int slot = 0; slot < BattleState.SLOTS; slot++)
            {
                CompoundTag c = new CompoundTag();
                CardRuntimeState runtime = state.cardStateAt(side, slot);
                c.putIntArray("dice", runtime.dice());
                CompoundTag counters = new CompoundTag();
                for (java.util.Map.Entry<String, Integer> entry : runtime.counters().entrySet())
                    counters.putInt(entry.getKey(), entry.getValue());
                if (!counters.isEmpty()) c.put("counters", counters);
                c.putInt("persistentBase", runtime.persistentBaseBonus());
                c.putInt("persistentMultiplier", runtime.persistentMultiplierBonus());
                c.putInt("persistentExtra", runtime.persistentExtraBonus());
                if (runtime.consumed()) c.putBoolean("consumed", true);
                if (runtime.roundsOnField() > 0) c.putInt("roundsOnField", runtime.roundsOnField());
                slots.add(c);
            }
            s.put("slots", slots);
            root.put("side" + side, s);
        }
        parent.put(ROOT, root);
    }

    public static BattleState load(CompoundTag parent)
    {
        return loadInto(new BattleState(), parent);
    }

    public static BattleState loadInto(BattleState state, CompoundTag parent)
    {
        java.util.Objects.requireNonNull(state);
        java.util.Objects.requireNonNull(parent);
        if (!parent.contains(ROOT, Tag.TAG_COMPOUND)) return state;
        CompoundTag root = parent.getCompound(ROOT);
        if (root.contains("round", Tag.TAG_INT)) state.setRound(Math.max(1, root.getInt("round")));
        for (int side = 0; side < BattleState.SIDES; side++)
        {
            CompoundTag s = root.getCompound("side" + side);
            if (s.contains("hand", Tag.TAG_INT)) state.setHandSizeForPersistence(side, Math.max(0, s.getInt("hand")));
            ListTag slots = s.getList("slots", Tag.TAG_COMPOUND);
            for (int slot = 0; slot < Math.min(BattleState.SLOTS, slots.size()); slot++)
            {
                CompoundTag c = slots.getCompound(slot);
                CardRuntimeState runtime = state.cardStateAt(side, slot);
                runtime.setDice(java.util.Arrays.stream(c.getIntArray("dice")).boxed().toList());
                CompoundTag counters = c.getCompound("counters");
                for (String name : counters.getAllKeys())
                    runtime.setCounter(name, Math.max(0, counters.getInt(name)));
                runtime.addPersistentBaseBonus(c.getInt("persistentBase"));
                runtime.addPersistentMultiplierBonus(c.getInt("persistentMultiplier"));
                runtime.addPersistentExtraBonus(c.getInt("persistentExtra"));
                if (c.getBoolean("consumed")) runtime.markConsumed();
                if (c.contains("roundsOnField", Tag.TAG_INT))
                    for (int i = 0; i < c.getInt("roundsOnField"); i++) runtime.incrementRoundsOnField();
            }
        }
        return state;
    }
}
