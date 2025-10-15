package edu.uestc.iscssl.itsbackend.controller;

import com.alibaba.fastjson.JSONObject;
import edu.uestc.iscssl.common.common.Constant;
import edu.uestc.iscssl.itsbackend.VO.ExperimentVO;
import edu.uestc.iscssl.itsbackend.controller.agent.annotation.ApiJsonObject;
import edu.uestc.iscssl.itsbackend.controller.agent.annotation.ApiJsonProperty;
import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentDataEntity;
import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentEntity;
import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentReportEntity;
import edu.uestc.iscssl.itsbackend.domain.simulation.SimulationEntity;
import edu.uestc.iscssl.itsbackend.service.ExperimentDataService;
import edu.uestc.iscssl.itsbackend.service.ExperimentReportService;
import edu.uestc.iscssl.itsbackend.service.ExperimentService;
import edu.uestc.iscssl.itsbackend.service.SimulationService;
import edu.uestc.iscssl.itsbackend.utils.JwtSentData;
import edu.uestc.iscssl.itsbackend.utils.R;
import edu.uestc.iscssl.itsbackend.utils.Regex;
import edu.uestc.iscssl.itsbackend.utils.UserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static edu.uestc.iscssl.itsbackend.utils.UserUtils.*;

/**
 * 实验内容控制器
 */

@CrossOrigin
@RestController
@Tag(name = "实验管理API", description = "与实验相关的接口")
public class ExperimentController {

    @Autowired
    ExperimentService experimentService;
    @Autowired
    ExperimentReportService experimentReportService;
    @Autowired
    private SimulationService simulationService;
    @Autowired
    private ExperimentDataService experimentDataService;

