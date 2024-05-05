package shu.scie.mariee.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import shu.scie.mariee.model.Preset;

import java.util.List;

public interface PresetRepository extends CrudRepository<Preset,Long> {

    @Query(value = "select * from preset where robot_id = ?1 order by slide_preset_id asc ",nativeQuery = true)
    List<Preset> findAllByRobot_id(Long robotId);

    Preset findPresetById(Long id);
}
