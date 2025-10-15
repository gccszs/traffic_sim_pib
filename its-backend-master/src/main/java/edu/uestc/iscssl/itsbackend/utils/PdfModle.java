package edu.uestc.iscssl.itsbackend.utils;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;
import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentReportEntity;
import edu.uestc.iscssl.itsbackend.domain.user.UserEntity;
import edu.uestc.iscssl.itsbackend.repository.ExperimentReportRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

public class PdfModle {
    @Autowired
    static
    ExperimentReportRepository experimentReportRepository;
    /**
     * 添加一个简单的模板
     *
     * @param filePath
     * @param user
     * @throws IOException
     */
    public static void createPdf(String filePath, ExperimentReportEntity reportVO, UserEntity user) {

        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(new File(filePath)));
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);
            PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
            Paragraph topTitle = new Paragraph("道路交通虚拟仿真" )
                    .setFontSize(24)
                    .setBold();//设置粗体
            topTitle.setFont(font);
            doc.add(topTitle);
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            Paragraph newTitle = new Paragraph("实 验 报 告")
                    .setFontSize(24)
                    .setBold()
                    .setFont(font);
            doc.add(newTitle);
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            //设置下划线
            doc.add(new Paragraph(" 学         号:  " + reportVO.getStuId()).setFont(font).setFirstLineIndent(200).setUnderline());
            doc.add(new Paragraph(" 姓         名:  " + reportVO.getStuName()).setFont(font).setFirstLineIndent(200).setUnderline());
            doc.add(new Paragraph(" 课程名称:  " + reportVO.getCourseName()).setFont(font).setFirstLineIndent(200).setUnderline());
            doc.add(new Paragraph(" 理论教师:  " + reportVO.getTheoryTeacher()).setFont(font).setFirstLineIndent(200).setUnderline());
            doc.add(new Paragraph(" 实验教师:  " + reportVO.getExperimentTeacher()).setFont(font).setFirstLineIndent(200).setUnderline());

            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));


//            doc.add(new AreaBreak(PageSize.A4.rotate()));//分页
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            PageSize size = pdf.getDefaultPageSize();

            //第二页内容
            doc.add(new Paragraph(user.getInstitution()).setFontSize(24).setBold().setFont(font));
            doc.add(newTitle);

//            //创建多列
//            float offSet = 36;
//            float columnWidth = (PageSize.A4.getWidth() - offSet * 2 + 10) / 3;
//            float columnHeight = (PageSize.A4.getHeight() - offSet * 2);
//            Rectangle[] columns = {
//                    new Rectangle(offSet - 5, offSet, columnWidth, columnHeight),
//                    new Rectangle(offSet + columnWidth, offSet, columnWidth, columnHeight),
//                    new Rectangle(
//                            offSet + columnWidth * 2 + 5, offSet, columnWidth, columnHeight)};
//            doc.setRenderer(new ColumnDocumentRenderer(doc, columns));
            Paragraph secOne = new Paragraph("学生姓名：" + reportVO.getStuName() + "                       学号：" + reportVO.getStuId() + "                        指导老师：" + reportVO.getExperimentTeacher()).setFontSize(14).setFont(font);
            doc.add(secOne);
            Paragraph secSecond = new Paragraph("实验地点：" + reportVO.getExperimentLocation() + "                            实验时间：" + reportVO.getExperimentTime()).setFontSize(14).setFont(font);
            doc.add(secSecond);

            doc.add(new Paragraph("一、实验名称：" + reportVO.getExperimentName()).setFontSize(14).setFont(font));
            doc.add(new Paragraph("二、实验学时：" + " 72 ").setFontSize(14).setFont(font));
            doc.add(new Paragraph("三、实验目的").setFontSize(14).setFont(font));
            doc.add(new Paragraph(reportVO.getExperimentObjective().replaceAll("\\<p>|</p>","")).setFirstLineIndent(28).setFontSize(14).setFont(font));
//            doc.add(new Paragraph("四、实验原理："+reportVO.getExperimentPrinciple()).setFontSize(14).setFont(font));
            //截取图片文件
