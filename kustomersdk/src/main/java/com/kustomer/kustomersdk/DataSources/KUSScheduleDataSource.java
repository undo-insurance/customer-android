package com.kustomer.kustomersdk.DataSources;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Enums.KUSBusinessHoursAvailability;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.Models.KUSHoliday;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Models.KUSSchedule;
import com.kustomer.kustomersdk.Models.KUSTrackingToken;
import com.kustomer.kustomersdk.Utils.JsonHelper;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;


public class KUSScheduleDataSource extends KUSObjectDataSource {
    //region Properties
    private boolean isActiveBusinessHours;
    //endregion

    //region Initializer
    public KUSScheduleDataSource(KUSUserSession userSession){
        super(userSession);
    }
    //endregion

    //region subclass methods
    @Override
    KUSModel objectFromJson(JSONObject jsonObject) throws KUSInvalidJsonException {
        return new KUSSchedule(jsonObject);
    }

    @Override
    void performRequest(KUSRequestCompletionListener completionListener){
        getUserSession().getRequestManager().getEndpoint(
                KUSConstants.URL.BUSINESS_SCHEDULE_ENDPOINT,
                true,
                completionListener
        );
    }

    public boolean isActiveBusinessHours(){
        KUSChatSettings chatSettings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();
        if(chatSettings == null || chatSettings.getAvailability() == KUSBusinessHoursAvailability.KUS_BUSINESS_HOURS_AVAILABILITY_ONLINE){
            return true;
        }

        KUSSchedule businessHours = (KUSSchedule) getObject();
        if(businessHours != null && businessHours.getEnabled()){
            // Check that current date is not in holiday date and time
            Date now = Calendar.getInstance().getTime();
            for (KUSHoliday holiday : businessHours.getHolidays()){
                if(holiday.getEnabled()){

                    boolean todayIsDuringOrAfterHolidayStartDate = now.equals(holiday.getStartDate())
                            || now.after(holiday.getStartDate());

                    boolean todayIsDuringOrBeforeHolidayEndDate = now.equals(holiday.getEndDate())
                            || now.before(holiday.getEndDate());

                    boolean todayIsHoliday = todayIsDuringOrAfterHolidayStartDate
                            && todayIsDuringOrBeforeHolidayEndDate;

                    if(todayIsHoliday){
                        return false;
                    }
                }
            }

            // Get Week Day
            Calendar calendar = Calendar.getInstance();
            int weekDay = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

            JSONArray businessHoursOfCurrentDay = JsonHelper.arrayFromKeyPath(businessHours.getHours(),
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

        return true;
    }
    //endregion
}
