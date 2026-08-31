"""从运行中的主机 MC 进程派生第二个客户端（联机测试子机）。

不再依赖 hostclient.log 里的 Command: 行（gradle 有时不打），直接抓 java.exe
进程命令行：
- 去掉 -cp 里的 build/classes 与 build/resources（dev mod 改从 mods 文件夹加载，避免重复）
- 保持 --gameDir . ，cwd 设为 run-client2（独立游戏目录，options/config/logs 不跟主机抢）
- 追加 --username PlayerB
用法: python relaunch_client2.py <laigu_root_windows_path>
"""
import subprocess
import os
import sys

root = sys.argv[1]

wmic_out = subprocess.run(
    ['wmic', 'process', 'where', "name='java.exe'", 'get', 'processid,commandline', '/format:list'],
    capture_output=True)
data = wmic_out.stdout.decode('gbk', errors='replace')
cmdline = None
for block in data.split('\r\r\n'):
    if block.startswith('CommandLine=') and 'forge.enableGameTest' in block:
        cmdline = block[len('CommandLine='):]
        break
if not cmdline:
    raise SystemExit('host MC client process not found (is the host running?)')

# 解析参数（尊重双引号）
args = []
i = 0
while i < len(cmdline):
    while i < len(cmdline) and cmdline[i].isspace():
        i += 1
    if i >= len(cmdline):
        break
    if cmdline[i] == '"':
        j = i + 1
        buf = []
        while j < len(cmdline):
            if cmdline[j] == '"':
                j += 1
                break
            buf.append(cmdline[j])
            j += 1
        args.append(''.join(buf))
        i = j
    else:
        j = i
        while j < len(cmdline) and not cmdline[j].isspace():
            j += 1
        args.append(cmdline[i:j])
        i = j

# 剔除 -cp 里的 build 路径（dev mod 改从 mods 文件夹加载）
dropped = []
for n, a in enumerate(args):
    if a == '-cp':
        keep = []
        for p in args[n + 1].split(';'):
            low = p.replace('\\', '/').lower()
            if 'build/classes/java/main' in low or 'build/resources/main' in low:
                dropped.append(p)
            else:
                keep.append(p)
        args[n + 1] = ';'.join(keep)
        break

# cwd=run-client2 时 --gameDir . 即指向 run-client2
args += ['--username', 'PlayerB']

run2 = os.path.join(root, 'run-client2')
os.makedirs(run2, exist_ok=True)
logf = open(os.path.join(root, 'client2.log'), 'wb', 0)
p = subprocess.Popen(args, cwd=run2, stdout=logf, stderr=subprocess.STDOUT)
print('LAUNCHED pid=%s cwd=%s' % (p.pid, run2))
print('dropped -cp entries: %d' % len(dropped))
for d in dropped:
    print('  -', d)
print('-cp entries kept: %d' % len(args[args.index('-cp') + 1].split(';')))
