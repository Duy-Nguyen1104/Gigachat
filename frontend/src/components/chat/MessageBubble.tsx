import { FileIcon, Download } from "lucide-react";
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

        {/* Message Content */}
        {message.type === "image" && message.attachmentUrl ? (
          <div className="relative max-w-sm">
            <img
              src={message.attachmentUrl}
              alt="Attachment"
              className="max-h-64 rounded-2xl shadow-sm cursor-pointer hover:opacity-95 transition border border-gray-100"
              onClick={() => window.open(message.attachmentUrl!, "_blank")}
            />
          </div>
        ) : (
          <div
            className={`px-4 py-2.5 rounded-2xl ${
              isOwn
                ? "bg-brand-300 text-gray-900 rounded-br-md"
                : "bg-gray-100 text-gray-900 rounded-bl-md"
            }`}
          >
            {message.type === "file" && message.attachmentUrl ? (
              <a
                href={message.attachmentUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 py-1 group/file"
              >
                <div className="w-10 h-10 bg-white/40 rounded-xl flex items-center justify-center text-gray-700 group-hover/file:bg-white transition">
                  <FileIcon className="w-5 h-5" />
                </div>
                <div className="flex flex-col">
                  <span className="text-[10px] font-bold uppercase tracking-wider opacity-50">
                    {message.content?.split(".").pop()} File
                  </span>
                  <div className="flex items-center gap-1 text-xs font-semibold">
                    <span>Download</span>
                    <Download className="w-3 h-3" />
                  </div>
                </div>
              </a>
            ) : (
              <p className="text-sm leading-relaxed whitespace-pre-wrap break-words">
                {message.content}
              </p>
            )}
          </div>
        )}

        {!isOwn && timestampBubble}
      </div>
    </div>
  );
}
