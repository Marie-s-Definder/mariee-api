package shu.scie.mariee.repository;

import org.springframework.data.repository.CrudRepository;
import shu.scie.mariee.model.Robot;

import java.util.List;

public interface RobotRepository extends CrudRepository<Robot, Long> {
    List<Robot> findAllByBuildingAndRoom(String building, String room);

}
