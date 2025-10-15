package edu.uestc.iscssl.itsbackend.repository;


import edu.uestc.iscssl.common.common.SIMULATION_STATUS;
import edu.uestc.iscssl.itsbackend.domain.simulation.SimulationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationRepository extends JpaRepository<SimulationEntity,String> {
    @Override
    List<SimulationEntity> findAllById(Iterable<String> iterable);
    <S extends SimulationEntity> S saveAndFlush(S s);
    List<SimulationEntity> findByUserId(long userId);
    Page<SimulationEntity> findByUserId(long userId,Pageable pageable);
    Page<SimulationEntity> findByStatusAndSimulationNameContainingOrderByCreateTime( SIMULATION_STATUS status, String simulationName,Pageable pageable);
    Page<SimulationEntity> findByStatusAndUserIdAndSimulationNameContainingOrderByCreateTime( SIMULATION_STATUS status, long userId,String simulationName,Pageable pageable);
    Page<SimulationEntity>findBySimulationNameContainingAndStatusNotOrderByCreateTime(String simulationName,SIMULATION_STATUS status,Pageable pageable);
    Page<SimulationEntity> findBySimulationNameContainingAndUserIdAndStatusNotOrderByCreateTime(String simulationId,long userId,SIMULATION_STATUS status,Pageable pageable);
    @Override
    Page<SimulationEntity> findAll(Pageable pageable);
    Page<SimulationEntity> findAllByStatus(SIMULATION_STATUS status,Pageable pageable);
    int countByStatus(SIMULATION_STATUS status);
    //int countDistinctByMapIdTrueAndStatus(SIMULATION_STATUS status);
    int countByUserId(long userId);
    void deleteById(String simulationId);
}
