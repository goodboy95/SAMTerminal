import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Activity, Save, Search, ArrowUpDown, Settings2 } from 'lucide-react';
import { toast } from 'sonner';
import { api } from '@/lib/api';

interface UserUsage {
  id: number;
  username: string;
  inputTokens: number;
  outputTokens: number;
  customLimit: number | null;
}

const UserMonitor = () => {
  const token = localStorage.getItem('sam_token') || undefined;
  const [globalLimit, setGlobalLimit] = useState(50000);
  const [users, setUsers] = useState<UserUsage[]>([]);
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [searchTerm, setSearchTerm] = useState('');
  const [editingUser, setEditingUser] = useState<{id: number; name: string; limit: number | string} | null>(null);

  const loadUsage = async () => {
    try {
      const data = await api.adminUsage(token);
      setGlobalLimit(data.globalLimit ?? 50000);
      setUsers(data.users || []);
    } catch {
      toast.error('加载用量失败');
    }
  };

  useEffect(() => {
    loadUsage();
  }, []);

  const calculateWeightedTokens = (input: number, output: number) => {
    return input * 1 + output * 8;
  };

  const sortedUsers = [...users]
    .filter(u => u.username.toLowerCase().includes(searchTerm.toLowerCase()))
    .sort((a, b) => {
      const totalA = calculateWeightedTokens(a.inputTokens, a.outputTokens);
      const totalB = calculateWeightedTokens(b.inputTokens, b.outputTokens);
      return sortOrder === 'desc' ? totalB - totalA : totalA - totalB;
    });

  const handleSaveGlobalLimit = async () => {
    try {
      await api.adminUpdateGlobalLimit(globalLimit, token);
      toast.success(`全局 Token 上限已更新为: ${globalLimit}`);
      loadUsage();
    } catch {
      toast.error('保存失败');
    }
  };

  const handleSaveIndividualLimit = async () => {
    if (!editingUser) return;
    const newLimit = editingUser.limit === '' ? null : Number(editingUser.limit);
    try {
      await api.adminUpdateUserLimit(editingUser.id, newLimit, token);
      toast.success(`用户 ${editingUser.name} 的上限已更新`);
      setEditingUser(null);
      loadUsage();
    } catch {
      toast.error('保存失败');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-2xl font-bold text-white">用户 Token 监测</h3>
          <p className="text-slate-400">监控用户消耗，设置全局或单独的 Token 使用上限。</p>
        </div>
      </div>

      <Card className="bg-slate-900 border-white/10">
        <CardHeader>
          <CardTitle className="text-white flex items-center gap-2">
            <Settings2 className="w-5 h-5 text-teal-400" />
            全局限制设置
          </CardTitle>
          <CardDescription>设置所有用户的默认 Token 消耗上限 (加权后)。</CardDescription>
        </CardHeader>
        <CardContent className="flex items-end gap-4">
          <div className="space-y-2 flex-1 max-w-xs">
            <Label className="text-slate-300">默认上限 (Weighted Tokens)</Label>
            <Input 
              type="number" 
              value={globalLimit}
              onChange={(e) => setGlobalLimit(Number(e.target.value))}
              className="bg-black/20 border-white/10 text-white"
            />
          </div>
          <Button onClick={handleSaveGlobalLimit} className="bg-teal-600 hover:bg-teal-700">
            <Save className="w-4 h-4 mr-2" /> 保存全局设置
          </Button>
        </CardContent>
      </Card>

      <Card className="bg-slate-900 border-white/10">
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-white flex items-center gap-2">
            <Activity className="w-5 h-5 text-teal-400" />
            实时消耗监控
          </CardTitle>
          <div className="flex items-center gap-2">
            <div className="relative">
              <Search className="absolute left-2 top-2.5 h-4 w-4 text-slate-500" />
              <Input
                placeholder="搜索用户..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-8 w-[200px] bg-black/20 border-white/10 text-white"
              />
            </div>
            <Button 
              variant="outline" 
              size="icon"
              onClick={() => setSortOrder(prev => prev === 'desc' ? 'asc' : 'desc')}
              className="border-white/10 text-slate-400 hover:text-white hover:bg-white/5"
            >
              <ArrowUpDown className="w-4 h-4" />
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow className="border-white/10 hover:bg-transparent">
                <TableHead className="text-slate-400">用户</TableHead>
                <TableHead className="text-slate-400">Input (1x)</TableHead>
                <TableHead className="text-slate-400">Output (8x)</TableHead>
                <TableHead className="text-teal-400 font-bold">加权总消耗</TableHead>
                <TableHead className="text-slate-400">当前上限</TableHead>
                <TableHead className="text-slate-400 text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {sortedUsers.map((user) => {
                const total = calculateWeightedTokens(user.inputTokens, user.outputTokens);
                const currentLimit = user.customLimit ?? globalLimit;
                const usagePercent = currentLimit > 0 ? (total / currentLimit) * 100 : 0;
                
                return (
                  <TableRow key={user.id} className="border-white/10 hover:bg-white/5">
                    <TableCell className="font-medium text-white">{user.username}</TableCell>
                    <TableCell className="text-slate-300">{user.inputTokens}</TableCell>
                    <TableCell className="text-slate-300">{user.outputTokens}</TableCell>
                    <TableCell>
                      <div className="flex flex-col gap-1">
                        <span className="text-teal-400 font-mono font-bold">{total}</span>
                        <div className="w-24 h-1.5 bg-slate-800 rounded-full overflow-hidden">
                          <div 
                            className={`h-full ${usagePercent > 90 ? 'bg-red-500' : 'bg-teal-500'}`} 
                            style={{ width: `${Math.min(usagePercent, 100)}%` }}
                          />
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      {user.customLimit ? (
                        <Badge variant="outline" className="border-yellow-500/50 text-yellow-400">
                          定制: {user.customLimit}
                        </Badge>
                      ) : (
                        <span className="text-slate-500 text-sm">默认: {globalLimit}</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <Dialog>
                        <DialogTrigger asChild>
                          <Button 
                            variant="ghost" 
                            size="sm"
                            onClick={() => setEditingUser({ 
                              id: user.id, 
                              name: user.username, 
                              limit: user.customLimit ?? '' 
                            })}
                            className="text-teal-400 hover:text-teal-300 hover:bg-teal-400/10"
                          >
                            设置上限
                          </Button>
                        </DialogTrigger>
                        <DialogContent className="bg-slate-900 border-white/10 text-white">
                          <DialogHeader>
                            <DialogTitle>设置用户上限 - {editingUser?.name}</DialogTitle>
                          </DialogHeader>
                          <div className="py-4">
                            <Label className="text-slate-300 mb-2 block">自定义 Token 上限 (留空则使用全局默认)</Label>
                            <Input 
                              type="number" 
                              placeholder={`当前全局默认: ${globalLimit}`}
                              value={editingUser?.limit}
                              onChange={(e) => setEditingUser(prev => prev ? ({ ...prev, limit: e.target.value }) : null)}
                              className="bg-black/20 border-white/10 text-white"
                            />
                          </div>
                          <DialogFooter>
                            <Button onClick={handleSaveIndividualLimit} className="bg-teal-600 hover:bg-teal-700">
                              保存更改
                            </Button>
                          </DialogFooter>
                        </DialogContent>
                      </Dialog>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
};

export default UserMonitor;
