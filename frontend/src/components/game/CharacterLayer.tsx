import { Emotion } from '@/lib/simulation';
import { cn } from '@/lib/utils';

interface CharacterLayerProps {
  emotion: Emotion;
  assetUrl?: string;
}

export const CharacterLayer = ({ emotion, assetUrl }: CharacterLayerProps) => {
  // 模拟立绘资源映射
  // 实际开发中，这里应该是真实的图片路径，如 '/assets/firefly_smile.png'
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
    <div className="absolute inset-0 z-10 flex items-end justify-center pointer-events-none">
      {/* 
        立绘容器 
        PC端: 限制最大高度，防止过大
        移动端: 撑满宽度
      */}
      <div className={cn(
        "relative transition-all duration-500 ease-in-out transform",
        "h-[70vh] w-auto aspect-[1/2]", // 模拟立绘比例
        "md:h-[85vh]",
        "flex items-center justify-center"
      )}>
        {assetUrl ? (
          <img src={assetUrl} alt={`Firefly ${emotion}`} className="w-full h-full object-contain" />
        ) : (
          <div className="w-full h-full bg-gradient-to-t from-teal-500/20 to-transparent rounded-t-full flex flex-col items-center justify-center backdrop-blur-[2px] border-x border-t border-white/10">
            <span className="text-9xl filter drop-shadow-lg animate-pulse">
              {getEmojiForEmotion(emotion)}
            </span>
            <span className="mt-8 text-white/50 text-sm font-mono uppercase tracking-widest">
              Firefly_Model_v1.0
            </span>
            <span className="text-white/80 font-bold mt-2">
              [{emotion.toUpperCase()}]
            </span>
          </div>
        )}
      </div>
    </div>
  );
};
