import { GameState, Item, Memory, Location, StarDomain, Message, FireflyAsset } from './simulation';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://samproject.seekerhut.com:8081';

const jsonHeaders = (token?: string) => {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
};

const normalizeUrl = (url?: string) => {
  if (!url) return url;
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  if (url.startsWith('/')) return `${API_BASE}${url}`;
  return url;
};

export interface AuthResult {
  token: string;
  username: string;
  role: string;
}

export const api = {
  async login(username: string, password: string): Promise<AuthResult> {
    const res = await fetch(`${API_BASE}/api/auth/login`, {
      method: 'POST',
      headers: jsonHeaders(),
      body: JSON.stringify({ username, password })
    });
    if (!res.ok) throw new Error('登录失败');
    return res.json();
  },
  async adminLogin(username: string, password: string): Promise<AuthResult> {
    const res = await fetch(`${API_BASE}/api/admin/login`, {
      method: 'POST',
      headers: jsonHeaders(),
      body: JSON.stringify({ username, password })
    });
    if (!res.ok) throw new Error('登录失败');
    return res.json();
  },
  async register(username: string, email: string, password: string): Promise<AuthResult> {
    const res = await fetch(`${API_BASE}/api/auth/register`, {
      method: 'POST',
      headers: jsonHeaders(),
      body: JSON.stringify({ username, email, password })
    });
    if (!res.ok) throw new Error('注册失败');
    return res.json();
  },
  async status(token?: string): Promise<GameState> {
    const res = await fetch(`${API_BASE}/api/game/status`, { headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('获取状态失败');
    const data = await res.json();
    return mapState(data);
  },
  async chat(content: string, token?: string, sessionId?: string): Promise<{ replies: Message[]; state: GameState; stateUpdate?: any; sessionId?: string }> {
    const res = await fetch(`${API_BASE}/api/game/chat`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify({ message: content, sessionId })
    });
    if (!res.ok) throw new Error('发送失败');
    const data = await res.json();
    const replies: Message[] = (data.messages || []).map((m: any) => ({
      id: m.id,
      sender: m.sender,
      npcName: m.npcName,
      content: m.content,
      narration: m.narration,
      timestamp: new Date(m.timestamp)
    }));
    return { replies, state: mapState(data.state), stateUpdate: data.stateUpdate, sessionId: data.sessionId };
  },
  async createSession(token?: string) {
    const res = await fetch(`${API_BASE}/api/game/session`, {
      method: 'POST',
      headers: jsonHeaders(token)
    });
    if (!res.ok) throw new Error('创建会话失败');
    return res.json();
  },
  async map(token?: string): Promise<{ domains: Record<string, StarDomain>; locations: Record<string, Location> }> {
    const res = await fetch(`${API_BASE}/api/world/map`, { headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('获取地图失败');
    const data = await res.json();
    const domains: Record<string, StarDomain> = {};
    data.domains.forEach((d: any) => {
      domains[d.id] = { id: d.id, name: d.name, description: d.description, coordinates: { x: d.x, y: d.y }, color: d.color };
    });
    const locations: Record<string, Location> = {};
    data.locations.forEach((l: any) => {
      locations[l.id] = {
        id: l.id,
        name: l.name,
        description: l.description,
        backgroundStyle: l.backgroundStyle,
        backgroundUrl: normalizeUrl(l.backgroundUrl),
        coordinates: { x: l.x, y: l.y },
        isUnlocked: l.unlocked,
        domainId: l.domainId
      };
    });
    return { domains, locations };
  },
  async inventory(token?: string): Promise<Item[]> {
    const res = await fetch(`${API_BASE}/api/player/inventory`, { headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('获取背包失败');
    return res.json();
  },
  async memories(token?: string): Promise<Memory[]> {
    const res = await fetch(`${API_BASE}/api/player/memories`, { headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('获取记忆失败');
    return res.json();
  },
  async adminAssets(token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/assets/firefly`, { headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('获取立绘失败');
    const data = await res.json();
    return (data || []).map((item: any) => ({
      ...item,
      url: normalizeUrl(item.url)
    }));
  },
  async saveAdminAssets(assets: Record<string, string>, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/assets/firefly`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify({ assets })
    });
    if (!res.ok) throw new Error('保存失败');
    return res.json();
  },
  async adminDomains(token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/domains`, { headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('获取星域失败');
    return res.json();
  },
  async adminSaveDomain(domain: any, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/domains`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify(domain)
    });
    if (!res.ok) throw new Error('保存星域失败');
    return res.json();
  },
  async adminDeleteDomain(id: number, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/domains/${id}`, {
      method: 'DELETE',
      headers: jsonHeaders(token)
    });
    if (!res.ok) throw new Error('删除星域失败');
    return res.json();
  },
  async adminBatchDomains(payload: any[], token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/domains/batch`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error('批量导入失败');
    return res.json();
  },
  async adminLocations(token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/locations`, { headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('获取区域失败');
    const data = await res.json();
    return (data || []).map((item: any) => ({
      ...item,
      backgroundUrl: normalizeUrl(item.backgroundUrl)
    }));
  },
  async adminSaveLocation(location: any, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/locations`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify(location)
    });
    if (!res.ok) throw new Error('保存区域失败');
    return res.json();
  },
  async adminDeleteLocation(id: number, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/locations/${id}`, {
      method: 'DELETE',
      headers: jsonHeaders(token)
    });
    if (!res.ok) throw new Error('删除区域失败');
    return res.json();
  },
  async adminBatchLocations(payload: any[], token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/locations/batch`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error('批量导入失败');
    return res.json();
  },
  async adminCharacters(token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/characters`, { headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('获取角色失败');
    const data = await res.json();
    return (data || []).map((item: any) => ({
      ...item,
      avatarUrl: normalizeUrl(item.avatarUrl)
    }));
  },
  async adminSaveCharacter(character: any, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/characters`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify(character)
    });
    if (!res.ok) throw new Error('保存角色失败');
    return res.json();
  },
  async adminDeleteCharacter(id: number, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/characters/${id}`, {
      method: 'DELETE',
      headers: jsonHeaders(token)
    });
    if (!res.ok) throw new Error('删除角色失败');
    return res.json();
  },
  async adminBatchCharacters(payload: any[], token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/world/characters/batch`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error('批量导入失败');
    return res.json();
  },
  async adminLlm(token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/system/llm`, { headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('获取设置失败');
    return res.json();
  },
  async saveAdminLlm(payload: any, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/system/llm`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error('保存失败');
    return res.json();
  },
  async testAdminLlm(token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/system/llm/test`, { method: 'POST', headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('测试失败');
    return res.json();
  },
  async adminLlmApis(token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/system/llm-apis`, { headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('获取 API 池失败');
    return res.json();
  },
  async adminCreateLlmApi(payload: any, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/system/llm-apis`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error('创建失败');
    return res.json();
  },
  async adminUpdateLlmApi(id: number, payload: any, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/system/llm-apis/${id}`, {
      method: 'PUT',
      headers: jsonHeaders(token),
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error('更新失败');
    return res.json();
  },
  async adminDeleteLlmApi(id: number, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/system/llm-apis/${id}`, {
      method: 'DELETE',
      headers: jsonHeaders(token)
    });
    if (!res.ok) throw new Error('删除失败');
    return res.json();
  },
  async adminResetLlmTokens(id: number, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/system/llm-apis/${id}/reset-tokens`, {
      method: 'POST',
      headers: jsonHeaders(token)
    });
    if (!res.ok) throw new Error('重置失败');
    return res.json();
  },
  async adminTestLlmApi(id: number, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/system/llm-apis/${id}/test`, {
      method: 'POST',
      headers: jsonHeaders(token)
    });
    if (!res.ok) throw new Error('测试失败');
    return res.json();
  },
  async adminUsage(token?: string): Promise<any> {
    const res = await fetch(`${API_BASE}/api/admin/users/usage`, { headers: jsonHeaders(token) });
    if (!res.ok) throw new Error('获取用量失败');
    return res.json();
  },
  async adminUpdateGlobalLimit(limit: number, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/settings/global-limit`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify({ limit })
    });
    if (!res.ok) throw new Error('保存失败');
    return res.json();
  },
  async adminUpdateUserLimit(userId: number, limit: number | null, token?: string) {
    const res = await fetch(`${API_BASE}/api/admin/users/${userId}/limit`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify({ limit })
    });
    if (!res.ok) throw new Error('保存失败');
    return res.json();
  },
  async uploadImage(file: File, token?: string) {
    const form = new FormData();
    form.append('file', file);
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(`${API_BASE}/api/upload/image`, {
      method: 'POST',
      headers,
      body: form
    });
    if (!res.ok) throw new Error('上传失败');
    const data = await res.json();
    if (data?.url) {
      return { ...data, url: normalizeUrl(data.url) };
    }
    return data;
  },
  async recallMemory(memoryId: string, token?: string, sessionId?: string) {
    const res = await fetch(`${API_BASE}/api/game/memory/recall`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify({ memoryId, sessionId })
    });
    if (!res.ok) throw new Error('回忆失败');
    return res.json();
  },
  async fireflyAssets(): Promise<FireflyAsset[]> {
    const res = await fetch(`${API_BASE}/api/world/assets/firefly`);
    if (!res.ok) throw new Error('获取立绘失败');
    const data = await res.json();
    return (data || []).map((item: any) => ({
      ...item,
      url: normalizeUrl(item.url)
    }));
  }
};

function mapState(data: any): GameState {
  return {
    currentLocation: {
      id: data.currentLocation,
      name: data.currentLocationName || data.currentLocation,
      description: data.locationDynamicState,
      backgroundStyle: 'bg-gradient-to-br from-slate-900 to-slate-800',
      coordinates: { x: 50, y: 50 },
      isUnlocked: true,
      domainId: 'penacony'
    },
    locationDynamicState: data.locationDynamicState,
    fireflyEmotion: data.fireflyEmotion,
    fireflyStatus: data.fireflyStatus,
    fireflyMoodDetails: data.fireflyMoodDetails,
    gameTime: data.gameTime,
    items: data.items || [],
    memories: data.memories || [],
    userName: data.userName || '开拓者'
  };
}

export { API_BASE };
