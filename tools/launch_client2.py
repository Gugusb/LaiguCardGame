"""用主机的完整 java 启动命令派生第二个客户端（联机测试子机）。

- 从 hostclient.log 里抓取 gradle runClient 的完整 java 命令行
- 去掉 -cp 里的 build/classes 与 build/resources（dev mod 改从 mods 文件夹加载，避免重复）
- cwd 设为 run-client2（独立游戏目录），追加 --username PlayerB
用法: python launch_client2.py <hostlog_windows_path> <laigu_root_windows_path>
"""
import sys
import subprocess
import os

logpath = sys.argv[1]
root = sys.argv[2]

cmdline = None
with open(logpath, 'rb') as f:
    for raw in f:
        s = raw.decode('gbk', errors='replace')
        if 'Command: ' in s:
            cmdline = s
if not cmdline:
    raise SystemExit('no Command line found in ' + logpath)

cmd = cmdline.split('Command: ', 1)[1].strip()
args = cmd.split(' ')
dropped = []
for i, a in enumerate(args):
    if a == '-cp':
        keep = []
        for p in args[i + 1].split(';'):
            low = p.replace('\\', '/').lower()
            if 'build/classes/java/main' in low or 'build/resources/main' in low:
                dropped.append(p)
            else:
                keep.append(p)
        args[i + 1] = ';'.join(keep)
args += ['--username', 'PlayerB']

run2 = os.path.join(root, 'run-client2')
os.makedirs(run2, exist_ok=True)
logf = open(os.path.join(root, 'client2.log'), 'wb', 0)
p = subprocess.Popen(args, cwd=run2, stdout=logf, stderr=subprocess.STDOUT)
print('LAUNCHED pid=%s cwd=%s' % (p.pid, run2))
print('dropped from -cp: %d' % len(dropped))
for d in dropped:
    print('  -', d)
print('-cp entries now: %d' % len(args[args.index('-cp') + 1].split(';')))
