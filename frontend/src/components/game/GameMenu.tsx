import { Map, Backpack, Settings } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';

interface GameMenuProps {
  onOpenMap: () => void;
  onOpenInventory: () => void;
}

export const GameMenu = ({ onOpenMap, onOpenInventory }: GameMenuProps) => {
  return (
    <div className="absolute top-20 right-4 z-30 flex flex-col gap-3">
      <Tooltip>
        <TooltipTrigger asChild>
          <Button 
            variant="secondary" 
            size="icon" 
            className="w-12 h-12 rounded-full bg-black/40 backdrop-blur-md border border-white/20 text-white hover:bg-teal-600 hover:border-teal-400 shadow-lg transition-all"
            onClick={onOpenMap}
          >
            <Map className="w-5 h-5" />
          </Button>
        </TooltipTrigger>
        <TooltipContent side="left">
          <p>导航地图</p>
        </TooltipContent>
      </Tooltip>

      <Tooltip>
        <TooltipTrigger asChild>
          <Button 
            variant="secondary" 
            size="icon" 
            className="w-12 h-12 rounded-full bg-black/40 backdrop-blur-md border border-white/20 text-white hover:bg-teal-600 hover:border-teal-400 shadow-lg transition-all"
            onClick={onOpenInventory}
          >
            <Backpack className="w-5 h-5" />
          </Button>
        </TooltipTrigger>
        <TooltipContent side="left">
          <p>背包与记忆</p>
        </TooltipContent>
      </Tooltip>

      <Tooltip>
        <TooltipTrigger asChild>
          <Button 
            variant="secondary" 
            size="icon" 
            className="w-12 h-12 rounded-full bg-black/40 backdrop-blur-md border border-white/20 text-white hover:bg-teal-600 hover:border-teal-400 shadow-lg transition-all"
          >
            <Settings className="w-5 h-5" />
          </Button>
        </TooltipTrigger>
        <TooltipContent side="left">
          <p>系统设置</p>
        </TooltipContent>
      </Tooltip>
    </div>
  );
};