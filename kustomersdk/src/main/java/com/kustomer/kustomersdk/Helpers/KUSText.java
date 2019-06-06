package com.kustomer.kustomersdk.Helpers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import com.kustomer.kustomersdk.Utils.KUSUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.noties.markwon.Markwon;
import ru.noties.markwon.SpannableConfiguration;
import ru.noties.markwon.spans.LinkSpan;
import ru.noties.markwon.spans.SpannableTheme;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSText {

    //region Properties
    private static final String EMAIL_REGEX = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,5}";
    private static final String PHONE_REGEX = "(\\+\\d{1,2}\\s)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}";
    private static final String CUSTOM_URL_REGEX = "(\\[[^\\)\\n\\r]*?\\]\\(\\s*?(mailto:)?"+Patterns.EMAIL_ADDRESS+
            "\\s*?\\))|("+Patterns.EMAIL_ADDRESS+")|("+Patterns.WEB_URL+")|(\\[[^\\)\\n\\r]*?\\]" +
            "\\(\\s*?"+Patterns.WEB_URL+"\\s*?\\))";
    private static final String CUSTOM_EMAIL_REGEX = "("+Patterns.EMAIL_ADDRESS+")|(\\[[^\\)\\n\\r]" +
            "*?\\]\\(\\s*?(mailto:)?"+Patterns.EMAIL_ADDRESS+"\\s*?\\))|(\\[[^\\)\\n\\r]*?\\]" +
            "\\(\\s*?"+Patterns.WEB_URL+"\\s*?\\))";
    //endregion

    //region Public Methods
    public static void setMarkDownText(@NonNull TextView textView, @Nullable String text) {

        if (text == null)
            return;

        SpannableTheme theme = SpannableTheme.builderWithDefaults(textView.getContext())
                .linkColor(textView.getTextColors().getDefaultColor())
                .build();

        SpannableConfiguration spannableConfiguration = SpannableConfiguration.builder(textView.getContext())
                .theme(theme)
                .linkResolver(new LinkSpan.Resolver() {
                    @Override
                    public void resolve(View view, @NonNull String link) {
                        KUSUtils.openUrl(view.getContext(), link);
                    }
                })
                .build();

        Markwon.setMarkdown(textView, spannableConfiguration, getFormattedText(text));
    }

    @NonNull
    private static String getFormattedText(@NonNull String text){
        String embeddedUrlFormat = "[%s](%s)";
        String embeddedEmailFormat = "[%s](mailto:%s)";

        String formattedText = text;
        formattedText = formatTextLineBreaks(formattedText);
        formattedText = convertEmailsAndUrlsToEmbeddedFormat(formattedText, CUSTOM_URL_REGEX,
                6, embeddedUrlFormat);
        formattedText = convertEmailsAndUrlsToEmbeddedFormat(formattedText, CUSTOM_EMAIL_REGEX,
                1, embeddedEmailFormat);

        return formattedText;
    }

    @NonNull
    private static String convertEmailsAndUrlsToEmbeddedFormat(@NonNull String text,
                                                               @NonNull String regex,
                                                               int matchingIndex,
                                                               @NonNull String embeddedFormat){
        String formattedText = "";
        int previousIndex = 0;

        Matcher matcher = Pattern.compile(regex).matcher(text);

        while (matcher.find()){
            String matchedText = matcher.group(matchingIndex);

            boolean isSimpleUrlOrEmail = matchedText != null;
            int startIndex = matcher.start();

            if(startIndex > 0)
                formattedText = formattedText.concat(text.substring(previousIndex, startIndex));

            if(isSimpleUrlOrEmail){
                formattedText = formattedText.concat(
                        String.format(embeddedFormat,matchedText,matchedText));
            }else {
                formattedText = formattedText.concat(matcher.group());
            }

            previousIndex = matcher.end();
        }

        if(previousIndex < text.length()){
            formattedText = formattedText.concat(text.substring(previousIndex));
        }

        return formattedText;
    }


    @NonNull
    private static String formatTextLineBreaks(@NonNull String text) {
        if (!text.contains("\n")) {
            return text;
        }

        StringBuilder updatedString = new StringBuilder();
        int nextIndex = text.contains("\n") ? text.indexOf("\n") : text.length();
        while (nextIndex < text.length()) {
            // if - is after \n
            if (nextIndex + 1 < text.length() && text.charAt(nextIndex + 1) == '-') {
                updatedString.append(text.substring(0, nextIndex + 1));
                text = text.substring(nextIndex + 1);
            }
            // if number is after \n
            else if (nextIndex + 1 < text.length() && Character.isDigit(text.charAt(nextIndex + 1))) {
                int digitRange = nextIndex + 1;
                // find total number length on text
                while (digitRange < text.length() && Character.isDigit(text.charAt(digitRange))) {
                    digitRange += 1;
                }

                // If . is after number then ignore \n as it is.
                if (digitRange < text.length() && text.charAt(digitRange) == '.') {
                    updatedString.append(text.substring(0, digitRange));
                    text = text.substring(digitRange);
                }
                // otherwise replace it with <br />
                else {
                    updatedString.append(text.substring(0, nextIndex)).append("<br />");
                    text = text.substring(nextIndex + 1);
                }
            }
            // Keep replacing \n with <br />
            else {
                updatedString.append(text.substring(0, nextIndex)).append("<br />");
                text = text.substring(nextIndex + 1);
            }
            nextIndex = text.contains("\n") ? text.indexOf("\n") : text.length();
        }

        // Replace multi occurrence of <br /> with \n
        return (updatedString + text).replace("<br /><br />", "\n\n");
    }

    public static boolean isValidEmail(String email) {
        if (email.length() == 0)
            return false;
        return Pattern.compile(EMAIL_REGEX).matcher(email).matches();
    }

    public static boolean isValidPhone(String phoneNo) {
        if (phoneNo.length() == 0)
            return false;
        return Pattern.compile(PHONE_REGEX).matcher(phoneNo).matches();
    }
    //endregion

}
