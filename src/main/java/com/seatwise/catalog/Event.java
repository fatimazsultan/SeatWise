package com.seatwise.catalog;

import com.seatwise.common.EntityNotFoundException;
import com.seatwise.common.Guard;
import com.seatwise.common.InvalidStateTransitionException;
import com.seatwise.common.Money;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An event that sells seats. Built only through {@link Builder} because it has many
 * fields and cross-field rules (sales open before start, unique section names).
 */
public final class Event {

    private final String id;
    private final String title;
    private final String description;
    private final String category;
    private final Venue venue;
    private final Instant startsAt;
    private final Instant salesOpenAt;
    private final List<Section> sections;
    private volatile EventStatus status;

    private Event(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.category = builder.category;
        this.venue = builder.venue;
        this.startsAt = builder.startsAt;
        this.salesOpenAt = builder.salesOpenAt;
        this.sections = List.copyOf(builder.sections);
        this.status = builder.status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String category() {
        return category;
    }

    public Venue venue() {
        return venue;
    }

    public Instant startsAt() {
        return startsAt;
    }

    public Instant salesOpenAt() {
        return salesOpenAt;
    }

    public List<Section> sections() {
        return sections;
    }

    public EventStatus status() {
        return status;
    }

    public int capacity() {
        return sections.stream().mapToInt(Section::capacity).sum();
    }

    public Optional<Section> section(String name) {
        return sections.stream().filter(s -> s.name().equalsIgnoreCase(name)).findFirst();
    }

    public Money basePriceOf(String sectionName) {
        return section(sectionName).map(Section::basePrice)
                .orElseThrow(() -> new EntityNotFoundException("Section", sectionName));
    }

    public boolean isOnSale(Instant now) {
        return status == EventStatus.PUBLISHED && !now.isBefore(salesOpenAt) && now.isBefore(startsAt);
    }

    void publish() {
        if (status != EventStatus.DRAFT) {
            throw new InvalidStateTransitionException("event", status.name(), "publish");
        }
        status = EventStatus.PUBLISHED;
    }

    void cancel() {
        if (status == EventStatus.CANCELLED) {
            throw new InvalidStateTransitionException("event", status.name(), "cancel");
        }
        status = EventStatus.CANCELLED;
    }

    @Override
    public String toString() {
        return id + " '" + title + "' @ " + venue.name() + " [" + status + "]";
    }

    /** Fluent builder. Validation runs in {@link #build()}. */
    public static final class Builder {
        private String id;
        private String title;
        private String description = "";
        private String category = "General";
        private Venue venue;
        private Instant startsAt;
        private Instant salesOpenAt;
        private final List<Section> sections = new ArrayList<>();
        private EventStatus status = EventStatus.DRAFT;

        private Builder() {
        }

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder title(String value) {
            this.title = value;
            return this;
        }

        public Builder description(String value) {
            this.description = value == null ? "" : value;
            return this;
        }

        public Builder category(String value) {
            this.category = value;
            return this;
        }

        public Builder venue(Venue value) {
            this.venue = value;
            return this;
        }

        public Builder startsAt(Instant value) {
            this.startsAt = value;
            return this;
        }

        public Builder salesOpenAt(Instant value) {
            this.salesOpenAt = value;
            return this;
        }

        public Builder addSection(Section section) {
            this.sections.add(Guard.notNull(section, "section"));
            return this;
        }

        public Builder status(EventStatus value) {
            this.status = value;
            return this;
        }

        public Event build() {
            Guard.notBlank(id, "event.id");
            Guard.notBlank(title, "event.title");
            Guard.notBlank(category, "event.category");
            Guard.notNull(venue, "event.venue");
            Guard.notNull(startsAt, "event.startsAt");
            Guard.notNull(status, "event.status");
            if (salesOpenAt == null) {
                salesOpenAt = Instant.EPOCH;
            }
            Guard.that(salesOpenAt.isBefore(startsAt), "event.salesOpenAt must be before event.startsAt");
            Guard.notEmpty(sections, "event.sections");
            long distinct = sections.stream().map(s -> s.name().toLowerCase()).distinct().count();
            Guard.that(distinct == sections.size(), "event.sections must have unique names");
            return new Event(this);
        }
    }
}
