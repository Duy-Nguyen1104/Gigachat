import { useState, useRef, useEffect, useCallback } from "react";
import {
  Send,
  Paperclip,
  User,
  Phone,
  Video,
  MoreVertical,
  Loader2,
} from "lucide-react";
import type { Message, Conversation } from "../../types";
import { useAuth } from "../../context/AuthContext";
import MessageBubble from "./MessageBubble";
import { getUploadUrl } from "../../api/messages";
import axios from "axios";

interface ChatAreaProps {
  conversation: Conversation | null;
  messages: Message[];
  onSendMessage?: (payload: {
    type: "text" | "image" | "file" | "voice";
    content?: string;
    attachmentUrl?: string;
  }) => void;
  loading?: boolean;
  /** Display names of users currently typing. */
  typingUsers?: string[];
  onTypingStart?: () => void;
  onTypingStop?: () => void;
}

export default function ChatArea({
  conversation,
  messages,
  onSendMessage,
  loading = false,
  typingUsers = [],
  onTypingStart,
  onTypingStop,
}: ChatAreaProps) {
  const [input, setInput] = useState("");
  const [isUploading, setIsUploading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const stopTypingTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isTyping = useRef(false);
  const { user } = useAuth();

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = () => {
    const trimmed = input.trim();
    if (!trimmed) return;
    // Stop typing before sending
    if (isTyping.current) {
      onTypingStop?.();
      isTyping.current = false;
    }
    if (stopTypingTimer.current) {
      clearTimeout(stopTypingTimer.current);
      stopTypingTimer.current = null;
    }
    onSendMessage?.({ type: "text", content: trimmed });
    setInput("");
  };

  const handleFileClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !conversation) return;

    try {
      setIsUploading(true);

      // 1. Get presigned URL
      const { uploadUrl, fileKey } = await getUploadUrl({
        fileName: file.name,
        fileType: file.type,
        conversationId: conversation.id,
      });

      // 2. Upload to S3
      await axios.put(uploadUrl, file, {
        headers: {
          "Content-Type": file.type,
        },
      });

      // 3. Send message; server resolves key to presigned GET URL when fetching messages
      const type = file.type.startsWith("image/") ? "image" : "file";
      onSendMessage?.({
        type,
        attachmentUrl: fileKey,
        content: file.name,
      });
    } catch (err) {
      console.error("Upload failed", err);
      alert("Failed to upload file. Please try again.");
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      setInput(e.target.value);
      if (!isTyping.current && e.target.value.trim()) {
        onTypingStart?.();
        isTyping.current = true;
      }
      // Reset the stop-typing debounce
      if (stopTypingTimer.current) clearTimeout(stopTypingTimer.current);
      if (e.target.value.trim()) {
        stopTypingTimer.current = setTimeout(() => {
          onTypingStop?.();
          isTyping.current = false;
          stopTypingTimer.current = null;
        }, 4000);
      } else {
        // Field cleared — stop immediately
        if (isTyping.current) {
          onTypingStop?.();
          isTyping.current = false;
        }
      }
    },
    [onTypingStart, onTypingStop],
  );

  const typingLabel = (() => {
    if (typingUsers.length === 0) return null;
    if (typingUsers.length === 1) return `${typingUsers[0]} is typing…`;
    if (typingUsers.length === 2)
      return `${typingUsers[0]} and ${typingUsers[1]} are typing…`;
    return `${typingUsers[0]}, ${typingUsers[1]}, and ${typingUsers.length - 2} others are typing…`;
  })();

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
            {typingLabel && (
              <div className="flex justify-start mb-2">
                <span className="text-xs text-gray-400 italic px-2">
                  {typingLabel}
                </span>
              </div>
            )}
            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* Input */}
      <div className="px-6 py-4 border-t border-gray-100 bg-white">
        <div className="flex items-end gap-3">
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            className="hidden"
          />
          <button
            onClick={handleFileClick}
            disabled={isUploading || !conversation}
            className="w-10 h-10 rounded-xl hover:bg-gray-100 flex items-center justify-center text-gray-500 transition flex-shrink-0 disabled:opacity-50"
          >
            {isUploading ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <Paperclip className="w-5 h-5" />
            )}
          </button>
          <div className="flex-1 bg-gray-50 rounded-2xl border border-gray-200 px-4 py-3 focus-within:ring-2 focus-within:ring-brand-300 transition">
            <textarea
              value={input}
              onChange={handleInputChange}
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
