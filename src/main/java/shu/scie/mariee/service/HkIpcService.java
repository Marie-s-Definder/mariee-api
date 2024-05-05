package shu.scie.mariee.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import shu.scie.mariee.model.HkIpc;
import shu.scie.mariee.repository.HkIpcRepository;

@Service
public class HkIpcService {


    private final HkIpcRepository hkIpcRepository;

    public HkIpcService(HkIpcRepository hkIpcRepository) {
        this.hkIpcRepository = hkIpcRepository;
    }

    public HkIpc getById(Long id) {
        return hkIpcRepository.findById(id).orElse(null);
    }


}
