import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { ArrowRight, Cpu } from 'lucide-react';
import { toast } from 'sonner';
import { api, API_BASE } from '@/lib/api';
import AltchaWidget from '@/components/AltchaWidget';

const Index = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [regUsername, setRegUsername] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [emailCode, setEmailCode] = useState('');
  const [emailRequestId, setEmailRequestId] = useState<string | null>(null);
  const [emailVerified, setEmailVerified] = useState(false);
  const [resendAvailableAt, setResendAvailableAt] = useState<Date | null>(null);
  const [resendSecondsLeft, setResendSecondsLeft] = useState(0);
  const [sendStatus, setSendStatus] = useState<'IDLE' | 'PENDING' | 'SENT' | 'FAILED'>('IDLE');
  const [sendLoading, setSendLoading] = useState(false);
  const [verifyLoading, setVerifyLoading] = useState(false);
  const [showAltcha, setShowAltcha] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const altchaTestMode = import.meta.env.VITE_ALTCHA_TEST_MODE === 'true';

  useEffect(() => {
    setEmailRequestId(null);
    setEmailCode('');
    setEmailVerified(false);
    setSendStatus('IDLE');
    setResendAvailableAt(null);
  }, [regEmail, regUsername]);

  useEffect(() => {
    if (!resendAvailableAt) {
      setResendSecondsLeft(0);
      return;
    }
    const tick = () => {
      const diff = Math.max(0, Math.ceil((resendAvailableAt.getTime() - Date.now()) / 1000));
      setResendSecondsLeft(diff);
    };
    tick();
    const timer = setInterval(tick, 1000);
    return () => clearInterval(timer);
  }, [resendAvailableAt]);

  useEffect(() => {
    if (!emailRequestId || sendStatus !== 'PENDING') return;
    let cancelled = false;
    const poll = async () => {
      try {
        const res = await api.emailSendStatus(emailRequestId);
        if (cancelled) return;
        if (res.status && res.status !== sendStatus) {
          setSendStatus(res.status);
          if (res.status === 'SENT') toast.success('验证码已发送');
          if (res.status === 'FAILED') toast.error(res.lastError || '验证码发送失败');
        }
      } catch (err) {
        if (!cancelled) {
          setSendStatus('FAILED');
        }
      }
    };
    const interval = setInterval(poll, 3000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [emailRequestId, sendStatus]);

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
    if (!emailVerified || !emailRequestId) {
      toast.error('请先完成邮箱验证码校验');
      return;
    }
    setIsLoading(true);
    try {
      const res = await api.registerWithEmailCode(regUsername, regEmail, regPassword, emailCode, emailRequestId);
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

  const handleSendEmailCode = async (payload: string) => {
    if (!regUsername || !regEmail) {
      toast.error('请先填写用户名与邮箱');
      return;
    }
    setSendLoading(true);
    try {
      const res = await api.sendRegisterEmailCode(regUsername, regEmail, payload);
      setEmailRequestId(res.requestId);
      setResendAvailableAt(res.resendAvailableAt ? new Date(res.resendAvailableAt) : null);
      setSendStatus(res.sendStatus || 'PENDING');
      setEmailVerified(false);
      toast.success('验证码发送请求已提交');
    } catch (err: any) {
      const payloadData = err?.payload;
      if (payloadData?.resendAvailableAt) {
        setResendAvailableAt(new Date(payloadData.resendAvailableAt));
      }
      toast.error(err?.message || '发送失败');
    } finally {
      setSendLoading(false);
      setShowAltcha(false);
    }
  };

  const handleSendButton = () => {
    if (resendSecondsLeft > 0 || sendLoading) return;
    if (altchaTestMode) {
      handleSendEmailCode('test-bypass');
      return;
    }
    setShowAltcha(true);
  };

  const handleVerifyCode = async () => {
    if (!emailRequestId || !emailCode) {
      toast.error('请输入验证码');
      return;
    }
    setVerifyLoading(true);
    try {
      await api.verifyRegisterEmailCode(emailRequestId, regEmail, emailCode);
      setEmailVerified(true);
      toast.success('验证码验证通过');
    } catch (err) {
      setEmailVerified(false);
      toast.error('验证码验证失败，请检查或重新发送');
    } finally {
      setVerifyLoading(false);
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
                    <Label htmlFor="reg-email-code" className="text-slate-200">邮箱验证码</Label>
                    <div className="grid md:grid-cols-3 gap-2">
                      <Input
                        id="reg-email-code"
                        className="bg-black/20 border-white/10 text-white font-mono md:col-span-2"
                        value={emailCode}
                        onChange={(e) => setEmailCode(e.target.value)}
                        inputMode="numeric"
                        maxLength={6}
                        placeholder="输入 6 位验证码"
                        required
                      />
                      <Button
                        type="button"
                        className="bg-teal-600 hover:bg-teal-700 text-white"
                        onClick={handleSendButton}
                        disabled={sendLoading || resendSecondsLeft > 0}
                      >
                        {sendLoading ? '发送中...' : resendSecondsLeft > 0 ? `重发 ${resendSecondsLeft}s` : '发送验证邮件'}
                      </Button>
                    </div>
                    <div className="flex flex-wrap items-center gap-3 text-xs text-slate-400">
                      <Button
                        type="button"
                        variant="secondary"
                        className="bg-slate-800 text-white"
                        onClick={handleVerifyCode}
                        disabled={verifyLoading || !emailRequestId || !emailCode}
                      >
                        {verifyLoading ? '验证中...' : '验证验证码'}
                      </Button>
                      {emailVerified && <span className="text-emerald-400">验证通过</span>}
                      {!emailVerified && emailRequestId && <span>验证码已发送，5 分钟内有效</span>}
                      {sendStatus === 'FAILED' && <span className="text-rose-400">发送失败，请重试</span>}
                      {sendStatus === 'PENDING' && <span>发送中...</span>}
                    </div>
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

        <Dialog open={showAltcha} onOpenChange={setShowAltcha}>
          <DialogContent className="bg-slate-900 border-white/10 text-white">
            <DialogHeader>
              <DialogTitle className="text-white">安全验证</DialogTitle>
              <DialogDescription className="text-slate-400">
                完成 ALTCHA 验证后即可发送验证码邮件。
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4">
              {altchaTestMode ? (
                <div className="text-sm text-slate-300">
                  当前为 ALTCHA 测试模式，将直接放行发送请求。
                </div>
              ) : (
                <AltchaWidget
                  challengeUrl={`${API_BASE}/api/captcha/altcha/challenge`}
                  verifyUrl={`${API_BASE}/api/captcha/altcha/verify`}
                  onVerified={handleSendEmailCode}
                  onError={(message) => toast.error(message)}
                />
              )}
            </div>
          </DialogContent>
        </Dialog>

        <p className="text-xs text-center text-slate-600 font-mono">
          Glamoth Iron Cavalry Protocol // v1.1.0
        </p>
      </div>
    </div>
  );
};

export default Index;
