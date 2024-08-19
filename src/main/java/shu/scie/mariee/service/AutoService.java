package shu.scie.mariee.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import shu.scie.mariee.model.*;
import shu.scie.mariee.repository.DataInfoRepository;
import shu.scie.mariee.repository.PresetRepository;
import shu.scie.mariee.repository.IotreadonlyRepository;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.Instant;

/**
 * 定时巡检
 */
public class AutoService implements Runnable {

    private final HkIpcService hkIpcService;

    private final HttpClient httpClient;

    private final Long id;

    private final PresetRepository presetRepository;

    private final DataInfoRepository dataInfoRepository;

    private final DeviceService deviceService;

    private final IotreadonlyRepository iotreadonlyRepository;

    private TcpClient SlideService;

    private final DataService dataService;

    private IOTService IotService;

    public AutoService(HkIpcService hkIpcService, Long id, PresetRepository presetRepository,
                       DataInfoRepository dataInfoRepository, DeviceService deviceService, DataService dataService,IotreadonlyRepository iotreadonlyRepository) {
        this.hkIpcService = hkIpcService;
        this.httpClient = UtilService.createHttpClient();
        this.id = id;
        this.presetRepository = presetRepository;
        this.dataInfoRepository = dataInfoRepository;
        this.deviceService = deviceService;
        this.SlideService = new TcpClient(hkIpcService.getById(id));
        this.dataService = dataService;
        this.IotService = new IOTService("obix","Obix123456");
        this.iotreadonlyRepository = iotreadonlyRepository;

    }

