# WebSocket 连接问题 - 快速操作指南

## ✅ 问题已修复

WebSocket 连接失败的问题已经修复。引擎现在会正确连接到 Java 后端的 3822 端口。

## 📋 修复内容

已修改以下文件，让引擎直接连接到 Java 后端：
- ✅ `map_convert_services/simulation_service.py`
- ✅ `map_convert_services/web_app.py`

## 🚀 立即开始

### 步骤 1: 验证配置（推荐）

```bash
cd map_convert_services
python check_config.py
```

**预期输出**：
```
[OK] 所有检查通过，可以启动服务
引擎将连接到: ws://localhost:3822/ws/exe/{sessionId}
```

### 步骤 2: 启动服务

**Windows 用户**：
```bash
cd map_convert_services
start.bat
```

**Linux/Mac 用户**：
```bash
cd map_convert_services
bash start.sh
```

### 步骤 3: 验证连接

启动服务后，检查日志：

**Python 日志** - 应该看到：
```
Simulation command: ... --ip=localhost --port=3822 --ws=ws://localhost:3822/ws/exe/xxx ...
WebSocket URL: ws://localhost:3822/ws/exe/xxx
```

**Java 日志** - 应该看到：
```
Engine WebSocket connected: {sessionId}
Engine initialized for session: {sessionId}
```

## ✨ 预期效果

修复后：
- ✅ 引擎成功连接到 Java 后端
- ✅ 仿真数据实时传输
- ✅ Java 和引擎终端都能输出实时仿真信息
- ✅ 前端显示实时仿真数据

## 🔧 架构说明

```
前端 ←→ Java 后端 (WebSocket 3822) ←→ 引擎
         ↓ (gRPC 50051)
    Python 服务
         ↓
    启动引擎（配置连接到 Java 3822）
```

**关键点**：
- Java 后端是 WebSocket 中心（端口 3822）
- Python 服务通过 gRPC 接收创建引擎请求（端口 50051）
- 引擎直接连接 Java 后端，不再通过 Python 中介

## 📊 端口说明

| 端口 | 服务 | 说明 |
|------|------|------|
| **3822** | Java 后端 | **引擎和前端的 WebSocket 连接** ⭐ |
| 8000 | Python FastAPI | HTTP 服务（保留兼容） |
| 50051 | Python gRPC | 仿真服务（创建引擎） |
| 50052 | Python gRPC | 地图转换服务 |

## ❓ 常见问题

### Q: 引擎仍然连接失败？
**A**: 
1. 确认 Java 后端已启动（端口 3822）
2. 运行 `python check_config.py` 验证配置
3. 检查防火墙是否阻止 3822 端口
4. 查看 Python 日志确认引擎启动命令包含 `--port=3822`

### Q: 如何确认修复成功？
**A**: 
1. Python 日志显示：`--port=3822 --ws=ws://localhost:3822/ws/exe/...`
2. Java 日志显示：`Engine WebSocket connected: {sessionId}`
3. 启动仿真后，两端终端都有实时输出

### Q: 需要修改配置吗？
**A**: 
不需要。默认配置已经正确（Java 后端 3822 端口）。
如果需要自定义，可以设置环境变量：
```bash
BACKEND_HOST=localhost
BACKEND_PORT=3822
```

## 📚 详细文档

- **本文档**: 快速操作指南
- **详细修复报告**: `map_convert_services/WEBSOCKET_FIX.md`
- **配置说明**: `map_convert_services/CONFIG.md`
- **修复总结**: `WEBSOCKET_FIX_SUMMARY.md`

## 🎯 下一步

1. ✅ 运行 `python check_config.py` 验证配置
2. ✅ 启动 Python 服务（`start.bat` 或 `bash start.sh`）
3. ✅ 启动 Java 后端（如果还未启动）
4. ✅ 创建仿真任务测试连接
5. ✅ 验证实时数据传输

---

**修复完成日期**: 2026-01-20  
**验证状态**: ✅ 配置验证通过  
**测试状态**: 待实际运行测试
