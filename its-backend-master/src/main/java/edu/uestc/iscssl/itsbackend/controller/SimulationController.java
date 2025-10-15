package edu.uestc.iscssl.itsbackend.controller;

import com.google.gson.Gson;
import edu.uestc.iscssl.common.common.Constant;
import edu.uestc.iscssl.common.common.SIMULATION_STATUS;
import edu.uestc.iscssl.common.params.ODParm;
import edu.uestc.iscssl.common.params.distributionParm;
import edu.uestc.iscssl.common.params.signalParm;
import edu.uestc.iscssl.common.params.webParm;
import edu.uestc.iscssl.itsbackend.DTO.SimulationSubmitDTO;
import edu.uestc.iscssl.itsbackend.VO.SimulationVO;
import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentEntity;
import edu.uestc.iscssl.itsbackend.domain.simulation.SimulationEntity;
import edu.uestc.iscssl.itsbackend.domain.simulation.TrafficFlowGenerateModel;
import edu.uestc.iscssl.itsbackend.service.*;
import edu.uestc.iscssl.itsbackend.utils.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static edu.uestc.iscssl.itsbackend.utils.UserUtils.checkManager;

@CrossOrigin
@Controller
@Tag(name = "引擎管理API", description = "引擎、仿真数据管理")
public class SimulationController {
    @Autowired
    EngineManagerService engineManagerService;
    @Autowired
    MapService mapService;
    @Autowired
    SimulationService simulationService;
    @Autowired
    MapInfoService mapInfoService;
    @Autowired
    ParamInfoService paramInfoService;
    Log logger = LogFactory.getLog(SimulationController.class);
    @Autowired
    ExperimentService experimentService;
    @Autowired
    ExperimentReportService experimentReportService;
    
    // 从配置文件读取仿真文件路径
    @Value("${app.simulation.file.path:./simulationFile}")
    private String simulationFilePath;

    @ResponseBody
    @RequestMapping(value = "/submit",method = RequestMethod.POST)
    public R submitTask(@RequestBody SimulationSubmitDTO dto) throws IOException {
        String experimentId = dto.getExperimentId();
        long userId = dto.getUserId();
        int mapId = dto.getMapId();
        int step = dto.getStep();
        int experimentType = dto.getType();
        String note = dto.getNote();
        
        //将前端的道路车流方案的道路id映射为引擎中的id
        MapInfo mapInfo = XmlUtils.getMapInfo(mapService.getMapFilePath(mapId));
        for (ODParm od : dto.getOd()) {
            //od中的origin修改为对应id的map中的Link_Entry
            od.setOrgin(mapInfo.getMap(od.getOrgin()).getLinkEntity());
            for (distributionParm dis : od.getDist()) {
                //找到mapid与od中每个dist的dest值相等的map，将map的linkExit设置为dest。
                if (mapInfo.getMap(dis.getDest()) != null)
                    dis.setDest(mapInfo.getMap(dis.getDest()).getLinkExit());
                else {
                    logger.warn(new Gson().toJson(dis));
                    throw new ITSException("不存在的dest：" + dis.getDest());
                }
            }
        }
        
        // 设置控制器数量和文件路径
        dto.setControllerNum(mapInfo.getControllerNumber());
        for (signalParm p:dto.getSignal()){
            p.setCrossID(p.getCrossID()-1);
        }
        dto.setFilePath(mapService.getMapFilePath(mapId));
        
        // 转换为webParm供引擎使用
        webParm parms = convertToWebParm(dto);
        parms.setControllerNum(dto.getControllerNum());
        parms.setFilePath(dto.getFilePath());
        ParamInfo paramInfo = new ParamInfo();
        paramInfo.setUserId(userId);
        paramInfo.setWebparm(parms);
        paramInfo.setCreateTime(new Date().toString());
        String simulationId = UUID.randomUUID().toString().substring(0, 20).replace("-","");
        String mapName = mapService.findMapEntitiesById(mapId).getName();
        List<ExperimentEntity> experimentEntity1 = experimentService.findByExperimentId(experimentId);
        if (experimentEntity1.size()==1&&experimentEntity1.get(0).getSimId() == null) {
            simulationId = experimentId;
            experimentEntity1.get(0).setSimId(simulationId);
            experimentEntity1.get(0).setUserId(UserUtils.getUserId());
            experimentEntity1.get(0).setType(experimentType);
            experimentEntity1.get(0).setMapName(mapName);
            experimentService.saveExperiment(experimentEntity1.get(0));
        }else {
            ExperimentEntity experimentEntity2 = new ExperimentEntity();
            experimentEntity2.setExperimentId(experimentId);
            experimentEntity2.setCreateTime(experimentEntity1.get(0).getCreateTime());
            experimentEntity2.setExperimentName(experimentEntity1.get(0).getExperimentName());
            experimentEntity2.setReportId(experimentEntity1.get(0).getReportId());
            experimentEntity2.setSimId(simulationId);
            experimentEntity2.setUserId(UserUtils.getUserId());
            experimentEntity2.setType(experimentType);
            experimentEntity2.setMapName(mapName);
            experimentService.saveExperiment(experimentEntity2);
        }
        String simulationName = experimentEntity1.get(0).getExperimentName();
        paramInfo.setSimulationId(simulationId);
        ParamInfo paramInfo1 = paramInfoService.addParam(paramInfo);
        SimulationEntity entity = new SimulationEntity(simulationId, userId, step, simulationName, note, mapId, UserUtils.getUser().getUserName(), dto.getLaneNum(), paramInfo.getId().toString());
        engineManagerService.submitSimulationEntity(entity, parms);
        Map<String, Object> result = new HashMap<>();
        result.put("msg", "提交仿真任务成功，请等待仿真开始");
        result.put("simulationId", simulationId);
        return R.ok(result);
    }
    @ResponseBody
    @RequestMapping(value = "/getSimulationInfo",method = RequestMethod.GET)
    public R getSimulationInfo(@RequestParam(required = true)String simulationId){
        Map<String, Object> result = new HashMap<>();
        result.put("simulation", simulationService.findSimulationById(simulationId));
        return R.ok(result);
    }

