package edu.uestc.iscssl.itsbackend.controller;

import com.alibaba.fastjson.JSONObject;
import edu.uestc.iscssl.itsbackend.VO.ExperimentReportVO;
import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentEntity;
import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentReportEntity;
import edu.uestc.iscssl.itsbackend.domain.user.UserEntity;
import edu.uestc.iscssl.itsbackend.service.ExperimentDataService;
import edu.uestc.iscssl.itsbackend.service.ExperimentReportService;
import edu.uestc.iscssl.itsbackend.service.ExperimentService;
import edu.uestc.iscssl.itsbackend.service.UserService;
import edu.uestc.iscssl.itsbackend.utils.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

import static edu.uestc.iscssl.itsbackend.utils.UserUtils.*;

/**
 *  实验报告控制器
 */
@CrossOrigin
@Controller
@Api(value = "实验报告相关API" ,description = "实验报告相关API")
public class ExperimentReportController {
    @Autowired
    private ExperimentReportService experimentReportService;
    @Autowired
    private ExperimentService experimentService;
    @Autowired
    private ExperimentDataService experimentDataService;
    @Autowired
    private UserService userService;
    /**
     * 查看实验报告列表
     * @return
     */
    @ApiOperation(value = "查看一个实验报告信息",notes = "通过实验id找到对应实验报告信息 但需要将token保存到Headers中验证")
    @GetMapping(value = "/getReport")
    @ResponseBody
    public R selectExperimentReportById(@RequestParam String reportid ){
        Map<String,Object> result = new HashMap<>();
        ExperimentReportEntity reportEntity = experimentReportService.findExperimentReportById(reportid);
        reportEntity.setExperimentEntity(experimentService.findByExperimentId(reportid).get(0));
        int type = reportEntity.getExperimentEntity().getType();
        if(type>10){
            type = type / 10;
        }
        reportEntity.getExperimentEntity().setType(type);
        if(reportEntity != null){
            result.put("report",reportEntity);
            result.put("msg","You are getting experimentReport");
            return R.ok(result);
        }
        return R.error(HttpStatus.NOT_FOUND,"实验报告不存在");
    }

