package com.oracoolsss;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private UUID uuid;
    private String text;
    private String senderName;
    private MessageType type;
    private Node substitutor;

    public enum MessageType {
        REGISTER_MESSAGE,
        TEXT_MESSAGE,
        CONFIRMATION_MESSAGE,
        SUBSTITUTION_MESSAGE
    }

    Message(UUID uuid, String message, String senderName, MessageType messageType) {
        this.uuid = uuid;
        this.text = message;
        this.senderName = senderName;
        this.type = messageType;
    }

    Message(UUID uuid, String message, String senderName, MessageType messageType, Node substitutor) {
        this(uuid, message, senderName, messageType);
        this.substitutor = substitutor;
    }

    Message(Message message) {
        this.uuid = message.uuid;
        this.text = message.text;
        this.senderName = message.senderName;
        this.type = message.type;
    }

    UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    String getText() {
        return text;
    }

    String getSenderName() {
        return senderName;
    }

    MessageType getType() {
        return type;
    }

    void setType(MessageType type) {
        this.type = type;
    }

    Node getSubstitutor() {
        return substitutor;
    }

    @Override
    public String toString() {
        return "UUID: " + uuid + ". " + "Message: " + text + ". "
                + "Sender name: " + senderName + ". " + "Message type: " + type + ". ";
    }
}
