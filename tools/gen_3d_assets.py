# -*- coding: utf-8 -*-
"""
来古牌「端详 3D 卡牌」贴图生成器
================================
为端详时的立体牌框模型生成三张 32x32 贴图（纯 Python 手写 PNG，无需 Pillow）：
  card_frame.png  金色牌框（不透明）——4 根包边条 + 卡体侧边
  card_back.png   卡背（不透明深红 + 金色菱形纹）
  card_glass.png  玻璃罩（透明 + 斜向白色反光带，制造「玻璃包裹」感）

仅本特性需要；卡面原图仍来自 卡牌输出 目录，本脚本不触碰。
运行：py tools/gen_3d_assets.py
"""
import os
import struct
import zlib

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                   'assets', 'laigu', 'textures', 'item')

SIZE = 32


# ---------------------------------------------------------------------------
# PNG 编码（RGBA8，无压缩滤镜）
# ---------------------------------------------------------------------------
def write_png(path, size, pixel):
    """pixel(x, y) -> (r, g, b, a)。"""
    raw = bytearray()
    for y in range(size):
        raw.append(0)  # filter: none
        for x in range(size):
            r, g, b, a = pixel(x, y)
            raw += bytes((max(0, min(255, r)), max(0, min(255, g)),
                          max(0, min(255, b)), max(0, min(255, a))))
    ihdr = struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)  # RGBA
    chunk = lambda typ, data: (struct.pack('>I', len(data)) + typ + data
                               + struct.pack('>I', zlib.crc32(typ + data) & 0xffffffff))
    png = (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr)
           + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b''))
    with open(path, 'wb') as f:
        f.write(png)
    print('  ->', os.path.relpath(path))


# ---------------------------------------------------------------------------
# card_frame.png：金色牌框（边缘深色描边 + 亮色高光 + 拉丝质感）
# ---------------------------------------------------------------------------
def gen_frame():
    def px(x, y):
        d = min(x, y, SIZE - 1 - x, SIZE - 1 - y)
        if d == 0:
            return 48, 32, 16, 255
        if d == 1:
            return 60, 42, 22, 255
        if d == 2:
            return 246, 214, 140, 255       # 外缘高光
        if d == 3:
            return 233, 197, 112, 255       # 亮金
        # 主体：竖向上亮下暗 + 对角拉丝
        t = y / (SIZE - 1)
        base = (208 - int(34 * t), 168 - int(32 * t), 84 - int(22 * t))
        if (x + y) % 4 == 0:                # 拉丝斜纹
            base = tuple(min(255, c + 16) for c in base)
        if (x + y) % 8 == 3:
            base = tuple(max(0, c - 12) for c in base)
        if d == 8:                          # 内圈暗纹（增强立体）
            base = tuple(int(c * 0.72) for c in base)
        return base[0], base[1], base[2], 255
    write_png(os.path.join(OUT, 'card_frame.png'), SIZE, px)


# ---------------------------------------------------------------------------
# card_back.png：卡背（深红底 + 金边 + 中央金色菱形）
# ---------------------------------------------------------------------------
def gen_back():
    def px(x, y):
        d = min(x, y, SIZE - 1 - x, SIZE - 1 - y)
        if d == 0:
            return 26, 10, 12, 255
        if d <= 2:
            return 198, 158, 66, 255        # 金边
        if d == 3:
            return 244, 214, 132, 255       # 边高光
        # 深红渐变（中心略暗）
        cx = cy = SIZE // 2
        r = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 / (SIZE * 0.72)
        base = (132 - int(42 * r), 24 - int(8 * r), 34 - int(10 * r))
        # 中央菱形纹
        md = abs(x - cx) + abs(y - cy)
        if 6 <= md <= 8:
            return 224, 182, 92, 255        # 外菱形
        if md <= 3:
            return 238, 202, 122, 255       # 内菱形
        if md == 4 or md == 5:
            return int(base[0] * 0.8), int(base[1] * 0.8), int(base[2] * 0.8), 255
        return base[0], base[1], base[2], 255
    write_png(os.path.join(OUT, 'card_back.png'), SIZE, px)


# ---------------------------------------------------------------------------
# card_glass.png：玻璃罩（透明 + 斜向白色反光带）
# ---------------------------------------------------------------------------
def gen_glass():
    def px(x, y):
        # 主反光带沿对角线 y = x - 5 方向，d 为到该直线的有向距离
        d = (x - y + 5.0) / 2 ** 0.5
        ad = abs(d)
        if ad > 7:
            return 0, 0, 0, 0
        # 柔和渐变主带
        a = max(0.0, 1.0 - ad / 7.0)
        # 中心一条更亮的细线
        if ad <= 1.2:
            a = max(a, 1.0 - ad / 1.2)
        a *= 0.32
        if a <= 0.0:
            return 0, 0, 0, 0
        return int(236 * a), int(242 * a), int(255 * a), int(a * 255)
    write_png(os.path.join(OUT, 'card_glass.png'), SIZE, px)


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    gen_frame()
    gen_back()
    gen_glass()
    print('done')
