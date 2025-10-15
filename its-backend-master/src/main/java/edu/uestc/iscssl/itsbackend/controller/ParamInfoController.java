package edu.uestc.iscssl.itsbackend.controller;

import edu.uestc.iscssl.itsbackend.service.ParamInfoService;
import edu.uestc.iscssl.itsbackend.utils.ParamInfo;
import edu.uestc.iscssl.itsbackend.utils.XmlUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@CrossOrigin
@RestController
@Api(value = "用户仿真参数读取API",description = "用户仿真参数读取")
public class ParamInfoController {
    @Autowired
    ParamInfoService paramInfoService;

    @ApiOperation(value = "从mongoDB获取仿真參數数据")
    @RequestMapping(value = "/getWebParamDB",method = RequestMethod.GET)
    @RequiresPermissions(logical = Logical.AND, value = {"user:select"})
    public void findParamInfo(@RequestParam(required = true) String simulationId, HttpServletResponse response) throws IOException {
        //List<WebParm> params = webParamService.findParams();
        ParamInfo param = paramInfoService.findParamById(simulationId);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(XmlUtils.Param2Json(param));
        response.flushBuffer();
    }
}