    @ResponseBody
    @RequestMapping(value = "/getTrafficFlowGenerateModel",method = RequestMethod.GET)
    //交通流生成方案id与实现的映射在引擎里面写死了，所以目前这里只能硬编码或者写入数据库
    public R getTrafficFlowGenerateModel() {
        Map<String, Object> result = new HashMap<>();
        result.put("models", TrafficFlowGenerateModel.getModel());
        return R.ok(result);
    }

    public void setStep(int step, String simulationId) {
        engineManagerService.setStep(simulationId, step);
    }
    
    /**
     * 将DTO转换为webParm供引擎使用
     */
    private webParm convertToWebParm(SimulationSubmitDTO dto) {
        webParm parms = new webParm();
        parms.setExperimentId(dto.getExperimentId());
        parms.setUserId((int) dto.getUserId()); // 从long转换为int
        parms.setMapId(dto.getMapId());
        parms.setStep(dto.getStep());
        parms.setType(dto.getType());
        parms.setNote(dto.getNote());
        // 将List转换为数组
        if (dto.getOd() != null) {
            parms.setOd(dto.getOd().toArray(new ODParm[0]));
        }
        if (dto.getSignal() != null) {
            parms.setSignal(dto.getSignal().toArray(new signalParm[0]));
        }
        parms.setLaneNum(dto.getLaneNum());
        parms.setControllerNum(dto.getControllerNum());
        parms.setFilePath(dto.getFilePath());
        return parms;
    }

