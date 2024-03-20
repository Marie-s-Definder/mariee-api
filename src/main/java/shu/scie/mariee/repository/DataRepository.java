package shu.scie.mariee.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import shu.scie.mariee.model.Data;

import java.util.Date;
import java.util.List;

public interface DataRepository extends CrudRepository<Data,Long> {
    List<Data> findAllByRobotidAndDevicenameOrderByDateDesc(Long robotId, String deviceName);

    List<Data> findByDateBetweenAndRobotidAndDevicenameOrderByDateDesc(Date startDate, Date endDate, Long robotId, String deviceName);

}