    @Override
    public void run() {
        HkIpc ipc = hkIpcService.getById(id);
        if (ipc == null) {
            System.out.println("no robot select!");
            return;
        }
        List<Preset> presets = presetRepository.findAllByRobot_id(id);
        System.out.println("Time is:" + new Date());
        System.out.println("presetssize:" + presets.size());

        for (int i = 0; i < presets.size(); i++) {

            if (Thread.currentThread().isInterrupted()) {
                System.out.println("Thread interrupted. Exiting loop!");
                return;
            }
            Preset preset = presets.get(i);
            TempPreset.robotid_preset.put(String.valueOf(ipc.id), String.valueOf(preset.device.intValue()));

            String dataInfoId = preset.dataInfoId;
            Long[] dataInfoIdList = stringToArray(dataInfoId);
            List<DataInfo> dataInfos = new ArrayList<>();

            List<Iotreadonly> Iotreads = iotreadonlyRepository.findByDescriptionNotContainingAndPresetidOrderByIdAsc("读",preset.device);
            List<Float> Iot_value_float = new ArrayList<>();
            List<Boolean> Iot_value_bool = new ArrayList<>();
            for (int j = 0; j < Iotreads.size(); j++){
                Iotreadonly Iotread = Iotreads.get(j);
                String Iot_url = Iotread.url;
                if(Iotread.datatype.contains("bool")){
                    Boolean value = IotService.readIOTBool(Iot_url);
                    Iot_value_bool.add(value);
                }
                else {
                    Float value = IotService.readIOTReal(Iot_url);
                    Iot_value_float.add(value);
                }
            }
            List<JSONObject> Iotjsons = getJSONIot(Iotreads, Iot_value_float, Iot_value_bool);


            for (Long dataInfo : dataInfoIdList) {
                DataInfo dataInfo1 = dataInfoRepository.findAllById(dataInfo);
                dataInfos.add(dataInfo1);
            }
            /**
             * 这段代码是每到一个预置点就向左移动一下，摄像头角度向左动一下，焦距向前拉一下，然后再去预置点
             */
            //向左滑动一下
            TcpClient tcpClient = new TcpClient(ipc);
            tcpClient.right();
            //向左动一下摄像头
            String res = this.startIpcMove(ipc, "pan", "left");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            res = this.stopIpcMove(ipc, "pan");
            //焦距往前拉一下
            String res1 = this.startIpcMove(ipc, "zoom", "in");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            res1 = this.stopIpcMove(ipc, "zoom");

            // go to presets
            // 哈希表中转一下
            /**
             * 滑轨移动到指定位置
             */
            SlideService.gotoPresetPoint(preset.device.intValue());//给预置点的编号

            String requestBody1 = "<PTZData version=\"2.0\" xmlns=\"http://www.isapi.org/ver20/XMLSchema\"><zoom>-100</zoom></PTZData>";


            String requestBody = "<PTZData version=\"2.0\" xmlns=\"http://www.isapi.org/ver20/XMLSchema\"><AbsoluteHigh>" +
                    "<elevation>"+preset.p+"</elevation><azimuth>"+preset.t+"</azimuth><absoluteZoom>"+preset.z+"</absoluteZoom>" +
                    "</AbsoluteHigh></PTZData>";

            HttpRequest moveReq1 =null;
            HttpRequest moveReq =null;
            try {
                moveReq1 = this.makeIpcZoomRequest(ipc,requestBody1);
                moveReq = this.makeIpcRequest(ipc, requestBody);
            } catch (URISyntaxException e) {
                System.out.println("Start auto pilot request failed:" + e.getMessage());
            }
            HttpResponse<String> moveRes1 = null;
            HttpResponse<String> moveRes = null;
            try {
                moveRes1 = this.httpClient.send(moveReq1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                Thread.sleep(2000);
                moveRes = this.httpClient.send(moveReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.out.println("Start auto pilot request failed: "+ e.getMessage());
            }
            if (moveRes.statusCode() == HttpStatus.OK.value()) {
                try {
                    Thread.sleep(32000);
                    ApiResult<String> picPath = takePic(ipc);
                    moveRes1 = this.httpClient.send(moveReq1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    String picPath1 = picPath.data;
                    JSONObject jsonData = getJSONString(dataInfos,picPath1,Iotjsons);
                    JSONObject jsonObject = getDetections(jsonData,"http://172.16.104.254:5000/Recognition");
                    if (jsonObject.get("response") == "false") {
                        return;
                    } else {
                        writeData(jsonObject.getJSONObject("response"), id, dataInfos);
                    }
                } catch (InterruptedException e) {
                    System.out.println("Thread is interrupted while sleeping! Exiting.");
                    Thread.currentThread().interrupt();
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Start auto pilot request failed: " + moveRes.statusCode());
            }
            System.out.println("this is the" + i + "time loop for robot:" + id);
        }
    }//

    // make HTTP request
    private HttpRequest makeIpcRequest(HkIpc ipc, String body) throws URISyntaxException {//其实就是absolute
        return HttpRequest.newBuilder()
                .uri(new URI(STR."http://\{ipc.ip}/ISAPI/PTZCtrl/channels/\{ipc.ptzChannel}/absolute"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .headers(HttpHeaders.AUTHORIZATION, STR."Basic \{Base64.getEncoder().encodeToString(STR."\{ipc.username}:\{ipc.password}".getBytes())}")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest makeIpcZoomRequest(HkIpc ipc, String body) throws URISyntaxException {//是makeIpcRequest
        return HttpRequest.newBuilder()
                .uri(new URI(STR."http://\{ipc.ip}/ISAPI/PTZCtrl/channels/\{ipc.ptzChannel}/continuous"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .headers(HttpHeaders.AUTHORIZATION, STR."Basic \{Base64.getEncoder().encodeToString(STR."\{ipc.username}:\{ipc.password}".getBytes())}")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    // take pictures
    private ApiResult<String> takePic(HkIpc ipc) {
        Path snapshotFolderPath = Path.of("./data/snapshots");
        try {
            Files.createDirectories(snapshotFolderPath.normalize());
        } catch (IOException e) {
            return new ApiResult<>(false, null);
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
            return new ApiResult<>(false, null);
        }

        String newFileName = STR."\{Instant.now().toEpochMilli()}.jpg";

        HttpResponse<Path> res;
        try {
            res = this.httpClient.send(req, HttpResponse.BodyHandlers.ofFile(Path.of(snapshotFolderPath.toString(), newFileName)));
        } catch (Exception e) {
            return new ApiResult<>(false, null);
        }

        if (res.statusCode() != HttpStatus.OK.value()) {
            return new ApiResult<>(false, null);
        }
        String finaPath = "127.0.0.1:8080/snapshots/" + newFileName;
        System.out.println("image path:" + finaPath);
        return new ApiResult<>(true, newFileName);
    }


    private static Long[] stringToArray(String stringData) {
        String[] stringArray = stringData.split(","); // 使用逗号分割字符串，得到字符串数组

        Long[] intArray = new Long[stringArray.length]; // 创建一个与字符串数组相同长度的整数数组

        // 将字符串数组中的每个元素转换为整数，并放入新的整数数组中
        for (int i = 0; i < stringArray.length; i++) {
            intArray[i] = Long.parseLong(stringArray[i]);
        }
        return intArray;
    }


    private static JSONObject getJSONString(List<DataInfo> dataInfos, String picUrl, List<JSONObject> Iotjsons) {
        JSONObject json = new JSONObject();
        json.put("url",picUrl);
        JSONArray light = new JSONArray();
        JSONArray bottom = new JSONArray();
        JSONObject json1 = new JSONObject();
        json1.put("id",1);
        JSONObject json2 = new JSONObject();
        JSONArray data = new JSONArray();
        json2.put("id",2);
        StringBuilder upperLimit = new StringBuilder();
        StringBuilder lowerLimit = new StringBuilder();
//        List<String> upperLimit = new ArrayList<>();
//        List<String> lowerLimit = new ArrayList<>();
        for (int i = 0; i < dataInfos.size(); i++) {
            DataInfo dataInfo = dataInfos.get(i);
//            System.out.println(dataInfo.type);
            if (dataInfo.type.equals("light")) {
                upperLimit.append(dataInfo.upper_limit.toString());
                lowerLimit.append(dataInfo.lower_limit.toString());
                if (!json1.containsKey("type")){
                    json1.put("type",dataInfo.type);
                }
                json1.put("preset_boxes" + (i +  1), dataInfo.box);
            } else {
                if (!json1.containsKey("upperLimit")) {
                    json2.put("upperLimit",dataInfo.upper_limit);
                    json2.put("lowerLimit",dataInfo.lower_limit);
                }

                if (!json2.containsKey("type")){
                    json2.put("type",dataInfo.type);
                }
                json2.put("location", dataInfo.box);
            }
        }
        json1.put("upperLimit",upperLimit.toString());
        json1.put("lowerLimit",lowerLimit.toString());
        light.add(json1);
        bottom.add(json2);
        data.add(json1);
        data.add(json2);
        if (Iotjsons != null){
            data.addAll(Iotjsons);
        }
        json.put("data",data);
        return json;
    }

    private static JSONObject getDetections(JSONObject jsonObject, String detectIp) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(detectIp))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                .build();
        JSONObject jsonResponse = new JSONObject();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode()!=HttpStatus.OK.value()) {
                jsonResponse.put("response","false");
                return jsonResponse;
            } else  {
                jsonResponse.put("response",response.body());
                System.out.println(jsonResponse);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return jsonResponse;
    }

    private static List<JSONObject> getJSONIot(List<Iotreadonly> Iotreads, List<Float> Iot_value_float, List<Boolean> Iot_value_bool)
    {
        List<JSONObject> Iotjsons = new ArrayList<>();
        int m=0,n=0;
        for (int i = 0; i < Iotreads.size(); i++) {
            Iotreadonly Iotread = Iotreads.get(i);
            JSONObject Iotjson = new JSONObject();
            Iotjson.put("id",i+3);
            Iotjson.put("type","iot");
            Iotjson.put("deviceNname",Iotread.description);
            if (Iotread.datatype.contains("bool")){
                Iotjson.put("value",Iot_value_bool.get(m));
                m++;
            }
            else {
                Iotjson.put("value",Iot_value_float.get(n));
                n++;
            }
            Iotjson.put("unit","");
            Iotjson.put("status",0);
            Iotjsons.add(Iotjson);
        }
        return Iotjsons;
    }

    private void writeData(JSONObject jsonObject, Long robotId, List<DataInfo> dataInfos) {
        String imgPath = jsonObject.getString("url");
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        String pattern = "yyyy-MM-dd HH:mm:ss";
        try {
            Device devicefresh = deviceService.getDeviceById(dataInfos.get(0).device_id); // 获取第一个
            devicefresh.status = 0L; // 先设为0后面有异常自动设为1
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject dataObject = jsonArray.getJSONObject(i);
                DataInfo dataInfo = dataInfos.get(i);
//                System.out.println(dataInfo);
                Data data = new Data();
                Device devicestatuschange = new Device();
                data.robotid = robotId;
                Device device = deviceService.getDeviceById(dataInfo.device_id);
                data.devicename = device.name;
                SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
                data.date = dateFormat.parse(dataObject.getString("time"));
                data.name = dataInfo.name;
                data.result = dataObject.getString("result");
                data.status = dataObject.getLong("status");
                if( data.status == 1){ // 如果有异常就设为 true
                    devicefresh.status = 1L;
                }
                data.imgpath = imgPath;
                data.getby = imgPath == null ? 1L : 0L;
                dataService.insertDate(data);
                dataService.deviceStatus(devicestatuschange);
                // System.out.println(data);
            }
            dataService.deviceStatus(devicefresh);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * 这段代码直接复制的HkIpcController，用于完成每次到预置点前就乱动一下的任务
     * @param ipc
     * @param action
     * @param direction
     * @return
     */
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

}