    @Operation(summary = "插入实验信息及实验报告信息")
    @ResponseBody
    @RequestMapping(value = "/saveExperimentName",method = RequestMethod.POST)
    @RequiresPermissions(logical = Logical.AND, value = {"user:select"})
    public R saveExperimentName(@ApiJsonObject(name = "insert_experiment", value = {
            @ApiJsonProperty(key = "experimentName",example = "实验名",description = "实验名")
            ,@ApiJsonProperty(key = "type",example = "0",description = "0代表仿真实验,1代表对比实验")
    }) @RequestBody Map<String,String> map){
        long id  = UserUtils.getUserId();
        DateFormat df = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        int type = Integer.parseInt(map.get("type"));
        String experimentName = map.get("experimentName");
        if(type==1){
            experimentName = " 于"+df.format(new Date())+"创建的" +map.get("experimentName");
        }
        Map<String, Object> result = new HashMap<>();
        ExperimentReportEntity experimentReportEntity = new ExperimentReportEntity();
        experimentReportEntity.setStatus(0);//设置实验报告初始状态为未填写
        experimentReportEntity.setExperimentName(experimentName);
        experimentReportEntity.setReportid(UUID.randomUUID().toString().substring(0, 20).replaceAll("-",""));
        experimentReportEntity.setExperimentTime(new Date());
        ExperimentReportEntity reportEntity = experimentReportService.insertExperimentReport(experimentReportEntity);

        ExperimentEntity experimentEntity = new ExperimentEntity();
        experimentEntity.setExperimentId(reportEntity.getReportid());
        experimentEntity.setExperimentName(experimentName);
        experimentEntity.setCreateTime(new Date());
        experimentEntity.setReportId(reportEntity.getReportid());
        try {
            experimentService.saveExperiment(experimentEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(getUser().getType() == 2){
            JSONObject param=new JSONObject();
            param.put("username",getUserName());
            //param.put("issuerId","100400");
            param.put("issuerId","100916");
            String json=param.toString();
            int sentStatusId = 0;
            try {
                sentStatusId = JwtSentData.sendData(json,1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (sentStatusId == 7){
                result.put("experimentId",experimentEntity.getExperimentId());
                result.put("msg","已发送到实验空间");
                return R.ok(result);
            }
            else if(sentStatusId>1 && sentStatusId<7){
                result.put("experimentId",experimentEntity.getExperimentId());
                result.put("msg","发送到实验空间失败");
                return R.ok(result);
            }
        }
        result.put("experimentId",experimentEntity.getExperimentId());
        return R.ok(result);
    }
    @Operation(summary = "查看仿真实验列表", description = "分页查看仿真实验列表")
    @ResponseBody
    @RequestMapping(value = "/getUserExperiments",method = RequestMethod.GET)
    public R getUserSimulations(@RequestParam(required = false) String experimentName
            , @RequestParam(required = false, defaultValue = "1") int page
            , @RequestParam(required = false, defaultValue = "20") int limit) {
        List<ExperimentEntity> experimentEntities= new ArrayList<ExperimentEntity>();
        Pageable pageable = PageRequest.of(page-1, limit, Sort.Direction.DESC,"createTime");
        boolean isManager = checkManager();
        int totalPages=-1;
        long totalElement=-1;
        Page<ExperimentEntity> pages =null;
        if (experimentName != null&&!experimentName.equals("")){
            pages = experimentService.findByExperimentNameLike(experimentName,pageable,isManager);
        }
        else if (isManager){
            pages = experimentService.findAll(pageable);
        }else{
            pages=experimentService.findByUserId(UserUtils.getUserId(),pageable);
        }
        if (pages!=null){
            experimentEntities =pages.getContent();
            totalPages=pages.getTotalPages();
            totalElement=pages.getTotalElements();
        }else{
            totalPages=0;
            totalElement=0;
        }
        List<ExperimentVO> experimentVOS = new ArrayList<ExperimentVO>();
        for(ExperimentEntity experimentEntity: experimentEntities){
            SimulationEntity simulationEntity = simulationService.findSimulationById(experimentEntity.getSimId());
            if(simulationEntity != null){
                experimentVOS.add(new ExperimentVO(experimentEntity,simulationEntity));
            }
        }
        Map<String,Object> result=new HashMap<>();
        result.put("experimentList",experimentVOS);
        result.put("totalPages",totalPages);
        result.put("totalElement",totalElement);
        return R.ok(result);
    }

    @Operation(summary = "查找实验仿真数据", description = "通过experiment_id查找仿真数据")
    @GetMapping(value = "/getExperimentData")
    @RequiresPermissions(logical = Logical.AND, value = {"user:select"})
    public void getExperimentSimulation(@RequestParam("experimentId") String experimentId, HttpServletResponse response) throws IOException {
        List<ExperimentEntity> experimentEntities = experimentService.findByExperimentId(experimentId);//即一个实验通过多条记录存储
        if (UserUtils.getUserId() != experimentEntities.get(0).getUserId()){
            response.getWriter().write("你无法访问不属于自己的实验数据");
            response.flushBuffer();
        }
        String simulationId = null;
        if(experimentEntities.size()==0){
            System.out.println(UserUtils.getUserId());
            response.getWriter().write("没有仿真");
        }else{
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{");
            List<List<String>> results = new ArrayList<List<String>>();
            Regex r = new Regex(Regex.GET_STEP_NUMBER);
            for (int i =0; i<experimentEntities.size(); i++) {
                simulationId = experimentEntities.get(i).getSimId();
                List<String> result =Files.lines(Paths.get("C:\\its\\simulationFile\\" + simulationId)).filter(new Predicate<String>() {
                    @Override
                    public boolean test(String s) {
                        int step = Integer.parseInt(r.getStepNumOfLine(s));
                        return true;
                    }
                }).collect(Collectors.toList());
                results.add(result);
            }
            for(int i = 0;i < results.size(); i++){
                List<String> result = results.get(i);
                //response.getWriter().write("\""+"simulationId"+"\":"+"\""+experimentEntities.get(i).getSimId()+"\""+","+"\"");
                response.getWriter().write("\""+experimentEntities.get(i).getSimId()+"\""+":"+"{");
                while (result.size() > 1) {
                    response.getWriter().write("\"" + r.getStepNumOfLine(result.get(0)) + "\":" + result.get(0) + ",");
                    result.remove(0);
                }
                if (result.size() == 1)
                    response.getWriter().write("\"" + r.getStepNumOfLine(result.get(0)) + "\":" + result.get(0)+"}");
                if(i+1 != results.size()){
                    response.getWriter().write(",");
                }
            }
        }
        response.getWriter().write("}");
        response.flushBuffer();
    }
    @Operation(summary = "查找实验仿真图像数据", description = "通过experiment_id查找仿真图像数据")
    @GetMapping(value = "/getExperimentDataImg")
    @RequiresPermissions(logical = Logical.AND, value = {"user:select"})
    public R getExperimentDataImg(@RequestParam("experimentId") String experimentId) {
        Map<String,Object> result = new HashMap<>();
        List<ExperimentEntity> experimentEntities = experimentService.findByExperimentId(experimentId);//即一个实验通过多条记录存储
        if (getUserId() != experimentEntities.get(0).getUserId()){
            result.put("msg","You can't get others' data");
            return R.ok(result);
        }
        ExperimentDataEntity dataEntity = experimentDataService.fingExperimentDataById(experimentId);
        if(dataEntity != null){
            result.put("experimentDataImg",dataEntity);
            result.put("msg","Find ExperimentDataImg Success");
            return R.ok(result);
        }else {
            result.put("msg","Find ExperimentDataImg Failed");
            return R.ok(result);
        }
    }

    @Operation(summary = "根据实验id删除实验数据")
    @DeleteMapping(value = "deleteExperiment")
    public R deleteExperiment(@RequestParam(required = true) String simulationId) {
        Map<String,Object> result = new HashMap<>();
        experimentService.deleteExperimentByExperimentId(simulationId);
        experimentReportService.deleteExperimentReportByReportId(simulationId);
        experimentDataService.deleteExperimentDataByExperimentId(simulationId);
        simulationService.deleteSimulationByReportId(simulationId);
        result.put("msg","delete successful");
        return R.ok(result);
    }
}
