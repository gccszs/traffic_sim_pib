package edu.uestc.iscssl.itsbackend.utils;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.List;

/**
 *引擎提供的地图xml文件格式不是标准格式，因此在使用xml解析的时候会遇到各种问题。
 * 这里使用的jackson会根据属性和属性的setter来注入值。以Demand为例，我们创建了一个demand属性，同时也创建了demand的集合demands，
 * 程序解析的时候，遇见"Demand"确认Map中有demand和setDemand，于是调用setDemand进行注入，由于xml中有多个demand，所以setter的实现
 * 实际上是将demand放入demands中。
 *
 * 建议有空可以将xml信息转入数据库存储
 */
@Document
public class MapInfo {
    @Id
    ObjectId id;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    int roadNum;

    int controllerNumber;

    public int getControllerNumber() {
        return controllerNumber;
    }

    public void setControllerNumber(int controllerNumber) {
        this.controllerNumber = controllerNumber;
    }

    public int getRoadNum() {
        return roadNum;
    }

    public void setRoadNum(int roadNum) {
        this.roadNum = roadNum;
    }

    @JacksonXmlProperty(localName = "Demand")
    Demand demand;
    List<Demand> demands=new ArrayList<>();
    @JacksonXmlProperty(localName = "MarginalPoint")
    MarginalPoint marginalPoint;
    List<MarginalPoint> marginalPoints=new ArrayList<>();
    @JacksonXmlProperty(localName = "Cross")
    Cross cross;
    List<Cross> crosses=new ArrayList<>();
    @JacksonXmlProperty(localName = "Link")
    Link link;
    List<Link> links=new ArrayList<>();
    @JacksonXmlProperty(localName = "Lane")
    Lane lane;
    List<Lane> lanes=new ArrayList<>();
    @JacksonXmlProperty(localName = "Controller")
    Controller controller;
    List<Controller> controllers=new ArrayList<>();
    @JacksonXmlProperty(localName = "Baseline")
    Baseline baseline;
    List<Baseline> baselines=new ArrayList<>();
    @JacksonXmlProperty(localName = "FixedODMap")
    FixedODMap fixedODMap;
    @JacksonXmlProperty(localName = "RoadMap")
    RoadMap map;

    //List<FixedODMap> fixedODMaps=new ArrayList<>();


    public List<Cross> getCrosses() {
        return crosses;
    }

    public void setCrosses(List<Cross> crosses) {
        this.crosses = crosses;
    }

    public MapInfo() {
    }

    public MapInfo(Demand demand) {
        this.demands.add(demand);
    }

    public void setDemand(Demand demand){
        this.demands.add(demand);
    }
    public void setMarginalPoint(MarginalPoint marginalPoint){
        this.marginalPoints.add(marginalPoint);
    }
    public void setLink(Link link){
        this.links.add(link);
    }
    public void setLane(Lane lane){
        this.lanes.add(lane);
    }
    public void setCross(Cross cross){
        this.crosses.add(cross);
    }
    public void setController(Controller controller){
        this.controllers.add(controller);
    }
    public void setBaseline(Baseline baseline){
        this.baselines.add(baseline);
    }

    public List<MarginalPoint> getMarginalPoints() {
        return marginalPoints;
    }

    public List<RoadMap.Map> getMaps() {
        return map.getMaps();
    }
    public RoadMap.Map getMap(int roadId){
        for ( RoadMap.Map map:getMaps()){
            if (map.getRoadId()==roadId)
                return map;
        }
        return null;
    }
    class Demand{
        @JacksonXmlProperty(localName = "Time")
        float time;
        @JacksonXmlProperty(localName = "Value")
        int value;

        public Demand() {
        }

        public float getTime() {
            return time;
        }

