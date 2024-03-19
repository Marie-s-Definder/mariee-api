package shu.scie.mariee.repository;

import org.springframework.data.repository.CrudRepository;
import shu.scie.mariee.model.Device;

import java.util.List;

public interface DeviceRepository extends CrudRepository<Device, Long> {
    List<Device> findAllByRobotid(Long robotId);
}
