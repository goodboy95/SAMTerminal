import { v4 as uuidv4 } from 'uuid';

// --- 类型定义 ---

export type Emotion = 'normal' | 'smile' | 'sad' | 'shy' | 'excited' | 'angry' | 'thinking' | 'surprise';

export interface Location {
  id: string;
  name: string;
  description: string; // 地点的基础固定描述
  backgroundStyle: string;
  backgroundUrl?: string;
  coordinates: { x: number; y: number }; // 区域地图上的坐标
  isUnlocked: boolean;
  domainId: string; // 所属星域 ID
}

export interface StarDomain {
  id: string;
  name: string;
  description: string;
  coordinates: { x: number; y: number }; // 星轨航图上的坐标
  color: string;
}

export interface Item {
  id: string;
  name: string;
  description: string;
  icon: string;
  quantity: number;
}

export interface Memory {
  id: string;
  title: string;
  content: string;
  date: string;
  tags: string[];
}

export interface FireflyAsset {
  emotion: string;
  url: string;
}

export interface Message {
  id: string;
  sender: 'user' | 'firefly' | 'npc' | 'system';
  npcName?: string;
  content: string;
  narration?: string;
  timestamp: Date;
}

export interface GameState {
  currentLocation: Location;
  locationDynamicState: string; // 新增：地点当前的动态状态（如：正在下雨，或者很吵闹）
  
  fireflyEmotion: Emotion; // 表情资源 Key
  fireflyStatus: string;   // 新增：流萤当前的客观状态（如：正在散步、正在思考）
  fireflyMoodDetails: string; // 新增：流萤当前的主观心情文本
  
  gameTime: string;
  items: Item[];
  memories: Memory[];
  userName: string;
}

// --- 模拟数据 ---

export const STAR_DOMAINS: Record<string, StarDomain> = {
  'penacony': {
    id: 'penacony',
    name: '匹诺康尼',
    description: '盛会之星，美梦的国度。',
    coordinates: { x: 70, y: 50 },
    color: 'text-purple-400',
  },
  'jarilo': {
    id: 'jarilo',
    name: '雅利洛-VI',
    description: '冰雪覆盖的星球，存护的领地。',
    coordinates: { x: 30, y: 30 },
    color: 'text-blue-400',
  },
  'herta': {
    id: 'herta',
    name: '黑塔空间站',
    description: '天才俱乐部黑塔女士的私人财产。',
    coordinates: { x: 20, y: 70 },
    color: 'text-indigo-400',
  },
  'luofu': {
    id: 'luofu',
    name: '仙舟「罗浮」',
    description: '巡猎的巨舰，云骑军的驻地。',
    coordinates: { x: 80, y: 20 },
    color: 'text-teal-400',
  },
};

export const LOCATIONS: Record<string, Location> = {
  // 匹诺康尼
  'golden-hour': {
    id: 'golden-hour',
    name: '黄金的时刻',
    description: '永远停留在午夜之前的繁华都市，霓虹灯闪烁，是匹诺康尼最热闹的梦境区域。',
    backgroundStyle: 'bg-gradient-to-br from-yellow-600 via-orange-500 to-red-500',
    coordinates: { x: 50, y: 50 },
    isUnlocked: true,
    domainId: 'penacony',
  },
  'dream-edge': {
    id: 'dream-edge',
    name: '筑梦边境',
    description: '梦境与现实交汇的边缘，可以看到巨大的都市倒影，正在建设中的梦境荒野。',
    backgroundStyle: 'bg-gradient-to-b from-indigo-900 to-purple-800',
    coordinates: { x: 80, y: 30 },
    isUnlocked: true,
    domainId: 'penacony',
  },
  'firefly-secret': {
    id: 'firefly-secret',
    name: '流梦礁·秘密基地',
    description: '只有流萤知道的安静角落，可以看到蓝色的忆质海洋，远离了喧嚣。',
    backgroundStyle: 'bg-gradient-to-t from-blue-900 to-slate-800',
    coordinates: { x: 20, y: 70 },
    isUnlocked: true,
    domainId: 'penacony',
  },
  'hotel-lobby': {
    id: 'hotel-lobby',
    name: '白日梦酒店',
    description: '现实中的酒店大堂，金碧辉煌，是入梦前的必经之地。',
    backgroundStyle: 'bg-gradient-to-r from-slate-900 to-slate-700',
    coordinates: { x: 30, y: 20 },
    isUnlocked: false,
    domainId: 'penacony',
  },
  // 雅利洛-VI (示例)
  'admin-district': {
    id: 'admin-district',
    name: '行政区',
    description: '贝洛伯格的上层区，永冬之城的中心，巨大的齿轮雕塑矗立在广场中央。',
    backgroundStyle: 'bg-gradient-to-b from-slate-200 to-slate-400',
    coordinates: { x: 50, y: 50 },
    isUnlocked: true,
    domainId: 'jarilo',
  },
};

const INITIAL_ITEMS: Item[] = [
  { id: '1', name: '橡木蛋糕卷', description: '木头做的？不，是橡木家系的特产。', icon: '🍰', quantity: 2 },
  { id: '2', name: '信用点', description: '通用的货币。', icon: '💰', quantity: 20000 },
];

