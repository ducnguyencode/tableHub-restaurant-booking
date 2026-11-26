package com.tablehub.operations.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(
    name = "dining_tables",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tables_rest_code", columnNames = {"restaurant_id", "table_code"}),
        @UniqueConstraint(name = "uk_tables_rest_id", columnNames = {"restaurant_id", "id"})
    }
)
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "area_name", nullable = false, length = 100)
    private String areaName;

    @Column(name = "table_code", nullable = false, length = 30)
    private String tableCode;

    @Column(name = "min_party_size", nullable = false)
    private Integer minPartySize = 1;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "is_combinable", nullable = false)
    private Boolean isCombinable = false;

    @Column(name = "pos_x")
    private Integer posX;

    @Column(name = "pos_y")
    private Integer posY;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public String getTableCode() { return tableCode; }
    public void setTableCode(String tableCode) { this.tableCode = tableCode; }

    public Integer getMinPartySize() { return minPartySize; }
    public void setMinPartySize(Integer minPartySize) { this.minPartySize = minPartySize; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Boolean getIsCombinable() { return isCombinable; }
    public void setIsCombinable(Boolean isCombinable) { this.isCombinable = isCombinable; }

    public Integer getPosX() { return posX; }
    public void setPosX(Integer posX) { this.posX = posX; }

    public Integer getPosY() { return posY; }
    public void setPosY(Integer posY) { this.posY = posY; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiningTable that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
