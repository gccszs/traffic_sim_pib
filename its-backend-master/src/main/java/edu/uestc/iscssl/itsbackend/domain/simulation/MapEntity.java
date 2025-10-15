package edu.uestc.iscssl.itsbackend.domain.simulation;

import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.*;
import java.io.Serializable;

@Table(name = "map")
@Entity
public class MapEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String description;
    private String filePath;

    @Lob
    @ApiModelProperty(value = "地图图片")
    @Column(name = "mapImage",columnDefinition = "text")
    private String mapImage;

    private long ownerId;
    private String mapId;
    private MAP_STATUS status;

    public String getMapId() {
        return mapId;
    }

    public void setMapId(String mapId) {
        this.mapId = mapId;
    }

    public enum MAP_STATUS {
        PUBLIC,PRIVATE,FORBIDDEN;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public MAP_STATUS getStatus() {
        return status;
    }

    public void setStatus(MAP_STATUS status) {
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMapImage() { return mapImage; }

    public void setMapImage(String mapImage) {this.mapImage = mapImage;}
}
