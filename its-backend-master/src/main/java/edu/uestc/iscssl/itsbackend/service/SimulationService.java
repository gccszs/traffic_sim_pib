package edu.uestc.iscssl.itsbackend.service;


import edu.uestc.iscssl.common.common.SIMULATION_STATUS;
import edu.uestc.iscssl.itsbackend.domain.simulation.MapEntity;
import edu.uestc.iscssl.itsbackend.domain.simulation.SimulationEntity;
import edu.uestc.iscssl.itsbackend.repository.MapRepository;
import edu.uestc.iscssl.itsbackend.repository.SimulationRepository;
import edu.uestc.iscssl.itsbackend.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Transactional
@Service
public class SimulationService {
    @Autowired
    SimulationRepository simulationRepository;
    @Autowired
    MapRepository mapRepository;
    public SimulationEntity SearchSimulationByUserId(String id){
        try {
            return simulationRepository.findById(id).get();
        }catch (NoSuchElementException e){

        }
        return null;
    }
    public void saveSimulation(SimulationEntity simulationEntity){
        simulationRepository.saveAndFlush(simulationEntity);
    }

    public List<SimulationEntity> findByUserId(long userId){
        return simulationRepository.findByUserId(userId);
    }
    public Page<SimulationEntity> findByUserId(long userId,Pageable pageable){
        return simulationRepository.findByUserId(userId,pageable);
    }
    public Page<SimulationEntity> findBySimulationNameLikeAndStatus(String simulationName, SIMULATION_STATUS status, Pageable pageable,boolean isManager){
        if (isManager){
            return simulationRepository.findByStatusAndSimulationNameContainingOrderByCreateTime(  status,  simulationName, pageable);
        }
        else {
            return simulationRepository.findByStatusAndUserIdAndSimulationNameContainingOrderByCreateTime(  status,  UserUtils.getUserId(), simulationName, pageable);
        }
    }
    public String getName(int id){
        MapEntity mapEntity = mapRepository.findMapEntitiesById(id);
        return mapEntity.getName();
    }
    public Page<SimulationEntity>findBySimulationNameLike(String simulationName,Pageable pageable,boolean isManager){
        if (isManager)
            return simulationRepository.findBySimulationNameContainingAndStatusNotOrderByCreateTime(simulationName,SIMULATION_STATUS.DELETED,pageable);
        else{
            return simulationRepository.findBySimulationNameContainingAndUserIdAndStatusNotOrderByCreateTime(simulationName,UserUtils.getUserId(),SIMULATION_STATUS.DELETED,pageable);
        }
    }
    public Page<SimulationEntity> findAll(Pageable pageable){
        return simulationRepository.findAll(pageable);
    }
    public Page<SimulationEntity> findAllByStatus(SIMULATION_STATUS status,Pageable pageable){
     return simulationRepository.findAllByStatus(status,pageable);
    }
    public int getSimulationNumberByStatus(SIMULATION_STATUS status){
        return simulationRepository.countByStatus(status);
    }
    public int getSimulationNumberByUserId(long userId){
        return simulationRepository.countByUserId(userId);
    }
    public int getSimulationStepByUserId(long userId){
        List<SimulationEntity>simulationEntities=simulationRepository.findByUserId(userId);
        int step = 0;
        for(SimulationEntity simulationEntity: simulationEntities){
            step += simulationEntity.getStep();
        }
        return step;
    }
    public void deleteSimulationById(Iterable<String> iterator){
        List<SimulationEntity>simulationEntities=simulationRepository.findAllById(iterator);
        simulationEntities.forEach(entity -> {
            entity.setStatus(SIMULATION_STATUS.DELETED);
        });
        simulationRepository.saveAll(simulationEntities);
    }
    public SimulationEntity findSimulationById(String simulationId){
        Optional<SimulationEntity> simulationEntity = simulationRepository.findById(simulationId);
        if (simulationEntity.isPresent()){
            return simulationEntity.get();
        }
        return null;
    }

    public void deleteSimulationByReportId(String simulationId) {
        SimulationEntity simulationEntity = simulationRepository.findById(simulationId).get();
        if (simulationEntity != null)
            simulationRepository.deleteById(simulationId);

    }
}
