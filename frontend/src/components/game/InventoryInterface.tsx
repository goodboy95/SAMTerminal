import { GameState } from '@/lib/simulation';
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import { Package, BrainCircuit, Calendar } from 'lucide-react';

interface InventoryInterfaceProps {
  state: GameState;
  onClose: () => void;
  onRecall?: (memoryId: string) => void;
}

export const InventoryInterface = ({ state, onClose, onRecall }: InventoryInterfaceProps) => {
  return (
    <div className="absolute inset-0 z-40 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl h-[600px] bg-slate-950 border border-white/20 rounded-xl overflow-hidden shadow-2xl flex flex-col">
        
        {/* 头部 */}
        <div className="flex items-center justify-between p-4 border-b border-white/10 bg-slate-900/50">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Package className="w-5 h-5 text-teal-400" />
            开拓者终端
          </h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white">✕</button>
        </div>

        {/* 内容区 */}
        <div className="flex-1 p-4 overflow-hidden">
          <Tabs defaultValue="items" className="h-full flex flex-col">
            <TabsList className="grid w-full grid-cols-2 bg-slate-900">
              <TabsTrigger value="items">背包物品</TabsTrigger>
              <TabsTrigger value="memories">记忆回廊</TabsTrigger>
            </TabsList>

            {/* 物品 Tab */}
            <TabsContent value="items" className="flex-1 mt-4">
              <ScrollArea className="h-full pr-4">
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                  {(state.items || []).map((item) => (
                    <div key={item.id} className="bg-slate-900 border border-white/10 rounded-lg p-4 flex flex-col items-center gap-2 hover:border-teal-500/50 transition-colors group">
                      <div className="text-4xl mb-2 group-hover:scale-110 transition-transform">{item.icon}</div>
                      <div className="font-medium text-white">{item.name}</div>
                      <div className="text-xs text-slate-400 text-center line-clamp-2">{item.description}</div>
                      <Badge variant="secondary" className="mt-auto bg-slate-800 text-teal-400">x{item.quantity}</Badge>
                    </div>
                  ))}
                </div>
              </ScrollArea>
            </TabsContent>

            {/* 记忆 Tab */}
            <TabsContent value="memories" className="flex-1 mt-4">
              <ScrollArea className="h-full pr-4">
                <div className="space-y-4">
                  {(state.memories || []).map((mem) => (
                    <button
                      key={mem.id}
                      onClick={() => onRecall?.(mem.id)}
                      className="text-left w-full bg-slate-900/50 border border-white/10 rounded-lg p-4 hover:bg-slate-900 transition-colors"
                    >
                      <div className="flex items-start justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <BrainCircuit className="w-4 h-4 text-pink-400" />
                          <h3 className="font-bold text-white">{mem.title}</h3>
                        </div>
                        <div className="flex items-center gap-1 text-xs text-slate-500">
                          <Calendar className="w-3 h-3" />
                          {mem.date}
                        </div>
                      </div>
                      <p className="text-sm text-slate-300 leading-relaxed mb-3">
                        {mem.content}
                      </p>
                      <div className="flex gap-2">
                        {(mem.tags || []).map(tag => (
                          <Badge key={tag} variant="outline" className="text-xs border-white/10 text-slate-400">
                            #{tag}
                          </Badge>
                        ))}
                      </div>
                    </button>
                  ))}
                </div>
              </ScrollArea>
            </TabsContent>
          </Tabs>
        </div>

      </div>
    </div>
  );
};
