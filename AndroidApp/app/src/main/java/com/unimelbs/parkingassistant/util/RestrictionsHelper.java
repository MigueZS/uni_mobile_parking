package com.unimelbs.parkingassistant.util;

import com.unimelbs.parkingassistant.parkingapi.Restriction;

import java.util.List;

public class RestrictionsHelper {

    public static String convertRestrictionsToString(List<Restriction> restrictions) {
        String restrictionMsg = "";
        for (int i = 0; i < restrictions.size(); i++) {
            restrictionMsg = restrictionMsg +
                    "Restriction " + (i+1) +": \n"+
                    "\t"+restrictions.get(i).getDescription()+"\n"+
                    "\t"+restrictions.get(i).getDuration()+"\n";
        }
        return  restrictionMsg;
    }

    public static boolean isValid(List<Restriction> restrictions, String strHour) {
        for (int i = 0; i < restrictions.size(); i++) {

        }
        return true;
    }

    public static String getInvalidReason(List<Restriction> restrictions, String strHour) {
        for (int i = 0; i < restrictions.size(); i++) {

        }
        return "Invalid Input";
    }
}
