package shu.scie.mariee.repository;

import org.springframework.data.repository.CrudRepository;
import shu.scie.mariee.model.DataInfo;
import shu.scie.mariee.model.Preset;

import java.util.List;

public interface DataInfoRepository extends CrudRepository<DataInfo, Long> {

    DataInfo findAllById(Long Id);
}
