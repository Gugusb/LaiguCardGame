package com.laigu.laigu.client;

import com.laigu.laigu.duel.DuelActions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 对局内表情：8 个常用像素风表情（16×16 网格，程序化绘制，不依赖字体/贴图，
 * 任何客户端渲染一致）。由 DuelScreen 的表情面板展示、以气泡形式显示。
 */
@OnlyIn(Dist.CLIENT)
public final class DuelEmoji
{
    /** 与 DuelActions.EMOJI_COUNT 保持一致（服务端校验用同一个数）。 */
    public static final int COUNT = DuelActions.EMOJI_COUNT;

    /** 表情名称（面板悬停提示用）。 */
    public static final String[] LABELS = { "微笑", "笑哭", "大哭", "愤怒", "爱心眼", "震惊", "睡觉", "爱心" };

    // ---- 调色板 ----
    private static final int Y = 0xFFE9B84A; // 脸黄
    private static final int K = 0xFF332A20; // 深棕（眼/嘴/眉毛）
    private static final int W = 0xFFFFFFFF; // 白
    private static final int R = 0xFFE24A3C; // 红（愤怒的脸/心）
    private static final int P = 0xFFF29AC1; // 粉（腮红）
    private static final int B = 0xFF5AA9E6; // 蓝（眼泪）

    private static final int[][] GRIDS = new int[COUNT][256];

    static
    {
        buildSmile(GRIDS[0]);
        buildLol(GRIDS[1]);
        buildCry(GRIDS[2]);
        buildAngry(GRIDS[3]);
        buildHeartEyes(GRIDS[4]);
        buildSurprised(GRIDS[5]);
        buildSleepy(GRIDS[6]);
        buildHeart(GRIDS[7]);
    }

    /** 在 (x,y) 画 size×size 的表情。alpha=255 不透明，用于气泡淡出。 */
    public static void draw(GuiGraphics g, int index, int x, int y, int size, int alpha)
    {
        if (index < 0 || index >= COUNT || size <= 0 || alpha <= 0) return;
        int[] grid = GRIDS[index];
        for (int r = 0; r < 16; r++)
        {
            for (int c = 0; c < 16; c++)
            {
                int col = grid[r * 16 + c];
                if (col == 0) continue;
                int x0 = x + c * size / 16, x1 = x + (c + 1) * size / 16;
                int y0 = y + r * size / 16, y1 = y + (r + 1) * size / 16;
                if (x1 <= x0 || y1 <= y0) continue;
                g.fill(x0, y0, x1, y1, (alpha << 24) | (col & 0xFFFFFF));
            }
        }
    }

    public static void draw(GuiGraphics g, int index, int x, int y, int size)
    {
        draw(g, index, x, y, size, 255);
    }

    // ---- 基本图元（全部落在 16×16 逻辑网格） ----

    private static void set(int[] g, int c, int r, int color)
    {
        if (c >= 0 && c < 16 && r >= 0 && r < 16) g[r * 16 + c] = color;
    }

    private static void rect(int[] g, int x, int y, int w, int h, int color)
    {
        for (int r = y; r < y + h; r++) for (int c = x; c < x + w; c++) set(g, c, r, color);
    }

    /** 圆脸：中心 (7.5,7.5) 半径 6.3。 */
    private static void face(int[] g, int color)
    {
        for (int r = 0; r < 16; r++)
        {
            for (int c = 0; c < 16; c++)
            {
                double dx = c - 7.5, dy = r - 7.5;
                if (dx * dx + dy * dy <= 6.3 * 6.3) g[r * 16 + c] = color;
            }
        }
    }

    private static void eyes(int[] g)
    {
        rect(g, 4, 5, 2, 2, K);
        rect(g, 10, 5, 2, 2, K);
    }

    private static void blush(int[] g)
    {
        rect(g, 3, 9, 2, 2, P);
        rect(g, 11, 9, 2, 2, P);
    }

    /** 微笑嘴：两端上扬的小弧。 */
    private static void smileArc(int[] g)
    {
        set(g, 4, 8, K);
        set(g, 11, 8, K);
        for (int x = 5; x <= 10; x++) set(g, x, 9, K);
    }

