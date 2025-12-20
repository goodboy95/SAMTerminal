# UploadController 接口

## POST /api/upload/image
- 描述：上传图片资源（用于背景/头像/立绘等）。
- 请求：`multipart/form-data`，字段名 `file`。
- 响应：`{ "url": "/uploads/xxx.png" }`
