'''
Created on 2015-8-5

@author: Jone
'''

#import numpy as np

#class CrossLane:
#    def __init__(self):
#        self.id = ''
#        self.parent = ''
#        self.frLane = ''
#        self.toLane = ''
#        self.routine = [] # routine line
#        self.leng = 0
#    
#    def frontveh(self,veh,world):
#        if world.vehctrl.activeVehs.get(veh.laneid):#first vehicle
#            lanevehs = (world.vehctrl.activeVehs[veh.laneid])[::-1]
#            for v in lanevehs:
#                if v.location >= veh.location and v.location - veh.location < 10*veh.length and v.id != veh.id:
#                    return v    
#        return None


class Cross:
    def __init__(self,world):
        self.world = world
        self.id = ''
        self.north = ''
        self.south = ''
        self.west = ''
        self.est = ''
        self.lanecnt = 0                 #linked road number
        self.anchorcnt = 0
        self.type = 0                     # cross type 0: 4 crossroad,1:T crossroad,2:4 round crossroad, 3:5round corssroad
        self.anchors = []
        self.collitionPt = (0.0,0.0)
        self.lanes = {}
        self.center = (0,0,0)
        self.radius = 0                    #round cross's radius
        self.five = ''                     #5round cross's road 
    
    def toXML(self):
        pass
    
    def frXML(self):
        pass
    
    def genShapes(self):
        pass

