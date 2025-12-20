import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Save, Plus, Upload, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { api } from '@/lib/api';

const LocationManager = () => {
  const token = localStorage.getItem('sam_token') || undefined;
  const [locations, setLocations] = useState<any[]>([]);
  const [domains, setDomains] = useState<any[]>([]);
  const [locationImport, setLocationImport] = useState('');
  const [domainImport, setDomainImport] = useState('');

  useEffect(() => {
    api.adminLocations(token).then(setLocations).catch(() => toast.error('加载区域失败'));
    api.adminDomains(token).then(setDomains).catch(() => toast.error('加载星域失败'));
  }, []);

  const handleLocationField = (id: any, field: string, value: any) => {
    setLocations(prev => prev.map(l => l.id === id ? { ...l, [field]: value } : l));
  };

  const saveLocation = async (loc: any) => {
    try {
      const payload = {
        id: typeof loc.id === 'number' ? loc.id : null,
        code: loc.code,
        name: loc.name,
        description: loc.description,
        aiDescription: loc.aiDescription,
        backgroundStyle: loc.backgroundStyle,
        backgroundUrl: loc.backgroundUrl,
        coordX: Number(loc.coordX),
        coordY: Number(loc.coordY),
        unlocked: !!loc.unlocked,
        domainCode: loc.domainCode
      };
      const saved = await api.adminSaveLocation(payload, token);
      setLocations(prev => prev.map(l => l.id === loc.id ? saved : l));
      toast.success('区域已保存');
    } catch {
      toast.error('保存失败');
    }
  };

  const saveDomain = async (domain: any) => {
    try {
      const payload = {
        id: typeof domain.id === 'number' ? domain.id : null,
        code: domain.code,
        name: domain.name,
        description: domain.description,
        aiDescription: domain.aiDescription,
        coordX: Number(domain.coordX),
        coordY: Number(domain.coordY),
        color: domain.color
      };
      const saved = await api.adminSaveDomain(payload, token);
      setDomains(prev => prev.map(d => d.id === domain.id ? saved : d));
      toast.success('星域已保存');
    } catch {
      toast.error('保存失败');
    }
  };

  const addLocation = () => {
    setLocations(prev => [...prev, { id: `tmp-${Date.now()}`, name: '新区域', code: `loc-${prev.length+1}`, description: '', aiDescription: '', domainCode: domains[0]?.code || '', backgroundStyle: '', backgroundUrl: '', unlocked: false, coordX: 50, coordY: 50 }]);
  };

  const addDomain = () => {
    setDomains(prev => [...prev, { id: `tmp-${Date.now()}`, name: '新星域', code: `domain-${prev.length+1}`, description: '', aiDescription: '', coordX: 50, coordY: 50, color: 'text-purple-400' }]);
  };

  const handleLocationImport = async () => {
    try {
      const parsed = JSON.parse(locationImport);
      if (!Array.isArray(parsed)) {
        throw new Error('invalid');
      }
      await api.adminBatchLocations(parsed, token);
      toast.success('批量导入成功');
      setLocationImport('');
      api.adminLocations(token).then(setLocations);
    } catch {
      toast.error('导入失败，请检查 JSON');
    }
  };

  const handleDomainImport = async () => {
    try {
      const parsed = JSON.parse(domainImport);
      if (!Array.isArray(parsed)) {
        throw new Error('invalid');
      }
      await api.adminBatchDomains(parsed, token);
      toast.success('批量导入成功');
      setDomainImport('');
      api.adminDomains(token).then(setDomains);
    } catch {
      toast.error('导入失败，请检查 JSON');
    }
  };

  const handleUpload = async (locId: any, file: File) => {
    try {
      const res = await api.uploadImage(file, token);
      setLocations(prev => prev.map(l => l.id === locId ? { ...l, backgroundUrl: res.url } : l));
      toast.success('上传成功');
    } catch {
      toast.error('上传失败');
    }
  };

  const deleteLocation = async (loc: any) => {
    if (typeof loc.id !== 'number') {
      setLocations(prev => prev.filter(l => l.id !== loc.id));
      return;
    }
    try {
      await api.adminDeleteLocation(loc.id, token);
      setLocations(prev => prev.filter(l => l.id !== loc.id));
      toast.success('已删除');
    } catch {
      toast.error('删除失败');
    }
  };

  const deleteDomain = async (domain: any) => {
    if (typeof domain.id !== 'number') {
      setDomains(prev => prev.filter(d => d.id !== domain.id));
      return;
    }
    try {
      await api.adminDeleteDomain(domain.id, token);
      setDomains(prev => prev.filter(d => d.id !== domain.id));
      toast.success('已删除');
    } catch {
      toast.error('删除失败');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-2xl font-bold text-white">区域与星球管理</h3>
          <p className="text-slate-400">管理游戏内的星域结构和具体场景信息。</p>
        </div>
        <Button className="bg-teal-600 hover:bg-teal-700" onClick={() => { locations.forEach(saveLocation); domains.forEach(saveDomain); }}>
          <Save className="w-4 h-4 mr-2" />
          保存所有更改
        </Button>
      </div>

      <Tabs defaultValue="locations" className="w-full">
        <TabsList className="bg-slate-900 border border-white/10">
          <TabsTrigger value="locations">区域场景 (Locations)</TabsTrigger>
          <TabsTrigger value="domains">星域 (Domains)</TabsTrigger>
        </TabsList>

        <TabsContent value="locations" className="mt-6 space-y-6">
          <Card className="bg-slate-900 border-white/10">
            <CardHeader>
              <CardTitle className="text-lg text-white">批量导入区域</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <Textarea 
                value={locationImport}
                onChange={(e) => setLocationImport(e.target.value)}
                placeholder='JSON 数组，如 [{"code":"golden-hour","name":"黄金的时刻","domainCode":"penacony"}]'
                className="bg-black/20 border-white/10 text-white h-28"
              />
              <Button onClick={handleLocationImport} className="bg-teal-600 hover:bg-teal-700">
                <Upload className="w-4 h-4 mr-2" /> 导入
              </Button>
            </CardContent>
          </Card>
          {locations.map((loc) => (
            <Card key={loc.id} className="bg-slate-900 border-white/10">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-lg text-white">{loc.name}</CardTitle>
                <div className="flex gap-2">
                  <Button variant="ghost" size="sm" onClick={() => saveLocation(loc)} className="text-teal-300">保存</Button>
                  <Button variant="ghost" size="sm" onClick={() => deleteLocation(loc)} className="text-red-400 hover:text-red-300">
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label className="text-slate-300">场景代码</Label>
                    <Input value={loc.code || ''} onChange={(e) => handleLocationField(loc.id, 'code', e.target.value)} className="bg-black/20 border-white/10 text-white" />
                  </div>
                  <div className="space-y-2">
                    <Label className="text-slate-300">场景名称</Label>
                    <Input value={loc.name} onChange={(e) => handleLocationField(loc.id, 'name', e.target.value)} className="bg-black/20 border-white/10 text-white" />
                  </div>
                  <div className="space-y-2">
                    <Label className="text-slate-300">所属星域</Label>
                    <Select value={loc.domainCode || ''} onValueChange={(value) => handleLocationField(loc.id, 'domainCode', value)}>
                      <SelectTrigger className="bg-black/20 border-white/10 text-white">
                        <SelectValue placeholder="选择星域" />
                      </SelectTrigger>
                      <SelectContent>
                        {domains.map((domain) => (
                          <SelectItem key={domain.code} value={domain.code}>
                            {domain.name} ({domain.code})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label className="text-slate-300">背景图 URL</Label>
                    <Input value={loc.backgroundUrl || ''} onChange={(e) => handleLocationField(loc.id, 'backgroundUrl', e.target.value)} placeholder="https://..." className="bg-black/20 border-white/10 text-white" />
                    <Input type="file" onChange={(e) => e.target.files && handleUpload(loc.id, e.target.files[0])} className="bg-black/20 border-white/10 text-white" />
                  </div>
                  <div className="space-y-2">
                    <Label className="text-slate-300">背景样式 (备用)</Label>
                    <Input value={loc.backgroundStyle || ''} onChange={(e) => handleLocationField(loc.id, 'backgroundStyle', e.target.value)} className="bg-black/20 border-white/10 text-white" />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label className="text-slate-300">坐标 X</Label>
                      <Input value={loc.coordX ?? ''} onChange={(e) => handleLocationField(loc.id, 'coordX', e.target.value)} className="bg-black/20 border-white/10 text-white" />
                    </div>
                    <div className="space-y-2">
                      <Label className="text-slate-300">坐标 Y</Label>
                      <Input value={loc.coordY ?? ''} onChange={(e) => handleLocationField(loc.id, 'coordY', e.target.value)} className="bg-black/20 border-white/10 text-white" />
                    </div>
                  </div>
                </div>
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label className="text-slate-300">简介信息 (UI展示)</Label>
                    <Textarea 
                      value={loc.description} 
                      onChange={(e) => handleLocationField(loc.id, 'description', e.target.value)}
                      className="bg-black/20 border-white/10 text-white h-20" 
                    />
                  </div>
                  <div className="space-y-2">
                    <Label className="text-teal-400">详细信息 (提交给大模型)</Label>
                    <Textarea 
                      value={loc.aiDescription || ''}
                      onChange={(e) => handleLocationField(loc.id, 'aiDescription', e.target.value)}
                      placeholder="在此输入该场景的详细设定、环境描写、可交互物体等，用于 AI 生成更准确的回复..."
                      className="bg-black/20 border-teal-500/30 text-white h-32" 
                    />
                  </div>
                  <div className="flex items-center gap-2">
                    <Label className="text-slate-300">解锁</Label>
                    <input type="checkbox" checked={!!loc.unlocked} onChange={(e) => handleLocationField(loc.id, 'unlocked', e.target.checked)} />
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
          <Button variant="outline" onClick={addLocation} className="w-full border-dashed border-white/20 text-slate-400 hover:text-white hover:bg-white/5">
            <Plus className="w-4 h-4 mr-2" /> 添加新区域
          </Button>
        </TabsContent>

        <TabsContent value="domains" className="mt-6 space-y-6">
          <Card className="bg-slate-900 border-white/10">
            <CardHeader>
              <CardTitle className="text-lg text-white">批量导入星域</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <Textarea 
                value={domainImport}
                onChange={(e) => setDomainImport(e.target.value)}
                placeholder='JSON 数组，如 [{"code":"penacony","name":"匹诺康尼"}]'
                className="bg-black/20 border-white/10 text-white h-28"
              />
              <Button onClick={handleDomainImport} className="bg-teal-600 hover:bg-teal-700">
                <Upload className="w-4 h-4 mr-2" /> 导入
              </Button>
            </CardContent>
          </Card>
          {domains.map((domain) => (
            <Card key={domain.id} className="bg-slate-900 border-white/10">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="text-lg text-white">{domain.name}</CardTitle>
                  <div className="flex gap-2">
                    <Button size="sm" variant="ghost" onClick={() => saveDomain(domain)} className="text-teal-300">保存</Button>
                    <Button size="sm" variant="ghost" onClick={() => deleteDomain(domain)} className="text-red-400 hover:text-red-300">
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label className="text-slate-300">星域代码</Label>
                  <Input value={domain.code || ''} onChange={(e) => setDomains(prev => prev.map(d => d.id === domain.id ? { ...d, code: e.target.value } : d))} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">星域名称</Label>
                  <Input value={domain.name || ''} onChange={(e) => setDomains(prev => prev.map(d => d.id === domain.id ? { ...d, name: e.target.value } : d))} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">星域简介</Label>
                  <Input value={domain.description} onChange={(e) => setDomains(prev => prev.map(d => d.id === domain.id ? { ...d, description: e.target.value } : d))} className="bg-black/20 border-white/10 text-white" />
                </div>
                <div className="space-y-2">
                  <Label className="text-slate-300">颜色 class</Label>
                  <Input value={domain.color || ''} onChange={(e) => setDomains(prev => prev.map(d => d.id === domain.id ? { ...d, color: e.target.value } : d))} className="bg-black/20 border-white/10 text-white" placeholder="text-purple-400" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label className="text-slate-300">坐标 X</Label>
                    <Input value={domain.coordX ?? ''} onChange={(e) => setDomains(prev => prev.map(d => d.id === domain.id ? { ...d, coordX: e.target.value } : d))} className="bg-black/20 border-white/10 text-white" />
                  </div>
                  <div className="space-y-2">
                    <Label className="text-slate-300">坐标 Y</Label>
                    <Input value={domain.coordY ?? ''} onChange={(e) => setDomains(prev => prev.map(d => d.id === domain.id ? { ...d, coordY: e.target.value } : d))} className="bg-black/20 border-white/10 text-white" />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label className="text-teal-400">详细设定 (AI)</Label>
                  <Textarea 
                    value={domain.aiDescription || ''}
                    onChange={(e) => setDomains(prev => prev.map(d => d.id === domain.id ? { ...d, aiDescription: e.target.value } : d))}
                    className="bg-black/20 border-teal-500/30 text-white" placeholder="星域的历史背景、风土人情..." 
                  />
                </div>
              </CardContent>
            </Card>
          ))}
          <Button variant="outline" onClick={addDomain} className="w-full border-dashed border-white/20 text-slate-400 hover:text-white hover:bg-white/5">
            <Plus className="w-4 h-4 mr-2" /> 添加新星域
          </Button>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default LocationManager;
