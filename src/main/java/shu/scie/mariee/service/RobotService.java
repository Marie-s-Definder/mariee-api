package shu.scie.mariee.service;

import org.springframework.stereotype.Service;
import shu.scie.mariee.model.Robot;
import shu.scie.mariee.repository.RobotRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class RobotService {
    private final RobotRepository robotRepository;

    public RobotService(RobotRepository robotRepository) {
        this.robotRepository = robotRepository;
    }

    public List<Robot> getRobotsByBuildingAndRoom(String building, String room) {
        List<Robot> robots = robotRepository.findAllByBuildingAndRoom(building, room);
        return robots;
    }

    public Long getAllRobotsRows() {
        return robotRepository.count();
    }
}
