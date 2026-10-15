package com.rendezvous.common.geo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

class GeoPointsTest {

    @Test
    void pointUsesLonAsXLatAsYAndWgs84() {
        Point point = GeoPoints.point(50.06, 19.94);

        assertThat(point.getX()).isEqualTo(19.94);
        assertThat(point.getY()).isEqualTo(50.06);
        assertThat(point.getSRID()).isEqualTo(4326);
        assertThat(GeoPoints.lat(point)).isEqualTo(50.06);
        assertThat(GeoPoints.lon(point)).isEqualTo(19.94);
    }
}
