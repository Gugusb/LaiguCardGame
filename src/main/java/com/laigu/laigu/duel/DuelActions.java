package com.laigu.laigu.duel;

/** 客户端 → 服务端 对局操作码（DuelActionC2SPacket 的 action 字段）。 */
public final class DuelActions
{
    public static final int DEPLOY_PUT    = 1; // a=手牌下标, b=场上槽位，放置/替换（消耗 1 行动力）
    public static final int DEPLOY_CONFIRM = 3; // 确认部署（双方都确认后自动揭示）
    public static final int PICK_DIE      = 4; // a=共享池下标，选走该骰
    public static final int SKIP_DRAFT    = 17; // 放弃本次拿骰（跳过当前抓取，进入下一次/布置）
    public static final int PLACE_DIE     = 5; // a=我的骰池下标，b=场上槽位，布置
    public static final int PLACE_TAKE_DIE = 13; // a=场上槽位, b=该卡骰下标，确认前取下退回骰池
    public static final int PLACE_CONFIRM = 6; // 确认布置（未布置的骰继承到下轮）
    public static final int NEXT_ROUND    = 7; // 确认看分，进入下一轮
    public static final int REMATCH       = 8; // 再来一局（同卡组）
    public static final int FORFEIT       = 9; // 认输/中途退出
    public static final int LEAVE_ROOM    = 14; // 离开房间（对局结束后关闭界面时发送；= 退出房间）
    public static final int SPECTATE_LEAVE = 15; // 退出观战（只移除观战者，不动房间）
    public static final int EMOJI         = 16; // a=表情序号(0-7)，以气泡广播给双方与观战者
    public static final int EMOJI_COUNT   = 8;  // 表情总数（客户端 DuelEmoji 与其保持一致）

    // 以下由对战方块实体处理（登记/设置，不经 DuelGame）：
    public static final int REGISTER_CONFIRM = 10; // 从登记界面提交卡组
    public static final int HOST_SETTINGS    = 11; // a=黑暗对决(1/0)，主机开战设置
    public static final int ADD_AI           = 12; // 主机在设置界面追加 AI 对手

    private DuelActions() {}
}
