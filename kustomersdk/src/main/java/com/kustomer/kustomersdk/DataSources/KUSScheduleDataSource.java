package com.kustomer.kustomersdk.DataSources;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Enums.KUSBusinessHoursAvailability;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.Models.KUSHoliday;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Models.KUSSchedule;
import com.kustomer.kustomersdk.Utils.KUSJsonHelper;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;


public class KUSScheduleDataSource extends KUSObjectDataSource {
    //region Properties
    private boolean isActiveBusinessHours;

    @Nullable
    private String scheduleId;
    @Nullable
    private String lastFetchedScheduleId;
    private boolean isScheduleNotFound;
    private boolean isFetched;
    //endregion

    //region Initializer
    public KUSScheduleDataSource(KUSUserSession userSession) {
        super(userSession);
    }
    //endregion

    //region subclass methods
    @Override
    KUSModel objectFromJson(JSONObject jsonObject) throws KUSInvalidJsonException {
        return new KUSSchedule(jsonObject);
    }

    @Override
    void performRequest(@NonNull final KUSRequestCompletionListener completionListener) {
        if (getUserSession() == null) {
            completionListener.onCompletion(new Error(), null);
            return;
        }

        final String scheduleIdToFetch = scheduleIdToFetch();
        String endPoint = String.format(KUSConstants.URL.BUSINESS_SCHEDULE_ENDPOINT_WITH_ID, scheduleIdToFetch);

        getUserSession().getRequestManager().getEndpoint(
                endPoint,
                true,
                new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, JSONObject response) {

                        boolean isSuccessfullyFetched = error == null;

                        int statusCode = KUSJsonHelper.getErrorStatus(error);

                        isScheduleNotFound = statusCode == KUSConstants.ApiStatusCodes.NOT_FOUND_CODE;
                        isFetched = isSuccessfullyFetched || isScheduleNotFound;

                        if (isSuccessfullyFetched)
                            lastFetchedScheduleId = scheduleIdToFetch;

                        scheduleId = null;
                        completionListener.onCompletion(error, response);
                    }
                }
        );
    }

    @Override
    public void fetch() {
        boolean isNewSchedule = !Objects.equals(lastFetchedScheduleId, scheduleIdToFetch());
        boolean shouldFetch = !isFetched() || isNewSchedule || isScheduleNotFound;
        if (shouldFetch) {
            isFetched = false;
            super.fetch();
        } else {
            scheduleId = null;
        }
    }

    @Override
    public boolean isFetched() {
        return isFetched;
    }

    @NonNull
    private String scheduleIdToFetch() {
        return scheduleId != null ? scheduleId : "default";
    }

    public void setScheduleId(@Nullable String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public boolean isActiveBusinessHours() {
        KUSChatSettings chatSettings = null;

        if (getUserSession() != null)
            chatSettings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();

        if (chatSettings == null || chatSettings.getAvailability() ==
                KUSBusinessHoursAvailability.KUS_BUSINESS_HOURS_AVAILABILITY_ONLINE) {
            return true;
        }

        if (isScheduleNotFound)
            return true;

        KUSSchedule businessHours = (KUSSchedule) getObject();

        if (businessHours == null)
            return true;

        // Check that current date is not in holiday date and time
        Date now = Calendar.getInstance().getTime();
        for (KUSHoliday holiday : businessHours.getHolidays()) {
            if (holiday.getEnabled()) {

                boolean todayIsDuringOrAfterHolidayStartDate = now.equals(holiday.getStartDate())
                        || now.after(holiday.getStartDate());

                boolean todayIsDuringOrBeforeHolidayEndDate = now.equals(holiday.getEndDate())
                        || now.before(holiday.getEndDate());

                boolean todayIsHoliday = todayIsDuringOrAfterHolidayStartDate
                        && todayIsDuringOrBeforeHolidayEndDate;

                if (todayIsHoliday) {
                    return false;
                }
            }
        }

        // Get Week Day
        Calendar calendar = Calendar.getInstance();
        int weekDay = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        JSONArray businessHoursOfCurrentDay = KUSJsonHelper.arrayFromKeyPath(businessHours.getHours(),
                String.valueOf(weekDay));
        if (businessHoursOfCurrentDay == null)
            return false;

        for (int i = 0; i < businessHoursOfCurrentDay.length(); i++) {
            try {
                JSONArray businessHoursRange = businessHoursOfCurrentDay.getJSONArray(i);
                if (businessHoursRange != null && businessHoursRange.length() == 2
                        && businessHoursRange.getInt(0) <= minutes
                        && businessHoursRange.getInt(1) >= minutes) {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
    //endregion
}
