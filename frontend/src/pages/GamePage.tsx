import { useState, useEffect } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { StatusBar } from '@/components/game/StatusBar';
import { CharacterLayer } from '@/components/game/CharacterLayer';
import { ChatInterface } from '@/components/game/ChatInterface';
import { MapInterface } from '@/components/game/MapInterface';
import { InventoryInterface } from '@/components/game/InventoryInterface';
import { GameMenu } from '@/components/game/GameMenu';
import { GameState, Message, Location, StarDomain, INITIAL_STATE } from '@/lib/simulation';
import { api, API_BASE } from '@/lib/api';
import { cn } from '@/lib/utils';
import { preloadImage } from '@/lib/imageCache';

const GamePage = () => {
  const [gameState, setGameState] = useState<GameState>(INITIAL_STATE);
  const [messages, setMessages] = useState<Message[]>([]);
  const [isTyping, setIsTyping] = useState(false);
  const [domains, setDomains] = useState<Record<string, StarDomain>>({});
  const [locations, setLocations] = useState<Record<string, Location>>(INITIAL_STATE ? { [INITIAL_STATE.currentLocation.id]: INITIAL_STATE.currentLocation } : {});
  const [fireflyAssets, setFireflyAssets] = useState<Record<string, string>>({});
  const [sessionId, setSessionId] = useState<string | null>(() => localStorage.getItem('sam_session_id'));
  
  // UI 状态
  const [showMap, setShowMap] = useState(false);
  const [showInventory, setShowInventory] = useState(false);

  // 初始化欢迎语
  useEffect(() => {
    setMessages([
      {
        id: uuidv4(),
        sender: 'firefly',
        content: '开拓者，你来了！今天想去哪里逛逛吗？',
        narration: '*流萤微笑着向你挥手*',
        timestamp: new Date(),
      }
    ]);
  }, []);

  // 初始化地图和状态
  useEffect(() => {
    const token = localStorage.getItem('sam_token') || undefined;
    api.map(token).then(({ domains, locations }) => {
      setDomains(domains);
      setLocations(locations);
    }).catch(console.error);

    api.fireflyAssets()
      .then((assets) => {
        const mapped: Record<string, string> = {};
        assets.forEach((asset) => {
          mapped[asset.emotion] = asset.url;
        });
        setFireflyAssets(mapped);
      })
      .catch(console.error);

    api.status(token)
      .then((state) => {
        setGameState(applyLocation(state));
      })
      .catch(console.error);
  }, []);

  useEffect(() => {
    const token = localStorage.getItem('sam_token') || undefined;
    if (!sessionId && token) {
      api.createSession(token)
        .then((data) => {
          if (data?.sessionId) {
            localStorage.setItem('sam_session_id', data.sessionId);
            setSessionId(data.sessionId);
          }
        })
        .catch(console.error);
    }
  }, [sessionId]);

  useEffect(() => {
    setGameState(prev => applyLocation(prev));
  }, [locations]);

  useEffect(() => {
    Object.values(locations).forEach((loc) => {
      if (loc.backgroundUrl) {
        preloadImage(loc.backgroundUrl).catch(() => undefined);
      }
    });
  }, [locations]);

  useEffect(() => {
    Object.values(fireflyAssets).forEach((url) => {
      preloadImage(url).catch(() => undefined);
    });
  }, [fireflyAssets]);

  const applyLocation = (state: GameState) => {
    const loc = locations[state.currentLocation.id];
    if (loc) {
      return {
        ...state,
        currentLocation: { ...loc },
        locationDynamicState: state.locationDynamicState || loc.description
      };
    }
    return state;
  };

  const handleSendMessage = async (content: string) => {
    const userMsg: Message = {
      id: uuidv4(),
      sender: 'user',
      content,
      timestamp: new Date(),
    };
    setMessages(prev => [...prev, userMsg]);
    setIsTyping(true);

    try {
      const token = localStorage.getItem('sam_token') || undefined;
      const { replies, state, stateUpdate, sessionId: newSessionId } = await api.chat(content, token, sessionId || undefined);
      if (newSessionId && newSessionId !== sessionId) {
        localStorage.setItem('sam_session_id', newSessionId);
        setSessionId(newSessionId);
      }
      if (stateUpdate?.location?.id && stateUpdate.location.backgroundUrl) {
        const normalized = stateUpdate.location.backgroundUrl.startsWith('http')
          ? stateUpdate.location.backgroundUrl
          : `${API_BASE}${stateUpdate.location.backgroundUrl}`;
        setLocations(prev => ({
          ...prev,
          [stateUpdate.location.id]: {
            ...(prev[stateUpdate.location.id] || state.currentLocation),
            backgroundUrl: normalized
          }
        }));
      }
      setGameState(applyLocation(state));
      setMessages(prev => [...prev, ...replies]);
    } catch (error) {
      console.error('Failed to get response', error);
    } finally {
      setIsTyping(false);
    }
  };

  const handleRecall = async (memoryId: string) => {
    setIsTyping(true);
    try {
      const token = localStorage.getItem('sam_token') || undefined;
      const data = await api.recallMemory(memoryId, token, sessionId || undefined);
      if (data?.sessionId && data.sessionId !== sessionId) {
        localStorage.setItem('sam_session_id', data.sessionId);
        setSessionId(data.sessionId);
      }
      const replies: Message[] = (data.messages || []).map((m: any) => ({
        id: m.id,
        sender: m.sender,
        npcName: m.npcName,
        content: m.content,
        narration: m.narration,
        timestamp: new Date(m.timestamp)
      }));
      setMessages(prev => [...prev, ...replies]);
      if (data.state) {
        setGameState(applyLocation(data.state));
      }
    } catch (error) {
      console.error('Failed to recall memory', error);
    } finally {
      setIsTyping(false);
    }
  };

  // 处理地图移动
  const handleTravel = (locationId: string) => {
    setShowMap(false);
    // 模拟发送指令，触发 Agent 的移动逻辑
    handleSendMessage(`travel_to:${locationId}`);
  };

  return (
    <div className="relative w-full h-screen overflow-hidden bg-black text-white font-sans">
      
      {/* Layer 0: 背景层 */}
      <div className={cn(
        "absolute inset-0 z-0 transition-colors duration-1000 ease-in-out",
        gameState.currentLocation.backgroundStyle
      )} style={gameState.currentLocation.backgroundUrl ? { backgroundImage: `url(${gameState.currentLocation.backgroundUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : undefined}>
        {/* 模拟背景纹理 */}
        <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/stardust.png')] opacity-30 mix-blend-overlay"></div>
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-black/20"></div>
      </div>

      {/* Layer 1: 角色层 */}
      <CharacterLayer emotion={gameState.fireflyEmotion} assetUrl={fireflyAssets[gameState.fireflyEmotion]} />

      {/* Layer 2: UI 层 */}
      <div className="relative z-20 w-full h-full flex flex-col md:flex-row pointer-events-none">
        
        {/* 顶部状态栏 (pointer-events-auto 在组件内部处理或这里加) */}
        <div className="pointer-events-auto">
          <StatusBar state={gameState} />
        </div>

        {/* 右上角菜单 */}
        <div className="pointer-events-auto">
          <GameMenu 
            onOpenMap={() => setShowMap(true)} 
            onOpenInventory={() => setShowInventory(true)} 
          />
        </div>

        {/* 左侧/中间留空给立绘 (PC端) */}
        <div className="flex-1 hidden md:block" />

        {/* 右侧聊天栏 (PC端) / 底部聊天栏 (移动端) */}
        <div className="w-full md:w-[400px] lg:w-[450px] h-[40vh] md:h-full mt-auto md:mt-0 pointer-events-auto">
          <ChatInterface 
            messages={messages} 
            onSendMessage={handleSendMessage}
            isTyping={isTyping}
          />
        </div>
      </div>

      {/* Layer 3: 模态框 Overlay */}
      {showMap && (
        <MapInterface 
          currentLocation={gameState.currentLocation} 
          onTravel={handleTravel}
          onClose={() => setShowMap(false)}
          domains={domains}
          locations={locations}
        />
      )}

      {showInventory && (
        <InventoryInterface 
          state={gameState} 
          onClose={() => setShowInventory(false)}
          onRecall={handleRecall}
        />
      )}

    </div>
  );
};

export default GamePage;