    /** 大笑嘴：深色唇 + 白色口腔 + 红色舌头。 */
    private static void openMouth(int[] g)
    {
        for (int x = 5; x <= 10; x++)
        {
            set(g, x, 8, K);
            set(g, x, 11, K);
        }
        for (int r = 9; r <= 10; r++) for (int x = 5; x <= 10; x++) set(g, x, r, W);
        for (int x = 5; x <= 10; x++) set(g, x, 10, R);
    }

    /** 大哭嘴：深色唇 + 白色大张口腔（无舌头，更"啊——"）。 */
    private static void wailMouth(int[] g)
    {
        for (int x = 5; x <= 10; x++)
        {
            set(g, x, 8, K);
            set(g, x, 11, K);
        }
        for (int r = 9; r <= 10; r++) for (int x = 5; x <= 10; x++) set(g, x, r, W);
    }

    /** 小号 3×3 红心（爱心眼用），cx/cy 为中心坐标。 */
    private static void smallHeart(int[] g, int cx, int cy)
    {
        set(g, cx - 1, cy - 1, R);
        set(g, cx + 1, cy - 1, R);
        set(g, cx - 1, cy, R);
        set(g, cx, cy, R);
        set(g, cx + 1, cy, R);
        set(g, cx, cy + 1, R);
    }

    // ---- 8 个表情 ----

    private static void buildSmile(int[] g)
    {
        face(g, Y);
        eyes(g);
        smileArc(g);
        blush(g);
    }

    private static void buildLol(int[] g)
    {
        face(g, Y);
        eyes(g);
        openMouth(g);
        set(g, 3, 7, B); // 左眼笑出一滴泪
        set(g, 3, 8, B);
    }

    private static void buildCry(int[] g)
    {
        face(g, Y);
        eyes(g);
        wailMouth(g);
        // 两眼两行泪
        set(g, 4, 7, B); set(g, 4, 8, B);
        set(g, 5, 7, B); set(g, 5, 8, B);
        set(g, 10, 7, B); set(g, 10, 8, B);
        set(g, 11, 7, B); set(g, 11, 8, B);
    }

    private static void buildAngry(int[] g)
    {
        face(g, R);
        rect(g, 4, 6, 2, 2, K);   // 眼睛下压
        rect(g, 10, 6, 2, 2, K);
        // 怒眉：外高内低
        set(g, 3, 4, K); set(g, 4, 4, K); set(g, 5, 5, K);
        set(g, 10, 5, K); set(g, 11, 4, K); set(g, 12, 4, K);
        // 撇嘴（∩ 形）：嘴角下垂
        set(g, 4, 11, K); set(g, 11, 11, K);
        set(g, 5, 10, K); set(g, 10, 10, K);
        for (int x = 6; x <= 9; x++) set(g, x, 10, K);
    }

    private static void buildHeartEyes(int[] g)
    {
        face(g, Y);
        smallHeart(g, 4, 5);
        smallHeart(g, 10, 5);
        smileArc(g);
        blush(g);
    }

    private static void buildSurprised(int[] g)
    {
        face(g, Y);
        rect(g, 4, 5, 3, 3, K);   // 大圆眼
        rect(g, 10, 5, 3, 3, K);
        for (int x = 5; x <= 10; x++)
        {
            set(g, x, 8, K);
            set(g, x, 13, K);
        }
        for (int r = 9; r <= 12; r++) for (int x = 5; x <= 10; x++) set(g, x, r, W);
    }

    private static void buildSleepy(int[] g)
    {
        face(g, Y);
        set(g, 4, 6, K); set(g, 5, 6, K);  // 闭眼横线
        set(g, 10, 6, K); set(g, 11, 6, K);
        smileArc(g);
        // 右上角 Z
        set(g, 11, 1, K); set(g, 12, 1, K); set(g, 13, 1, K);
        set(g, 12, 2, K);
        set(g, 11, 3, K); set(g, 12, 3, K); set(g, 13, 3, K);
    }

    private static void buildHeart(int[] g)
    {
        for (int x = 5; x <= 6; x++) set(g, x, 4, R);  // 顶部双丘
        for (int x = 9; x <= 10; x++) set(g, x, 4, R);
        for (int r = 5; r <= 7; r++) for (int x = 4; x <= 11; x++) set(g, x, r, R);
        for (int x = 5; x <= 10; x++) set(g, x, 8, R);
        for (int x = 6; x <= 9; x++) set(g, x, 9, R);
        for (int x = 7; x <= 8; x++) set(g, x, 10, R);
        set(g, 8, 11, R);
    }

    private DuelEmoji() {}
}