//            String content = reportVO.getExperimentPrinciple();
////            String[] splitString = content.split("'");//用逗号来框住文件地址
//            String[] splitString = splitString(content);
//            if (splitString.length > 1) {
//                //处理添加图片和分割操作
//                for (int i = 0; i < splitString.length; i++) {
//                    System.out.println("输出截取字符串的值：" + i + splitString[i]);
//                    if (i == 0) {
//                        doc.add(new Paragraph("四、实验原理：" + splitString[i]).setFontSize(14).setFont(font));
//                    }
//                    if (i % 2 == 1) {//展示图片
//                        byte[] decodeString = Base64.getDecoder().decode(splitString[i]);
//                        String pictureAddress = new String(decodeString,"UTF-8");
//                        Image image = new Image(ImageDataFactory.create(pictureAddress));
//                        System.out.println("图片尺寸：" + image.getImageWidth() + ":" + image.getImageHeight());
//                        if (image.getImageWidth() > PageSize.A4.getWidth() || image.getImageHeight() > PageSize.A4.getHeight()) {//图片太大的时候 压缩一下图片
//                            image.scaleAbsolute(PageSize.A4.getWidth(), PageSize.A4.getHeight());//绝对比例
////                            image.scaleToFit(PageSize.A4.getWidth(),PageSize.A4.getHeight());
//                            Paragraph imagePa = new Paragraph("")
//                                    .add(image)
//                                    .setPaddingLeft(-35);//缩进一下 不然不好看
//                            doc.add(imagePa);
//                        } else {
//                            Paragraph imagePa = new Paragraph("")
//                                    .add(image);
//                            doc.add(imagePa);
//                        }
//                        System.out.println("pdf的尺寸：" + PageSize.A4.getWidth() + ":" + PageSize.A4.getHeight());
//                    } else {
//                        //展示字符串
//                        doc.add(new Paragraph(splitString[i]).setFontSize(14).setFont(font));
//                    }
////                    System.out.println("输出截取字符串的值："+i+splitString[i]);
//                }
//            } else {
            //正常处理
            doc.add(new Paragraph("四、实验原理").setFontSize(14).setFont(font));
            doc.add(new Paragraph(reportVO.getExperimentPrinciple().replaceAll("\\<p>|</p>","")).setFirstLineIndent(28).setFontSize(14).setFont(font));
