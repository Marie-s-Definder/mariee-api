package shu.scie.mariee.service;

import org.springframework.stereotype.Service;
import shu.scie.mariee.model.Data;
import shu.scie.mariee.repository.DataRepository;

import java.util.Date;
import java.util.List;

@Service
public class DataService {

    private DataRepository dataRepository;

    public DataService(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    public Long getAllDataCount() {
        return dataRepository.count();
    }

    public List<Data> getAllData(Long robotId, String deviceName) {
        List<Data> lists = dataRepository.findAllByRobotidAndDevicenameOrderByDateDesc(robotId, deviceName);
        return lists;
    }

    public List<Data> getByDate(Date startTime, Date endTime, Long robotId, String deviceName) {
        return dataRepository.findByDateBetweenAndRobotidAndDevicenameOrderByDateDesc(startTime, endTime, robotId, deviceName);
    }

    public void insertDate(Data data) {
        dataRepository.save(data);
    }

}
