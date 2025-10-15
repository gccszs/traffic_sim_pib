'''
Created on 2015-8-7

@author: Jone
'''
import string
import sys  # add 18.01.08
import operator # add 18.11.21
import os

from road import *
from cross import *

class NetDocTool:
	def __init__(self,net):
		self.net = net
	
	def loadNetTXT(self,strings):
		#crsnum = 0
		#linknum = 0
		#marnum = 0
		print("loadNetTXT")
		#nettxt = open(path)
		#cm.debug_print('Starting to parsing network text file.')
		#print(strings)
		tmp = strings.split('\n')
		#print(tmp)
		for line in tmp: # parse txt net
			#print(line)
			darr = line.split(',')
			if darr[0] == 'R': #road
				rd = Road(self.net)
				rd.id = darr[1]
				rd.leftcross = darr[2]
				if operator.eq(rd.leftcross,' ') :
					rd.leftcross = '-'
				rd.rightcross = darr[3]
				if operator.eq(rd.rightcross,' ') :
					rd.rightcross = '-'
				rd.anchorcnt = int(darr[4])
				if rd.anchorcnt == 2:
					rd.tp = 's' #straight road
				else:
					rd.tp = 'c' #curve road
				idx = 0
				for i in range(0,rd.anchorcnt):
					idx = i*3
					pt = (int(darr[5+idx]),
						  int(darr[6+idx]),
						  int(darr[7+idx]))
					rd.anchors.append(pt)
				
				if rd.leftcross == '-' or rd.rightcross == '-':
					mar = MarginalPt()
					mar.id = '%s' % rd.id
					if rd.leftcross == '-':
						mar.pos = rd.anchors[0]
					elif rd.rightcross == '-':
						mar.pos = rd.anchors[-1]
					self.net.marginallist.append(mar)   # MarginalPt Object:id,pos 
				
				idx += 8 # skip anchor points
				rd.leftcnt = int(darr[idx])
				idx += 1
				rd.rightcnt = int(darr[idx])
				idx += 1
				rd.eachw = int(darr[idx])
				self.net.roadlist.append(rd.id)
				self.net.netmap[rd.id] = rd
			elif darr[0] == 'C': #cross
				cr = Cross(self.net)
				cr.id = darr[1]
				cr.type = int(darr[2])
				#cr.lanecnt = int(darr[3])
				cr.west = darr[4]
				if cr.west == '0':
					cr.west = '-'
				cr.est = darr[5]
				if cr.est == '0':
					cr.est = '-'
				cr.north = darr[6]
				if cr.north == '0':
					cr.north = '-'
				cr.south = darr[7]
				if cr.south == '0':
					cr.south = '-'
				cr.five = darr[8]
				if cr.five == '0' :
					cr.five = '-'
				elif 0 == cr.type or 2 == cr.type:
					cr.lanecnt = 4
				elif 1 == cr.type:
					cr.lanecnt = 3
				elif 3 == cr.type:
					cr.lanecnt = 5
				cr.anchorcnt = int(darr[9])
				idx = 0
				for i in range(0,cr.anchorcnt):
					idx = i*3
					pt = (int(darr[10 + idx]),
						  int(darr[11 + idx]),
						  int(darr[12 + idx]))
					cr.anchors.append(pt)
				cx,cy,cz = 0.0,0.0,0.0
				for pt in cr.anchors:
					cx += pt[0]
					cy += pt[1]
					cz += pt[2]
				cx /= cr.anchorcnt
				cy /= cr.anchorcnt
				cz /= cr.anchorcnt
				cr.center = (cx,cy,cz)
				if 2 == cr.type or 3 == cr.type :
					cr.radius = int(darr[-1])
				self.net.crosslist.append(cr.id)
				self.net.netmap[cr.id] = cr
			elif darr[0] == 'S':
				self.net.scale = float(darr[2])
			elif darr[0] == 'L':
				self.net.L = int(darr[1])
		#cm.debug_print('Finished parsing network text file')
	
	def createNetXML(self,mapName): # add additional geometry information
		print("createNetXML")
		margmap = {}
		crsmap = {}
		linkmap = {}
		corsslink = {} #4 links connect 1 corss
		marnum = 0
		crsnum = 0
		linknum = 0
		path = os.getcwd()+"\\map\\"+mapName+".xml"
		print(path)
		xmlnet = open(path,'w')
		xmlnet.write('<Data>\n')
		#demand
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
		#marginals
		# list<marginallist>:MarginalPt Object:id,pos
		# dict<margmap>:key-value of marginal roadID,marginal number
		for m in self.net.marginallist:
			xmlnet.write('\t<MarginalPoint>\n')
			xmlnet.write('\t\t<Road_ID>%s</Road_ID>\n'
						 '\t\t<Object_ID>%s</Object_ID>\n'
						 '\t\t<Object_Type>M</Object_Type>\n'
						 '\t\t<Object_Label>M%s</Object_Label>\n'
						 '\t\t<x>%d</x>\n'
						 '\t\t<y>%d</y>\n' % (m.id,marnum,marnum,m.pos[0],m.pos[1]))
			margmap[m.id] = marnum
			marnum += 1							 
			xmlnet.write('\t</MarginalPoint>\n')	
		#crosses
		for cid in self.net.crosslist:
			xmlnet.write('\t<Cross>\n')
			c = self.net.netmap[cid]
			xmlnet.write('\t\t<Cross_Type>%d</Cross_Type>\n'
						 '\t\t<Object_ID>%s</Object_ID>\n'
						 '\t\t<Object_Type>C</Object_Type>\n'
						 '\t\t<Object_Label>C%s</Object_Label>\n'
						 '\t\t<Cross_Id>%s</Cross_Id>\n'
						 '\t\t<Cross_Radius>%d</Cross_Radius>\n'
						 '\t\t<Connected_Segment_Number>%d</Connected_Segment_Number>\n'
						 '\t\t<x>%d</x>\n'
						 '\t\t<y>%d</y>\n' % (c.type,crsnum,crsnum,c.id,c.radius,c.lanecnt,c.center[0],c.center[1]))
			crsmap[c.id] = crsnum
			crsnum += 1
			xmlnet.write('\t</Cross>\n')

		#links
		ridx = 0
		for rid in self.net.roadlist:
			rd = self.net.netmap[rid]
			rdtype = 0
			isod = 0
			if rd.tp == 's':
				rdtype = 0
			elif rd.tp == 'c':
				rdtype = 1
			startObj = rd.leftcross
			if startObj == '-':
				startType = 'M'
				startObj = margmap[rd.id]
				isod = 1
			else:
				startType = 'C'
				startObj = crsmap[rd.leftcross]
			endObj = rd.rightcross
			if endObj == '-':
				endType = 'M'
				endObj = margmap[rd.id]
				isod = 1
			else:
				endType = 'C'
				endObj = crsmap[rd.rightcross]

			#right direction
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
						 '\t\t<Limited_Speed>14</Limited_Speed>\n' % (linknum,linknum,rd.id,rd.rightcnt,startType,startObj,endType,endObj,isod,isod,rdtype))
			xmlnet.write('\t\t<Path_ID>%d</Path_ID>\n' % ridx)
			xmlnet.write('\t</Link>\n')
			
			linkmap[rd.id+'0'] = linknum
			
			if endType == 'C':
				if endObj in corsslink:
					corsslink[endObj] += " " + str(linknum) 
				else:
					corsslink[endObj] = str(linknum)
					
			linknum += 1
			#left direction
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
						 '\t\t<Limited_Speed>14</Limited_Speed>\n' % (linknum,linknum,rd.id,rd.leftcnt,endType,endObj,startType,startObj,isod,isod,rdtype))
			xmlnet.write('\t\t<Path_ID>%d</Path_ID>\n' % ridx)
			
			linkmap[rd.id+'1'] = linknum
			
			if startType == 'C':
				if startObj in corsslink:
					corsslink[startObj] += " " + str(linknum)
				else:
					corsslink[startObj] = str(linknum)
			
			linknum += 1
			xmlnet.write('\t</Link>\n')
			ridx += 1

		#RoadMap
		xmlnet.write('<RoadMap>\n')
		mLinkNum = 0
		for rid in self.net.roadlist:
			rd = self.net.netmap[rid]
			startObj = rd.leftcross
			if startObj == '-':
				startType = 'M'
				startObj = margmap[rd.id]
			else:
				startType = 'C'
				startObj = crsmap[rd.leftcross]
			endObj = rd.rightcross
			if endObj == '-':
				endType = 'M'
				endObj = margmap[rd.id]
			else:
				endType = 'C'
				endObj = crsmap[rd.rightcross]

			xmlnet.write('\t<Map>\n'
						 '\t\t<Road_ID>%s</Road_ID>\n' % (rd.id))
			if startType == 'M' and endType == 'C':
				xmlnet.write('\t\t<Link_Entry>%d</Link_Entry>\n' % (mLinkNum))
				mLinkNum += 1
				xmlnet.write('\t\t<Link_Exit>%d</Link_Exit>\n' % (mLinkNum))
				mLinkNum += 1
			elif startType == 'C' and endType == 'M':
				temp = mLinkNum
				mLinkNum += 1
				xmlnet.write('\t\t<Link_Entry>%d</Link_Entry>\n' % (mLinkNum))
				xmlnet.write('\t\t<Link_Exit>%d</Link_Exit>\n' % (temp))
				mLinkNum += 1
			else:
				xmlnet.write('\t\t<Link_Entry>%d</Link_Entry>\n' % (mLinkNum))
				mLinkNum += 1
				xmlnet.write('\t\t<Link_Exit>%d</Link_Exit>\n' % (mLinkNum))
				mLinkNum += 1
			xmlnet.write('\t</Map>\n')
		xmlnet.write('</RoadMap>\n')

		#lanes
		for rdid in self.net.roadlist:
			rd = self.net.netmap[rdid]
			sidecnt = rd.rightcnt
			bleft,bstraight,bright = 0,0,0
			for i in range(sidecnt):
				if i == 0:
					bleft = 1
					bstraight = 0
					bright = 0
				elif i == sidecnt-1:
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
							 '\t</Lane>\n' % (linkmap[rd.id+'0'],i,bleft,bstraight,bright))
			sidecnt = rd.leftcnt
			for i in range(sidecnt):
				if i == 0:
					bleft = 1
					bstraight = 0
					bright = 0
				elif i == sidecnt-1:
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
							 '\t</Lane>\n' % (linkmap[rd.id+'1'],i,bleft,bstraight,bright))

		#controllers
		for crsid in self.net.crosslist:
			crs = self.net.netmap[crsid]
			if crs.type > 1 :
				continue
			crsidx = crsmap[crs.id]
			strlist= list(map(int, corsslink.get(crsidx).split(' ')))  #get crosslist's string and convert to integer list
			if 4 == len(strlist):
				northidx_1,southidx_1,westidx_1,estidx_1 = linkmap[crs.north+'0'],linkmap[crs.south+'1'],linkmap[crs.west+'0'],linkmap[crs.est+'1']
				northidx_2,southidx_2,westidx_2,estidx_2 = linkmap[crs.north+'1'],linkmap[crs.south+'0'],linkmap[crs.west+'1'],linkmap[crs.est+'0']
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
			elif 3 == len(strlist):
				if crs.est == '-':
					estidx = -4
					northidx_1,southidx_1,westidx_1 = linkmap[crs.north+'0'],linkmap[crs.south+'1'],linkmap[crs.west+'0']
					northidx_2,southidx_2,westidx_2 = linkmap[crs.north+'1'],linkmap[crs.south+'0'],linkmap[crs.west+'1']
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
				if crs.west == '-':
					westidx = -3
					northidx_1,southidx_1,estidx_1 = linkmap[crs.north+'0'],linkmap[crs.south+'1'],linkmap[crs.est+'1']
					northidx_2,southidx_2,estidx_2 = linkmap[crs.north+'1'],linkmap[crs.south+'0'],linkmap[crs.est+'0']
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
				if crs.north == '-':
					northidx = -1
					southidx_1,westidx_1,estidx_1 = linkmap[crs.south+'1'],linkmap[crs.west+'0'],linkmap[crs.est+'1']
					southidx_2,westidx_2,estidx_2 = linkmap[crs.south+'0'],linkmap[crs.west+'1'],linkmap[crs.est+'0']
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
				if crs.south == '-':
					southidx = -2
					northidx_1,westidx_1,estidx_1 = linkmap[crs.north+'0'],linkmap[crs.west+'0'],linkmap[crs.est+'1']
					northidx_2,westidx_2,estidx_2 = linkmap[crs.north+'1'],linkmap[crs.west+'1'],linkmap[crs.est+'0']
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
						
			#increment's value is 0,1,2 or 3,used to control different cross to different phase
			increment = crsidx % 4
			xmlnet.write('\t<Controller>\n')
			xmlnet.write('\t\t<Cross_ID>%d</Cross_ID>\n'%(crsidx))
			xmlnet.write('\t\t<Cycle_Time>200</Cycle_Time>\n')
			xmlnet.write('\t\t<Phase_Number>6</Phase_Number>\n')

			#phase 0
			xmlnet.write('\t\t<Phase>\n')
			xmlnet.write('\t\t\t<Phase_ID>0</Phase_ID>\n')
			xmlnet.write('\t\t\t<Direction>1</Direction>\n')
			xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
			xmlnet.write('\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n'%(25*increment+0))
			xmlnet.write('\t\t\t<Connect_Link_ID>\n')
			xmlnet.write('\t\t\t\t<A>%d</A>\n'%(northidx))
			xmlnet.write('\t\t\t\t<B>%d</B>\n'%(southidx))
			xmlnet.write('\t\t\t</Connect_Link_ID>\n')
			xmlnet.write("\t\t</Phase>\n")

			#phase 1
			xmlnet.write('\t\t<Phase>\n')
			xmlnet.write('\t\t\t<Phase_ID>1</Phase_ID>\n')
			xmlnet.write('\t\t\t<Direction>0</Direction>\n')
			xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
			xmlnet.write('\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n'%((25*increment+25)%100))
			xmlnet.write('\t\t\t<Connect_Link_ID>\n')
			xmlnet.write('\t\t\t\t<A>%d</A>\n'%(northidx))
			xmlnet.write('\t\t\t\t<B>%d</B>\n'%(southidx))
			xmlnet.write('\t\t\t</Connect_Link_ID>\n')
			xmlnet.write("\t\t</Phase>\n")

			#phase2
			xmlnet.write('\t\t<Phase>\n')
			xmlnet.write('\t\t\t<Phase_ID>2</Phase_ID>\n')
			xmlnet.write('\t\t\t<Direction>2</Direction>\n')
			xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
			xmlnet.write('\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n'%(25*increment+0))
			xmlnet.write('\t\t\t<Connect_Link_ID>\n')
			xmlnet.write('\t\t\t\t<A>%d</A>\n'%(northidx))
			xmlnet.write('\t\t\t\t<B>%d</B>\n'%(southidx))
			xmlnet.write('\t\t\t</Connect_Link_ID>\n')
			xmlnet.write("\t\t</Phase>\n")

			#phase3
			xmlnet.write('\t\t<Phase>\n')
			xmlnet.write('\t\t\t<Phase_ID>3</Phase_ID>\n')
			xmlnet.write('\t\t\t<Direction>1</Direction>\n')
			xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
			xmlnet.write('\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n'%((25*increment+50)%100))
			xmlnet.write('\t\t\t<Connect_Link_ID>\n')
			xmlnet.write('\t\t\t\t<A>%d</A>\n'%(westidx))
			xmlnet.write('\t\t\t\t<B>%d</B>\n'%(estidx))
			xmlnet.write('\t\t\t</Connect_Link_ID>\n')
			xmlnet.write("\t\t</Phase>\n")

			#phase4
			xmlnet.write('\t\t<Phase>\n')
			xmlnet.write('\t\t\t<Phase_ID>4</Phase_ID>\n')
			xmlnet.write('\t\t\t<Direction>0</Direction>\n')
			xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
			xmlnet.write('\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n'%((25*increment+75)%100))
			xmlnet.write('\t\t\t<Connect_Link_ID>\n')
			xmlnet.write('\t\t\t\t<A>%d</A>\n'%(westidx))
			xmlnet.write('\t\t\t\t<B>%d</B>\n'%(estidx))
			xmlnet.write('\t\t\t</Connect_Link_ID>\n')
			xmlnet.write("\t\t</Phase>\n")

			#phase5
			xmlnet.write('\t\t<Phase>\n')
			xmlnet.write('\t\t\t<Phase_ID>5</Phase_ID>\n')
			xmlnet.write('\t\t\t<Direction>2</Direction>\n')
			xmlnet.write('\t\t\t<Green_Percent>25</Green_Percent>\n')
			xmlnet.write('\t\t\t<Green_Start_Time_Percent>%d</Green_Start_Time_Percent>\n'%((25*increment+50)%100))
			xmlnet.write('\t\t\t<Connect_Link_ID>\n')
			xmlnet.write('\t\t\t\t<A>%d</A>\n'%(westidx))
			xmlnet.write('\t\t\t\t<B>%d</B>\n'%(estidx))
			xmlnet.write('\t\t\t</Connect_Link_ID>\n')
			xmlnet.write("\t\t</Phase>\n")

			xmlnet.write('\t</Controller>\n')

		#paths
		pathidx = 0
		for rdid in self.net.roadlist:
			rd = self.net.netmap[rdid]
			xmlnet.write('\t<Baseline>\n')
			xmlnet.write('\t\t<Path_ID>%d</Path_ID>\n' % pathidx)
			xmlnet.write('\t\t<Point_Count>%d</Point_Count>\n' % rd.anchorcnt)
			xmlnet.write('\t\t<Points>')
			for i in range(rd.anchorcnt):
				#xmlnet.write('\t\t\t<Point>\n\t\t\t\t<X>%d</X>\n\t\t\t\t<Y>%d</Y>\n\t\t\t</Point>\n' % (rd.anchors[i][0],rd.anchors[i][1]))
				xmlnet.write('%d %d,' % (rd.anchors[i][0],rd.anchors[i][1]))
			xmlnet.write('</Points>\n')
			xmlnet.write('\t</Baseline>\n')
			pathidx += 1
		
		xmlnet.write('</Data>')

class NetWorld:
	def __init__(self):
		self.crosslist = []
		self.marginallist = []
		self.roadlist = []
		self.signlist = []
		self.netmap = {}   # dictionary of physics network -- road,cross
		self.scale = 0
		self.L = ''
		self.doc = NetDocTool(self)
		
	def getElmt(self,rdid):
		try:
			return self.netmap[rdid]
		except Exception:
			print('No such Element')

def makeMap():
	world = NetWorld()
	world.doc.loadNetTXT('demo.txt')
	world.doc.createNetXML()


def mapGen(string1, mapName):
	print("mapGen")
	world = NetWorld()
	world.doc.loadNetTXT(string1)
	world.doc.createNetXML(mapName)
	

if __name__ == '__main__':
	print("main")
	#determine the number of parameters
	if len(sys.argv) != 3:
		print("Usage: python3 mapmaker.py String1 String2")
		sys.exit(1)
	str1 = sys.argv[1]
	mapName = sys.argv[2]
	
	#makeMap()
	mapGen(str1, mapName)
