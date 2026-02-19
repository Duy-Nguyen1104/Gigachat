import { useState, useRef, useEffect } from "react";
import {
  Send,
  Paperclip,
  User,
  Phone,
  Video,
  MoreVertical,
} from "lucide-react";
import type { Message, Conversation } from "../../types";
import { useAuth } from "../../context/AuthContext";
import MessageBubble from "./MessageBubble";

interface ChatAreaProps {
  conversation: Conversation | null;
  messages: Message[];
  onSendMessage?: (content: string) => void;
  loading?: boolean;
}

export default function ChatArea({
  conversation,
  messages,
  onSendMessage,
  loading = false,
}: ChatAreaProps) {
  const [input, setInput] = useState("");
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { user } = useAuth();

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = () => {
    const trimmed = input.trim();
    if (!trimmed) return;
    onSendMessage?.(trimmed);
    setInput("");
  };

  if (!conversation) {
    return (
      <div className="flex-1 flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="w-20 h-20 bg-brand-100 rounded-3xl flex items-center justify-center mx-auto mb-5">
            <Send className="w-9 h-9 text-brand-600" />
          </div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">
            Your messages
          </h3>
          <p className="text-gray-500 text-sm max-w-xs">
            Select a conversation to start chatting
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col bg-white">
      {/* Header */}
      <div className="h-16 px-6 flex items-center justify-between border-b border-gray-100 bg-white">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-gray-600">
            {conversation.avatarUrl ? (
              <img
                src={conversation.avatarUrl}
                alt=""
                className="w-full h-full object-cover rounded-full"
              />
            ) : (
              <User className="w-5 h-5" />
            )}
          </div>
          <div>
            <h3 className="text-sm font-semibold text-gray-900">
              {conversation.name || "Unnamed"}
            </h3>
            <p className="text-xs text-green-500">Online</p>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <button className="w-10 h-10 rounded-xl hover:bg-gray-100 flex items-center justify-center text-gray-500 transition">
            <Phone className="w-5 h-5" />
          </button>
          <button className="w-10 h-10 rounded-xl hover:bg-gray-100 flex items-center justify-center text-gray-500 transition">
            <Video className="w-5 h-5" />
          </button>
          <button className="w-10 h-10 rounded-xl hover:bg-gray-100 flex items-center justify-center text-gray-500 transition">
            <MoreVertical className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-6 py-4 bg-gray-50">
        {loading ? (
          <div className="flex items-center justify-center h-full text-gray-400 text-sm">
            Loading messages...
          </div>
        ) : messages.length === 0 ? (
          <div className="flex items-center justify-center h-full text-gray-400 text-sm">
            No messages yet. Start the conversation!
          </div>
        ) : (
          <>
            {messages.map((msg) => (
              <MessageBubble
                key={msg.id}
                message={msg}
                isOwn={msg.senderId === user?.id}
              />
            ))}
            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* Input */}
      <div className="px-6 py-4 border-t border-gray-100 bg-white">
        <div className="flex items-end gap-3">
          <button className="w-10 h-10 rounded-xl hover:bg-gray-100 flex items-center justify-center text-gray-500 transition flex-shrink-0">
            <Paperclip className="w-5 h-5" />
          </button>
          <div className="flex-1 bg-gray-50 rounded-2xl border border-gray-200 px-4 py-3 focus-within:ring-2 focus-within:ring-brand-300 transition">
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  handleSend();
                }
              }}
              placeholder="Type a message..."
              rows={1}
              className="w-full bg-transparent text-sm text-gray-900 placeholder-gray-400 resize-none flex items-center justify-center focus:outline-none max-h-32"
            />
          </div>
          <button
            onClick={handleSend}
            disabled={!input.trim()}
            className="w-10 h-10 rounded-xl bg-brand-400 hover:bg-brand-500 flex items-center justify-center text-gray-900 transition disabled:opacity-40 disabled:cursor-not-allowed flex-shrink-0 shadow-sm"
          >
            <Send className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );
}
