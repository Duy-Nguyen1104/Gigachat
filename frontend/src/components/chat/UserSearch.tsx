import { useState, useEffect, useRef } from "react";
import { Search, User, X, Loader2 } from "lucide-react";
import api from "../../api/axios";
import type { UserSearchResult } from "../../types";

interface UserSearchProps {
  onStartChat: (userId: string, displayName: string) => void;
  onClose: () => void;
}

export default function UserSearch({ onStartChat, onClose }: UserSearchProps) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<UserSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [startingChat, setStartingChat] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);

    if (query.trim().length < 2) {
      setResults([]);
      setError(null);
      return;
    }

    debounceRef.current = setTimeout(async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await api.get<{
          users: UserSearchResult[];
          hasMore: boolean;
        }>(`/users/search?q=${encodeURIComponent(query.trim())}&limit=10`);
        setResults(res.data.users);
      } catch {
        setError("Search failed. Please try again.");
      } finally {
        setLoading(false);
      }
    }, 300);

    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query]);

  async function handleSelectUser(user: UserSearchResult) {
    setStartingChat(user.id);
    try {
      await api.post("/conversations", {
        type: "direct",
        participantId: user.id,
      });
      onStartChat(user.id, user.displayName || user.username);
    } catch (err: unknown) {
      // 409 means conversation already exists — still navigate
      const status = (err as { response?: { status?: number } })?.response
        ?.status;
      if (status === 409) {
        onStartChat(user.id, user.displayName || user.username);
      } else {
        setError("Could not start conversation. Please try again.");
        setStartingChat(null);
      }
    }
  }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between px-6 pt-6 pb-4 border-b border-gray-100">
        <h2 className="text-xl font-bold text-gray-900">New Chat</h2>
        <button
          onClick={onClose}
          className="w-8 h-8 rounded-xl hover:bg-gray-100 flex items-center justify-center text-gray-500 transition"
          aria-label="Close search"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Search input */}
      <div className="px-4 py-3 border-b border-gray-100">
        <div className="relative">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by username..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-gray-50 border border-gray-100 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-300 focus:border-transparent transition"
          />
          {loading && (
            <Loader2 className="absolute right-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 animate-spin" />
          )}
        </div>
      </div>

      {/* Results */}
      <div className="flex-1 overflow-y-auto py-2 px-3">
        {error && (
          <p className="text-center text-sm text-red-500 py-4">{error}</p>
        )}

        {!loading &&
          !error &&
          query.trim().length >= 2 &&
          results.length === 0 && (
            <p className="text-center text-sm text-gray-400 py-8">
              No users found for &ldquo;{query}&rdquo;
            </p>
          )}

        {query.trim().length < 2 && !error && (
          <p className="text-center text-sm text-gray-400 py-8">
            Type at least 2 characters to search
          </p>
        )}

        {results.map((user) => (
          <button
            key={user.id}
            onClick={() => handleSelectUser(user)}
            disabled={startingChat === user.id}
            className="w-full flex items-center gap-3 px-3 py-3 rounded-xl mb-1 text-left transition hover:bg-gray-50 border border-transparent disabled:opacity-60"
          >
            {/* Avatar */}
            <div className="w-11 h-11 rounded-full bg-gray-200 flex items-center justify-center flex-shrink-0 text-gray-600 overflow-hidden">
              {user.avatarUrl ? (
                <img
                  src={user.avatarUrl}
                  alt={user.displayName || user.username}
                  className="w-full h-full object-cover"
                />
              ) : (
                <User className="w-5 h-5" />
              )}
            </div>

            {/* Info */}
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold text-gray-900 truncate">
                {user.displayName || user.username}
              </p>
              <p className="text-xs text-gray-500 truncate">@{user.username}</p>
            </div>

            {/* Action */}
            {startingChat === user.id ? (
              <Loader2 className="w-4 h-4 text-gray-400 animate-spin flex-shrink-0" />
            ) : (
              <span className="text-xs font-medium flex-shrink-0">Chat</span>
            )}
          </button>
        ))}
      </div>
    </div>
  );
}
