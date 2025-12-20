import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { ShieldCheck, Lock } from 'lucide-react';
import { toast } from 'sonner';
import { api } from '@/lib/api';

const AdminLogin = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      const res = await api.adminLogin(username, password);
      if (res.role !== 'ADMIN') {
        throw new Error('not admin');
      }
      localStorage.setItem('is_admin_logged_in', 'true');
      localStorage.setItem('sam_token', res.token);
      toast.success('管理员登录成功');
      navigate('/admin/dashboard');
    } catch (err) {
      toast.error('账号或密码错误');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 p-4">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,_var(--tw-gradient-stops))] from-slate-900 via-slate-950 to-slate-950"></div>
      
      <Card className="w-full max-w-md relative z-10 bg-slate-900/80 border-white/10 backdrop-blur-md">
        <CardHeader className="text-center">
          <div className="mx-auto w-12 h-12 bg-teal-500/10 rounded-full flex items-center justify-center mb-4 border border-teal-500/20">
            <ShieldCheck className="w-6 h-6 text-teal-400" />
          </div>
          <CardTitle className="text-white text-xl">S.A.M. 系统管理终端</CardTitle>
          <CardDescription className="text-slate-400">请输入管理员凭证以访问核心数据库</CardDescription>
        </CardHeader>
        <form onSubmit={handleLogin}>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="username" className="text-slate-200">管理员账号</Label>
              <Input 
                id="username" 
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="bg-black/20 border-white/10 text-white"
                placeholder="admin"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password" className="text-slate-200">密码</Label>
              <Input 
                id="password" 
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="bg-black/20 border-white/10 text-white"
                placeholder="••••••"
              />
            </div>
          </CardContent>
          <CardFooter>
            <Button 
              type="submit" 
              className="w-full bg-teal-600 hover:bg-teal-700 text-white"
              disabled={isLoading}
            >
              {isLoading ? '正在验证权限...' : '进入系统'}
              {!isLoading && <Lock className="ml-2 w-4 h-4" />}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
};

export default AdminLogin;
