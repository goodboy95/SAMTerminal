# AuthController 接口

## POST /api/auth/login
- 描述：用户登录，返回 JWT。
- 请求体：`{ "username": string, "password": string }`
- 响应：`{ token: string, username: string, role: string }`
- 备注：目前为演示逻辑，密码校验简单；返回的 role 依据用户表。

## POST /api/auth/register
- 描述：注册新用户，初始化游戏状态、默认背包与记忆。
- 请求体：`{ "username": string, "email": string, "password": string, "emailCode": string, "emailRequestId": string }`
- 响应：同登录。
- 备注：必须先完成邮箱验证码校验。
