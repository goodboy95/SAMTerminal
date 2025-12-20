import { MapPin, Clock, Heart, Info } from 'lucide-react';
import { GameState, Emotion } from '@/lib/simulation';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { Separator } from '@/components/ui/separator';

interface StatusBarProps {
  state: GameState;
}

export const StatusBar = ({ state }: StatusBarProps) => {
  
  const getEmojiForEmotion = (e: Emotion) => {
    switch (e) {
      case 'smile': return '😊';
      case 'sad': return '😢';
      case 'shy': return '😳';
      case 'excited': return '✨';
      case 'angry': return '😠';
      case 'thinking': return '🤔';
      case 'surprise': return '😮';
      default: return '😐';
    }
  };

  return (
    <div className="fixed top-0 left-0 right-0 z-50 p-4">
      <div className="mx-auto max-w-4xl bg-black/40 backdrop-blur-md border border-white/10 rounded-full px-6 py-2 flex items-center justify-between text-white shadow-lg">
        
        {/* 地点 (Tooltip: 基础信息 + 动态状态) */}
        <Tooltip>
          <TooltipTrigger asChild>
            <div className="flex items-center gap-2 cursor-help hover:bg-white/5 px-2 py-1 rounded-full transition-colors">
              <MapPin className="w-4 h-4 text-yellow-400" />
              <span className="text-sm font-medium tracking-wide">{state.currentLocation.name}</span>
            </div>
          </TooltipTrigger>
          <TooltipContent className="bg-slate-900/95 border-white/20 text-white max-w-xs p-4" sideOffset={5}>
            <div className="space-y-2">
              <div>
                <h4 className="font-bold text-yellow-400 flex items-center gap-2">
                  {state.currentLocation.name}
                </h4>
                <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                  {state.currentLocation.description}
                </p>
              </div>
              <Separator className="bg-white/10" />
              <div>
                <span className="text-xs font-bold text-teal-400 block mb-1">当前环境</span>
                <p className="text-xs text-slate-200 leading-relaxed">
                  {state.locationDynamicState}
                </p>
              </div>
            </div>
          </TooltipContent>
        </Tooltip>

        {/* 心情 (Emoji + Tooltip: 状态 + 心情) */}
        <Tooltip>
          <TooltipTrigger asChild>
            <div className="flex items-center gap-2 cursor-help hover:bg-white/5 px-2 py-1 rounded-full transition-colors">
              <Heart className="w-4 h-4 text-pink-400 fill-pink-400/20" />
              <span className="text-lg leading-none filter drop-shadow-sm">
                {getEmojiForEmotion(state.fireflyEmotion)}
              </span>
            </div>
          </TooltipTrigger>
          <TooltipContent className="bg-slate-900/95 border-white/20 text-white max-w-xs p-4" sideOffset={5}>
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <span className="text-2xl">{getEmojiForEmotion(state.fireflyEmotion)}</span>
                <div>
                  <span className="text-xs text-slate-400 uppercase tracking-wider">Status</span>
                  <p className="font-bold text-pink-300">{state.fireflyStatus}</p>
                </div>
              </div>
              <Separator className="bg-white/10" />
              <div>
                <span className="text-xs text-slate-400 uppercase tracking-wider">Mood</span>
                <p className="text-sm text-slate-200 mt-1 italic">
                  "{state.fireflyMoodDetails}"
                </p>
              </div>
            </div>
          </TooltipContent>
        </Tooltip>

        {/* 时间 */}
        <div className="flex items-center gap-2 px-2 py-1">
          <Clock className="w-4 h-4 text-blue-300" />
          <span className="text-sm font-mono">{state.gameTime}</span>
        </div>
        
      </div>
    </div>
  );
};