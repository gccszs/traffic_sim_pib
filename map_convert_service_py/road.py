'''
Created on 2015-8-4

@author: Jone
'''

class MarginalPt:
    def __init__(self):
        self.id = ''
        self.pos = (0,0,0)

#class RoadLane:
#    def __init__(self):
#        self.id = ''
#        self.parent = ''
#        self.no = 0
#        self.tp = '-'
#        self.detect = None
#        self.odpt = None
#        self.routine = [] # routine line
#        self.crosslane = None
#        self.vehs = []
#    
#   
        
#network data structures
class Road:
    def __init__(self,world):
        self.world = world
        self.id = '' #road id
        self.leftcross = ''#road leftcross id object_id
        self.rightcross = ''#road rightcross id
        self.odlane = ''
        self.tp = '' #road type straight road is 's',curve road is'c'
        self.lanecnt = 0
        self.rampcnt = 0
        self.leftcnt = 0  #left road's lane number
        self.rightcnt = 0 #right road's lane number
        self.eachw = 0    #each lane's width
        self.anchorcnt = 0
        self.vehcnt = 0
        self.length = 0
        self.cost = self.length
        self.lanes = {} # 0: lane1 , 1: lane2
        self.ramps = {}
        self.sigs = {}
        #self.crosslanes = []
        self.guides = []
        self.anchors = []
        self.shape = []
        self.baseline = []
        self.vertualines = {}  # 0: [(x1,y1),(x2,y2),...]  
        self.device = None
