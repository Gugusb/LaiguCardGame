# -*- coding: utf-8 -*-
"""
生成来古牌容器 GUI 贴图（176×166，与各菜单槽位坐标精确对齐）
============================================================
  card_pouch.png    卡袋菜单（CardPouchMenu.java）：
                      卡袋 3×2 : x = 62 + col*18, y = 18 + row*18
                      背包 3×9 : x = 8 + col*18,  y = 84 + row*18
                      快捷栏 9 : x = 8 + col*18,  y = 142
  card_exchange.png 卡牌交换台（CardExchangeMenu.java）：
                      卡牌槽 A/B : (62,26) / (96,26)（中间为交换指示）
                      背包 3×9 : x = 8 + col*18,  y = 84 + row*18
                      快捷栏 9 : x = 8 + col*18,  y = 142
槽位可见格为 16×16，居中于 18×18 网格，左上角 = 槽位(x+1, y+1)。

用法：python tools/gen_pouch_gui.py
"""
import io
import os
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
GUI_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'laigu', 'textures', 'gui')

W, H = 176, 166

# 配色（暗色面板 + 内凹槽格，观感接近原版容器）
PANEL = (24, 22, 26, 255)
PANEL_EDGE = (46, 42, 50, 255)   # 面板描边
SLOT_BG = (12, 11, 14, 255)      # 槽格内凹底
SLOT_EDGE = (52, 48, 58, 255)    # 槽格描边
ACCENT = (196, 148, 44, 255)     # 强调色（交换指示）

# ---- 通用槽格辅助 ----

def draw_slot(d, sx, sy):
    """在 18×18 网格(x,y)处画 16×16 内凹槽格。"""
    d.rectangle([sx + 1, sy + 1, sx + 16, sy + 16], fill=SLOT_BG, outline=SLOT_EDGE, width=1)


def player_inventory_rects():
    rects = []
    for row in range(3):
        for col in range(9):
            rects.append((8 + col * 18, 84 + row * 18))
    for col in range(9):
        rects.append((8 + col * 18, 142))
    return rects


def draw_panel(img, extra):
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W - 1, H - 1], outline=PANEL_EDGE, width=1)
    for sx, sy in player_inventory_rects():
        draw_slot(d, sx, sy)
    extra(d)


def save(img, name):
    out = os.path.join(GUI_DIR, name)
    os.makedirs(GUI_DIR, exist_ok=True)
    with io.open(out, 'wb') as f:
        img.save(f, 'PNG')
    print('saved', out, img.size)


# ---- 卡袋 ----

def gen_pouch():
    def extra(d):
        for row in range(2):
            for col in range(3):
                draw_slot(d, 62 + col * 18, 18 + row * 18)
    img = Image.new('RGBA', (W, H), PANEL)
    draw_panel(img, extra)
    save(img, 'card_pouch.png')


# ---- 卡牌交换台 ----

def gen_exchange():
    def extra(d):
        # A/B 两侧卡牌槽
        draw_slot(d, 62, 26)
        draw_slot(d, 96, 26)
        # 中间交换指示：两个相对箭头（⇄）
        d.polygon([(80, 26), (80, 42), (87, 34)], fill=ACCENT)   # →
        d.polygon([(95, 26), (95, 42), (88, 34)], fill=ACCENT)   # ←
    img = Image.new('RGBA', (W, H), PANEL)
    draw_panel(img, extra)
    save(img, 'card_exchange.png')


def main():
    gen_pouch()
    gen_exchange()


if __name__ == '__main__':
    main()
