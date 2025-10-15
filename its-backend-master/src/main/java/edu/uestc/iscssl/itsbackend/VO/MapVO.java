package edu.uestc.iscssl.itsbackend.VO;

import edu.uestc.iscssl.itsbackend.domain.simulation.MapEntity;

import java.io.Serializable;

public class MapVO implements Serializable {
    private int mapId;
    private String name;
    private String description;
    private String mapImage;
    private String userName;

    public MapVO(MapEntity entity) {
        this.mapId=entity.getId();
        this.description=entity.getDescription();
        this.name=entity.getName();
    }

    public MapVO(MapEntity entity,String userName) {
        this.mapId=entity.getId();
        this.description=entity.getDescription();
        this.name=entity.getName();
        this.mapImage=entity.getMapImage();
        this.userName=userName;
    }


    public int getId() {
        return mapId;
    }

    public void setId(int id) {
        this.mapId = id;
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

    public String getMapImage() { return mapImage; }

    public void setMapImage(String mapImage) {this.mapImage = mapImage;}

    public String getUserName() { return userName; }

    public void setUserName(String userName) {this.userName = userName;}
}
