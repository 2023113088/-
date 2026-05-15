# Frontend

静态前端，默认通过 `http://localhost:8080` 访问 Gateway。

## 启动

```powershell
cd frontend
node server.mjs
```

浏览器打开：

```text
http://localhost:5173
```

## 功能

- 用户注册、登录、退出
- 图书浏览、搜索、借阅
- 当前借阅记录与归还
- 管理员新增、编辑、删除图书
- 显示当前命中的 `BOOK-SERVICE` 实例
