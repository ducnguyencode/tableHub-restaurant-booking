package com.tablehub.operations.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
    name = "special_schedules",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_special_sched_rest_date", columnNames = {"restaurant_id", "specific_date"})
    }
)
public class SpecialSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "specific_date", nullable = false)
    private LocalDate specificDate;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = false;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(name = "force_deposit", nullable = false)
    private Boolean forceDeposit = false;

    @Column(name = "deposit_override_per_guest")
    private Long depositOverridePerGuest;

    @Column(name = "note", length = 255)
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public LocalDate getSpecificDate() { return specificDate; }
    public void setSpecificDate(LocalDate specificDate) { this.specificDate = specificDate; }

    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }

    public LocalTime getOpensAt() { return opensAt; }
    public void setOpensAt(LocalTime opensAt) { this.opensAt = opensAt; }

    public LocalTime getClosesAt() { return closesAt; }
    public void setClosesAt(LocalTime closesAt) { this.closesAt = closesAt; }

    public Boolean getForceDeposit() { return forceDeposit; }
    public void setForceDeposit(Boolean forceDeposit) { this.forceDeposit = forceDeposit; }

    public Long getDepositOverridePerGuest() { return depositOverridePerGuest; }
    public void setDepositOverridePerGuest(Long depositOverridePerGuest) { this.depositOverridePerGuest = depositOverridePerGuest; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
