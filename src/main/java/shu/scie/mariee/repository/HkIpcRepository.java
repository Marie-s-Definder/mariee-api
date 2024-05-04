package shu.scie.mariee.repository;

import org.springframework.data.repository.CrudRepository;
import shu.scie.mariee.model.HkIpc;

import java.util.List;
import java.util.Optional;

public interface HkIpcRepository extends CrudRepository<HkIpc, Long> {
    List<HkIpc> findByName(String name);

    Optional<HkIpc> findById(Long id);

}
