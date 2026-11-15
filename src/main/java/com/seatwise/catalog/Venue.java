package com.seatwise.catalog;

import com.seatwise.common.Guard;

/** A venue that hosts events. Immutable. */
public record Venue(String id, String name, String city) {

    public Venue {
        Guard.notBlank(id, "venue.id");
        Guard.notBlank(name, "venue.name");
        Guard.notBlank(city, "venue.city");
    }
}
