package com.kustomer.kustomersdk.Utils;

/**
 * Created by Junaid on 1/23/2018.
 */

public class KUSConstants {

    public interface Pattern {
        String URL_PATTERN = "(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    }

    public interface URL {
        String HOST_NAME = "kustomerapp.com";
        String PUSHER_AUTH = "/c/v1/pusher/auth";
        String MESSAGES_LIST_ENDPOINT = "/c/v1/chat/sessions/%s/messages";
        String SEND_MESSAGE_ENDPOINT = "/c/v1/chat/messages";
        String CHAT_SESSIONS_ENDPOINT = "/c/v1/chat/sessions";
        String CONVERSATIONS_ENDPOINT = "/c/v1/conversations";
        String TRACKING_TOKEN_ENDPOINT = "/c/v1/tracking/tokens";
        String CURRENT_TRACKING_TOKEN_ENDPOINT = "/c/v1/tracking/tokens/current";
        String IDENTITY_ENDPOINT = "/c/v1/identity";
        String SETTINGS_ENDPOINT = "/c/v1/chat/settings";
        String TEAMS_ENDPOINT = "/c/v1/chat/teams/%s";
        String FORMS_ENDPOINT = "/c/v1/chat/forms/%s";
        String FORMS_RESPONSES_ENDPOINT = "/c/v1/chat/forms/%s/responses";
        String CURRENT_CUSTOMER_ENDPOINT = "/c/v1/customers/current";
        String ATTACHMENT_ENDPOINT = "https://%s.api.%s/c/v1/chat/messages/%s/attachments/%s?redirect=true";
        String CHAT_ATTACHMENT_ENDPOINT = "/c/v1/chat/attachments";
        String CLIENT_ACTIVITY_ENDPOINT = "/c/v1/client-activity";
        String VOLUME_CONTROL_ENDPOINT = "/c/v1/chat/volume-control/responses";
        String SESSION_LOCK_ENDPOINT = "/c/v1/chat/sessions/%s";
        String CUSTOMER_STATS_ENDPOINT = "/c/v1/chat/customers/stats";
        String BUSINESS_SCHEDULE_ENDPOINT = "/c/v1/schedules/default?include=holidays";
        String SESSION_QUEUE_ENDPOINT = "/c/v1/chat/sessions/%s/queue";
        String SINGLE_MESSAGE_ENDPOINT = "/c/v1/chat/sessions/%s/messages/%s";
        String SATISFACTION_RESPONSE_ENDPOINT = "/c/v1/chat/sessions/%s/satisfaction";
        String SATISFACTION_RESPONSE_SUBMIT_ENDPOINT = "/c/v1/chat/satisfaction-responses/%s";
    }

    public interface Keys {
        String K_KUSTOMER_TRACKING_TOKEN_HEADER_KEY = "x-kustomer-tracking-token";
        String K_KUSTOMER_ORG_ID_KEY = "org";
        String K_KUSTOMER_ORG_NAME_KEY = "orgName";
        String K_KUSTOMER_URL_KEY = "url";
    }

    public interface HeaderKeys {
        String K_KUSTOMER_LANGUAGE_KEY = "lang";
        String K_KUSTOMER_X_KUSTOMER_KEY = "X-Kustomer";
        String K_KUSTOMER_ACCEPT_LANGUAGE_KEY = "Accept-Language";
        String K_KUSTOMER_USER_AGENT_KEY = "User_Agent";
        String K_KUSTOMER_X_CLIENT_KEY = "x-kustomer-client";
        String K_KUSTOMER_X_VERSION_KEY = "x-kustomer-version";
    }

    public interface PusherEventNames {
        String SEND_MESSAGE_EVENT = "kustomer.app.chat.message.send";
        String END_SESSION_EVENT = "kustomer.app.chat.session.end";
        String CHAT_ACTIVITY_TYPING_EVENT = "client-kustomer.app.chat.activity.typing";
    }

    public interface BundleName {
        String CHAT_SESSION_BUNDLE_KEY = "Chat_Session_bundle";
        String CHAT_SCREEN_BACK_BUTTON_KEY = "Chat_back_button_bundle";
        String NOTIFICATION_ID_BUNDLE_KEY = "Notification_ID";
        String CHAT_SCREEN_MESSAGE = "Chat_screen_message";
        String CHAT_SCREEN_RESTARTED_KEY = "Chat_screen_restarted_bundle";
        String CHAT_SCREEN_FORM_ID = "Chat_form_id";
    }

    public interface ChatSession {
        String TEMP_SESSION_ID = "temp_session_id";
    }

}
