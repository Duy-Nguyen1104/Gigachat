import { useEffect, useRef, useCallback } from "react";
import { Client, type StompSubscription } from "@stomp/stompjs";
import SockJSImport from "sockjs-client";

// sockjs-client is CJS; after Vite pre-bundles it (via optimizeDeps.include)
// the constructor lives on .default. Guard for both cases.
// Also guard against null/undefined to prevent blank page crashes.
const SockJS: any = (() => {
  if (!SockJSImport) return null;
  return (SockJSImport as any).default || SockJSImport;
})();

interface WsEvent {
  type: string;
  data: unknown;
}

const NOTIFICATIONS_DESTINATION = "/user/queue/notifications";
const conversationTopic = (conversationId: string) =>
  `/topic/conversation/${conversationId}`;

interface UseWebSocketOptions {
  /** Currently active conversation to subscribe to. */
  conversationId: string | null;
  /** Called whenever an event arrives on the active conversation topic. */
  onEvent: (type: string, data: unknown) => void;
}

/**
 * Manages a single STOMP-over-SockJS connection for the life of the component.
 *
 * Subscribes to `/topic/conversation/{conversationId}` and re-subscribes
 * automatically when the active conversation changes or on reconnect.
 */
export function useWebSocket({ conversationId, onEvent }: UseWebSocketOptions) {
  const clientRef = useRef<Client | null>(null);
  const subRef = useRef<StompSubscription | null>(null);
  const notificationSubRef = useRef<StompSubscription | null>(null);

  // Always keep the callbacks current (avoids stale closure in subscribe handler)
  const onEventRef = useRef(onEvent);
  const convIdRef = useRef(conversationId);
  useEffect(() => {
    onEventRef.current = onEvent;
  });
  useEffect(() => {
    convIdRef.current = conversationId;
  });

  const handleFrame = useCallback((body: string) => {
    try {
      const event = JSON.parse(body) as WsEvent;
      onEventRef.current(event.type, event.data);
    } catch (e) {
      console.error("WS frame parse error", e);
    }
  }, []);

  // ── Subscribe helper — safe to call any time the client is connected ────
  const subscribe = useCallback((client: Client, id: string) => {
    subRef.current?.unsubscribe();
    subRef.current = client.subscribe(conversationTopic(id), (frame) => {
      handleFrame(frame.body);
    });
  }, [handleFrame]);

  const subscribeToNotifications = useCallback((client: Client) => {
    notificationSubRef.current?.unsubscribe();
    notificationSubRef.current = client.subscribe(NOTIFICATIONS_DESTINATION, (frame) => {
      handleFrame(frame.body);
    });
  }, [handleFrame]);

  // ── Create STOMP client once on mount ────────────────────────────────────
  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token || !SockJS) {
      if (!SockJS)
        console.error("SockJS constructor not found, WebSocket will not work.");
      return;
    }

    const client = new Client({
      webSocketFactory: () =>
        new SockJS(import.meta.env.VITE_WS_URL || "http://localhost:8080/ws"),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        // Always listen for personal notifications so messages arrive even
        // before the user opens a conversation after login/reload.
        subscribeToNotifications(client);
        // Re-subscribe to whatever conversation is active (handles reconnects too)
        if (convIdRef.current) subscribe(client, convIdRef.current);
      },
      onStompError: (frame) =>
        console.error("STOMP error", frame.headers["message"]),
    });

    client.activate(); // send the CONNECT frame
    clientRef.current = client;

    return () => {
      subRef.current?.unsubscribe();
      notificationSubRef.current?.unsubscribe();
      client.deactivate();
      clientRef.current = null;
    };
  }, [subscribe, subscribeToNotifications]);

  // ── Re-subscribe when active conversation changes ────────────────────────
  useEffect(() => {
    const client = clientRef.current;
    if (!client?.connected) return; // onConnect will handle it after reconnect

    if (conversationId) {
      subscribe(client, conversationId);
    } else {
      subRef.current?.unsubscribe();
      subRef.current = null;
    }
  }, [conversationId, subscribe]);

  // ── Publish helpers ──────────────────────────────────────────────────────
  const sendTypingStart = useCallback((convId: string) => {
    clientRef.current?.publish({
      destination: "/app/typing.start",
      body: JSON.stringify({ conversationId: convId }),
    });
  }, []);

  const sendTypingStop = useCallback((convId: string) => {
    clientRef.current?.publish({
      destination: "/app/typing.stop",
      body: JSON.stringify({ conversationId: convId }),
    });
  }, []);

  return { sendTypingStart, sendTypingStop };
}
