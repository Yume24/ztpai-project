package com.rendezvous.common.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public final class GeoPoints {

    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    private GeoPoints() {
    }

    public static Point point(double lat, double lon) {
        return FACTORY.createPoint(new Coordinate(lon, lat));
    }

    public static double lat(Point point) {
        return point.getY();
    }

    public static double lon(Point point) {
        return point.getX();
    }
}
