package com.railguard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing bounding box coordinates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoundingBoxDto {
    @JsonProperty("xMin")
    private Double xMin;

    @JsonProperty("yMin")
    private Double yMin;

    @JsonProperty("xMax")
    private Double xMax;

    @JsonProperty("yMax")
    private Double yMax;

    // Getters for lower-case compatibility
    @JsonProperty("xmin")
    public Double getXmin() { return xMin; }

    @JsonProperty("ymin")
    public Double getYmin() { return yMin; }

    @JsonProperty("xmax")
    public Double getXmax() { return xMax; }

    @JsonProperty("ymax")
    public Double getYmax() { return yMax; }
}
