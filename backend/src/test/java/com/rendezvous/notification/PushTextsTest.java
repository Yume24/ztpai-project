package com.rendezvous.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.rendezvous.alert.domain.AlertType;
import org.junit.jupiter.api.Test;

class PushTextsTest {

    @Test
    void textsMatchSpecification() {
        assertThat(PushTexts.TITLE).isEqualTo("Rendezvous");
        assertThat(PushTexts.roomStarted("Rynek"))
                .isEqualTo("Pokój „Rynek” wystartował — dotknij, aby udostępniać lokalizację");
        assertThat(PushTexts.alert(AlertType.GEOFENCE_EXIT, "Ola", "Rynek"))
                .isEqualTo("Ola wychodzi poza strefę w „Rynek”");
        assertThat(PushTexts.alert(AlertType.GEOFENCE_RETURN, "Ola", "Rynek"))
                .isEqualTo("Ola wraca do strefy w „Rynek”");
        assertThat(PushTexts.alert(AlertType.MEMBER_LEFT, "Ola", "Rynek"))
                .isEqualTo("Ola opuszcza pokój „Rynek”");
        assertThat(PushTexts.removed("Rynek")).isEqualTo("Usunięto cię z pokoju „Rynek”");
    }
}