        public void setTime(float time) {
            this.time = time;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
    class MarginalPoint{
        @JacksonXmlProperty(localName = "Road_ID")
        int  Road_ID;
        @JacksonXmlProperty(localName = "Object_ID")
        int  Object_ID;
        @JacksonXmlProperty(localName = "Object_Type")
        String Object_Type;
        @JacksonXmlProperty(localName = "Object_Label")
        String Object_Label;
        @JacksonXmlProperty(localName = "x")
        int x;
        @JacksonXmlProperty(localName = "y")
        int y;

        public MarginalPoint() {
        }

        public int getRoad_ID() {
            return Road_ID;
        }

        public void setRoad_ID(int road_ID) {
            Road_ID = road_ID;
        }

        public int getObject_ID() {
            return Object_ID;
        }

        public void setObject_ID(int object_ID) {
            Object_ID = object_ID;
        }

        public String getObject_Type() {
            return Object_Type;
        }

        public void setObject_Type(String object_Type) {
            Object_Type = object_Type;
        }

        public String getObject_Label() {
            return Object_Label;
        }

        public void setObject_Label(String object_Label) {
            Object_Label = object_Label;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }
    class Cross{
        @JacksonXmlProperty(localName = "Cross_Type")
        int Cross_Type;
        @JacksonXmlProperty(localName = "Object_ID")
        int Object_ID;
        @JacksonXmlProperty(localName = "Object_Type")
        String Object_Type;
        @JacksonXmlProperty(localName = "Object_Label")
        String Object_Label;
        @JacksonXmlProperty(localName = "Cross_Id")
        int Cross_Id;
        @JacksonXmlProperty(localName = "Cross_Radius")
        float Cross_Radius;
        @JacksonXmlProperty(localName = "Connected_Segment_Number")
        int Connected_Segment_Number;
        @JacksonXmlProperty(localName = "x")
        int x;
        @JacksonXmlProperty(localName = "y")
        int y;

        public Cross() {
        }

        public int getCross_Type() {
            return Cross_Type;
        }

        public void setCross_Type(int cross_Type) {
            Cross_Type = cross_Type;
        }

        public int getObject_ID() {
            return Object_ID;
        }

        public void setObject_ID(int object_ID) {
            Object_ID = object_ID;
        }

        public String getObject_Type() {
            return Object_Type;
        }

        public void setObject_Type(String object_Type) {
            Object_Type = object_Type;
        }

        public String getObject_Label() {
            return Object_Label;
        }

        public void setObject_Label(String object_Label) {
            Object_Label = object_Label;
        }

        public int getCross_Id() {
            return Cross_Id;
        }

        public void setCross_Id(int cross_Id) {
            Cross_Id = cross_Id;
        }

        public float getCross_Radius() {
            return Cross_Radius;
        }

        public void setCross_Radius(float cross_Radius) {
            Cross_Radius = cross_Radius;
        }

        public int getConnected_Segment_Number() {
            return Connected_Segment_Number;
        }

        public void setConnected_Segment_Number(int connected_Segment_Number) {
            Connected_Segment_Number = connected_Segment_Number;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

    }
    class Link{
        @JacksonXmlProperty(localName = "Object_ID")
        int Object_ID;
        @JacksonXmlProperty(localName = "Object_Type")
        String Object_Type;
        @JacksonXmlProperty(localName = "Object_Label")
        String Object_Label;
        @JacksonXmlProperty(localName = "Road_ID")
        int Road_ID;
        @JacksonXmlProperty(localName = "Lane_Number")
        int Lane_Number;
        @JacksonXmlProperty(localName = "Guidance_or_Not")
        int Guidance_or_Not;
        @JacksonXmlProperty(localName = "Detector_Location")
        Detector_Location detector_location;
        @JacksonXmlProperty(localName = "Link_Start")
        Link_Start link_start;
        @JacksonXmlProperty(localName = "Link_End")
        Link_End link_end;
        @JacksonXmlProperty(localName = "Is_Origin")
        int Is_Origin;
        @JacksonXmlProperty(localName = "Is_Dest")
        int Is_Dest;
        @JacksonXmlProperty(localName = "Is_Curve")
        int Is_Curve;
        @JacksonXmlProperty(localName = "Limited_Speed")
        float Limited_Speed;
        @JacksonXmlProperty(localName = "Path_ID")
        int Path_ID;

        public Link() {
        }

        class Detector_Location{
            public Detector_Location() {
            }
            @JacksonXmlProperty(localName = "One")
            int One;
            @JacksonXmlProperty(localName = "Two")
            int Two;
            @JacksonXmlProperty(localName = "Three")
            int Three;
        }
        class Link_Start{
            public Link_Start() {
            }
            @JacksonXmlProperty(localName = "Object_Type")
            String Object_Type;
            @JacksonXmlProperty(localName = "Object_ID")
            int Object_ID;
        }
        class Link_End{
            public Link_End() {
            }
            @JacksonXmlProperty(localName = "Object_Type")
            String Object_Type;
            @JacksonXmlProperty(localName = "Object_ID")
            int Object_ID;
        }
    }
    class Lane{
        public Lane() {
        }

        @JacksonXmlProperty(localName = "Link_ID")
        int Link_ID;
        @JacksonXmlProperty(localName = "Lane_ID")
        int Lane_ID;
        @JacksonXmlProperty(localName = "Left_Turn")
        int Left_Turn;
        @JacksonXmlProperty(localName = "Straight_Turn")
        int Straight_Turn;
        @JacksonXmlProperty(localName = "Right_Turn")
        int Right_Turn;
    }
    class Controller{
        @JacksonXmlProperty(localName = "Cross_ID")
        int Cross_ID;
        @JacksonXmlProperty(localName = "Cycle_Time")
        int Cycle_Time;
        @JacksonXmlProperty(localName = "Phase_Number")
        int Phase_Number;
        @JacksonXmlProperty(localName = "Phase")
        Phase phase;
        List<Phase> phases=new ArrayList<>();

        public void setPhase(Phase phase) {
            this.phases.add(phase);
        }

        public Controller() {
        }

        class Phase{
            @JacksonXmlProperty(localName = "Phase_ID")
            int Phase_ID;
            @JacksonXmlProperty(localName = "Direction")
            int Direction;
            @JacksonXmlProperty(localName = "Green_Percent")
            float Green_Percent;
            @JacksonXmlProperty(localName = "Green_Start_Time_Percent")
            float Green_Start_Time_Percent;
            @JacksonXmlProperty(localName = "Connect_Link_ID")
            Connect_Link_ID connect_link_id;

            public Phase() {
            }

            class Connect_Link_ID{
                @JacksonXmlProperty(localName = "A")
                int A;
                @JacksonXmlProperty(localName = "B")
                int B;

                public Connect_Link_ID() {
                }
            }
        }
    }
    class RoadMap{
        @JacksonXmlProperty(localName = "Map")
        Map map;
        List<Map> maps=new ArrayList<>();
        public RoadMap(){
        }
        public List<Map> getMaps() {
            return maps;
        }

        public void setMap(Map map) {
            this.maps.add(map);
        }

        public class Map{
            public Map() {
            }

            @JacksonXmlProperty(localName = "Road_ID")
             int roadId;
            @JacksonXmlProperty(localName = "Link_Entry")
             int linkEntity;
            @JacksonXmlProperty(localName = "Link_Exit")
            int linkExit;

             public int getRoadId() {
                 return roadId;
             }

             public void setRoadId(int roadId) {
                 this.roadId = roadId;
             }

             public int getLinkEntity() {
                 return linkEntity;
             }

             public void setLinkEntity(int linkEntity) {
                 this.linkEntity = linkEntity;
             }

             public int getLinkExit() {
                 return linkExit;
             }

             public void setLinkExit(int linkExit) {
                 this.linkExit = linkExit;
             }
         }
    }

    class Baseline{
        public Baseline() {
        }

        @JacksonXmlProperty(localName = "Path_ID")
        int Path_ID;
        @JacksonXmlProperty(localName = "Point_Count")
        int Point_Count;
        @JacksonXmlProperty(localName = "Points")
        String Points;
    }
    class FixedODMap{
        @JacksonXmlProperty(localName = "OD")
        OD od;
        List<OD> ods=new ArrayList<>();

        public FixedODMap() {
        }

        public void setOd(OD od) {
            this.ods.add(od);
        }

        class OD{
            @JacksonXmlProperty(localName = "Origin_Link_ID")
            int Origin_Link_ID;
            @JacksonXmlProperty(localName = "Demand")
            int Demand;
            @JacksonXmlProperty(localName = "Dest")
            Dest dest;
            List<Dest> dests=new ArrayList<>();

            public OD() {
            }

            public void setDest(Dest dest) {
                this.dests.add(dest);
            }

            class Dest{
                public Dest() {
                }
                @JacksonXmlProperty(localName = "Dest_Link_ID")
                int Dest_Link_ID;
                @JacksonXmlProperty(localName = "Probability")
                float Probability;
            }
        }
    }
}
