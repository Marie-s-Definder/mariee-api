package shu.scie.mariee.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import shu.scie.mariee.model.Iotreadonly;

import java.util.List;

public interface IotreadonlyRepository extends CrudRepository<Iotreadonly,Long>{

    List<Iotreadonly> findByDescriptionNotContainingAndPrestid(String name, Long prestid);

}