//            }
//            String testPath = "D:\\boder.jpg";
//            String testPath1 = "D:\\image1.jpg";
//            Image image = new Image(ImageDataFactory.create(testPath));
//            Image image1 = new Image(ImageDataFactory.create(testPath1));
//            Paragraph imagePa = new Paragraph("")
//                    .add(image)
//                    .add("这是第二张图片")
//                    .add(image1);//这里加了文字没的效果 只能分开写
//            doc.add(imagePa);
            doc.add(new Paragraph("五、实验内容").setFontSize(14).setFont(font));
            doc.add(new Paragraph(reportVO.getExperimentContent().replaceAll("\\<p>|</p>","")).setFirstLineIndent(28).setFontSize(14).setFont(font));
            //doc.add(new Paragraph("六、实验器材（设备、元器件）："+reportVO.getLaboratoryEquipment()).setFontSize(14).setFont(font));
            doc.add(new Paragraph("六、实验步骤").setFontSize(14).setFont(font));
            doc.add(new Paragraph(reportVO.getExperimentStep().replaceAll("\\<p>|</p>","")).setFirstLineIndent(28).setFontSize(14).setFont(font));

            doc.add(new Paragraph("七、实验数据").setFontSize(14).setFont(font));
            File f = new File("C:/its/image");
            if(!f.exists()){
                f.mkdirs();
            }
            String imgFilePath = f.getPath()+"/"+reportVO.getReportid()+"vehiclestatistical.jpeg";
            System.out.println(f.getPath());
            parseBase64(reportVO.getExperimentData().getVehicleStatistical(),imgFilePath);
            Image image1 = new Image(ImageDataFactory.create(imgFilePath));
            //image1.scaleAbsolute(300,300);
            image1.setMarginLeft(60);
            System.out.println("图片宽度:"+image1.getImageWidth()+",图片高度:"+image1.getImageHeight());
            doc.add(new Paragraph().add(image1));
            doc.add(new Paragraph("图1  车流统计").setFontSize(14).setFont(font));

            imgFilePath = f.getPath()+"/"+reportVO.getExperimentData().getId()+"vehicleparam.jpeg";
            parseBase64(reportVO.getExperimentData().getVehicleParam(),imgFilePath);
            Image image2 = new Image(ImageDataFactory.create(imgFilePath));
            image2.setMarginLeft(60);
            doc.add(new Paragraph().add(image2));
            doc.add(new Paragraph("图2  车辆参数").setFontSize(14).setFont(font));

            imgFilePath = f.getPath()+"/"+reportVO.getExperimentData().getId()+"lineinfo.jpeg";
            parseBase64(reportVO.getExperimentData().getLineInfo(),imgFilePath);
            Image image3 = new Image(ImageDataFactory.create(imgFilePath));
            image3.setMarginLeft(60);
            doc.add(new Paragraph().add(image3));
            doc.add(new Paragraph("图3  排队信息").setFontSize(14).setFont(font));

            imgFilePath = f.getPath()+"/"+reportVO.getExperimentData().getId()+"congestionindex.jpeg";
            parseBase64(reportVO.getExperimentData().getCongestionIndex(),imgFilePath);
            Image image4 = new Image(ImageDataFactory.create(imgFilePath));
            image4.setMarginLeft(60);
            doc.add(new Paragraph().add(image4));
            doc.add(new Paragraph("图4  拥堵指数").setFontSize(14).setFont(font));

            imgFilePath = f.getPath()+"/"+reportVO.getExperimentData().getId()+"parkinfo.jpeg";
            parseBase64(reportVO.getExperimentData().getParkInfo(),imgFilePath);
            Image image5 = new Image(ImageDataFactory.create(imgFilePath));
            image5.setMarginLeft(60);
            doc.add(new Paragraph().add(image5));
            doc.add(new Paragraph("图5  停车信息").setFontSize(14).setFont(font));

            imgFilePath = f.getPath()+"/"+reportVO.getExperimentData().getId()+"trafficcapacity.jpeg";
            parseBase64(reportVO.getExperimentData().getTrafficCapacity(),imgFilePath);
            Image image6 = new Image(ImageDataFactory.create(imgFilePath));
            image6.setMarginLeft(60);
            doc.add(new Paragraph().add(image6));
            doc.add(new Paragraph("图6  通行能力").setFontSize(14).setFont(font));

            doc.add(new Paragraph("八、实验结果分析（含重要数据结果分析和核心代码流程分析）").setFontSize(14).setFont(font));
            doc.add(new Paragraph(reportVO.getExperimentResult().replaceAll("\\<p>|</p>","")).setFirstLineIndent(28).setFontSize(14).setFont(font));
            doc.add(new Paragraph("九、总结心得").setFontSize(14).setFont(font));
            doc.add(new Paragraph(reportVO.getExperimentSummary().replaceAll("\\<p>|</p>","")).setFirstLineIndent(28).setFontSize(14).setFont(font));
            doc.add(new Paragraph("十、对本实验过程及方法、手段的改进建议").setFontSize(14).setFont(font));
            doc.add(new Paragraph(reportVO.getExperimentProposal().replaceAll("\\<p>|</p>","")).setFirstLineIndent(28).setFontSize(14).setFont(font));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
//            doc.add(new Paragraph("评分报告："+" 89 ").setFontSize(14).setFont(font).setPaddingLeft(300));
//            doc.add(new Paragraph("指导教师签字："+reportVO.getTheoryTeacher()).setFontSize(14).setFont(font).setPaddingLeft(280));
            PageSize size1 = pdf.getDefaultPageSize();
            doc.close();
            writer.close();
            pdf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //截取字符串的长度
    public static String[] splitString(String value) {
        String[] result = null;
        result = value.split(",");
        return result;
    }
    public static void parseBase64(String experimentData,String imgFilePath) {
        Base64.Decoder decoder = Base64.getDecoder();
        String baseValue = experimentData.replaceAll(" ", "+");
        try{
        byte[] b = decoder.decode(baseValue.replace("data:image/jpeg;base64,",""));//去除base64中无用的部分
        OutputStream out = null;
            for(int i=0; i<b.length; i++){
                if(b[i]<0){//调整异常数据
                    b[i] += 256;
                }
            }
            out = new FileOutputStream(imgFilePath);
            out.write(b);
            out.flush();
            out.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}