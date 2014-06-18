package com.feedient.models.feed;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Content {
    @SerializedName("message")
    private String message;

    @SerializedName("date_created")
    private Date dateCreated;

    @SerializedName("action_counts")
    private ActionCounts actionCounts;

    @SerializedName("actions_performed")
    private ActionsPerformed actionsPerformed;

    @SerializedName("is_conversation")
    private boolean isConversation;

    @SerializedName("entities")
    private Entity entities;

    public Content() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public ActionCounts getActionCounts() {
        return actionCounts;
    }

    public void setActionCounts(ActionCounts actionCounts) {
        this.actionCounts = actionCounts;
    }

    public ActionsPerformed getActionsPerformed() {
        return actionsPerformed;
    }

    public void setActionsPerformed(ActionsPerformed actionsPerformed) {
        this.actionsPerformed = actionsPerformed;
    }

    public boolean isConversation() {
        return isConversation;
    }

    public void setConversation(boolean isConversation) {
        this.isConversation = isConversation;
    }

    public Entity getEntities() {
        return entities;
    }

    public void setEntities(Entity entities) {
        this.entities = entities;
    }
}
