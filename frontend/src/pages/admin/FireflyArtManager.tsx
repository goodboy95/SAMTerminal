import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Save } from 'lucide-react';
import { toast } from 'sonner';
import { api } from '@/lib/api';

const EMOTIONS = ['normal', 'smile', 'sad', 'shy', 'excited', 'angry', 'thinking', 'surprise'];

const FireflyArtManager = () => {
  const token = localStorage.getItem('sam_token') || undefined;
  const [assets, setAssets] = useState<Record<string, string>>({});

  useEffect(() => {
    api.adminAssets(token).then((data) => {
      const map: Record<string, string> = {};
      data.forEach((item: any) => {
        map[item.emotion] = item.url;
      });
      setAssets(map);
    }).catch(() => toast.error('读取立绘配置失败'));
  }, []);

  const handleSave = async () => {
    try {
      await api.saveAdminAssets(assets, token);
      toast.success('立绘配置已保存');
    } catch (err) {
      toast.error('保存失败');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-2xl font-bold text-white">流萤立绘管理</h3>
          <p className="text-slate-400">配置流萤在不同情绪状态下显示的 Live2D 或图片资源。</p>
        </div>
        <Button onClick={handleSave} className="bg-teal-600 hover:bg-teal-700">
          <Save className="w-4 h-4 mr-2" />
          保存配置
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {EMOTIONS.map((emotion) => (
          <Card key={emotion} className="bg-slate-900 border-white/10">
            <CardHeader className="pb-2">
              <CardTitle className="text-lg text-white capitalize flex items-center gap-2">
                {emotion}
                <span className="text-xs font-normal text-slate-500 px-2 py-0.5 bg-white/5 rounded">
                  {getEmotionLabel(emotion)}
                </span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* 预览区域 */}
              <div className="aspect-[3/4] bg-black/40 rounded-lg border border-white/5 flex items-center justify-center overflow-hidden relative group">
                {assets[emotion] ? (
                  <img src={assets[emotion]} alt={emotion} className="w-full h-full object-contain" />
                ) : (
                  <div className="text-slate-600 text-sm">暂无图片</div>
                )}
                <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white text-xs">
                  预览模式
                </div>
              </div>
              
              <div className="space-y-2">
                <Label className="text-slate-300">资源 URL</Label>
                <Input 
                  placeholder={`/assets/firefly/${emotion}.png`}
                  className="bg-black/20 border-white/10 text-white"
                  value={assets[emotion] || ''}
                  onChange={(e) => setAssets(prev => ({ ...prev, [emotion]: e.target.value }))}
                />
                <Input
                  type="file"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (!file) return;
                    api.uploadImage(file, token)
                      .then((res) => setAssets(prev => ({ ...prev, [emotion]: res.url })))
                      .catch(() => toast.error('上传失败'));
                  }}
                  className="bg-black/20 border-white/10 text-white"
                />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
};

function getEmotionLabel(e: string) {
  const map: Record<string, string> = {
    normal: '常态', smile: '微笑', sad: '悲伤', shy: '害羞',
    excited: '兴奋', angry: '生气', thinking: '思考', surprise: '惊讶'
  };
  return map[e] || e;
}

export default FireflyArtManager;
