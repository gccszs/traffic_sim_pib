'''
Created on 2015-8-7

@author: Jone
'''

import string
import sys  # add 18.01.08
import operator # add 18.11.21
from pathlib import Path
#用于禁止print输出
import contextlib
import io

# 获取当前脚本的绝对路径，然后找到项目根目录
current_dir = Path(__file__).parent
project_root = current_dir.parent

# 添加到Python路径
if str(project_root) not in sys.path:
    sys.path.insert(0, str(project_root))
if str(current_dir) not in sys.path:
    sys.path.insert(0, str(current_dir))

# 或者如果road.py在同一目录
from road import *
from cross import *

class NetDocTool:
    def __init__(self, net):
        self.net = net

    def loadNetTXT(self, path):
        # crsnum = 0
        # linknum = 0
        # marnum = 0
        nettxt = open(path)
        # k:crid v:point
        cross_pt_dict = {}
        # 解析txt文件
        for line in nettxt.readlines():
            # 以行划分
            darr = line.split(',')
            # 根据第一位标签判断是否是交叉口
            if darr[0] == 'C':
                # 获取交叉口对象
                cr = Cross(self.net)
                # 赋值交叉口id
                cr.id = darr[1]  # 交叉口id，第二位
                # 赋值交叉口类型
                cr.type = int(darr[2])
                cr.lanecnt = int(darr[3])# 路口连接的道路数，第四位
                # 连接的道路,西东北南
                cr.west = darr[4]  # 西边连接的道路id，第五位
                if cr.west == ' ':  # 0则表示西边未连接道路
                    cr.west = '-'
                cr.est = darr[5]  # 东，第六位
                if cr.est == ' ':
                    cr.est = '-'
                cr.north = darr[6]  # 北，第七位
                if cr.north == ' ':
                    cr.north = '-'
                cr.south = darr[7]  # 南，第八位
                if cr.south == ' ':
                    cr.south = '-'
                cr.five = darr[8]  # 五向路口，第九位
                if cr.five == ' ':  # 非五向路口
                    cr.five = '-'
                elif 0 == cr.type or 2 == cr.type:  # 环形四路口
                    cr.lanecnt = 4
                elif 1 == cr.type:  # T路口
                    cr.lanecnt = 3
                elif 3 == cr.type:  # 五路口
                    cr.lanecnt = 5
                cr.anchorcnt = int(darr[9])  # 交叉口点数总数，第十位
                # idx = 0
                # for i in range(0,cr.anchorcnt):
                #     idx = i*3
                cx = int(darr[10])
                cy = int(darr[11])
                cz = int(darr[12])
                # cr.anchors.append(pt)
                # cx, cy, cz = 0.0, 0.0, 0.0
                # for pt in cr.anchors:                         #计算中心点
                #     cx += pt[0]
                #     cy += pt[1]
                #     cz += pt[2]
                # cx /= cr.anchorcnt
                # cy /= cr.anchorcnt
                # cz /= cr.anchorcnt
                cr.center = (cx, cy, cz)
                cross_pt_dict[int(cr.id)] = [cx, cy]
                if 2 == cr.type or 3 == cr.type:
                    cr.radius = int(darr[-1])  # 交叉口半径，最后一位
                # 给交叉口list中添加交叉口id
                self.net.crosslist.append(cr.id)
                # map集合中添加进交叉口全部信息
                self.net.netmap[cr.id] = cr
                # cm.debug_print('Starting to parsing network text file.')

        print("交叉点字典:", cross_pt_dict)

        nettxt = open(path)
        # 重新读txt，读取R的信息
        for line in nettxt.readlines():
            # 分行读
            darr = line.split(',')
            # 根据标签R确定路的信息
            if darr[0] == 'R':  # road R表示是道路
                rd = Road(self.net)  # road.py中的Road类
                rd.id = darr[1]  # road的id
                rd.leftcross = darr[2]  # road的左交叉口
                if operator.eq(rd.leftcross, ' '):# 如果左交叉口为空，则加上符号-
                    rd.leftcross = '-'
                rd.rightcross = darr[3]# 同上，右交叉口
                if operator.eq(rd.rightcross, ' '):
                    rd.rightcross = '-'
                rd.anchorcnt = int(darr[4])# 路的点数
                if rd.anchorcnt == 2:
                    rd.tp = 's'  # straight road
                else:
                    rd.tp = 'c'  # curve road

                idx = 0
                # k:point v:distance
                pt_dis_dict = {}
                # 以点来循环
                for i in range(0, rd.anchorcnt):
                    # 三位一组
                    idx = i * 3
                    pt = (int(darr[5 + idx]),
                          int(darr[6 + idx]),
                          int(darr[7 + idx]))
                    # 记录点的坐标
                    rd.anchors.append(pt)
                    # 计算距离，记录找出最长的那个点作为边缘点
                    if rd.leftcross != '-':
                       point = cross_pt_dict[int(rd.leftcross)]
                       d = (int(darr[5 + idx])-int(point[0]))**2+(int(darr[6 + idx])-int(point[1]))**2
                       pt_dis_dict[pt[0], pt[1]] = d
                    if rd.rightcross != '-':
                       point = cross_pt_dict[int(rd.rightcross)]
                       d = (int(darr[5 + idx])-int(point[0]))**2+(int(darr[6 + idx])-int(point[1]))**2
                       pt_dis_dict[pt[0], pt[1]] = d

                print("本条路的距离列表:", pt_dis_dict)
                pt_dis_2_dict = []
                pt_dis_2_dict.append(pt_dis_dict[(rd.anchors[0][0], rd.anchors[0][1])])
                pt_dis_2_dict.append(pt_dis_dict[(rd.anchors[-1][0], rd.anchors[-1][1])])
                # 找出距离最远的值
                max_d = max(pt_dis_2_dict)
                print(pt_dis_2_dict)
                pt_dis_2_dict.clear()
                print("最大值为:", max_d)

                # 通过值反寻键，并以此点为边缘点(距离交叉点最远)
                mar_pt = [k for k, v in pt_dis_dict.items() if v == max_d]

                print(rd.id, "号路的边缘点:", mar_pt)

                if rd.leftcross == '-' or rd.rightcross == '-':# 如果左/右交叉口为空，
                    mar = MarginalPt()# 边缘点
                    mar.id = '%s' % rd.id
                    if rd.leftcross == '-':#左交叉口为空
                        mar.pos = mar_pt[0]
                    elif rd.rightcross == '-':
                        mar.pos = mar_pt[0]
                    # 将边缘点加入边缘点列表中
                    self.net.marginallist.append(mar)  # MarginalPt Object:id,pos

                idx += 8  # skip anchor points 如果有4个点，那么idx=3*3+8=17，第17位就是点的坐标完了的位置

                rd.leftcnt = int(darr[idx])# 左车道数
                idx += 1
                rd.rightcnt = int(darr[idx])# 右车道数
                idx += 1
                rd.eachw = int(darr[idx])# 每条车道的宽度
                self.net.roadlist.append(rd.id)# 路的id集合
                self.net.netmap[rd.id] = rd# 路(对象)
                # 上面的部分是对txt文件中路的一个信息的存储，
            elif darr[0] == 'S':
                 self.net.scale = int(darr[2])
            elif darr[0] == 'L':
                 self.net.L = int(darr[1])
         #cm.debug_print('Finished parsing network text file')
    # 对上面存储信息的一个提取/输出，生成XML文件以供引擎使用
    def createNetXML(self, path):  # add additional geometry information
        global northidx
        # k:rd_id v:num
        margmap = {}
        # k:cr_id v:crnum
        crsmap = {}

        linkmap = {}

        corsslink = {}  # 4 links connect 1 corss

        marnum = 0

        crsnum = 0

        linknum = 0
        # 写方式
        xmlnet = open(path, 'w')
        # 数据部分
        xmlnet.write('<Data>\n')
        # demand
        xmlnet.write('\t<Demand>\n'
                     '\t\t<Time>0</Time>\n'
                     '\t\t<Value>5</Value>\n'
                     '\t</Demand>\n'
                     '\t<Demand>\n'
                     '\t\t<Time>0.55</Time>\n'
                     '\t\t<Value>10</Value>\n'
                     '\t</Demand>\n'
                     '\t<Demand>\n'
                     '\t\t<Time>0.5</Time>\n'
                     '\t\t<Value>0</Value>\n'
                     '\t</Demand>\n')
        # marginals
        # list<marginallist>:MarginalPt Object:id,pos
        # dict<margmap>:key-value of marginal roadID,marginal number
        # 以边缘点为循环
        for m in self.net.marginallist:
            # 边缘点信息
            xmlnet.write('\t<MarginalPoint>\n')
            xmlnet.write('\t\t<Object_ID>%s</Object_ID>\n'  # 对象id
                         '\t\t<Object_Type>M</Object_Type>\n'  # 对象类型(mar)
                         '\t\t<Object_Label>M%s</Object_Label>\n'  # 对象标签
                         '\t\t<x>%d</x>\n'  # 边缘点坐标
                         '\t\t<y>%d</y>\n' % (marnum, marnum, m.pos[0], m.pos[1]))
            margmap[m.id] = marnum
            marnum += 1  # 下一个边缘点
            xmlnet.write('\t</MarginalPoint>\n')
        # 以交叉口为循环
        for cid in self.net.crosslist:
            # 交叉口信息
            xmlnet.write('\t<Cross>\n')
            # 通过cid获取交叉口信息
            c = self.net.netmap[cid]
            xmlnet.write('\t\t<Cross_Type>%d</Cross_Type>\n'  # 交叉口类型(0 十字路口/1 T路口)
                         '\t\t<Object_ID>%s</Object_ID>\n'  # 对象id，体现出有几个对象
                         '\t\t<Object_Type>C</Object_Type>\n'   # 对象类型，C
                         '\t\t<Object_Label>C%s</Object_Label>\n'   # 对象标签
                         '\t\t<Cross_Id>%s</Cross_Id>\n'    # 交叉口id
                         '\t\t<Cross_Radius>%d</Cross_Radius>\n'  # 交叉口半径，暂时为固定值16
                         '\t\t<Connected_Segment_Number>%d</Connected_Segment_Number>\n'    # 连接道路数
                         '\t\t<x>%d</x>\n'
                         '\t\t<y>%d</y>\n' % (
                         c.type, crsnum, crsnum, c.id, c.radius, c.lanecnt, c.center[0], c.center[1]))
            crsmap[c.id] = crsnum
            crsnum += 1
            xmlnet.write('\t</Cross>\n')

        print("crsmap:", crsmap)

        # links
        ridx = 0
        # 以道路为循环，得到一些关于Link的信息
        for rid in self.net.roadlist:
            # 获取路的信息map
            rd = self.net.netmap[rid]
            # 道路类型,暂无意义
            rdtype = 0
            # od矩阵
            isorigin = 0
            isdest = 0

            if rd.tp == 's':# 道路是直的
                rdtype = 0
            elif rd.tp == 'c':# 道路是弯的
                rdtype = 1
            # 开始对象
            startObj = rd.leftcross# 左交叉口
            # 此条路无左交叉口
            if startObj == '-':
                startType = 'M'
                startObj = margmap[rd.id]  # 边缘点集合
            else:
                startType = 'C'# 有交叉口
                startObj = crsmap[rd.leftcross]

            endObj = rd.rightcross
            if endObj == '-':
                endType = 'M'
                endObj = margmap[rd.id]
            else:
                endType = 'C'
                endObj = crsmap[rd.rightcross]# 以左交叉口为开始，右交叉口为结束
            
                        #当Link起点是M, 终点是C时, 一定(可以)是源点, 且不能是终点
            #当Link起点是M, 终点是M时, 一定(可以)是源点, 且(可以)是终点
            #当Link起点是C, 终点是M时，一定不能是源点, 且(可以)是终点
            #当Link起点是C, 终点是C时, 一定不能是源点, 且不能是终点
            if ((startType == 'M') and (endType == 'C')):
                isorigin = 1
                isdest = 0
            elif ((startType == 'M') and (endType == 'M')):
                isorigin = 1
                isdest = 1
            elif ((startType == 'C') and (endType == 'M')):
                isorigin = 0
                isdest = 1
            elif ((startType == 'C') and (endType == 'C')):
                isorigin = 0
                isdest = 0

            # right direction
            # Link数据信息
            xmlnet.write('\t<Link>\n')
            xmlnet.write('\t\t<Object_ID>%d</Object_ID>\n'  # 对象id 
                         '\t\t<Object_Type>R</Object_Type>\n'  # 对象类型，R
                         '\t\t<Object_Label>R%d</Object_Label>\n'  # 对象标签
                         '\t\t<Road_ID>%s</Road_ID>\n'  # 路的id
                         '\t\t<Lane_Number>%d</Lane_Number>\n'  # 右边包含的车道数
                         '\t\t<Guidance_or_Not>0</Guidance_or_Not>\n'
                         '\t\t<Detector_Location>\n'
                         '\t\t\t<One>0</One>\n'
                         '\t\t\t<Two>29</Two>\n'
                         '\t\t\t<Three>-11</Three>\n'
                         '\t\t</Detector_Location>\n'
                         '\t\t<Link_Start>\n'   # 开始对象 
                         '\t\t\t<Object_Type>%s</Object_Type>\n'
                         '\t\t\t<Object_ID>%s</Object_ID>\n'
                         '\t\t</Link_Start>\n'
                         '\t\t<Link_End>\n'     # 结束对象
                         '\t\t\t<Object_Type>%s</Object_Type>\n'
                         '\t\t\t<Object_ID>%s</Object_ID>\n'
                         '\t\t</Link_End>\n'
                         '\t\t<Is_Origin>%d</Is_Origin>\n'
                         '\t\t<Is_Dest>%d</Is_Dest>\n'
                         '\t\t<Is_Curve>%d</Is_Curve>\n'
                         '\t\t<Limited_Speed>14</Limited_Speed>\n' % (
                         linknum, linknum, rd.id, rd.rightcnt, startType, startObj, endType, endObj, isorigin, isdest,
                         rdtype))
            xmlnet.write('\t\t<Path_ID>%d</Path_ID>\n' % ridx)
            xmlnet.write('\t</Link>\n')

            linkmap[rd.id + '0'] = linknum  # int类型转成char类型
            # 如果此路右边方向，结束对象为交叉口
            if endType == 'C':
                if endObj in corsslink:
                    corsslink[endObj] += " " + str(linknum)
                else:
                    corsslink[endObj] = str(linknum)  # crosslink里存的是道路id

            linknum += 1
            # left direction
            xmlnet.write('\t<Link>\n')
            xmlnet.write('\t\t<Object_ID>%d</Object_ID>\n'
                         '\t\t<Object_Type>R</Object_Type>\n'
                         '\t\t<Object_Label>R%d</Object_Label>\n'
                         '\t\t<Road_ID>%s</Road_ID>\n'
                         '\t\t<Lane_Number>%d</Lane_Number>\n'
                         '\t\t<Guidance_or_Not>0</Guidance_or_Not>\n'
                         '\t\t<Detector_Location>\n'
                         '\t\t\t<One>0</One>\n'
                         '\t\t\t<Two>29</Two>\n'
                         '\t\t\t<Three>-11</Three>\n'
                         '\t\t</Detector_Location>\n'
                         '\t\t<Link_Start>\n'
                         '\t\t\t<Object_Type>%s</Object_Type>\n'
                         '\t\t\t<Object_ID>%s</Object_ID>\n'
                         '\t\t</Link_Start>\n'
                         '\t\t<Link_End>\n'
                         '\t\t\t<Object_Type>%s</Object_Type>\n'
                         '\t\t\t<Object_ID>%s</Object_ID>\n'
                         '\t\t</Link_End>\n'
                         '\t\t<Is_Origin>%d</Is_Origin>\n'
                         '\t\t<Is_Dest>%d</Is_Dest>\n'
                         '\t\t<Is_Curve>%d</Is_Curve>\n'
                         '\t\t<Limited_Speed>14</Limited_Speed>\n' % (
                         linknum, linknum, rd.id, rd.leftcnt, endType, endObj, startType, startObj, isdest, isorigin, rdtype))
            xmlnet.write('\t\t<Path_ID>%d</Path_ID>\n' % ridx)

            linkmap[rd.id + '1'] = linknum

            if startType == 'C':
                if startObj in corsslink:
                    corsslink[startObj] += " " + str(linknum)
                else:
                    corsslink[startObj] = str(linknum)

            linknum += 1
            xmlnet.write('\t</Link>\n')
            # 下一条R
            ridx += 1

        print("crosslink:", corsslink)

        # lanes
        print("roadlist:", self.net.roadlist)

        # 每条路的循环，得到一些lane的信息
        for rdid in self.net.roadlist:
            rd = self.net.netmap[rdid]
            # 右车道数
            sidecnt = rd.rightcnt
            bleft, bstraight, bright = 0, 0, 0  # 是否能左转/直行/右转
            # 以右车道为循环，获取左转、直行、右转等信息
            # 只有一条道时，只能左转或右转
            # 有4条道时，第一条道只能左转,第二条道只能直行，第三条道只能直行，第四条道能右转
            for i in range(sidecnt):
                if i == 0:
                    bleft = 1
                    bstraight = 0
                    bright = 0
                elif i == sidecnt - 1:
                    bleft = 0
                    bstraight = 0
                    bright = 1
                else:
                    bleft = 0
                    bstraight = 1
                    bright = 0
                xmlnet.write('\t<Lane>\n'
                             '\t\t<Link_ID>%d</Link_ID>\n'
                             '\t\t<Lane_ID>%d</Lane_ID>\n'
                             '\t\t<Left_Turn>%d</Left_Turn>\n'
                             '\t\t<Straight_Turn>%d</Straight_Turn>\n'
                             '\t\t<Right_Turn>%d</Right_Turn>\n'
                             '\t</Lane>\n' % (linkmap[rd.id + '0'], i, bleft, bstraight, bright))
            sidecnt = rd.leftcnt    # 左车道数

            for i in range(sidecnt):
                if i == 0:
                    bleft = 1
                    bstraight = 0
                    bright = 0
                elif i == sidecnt - 1:
                    bleft = 0
                    bstraight = 0
                    bright = 1
                else:
                    bleft = 0
                    bstraight = 1
                    bright = 0
                xmlnet.write('\t<Lane>\n'
                             '\t\t<Link_ID>%d</Link_ID>\n'
                             '\t\t<Lane_ID>%d</Lane_ID>\n'
                             '\t\t<Left_Turn>%d</Left_Turn>\n'
                             '\t\t<Straight_Turn>%d</Straight_Turn>\n'
                             '\t\t<Right_Turn>%d</Right_Turn>\n'
                             '\t</Lane>\n' % (linkmap[rd.id + '1'], i, bleft, bstraight, bright))
        print("linkmap:", linkmap)
        print("margmap:", margmap)
        # 下面定义相位
        # controllers
        # 每一个交叉口就是一次循环
        for crsid in self.net.crosslist:
            northidx = 0
            # 南索引
            southidx = 0
            # 西索引
            westidx = 0
            # 东索引
            estidx = 0
            # 获取交叉口对象
            crs = self.net.netmap[crsid]
            # 只接受十字路口和T路口两种路口
            if crs.type > 1:
                continue
            crsidx = crsmap[crs.id]
            strlist = list(
                map(int, corsslink.get(crsidx).split(' ')))  # get crosslist's string and convert to integer list
            print("strlist:", strlist)
            print("crs north/south/east/west:", crs.north, crs.south, crs.est, crs.west)
            print("strlist的长度:", len(strlist))
            if 4 == len(strlist):  # 左右两边的车道
                northidx_1, southidx_1, westidx_1, estidx_1 = linkmap[crs.north + '0'], linkmap[crs.south + '1'], \
                linkmap[crs.west + '0'], linkmap[crs.est + '1']  # 1 + '0' = '10'

                northidx_2, southidx_2, westidx_2, estidx_2 = linkmap[crs.north + '1'], linkmap[crs.south + '0'], \
                linkmap[crs.west + '1'], linkmap[crs.est + '0']

                # 如果northidx本身在crosslink中
                if northidx_1 in strlist:
                    northidx = northidx_1
                else:
                    northidx = northidx_2

                if southidx_1 in strlist:
                    southidx = southidx_1
                else:
                    southidx = southidx_2
                if westidx_1 in strlist:
                    westidx = westidx_1
                else:
                    westidx = westidx_2
                if estidx_1 in strlist:
                    estidx = estidx_1
                else:
                    estidx = estidx_2
            elif 3 == len(strlist):  # 3条道路
                print("进入3条道路")
                if crs.est == '0':  # 缺少东边的道路
                    estidx = -4
                    northidx_1, southidx_1, westidx_1 = linkmap[crs.north + '0'], linkmap[crs.south + '1'], linkmap[
                        crs.west + '0']
                    northidx_2, southidx_2, westidx_2 = linkmap[crs.north + '1'], linkmap[crs.south + '0'], linkmap[
                        crs.west + '1']
                    if northidx_1 in strlist:
                        northidx = northidx_1
                    else:
                        northidx = northidx_2
                    if southidx_1 in strlist:
                        southidx = southidx_1
                    else:
                        southidx = southidx_2
                    if westidx_1 in strlist:
                        westidx = westidx_1
                    else:
                        westidx = westidx_2
                if crs.west == '0':  # 缺少西边的道路
                    westidx = -3
                    northidx_1, southidx_1, estidx_1 = linkmap[crs.north + '0'], linkmap[crs.south + '1'], linkmap[
                        crs.est + '1']
                    northidx_2, southidx_2, estidx_2 = linkmap[crs.north + '1'], linkmap[crs.south + '0'], linkmap[
                        crs.est + '0']
                    if northidx_1 in strlist:
                        northidx = northidx_1
                    else:
                        northidx = northidx_2
                    if southidx_1 in strlist:
                        southidx = southidx_1
                    else:
                        southidx = southidx_2
                    if estidx_1 in strlist:
                        estidx = estidx_1
                    else:
                        estidx = estidx_2
                if crs.north == '0':  # 缺少北边的道路
                    northidx = -1
                    southidx_1, westidx_1, estidx_1 = linkmap[crs.south + '1'], linkmap[crs.west + '0'], linkmap[
                        crs.est + '1']
                    southidx_2, westidx_2, estidx_2 = linkmap[crs.south + '0'], linkmap[crs.west + '1'], linkmap[
                        crs.est + '0']
                    if southidx_1 in strlist:
                        southidx = southidx_1
                    else:
                        southidx = southidx_2
                    if westidx_1 in strlist:
                        westidx = westidx_1
                    else:
                        westidx = westidx_2
                    if estidx_1 in strlist:
                        estidx = estidx_1
                    else:
                        estidx = estidx_2
                if crs.south == '0':  # 缺少南方的道路
                    print("进入南")
                    southidx = -2
                    northidx_1, westidx_1, estidx_1 = linkmap[crs.north + '0'], linkmap[crs.west + '0'], linkmap[
                        crs.est + '1']
                    northidx_2, westidx_2, estidx_2 = linkmap[crs.north + '1'], linkmap[crs.west + '1'], linkmap[
                        crs.est + '0']
                    if northidx_1 in strlist:
                        northidx = northidx_1
                    else:
                        northidx = northidx_2
                    if westidx_1 in strlist:
                        westidx = westidx_1
                    else:
                        westidx = westidx_2
                    if estidx_1 in strlist:
                        estidx = estidx_1
                    else:
                        estidx = estidx_2

            # increment's value is 0,1,2 or 3,used to control different cross to different phase
            # 还在交叉口的循环内
            print("northidx:", northidx)
            print("southidx:", southidx)
            print("estidx:", estidx)
            print("westidx:", westidx)
            increment = crsidx % 4
            xmlnet.write('\t<Controller>\n')
            xmlnet.write('\t\t<Cross_ID>%d</Cross_ID>\n' % (crsidx))
            xmlnet.write('\t\t<Cycle_Time>200</Cycle_Time>\n')
            xmlnet.write('\t\t<Phase_Number>6</Phase_Number>\n')

            # phase 0
            xmlnet.write('\t\t<Phase>\n')
            xmlnet.write('\t\t\t<Phase_ID>0</Phase_ID>\n')
            xmlnet.write('\t\t\t<Direction>1</Direction>\n')
            xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
            xmlnet.write('\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n' % (25 * increment + 0))
            xmlnet.write('\t\t\t<Connect_Link_ID>\n')
            # print("后面的",northidx,type(northidx))
            xmlnet.write('\t\t\t\t<A>%d</A>\n' % (northidx))
            xmlnet.write('\t\t\t\t<B>%d</B>\n' % (southidx))
            xmlnet.write('\t\t\t</Connect_Link_ID>\n')
            xmlnet.write("\t\t</Phase>\n")  # 链接北方和南方两条道路

            # phase 1
            xmlnet.write('\t\t<Phase>\n')
            xmlnet.write('\t\t\t<Phase_ID>1</Phase_ID>\n')
            xmlnet.write('\t\t\t<Direction>0</Direction>\n')
            xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
            xmlnet.write(
                '\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n' % ((25 * increment + 25) % 100))
            xmlnet.write('\t\t\t<Connect_Link_ID>\n')
            xmlnet.write('\t\t\t\t<A>%d</A>\n' % (northidx))
            xmlnet.write('\t\t\t\t<B>%d</B>\n' % (southidx))
            xmlnet.write('\t\t\t</Connect_Link_ID>\n')
            xmlnet.write("\t\t</Phase>\n")

            # phase2
            xmlnet.write('\t\t<Phase>\n')
            xmlnet.write('\t\t\t<Phase_ID>2</Phase_ID>\n')
            xmlnet.write('\t\t\t<Direction>2</Direction>\n')
            xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
            xmlnet.write('\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n' % (25 * increment + 0))
            xmlnet.write('\t\t\t<Connect_Link_ID>\n')
            xmlnet.write('\t\t\t\t<A>%d</A>\n' % (northidx))
            xmlnet.write('\t\t\t\t<B>%d</B>\n' % (southidx))
            xmlnet.write('\t\t\t</Connect_Link_ID>\n')
            xmlnet.write("\t\t</Phase>\n")

            # phase3
            xmlnet.write('\t\t<Phase>\n')
            xmlnet.write('\t\t\t<Phase_ID>3</Phase_ID>\n')
            xmlnet.write('\t\t\t<Direction>1</Direction>\n')
            xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
            xmlnet.write(
                '\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n' % ((25 * increment + 50) % 100))
            xmlnet.write('\t\t\t<Connect_Link_ID>\n')
            xmlnet.write('\t\t\t\t<A>%d</A>\n' % (westidx))
            xmlnet.write('\t\t\t\t<B>%d</B>\n' % (estidx))
            xmlnet.write('\t\t\t</Connect_Link_ID>\n')
            xmlnet.write("\t\t</Phase>\n")  # 链接西方和东方两条道路

            # phase4
            xmlnet.write('\t\t<Phase>\n')
            xmlnet.write('\t\t\t<Phase_ID>4</Phase_ID>\n')
            xmlnet.write('\t\t\t<Direction>0</Direction>\n')
            xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
            xmlnet.write(
                '\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n' % ((25 * increment + 75) % 100))
            xmlnet.write('\t\t\t<Connect_Link_ID>\n')
            xmlnet.write('\t\t\t\t<A>%d</A>\n' % (westidx))
            xmlnet.write('\t\t\t\t<B>%d</B>\n' % (estidx))
            xmlnet.write('\t\t\t</Connect_Link_ID>\n')
            xmlnet.write("\t\t</Phase>\n")

            # phase5
            xmlnet.write('\t\t<Phase>\n')
            xmlnet.write('\t\t\t<Phase_ID>5</Phase_ID>\n')
            xmlnet.write('\t\t\t<Direction>2</Direction>\n')
            xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
            xmlnet.write(
                '\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n' % ((25 * increment + 50) % 100))
            xmlnet.write('\t\t\t<Connect_Link_ID>\n')
            xmlnet.write('\t\t\t\t<A>%d</A>\n' % (westidx))
            xmlnet.write('\t\t\t\t<B>%d</B>\n' % (estidx))
            xmlnet.write('\t\t\t</Connect_Link_ID>\n')
            xmlnet.write("\t\t</Phase>\n")

            xmlnet.write('\t</Controller>\n')

        # paths
        pathidx = 0
        for rdid in self.net.roadlist:
            rd = self.net.netmap[rdid]
            xmlnet.write('\t<Baseline>\n')
            xmlnet.write('\t\t<Path_ID>%d</Path_ID>\n' % pathidx)
            xmlnet.write('\t\t<Point_Count>%d</Point_Count>\n' % rd.anchorcnt)
            xmlnet.write('\t\t<Points>')
            for i in range(rd.anchorcnt):
                # xmlnet.write('\t\t\t<Point>\n\t\t\t\t<X>%d</X>\n\t\t\t\t<Y>%d</Y>\n\t\t\t</Point>\n' % (rd.anchors[i][0],rd.anchors[i][1]))
                xmlnet.write('%d %d,' % (rd.anchors[i][0], rd.anchors[i][1]))  # 点的坐标
            xmlnet.write('</Points>\n')
            xmlnet.write('\t</Baseline>\n')
            pathidx += 1  # 下一条道路

        xmlnet.write('</Data>')  # 数据部分结束


