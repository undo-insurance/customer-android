package com.kustomer.kustomersdk.DataSources;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Models.KUSTeam;
import com.kustomer.kustomersdk.Utils.KUSConstants;
import com.kustomer.kustomersdk.Utils.KUSUtils;

import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSTeamsDataSource extends KUSPaginatedDataSource {

    //region Properties
    List<String> teamIds;
    //endregion

    //region Initializer
    public KUSTeamsDataSource(KUSUserSession userSession, List<String> teamIds) {
        super(userSession);
        this.teamIds = new ArrayList<>(teamIds);
    }

    public List<String> getTeamIds() {
        return teamIds;
    }
    //endregion

    //region subclass methods
    public URL getFirstUrl() {
        if (getUserSession() == null)
            return null;

        if (teamIds != null) {
            String endPoint = String.format(KUSConstants.URL.TEAMS_ENDPOINT,
                    KUSUtils.listJoinedByString(teamIds, ","));
            return getUserSession().getRequestManager().urlForEndpoint(endPoint);
        } else
            return null;
    }

    @Override
    public List<KUSModel> objectsFromJSON(JSONObject jsonObject) {
        ArrayList<KUSModel> arrayList = null;

        KUSModel model = null;
        try {
            model = new KUSTeam(jsonObject);
        } catch (KUSInvalidJsonException e) {
            e.printStackTrace();
        }

        if (model != null) {
            arrayList = new ArrayList<>();
            arrayList.add(model);
        }

        return arrayList;
    }
    //endregion
}
