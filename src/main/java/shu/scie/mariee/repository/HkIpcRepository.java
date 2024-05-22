package shu.scie.mariee.repository;

import org.springframework.data.repository.CrudRepository;
import shu.scie.mariee.model.HkIpc;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HkIpcRepository extends CrudRepository<HkIpc, Long> {
    List<HkIpc> findByName(String name);

    Optional<HkIpc> findById(Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE HkIpc SET interval_time = :intervalTime WHERE id = :id", nativeQuery = true)
    void updateIntervalTimeById(@Param("id") Long id, @Param("intervalTime") Long intervalTime);
}
