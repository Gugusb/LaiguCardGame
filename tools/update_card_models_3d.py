# -*- coding: utf-8 -*-
"""
一次性：为现有卡牌物品模型挂上「端详 3D」override，并生成 <id>_<rarity>_3d.json。
已处理过的模型（已有 overrides）会跳过，可重复运行。
用法：py tools/update_card_models_3d.py
"""
import io
import json
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
MODEL_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'laigu', 'models', 'item')
CATALOG = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'laigu', 'laigu', 'card', 'CardCatalog.java')


def card_ids():
    src = io.open(CATALOG, encoding='utf-8').read()
    m = re.search(r'List\.of\((.*?)\);', src, re.S)
    if not m:
        raise SystemExit('无法从 CardCatalog.java 解析 CARD_IDS')
    return re.findall(r'"([a-z0-9_]+)"', m.group(1))


def main():
    ids = card_ids()
    rarities = ("common", "gold")
    done = skipped = 0
    for cid in ids:
        for r in rarities:
            item_id = cid + "_" + r
            flat = os.path.join(MODEL_DIR, item_id + ".json")
            if not os.path.exists(flat):
                print('  [缺失]', item_id)
                continue
            model = json.load(io.open(flat, encoding='utf-8'))
            if 'overrides' in model:
                skipped += 1
                continue
            model['overrides'] = [{
                'predicate': {'laigu:inspecting': 1},
                'model': 'laigu:item/' + item_id + '_3d',
            }]
            with io.open(flat, 'w', encoding='utf-8') as f:
                json.dump(model, f, ensure_ascii=False, indent=2)
            model3d = {
                'parent': 'laigu:item/card_3d',
                'textures': {'layer0': 'laigu:item/' + item_id},
            }
            with io.open(os.path.join(MODEL_DIR, item_id + '_3d.json'), 'w', encoding='utf-8') as f:
                json.dump(model3d, f, ensure_ascii=False, indent=2)
            done += 1
    print('更新 %d 张卡牌模型，跳过 %d（已处理）。' % (done, skipped))


if __name__ == '__main__':
    main()
