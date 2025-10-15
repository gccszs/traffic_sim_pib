package edu.uestc.iscssl.itsbackend.controller;

import edu.uestc.iscssl.itsbackend.VO.MapVO;
import edu.uestc.iscssl.itsbackend.controller.agent.annotation.ApiJsonObject;
import edu.uestc.iscssl.itsbackend.controller.agent.annotation.ApiJsonProperty;
import edu.uestc.iscssl.itsbackend.domain.simulation.MapEntity;
import edu.uestc.iscssl.itsbackend.domain.user.UserEntity;
import edu.uestc.iscssl.itsbackend.service.MapInfoService;
import edu.uestc.iscssl.itsbackend.service.MapService;
import edu.uestc.iscssl.itsbackend.service.UserService;
import edu.uestc.iscssl.itsbackend.utils.MapInfo;
import edu.uestc.iscssl.itsbackend.utils.R;
import edu.uestc.iscssl.itsbackend.utils.UserUtils;
import edu.uestc.iscssl.itsbackend.utils.XmlUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static edu.uestc.iscssl.itsbackend.utils.XmlUtils.getMapInfo;

@CrossOrigin
@RestController
@Api(value = "地图API",description = "自定义地图数据存储、读取到mongoDB的相关API")
public class MapController {

    @Autowired
    MapInfoService mapInfoService;
    @Autowired
    MapService mapService;
    @Autowired
    UserService userService;

    @ApiOperation(value = "存储map数据到mongoDB",notes = "")
    @PostMapping(path = "/saveMapInfo")
    @RequiresPermissions(logical = Logical.AND, value = {"user:select"})
    public R saveMapInfo(@ApiJsonObject(name = "save_map", value = {
            @ApiJsonProperty(key = "mapFile", example = "R", description = "地图内容"),
            @ApiJsonProperty(key = "name", example = "米字路口", description = "地图名"),
            @ApiJsonProperty(key = "description", example = "描述", description = "地图描述"),
            @ApiJsonProperty(key = "status", example = "0", description = "地图状态:0即公开，1即隐私")
    })@RequestBody Map<String,String> map) throws IOException {

        Map<String,Object> result = new HashMap<>();
        //处理地图字符串，author：chen
        String newStr = String.format("\"%s\"",map.get("mapFile"));
        String path = String.format(System.getProperty("user.dir")+"\\its-backend\\python\\mapmaker.py");
        System.out.println(path+","+newStr);
        String mapPath =  UUID.randomUUID().toString().replace("-","");
        String[] cmds = new String[] {"python",path,newStr,mapPath};
        Process process = Runtime.getRuntime().exec(cmds);
        InputStreamReader in = new InputStreamReader(process.getInputStream());
        LineNumberReader br = new LineNumberReader(in);
        String lineStr;
        while ((lineStr = br.readLine()) != null) {
            System.out.println(lineStr);
        }
        MapEntity mapEntity = new MapEntity();
        mapEntity.setName(map.get("name"));
        mapEntity.setDescription(map.get("description"));
        mapEntity.setOwnerId(UserUtils.getUserId());
        mapEntity.setFilePath("map\\"+mapPath+".xml");
        if (map.get("status").equals("public"))
            mapEntity.setStatus(MapEntity.MAP_STATUS.PUBLIC);
        else
            mapEntity.setStatus(MapEntity.MAP_STATUS.PRIVATE);
        mapEntity.setMapId("null");
        try {
            mapService.saveMap(mapEntity);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpStatus.NOT_IMPLEMENTED,e.getMessage());
        }

        MapInfo mapInfo1 = new MapInfo();
        try {
            MapInfo mapInfo = getMapInfo("map\\"+mapPath+".xml");
            mapInfo.setRoadNum(mapInfo.getMarginalPoints().size());
            mapInfo1 = mapInfoService.addMapInfo(mapInfo);
        } catch (IOException e) {
            e.printStackTrace();
            return R.error(HttpStatus.NOT_IMPLEMENTED,e.getMessage());
        }
        mapEntity.setMapId(mapInfo1.getId().toString());
        MapEntity mapEntity1 = mapService.saveMap(mapEntity);

