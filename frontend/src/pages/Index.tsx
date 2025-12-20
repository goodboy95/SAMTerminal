import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowRight, Cpu } from 'lucide-react';
import { toast } from 'sonner';
import { api } from '@/lib/api';

const Index = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [regUsername, setRegUsername] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) return;
    setIsLoading(true);
    try {
      const res = await api.login(username, password);
      localStorage.setItem('sam_token', res.token);
      localStorage.setItem('firefly_user', res.username);
      navigate('/game');
    } catch (err) {
      toast.error('登录失败，请检查凭证');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!regUsername || !regEmail || !regPassword) return;
    setIsLoading(true);
    try {
      const res = await api.register(regUsername, regEmail, regPassword);
      localStorage.setItem('sam_token', res.token);
      localStorage.setItem('firefly_user', res.username);
      toast.success('注册成功，已自动登录');
      navigate('/game');
    } catch (err) {
      toast.error('注册失败，请重试');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-950 text-white relative overflow-hidden p-4">
      
      {/* 装饰背景 */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-teal-900/20 via-slate-950 to-slate-950"></div>
      
      <div className="relative z-10 w-full max-w-md space-y-8">
        
        {/* Logo 区域 */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-teal-500/10 border border-teal-500/20 mb-4 animate-pulse">
            <Cpu className="w-8 h-8 text-teal-400" />
          </div>
          <h1 className="text-3xl font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-teal-200 to-teal-500 font-mono">
            S.A.M. Terminal
          </h1>
          <p className="text-slate-400 text-xs tracking-[0.2em] uppercase">
            Strategic Assault Mech · System Online
          </p>
        </div>

        {/* 登录/注册卡片 */}
        <Tabs defaultValue="login" className="w-full">
          <TabsList className="grid w-full grid-cols-2 bg-slate-900/50 border border-white/10">
            <TabsTrigger value="login">接入系统</TabsTrigger>
            <TabsTrigger value="register">注册识别码</TabsTrigger>
          </TabsList>
          
          <TabsContent value="login">
            <Card className="bg-slate-900/50 border-white/10 backdrop-blur-sm">
              <CardHeader>
                <CardTitle className="text-white">身份验证</CardTitle>
                <CardDescription className="text-slate-400">请输入您的代号以连接 S.A.M. 神经中枢。</CardDescription>
              </CardHeader>
              <form onSubmit={handleLogin}>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="username" className="text-slate-200">代号 / 用户名</Label>
                  <Input 
                    id="username" 
                    placeholder="Caelus / Stelle" 
                    className="bg-black/20 border-white/10 text-white font-mono"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="password" className="text-slate-200">访问密钥</Label>
                    <Input 
                      id="password" 
                      type="password" 
                      className="bg-black/20 border-white/10 text-white font-mono"
                      placeholder="••••••••"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                    />
                  </div>
                </CardContent>
                <CardFooter>
                  <Button 
                    type="submit" 
                    className="w-full bg-teal-600 hover:bg-teal-700 text-white"
                    disabled={isLoading}
                  >
                    {isLoading ? '正在建立神经连接...' : '初始化连接'}
                    {!isLoading && <ArrowRight className="ml-2 w-4 h-4" />}
                  </Button>
                </CardFooter>
              </form>
            </Card>
          </TabsContent>
          
          <TabsContent value="register">
            <Card className="bg-slate-900/50 border-white/10 backdrop-blur-sm">
              <CardHeader>
                <CardTitle className="text-white">新用户注册</CardTitle>
                <CardDescription className="text-slate-400">创建新的驾驶员/协作者档案。</CardDescription>
              </CardHeader>
              <form onSubmit={handleRegister}>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="reg-username" className="text-slate-200">代号</Label>
                    <Input id="reg-username" className="bg-black/20 border-white/10 text-white font-mono" value={regUsername} onChange={(e) => setRegUsername(e.target.value)} required />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="reg-email" className="text-slate-200">通讯频段 (Email)</Label>
                    <Input id="reg-email" type="email" className="bg-black/20 border-white/10 text-white font-mono" value={regEmail} onChange={(e) => setRegEmail(e.target.value)} required />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="reg-password" className="text-slate-200">设置密钥</Label>
                    <Input id="reg-password" type="password" className="bg-black/20 border-white/10 text-white font-mono" value={regPassword} onChange={(e) => setRegPassword(e.target.value)} required />
                  </div>
                </CardContent>
                <CardFooter>
                  <Button type="submit" className="w-full bg-slate-700 hover:bg-slate-600 text-white" disabled={isLoading}>
                    {isLoading ? '正在注册...' : '提交档案'}
                  </Button>
                </CardFooter>
              </form>
            </Card>
          </TabsContent>
        </Tabs>

        <p className="text-xs text-center text-slate-600 font-mono">
          Glamoth Iron Cavalry Protocol // v1.1.0
        </p>
      </div>
    </div>
  );
};

export default Index;
