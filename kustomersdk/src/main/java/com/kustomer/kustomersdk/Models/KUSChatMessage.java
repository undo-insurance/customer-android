package com.kustomer.kustomersdk.Models;

import android.support.annotation.NonNull;

import com.kustomer.kustomersdk.Enums.KUSChatMessageDirection;
import com.kustomer.kustomersdk.Enums.KUSChatMessageState;
import com.kustomer.kustomersdk.Enums.KUSChatMessageType;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Utils.JsonHelper;
import com.kustomer.kustomersdk.Utils.KUSConstants;
import com.kustomer.kustomersdk.Utils.KUSUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static com.kustomer.kustomersdk.Utils.JsonHelper.arrayListFromKeyPath;
import static com.kustomer.kustomersdk.Utils.JsonHelper.dateFromKeyPath;
import static com.kustomer.kustomersdk.Utils.JsonHelper.stringFromKeyPath;

/**
 * Created by Junaid on 1/20/2018.
 */



public class KUSChatMessage extends KUSModel {

    //region Properties
    private String trackingId;
    private String body;
    private URL imageUrl;
    private List attachmentIds;

    private Date createdAt;
    private Date importedAt;
    private KUSChatMessageDirection direction;
    private String sentById;
    private String campaignId;

    private KUSChatMessageType type;
    private KUSChatMessageState state;
    private String value;
    //endregion

    //region Initializer
    public KUSChatMessage(){

    }

    public KUSChatMessage(JSONObject json) throws KUSInvalidJsonException {
        this(json, KUSChatMessageType.KUS_CHAT_MESSAGE_TYPE_TEXT, null);
    }

    public KUSChatMessage(JSONObject json, KUSChatMessageType type, URL imageUrl) throws KUSInvalidJsonException {
        super(json);

        state = KUSChatMessageState.KUS_CHAT_MESSAGE_STATE_SENT;
        trackingId = stringFromKeyPath(json,"attributes.trackingId");
        body = stringFromKeyPath(json,"attributes.body");
        this.type = type;
        this.imageUrl = imageUrl;

        JSONArray attachmentArray = JsonHelper.arrayFromKeyPath(json,"relationships.attachments.data");

        if(attachmentArray != null)
            this.attachmentIds = arrayListFromJsonArray(attachmentArray,"id");

        this.createdAt = dateFromKeyPath(json,"attributes.createdAt");
        this.importedAt = dateFromKeyPath(json,"attributes.importedAt");
        this.direction = KUSChatMessageDirectionFromString(stringFromKeyPath(json,"attributes.direction"));
        this.sentById = stringFromKeyPath(json, "relationships.sentBy.data.id");
        this.campaignId = stringFromKeyPath(json, "relationships.campaign.data.id");
    }

    //endregion

    //region Public Methods
    public static boolean KUSChatMessageSentByUser(KUSChatMessage message) {

        return message != null && message.direction == KUSChatMessageDirection.KUS_CHAT_MESSAGE_DIRECTION_IN;

    }

    public static boolean KUSMessagesSameSender(KUSChatMessage message1, KUSChatMessage message2) {
        return message1 != null
                && message2 != null
                && message1.direction == message2.direction
                && (message1.sentById != null && message1.sentById.equalsIgnoreCase(message2.sentById));
    }

    private static KUSChatMessageDirection KUSChatMessageDirectionFromString(String str) {

        if(str == null)
            return null;

        return str.equalsIgnoreCase("in")
                ? KUSChatMessageDirection.KUS_CHAT_MESSAGE_DIRECTION_IN
                : KUSChatMessageDirection.KUS_CHAT_MESSAGE_DIRECTION_OUT;
    }

    public static URL attachmentUrlForMessageId(String messageId, String attachmentId) throws MalformedURLException {
        String imageUrlString = String.format(KUSConstants.URL.ATTACHMENT_ENDPOINT,
                Kustomer.getSharedInstance().getUserSession().getOrgName(),
                Kustomer.hostDomain(),
                messageId,
                attachmentId);

        return new URL(imageUrlString);
    }

    @Override
    public String modelType(){
        return "chat_message";
    }

    @Override
    public String toString() {
        //Missing %p (this)
        return String.format("<%s : oid: %s; body: %s>",this.getClass(),this.getId(),this.body);
    }

    @Override
    public boolean equals(Object obj) {

        if(obj == this)
            return true;
        if(!obj.getClass().equals(this.getClass()))
            return false;

        KUSChatMessage chatMessage = (KUSChatMessage) obj;

        if(chatMessage.state != this.state)
            return false;
        if(chatMessage.direction != this.direction)
            return false;
        if(chatMessage.type != this.type)
            return false;
        if(chatMessage.attachmentIds != null && this.attachmentIds!=null && !chatMessage.attachmentIds.equals(this.attachmentIds))
            return false;
        if(chatMessage.attachmentIds == null || this.attachmentIds == null)
            return false;
        if(!chatMessage.getId().equals(this.getId()))
            return false;
        if(!chatMessage.createdAt.equals(this.createdAt))
            return false;
        if(chatMessage.importedAt != null && !chatMessage.importedAt.equals(this.importedAt))
            return false;
        if(!chatMessage.body.equals(this.body))
            return false;

        return true;
    }

    @Override
    public int compareTo(@NonNull KUSModel kusModel) {
        KUSChatMessage message = (KUSChatMessage) kusModel;

        int date;
        if (this.createdAt == null && message.createdAt == null)
            date = 0;
        else if (this.createdAt == null)
            date = 1;
        else if (message.createdAt == null)
            date = -1;
        else
            date = message.createdAt.compareTo(this.createdAt);

        int parent = super.compareTo(kusModel);
        return date == 0 ? parent : date;
    }
    //endregion

    //region Private Methods
    private List<String> arrayListFromJsonArray(JSONArray array, String id) {
        List<String> list = new ArrayList<>();

        for(int i = 0 ; i<array.length() ; i++){
            try {
                JSONObject jsonObject = array.getJSONObject(i);
                list.add(jsonObject.getString(id));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return list;
    }
    //endregion

    //region Accessors

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public URL getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(URL imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List getAttachmentIds() {
        return attachmentIds;
    }

    public void setAttachmentIds(ArrayList attachmentIds) {
        this.attachmentIds = attachmentIds;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Date importedAt) {
        this.importedAt = importedAt;
    }

    public KUSChatMessageDirection getDirection() {
        return direction;
    }

    public void setDirection(KUSChatMessageDirection direction) {
        this.direction = direction;
    }

    public String getSentById() {
        return sentById;
    }

    public void setSentById(String sentById) {
        this.sentById = sentById;
    }

    public KUSChatMessageType getType() {
        return type;
    }

    public void setType(KUSChatMessageType type) {
        this.type = type;
    }

    public KUSChatMessageState getState() {
        return state;
    }

    public void setState(KUSChatMessageState state) {
        this.state = state;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCampaignId() {
        return campaignId;
    }

    //endregion

}
