import type { Message } from "../../types";

interface MessageBubbleProps {
  message: Message;
  isOwn: boolean;
}

export default function MessageBubble({ message, isOwn }: MessageBubbleProps) {
  const timestamp = new Date(message.createdAt).toLocaleString([], {
    weekday: "long",
    hour: "2-digit",
    minute: "2-digit",
  });

  const timestampBubble = (
    <span className="shrink-0 self-center text-[10px] text-white bg-gray-700 px-2 py-0.5 rounded-full whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity duration-150 pointer-events-none">
      {timestamp}
      {message.isEdited && <span className="ml-1">(edited)</span>}
    </span>
  );

  return (
    <div className={`flex ${isOwn ? "justify-end" : "justify-start"} mb-1`}>
      <div className="group flex items-center gap-2 max-w-[75%]">
        {/* Timestamp left of own messages, right of others */}
        {isOwn && timestampBubble}

        {/* Message bubble */}
        <div
          className={`px-4 py-2.5 rounded-2xl ${
            isOwn
              ? "bg-brand-300 text-gray-900 rounded-br-md"
              : "bg-gray-100 text-gray-900 rounded-bl-md"
          }`}
        >
          <p className="text-sm leading-relaxed whitespace-pre-wrap break-words">
            {message.content}
          </p>
        </div>

        {!isOwn && timestampBubble}
      </div>
    </div>
  );
}
