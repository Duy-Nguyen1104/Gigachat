package com.project.gigachat.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic wrapper for all WebSocket events sent to clients.
 * <pre>{ "type": "MESSAGE_NEW", "data": { ... } }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsEvent {

    /** Event type discriminator — e.g. MESSAGE_NEW, TYPING_START, USER_ONLINE */
    private String type;

    /** The actual payload; serialised as a nested JSON object. */
    private Object data;
}
