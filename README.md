# PAI - AI 聊天应用

PAI 是一个基于 Android Jetpack Compose 开发的 AI 聊天应用，支持多种功能，包括消息编辑、Markdown 渲染、聊天管理等。

## 功能特点

- **模型配置管理**：支持添加、编辑、删除模型配置，支持测试连接
- **聊天管理**：支持创建、删除、重命名聊天会话
- **消息功能**：
  - 支持消息编辑
  - 支持消息删除
  - 支持消息重新发送
  - 支持 Markdown 格式显示
  - 支持附件发送
- **AI 状态显示**：显示 AI 处理状态，提供清晰的反馈
- **UI 优化**：紧凑的界面设计，最大化聊天显示空间
- **数据持久化**：使用 SharedPreferences 存储模型配置和聊天数据

## 技术栈

- **前端**：Android Jetpack Compose
- **后端**：通过 API 与 AI 模型通信
- **数据存储**：SharedPreferences
- **网络请求**：OkHttp
- **JSON 解析**：Gson
- **Markdown 渲染**：compose-markdown

## 安装与使用

1. 克隆项目到本地：
   ```bash
   git clone <仓库地址>
   ```

2. 在 Android Studio 中打开项目

3. 构建并运行应用

4. 首次使用时，需要配置模型信息：
   - 点击设置图标
   - 添加模型配置
   - 测试连接确保配置正确

5. 开始聊天：
   - 直接在聊天框中输入消息
   - 点击发送按钮
   - 查看 AI 回复

## 项目结构

```
app/
├── src/
│   ├── main/
│   │   ├── java/top/nones/pai/
│   │   │   ├── data/            # 数据模型和存储
│   │   │   ├── ui/              # 界面组件
│   │   │   ├── viewmodel/       # 视图模型
│   │   │   ├── MainActivity.kt  # 主活动
│   │   │   └── App.kt           # 应用入口
│   │   └── res/                 # 资源文件
└── build.gradle.kts             # 应用构建配置

compose-markdown/                # Markdown 渲染库

settings.gradle.kts              # 项目设置
```

## 注意事项

- 确保网络连接正常，特别是配置本地模型时
- 模型配置信息会被保存到本地，无需重复配置
- 聊天数据会被保存到本地，可在聊天管理中查看和删除

## 许可证

MIT
