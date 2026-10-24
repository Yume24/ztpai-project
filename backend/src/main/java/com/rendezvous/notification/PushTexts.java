package com.rendezvous.notification;

import com.rendezvous.alert.domain.AlertType;

public final class PushTexts {

    public static final String TITLE = "Rendezvous";

    private PushTexts() {
    }

    public static String roomStarted(String roomName) {
        return "Pokój „" + roomName + "” wystartował — dotknij, aby udostępniać lokalizację";
    }

    public static String alert(AlertType type, String person, String roomName) {
        return switch (type) {
            case GEOFENCE_EXIT -> person + " wychodzi poza strefę w „" + roomName + "”";
            case GEOFENCE_RETURN -> person + " wraca do strefy w „" + roomName + "”";
            case MEMBER_LEFT -> person + " opuszcza pokój „" + roomName + "”";
        };
    }

    public static String removed(String roomName) {
        return "Usunięto cię z pokoju „" + roomName + "”";
    }
}
