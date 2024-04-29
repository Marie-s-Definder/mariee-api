package shu.scie.mariee.controller;

import io.micrometer.common.lang.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.web.bind.annotation.*;
import shu.scie.mariee.model.*;
import shu.scie.mariee.repository.DataInfoRepository;
import shu.scie.mariee.repository.DataRepository;
import shu.scie.mariee.repository.HkIpcRepository;
import shu.scie.mariee.repository.PresetRepository;
import shu.scie.mariee.repository.IotreadonlyRepository;
import shu.scie.mariee.service.*;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/hkipc")
public class HkIpcController {

    private final HttpClient httpClient;
    private final HkIpcService hkIpcService;
    private final HkIpcRepository hkIpcRepository;

    private final RobotService robotService;

    private final DataService dataService;

    private final DeviceService deviceService;

    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private final PresetRepository presetRepository;

    private final DataInfoRepository dataInfoRepository;

    private final HashMap<String, ScheduledFutureHolder> scheduleMap = new HashMap<>();

    private final IotreadonlyRepository iotreadonlyRepository;

    public HkIpcController(HkIpcService hkIpcService, HkIpcRepository hkIpcRepository, RobotService robotService,
                           DataService dataService, DeviceService deviceService, ThreadPoolTaskScheduler threadPoolTaskScheduler,
                           PresetRepository presetRepository, DataInfoRepository dataInfoRepository, IotreadonlyRepository iotreadonlyRepository) {
        this.hkIpcRepository = hkIpcRepository;
        this.httpClient = UtilService.createHttpClient();
        this.hkIpcService = hkIpcService;
        this.robotService = robotService;
        this.dataService = dataService;
        this.deviceService = deviceService;
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        this.presetRepository = presetRepository;
        this.dataInfoRepository = dataInfoRepository;
        this.iotreadonlyRepository = iotreadonlyRepository;
    }

    @GetMapping("/startTimer")
    public ApiResult<String> startTimer(@RequestParam("id") Long id) {
        try {
            if (scheduleMap.containsKey(id.toString())) {
                System.out.println("timer" + id + "is already started!");
                return new ApiResult<>(true,"timer starting repeat!");
            }
            AutoService autoService = new AutoService(hkIpcService, id, presetRepository, dataInfoRepository, deviceService);
            String corn = "0 0/1 * * * ? ";

            ScheduledFuture<?> schedule = threadPoolTaskScheduler.schedule(autoService, new CronTrigger(corn));

            ScheduledFutureHolder scheduledFutureHolder = new ScheduledFutureHolder();
            scheduledFutureHolder.setScheduledFuture(schedule);
            scheduledFutureHolder.setRunnableClass(autoService.getClass());
            scheduledFutureHolder.setCorn(corn);

            scheduleMap.put(id.toString(),scheduledFutureHolder);
            System.out.println("start timer with robot id: " + id);
            return new ApiResult<>(true,"start timer!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ApiResult<>(false,"failed to start timer!");

    }

    @RequestMapping("/queryTimer")
    public void queryTask(){
        scheduleMap.forEach((k,v)->{
            System.out.println(k+"  "+v);
        });
    }

    @GetMapping("/stopTimer")
    public ApiResult<String> stopTimer(@RequestParam("id") Long id) {
        if (scheduleMap.containsKey(id.toString())) {
            ScheduledFuture<?> scheduledFuture = scheduleMap.get(id.toString()).getScheduledFuture();
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
                scheduleMap.remove(id.toString());
                System.out.println("stop timer for robot:" + "id");
                return new ApiResult<>(true,"stop service for robot: " + id + " successful!");
            }
        }
        return new ApiResult<>(false,"failed to stop service for robot: " + id );
    }

    @PostMapping("/create")
    public ApiResult<String> create(@RequestBody HkIpc hkIpc) {
        if(hkIpc.id != null) {
            return new ApiResult<>(false, "Not valid hkipc record with not null id");
        }

        try {
            hkIpcRepository.save(hkIpc);
        } catch (Exception e) {
            return new ApiResult<>(false, e.getMessage());
        }
        return new ApiResult<>(true, "success create one hkipc record");
    }

    @DeleteMapping("/delete")
    public ApiResult<String> delete(@RequestParam String name) {
        List<HkIpc> hkIpcList = hkIpcRepository.findByName(name);
        if(hkIpcList.isEmpty()){
            return new ApiResult<>(true, STR."fail to find hkipc record with name: \{name}");
        }

        try {
            hkIpcRepository.deleteById(hkIpcList.getFirst().id);
            return new ApiResult<>(true, STR."success delete hkipc record by name: \{name} id: \{hkIpcList.getFirst().id}");
        } catch (Exception e) {
            return new ApiResult<>(false, e.getMessage());
        }
    }

    @PutMapping("/update")
    public ApiResult<String> insert(@RequestBody HkIpc hkIpc) {
        if(hkIpc.id == null) {
            return new ApiResult<>(false, "Not valid hkipc record with empty id");
        }

        try {
            boolean hkIpcExisted = hkIpcRepository.existsById(hkIpc.id);
            if(hkIpcExisted) {
                hkIpcRepository.save(hkIpc);
                return new ApiResult<>(true, STR."success update one hkipc record with id: \{hkIpc.id}");
            }
            return new ApiResult<>(false, STR."fail to find hkipc record with id \{hkIpc.id}");
        } catch (Exception e) {
            return new ApiResult<>(false, e.getMessage());
        }
    }

    @GetMapping("/query")
    public ApiResult<HkIpc> query(@Nullable @RequestParam Long id, @Nullable @RequestParam String name) {
        if ((id == null) && (name == null || name.isEmpty())) {
            System.out.println("not valid id and name");
            return new ApiResult<>(false, null);
        }

        if (id != null) {
            return new ApiResult<>(true, hkIpcRepository.findById(id).orElse(null));
        }
        return new ApiResult<>(true, hkIpcRepository.findByName(name).getFirst());
    }

    @GetMapping("/queryDevice")
    public ApiResult<List<Device>> queryDevice(@RequestParam("robotId") Long robotId) {
        try {
            List<Device> lists = deviceService.getDeviceByRobotId(robotId);
//            System.out.println("queryDevice!");
            return new ApiResult<>(true,lists);
        } catch (Exception e) {
            System.out.println("no device get");
            return new ApiResult<>(false, null);
        }
    }

    @GetMapping("/queryAllData")
    public ApiResult<List<Data>> queryAllData(@RequestParam("robotId") Long robotId,
                                              @RequestParam("deviceName") String deviceName,
                                              @Nullable @RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
                                              @Nullable @RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        try {
            if (startTime != null && endTime != null) {
                List<Data> dataList = dataService.getByDate(startTime, endTime, robotId, deviceName);
                System.out.println("queryAllData");
                return new ApiResult<>(true, dataList);
            } else  {
                List<Data> dataList = dataService.getAllData(robotId, deviceName);
                System.out.println("queryAllData");
                return new ApiResult<>(true, dataList);
            }
        } catch (Exception e) {
            System.out.println("get no data！");
            return new ApiResult<>(false, null);
        }
    }

    // 根据楼栋、楼层、页码、每页的数量返回机器人的信息
    @GetMapping("/queryRobot")
    public ApiResult<List<Robot>> queryRobot(@RequestParam("building") String building,
                                             @RequestParam("room") String room) {
        try {
            List<Robot> robots = robotService.getRobotsByBuildingAndRoom(building, room);
            System.out.println("queryRobot！");
            return new ApiResult<>(true, robots);
        } catch (Exception e) {
            System.out.println("未获取到机器人信息！");
            return new ApiResult<>(false, null);
        }
    }

    @GetMapping("/queryPresets")
    public ApiResult<List<Preset>> queryPresets(@RequestParam("robotId") Long robotId) {
        try {
            List<Preset> presets = presetRepository.findAllByRobot_id(robotId);
            return new ApiResult<>(true,presets);
        } catch (Exception e) {
            return new ApiResult<>(false,null);
        }
    }

    @GetMapping("/goToPreset")
    public ApiResult<String> goToPreset(@RequestParam("robotId") Long robotId,
                                        @RequestParam("presetId") Long presetId) {
        HkIpc ipc = this.hkIpcService.getById(robotId);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{robotId}");
        }
        TcpClient tcpClient = new TcpClient(ipc);
        tcpClient.gotoPresetPoint(presetId.intValue());
        return new ApiResult<>(true,"i do not know if the robot arrived");
    }

