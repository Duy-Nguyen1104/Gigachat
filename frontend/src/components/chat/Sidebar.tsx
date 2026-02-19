import { useState } from "react";
import { Search, Plus, User } from "lucide-react";
import type { Conversation } from "../../types";
import UserSearch from "./UserSearch";

interface SidebarProps {
  conversations: Conversation[];
  activeId: string | null;
  onSelect: (id: string) => void;
  onStartChat?: (userId: string, displayName: string) => void;
  loading?: boolean;
}

export default function Sidebar({
  conversations,
  activeId,
  onSelect,
  onStartChat,
  loading = false,
}: SidebarProps) {
  const [search, setSearch] = useState("");
  const [showUserSearch, setShowUserSearch] = useState(false);

  function handleStartChat(userId: string, displayName: string) {
    setShowUserSearch(false);
    onStartChat?.(userId, displayName);
  }

  if (showUserSearch) {
    return (
      <div className="w-80 border-r border-gray-100 bg-white flex flex-col">
        <UserSearch
          onStartChat={handleStartChat}
          onClose={() => setShowUserSearch(false)}
        />
      </div>
    );
  }

  const filtered = conversations.filter((c) => {
    const name = c.name || "Unnamed";
    return name.toLowerCase().includes(search.toLowerCase());
  });

  return (
    <div className="w-80 border-r border-gray-100 bg-white flex flex-col">
      {/* Header */}
      <div className="px-6 pt-6 pb-4 border-b border-gray-100 ">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-900">Messages</h2>
          <button
            onClick={() => setShowUserSearch(true)}
            className="w-10 h-10 rounded-xl bg-brand-100 hover:bg-brand-200 flex items-center justify-center text-brand-700 transition"
            title="New conversation"
          >
            <Plus className="w-5 h-5" />
          </button>
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search conversations..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-gray-50 border border-gray-100 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-300 focus:border-transparent transition"
          />
        </div>
      </div>

      {/* Conversation List */}
      <div className="flex-1 overflow-y-auto py-2 px-3">
        {loading ? (
          <div className="text-center py-12 text-gray-400 text-sm">
            Loading...
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-12 text-gray-400 text-sm">
            {conversations.length === 0 ? "No conversations yet" : "No results"}
          </div>
        ) : (
          filtered.map((conv) => (
            <button
              key={conv.id}
              onClick={() => onSelect(conv.id)}
              className={`w-full flex items-start gap-3 px-3 py-3 rounded-xl mb-1 text-left transition ${
                activeId === conv.id
                  ? "bg-brand-50 border border-brand-200"
                  : "hover:bg-gray-50 border border-transparent"
              }`}
            >
              {/* Avatar */}
              <div className="w-12 h-12 rounded-full bg-gray-200 flex items-center justify-center flex-shrink-0 text-gray-600">
                {conv.avatarUrl ? (
                  <img
                    src={conv.avatarUrl}
                    alt=""
                    className="w-full h-full object-cover rounded-full"
                  />
                ) : (
                  <User className="w-5 h-5" />
                )}
              </div>

              {/* Info */}
              <div className="flex-1 min-w-0 pt-0.5">
                <div className="flex items-baseline justify-between mb-0.5">
                  <span className="text-sm font-semibold text-gray-900 truncate">
                    {conv.name || "Unnamed"}
                  </span>
                  {conv.lastMessage && (
                    <span className="text-xs text-gray-400 ml-2 flex-shrink-0">
                      {formatTime(conv.lastMessage.createdAt)}
                    </span>
                  )}
                </div>
                {(conv.unreadCount ?? 0) > 0 ? (
                  <p className="text-xs font-bold truncate">
                    {conv.lastMessage?.content}
                  </p>
                ) : (
                  <p className="text-xs text-gray-500 truncate">
                    {conv.lastMessage?.content || "No messages yet"}
                  </p>
                )}
              </div>

              {/* Unread badge */}
              {(conv.unreadCount ?? 0) > 0 && (
                <div className="w-5 h-5 rounded-full bg-brand-400 flex items-center justify-center flex-shrink-0 mt-1">
                  <span className="text-[10px] font-bold text-gray-900">
                    {conv.unreadCount > 9 ? "9+" : conv.unreadCount}
                  </span>
                </div>
              )}
            </button>
          ))
        )}
      </div>
    </div>
  );
}

function formatTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const days = Math.floor(diff / 86400000);

  if (days === 0) {
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  }
  if (days === 1) return "Yesterday";
  if (days < 7) {
    return date.toLocaleDateString([], { weekday: "short" });
  }
  return date.toLocaleDateString([], { month: "short", day: "numeric" });
}
