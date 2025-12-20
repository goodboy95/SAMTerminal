import { useState, useRef, useEffect } from 'react';
import { Send, User, Sparkles } from 'lucide-react';
import { Message } from '@/lib/simulation';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { ScrollArea } from '@/components/ui/scroll-area';
import { cn } from '@/lib/utils';

interface ChatInterfaceProps {
  messages: Message[];
  onSendMessage: (content: string) => void;
  isTyping: boolean;
}

export const ChatInterface = ({ messages, onSendMessage, isTyping }: ChatInterfaceProps) => {
  const [inputValue, setInputValue] = useState('');
  const scrollRef = useRef<HTMLDivElement>(null);

  // 自动滚动到底部
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isTyping]);

  const handleSend = () => {
    if (!inputValue.trim()) return;
    onSendMessage(inputValue);
    setInputValue('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSend();
  };

  return (
    <div className="flex flex-col h-full bg-black/60 backdrop-blur-md border-t md:border-l border-white/10 shadow-2xl">
      
      {/* 聊天记录区域 */}
      <ScrollArea className="flex-1 p-4">
        <div className="flex flex-col gap-6 pb-4">
          {messages.map((msg) => (
            <div key={msg.id} className="flex flex-col gap-1">
              
              {/* 旁白/动作描述 (居中) */}
              {msg.narration && (
                <div className="flex justify-center my-2">
                  <span className="text-xs text-white/60 italic bg-black/30 px-3 py-1 rounded-full">
                    {msg.narration}
                  </span>
                </div>
              )}

              {/* 消息气泡 */}
              <div className={cn(
                "flex gap-3 max-w-[90%]",
                msg.sender === 'user' ? "self-end flex-row-reverse" : "self-start"
              )}>
                {/* 头像 */}
                <div className={cn(
                  "w-8 h-8 rounded-full flex items-center justify-center shrink-0 border border-white/20",
                  msg.sender === 'user' ? "bg-yellow-500/20" : 
                  msg.sender === 'firefly' ? "bg-teal-500/20" : "bg-purple-500/20"
                )}>
                  {msg.sender === 'user' ? <User size={14} className="text-yellow-200" /> : 
                   msg.sender === 'firefly' ? <Sparkles size={14} className="text-teal-200" /> :
                   <span className="text-xs font-bold text-purple-200">{msg.npcName?.[0]}</span>}
                </div>

                {/* 气泡内容 */}
                <div className="flex flex-col gap-1">
                  {/* 发送者名字 (仅非用户显示) */}
                  {msg.sender !== 'user' && (
                    <span className="text-xs text-white/50 ml-1">
                      {msg.sender === 'firefly' ? '流萤' : msg.npcName}
                    </span>
                  )}
                  
                  <div className={cn(
                    "px-4 py-2 rounded-2xl text-sm leading-relaxed shadow-sm",
                    msg.sender === 'user' 
                      ? "bg-white text-black rounded-tr-none" 
                      : "bg-slate-800/90 text-white border border-white/10 rounded-tl-none"
                  )}>
                    {msg.content}
                  </div>
                </div>
              </div>
            </div>
          ))}

          {/* 正在输入指示器 */}
          {isTyping && (
            <div className="flex gap-3 self-start">
               <div className="w-8 h-8 rounded-full bg-teal-500/20 flex items-center justify-center border border-white/20">
                 <Sparkles size={14} className="text-teal-200 animate-pulse" />
               </div>
               <div className="bg-slate-800/50 px-4 py-2 rounded-2xl rounded-tl-none border border-white/5 flex items-center gap-1">
                 <span className="w-1.5 h-1.5 bg-white/50 rounded-full animate-bounce [animation-delay:-0.3s]"></span>
                 <span className="w-1.5 h-1.5 bg-white/50 rounded-full animate-bounce [animation-delay:-0.15s]"></span>
                 <span className="w-1.5 h-1.5 bg-white/50 rounded-full animate-bounce"></span>
               </div>
            </div>
          )}
          
          {/* 滚动锚点 */}
          <div ref={scrollRef} />
        </div>
      </ScrollArea>

      {/* 输入区域 */}
      <div className="p-4 border-t border-white/10 bg-black/20">
        <div className="flex gap-2">
          <Input 
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="和流萤说点什么... (试着输入: 去筑梦边境)"
            className="bg-white/5 border-white/10 text-white placeholder:text-white/30 focus-visible:ring-teal-500/50"
          />
          <Button 
            onClick={handleSend} 
            disabled={isTyping || !inputValue.trim()}
            size="icon"
            className="bg-teal-600 hover:bg-teal-700 text-white shrink-0"
          >
            <Send size={18} />
          </Button>
        </div>
      </div>
    </div>
  );
};