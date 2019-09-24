package com.kustomer.kustomersdk.Models;

import com.kustomer.kustomersdk.Enums.KUSBusinessHoursAvailability;
import com.kustomer.kustomersdk.Enums.KUSVolumeControlMode;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.KUSJsonHelper;

import org.json.JSONObject;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSChatSettings extends KUSModel implements Serializable {
    //region Properties
    private String teamName;
    private URL teamIconURL;
    private String greeting;
    private String activeFormId;
    private String pusherAccessKey;
    private Boolean enabled;

    private KUSBusinessHoursAvailability availability;
    private String offHoursImageUrl;
    private String offHoursMessage;

    private String waitMessage;
    private String customWaitMessage;
    private Integer timeOut;
    private Integer promptDelay;
    private Boolean hideWaitOption;
    private ArrayList<String> followUpChannels;
    private Boolean useDynamicWaitMessage;
    private Boolean markDoneAfterTimeout;
    private Boolean volumeControlEnabled;
    private Boolean closableChat;
    private Boolean singleSessionChat;
    private Boolean noHistory;
    private Boolean shouldShowTypingIndicatorCustomerWeb;
    private Boolean shouldShowTypingIndicatorWeb;

    private KUSVolumeControlMode volumeControlMode;
    private Integer upfrontWaitThreshold;
    private Boolean showKustomerBranding;
    //endregion

    //region Initializer
    public KUSChatSettings(JSONObject json) throws KUSInvalidJsonException {
        super(json);

        teamName = KUSJsonHelper.stringFromKeyPath(json, "attributes.teamName");
        teamIconURL = KUSJsonHelper.urlFromKeyPath(json, "attributes.teamIconUrl");
        greeting = KUSJsonHelper.stringFromKeyPath(json, "attributes.greeting");
        activeFormId = KUSJsonHelper.stringFromKeyPath(json, "attributes.activeForm");
        pusherAccessKey = KUSJsonHelper.stringFromKeyPath(json, "attributes.pusherAccessKey");
        enabled = KUSJsonHelper.boolFromKeyPath(json, "attributes.enabled");

        closableChat = KUSJsonHelper.boolFromKeyPath(json, "attributes.closableChat");
        waitMessage = KUSJsonHelper.stringFromKeyPath(json, "attributes.waitMessage");
        singleSessionChat = KUSJsonHelper.boolFromKeyPath(json, "attributes.singleSessionChat");
        noHistory = KUSJsonHelper.boolFromKeyPath(json, "attributes.noHistory");

        customWaitMessage = KUSJsonHelper.stringFromKeyPath(json, "attributes.volumeControl.customWaitMessage");
        timeOut = KUSJsonHelper.integerFromKeyPath(json, "attributes.volumeControl.timeout");
        promptDelay = KUSJsonHelper.integerFromKeyPath(json, "attributes.volumeControl.promptDelay");
        hideWaitOption = KUSJsonHelper.boolFromKeyPath(json, "attributes.volumeControl.hideWaitOption");
        followUpChannels = KUSJsonHelper.arrayListFromKeyPath(json, "attributes.volumeControl.followUpChannels");
        useDynamicWaitMessage = KUSJsonHelper.boolFromKeyPath(json, "attributes.volumeControl.useDynamicWaitMessage");
        markDoneAfterTimeout = KUSJsonHelper.boolFromKeyPath(json, "attributes.volumeControl.markDoneAfterTimeout");
        volumeControlEnabled = KUSJsonHelper.boolFromKeyPath(json, "attributes.volumeControl.enabled");

        offHoursMessage = KUSJsonHelper.stringFromKeyPath(json,"attributes.offhoursMessage");
        offHoursImageUrl = KUSJsonHelper.stringFromKeyPath(json,"attributes.offhoursImageUrl");
        availability = getKUSBusinessHoursAvailabilityFromString(KUSJsonHelper.stringFromKeyPath(json,"attributes.offhoursDisplay"));

        volumeControlMode = KUSVolumeControlModeFromString(KUSJsonHelper.stringFromKeyPath(json,"attributes.volumeControl.mode"));
        upfrontWaitThreshold = KUSJsonHelper.integerFromKeyPath(json,"attributes.volumeControl.upfrontWaitThreshold");
        showKustomerBranding = KUSJsonHelper.boolFromKeyPath(json, "attributes.showBrandingIdentifier");

        shouldShowTypingIndicatorCustomerWeb = KUSJsonHelper.boolFromKeyPath(json,
                "attributes.showTypingIndicatorCustomerWeb");
        shouldShowTypingIndicatorWeb = KUSJsonHelper.boolFromKeyPath(json,
                "attributes.showTypingIndicatorWeb");
    }

    @Override
    public String modelType() {
        return "chat_settings";
    }
    //endregion

    //region Private Methods
    private KUSVolumeControlMode KUSVolumeControlModeFromString(String string){

        if(string == null)
            return KUSVolumeControlMode.KUS_VOLUME_CONTROL_MODE_UNKNOWN;

        if(string.equals("upfront")){
            return KUSVolumeControlMode.KUS_VOLUME_CONTROL_MODE_UPFRONT;
        }else if (string.equals("delayed")){
            return KUSVolumeControlMode.KUS_VOLUME_CONTROL_MODE_DELAYED;
        }

        return KUSVolumeControlMode.KUS_VOLUME_CONTROL_MODE_UNKNOWN;
    }

    private String stringSanitizedReply(String autoReply) {
        if (autoReply != null)
            return autoReply.trim().length() > 0 ? autoReply.trim() : null;
        else
            return null;
    }

    private static KUSBusinessHoursAvailability getKUSBusinessHoursAvailabilityFromString(String string) {
        if(string == null)
            return KUSBusinessHoursAvailability.KUS_BUSINESS_HOURS_AVAILABILITY_HIDE_CHAT;

        switch (string) {
            case "online":
                return KUSBusinessHoursAvailability.KUS_BUSINESS_HOURS_AVAILABILITY_ONLINE;
            case "offline":
                return KUSBusinessHoursAvailability.KUS_BUSINESS_HOURS_AVAILABILITY_OFFLINE;
            default:
                return KUSBusinessHoursAvailability.KUS_BUSINESS_HOURS_AVAILABILITY_HIDE_CHAT;
        }
    }
    //endregion

    //region Accessors

    public String getTeamName() {
        return teamName;
    }

    public URL getTeamIconURL() {
        return teamIconURL;
    }

    public String getGreeting() {
        return greeting;
    }

    public String getActiveFormId() {
        return activeFormId;
    }

    public String getPusherAccessKey() {
        return pusherAccessKey;
    }

    public Boolean getEnabled() {
        return enabled != null ? enabled : false;
    }

    public String getCustomWaitMessage() {
        return customWaitMessage;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public int getPromptDelay() {
        return promptDelay;
    }

    public Boolean isHideWaitOption() {
        return hideWaitOption != null ? hideWaitOption : false;
    }

    public ArrayList<String> getFollowUpChannels() {
        return followUpChannels;
    }

    public Boolean isUseDynamicWaitMessage() {
        return useDynamicWaitMessage != null ? useDynamicWaitMessage : false;
    }

    public Boolean isMarkDoneAfterTimeout() {
        return markDoneAfterTimeout != null ? markDoneAfterTimeout : false;
    }

    public Boolean isVolumeControlEnabled() {
        return volumeControlEnabled != null ? volumeControlEnabled : false;
    }

    public String getWaitMessage() {
        return waitMessage;
    }

    public Boolean getHideWaitOption() {
        return hideWaitOption != null ? hideWaitOption : false;
    }

    public Boolean getUseDynamicWaitMessage() {
        return useDynamicWaitMessage != null ? useDynamicWaitMessage : false;
    }

    public Boolean getMarkDoneAfterTimeout() {
        return markDoneAfterTimeout != null ? markDoneAfterTimeout : false;
    }

    public Boolean getVolumeControlEnabled() {
        return volumeControlEnabled != null ? volumeControlEnabled : false;
    }

    public Boolean getClosableChat() {
        return closableChat != null ? closableChat : false;
    }

    public Boolean getSingleSessionChat() {
        return singleSessionChat != null ? singleSessionChat : false;
    }

    public Boolean getNoHistory() {
        return noHistory != null ? noHistory : false;
    }

    public KUSBusinessHoursAvailability getAvailability() {
        return availability;
    }

    public String getOffHoursImageUrl() {
        return offHoursImageUrl;
    }

    public String getOffHoursMessage() {
        return offHoursMessage;
    }

    public KUSVolumeControlMode getVolumeControlMode() {
        return volumeControlMode;
    }

    public int getUpfrontWaitThreshold() {
        return upfrontWaitThreshold;
    }

    public boolean shouldShowKustomerBranding() {
        return showKustomerBranding != null ? showKustomerBranding : false;
    }

    public boolean getShouldShowTypingIndicatorCustomerWeb() {
        return shouldShowTypingIndicatorCustomerWeb != null ? shouldShowTypingIndicatorCustomerWeb : false;
    }

    public boolean getShouldShowTypingIndicatorWeb() {
        return shouldShowTypingIndicatorWeb != null ? shouldShowTypingIndicatorWeb :false;
    }

    //endregion
}
