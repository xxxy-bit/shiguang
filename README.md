# 拾光

记录有趣和有启发的句子，在手机桌面随机展示。

## 项目结构

```
shiguang/
├── server/          # FastAPI 后端
│   ├── main.py      # API 服务（句子 + 日程）
│   ├── database.py  # SQLite 数据层
│   └── models.py    # 数据模型
├── android/         # Android App
│   └── app/src/     # Kotlin 源码
└── .gitignore
```

## 功能

- App 内输入保存句子
- Android 桌面小组件随机展示
- 句子和日程管理
- 小组件样式自定义

## 部署

```bash
# 服务端
cd server
pip install -r requirements.txt
python main.py

# Android
# 用 Android Studio 打开 android/ 目录，编译安装
```
