package shu.scie.mariee.controller;

import io.micrometer.common.lang.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
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
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
            HkIpc ipc = this.hkIpcService.getById(id);
            if (ipc == null) {
                return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
            }

            if (scheduleMap.containsKey(id.toString())) {
                System.out.println("timer" + id + "is already started!");
                return new ApiResult<>(true,"timer starting repeat!");
            }

            AutoService autoService = new AutoService(hkIpcService, id, presetRepository, dataInfoRepository, deviceService, dataService, iotreadonlyRepository);
            //String corn = "0 0/20 * * * ? ";
            PeriodicTrigger periodicTrigger = new PeriodicTrigger(ipc.interval_time, TimeUnit.MINUTES);



            ScheduledFuture<?> schedule = threadPoolTaskScheduler.schedule(autoService, periodicTrigger);

            ScheduledFutureHolder scheduledFutureHolder = new ScheduledFutureHolder();
            scheduledFutureHolder.setScheduledFuture(schedule);
            scheduledFutureHolder.setRunnableClass(autoService.getClass());
            scheduledFutureHolder.setinterval(ipc.interval_time);
            //scheduledFutureHolder.setCorn(corn);

            scheduleMap.put(id.toString(),scheduledFutureHolder);
            System.out.println("start timer with robot id: " + id);
            return new ApiResult<>(true,"start timer!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ApiResult<>(false,"failed to start timer!");

    }

    @GetMapping("/queryTimer")
    public ApiResult<String> queryTask(@RequestParam("id") Long id){
        if (scheduleMap.containsKey(id.toString())) {
            ScheduledFutureHolder scheduledFuturehold = scheduleMap.get(id.toString());
            if (scheduledFuturehold != null) {
                return new ApiResult<>(true,"service for robot: " + id + " at "+ scheduledFuturehold.getinterval() +" minutes intervals.");
            }

        }
        return new ApiResult<>(false,"not found service for robot: " + id );
    }

    @GetMapping("/queryPresetNow")
    public ApiResult<String> queryPresetNow(@RequestParam("id") Long id){
        if (TempPreset.robotid_preset.containsKey(String.valueOf(id))) {
            return new ApiResult<>(true,"PRESET IN:" + TempPreset.robotid_preset.get(String.valueOf(id)));
        }
        return new ApiResult<>(false,"PRESET IN:nulll" );
    }

    @PostMapping("/modifyTimer")
    public ApiResult<String> modifyTimer(@RequestParam("robotId") Long robotId,
                                         @RequestParam("intervalTime") Long intervalTime) {
        // get camera
        HkIpc ipc = this.hkIpcService.getById(robotId);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{robotId}");
        }
        this.hkIpcService.updateTimeById(robotId,intervalTime);

        // get service
        if (scheduleMap.containsKey(robotId.toString())) {
            ScheduledFuture<?> scheduledFuture = scheduleMap.get(robotId.toString()).getScheduledFuture();
            scheduledFuture.cancel(true);
            scheduleMap.remove(robotId.toString());

            AutoService autoService = new AutoService(hkIpcService, robotId, presetRepository, dataInfoRepository, deviceService, dataService, iotreadonlyRepository);
            PeriodicTrigger periodicTrigger = new PeriodicTrigger(ipc.interval_time, TimeUnit.MINUTES);

            ScheduledFuture<?> schedule = threadPoolTaskScheduler.schedule(autoService, periodicTrigger);

            ScheduledFutureHolder scheduledFutureHolder = new ScheduledFutureHolder();
            scheduledFutureHolder.setScheduledFuture(schedule);
            scheduledFutureHolder.setRunnableClass(autoService.getClass());
            scheduledFutureHolder.setinterval(ipc.interval_time);

            scheduleMap.put(robotId.toString(),scheduledFutureHolder);

            return new ApiResult<>(true, STR."robot ID: \{robotId} intervals modified to \{intervalTime} minutes and timer restarted.");

        }
        else {
            return new ApiResult<>(true, STR."robot ID: \{robotId} intervals modified to \{intervalTime} minutes but not start.");
        }


    }


    @GetMapping("/stopTimer")
    public ApiResult<String> stopTimer(@RequestParam("id") Long id) {
        // get camera
        HkIpc ipc = this.hkIpcService.getById(id);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
        }

        if (scheduleMap.containsKey(id.toString())) {
            ScheduledFuture<?> scheduledFuture = scheduleMap.get(id.toString()).getScheduledFuture();
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
                scheduleMap.remove(id.toString());
                System.out.println("stop timer for robot:" + "id");
                TempPreset.robotid_preset.put(String.valueOf(id), "");

                //restart timer
                AutoService autoService = new AutoService(hkIpcService, id, presetRepository, dataInfoRepository, deviceService, dataService, iotreadonlyRepository);
                Runnable restartTimer = () -> restarttimer(autoService,ipc,id);
                PeriodicTrigger periodicTrigger = new PeriodicTrigger(10, TimeUnit.MINUTES);
                periodicTrigger.setInitialDelay(10);
                ScheduledFuture<?> schedule = threadPoolTaskScheduler.schedule(restartTimer, periodicTrigger);

                ScheduledFutureHolder scheduledFutureHolder = new ScheduledFutureHolder();
                scheduledFutureHolder.setScheduledFuture(schedule);
                scheduledFutureHolder.setRunnableClass(restartTimer.getClass());
                scheduledFutureHolder.setinterval(10L);

                scheduleMap.put(STR."re\{id.toString()}",scheduledFutureHolder);

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
    public ApiResult<List<DataOne>> queryAllData(@RequestParam("robotId") Long robotId,
                                              @RequestParam("deviceName") String deviceName,
                                              @Nullable @RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
                                              @Nullable @RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        try {
            if (startTime != null && endTime != null) {
                List<Data> dataList = dataService.getByDate(startTime, endTime, robotId, deviceName);
//                Thread.sleep(2000);
//                System.out.println(dataList.size());
//                for (Data fruit : dataList) {
//                    System.out.println(fruit.date);
//                }
                List<DataOne> finalList = CutSeven2One(dataList); //处理后送给前端
                System.out.println("queryAllData");
                return new ApiResult<>(true, finalList);
            } else {
//                System.out.println("--------------------");
//                System.out.println(robotId);
//                System.out.println(deviceName);
                List<Data> dataList = dataService.getAllData(robotId, deviceName);

//                for (Data fruit : dataList) {
//                    System.out.println(fruit.date);
//                }
//                Thread.sleep(2000);
//                dataList.forEach(element -> System.out.println(element.id));
//                System.out.println(dataList.size());
                List<DataOne> finalList = CutSeven2One(dataList); //处理后送给前端
//                System.out.println("queryAllData");
                return new ApiResult<>(true, finalList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(" get no data！");
            return new ApiResult<>(false, null);
        }
    }
    public List<DataOne> CutSeven2One(List<Data> allist){
        List<DataOne> aloneList = new ArrayList<DataOne>();
        Data TempBefore = allist.get(0);
        DataOne one = new DataOne();
        /*
        * 先塞一个进去
        * */
        int index = 1;
        add2Data(one,TempBefore,index);
        add2OtherData(one,TempBefore);

        for (int i = 1; i < allist.size(); i++) {
            index++;
            Data TempNow = allist.get(i);

            /*
            * 如果为False，则说明大于60秒该建立新的one用来塞
            * 如果为true，则说明继续塞到one里面
            * */
            if(CompareData(TempBefore, TempNow) ){
                add2Data(one, TempNow, index);
            } else {
                index = 1;
                aloneList.add(one);
                one = new DataOne();
                add2Data(one, TempNow, index);
                add2OtherData(one,TempBefore);//添加其他信息
            }

            TempBefore = TempNow;
        }
        return aloneList;
    }
    public boolean CompareData(Data TempBefore, Data TempNow) {
        Long compare = TempBefore.date.getTime()/1000 - TempNow.date.getTime()/1000;//秒数相减
        if( compare < 60 && compare > -60){
            return true;// 继续塞进去
        } else {
            return false;// 建立新的one用来塞
        }
    }
    public void add2OtherData(DataOne one, Data TempBefore) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 格式化 Date 对象为字符串
        one.id = TempBefore.id;
        one.date = sdf.format(TempBefore.date);
        one.devicename = TempBefore.devicename;
        one.robotid = TempBefore.robotid;
        one.getby = TempBefore.getby;
        one.imgpath = TempBefore.imgpath;
        one.status = TempBefore.status;
    }
    public void add2Data(DataOne one, Data TempBefore, int index) {
        if(TempBefore.name.contains("旋钮")){
            one.name1 = TempBefore.name;
            one.result1 = TempBefore.result;
        }else if(TempBefore.name.contains("ICU排风机运行指示")){
            one.name2 = TempBefore.name;
            one.result2 = TempBefore.result;
        }else if(TempBefore.name.contains("风机停止指示")){
            one.name3 = TempBefore.name;
            one.result3 = TempBefore.result;
        }else if(TempBefore.name.contains("风机运行指示")){
            one.name4 = TempBefore.name;
            one.result4 = TempBefore.result;
        }else if(TempBefore.name.contains("ICU排风机故障指示")){
            one.name5 = TempBefore.name;
            one.result5 = TempBefore.result;
        }else if(TempBefore.name.contains("风机防火阀动作指示")){
            one.name6 = TempBefore.name;
            one.result6 = TempBefore.result;
        }else if(TempBefore.name.contains("风机变频器故障指示")){
            one.name7 = TempBefore.name;
            one.result7 = TempBefore.result;
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

        // get camera
        HkIpc ipc = this.hkIpcService.getById(robotId);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{robotId}");
        }

        // go to preset
        Preset preset = presetRepository.findPresetById(presetId);
        TcpClient tcpClient = new TcpClient(ipc);
        TempPreset.robotid_preset.put(String.valueOf(ipc.id), String.valueOf(preset.device.intValue()));
        tcpClient.gotoPresetPoint(preset.device.intValue());

        

        // set ptz of camera
        String requestBody = "<PTZData version=\"2.0\" xmlns=\"http://www.isapi.org/ver20/XMLSchema\"><AbsoluteHigh>" +
                "<elevation>"+preset.p+"</elevation><azimuth>"+preset.t+"</azimuth><absoluteZoom>"+preset.z+"</absoluteZoom>" +
                "</AbsoluteHigh></PTZData>";
        HttpRequest moveReq =null;
        try {
            moveReq = this.makeIpcAbsoluteRequest(ipc, requestBody);
        } catch (URISyntaxException e) {
            System.out.println("Start auto pilot request failed:" + e.getMessage());
        }
        HttpResponse<String> moveRes = null;
        try {
            moveRes = this.httpClient.send(moveReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("Start auto pilot request failed: "+ e.getMessage());
        }
        return new ApiResult<>(true,"i do not know if the robot arrived");
    }

    @GetMapping("/slideLeft")
    public ApiResult<String> slideLeft(@RequestParam("id") Long id) {
        HkIpc ipc = this.hkIpcService.getById(id);
        if (ipc == null) {
            return new ApiResult<>(false, STR."HkIpc Not Found with ID: \{id}");
        }
        //维护一个字典来记录当前预置点位置
        TempPreset.robotid_preset.put(String.valueOf(ipc.id), "e");
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
        //维护一个字典来记录当前预置点位置
        TempPreset.robotid_preset.put(String.valueOf(ipc.id), "d");
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

    @GetMapping("/")
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
        //维护一个字典来记录当前预置点位置
        TempPreset.robotid_preset.put(String.valueOf(ipc.id), "c");

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
        //维护一个字典来记录当前预置点位置
        TempPreset.robotid_preset.put(String.valueOf(ipc.id), "b");

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
        //维护一个字典来记录当前预置点位置
        TempPreset.robotid_preset.put(String.valueOf(ipc.id), "a");

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

    private HttpRequest makeIpcAbsoluteRequest(HkIpc ipc, String body) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(STR."http://\{ipc.ip}/ISAPI/PTZCtrl/channels/\{ipc.ptzChannel}/absolute"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .headers(HttpHeaders.AUTHORIZATION, STR."Basic \{Base64.getEncoder().encodeToString(STR."\{ipc.username}:\{ipc.password}".getBytes())}")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    public void restarttimer(AutoService autoService, HkIpc ipc, Long robotId){
        if (scheduleMap.containsKey(STR."re\{robotId.toString()}")) {

            ScheduledFuture<?> scheduledFuture = scheduleMap.get(STR."re\{robotId.toString()}").getScheduledFuture();
            scheduledFuture.cancel(false);
            scheduleMap.remove(STR."re\{robotId.toString()}");

            PeriodicTrigger periodicTrigger = new PeriodicTrigger(ipc.interval_time, TimeUnit.MINUTES);

            ScheduledFuture<?> schedule = threadPoolTaskScheduler.schedule(autoService, periodicTrigger);

            ScheduledFutureHolder scheduledFutureHolder = new ScheduledFutureHolder();
            scheduledFutureHolder.setScheduledFuture(schedule);
            scheduledFutureHolder.setRunnableClass(autoService.getClass());
            scheduledFutureHolder.setinterval(ipc.interval_time);

            scheduleMap.put(robotId.toString(),scheduledFutureHolder);
            System.out.println("restart timer for robot:" + robotId);
        }


    }
}