        result.put("mapId",mapEntity1.getId());
        result.put("status","success");
        return R.ok(result);
    }

    @ApiOperation(value = "预览地图图片")
    @PostMapping(path = "/previewMapInfo")
    @RequiresPermissions(logical = Logical.AND, value = {"user:select"})
    public void previewMapInfo(@ApiJsonObject(name = "preview_map", value = {
            @ApiJsonProperty(key = "mapFile", example = "R", description = "地图内容")
    })@RequestBody Map<String,String> map, HttpServletResponse response) throws IOException {
        String newStr = String.format("\"%s\"",map.get("mapFile"));
        String path = String.format(System.getProperty("user.dir")+"\\its-backend\\python\\mapmaker.py");
        System.out.println(path+","+newStr);
        String mapPath = "test";
        String[] cmds = new String[] {"python",path,newStr,mapPath};
        Process process = Runtime.getRuntime().exec(cmds);
        InputStreamReader in = new InputStreamReader(process.getInputStream());
        LineNumberReader br = new LineNumberReader(in);
        String lineStr;
        while ((lineStr = br.readLine()) != null) {
            System.out.println(lineStr);
        }
        long lines = Files.lines(Paths.get("map\\"+mapPath+".xml")).count();
        if (lines <= 400) {
            response.getWriter().write("failure");
            File file = new File("map\\" + mapPath + ".xml");
            System.out.println("Delete "+mapPath+".xml : "+file.delete());
        }
        else {
            //将xml文件的内容发给前端
            MapInfo mapInfo = getMapInfo("map\\test.xml");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(XmlUtils.MapInfo2Json(mapInfo));
        }
    }

    @ApiOperation(value = "从mongoDB获取map数据")
    @RequestMapping(value = "/getMapInfoDB",method = RequestMethod.GET)
    @RequiresPermissions(logical = Logical.AND, value = {"user:select"})
    public void findMapInfo(@RequestParam(required = true) String mapId,HttpServletResponse response) throws IOException {
        if(mapService.findMapStatusById(mapId)==2) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("该地图不被访问");
            response.flushBuffer();
        }
        long count = mapService.countByMapId(mapId);
        if(mapService.countByMapId(mapId)==0){
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("该地图不存在");
            response.flushBuffer();
        }
        ObjectId mapId1 = new ObjectId(mapId);
        MapInfo mapInfo = mapInfoService.findMapInfoById(mapId1);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(XmlUtils.MapInfo2Json(mapInfo));
        response.flushBuffer();
    }

    @ApiOperation(value = "通过状态删除地图信息 status为0可以公开访问地图，status为1可以私人访问地图，status为2地图被禁用")
    @DeleteMapping("/deleteMap")
    public R deleteMap(@ApiJsonObject(name = "deleteMap",value = {
            @ApiJsonProperty(key = "mapId",example = "1",description = "地图id"),
            @ApiJsonProperty(key = "status",example = "2",description = "访问状态 0为公开 1为私人 2为禁用")
    })@RequestBody Map<String,String> map) {
        String mapId = map.get("mapId");
        int status = Integer.parseInt(map.get("status"));
        try {
            mapService.deleteMapById(mapId,status);
        }catch (Exception e){
            e.printStackTrace();
            return R.error(e.getMessage());
        }
        Map<String,Object> result = new HashMap<>();
        result.put("msg","Delete Success");
        return R.ok(result);
    }

    @ApiOperation(value = "查询该用户自己上传的且没有被禁用的地图信息")
    @GetMapping(path = "/getUserMap")
    @RequiresPermissions(logical = Logical.AND,value = {"user:select"})
    public R findUserMap(@RequestParam(value = "page",defaultValue = "1") Integer page,
                         @RequestParam(value = "limit",defaultValue = "10") Integer limit,
                         @RequestParam(required = false)String mapName){
        long id = UserUtils.getUserId();
        Map<String,Object> result = new HashMap<>();
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.Direction.DESC, "id");
        Page<MapEntity> pages = null;
        int totalPages=-1;
        long totalElement=-1;
        List<MapEntity> mapEntities = new ArrayList<MapEntity>();
        if (mapName == null){
            //取到用户自己上传的地图信息
            pages = mapService.findByOwnerId(id, pageable);
            for (MapEntity mapEntity:pages){
                //根据MapId从MongoDB中读取地图信息
                if (mapEntity.getStatus() != MapEntity.MAP_STATUS.FORBIDDEN) {
                    mapEntities.add(mapEntity);
                }
            }
        }
        else {
            pages = mapService.findByOwnerIdAndMapName(id, mapName, pageable);
            for (MapEntity mapEntity:pages){
                //根据MapId从MongoDB中读取地图信息
                if (mapEntity.getStatus() != MapEntity.MAP_STATUS.FORBIDDEN) {
                    mapEntities.add(mapEntity);
                }
            }
        }
        totalPages = pages.getTotalPages();
        totalElement = pages.getTotalElements();
        result.put("mapList",mapEntities);
        result.put("totalPages",totalPages);
        result.put("totalElement",totalElement);
        return R.ok(result);
    }

    @ApiOperation(value = "查询该用户自己上传的地图信息")
    @GetMapping(path = "/getPublicMap")
    @RequiresPermissions(logical = Logical.AND,value = {"user:select"})
    public R getPublicMap(@RequestParam(value = "page",defaultValue = "1") Integer page,
                           @RequestParam(value = "limit",defaultValue = "10") Integer limit,
                           @RequestParam(required = false) String mapName) {
        Map<String, Object> result = new HashMap<>();
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.Direction.DESC, "id");
        Page<MapEntity> pages = null;
        int totalPages=-1;
        long totalElement=-1;
        List<MapEntity> mapEntities = new ArrayList<MapEntity>();
        if (mapName == null) {
            //取到属于自己的以及公有的所有Map信息
            pages = mapService.findByStatus(MapEntity.MAP_STATUS.PUBLIC, pageable);
            for (MapEntity mapEntity : pages) {
                //根据MapId从MongoDB中读取地图信息
                mapEntities.add(mapEntity);
            }
        }
        else {
            pages = mapService.findByStatusAndMapName(MapEntity.MAP_STATUS.PUBLIC, mapName, pageable);
            for (MapEntity mapEntity : pages){
                mapEntities.add(mapEntity);
            }
        }
        totalPages = pages.getTotalPages();
        totalElement = pages.getTotalElements();
        result.put("mapList",mapEntities);
        result.put("totalPages",totalPages);
        result.put("totalElement",totalElement);
        return R.ok(result);
    }

    @ApiOperation(value = "查询该用户自己上传的地图信息")
    @GetMapping(path = "/getAllMap")
    @RequiresPermissions(logical = Logical.AND,value = {"user:select"})
    public R getAllMap(@RequestParam(value = "page",defaultValue = "1") Integer page,
                       @RequestParam(value = "limit",defaultValue = "10") Integer limit,
                       @RequestParam(required = false) String mapName) {
        long roleId = UserUtils.getRoleId();
        Map<String, Object> result = new HashMap<>();
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.Direction.DESC, "id");
        Page<MapEntity> pages = null;
        int totalPages=-1;
        long totalElement=-1;
        List<MapVO> mapVOS = new ArrayList<MapVO>();
        List<MapEntity> mapEntities = new ArrayList<MapEntity>();
        if (roleId != 2)
            return R.error(HttpStatus.FORBIDDEN,"该用户无此权限");

        if (mapName == null) {
//            String userName = userService.selectById(2).getUserName();
            pages = mapService.getAllMapsVO(pageable);
            mapEntities = pages.getContent();
            for (MapEntity entity:mapEntities){
                UserEntity user = userService.selectById(entity.getOwnerId());
                if(user == null){
                    continue;
                }
                String userName = user.getUserName();
                mapVOS.add(new MapVO(entity,userName));
            }
        }
        else {
            pages = mapService.getMapsVOByMapName(mapName, pageable);
            mapEntities = pages.getContent();
            for (MapEntity entity:mapEntities){
                String userName = userService.selectById(entity.getOwnerId()).getUserName();
                mapVOS.add(new MapVO(entity,userName));
            }
        }
        totalPages = pages.getTotalPages();
        totalElement = pages.getTotalElements();

        result.put("mapList",mapVOS);
        result.put("totalPages",totalPages);
        result.put("totalElement",totalElement);
        return R.ok(result);
    }

    @ApiOperation(value = "根据地图id查询地图信息")
    @GetMapping(path = "/getUserMapInfo")
    @RequiresPermissions(logical = Logical.AND,value = {"user:select"})
    public void findUserMapInfo(@RequestParam(required = true) String mapId,HttpServletResponse response) throws IOException {
        //取到属于自己的以及公有的所有Map信息
        MapEntity mapEntity = mapService.findMapEntityByMapId(mapId);
        long id = UserUtils.getUserId();
        if (mapEntity.getOwnerId() == id || mapEntity.getStatus() == MapEntity.MAP_STATUS.PUBLIC){
            //根据MapId从MongoDB中读取地图信息
            MapInfo mapInfo = null;
            try {
                ObjectId mapId1 = new ObjectId(mapEntity.getMapId());
                mapInfo = mapInfoService.findMapInfoById(mapId1);
            } catch (Exception e) {
                response.getWriter().write("invalid hexadecimal representation of an Id:"+mapEntity.getMapId());
                e.printStackTrace();
            }
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(XmlUtils.MapInfo2Json(mapInfo));
            response.flushBuffer();
        }
        else {
            response.getWriter().write("You do not have access to this map information.");
            response.flushBuffer();
        }

    }

    @ApiOperation(value = "用户删除自己上传的地图")
    @RequestMapping(value = "/deleteUserMap", method = RequestMethod.DELETE)
    @RequiresPermissions(logical = Logical.AND,value = {"user:select"})
    public R deleteUserMap(@RequestParam(required = true) String id){
        long ownerId = UserUtils.getUserId();
        Map<String,Object> result = new HashMap<>();
        MapEntity mapEntity = mapService.findMapEntitiesById(Integer.parseInt(id));
        if (mapEntity.getOwnerId() == ownerId){
            mapEntity.setStatus(MapEntity.MAP_STATUS.FORBIDDEN);
            mapService.saveMap(mapEntity);
            result.put("msg","deleteMap Successful.");
        }
        else {
            result.put("msg","The Map isn`t belonging to you.");
        }
        return R.ok(result);
    }

    @ApiOperation(value = "用户根据地图名字搜索自己能看到的地图")
    @RequestMapping(value = "/getMapByMapName",method = RequestMethod.GET)
    @RequiresPermissions(logical = Logical.AND,value = {"user:select"})
    public R getMapByName(@RequestParam(required = true) String mapName, HttpServletResponse response){
        long id  = UserUtils.getUserId();
        List<MapEntity> mapEntities = mapService.findByMapName(mapName);
        Map<String,Object> result = new HashMap<>();
        for (MapEntity mapEntity:mapEntities){
            //根据MapId从MongoDB中读取地图信息
            if (mapEntity.getOwnerId() == id || mapEntity.getStatus() == MapEntity.MAP_STATUS.PUBLIC)
                result.put(String.valueOf(mapEntity.getId()),mapEntity);
        }
        return R.ok(result);
    }

    @ApiOperation(value = "存储Map地图的图片")
    @RequestMapping(value = "/saveMapImage",method = RequestMethod.POST)
    @RequiresPermissions(logical = Logical.AND,value = {"user:select"})
    public R saveMapImage(@RequestBody Map<String,Object> map){
        MapEntity mapEntity = mapService.findMapEntitiesById((Integer) map.get("id"));
        mapEntity.setMapImage((String) map.get("image"));
        mapService.saveMap(mapEntity);
        Map<String,Object> result = new HashMap<>();
        result.put("msg","图片存储成功！");
        return R.ok(result);
    }
}