    @ResponseBody
    @RequestMapping(value = "/stop",method = RequestMethod.POST)
    public R stopTask(@RequestBody Map<String, String> map) {
        String simulationId = map.get("simulationId");
        SimulationEntity simulationEntity = simulationService.findSimulationById(simulationId);
        if(null == simulationEntity){
            logger.error("stop:仿真实验id:" + simulationId + ",停止错误无此仿真实验");
            return new R(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (simulationEntity.getStatus()!=SIMULATION_STATUS.STOP){
            engineManagerService.stop(simulationId);
            //更新simulation表中的step数据
            String simulationFile = simulationFilePath + "/" + simulationId;
            logger.info("读取仿真文件: " + simulationFile);
            long lines = 0;
            try {
                java.io.File file = new java.io.File(simulationFile);
                if (file.exists()) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
                    while (reader.readLine() != null) {
                        lines++;
                    }
                    reader.close();
                }
            } catch (Exception e) {
                logger.error("读取仿真文件行数失败: " + e.getMessage());
                lines = 0;
            }
            simulationEntity = simulationService.findSimulationById(simulationId);
            simulationEntity.setStep((int) lines);//由于上面stop方法已经对仿真记录的相关字段进行了修改,这里仅修改仿真的步数,而不修改其他值
            simulationService.saveSimulation(simulationEntity);
            if(lines<=50){
                experimentReportService.deleteExperimentReportByReportId(simulationId);
                experimentService.deleteExperimentByExperimentId(simulationId);
                return new R(HttpStatus.OK);
            }
            return new R(HttpStatus.OK);
        }else {
            return new R(HttpStatus.GONE);
        }

    }

    @ResponseBody
    @RequestMapping(value = "/getUserSimulations",method = RequestMethod.GET)
    public R getUserSimulations(@RequestParam(required = false) String simulationName
            , @RequestParam(required = false) SIMULATION_STATUS simulationStatus
            , @RequestParam(required = false, defaultValue = "1") int page
            , @RequestParam(required = false, defaultValue = "20") int limit) {
        List<SimulationEntity> simulationEntities=new ArrayList<SimulationEntity>();
        Pageable pageable = PageRequest.of(page-1, limit, Sort.Direction.DESC,"createTime");
        boolean isManager = checkManager();
        int totalPages=-1;
        long totalElement=-1;
        Page<SimulationEntity> pages =null;
        List<SimulationVO> simulationVOS = new ArrayList<SimulationVO>();
        if (simulationName != null&&!simulationName.equals("")){
            if (simulationStatus != null) {
                pages=simulationService.findBySimulationNameLikeAndStatus(simulationName, simulationStatus, pageable, isManager);
            } else {
                pages = simulationService.findBySimulationNameLike(simulationName, pageable,isManager);
            }
        }

         else if (isManager){
            if (simulationStatus != null)
                pages = simulationService.findAllByStatus(simulationStatus, pageable);
            else
                pages = simulationService.findAll(pageable);
        }else{
            pages=simulationService.findByUserId(UserUtils.getUserId(),pageable);
        }
         if (pages!=null){
             //pages.getContent().forEach(simulationEntity -> simulationVOS.add(new SimulationVO(simulationEntity)));
             simulationEntities =pages.getContent();
             for(SimulationEntity simulationEntity: simulationEntities){
                 SimulationVO simulationVO = new SimulationVO();
                 simulationVO.setUserId(simulationEntity.getUserId());
                 simulationVO.setStatus(simulationEntity.getStatus());
                 simulationVO.setStep(simulationEntity.getStep());
                 simulationVO.setEngineManagerId(simulationEntity.getEngineManagerId());
                 simulationVO.setSimulationName(simulationEntity.getSimulationName());
                 simulationVO.setNote(simulationEntity.getNote());
                 simulationVO.setMapId(simulationEntity.getMapId());
                 simulationVO.setMapName(simulationService.getName(simulationEntity.getMapId()));
                 simulationVO.setSimulationId(simulationEntity.getSimulaitionId());
                 simulationVO.setUserName(simulationEntity.getUserName());
                 simulationVO.setCreateTime(simulationEntity.getCreateTime());
                 simulationVO.setLaneNum(simulationEntity.getLaneNum());
                 simulationVO.setId(simulationEntity.getSimulaitionId());
                 simulationVOS.add(simulationVO);
             }
             totalPages=pages.getTotalPages();
             totalElement=pages.getTotalElements();
         }else{
             totalPages=0;
             totalElement=0;
         }
        Map<String,Object> result=new HashMap<>();
        result.put("simulationList",simulationVOS);
        result.put("totalPages",totalPages);
        result.put("totalElement",totalElement);
        return R.ok(result);
    }
    @ResponseBody
    @RequestMapping(value = "/deleteSimulations",method = RequestMethod.DELETE)
    public R deleteSimulations(@RequestBody Map<String,Object> map){
        List<String> simulationIds=(List<String>)map.get("simulationIds");
        simulationService.deleteSimulationById(simulationIds);
        return R.ok("删除成功");
    }
    @ResponseBody
    @RequestMapping(value = "/getMapsList",method = RequestMethod.GET)
    public R getMapList() {
        long userId = UserUtils.getUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("maps", mapService.getMapsVO(userId));
        return R.ok(result);
    }

    @ResponseBody
    @RequestMapping(value = "/getMapInfo",method = RequestMethod.GET)
    public void getMapInfo(@RequestParam(required = true) int mapId, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(mapService.getMapInfo(mapId));
        response.flushBuffer();
    }

    @RequestMapping(value = "/getSimulation",method = RequestMethod.GET)
    public void getSimulation(@RequestParam(required = true) String simulationId,
                              @RequestParam(required = true) int startStep,
                              @RequestParam(required = true) int endStep,
                              HttpServletResponse response) throws IOException {
        if (startStep > endStep) throw new IllegalArgumentException("startStep:" + startStep + ",endStep:" + endStep);
        long lines = Files.lines(Paths.get("C:\\its\\simulationFile\\" + simulationId)).count();
        //当仿真实验达到最大步数时，更新simulation表数据
        if (lines >= 500){
            SimulationEntity simulationEntity = simulationService.findSimulationById(simulationId);
            simulationEntity.setStep((int)lines);
            simulationService.saveSimulation(simulationEntity);
        }
        logger.debug("-----------------------endStep:"+endStep+",lines:" + lines);
        //如果剩余内容较少，则增加仿真步数
        if ((endStep >= lines) || endStep+1 < lines && (endStep - startStep+1) > (lines - endStep))
            if(endStep <= 500 &&(lines+endStep-startStep+1) <= 500)
                setStep(endStep - startStep + 1, simulationId);
            else if ((startStep-1) < 500 && (lines+endStep-startStep+1) > 500)
                setStep((int) (500 - lines + 1), simulationId);
//        if ((endStep >= lines) || endStep < lines && (endStep - startStep) > (lines - endStep))
//            setStep(endStep - startStep, simulationId);


        Regex r = new Regex(Regex.GET_STEP_NUMBER);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{");
        List<String> results = Files.lines(Paths.get("C:\\its\\simulationFile\\" + simulationId)).filter(s -> {
            int step = Integer.parseInt(r.getStepNumOfLine(s));
            if (step >= (startStep - 1) && step <= endStep)
                return true;
            return false;
        }).collect(Collectors.toList());
        logger.info(results.size());
        while (results.size() > 1) {
            response.getWriter().write("\"" + r.getStepNumOfLine(results.get(0)) + "\":" + results.get(0) + ",");
            results.remove(0);
        }
        if (results.size() == 1)
            response.getWriter().write("\"" + r.getStepNumOfLine(results.get(0)) + "\":" + results.get(0));
        response.getWriter().write("}");
        response.flushBuffer();

    }
    @ResponseBody
    @RequestMapping(value = "/changeSignalPlan",method = RequestMethod.POST)
    public R changeSignalPlan(@RequestBody Map<String,Object> map) {
        int crossId=(int)map.get("crossId")-1;
        int cycleTime=(int)map.get("cycleTime");
        int ewStraight=(int)map.get("ewStraight");
        int snStraight=(int)map.get("snStraight");
        int snLeft = (int)map.get("snLeft");
        String simulationId=(String)map.get("simulationId");
        engineManagerService.changeSignalPlan(simulationId,crossId,cycleTime,ewStraight,snStraight,snLeft);
        return  R.ok("更新成功");
    }
    @ResponseBody
    @RequestMapping(value = "/setGeneratingModel",method = RequestMethod.POST)
    public R SetVehicleGeneratingModel(@RequestBody Map<String,Object> map) throws IOException {
        String simulationId=(String)map.get("simulationId");
        int roadId=(int)map.get("roadId");
        String mapPath=mapService.getMapFilePath(simulationService.findSimulationById(simulationId).getMapId());
        MapInfo mapInfo = XmlUtils.getMapInfo(mapPath);
        int linkId=mapInfo.getMap(roadId).getLinkEntity();
        int model=(int)map.get("model");
        int demand=(int)map.get("demand");
        double extraParams=(Integer)map.get("extraParams");

        try {
            engineManagerService.SetVehicleGeneratingModel(simulationId,linkId,model,demand,extraParams);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return R.ok("更新成功");
    }
    @ResponseBody
    @RequestMapping(value = "/setFollowingModel",method = RequestMethod.POST)
    public R SetVehicleFollowingModel(@RequestBody Map<String,Object> map){
        int modelId=(int)map.get("modelId");
        String simulationId=(String)map.get("simulationId");
        try {
            engineManagerService.SetVehicleFollowingModel(simulationId,modelId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return R.ok("更新成功");
    }
}
