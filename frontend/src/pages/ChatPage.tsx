import { useState, useEffect, useCallback, useRef } from "react";
import Navbar from "../components/chat/Navbar";
import Sidebar from "../components/chat/Sidebar";
import ChatArea from "../components/chat/ChatArea";
import type { Conversation, Message } from "../types";
import {
  getConversations,
  getConversation,
  createDirectConversation,
  markConversationRead,
} from "../api/conversations";
import { getMessages, sendMessage } from "../api/messages";
import { useWebSocket } from "../hooks/useWebSocket";
import { useAuth } from "../context/AuthContext";

/** { userId → displayName } for people currently typing in the active conversation. */
type TypingMap = Map<string, string>;

export default function ChatPage() {
  const { user: me } = useAuth();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<
    string | null
  >(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [loadingConversations, setLoadingConversations] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [typingUsers, setTypingUsers] = useState<TypingMap>(new Map());

  // Timeouts that auto-clear typing status after 6 s of silence
  const typingTimers = useRef<Map<string, ReturnType<typeof setTimeout>>>(
    new Map(),
  );

  // ── WebSocket event handler ───────────────────────────────────────────────
  const handleWsEvent = useCallback(
    (type: string, data: unknown) => {
      if (type === "MESSAGE_NEW") {
        const msg = data as Message;
        setMessages((prev) => {
          // Deduplicate: the sender already added it optimistically via HTTP
          if (prev.some((m) => m.id === msg.id)) return prev;
          return [...prev, msg];
        });
        setConversations((prev) =>
          prev.map((c) =>
            c.id === msg.conversationId
              ? {
                  ...c,
                  lastMessage: {
                    id: msg.id,
                    content: msg.content,
                    senderId: msg.senderId,
                    senderName: msg.sender?.displayName ?? null,
                    createdAt: msg.createdAt,
                    type: msg.type,
                  },
                  updatedAt: msg.createdAt,
                  // Increment unread only for inactive conversations from other users
                  unreadCount:
                    msg.conversationId === activeConversationId ||
                    msg.senderId === me?.id
                      ? (c.unreadCount ?? 0)
                      : (c.unreadCount ?? 0) + 1,
                }
              : c,
          ),
        );
      }

      if (type === "MESSAGE_UPDATED") {
        const updated = data as Message;
        setMessages((prev) =>
          prev.map((m) => (m.id === updated.id ? updated : m)),
        );
      }

      if (type === "MESSAGE_DELETED") {
        const deleted = data as Message;
        setMessages((prev) =>
          prev.map((m) =>
            m.id === deleted.id ? { ...m, isDeleted: true, content: "" } : m,
          ),
        );
      }

      if (type === "TYPING_START") {
        const { userId, displayName } = data as {
          userId: string;
          displayName: string;
        };
        if (userId === me?.id) return; // ignore own echo
        const existing = typingTimers.current.get(userId);
        if (existing) clearTimeout(existing);
        const timer = setTimeout(() => {
          setTypingUsers((prev) => {
            const next = new Map(prev);
            next.delete(userId);
            return next;
          });
          typingTimers.current.delete(userId);
        }, 6000);
        typingTimers.current.set(userId, timer);
        setTypingUsers((prev) => new Map(prev).set(userId, displayName));
      }

      if (type === "TYPING_STOP") {
        const { userId } = data as { userId: string };
        const existing = typingTimers.current.get(userId);
        if (existing) clearTimeout(existing);
        typingTimers.current.delete(userId);
        setTypingUsers((prev) => {
          const next = new Map(prev);
          next.delete(userId);
          return next;
        });
      }
    },
    [activeConversationId, me?.id],
  );

  const { sendTypingStart, sendTypingStop } = useWebSocket({
    conversationId: activeConversationId,
    onEvent: handleWsEvent,
  });

  // Clear typing state when switching conversations
  useEffect(() => {
    typingTimers.current.forEach(clearTimeout);
    typingTimers.current.clear();
    setTypingUsers(new Map());
  }, [activeConversationId]);

  // ── Load conversations on mount ──────────────────────────────────────────
  useEffect(() => {
    let cancelled = false;
    setLoadingConversations(true);
    getConversations()
      .then((data) => {
        if (!cancelled) setConversations(data.conversations);
      })
      .catch((err) => console.error("Failed to load conversations", err))
      .finally(() => {
        if (!cancelled) setLoadingConversations(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // ── Load messages when active conversation changes ───────────────────────
  useEffect(() => {
    if (!activeConversationId) {
      setMessages([]);
      return;
    }
    let cancelled = false;
    setLoadingMessages(true);
    getMessages(activeConversationId, { limit: 50 })
      .then((data) => {
        if (!cancelled) setMessages(data.messages);
      })
      .catch((err) => console.error("Failed to load messages", err))
      .finally(() => {
        if (!cancelled) setLoadingMessages(false);
      });
    return () => {
      cancelled = true;
    };
  }, [activeConversationId]);

  // ── Mark conversation as read when opened ───────────────────────────────
  useEffect(() => {
    if (!activeConversationId) return;
    markConversationRead(activeConversationId).catch(() => {});
    setConversations((prev) =>
      prev.map((c) =>
        c.id === activeConversationId ? { ...c, unreadCount: 0 } : c,
      ),
    );
  }, [activeConversationId]);

  // ── Send message ─────────────────────────────────────────────────────────
  const handleSendMessage = useCallback(
    async (payload: {
      type: "text" | "image" | "file" | "voice";
      content?: string;
      attachmentUrl?: string;
    }) => {
      if (!activeConversationId) return;
      if (payload.type === "text" && !payload.content?.trim()) return;

      sendTypingStop(activeConversationId);
      try {
        const newMsg = await sendMessage(activeConversationId, {
          type: payload.type,
          content: payload.content?.trim(),
          attachmentUrl: payload.attachmentUrl,
        });
        // Add optimistically — WS broadcast from server will dedup
        setMessages((prev) => {
          if (prev.some((m) => m.id === newMsg.id)) return prev;
          return [...prev, newMsg];
        });
        setConversations((prev) =>
          prev.map((c) =>
            c.id === activeConversationId
              ? {
                  ...c,
                  lastMessage: {
                    id: newMsg.id,
                    content: newMsg.content,
                    senderId: newMsg.senderId,
                    senderName: newMsg.sender?.displayName ?? null,
                    createdAt: newMsg.createdAt,
                    type: newMsg.type,
                  },
                  updatedAt: newMsg.createdAt,
                }
              : c,
          ),
        );
      } catch (err) {
        console.error("Failed to send message", err);
      }
    },
    [activeConversationId, sendTypingStop],
  );

  // ── Start a new direct conversation ──────────────────────────────────────
  const handleStartChat = useCallback(
    async (userId: string, _displayName: string) => {
      try {
        const conv = await createDirectConversation(userId);
        const full = await getConversation(conv.id);
        setConversations((prev) => {
          const exists = prev.find((c) => c.id === full.id);
          if (exists) return prev;
          return [full, ...prev];
        });
        setActiveConversationId(full.id);
      } catch (err) {
        console.error("Failed to start conversation", err);
      }
    },
    [],
  );

  // ── Select conversation ──────────────────────────────────────────────────
  const handleSelectConversation = useCallback(async (id: string) => {
    setActiveConversationId(id);
    try {
      const updated = await getConversation(id);
      setConversations((prev) => prev.map((c) => (c.id === id ? updated : c)));
    } catch {
      /* best-effort */
    }
  }, []);

  // ── Typing forwarded to ChatArea ─────────────────────────────────────────
  const handleTypingStart = useCallback(() => {
    if (activeConversationId) sendTypingStart(activeConversationId);
  }, [activeConversationId, sendTypingStart]);

  const handleTypingStop = useCallback(() => {
    if (activeConversationId) sendTypingStop(activeConversationId);
  }, [activeConversationId, sendTypingStop]);

  const activeConversation =
    conversations.find((c) => c.id === activeConversationId) ?? null;

  return (
    <div className="h-screen flex overflow-hidden">
      <Navbar />
      <Sidebar
        conversations={conversations}
        activeId={activeConversationId}
        onSelect={handleSelectConversation}
        onStartChat={handleStartChat}
        loading={loadingConversations}
      />
      <ChatArea
        conversation={activeConversation}
        messages={messages}
        onSendMessage={handleSendMessage}
        loading={loadingMessages}
        typingUsers={[...typingUsers.values()]}
        onTypingStart={handleTypingStart}
        onTypingStop={handleTypingStop}
      />
    </div>
  );
}