    /**
     * 添加实验报告列表数据
     * @param experimentReportEntity
     * @return
     */
    @ApiOperation(value = "添加实验报告" ,notes = "传入除实验报告序号(id)之外的其他所有参数")
    @PutMapping(value = "/updateReport")
    @ResponseBody
    public R insertExperimentReport(@RequestBody ExperimentReportEntity experimentReportEntity) {
        //修改实验报告
        int modifyStatus = experimentReportService.modifyExperimentReport(experimentReportEntity, experimentReportEntity.getReportid());
        System.out.println("modifyStatus"+modifyStatus);
        experimentReportEntity.setExperimentTime(experimentReportService.findExperimentReportById(experimentReportEntity.getReportid()).getExperimentTime());
        experimentReportEntity.setExperimentData(experimentDataService.fingExperimentDataById(experimentReportEntity.getReportid()));
        Map<String,Object> result = new HashMap<>();
        if(modifyStatus != 0){
            try {
//                String classpath = ResourceUtils.getURL("classpath:").getPath();
                File parent = new File("C:\\its\\pdfFolder");
                if(!parent.exists()){
                    parent.mkdirs();
                }
                String fileName = experimentReportEntity.getReportid();
                String filePath = parent.getPath() + File.separator + fileName + ".pdf";
                PdfModle.createPdf(filePath,experimentReportEntity,UserUtils.getUser());
                //更新实验报告PDF地址
                experimentReportService.updateExperimentReport(fileName,experimentReportEntity.getReportid());
                //报告填写完整以后，发送至实验空间
                int sentResultId = 0;
                if(modifyStatus == 1&&getUser().getType() == 2){
                    int attachmentId = JwtSentData.sendReport(experimentReportEntity.getReportid());
                    long now = new Date().getTime();
                    System.out.println("now=="+now);
                    JSONObject param=new JSONObject();
                    param.put("username",getUserName());
                    param.put("projectTitle","道路交通虚拟仿真教学实验");
                    Integer type = experimentService.findExperimentEntityByExperimentId(experimentReportEntity.getReportid()).getType();
                    String typeName = ExperimentTypeUtils.toType(type);
                    param.put("childProjectTitle",typeName);
                    param.put("status",1);
                    param.put("score",(int)(61+Math.random()*40));
                    param.put("startDate",experimentReportEntity.getExperimentTime().getTime());
                    param.put("endDate",now);
                    long a = (now-experimentReportEntity.getExperimentTime().getTime())/60000;
                    if(a<1)a=1;
                    param.put("timeUsed",a);
                    //param.put("issuerId","100400");
                    param.put("issuerId","100916");
                    param.put("attachmentId",attachmentId);
                    String json=param.toString();
                    sentResultId = JwtSentData.sendData(json,2);
                }
                result.put("msg","Create Success,return sentResultId"+sentResultId);
                return R.ok(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return R.ok("实验报告内容填写不完整，请填写完所有内容");
    }

    /**
     * 删除实验报告列表
     * @param id
     * @return
     */
    @ApiOperation(value = "删除实验报告",notes = "根据实验报告id删除对应的实验报告")
    @DeleteMapping(value = "/deleteReport")
    @ResponseBody
    public R deleteExperimentReport(@RequestParam Integer id) {
        experimentReportService.deleteExperimentReport(id);
        Map<String,Object> result = new HashMap<>();
        result.put("msg","Delete Success");
        return R.ok(result);
    }

    @ApiOperation(value = "分页查看所有实验报告信息",notes = "根据用户id查到所有实验id，再通过实验id查询到所有实验报告信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNumber",value = "当前页数",dataType = "Integer",paramType = "query"),
            @ApiImplicitParam(name = "pageSize",value = "每页显示条数",dataType = "Integer",paramType = "query"),
            @ApiImplicitParam(name = "experimentName",value = "实验名",dataType = "String",paramType = "query")
    })
    @GetMapping(value = "/listReports")
    public R ExperimentReport(@RequestParam(value = "page",defaultValue = "1") Integer page,
                              @RequestParam(value = "limit",defaultValue = "10") Integer limit,
                              @RequestParam(required = false) String experimentName){
        List<String> idList = experimentService.findExperimentId(UserUtils.getUserId());
        List<ExperimentReportEntity> reportEntityS = new ArrayList<ExperimentReportEntity>();
        if(idList.size()==0&&getRoleId()==1){
            System.out.println(UserUtils.getUserId());
            return R.ok("该用户没有进行过实验");
        }
        else {
            int totalPages=-1;
            long totalElement=-1;
            Page<ExperimentReportEntity> pages =null;
            if (experimentName != null && !experimentName.equals("")) {
                pages = experimentReportService.findExperimentReportByIdListAndExperimentName(idList, page, limit, experimentName);
            }else if(checkManager()) {
                pages = experimentReportService.findExperimentReport(page, limit);
            } else {
                pages = experimentReportService.findExperimentReportByIdList(idList, page, limit);
            }
            Map<String, Object> result = new HashMap<>();
            if (pages!=null){
                reportEntityS = pages.getContent();
                totalPages=pages.getTotalPages();
                totalElement=pages.getTotalElements();
            }else{
                totalPages=0;
                totalElement=0;
            }
            List<ExperimentReportVO> experimentReportVOS = new ArrayList<ExperimentReportVO>();
            for(ExperimentReportEntity experimentReportEntity: reportEntityS){
                ExperimentEntity experimentEntity = experimentService.findExperimentEntityByExperimentId(experimentReportEntity.getReportid());
                if(experimentEntity == null){//实验报告中的实验id，在实验记录表中不存在；（理想情况下不会出现）
                    continue;
                }
                if(experimentEntity.getUserId() == null){
                    continue;
                }
/*                if(experimentService.findExperimentEntityByExperimentId(experimentReportEntity.getReportid()).getUserId() == null){
                    continue;
                }*/
                long userId = experimentService.findExperimentEntityByExperimentId(experimentReportEntity.getReportid()).getUserId();
                if((Long)userId == null){
                    totalElement--;
                    continue;
                }
                UserEntity user = userService.selectById(userId);
                if(user == null){
                    totalElement--;
                    continue;
                }
                String userName = userService.selectById(userId).getUserName();
                Integer type = experimentService.findExperimentEntityByExperimentId(experimentReportEntity.getReportid()).getType();
                experimentReportVOS.add(new ExperimentReportVO(experimentReportEntity,userName,type));
            }
            if (reportEntityS.size() > 0) {
                result.put("msg", "You are getting your experimentReports");
                result.put("totalElement", totalElement);
                result.put("totalPages",totalPages);
                result.put("reportList", experimentReportVOS);
                return R.ok(result);
            }
            return R.ok("实验报告不存在");
        }
    }

    @ApiOperation(value = "预览Pdf",notes = "传入实验报告的pfd_address参数，根据此参数预览pdf")
    @ApiImplicitParam(name = "fileName",value = "查看pdf需要的地址，即实验报告的pdf_address属性",dataType = "String",paramType = "path")
    @GetMapping(value = "/viewPdf/{fileName}")
    public void viewPdf(@PathVariable String fileName, HttpServletResponse response){
        try {
//            String classpath = ResourceUtils.getURL("classpath:").getPath();
            String filePath = "C:\\its\\pdfFolder\\" + fileName + ".pdf";
            InputStream in = new FileInputStream(filePath);
            OutputStream out = response.getOutputStream();
            int length = -1;
            byte[] buffer = new byte[1024];
            while((length = in.read(buffer)) != -1){
                out.write(buffer,0,length);
            }
            out.flush();
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };
}