const INITIAL_MEMORIES: Memory[] = [
  { id: 'm1', title: '天台的约定', content: '在黄金的时刻边缘，流萤向你展示了她的秘密基地，并约定下次再见。', date: '2024-02-06', tags: ['重要', '流萤'] },
  { id: 'm2', title: '花火的恶作剧', content: '那个戴面具的愚者似乎对你们很有兴趣...', date: '2024-02-07', tags: ['NPC', '花火'] },
];

export const INITIAL_STATE: GameState = {
  currentLocation: LOCATIONS['golden-hour'],
  locationDynamicState: '街道上人来人往，苏乐达的广告牌正在播放欢快的音乐。',
  
  fireflyEmotion: 'smile',
  fireflyStatus: '正在享受逛街',
  fireflyMoodDetails: '虽然这里很吵闹，但只要和你在一起，就觉得很安心。',
  
  gameTime: '21:45',
  items: INITIAL_ITEMS,
  memories: INITIAL_MEMORIES,
  userName: '开拓者',
};

// --- 模拟 Agent 逻辑 ---

export const mockAgentResponse = async (
  userContent: string,
  currentState: GameState
): Promise<{
  messages: Message[];
  newState: Partial<GameState>;
}> => {
  await new Promise((resolve) => setTimeout(resolve, 800 + Math.random() * 800));

  const lowerContent = userContent.toLowerCase();
  const responseMessages: Message[] = [];
  let newState: Partial<GameState> = {};

  // 1. 意图识别：移动 (Travel)
  if (lowerContent.includes('travel_to:')) {
    const targetId = lowerContent.split(':')[1];
    const targetLoc = LOCATIONS[targetId];

    if (targetLoc) {
      if (!targetLoc.isUnlocked) {
         responseMessages.push({
          id: uuidv4(),
          sender: 'firefly',
          content: '那里现在好像还去不了呢...',
          timestamp: new Date(),
        });
        newState = { fireflyEmotion: 'thinking' };
      } else {
        // 根据地点设置不同的状态文本
        let newStatus = '正在探索';
        let newMood = '对新的景色充满好奇。';
        let newLocState = '这里的一切看起来都很新鲜。';

        if (targetLoc.id === 'firefly-secret') {
            newStatus = '放松身心';
            newMood = '这里是我的秘密基地，希望能让你也感到放松。';
            newLocState = '微风吹过，忆质的波浪轻轻拍打着岸边。';
        } else if (targetLoc.id === 'dream-edge') {
            newStatus = '警惕观察';
            newMood = '这里的氛围有点压抑，我们要小心一点。';
            newLocState = '远处的建筑还在不断重组，空气中弥漫着不稳定的气息。';
        }

        newState = {
          currentLocation: targetLoc,
          locationDynamicState: newLocState,
          fireflyStatus: newStatus,
          fireflyMoodDetails: newMood,
          fireflyEmotion: targetLoc.id === 'firefly-secret' ? 'shy' : 'smile',
        };
        responseMessages.push({
          id: uuidv4(),
          sender: 'firefly',
          content: `好呀，我们去${targetLoc.name}吧！`,
          narration: `*流萤拉起你的手，向${targetLoc.name}跑去*`,
          timestamp: new Date(),
        });
      }
    }
  } 
  // 2. 意图识别：自然语言移动
  else if (lowerContent.includes('去') || lowerContent.includes('走')) {
     if (lowerContent.includes('筑梦边境')) {
        const targetLoc = LOCATIONS['dream-edge'];
        newState = { 
            currentLocation: targetLoc, 
            fireflyEmotion: 'thinking',
            locationDynamicState: '远处的建筑还在不断重组，空气中弥漫着不稳定的气息。',
            fireflyStatus: '警惕观察',
            fireflyMoodDetails: '这里的氛围有点压抑，我们要小心一点。'
        };
        responseMessages.push({ id: uuidv4(), sender: 'firefly', content: '嗯，去筑梦边境看看吧。', timestamp: new Date() });
     } else {
        responseMessages.push({ id: uuidv4(), sender: 'firefly', content: '我们要去哪里呢？', timestamp: new Date() });
     }
  }
  // 3. 默认回复
  else {
    const randomReplies = [
      { 
          text: '只要和你在一起，时间就过得好快。', 
          emotion: 'shy', 
          narration: '*流萤低头看着脚尖*',
          status: '害羞',
          mood: '心跳好像变快了一点...'
      },
      { 
          text: '你看那边的广告牌，好像被花火改过了...', 
          emotion: 'thinking', 
          narration: '*流萤指着远处的霓虹灯*',
          status: '观察环境',
          mood: '那个广告牌的内容是不是有点奇怪？'
      },
      { 
          text: '下次我们叫上星穹列车的大家一起来吧？', 
          emotion: 'smile', 
          narration: '*流萤充满期待地看着你*',
          status: '充满期待',
          mood: '大家在一起一定会更热闹的。'
      },
    ];
    const reply = randomReplies[Math.floor(Math.random() * randomReplies.length)];
    
    newState = { 
        fireflyEmotion: reply.emotion as Emotion,
        fireflyStatus: reply.status,
        fireflyMoodDetails: reply.mood
    };
    responseMessages.push({
      id: uuidv4(),
      sender: 'firefly',
      content: reply.text,
      narration: reply.narration,
      timestamp: new Date(),
    });
  }

  return { messages: responseMessages, newState };
};