class NetWorld:
    def __init__(self):
        self.crosslist = []
        self.marginallist = []
        self.roadlist = []
        self.signlist = []
        self.netmap = {}  # dictionary of physics network -- road,cross
        self.scale = 0
        self.L = ''
        self.doc = NetDocTool(self)

    def getElmt(self, rdid):
        try:
            return self.netmap[rdid]
        except Exception:
            print('No such Element')


def makeMap():
    world = NetWorld()
    world.doc.loadNetTXT('demo.txt')
    world.doc.createNetXML('net.xml')


def mapGen(txt, xml):
    world = NetWorld()
    world.doc.loadNetTXT(txt)
    world.doc.createNetXML(xml)

# 只导出txt_to_xml函数
__all__ = ['txt_to_xml']

# 定义一个静音的上下文管理器来禁止输出
def mapmakernew_suppress_output():
    # 创建一个空的 StringIO 对象，用作 stdout 的重定向目标
    return contextlib.redirect_stdout(io.StringIO())

def _inter_txt_to_xml_new(txt_file_path: str, xml_file_path: str) -> bool:
    """将txt格式的路网数据转为引擎xml格式    

    Args:
        txt_file_path (str): txt格式文件路径
        xml_file_path (str): 要输出的xml格式文件路径

    Returns:
        bool: 是否转换成功
    """
    result = False
    try:
        mapGen(txt_file_path, xml_file_path)
        result = True
    except Exception as e:
        result = False
    return result

def txt_to_xml_new(txt_file_path: str, xml_file_path: str) -> bool:
    result = None
    with mapmakernew_suppress_output():
        result = _inter_txt_to_xml_new(txt_file_path, xml_file_path)
    return result

if __name__ == '__main__':

    # determine the number of parameters
    if len(sys.argv) != 3:
        print("Usage: python3 mapmaker.py ***.txt ***.xml")
        sys.exit(1)
    intxt, outxml = sys.argv[1:3]
    # makeMap()
    mapGen(intxt, outxml)
