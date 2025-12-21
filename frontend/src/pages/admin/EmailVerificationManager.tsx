import { useEffect, useMemo, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { toast } from 'sonner';
import { api } from '@/lib/api';

interface SmtpConfig {
  id: number;
  name?: string;
  host: string;
  port: number;
  username?: string;
  fromAddress: string;
  useTls: boolean;
  useSsl: boolean;
  enabled: boolean;
  maxPerMinute?: number | null;
  maxPerDay?: number | null;
  failureCount?: number | null;
  lastFailureAt?: string | null;
  lastSuccessAt?: string | null;
  circuitOpenedAt?: string | null;
  hasPassword?: boolean;
  status?: string;
}

interface EmailLogItem {
  id: number;
  username: string;
  ip: string;
  email: string;
  codeMasked: string;
  sentAt: string;
  smtpId?: number | null;
  status: string;
}

interface IpStatsItem {
  ip: string;
  requestedToday: number;
  unverifiedToday: number;
  requestedTotal: number;
  unverifiedTotal: number;
  banStatus: string;
  bannedUntil?: string | null;
}

const emptySmtpForm = {
  name: '',
  host: '',
  port: '587',
  username: '',
  password: '',
  fromAddress: '',
  useTls: true,
  useSsl: false,
  enabled: true,
  maxPerMinute: '',
  maxPerDay: ''
};

const EmailVerificationManager = () => {
  const token = localStorage.getItem('sam_token') || undefined;
  const [smtpConfigs, setSmtpConfigs] = useState<SmtpConfig[]>([]);
  const [smtpForm, setSmtpForm] = useState({ ...emptySmtpForm });
  const [editingId, setEditingId] = useState<number | null>(null);
  const [smtpTestEmail, setSmtpTestEmail] = useState('');

  const [logItems, setLogItems] = useState<EmailLogItem[]>([]);
  const [logPage, setLogPage] = useState(0);
  const [logTotal, setLogTotal] = useState(0);
  const [logStart, setLogStart] = useState(() => new Date().toISOString().slice(0, 10));
  const [logEnd, setLogEnd] = useState(() => new Date().toISOString().slice(0, 10));
  const [logSort, setLogSort] = useState('sentAt,desc');

  const [ipStats, setIpStats] = useState<IpStatsItem[]>([]);
  const [ipPage, setIpPage] = useState(0);
  const [ipTotal, setIpTotal] = useState(0);
  const [ipDate, setIpDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [ipSortField, setIpSortField] = useState('unverifiedToday');
  const [ipSortDir, setIpSortDir] = useState('desc');

  const [banIp, setBanIp] = useState('');
  const [banUntil, setBanUntil] = useState(() => {
    const next = new Date();
    next.setDate(next.getDate() + 1);
    return next.toISOString().slice(0, 16);
  });
  const [banReason, setBanReason] = useState('');

  const logPageCount = useMemo(() => Math.ceil(logTotal / 20), [logTotal]);
  const ipPageCount = useMemo(() => Math.ceil(ipTotal / 20), [ipTotal]);

  const loadSmtpConfigs = () => {
    api.adminEmailSmtpConfigs(token)
      .then(setSmtpConfigs)
      .catch(() => toast.error('读取 SMTP 配置失败'));
  };

  const loadLogs = (page = logPage) => {
    api.adminEmailLogs({ start: logStart, end: logEnd, page, size: 20, sort: logSort }, token)
      .then((res) => {
        setLogItems(res.items || []);
        setLogTotal(res.total || 0);
        setLogPage(res.page || 0);
      })
      .catch(() => toast.error('读取发送日志失败'));
  };

  const loadIpStats = (page = ipPage) => {
    api.adminEmailIpStats({ date: ipDate, page, size: 20, sortField: ipSortField, sortDir: ipSortDir }, token)
      .then((res) => {
        setIpStats(res.items || []);
        setIpTotal(res.total || 0);
        setIpPage(res.page || 0);
      })
      .catch(() => toast.error('读取 IP 统计失败'));
  };

  useEffect(() => {
    loadSmtpConfigs();
    loadLogs(0);
    loadIpStats(0);
  }, []);

  const resetSmtpForm = () => {
    setSmtpForm({ ...emptySmtpForm });
    setEditingId(null);
  };

  const handleEdit = (config: SmtpConfig) => {
    setEditingId(config.id);
    setSmtpForm({
      name: config.name || '',
      host: config.host,
      port: String(config.port),
      username: config.username || '',
      password: '',
      fromAddress: config.fromAddress,
      useTls: config.useTls,
      useSsl: config.useSsl,
      enabled: config.enabled,
      maxPerMinute: config.maxPerMinute != null ? String(config.maxPerMinute) : '',
      maxPerDay: config.maxPerDay != null ? String(config.maxPerDay) : ''
    });
  };

  const handleSave = async () => {
    const payload = {
      name: smtpForm.name || undefined,
      host: smtpForm.host.trim(),
      port: Number(smtpForm.port),
      username: smtpForm.username || undefined,
      password: smtpForm.password || undefined,
      fromAddress: smtpForm.fromAddress.trim(),
      useTls: smtpForm.useTls,
      useSsl: smtpForm.useSsl,
      enabled: smtpForm.enabled,
      maxPerMinute: smtpForm.maxPerMinute === '' ? undefined : Number(smtpForm.maxPerMinute),
      maxPerDay: smtpForm.maxPerDay === '' ? undefined : Number(smtpForm.maxPerDay)
    };
    try {
      if (editingId) {
        await api.adminUpdateEmailSmtpConfig(editingId, payload, token);
        toast.success('SMTP 配置已更新');
      } else {
        await api.adminCreateEmailSmtpConfig(payload, token);
        toast.success('SMTP 配置已创建');
      }
      loadSmtpConfigs();
      resetSmtpForm();
    } catch (err) {
      toast.error('保存失败，请检查参数');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await api.adminDeleteEmailSmtpConfig(id, token);
      toast.success('已删除');
      loadSmtpConfigs();
    } catch {
      toast.error('删除失败');
    }
  };

  const handleTest = async (id: number) => {
    if (!smtpTestEmail) {
      toast.error('请输入测试收件邮箱');
      return;
    }
    try {
      await api.adminTestEmailSmtpConfig(id, smtpTestEmail, token);
      toast.success('测试邮件已发送');
    } catch {
      toast.error('测试失败');
    }
  };

  const handleDecrypt = async (id: number) => {
    try {
      const res = await api.adminDecryptEmailLog(id, token);
      toast.success(`验证码：${res.code}`);
    } catch {
      toast.error('解密失败');
    }
  };

  const handleManualBan = async () => {
    if (!banIp || !banUntil) {
      toast.error('请输入 IP 与解封时间');
      return;
    }
    try {
      await api.adminManualBanIp(banIp, new Date(banUntil).toISOString(), banReason || undefined, token);
      toast.success('IP 已封禁');
      setBanIp('');
      setBanReason('');
      loadIpStats();
    } catch {
      toast.error('封禁失败');
    }
  };

  const handleManualUnban = async (ip: string) => {
    try {
      await api.adminManualUnbanIp(ip, token);
      toast.success('IP 已解封');
      loadIpStats();
    } catch {
      toast.error('解封失败');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-2xl font-bold text-white">邮件验证管理</h3>
        <p className="text-slate-400">配置 SMTP 服务、追踪发送日志，并管理 IP 风控封禁。</p>
      </div>

      <Tabs defaultValue="smtp" className="w-full">
        <TabsList className="grid w-full grid-cols-3 bg-slate-900/50 border border-white/10">
          <TabsTrigger value="smtp">SMTP 配置</TabsTrigger>
          <TabsTrigger value="logs">发送日志</TabsTrigger>
          <TabsTrigger value="ip">IP 统计与封禁</TabsTrigger>
        </TabsList>

        <TabsContent value="smtp" className="space-y-6">
          <Card className="bg-slate-900 border-white/10">
            <CardHeader>
              <CardTitle className="text-white">SMTP 服务列表</CardTitle>
              <CardDescription>可配置多组 SMTP，用于随机池发送与故障切换。</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-wrap gap-3 items-center">
                <Label className="text-slate-400">测试收件邮箱</Label>
                <Input value={smtpTestEmail} onChange={(e) => setSmtpTestEmail(e.target.value)} className="bg-black/20 border-white/10 text-white w-72" placeholder="test@example.com" />
              </div>
              {smtpConfigs.length === 0 && <div className="text-slate-500 text-sm">暂无配置</div>}
              {smtpConfigs.map((config) => (
                <div key={config.id} className="bg-black/30 rounded-lg p-4 space-y-2">
                  <div className="flex flex-wrap justify-between gap-3 text-sm">
                    <div className="text-white font-medium">{config.name || '未命名 SMTP'} ({config.host}:{config.port})</div>
                    <div className="text-slate-400">状态: {config.status || 'UNKNOWN'} / 失败 {config.failureCount ?? 0}</div>
                  </div>
                  <div className="text-xs text-slate-400 flex flex-wrap gap-4">
                    <span>From: {config.fromAddress}</span>
                    <span>启用: {config.enabled ? '是' : '否'}</span>
                    <span>限速: {config.maxPerMinute ?? '∞'}/min, {config.maxPerDay ?? '∞'}/day</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button size="sm" variant="secondary" className="bg-slate-800" onClick={() => handleEdit(config)}>编辑</Button>
                    <Button size="sm" variant="secondary" className="bg-slate-800" onClick={() => handleTest(config.id)}>测试发送</Button>
                    <Button size="sm" variant="destructive" onClick={() => handleDelete(config.id)}>删除</Button>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card className="bg-slate-900 border-white/10">
            <CardHeader>
              <CardTitle className="text-white">{editingId ? '编辑 SMTP' : '新增 SMTP'}</CardTitle>
              <CardDescription>支持 TLS/SSL、限速与启用状态。</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label className="text-slate-300">名称</Label>
                  <Input value={smtpForm.name} onChange={(e) => setSmtpForm({ ...smtpForm, name: e.target.value })} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">From 地址</Label>
                  <Input value={smtpForm.fromAddress} onChange={(e) => setSmtpForm({ ...smtpForm, fromAddress: e.target.value })} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">Host</Label>
                  <Input value={smtpForm.host} onChange={(e) => setSmtpForm({ ...smtpForm, host: e.target.value })} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">Port</Label>
                  <Input value={smtpForm.port} onChange={(e) => setSmtpForm({ ...smtpForm, port: e.target.value })} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">用户名</Label>
                  <Input value={smtpForm.username} onChange={(e) => setSmtpForm({ ...smtpForm, username: e.target.value })} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">密码</Label>
                  <Input type="password" value={smtpForm.password} onChange={(e) => setSmtpForm({ ...smtpForm, password: e.target.value })} className="bg-black/20 border-white/10 text-white" placeholder={editingId ? '留空表示不变' : ''} />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">每分钟限额</Label>
                  <Input value={smtpForm.maxPerMinute} onChange={(e) => setSmtpForm({ ...smtpForm, maxPerMinute: e.target.value })} className="bg-black/20 border-white/10 text-white" placeholder="留空不限" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">每日限额</Label>
                  <Input value={smtpForm.maxPerDay} onChange={(e) => setSmtpForm({ ...smtpForm, maxPerDay: e.target.value })} className="bg-black/20 border-white/10 text-white" placeholder="留空不限" />
                </div>
              </div>
              <div className="flex flex-wrap gap-6 text-sm text-slate-300">
                <label className="flex items-center gap-2">
                  <Switch checked={smtpForm.useTls} onCheckedChange={(val) => setSmtpForm({ ...smtpForm, useTls: val })} />
                  启用 TLS
                </label>
                <label className="flex items-center gap-2">
                  <Switch checked={smtpForm.useSsl} onCheckedChange={(val) => setSmtpForm({ ...smtpForm, useSsl: val })} />
                  启用 SSL
                </label>
                <label className="flex items-center gap-2">
                  <Switch checked={smtpForm.enabled} onCheckedChange={(val) => setSmtpForm({ ...smtpForm, enabled: val })} />
                  启用配置
                </label>
              </div>
              <div className="flex flex-wrap gap-2">
                <Button className="bg-teal-600 hover:bg-teal-700" onClick={handleSave}>保存配置</Button>
                {editingId && (
                  <Button variant="secondary" className="bg-slate-800" onClick={resetSmtpForm}>取消编辑</Button>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="logs" className="space-y-6">
          <Card className="bg-slate-900 border-white/10">
            <CardHeader>
              <CardTitle className="text-white">发送日志</CardTitle>
              <CardDescription>按时间范围查询验证码发送记录，支持解密查看。</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-wrap gap-4 items-end">
                <div className="space-y-2">
                  <Label className="text-slate-300">开始日期</Label>
                  <Input type="date" value={logStart} onChange={(e) => setLogStart(e.target.value)} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">结束日期</Label>
                  <Input type="date" value={logEnd} onChange={(e) => setLogEnd(e.target.value)} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">排序</Label>
                  <Select value={logSort} onValueChange={setLogSort}>
                    <SelectTrigger className="bg-black/20 border-white/10 text-white w-44">
                      <SelectValue placeholder="排序" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="sentAt,desc">时间倒序</SelectItem>
                      <SelectItem value="sentAt,asc">时间正序</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <Button className="bg-teal-600 hover:bg-teal-700" onClick={() => loadLogs(0)}>查询</Button>
              </div>
              <div className="space-y-2">
                {logItems.length === 0 && <div className="text-slate-500 text-sm">暂无日志</div>}
                {logItems.map((item) => (
                  <div key={item.id} className="bg-black/30 rounded-lg p-3 text-sm flex flex-wrap items-center justify-between gap-3">
                    <div className="space-y-1">
                      <div className="text-white">{item.email} / {item.username}</div>
                      <div className="text-xs text-slate-400">IP: {item.ip} · 时间: {item.sentAt}</div>
                      <div className="text-xs text-slate-400">验证码: {item.codeMasked} · 状态: {item.status}</div>
                    </div>
                    <Button size="sm" variant="secondary" className="bg-slate-800" onClick={() => handleDecrypt(item.id)}>解密查看</Button>
                  </div>
                ))}
              </div>
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span>第 {logPage + 1} / {Math.max(1, logPageCount)} 页</span>
                <div className="flex gap-2">
                  <Button size="sm" variant="secondary" className="bg-slate-800" disabled={logPage <= 0} onClick={() => loadLogs(logPage - 1)}>上一页</Button>
                  <Button size="sm" variant="secondary" className="bg-slate-800" disabled={logPage + 1 >= logPageCount} onClick={() => loadLogs(logPage + 1)}>下一页</Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="ip" className="space-y-6">
          <Card className="bg-slate-900 border-white/10">
            <CardHeader>
              <CardTitle className="text-white">IP 统计与封禁</CardTitle>
              <CardDescription>查看每日/累计统计，支持手动封禁与解封。</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-wrap gap-4 items-end">
                <div className="space-y-2">
                  <Label className="text-slate-300">日期</Label>
                  <Input type="date" value={ipDate} onChange={(e) => setIpDate(e.target.value)} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">排序字段</Label>
                  <Select value={ipSortField} onValueChange={setIpSortField}>
                    <SelectTrigger className="bg-black/20 border-white/10 text-white w-44">
                      <SelectValue placeholder="排序字段" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="unverifiedToday">当天未验证</SelectItem>
                      <SelectItem value="requestedToday">当天请求数</SelectItem>
                      <SelectItem value="unverifiedTotal">累计未验证</SelectItem>
                      <SelectItem value="requestedTotal">累计请求数</SelectItem>
                      <SelectItem value="ip">IP</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">排序方向</Label>
                  <Select value={ipSortDir} onValueChange={setIpSortDir}>
                    <SelectTrigger className="bg-black/20 border-white/10 text-white w-32">
                      <SelectValue placeholder="排序" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="desc">倒序</SelectItem>
                      <SelectItem value="asc">正序</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <Button className="bg-teal-600 hover:bg-teal-700" onClick={() => loadIpStats(0)}>查询</Button>
              </div>
              <div className="space-y-2">
                {ipStats.length === 0 && <div className="text-slate-500 text-sm">暂无统计</div>}
                {ipStats.map((item) => (
                  <div key={item.ip} className="bg-black/30 rounded-lg p-3 text-sm flex flex-wrap items-center justify-between gap-3">
                    <div className="space-y-1">
                      <div className="text-white">{item.ip}</div>
                      <div className="text-xs text-slate-400">当日: {item.requestedToday} 请求 / {item.unverifiedToday} 未验证</div>
                      <div className="text-xs text-slate-400">累计: {item.requestedTotal} 请求 / {item.unverifiedTotal} 未验证</div>
                      <div className="text-xs text-slate-400">封禁: {item.banStatus} {item.bannedUntil ? `至 ${item.bannedUntil}` : ''}</div>
                    </div>
                    {item.banStatus === 'MANUAL' && (
                      <Button size="sm" variant="secondary" className="bg-slate-800" onClick={() => handleManualUnban(item.ip)}>解封</Button>
                    )}
                  </div>
                ))}
              </div>
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span>第 {ipPage + 1} / {Math.max(1, ipPageCount)} 页</span>
                <div className="flex gap-2">
                  <Button size="sm" variant="secondary" className="bg-slate-800" disabled={ipPage <= 0} onClick={() => loadIpStats(ipPage - 1)}>上一页</Button>
                  <Button size="sm" variant="secondary" className="bg-slate-800" disabled={ipPage + 1 >= ipPageCount} onClick={() => loadIpStats(ipPage + 1)}>下一页</Button>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-slate-900 border-white/10">
            <CardHeader>
              <CardTitle className="text-white">手动封禁 IP</CardTitle>
              <CardDescription>设置封禁 IP 与自动解封时间。</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid md:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <Label className="text-slate-300">IP</Label>
                  <Input value={banIp} onChange={(e) => setBanIp(e.target.value)} className="bg-black/20 border-white/10 text-white" placeholder="127.0.0.1" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">解封时间</Label>
                  <Input type="datetime-local" value={banUntil} onChange={(e) => setBanUntil(e.target.value)} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">原因</Label>
                  <Input value={banReason} onChange={(e) => setBanReason(e.target.value)} className="bg-black/20 border-white/10 text-white" placeholder="可选" />
                </div>
              </div>
              <Button className="bg-rose-600 hover:bg-rose-700" onClick={handleManualBan}>封禁</Button>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default EmailVerificationManager;
