"""主机运行时绕开 gradle 锁直接重建：拷贝语言文件 + javac 增量编译 + 重打子机 dev jar。

- 语言文件: src/main/resources -> build/resources/main（processResources 等价，仅拷贝）
- 编译:    javac 用主机启动命令的完整 -cp，增量编译指定的几个改动类
- 打 jar:  build/classes + build/resources -> run-client2/mods/laigu-dev.jar
用法: python rebuild_dev.py <hostlog_windows_path> <laigu_root_windows_path>
"""
import sys
import os
import shutil
import subprocess

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
cp = None
for i, a in enumerate(args):
    if a == '-cp':
        cp = args[i + 1]
if not cp:
    raise SystemExit('no -cp in command')

classes = os.path.join(root, 'build', 'classes', 'java', 'main')
res = os.path.join(root, 'build', 'resources', 'main')
lang_src = os.path.join(root, 'src', 'main', 'resources', 'assets', 'laigu', 'lang')
lang_dst = os.path.join(res, 'assets', 'laigu', 'lang')
javac = r'D:\Env\Java\ms-17.0.16\bin\javac.exe'
jar = r'D:\Env\Java\ms-17.0.16\bin\jar.exe'

# 1. copy updated lang files
for f in ('zh_cn.json', 'en_us.json'):
    shutil.copyfile(os.path.join(lang_src, f), os.path.join(lang_dst, f))
    print('copied lang:', f)

# 2. incremental javac
files = [
    os.path.join(root, 'src', 'main', 'java', 'com', 'laigu', 'laigu', 'block', 'CardExchangeTableBlockEntity.java'),
    os.path.join(root, 'src', 'main', 'java', 'com', 'laigu', 'laigu', 'container', 'CardExchangeMenu.java'),
    os.path.join(root, 'src', 'main', 'java', 'com', 'laigu', 'laigu', 'client', 'CardExchangeScreen.java'),
]
r = subprocess.run([javac, '-cp', cp, '-encoding', 'UTF-8', '-d', classes] + files,
                   capture_output=True, text=True)
print('javac rc =', r.returncode)
if r.stdout:
    print('-- stdout --\n' + r.stdout)
if r.stderr:
    print('-- stderr --\n' + r.stderr)
if r.returncode != 0:
    raise SystemExit('javac failed')

# 3. repack dev jar for client2
jar_path = os.path.join(root, 'run-client2', 'mods', 'laigu-dev.jar')
r = subprocess.run([jar, 'cf', jar_path, '-C', classes, '.', '-C', res, '.'],
                   cwd=root, capture_output=True, text=True)
print('jar rc =', r.returncode)
if r.stderr:
    print('-- jar stderr --\n' + r.stderr)
print('jar:', jar_path, os.path.getsize(jar_path), 'bytes')
