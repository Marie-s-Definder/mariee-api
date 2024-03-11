package shu.scie.mariee.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import shu.scie.mariee.model.ApiResult;
import shu.scie.mariee.model.HkIpc;
import shu.scie.mariee.service.HkIpcService;
import shu.scie.mariee.service.UtilService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/hkipc")
public class HkIpcController {

    private final HttpClient httpClient;
    private final HkIpcService hkIpcService;

    public HkIpcController(HkIpcService hkIpcService) {
        this.httpClient = UtilService.createHttpClient();
        this.hkIpcService = hkIpcService;
    }

    @GetMapping("/liveUrl")
    public ApiResult<String> pan(@RequestParam String id) {
        HkIpc ipc = this.hkIpcService.getById(id);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
        }

        return new ApiResult<>(true, ipc.liveUrl);
    }

    @GetMapping("/snapshot")
    public ApiResult<String> snapshot(@RequestParam String id) {
        HkIpc ipc = this.hkIpcService.getById(id);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
        }

        Path snapshotFolderPath = Path.of("./data/snapshots");
        try {
            Files.createDirectories(snapshotFolderPath.normalize());
        } catch (IOException e) {
            return new ApiResult<>(false, e.getMessage());
        }

        HttpRequest req;
        try {
            req = HttpRequest.newBuilder()
                    .uri(new URI(STR."http://\{ipc.ip}/ISAPI/Streaming/channels/\{ipc.streamChannel}/picture"))
                    .GET()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                    .headers(HttpHeaders.AUTHORIZATION, STR."Basic \{Base64.getEncoder().encodeToString(STR."\{ipc.username}:\{ipc.password}".getBytes())}")
                    .build();
        } catch (URISyntaxException e) {
            return new ApiResult<>(false, e.getMessage());
        }

        String newFileName = STR."\{Instant.now().toEpochMilli()}.jpg";

        HttpResponse<Path> res;
        try {
            res = this.httpClient.send(req, HttpResponse.BodyHandlers.ofFile(Path.of(snapshotFolderPath.toString(), newFileName)));
        } catch (Exception e) {
            return new ApiResult<>(false, e.getMessage());
        }

        if (res.statusCode() != HttpStatus.OK.value()) {
            return new ApiResult<>(false, Integer.toString(res.statusCode()));
        }

        return new ApiResult<>(true, STR."snapshots/\{newFileName}");
    }

    @GetMapping("/pan")
    public ApiResult<String> pan(@RequestParam String id, @RequestParam String direction) throws InterruptedException {
        HkIpc ipc = this.hkIpcService.getById(id);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
        }

        String res = this.startIpcMove(ipc, "pan", direction);
        if (!res.isEmpty()) {
            return new ApiResult<>(false, res);
        }

        Thread.sleep(1000);

        res = this.stopIpcMove(ipc, "pan");
        if (!res.isEmpty()) {
            return new ApiResult<>(false, res);
        }

        return new ApiResult<>(true, STR."pan \{direction}");
    }

    @GetMapping("/tilt")
    public ApiResult<String> tilt(@RequestParam String id, @RequestParam String direction) throws InterruptedException {
        HkIpc ipc = this.hkIpcService.getById(id);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
        }

        String res = this.startIpcMove(ipc, "tilt", direction);
        if (!res.isEmpty()) {
            return new ApiResult<>(false, res);
        }

        Thread.sleep(1000);

        res = this.stopIpcMove(ipc, "tilt");
        if (!res.isEmpty()) {
            return new ApiResult<>(false, res);
        }

        return new ApiResult<>(true, STR."tilt \{direction}");
    }

    @GetMapping("/zoom")
    public ApiResult<String> zoom(@RequestParam String id, @RequestParam String direction) throws InterruptedException {
        HkIpc ipc = this.hkIpcService.getById(id);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
        }

        String res = this.startIpcMove(ipc, "zoom", direction);
        if (!res.isEmpty()) {
            return new ApiResult<>(false, res);
        }

        Thread.sleep(1000);

        res = this.stopIpcMove(ipc, "zoom");
        if (!res.isEmpty()) {
            return new ApiResult<>(false, res);
        }

        return new ApiResult<>(true, STR."zoom \{direction}");
    }


    private String startIpcMove(HkIpc ipc, String action, String direction) {
        if (ipc == null) {
            return "IPC is null";
        }

        if (!"pan".equals(action) && !"tilt".equals(action) && !"zoom".equals(action)) {
            return STR."Invalid action for IPC movement: \{action}";
        }

        if ("pan".equals(action)) {
            if (!"left".equals(direction) && !"right".equals(direction)) {
                return STR."Invalid direction for pan: \{direction}";
            }
        }
        if ("tilt".equals(action)) {
            if (!"up".equals(direction) && !"down".equals(direction)) {
                return STR."Invalid direction for tilt: \{direction}";
            }
        }
        if ("zoom".equals(action)) {
            if (!"in".equals(direction) && !"out".equals(direction)) {
                return STR."Invalid direction for zoom: \{direction}";
            }
        }

        String speedPrefix = "";
        if ("left".equals(direction) || "down".equals(direction) || "out".equals(direction)) {
            speedPrefix = "-";
        }

        String requestBody = STR."<PTZData version=\"2.0\" xmlns=\"http://www.isapi.org/ver20/XMLSchema\"><\{action}>\{speedPrefix}10</\{action}></PTZData>";

        HttpRequest moveReq;
        try {
            moveReq = this.makeIpcRequest(ipc, requestBody);
        } catch (URISyntaxException e) {
            return STR."Start \{action} request failed: \{e.getMessage()}";
        }

        HttpResponse<String> moveRes;
        try {
            moveRes = this.httpClient.send(moveReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return STR."Start \{action} request failed: \{e.getMessage()}";
        }

        if (moveRes.statusCode() == HttpStatus.OK.value()) {
            return "";
        } else {
            return STR."Start \{action} request failed: \{moveRes.statusCode()}";
        }

    }

    private String stopIpcMove(HkIpc ipc, String action) {
        if (ipc == null) {
            return "IPC is null";
        }
        if (!"pan".equals(action) && !"tilt".equals(action) && !"zoom".equals(action)) {
            return STR."Invalid action for IPC movement: \{action}";
        }

        String requestBody = STR."<PTZData version=\"2.0\" xmlns=\"http://www.isapi.org/ver20/XMLSchema\"><\{action}>0</\{action}></PTZData>";

        HttpRequest stopReq;
        try {
            stopReq = this.makeIpcRequest(ipc, requestBody);
        } catch (URISyntaxException e) {
            return STR."Stop pan request failed: \{e.getMessage()}";
        }

        HttpResponse<String> stopRes;
        try {
            stopRes = this.httpClient.send(stopReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return STR."Stop pan request failed: \{e.getMessage()}";
        }

        if (stopRes.statusCode() == HttpStatus.OK.value()) {
            return "";
        } else {
            return STR."Stop pan request failed: \{stopRes.statusCode()}";
        }
    }

    private HttpRequest makeIpcRequest(HkIpc ipc, String body) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(STR."http://\{ipc.ip}/ISAPI/PTZCtrl/channels/\{ipc.ptzChannel}/continuous"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .headers(HttpHeaders.AUTHORIZATION, STR."Basic \{Base64.getEncoder().encodeToString(STR."\{ipc.username}:\{ipc.password}".getBytes())}")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

}
