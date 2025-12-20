import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Button } from '@/components/ui/button';
import { Save, Plus, User, Upload, Trash2 } from 'lucide-react';
import { api } from '@/lib/api';
import { toast } from 'sonner';

const CharacterManager = () => {
  const token = localStorage.getItem('sam_token') || undefined;
  const [characters, setCharacters] = useState<any[]>([]);
  const [importText, setImportText] = useState('');

  useEffect(() => {
    api.adminCharacters(token).then(setCharacters).catch(() => toast.error('加载角色失败'));
  }, []);

  const handleField = (id: any, field: string, value: any) => {
    setCharacters(prev => prev.map(c => c.id === id ? { ...c, [field]: value } : c));
  };

  const saveCharacter = async (char: any) => {
    try {
      const payload = {
        id: typeof char.id === 'number' ? char.id : null,
        name: char.name,
        role: char.role,
        prompt: char.prompt,
        description: char.description,
        avatarUrl: char.avatarUrl
      };
      const saved = await api.adminSaveCharacter(payload, token);
      setCharacters(prev => prev.map(c => c.id === char.id ? saved : c));
      toast.success('角色已保存');
    } catch {
      toast.error('保存失败');
    }
  };

  const addCharacter = () => {
    setCharacters(prev => [...prev, { id: `tmp-${Date.now()}`, name: '新角色', role: '', prompt: '', description: '', avatarUrl: '' }]);
  };

  const handleImport = async () => {
    try {
      const parsed = JSON.parse(importText);
      if (!Array.isArray(parsed)) {
        throw new Error('invalid');
      }
      await api.adminBatchCharacters(parsed, token);
      toast.success('批量导入成功');
      setImportText('');
      api.adminCharacters(token).then(setCharacters);
    } catch {
      toast.error('导入失败，请检查 JSON');
    }
  };

  const handleUpload = async (charId: any, file: File) => {
    try {
      const res = await api.uploadImage(file, token);
      setCharacters(prev => prev.map(c => c.id === charId ? { ...c, avatarUrl: res.url } : c));
      toast.success('上传成功');
    } catch {
      toast.error('上传失败');
    }
  };

  const deleteCharacter = async (char: any) => {
    if (typeof char.id !== 'number') {
      setCharacters(prev => prev.filter(c => c.id !== char.id));
      return;
    }
    try {
      await api.adminDeleteCharacter(char.id, token);
      setCharacters(prev => prev.filter(c => c.id !== char.id));
      toast.success('已删除');
    } catch {
      toast.error('删除失败');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-2xl font-bold text-white">角色档案管理</h3>
          <p className="text-slate-400">配置 NPC 的基本信息和 AI 扮演设定。</p>
        </div>
        <Button className="bg-teal-600 hover:bg-teal-700" onClick={() => characters.forEach(saveCharacter)}>
          <Save className="w-4 h-4 mr-2" />
          保存档案
        </Button>
      </div>

      <Card className="bg-slate-900 border-white/10">
        <CardHeader>
          <CardTitle className="text-lg text-white">批量导入角色</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <Textarea 
            value={importText}
            onChange={(e) => setImportText(e.target.value)}
            placeholder='JSON 数组，如 [{"name":"花火","role":"trickster"}]'
            className="bg-black/20 border-white/10 text-white h-28" 
          />
          <Button onClick={handleImport} className="bg-teal-600 hover:bg-teal-700">
            <Upload className="w-4 h-4 mr-2" /> 导入
          </Button>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {characters.map((char) => (
          <Card key={char.id} className="bg-slate-900 border-white/10">
            <CardHeader className="flex flex-row items-center gap-4 pb-2">
              <div className="w-12 h-12 rounded-full bg-slate-800 flex items-center justify-center border border-white/10">
                <User className="text-slate-400" />
              </div>
              <div className="flex flex-col gap-1">
                <Input value={char.name || ''} onChange={(e) => handleField(char.id, 'name', e.target.value)} className="bg-black/30 border-white/10 text-white" />
                <Input value={char.role || ''} onChange={(e) => handleField(char.id, 'role', e.target.value)} className="bg-black/20 border-white/10 text-xs text-white" placeholder="角色定位" />
              </div>
              <div className="ml-auto flex gap-2">
                <Button size="sm" variant="ghost" onClick={() => saveCharacter(char)} className="text-teal-300">保存</Button>
                <Button size="sm" variant="ghost" onClick={() => deleteCharacter(char)} className="text-red-400 hover:text-red-300">
                  <Trash2 className="w-4 h-4" />
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label className="text-slate-300">立绘/头像 URL</Label>
                <Input value={char.avatarUrl || ''} onChange={(e) => handleField(char.id, 'avatarUrl', e.target.value)} placeholder="/assets/npc/..." className="bg-black/20 border-white/10 text-white" />
                <Input type="file" onChange={(e) => e.target.files && handleUpload(char.id, e.target.files[0])} className="bg-black/20 border-white/10 text-white" />
              </div>
              <div className="space-y-2">
                <Label className="text-slate-300">简介信息 (UI展示)</Label>
                <Input value={char.description || ''} onChange={(e) => handleField(char.id, 'description', e.target.value)} className="bg-black/20 border-white/10 text-white" />
              </div>
              <div className="space-y-2">
                <Label className="text-teal-400">详细人设 (Prompt)</Label>
                <Textarea 
                  placeholder="输入角色的性格特征、说话语气、背景故事等，用于 Prompt Engineering..."
                  value={char.prompt || ''}
                  onChange={(e) => handleField(char.id, 'prompt', e.target.value)}
                  className="bg-black/20 border-teal-500/30 text-white h-32" 
                />
              </div>
              <Button size="sm" onClick={() => saveCharacter(char)} className="bg-teal-600 hover:bg-teal-700 text-white">保存</Button>
            </CardContent>
          </Card>
        ))}
        
        {/* 添加按钮 */}
        <Button variant="outline" onClick={addCharacter} className="h-auto min-h-[300px] border-dashed border-white/20 text-slate-400 hover:text-white hover:bg-white/5 flex flex-col gap-2">
          <Plus className="w-8 h-8" />
          <span>新建角色档案</span>
        </Button>
      </div>
    </div>
  );
};

export default CharacterManager;