    @GetMapping("/slideLeft")
    public ApiResult<String> slideLeft(@RequestParam("id") Long id) {
        HkIpc ipc = this.hkIpcService.getById(id);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
        }
        TcpClient tcpClient = new TcpClient(ipc);
        tcpClient.right();
        return new ApiResult<>(true,"i do not know if the robot arrived");
    }

    @GetMapping("/slideRight")
    public ApiResult<String> slideRight(@RequestParam("id") Long id) {
        HkIpc ipc = this.hkIpcService.getById(id);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
        }
        TcpClient tcpClient = new TcpClient(ipc);
        tcpClient.left();
        return new ApiResult<>(true,"i do not know if the robot arrived");
    }

    @GetMapping("/liveUrl")
    public ApiResult<String> pan(@RequestParam("id") Long id) {
        HkIpc ipc = this.hkIpcService.getById(id);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
        }

        return new ApiResult<>(true, ipc.liveUrl);
    }

    @GetMapping("/snapshot")
    public ApiResult<String> snapshot(@RequestParam("id") Long id) {
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

    @GetMapping("/getLocation")
    public ApiResult<String> getLocation(@RequestParam("id") Long id) {
        HkIpc ipc = this.hkIpcService.getById(id);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
        }

        HttpRequest req;
        try {
            req = HttpRequest.newBuilder()
                    .uri(new URI(STR."http://\{ipc.ip}/ISAPI/PTZCtrl/channels/\{ipc.ptzChannel}/status"))
                    .GET()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                    .headers(HttpHeaders.AUTHORIZATION, STR."Basic \{Base64.getEncoder().encodeToString(STR."\{ipc.username}:\{ipc.password}".getBytes())}")
                    .build();
        } catch (URISyntaxException e) {
            return new ApiResult<>(false, e.getMessage());
        }


        HttpResponse<String> res;
        try {
            res = this.httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new ApiResult<>(false, e.getMessage());
        }

        if (res.statusCode() != HttpStatus.OK.value()) {
            return new ApiResult<>(false, Integer.toString(res.statusCode()));
        }

        return new ApiResult<>(true, res.body());
    }

    @GetMapping("/pan")
    public ApiResult<String> pan(@RequestParam("id") Long id, @RequestParam("direction") String direction) throws InterruptedException {
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
    public ApiResult<String> tilt(@RequestParam("id") Long id, @RequestParam("direction") String direction) throws InterruptedException {
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
    public ApiResult<String> zoom(@RequestParam("id") Long id, @RequestParam("direction") String direction) throws InterruptedException {
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

        String requestBody = STR."<PTZData version=\"2.0\" xmlns=\"http://www.isapi.org/ver20/XMLSchema\"><\{action}>\{speedPrefix}20</\{action}></PTZData>";

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
