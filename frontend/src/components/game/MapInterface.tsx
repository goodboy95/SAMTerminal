import { useState } from 'react';
import { Location, StarDomain } from '@/lib/simulation';
import { cn } from '@/lib/utils';
import { MapPin, Lock, Globe, ChevronLeft, Navigation } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface MapInterfaceProps {
  currentLocation: Location;
  domains: Record<string, StarDomain>;
  locations: Record<string, Location>;
  onTravel: (locationId: string) => void;
  onClose: () => void;
}

export const MapInterface = ({ currentLocation, onTravel, onClose, domains: domainsMap, locations: locationsMap }: MapInterfaceProps) => {
  // 状态：当前查看的星域 ID。如果为 null，则显示星轨航图（所有星域）
  const [viewingDomainId, setViewingDomainId] = useState<string | null>(currentLocation.domainId);
  // 状态：当前选中的区域（用于显示详情）
  const [selectedLocation, setSelectedLocation] = useState<Location | null>(null);

  const domains = Object.values(domainsMap);
  const locations = Object.values(locationsMap);

  // 过滤当前星域下的区域
  const currentDomainLocations = viewingDomainId 
    ? locations.filter(loc => loc.domainId === viewingDomainId)
    : [];

  const currentDomain = viewingDomainId ? domainsMap[viewingDomainId] : null;

  const handleTravelClick = () => {
    if (selectedLocation && selectedLocation.isUnlocked) {
      onTravel(selectedLocation.id);
    }
  };

  return (
    <div className="absolute inset-0 z-40 bg-black/90 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="relative w-full max-w-4xl aspect-video bg-slate-950 border border-white/20 rounded-xl overflow-hidden shadow-2xl flex">
        
        {/* 地图主视图 */}
        <div className="relative flex-1 h-full overflow-hidden">
          
          {/* 顶部导航栏 */}
          <div className="absolute top-4 left-4 z-10 flex items-center gap-2">
            {viewingDomainId ? (
              <Button 
                variant="outline" 
                size="sm" 
                onClick={() => {
                  setViewingDomainId(null);
                  setSelectedLocation(null);
                }}
                className="bg-black/50 border-white/20 text-white hover:bg-white/10 backdrop-blur-md"
              >
                <ChevronLeft className="w-4 h-4 mr-1" />
                星轨航图
              </Button>
            ) : (
              <div className="bg-black/50 px-4 py-2 rounded-md border border-white/10 backdrop-blur-md">
                <h2 className="text-sm font-bold text-white tracking-widest flex items-center gap-2">
                  <Globe className="w-4 h-4 text-teal-400" />
                  GALAXY MAP
                </h2>
              </div>
            )}
            
            {currentDomain && (
              <div className="bg-black/50 px-4 py-2 rounded-md border border-white/10 backdrop-blur-md">
                <span className={cn("text-sm font-bold", currentDomain.color)}>
                  {currentDomain.name}
                </span>
              </div>
            )}
          </div>

          {/* 关闭按钮 */}
          <button 
            onClick={onClose}
            className="absolute top-4 right-4 z-10 w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white transition-colors"
          >
            ✕
          </button>

          {/* 背景 */}
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,_var(--tw-gradient-stops))] from-indigo-900/20 via-slate-950 to-slate-950">
            <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/stardust.png')] opacity-40"></div>
            {/* 装饰性网格 */}
            <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.03)_1px,transparent_1px)] bg-[size:40px_40px]"></div>
          </div>

          {/* 层级 1: 星域视图 */}
          {!viewingDomainId && (
            <div className="absolute inset-0">
              {domains.map((domain) => (
                <button
                  key={domain.id}
                  onClick={() => setViewingDomainId(domain.id)}
                  className="absolute transform -translate-x-1/2 -translate-y-1/2 group flex flex-col items-center gap-2 transition-all duration-300 hover:scale-110"
                  style={{ left: `${domain.coordinates.x}%`, top: `${domain.coordinates.y}%` }}
                >
                  <div className={cn(
                    "w-16 h-16 rounded-full border-2 flex items-center justify-center shadow-[0_0_30px_rgba(0,0,0,0.5)] bg-slate-900",
                    domain.color.replace('text-', 'border-')
                  )}>
                    <Globe className={cn("w-8 h-8", domain.color)} />
                  </div>
                  <span className={cn("text-sm font-bold tracking-wider bg-black/60 px-2 py-1 rounded", domain.color)}>
                    {domain.name}
                  </span>
                </button>
              ))}
              {/* 星际连线 */}
              <svg className="absolute inset-0 pointer-events-none opacity-20">
                <path d="M 70% 50% L 30% 30%" stroke="white" strokeWidth="1" strokeDasharray="4 4" />
                <path d="M 70% 50% L 20% 70%" stroke="white" strokeWidth="1" strokeDasharray="4 4" />
                <path d="M 70% 50% L 80% 20%" stroke="white" strokeWidth="1" strokeDasharray="4 4" />
              </svg>
            </div>
          )}

          {/* 层级 2: 区域视图 */}
          {viewingDomainId && (
            <div className="absolute inset-0 animate-in zoom-in-95 duration-300">
              {currentDomainLocations.map((loc) => (
                <button
                  key={loc.id}
                  onClick={() => setSelectedLocation(loc)}
                  className={cn(
                    "absolute transform -translate-x-1/2 -translate-y-1/2 group transition-all duration-300",
                    !loc.isUnlocked && "opacity-50 grayscale"
                  )}
                  style={{ left: `${loc.coordinates.x}%`, top: `${loc.coordinates.y}%` }}
                >
                  <div className={cn(
                    "w-10 h-10 rounded-full border-2 flex items-center justify-center shadow-lg transition-all",
                    selectedLocation?.id === loc.id 
                      ? "bg-white border-teal-500 scale-125 shadow-teal-500/50" 
                      : loc.id === currentLocation.id
                        ? "bg-teal-500 border-white scale-110"
                        : "bg-slate-800 border-slate-600 hover:border-teal-400 hover:scale-110"
                  )}>
                    {loc.isUnlocked ? (
                      <MapPin className={cn("w-5 h-5", selectedLocation?.id === loc.id ? "text-teal-600" : loc.id === currentLocation.id ? "text-white" : "text-slate-400")} />
                    ) : (
                      <Lock className="w-4 h-4 text-slate-500" />
                    )}
                  </div>
                  
                  {/* 当前位置标记 */}
                  {loc.id === currentLocation.id && (
                    <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-teal-500 text-white text-[10px] px-2 py-0.5 rounded-full whitespace-nowrap animate-bounce">
                      当前位置
                    </div>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* 右侧详情面板 (仅在选中区域时显示) */}
        {selectedLocation && (
          <div className="w-80 bg-slate-900 border-l border-white/10 p-6 flex flex-col animate-in slide-in-from-right duration-300">
            <div className="mb-6">
              <h3 className="text-2xl font-bold text-white mb-2">{selectedLocation.name}</h3>
              <div className="flex items-center gap-2 text-xs text-slate-400 mb-4">
                <span className="px-2 py-0.5 bg-white/10 rounded border border-white/10">
                  {domainsMap[selectedLocation.domainId]?.name}
                </span>
                {selectedLocation.isUnlocked ? (
                  <span className="text-green-400 flex items-center gap-1">● 已解锁</span>
                ) : (
                  <span className="text-red-400 flex items-center gap-1">● 未解锁</span>
                )}
              </div>
              <p className="text-sm text-slate-300 leading-relaxed">
                {selectedLocation.description}
              </p>
            </div>

            <div className="mt-auto space-y-3">
              {selectedLocation.id === currentLocation.id ? (
                <Button disabled className="w-full bg-slate-700 text-slate-400">
                  当前所在地
                </Button>
              ) : (
                <Button 
                  onClick={handleTravelClick}
                  disabled={!selectedLocation.isUnlocked}
                  className={cn(
                    "w-full",
                    selectedLocation.isUnlocked 
                      ? "bg-teal-600 hover:bg-teal-700 text-white" 
                      : "bg-slate-800 text-slate-500 cursor-not-allowed"
                  )}
                >
                  {selectedLocation.isUnlocked ? (
                    <>
                      <Navigation className="w-4 h-4 mr-2" />
                      开始跃迁
                    </>
                  ) : (
                    <>
                      <Lock className="w-4 h-4 mr-2" />
                      暂未开放
                    </>
                  )}
                </Button>
              )}
            </div>
          </div>
        )}

      </div>
    </div>
  );
};
