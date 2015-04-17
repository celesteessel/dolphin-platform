package com.canoo.dolphin.server.event;

import com.canoo.dolphin.event.Subscription;

/**
 * The dolphin event bus that can be used to send messages to other dolphin sessions.
 */
public interface DolphinEventBus {

    /**
     * Publish a message to the given address
     *
     * @param data the data of the message
     */
    void publish(String address, Object data);

    /**
     * Register as a handler / listener for a given address. All messages that will be published for the given address
     * by any dolphin session will trigger the given handler in the correct dolphin session.
     *
     * @param topic   the topic
     * @param handler the handler
     */
    Subscription subscribe(String topic, MessageListener handler);
}
