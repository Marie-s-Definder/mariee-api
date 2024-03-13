package shu.scie.mariee.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import shu.scie.mariee.model.HkIpc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class HkIpcService {

    private final List<HkIpc> list;

    public HkIpcService() {
        this.list = this.getList();
    }

    public HkIpc getById(Long id) {
        List<HkIpc> list = this.list.stream().filter(i -> id.equals(i.id)).toList();
        if (!list.isEmpty()) {
            return list.getFirst();
        } else {
            return null;
        }
    }

    private List<HkIpc> getList() {
        try {
            return new ObjectMapper().readValue(new File("./data/hkipc.data.json"), new TypeReference<>() {
            });
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

}
