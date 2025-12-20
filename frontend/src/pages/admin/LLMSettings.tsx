import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Save, Key, Server, Bot } from 'lucide-react';
import { toast } from 'sonner';
import { api } from '@/lib/api';

const LLMSettings = () => {
  const token = localStorage.getItem('sam_token') || undefined;
  const [baseUrl, setBaseUrl] = useState('');
  const [apiKeyValue, setApiKeyValue] = useState('');
  const [modelName, setModelName] = useState('');
  const [temperature, setTemperature] = useState(0.7);

  useEffect(() => {
    api.adminLlm(token).then((data) => {
      setBaseUrl(data.baseUrl || '');
      setModelName(data.modelName || '');
      setTemperature(data.temperature ?? 0.7);
    }).catch(() => toast.error('读取配置失败'));
  }, []);

  const handleSave = async () => {
    try {
      await api.saveAdminLlm({ baseUrl, apiKey: apiKeyValue, modelName, temperature }, token);
      const res = await api.testAdminLlm(token);
      toast.success(`配置成功: ${res.status}`);
    } catch (err) {
      toast.error('保存或测试失败');
    }
  };

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h3 className="text-2xl font-bold text-white">模型连接设置</h3>
        <p className="text-slate-400">配置后端 Spring AI 连接的大语言模型参数。</p>
      </div>

      <Card className="bg-slate-900 border-white/10">
        <CardHeader>
          <CardTitle className="text-white flex items-center gap-2">
            <Server className="w-5 h-5 text-teal-400" />
            基础配置
          </CardTitle>
          <CardDescription>设置 API 端点和认证信息。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="space-y-2">
            <Label className="text-slate-300">Base URL</Label>
            <Input 
              placeholder="https://api.openai.com/v1" 
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
              className="bg-black/20 border-white/10 text-white font-mono"
            />
            <p className="text-xs text-slate-500">如果是本地模型 (如 Ollama)，通常为 http://localhost:11434</p>
          </div>

          <div className="space-y-2">
            <Label className="text-slate-300 flex items-center gap-2">
              <Key className="w-3 h-3" /> API Key
            </Label>
            <Input 
              type="password"
              placeholder="sk-..." 
              value={apiKeyValue}
              onChange={(e) => setApiKeyValue(e.target.value)}
              className="bg-black/20 border-white/10 text-white font-mono"
            />
          </div>
        </CardContent>
      </Card>

      <Card className="bg-slate-900 border-white/10">
        <CardHeader>
          <CardTitle className="text-white flex items-center gap-2">
            <Bot className="w-5 h-5 text-teal-400" />
            模型参数
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="space-y-2">
            <Label className="text-slate-300">模型名称 (Model Name)</Label>
            <div className="flex gap-2">
              <Input 
                placeholder="gpt-4-turbo" 
                value={modelName}
                onChange={(e) => setModelName(e.target.value)}
                className="bg-black/20 border-white/10 text-white font-mono"
              />
              <Select>
                <SelectTrigger className="w-[180px] bg-slate-800 border-white/10 text-white">
                  <SelectValue placeholder="常用预设" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="gpt-4o" onClick={() => setModelName('gpt-4o')}>GPT-4o</SelectItem>
                  <SelectItem value="gpt-3.5-turbo" onClick={() => setModelName('gpt-3.5-turbo')}>GPT-3.5 Turbo</SelectItem>
                  <SelectItem value="claude-3-opus" onClick={() => setModelName('claude-3-opus')}>Claude 3 Opus</SelectItem>
                  <SelectItem value="qwen-turbo" onClick={() => setModelName('qwen-turbo')}>Qwen Turbo</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label className="text-slate-300">Temperature (随机性)</Label>
            <Input 
              type="number" 
              step="0.1" 
              min="0" 
              max="2" 
              value={temperature}
              onChange={(e) => setTemperature(parseFloat(e.target.value))}
              className="bg-black/20 border-white/10 text-white font-mono max-w-[100px]"
            />
          </div>
        </CardContent>
      </Card>

      <Button onClick={handleSave} className="w-full bg-teal-600 hover:bg-teal-700 text-white">
        <Save className="w-4 h-4 mr-2" />
        保存并测试连接
      </Button>
    </div>
  );
};

export default LLMSettings;
