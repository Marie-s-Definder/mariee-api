package shu.scie.mariee.service;

import org.springframework.stereotype.Service;
import shu.scie.mariee.model.Device;
import shu.scie.mariee.repository.DeviceRepository;

import java.util.List;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public List<Device> getDeviceByRobotId(Long robotId) {
        return deviceRepository.findAllByRobotid(robotId);
    }
}
