import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Plus, RefreshCw, TestTube, Trash2, Save } from 'lucide-react';
import { toast } from 'sonner';
import { api } from '@/lib/api';

interface LlmApiConfig {
  id: number;
  name?: string;
  baseUrl: string;
  apiKey?: string;
  modelName: string;
  temperature?: number;
  role: 'PRIMARY' | 'BACKUP';
  tokenLimit?: number | null;
  tokenUsed?: number | null;
  status: 'ACTIVE' | 'CIRCUIT_OPEN' | 'DISABLED';
  failureCount?: number | null;
  maxLoad?: number | null;
  currentLoad?: number | null;
}

const emptyForm = {
  name: '',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  temperature: '0.7',
  role: 'PRIMARY' as const,
  status: 'ACTIVE' as const,
  tokenLimit: '',
  maxLoad: ''
};

const LLMSettings = () => {
  const token = localStorage.getItem('sam_token') || undefined;
  const [configs, setConfigs] = useState<LlmApiConfig[]>([]);
  const [form, setForm] = useState({ ...emptyForm });
  const [editingId, setEditingId] = useState<number | null>(null);

  const loadConfigs = () => {
    api.adminLlmApis(token)
      .then(setConfigs)
      .catch(() => toast.error('读取 API 池失败'));
  };

  useEffect(() => {
    loadConfigs();
  }, []);

  const resetForm = () => {
    setForm({ ...emptyForm });
    setEditingId(null);
  };

  const handleEdit = (config: LlmApiConfig) => {
    setEditingId(config.id);
    setForm({
      name: config.name || '',
      baseUrl: config.baseUrl,
      apiKey: '',
      modelName: config.modelName,
      temperature: String(config.temperature ?? 0.7),
      role: config.role,
      status: config.status,
      tokenLimit: config.tokenLimit != null ? String(config.tokenLimit) : '',
      maxLoad: config.maxLoad != null ? String(config.maxLoad) : ''
    });
  };

  const handleSave = async () => {
    const payload = {
      name: form.name?.trim() || undefined,
      baseUrl: form.baseUrl.trim(),
      apiKey: form.apiKey || undefined,
      modelName: form.modelName.trim(),
      temperature: Number(form.temperature),
      role: form.role,
      status: form.status,
      tokenLimit: form.tokenLimit === '' ? undefined : Number(form.tokenLimit),
      maxLoad: form.maxLoad === '' ? undefined : Number(form.maxLoad)
    };
    try {
      if (editingId) {
        await api.adminUpdateLlmApi(editingId, payload, token);
        toast.success('配置已更新');
      } else {
        await api.adminCreateLlmApi(payload, token);
        toast.success('配置已创建');
      }
      loadConfigs();
      resetForm();
    } catch (err) {
      toast.error('保存失败，请检查参数');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await api.adminDeleteLlmApi(id, token);
      toast.success('已删除');
      loadConfigs();
    } catch (err) {
      toast.error('删除失败');
    }
  };

  const handleResetTokens = async (id: number) => {
    try {
      await api.adminResetLlmTokens(id, token);
      toast.success('Token 已重置');
      loadConfigs();
    } catch (err) {
      toast.error('重置失败');
    }
  };

  const handleTest = async (id: number) => {
    try {
      const res = await api.adminTestLlmApi(id, token);
      toast.success(`测试结果: ${res.status}`);
    } catch (err) {
      toast.error('测试失败');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-2xl font-bold text-white">LLM API 池管理</h3>
        <p className="text-slate-400">配置主/备 API、负载与熔断状态，用于会话绑定与调度。</p>
      </div>

      <Card className="bg-slate-900 border-white/10">
        <CardHeader>
          <CardTitle className="text-white">API 池列表</CardTitle>
          <CardDescription>展示 API 当前状态、负载与 Token 统计。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-12 text-xs uppercase text-slate-500 gap-2">
            <span className="col-span-2">名称</span>
            <span className="col-span-3">Base URL</span>
            <span className="col-span-2">模型</span>
            <span className="col-span-2">Token</span>
            <span className="col-span-1">负载</span>
            <span className="col-span-1">角色</span>
            <span className="col-span-1">状态/失败</span>
          </div>
          {configs.length === 0 && (
            <div className="text-slate-500 text-sm">暂无配置</div>
          )}
          {configs.map((config) => (
            <div key={config.id} className="grid grid-cols-12 gap-2 items-center bg-black/30 rounded-lg p-3 text-sm">
              <span className="col-span-2 text-white truncate">{config.name || '未命名'}</span>
              <span className="col-span-3 text-slate-400 truncate" title={config.baseUrl}>{config.baseUrl}</span>
              <span className="col-span-2 text-slate-300 truncate">{config.modelName}</span>
              <span className="col-span-2 text-slate-300">
                {config.tokenUsed ?? 0} / {config.tokenLimit ?? '∞'}
              </span>
              <span className="col-span-1 text-slate-300">{config.currentLoad ?? 0}/{config.maxLoad ?? '∞'}</span>
              <span className="col-span-1 text-slate-300">{config.role}</span>
              <span className="col-span-1 text-slate-300">{config.status} / {config.failureCount ?? 0}</span>
              <div className="col-span-12 flex flex-wrap gap-2 justify-end pt-2">
                <Button size="sm" variant="secondary" className="bg-slate-800" onClick={() => handleEdit(config)}>
                  编辑
                </Button>
                <Button size="sm" variant="secondary" className="bg-slate-800" onClick={() => handleTest(config.id)}>
                  <TestTube className="w-4 h-4 mr-1" />测试
                </Button>
                <Button size="sm" variant="secondary" className="bg-slate-800" onClick={() => handleResetTokens(config.id)}>
                  <RefreshCw className="w-4 h-4 mr-1" />重置 Token
                </Button>
                <Button size="sm" variant="destructive" onClick={() => handleDelete(config.id)}>
                  <Trash2 className="w-4 h-4 mr-1" />删除
                </Button>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>

      <Card className="bg-slate-900 border-white/10">
        <CardHeader>
          <CardTitle className="text-white flex items-center gap-2">
            <Plus className="w-4 h-4 text-teal-400" />
            {editingId ? '编辑 API' : '新增 API'}
          </CardTitle>
          <CardDescription>Base URL 需为公网 http/https，API Key 可留空保持不变。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label className="text-slate-300">名称</Label>
              <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className="bg-black/20 border-white/10 text-white" />
            </div>
            <div className="space-y-2">
              <Label className="text-slate-300">角色</Label>
              <Select value={form.role} onValueChange={(value) => setForm({ ...form, role: value as 'PRIMARY' | 'BACKUP' })}>
                <SelectTrigger className="bg-black/20 border-white/10 text-white">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="PRIMARY">PRIMARY</SelectItem>
                  <SelectItem value="BACKUP">BACKUP</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="space-y-2">
            <Label className="text-slate-300">状态</Label>
            <Select value={form.status} onValueChange={(value) => setForm({ ...form, status: value as 'ACTIVE' | 'DISABLED' })}>
              <SelectTrigger className="bg-black/20 border-white/10 text-white">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ACTIVE">ACTIVE</SelectItem>
                <SelectItem value="DISABLED">DISABLED</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label className="text-slate-300">Base URL</Label>
            <Input value={form.baseUrl} onChange={(e) => setForm({ ...form, baseUrl: e.target.value })} className="bg-black/20 border-white/10 text-white font-mono" placeholder="https://api.example.com" />
          </div>

          <div className="space-y-2">
            <Label className="text-slate-300">API Key</Label>
            <Input type="password" value={form.apiKey} onChange={(e) => setForm({ ...form, apiKey: e.target.value })} className="bg-black/20 border-white/10 text-white font-mono" placeholder={editingId ? '留空保持不变' : 'sk-...'} />
          </div>

          <div className="grid md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label className="text-slate-300">模型名称</Label>
              <Input value={form.modelName} onChange={(e) => setForm({ ...form, modelName: e.target.value })} className="bg-black/20 border-white/10 text-white font-mono" placeholder="gpt-4o" />
            </div>
            <div className="space-y-2">
              <Label className="text-slate-300">Temperature</Label>
              <Input type="number" step="0.1" min="0" max="2" value={form.temperature} onChange={(e) => setForm({ ...form, temperature: e.target.value })} className="bg-black/20 border-white/10 text-white" />
            </div>
          </div>

          <div className="grid md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label className="text-slate-300">Token 上限</Label>
              <Input type="number" min="0" value={form.tokenLimit} onChange={(e) => setForm({ ...form, tokenLimit: e.target.value })} className="bg-black/20 border-white/10 text-white" placeholder="留空为不限" />
            </div>
            <div className="space-y-2">
              <Label className="text-slate-300">30s 最大负载</Label>
              <Input type="number" min="1" value={form.maxLoad} onChange={(e) => setForm({ ...form, maxLoad: e.target.value })} className="bg-black/20 border-white/10 text-white" placeholder="默认 30" />
            </div>
          </div>

          <div className="flex flex-wrap gap-3">
            <Button className="bg-teal-600 hover:bg-teal-700 text-white" onClick={handleSave}>
              <Save className="w-4 h-4 mr-2" />保存
            </Button>
            <Button variant="secondary" className="bg-slate-800" onClick={resetForm}>
              重置
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default LLMSettings;
