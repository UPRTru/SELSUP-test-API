package api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final static String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final String STRING_FORMAT = "yyyy-MM-dd";
    private final DateFormat FORMAT = new SimpleDateFormat(STRING_FORMAT);

    private final int requestLimit;
    private int requests = 0;
    private final long timeInterval;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new GsonBuilder().setDateFormat(STRING_FORMAT).create();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeInterval = timeUnit.toMillis(1);
        if (requestLimit > 0) {
            this.requestLimit = requestLimit;
        } else {
            throw new IllegalArgumentException("Значение должно быть больше 0");
        }
    }

    @Data
    @RequiredArgsConstructor
    public class Product {
        String certificate_document;
        Date certificate_document_date;
        String certificate_document_number;
        String owner_inn;
        String producer_inn;
        Date production_date;
        String tnved_code;
        String uit_code;
        String uitu_code;
    }

    @Data
    @RequiredArgsConstructor
    public class Description {
        String participantInn;
    }

    @Data
    @RequiredArgsConstructor
    public class Doc {
        Description description;
        String doc_id;
        String doc_status;
        HashMap<String, Integer> doc_type = new HashMap<>();
        Boolean importRequest;
        String owner_inn;
        String participant_inn;
        String producer_inn;
        Date production_date;
        String production_type;
        ArrayList<Product> products = new ArrayList<>();
        Date reg_date;
        String reg_number;
    }

    public Description addDescription (String participantInn) {
        Description result = new Description();
        result.setParticipantInn(participantInn);
        return result;
    }

    //Создание документа из json с проверками на существование полей в json
    public Doc addDocJson(String json) {
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        Doc result = new Doc();
        try {
            result.setDescription(addDescription(jsonObject.get("description").getAsJsonObject().get("participantInn").getAsString()));
        } catch (Exception e) {
            result.setDescription(null);
        }
        try {
            result.setDoc_id(jsonObject.get("doc_id").getAsString());
        } catch (Exception e) {
            result.setDoc_id(null);
        }
        try {
            result.setDoc_status(jsonObject.get("doc_status").getAsString());
        } catch (Exception e) {
            result.setDoc_status(null);
        }
        try {
            result.setImportRequest(jsonObject.get("importRequest").getAsBoolean());
        } catch (Exception e) {
            result.setImportRequest(null);
        }
        try {
            result.setOwner_inn(jsonObject.get("owner_inn").getAsString());
        } catch (Exception e) {
            result.setOwner_inn(null);
        }
        try {
            result.setParticipant_inn(jsonObject.get("participant_inn").getAsString());
        } catch (Exception e) {
            result.setParticipant_inn(null);
        }
        try {
            result.setProducer_inn(jsonObject.get("producer_inn").getAsString());
        } catch (Exception e) {
            result.setProducer_inn(null);
        }
        try {
            result.setProduction_date(FORMAT.parse(jsonObject.get("production_date").getAsString()));
        } catch (Exception e) {
            result.setProduction_date(null);
        }
        try {
            result.setProduction_type(jsonObject.get("production_type").getAsString());
        } catch (Exception e) {
            result.setProduction_type(null);
        }
        try {
            result.setReg_date(FORMAT.parse(jsonObject.get("production_date").getAsString()));
        } catch (Exception e) {
            result.setReg_date(null);
        }
        try {
            result.setReg_number(jsonObject.get("reg_number").getAsString());
        } catch (Exception e) {
            result.setReg_number(null);
        }
        try {
            for(String key : jsonObject.get("doc_type").getAsJsonObject().keySet()) {
                result.getDoc_type().put(key, jsonObject.get("doc_type").getAsJsonObject().get(key).getAsInt());
            }
        } catch (Exception ignored) {}
        try {
            for(JsonElement jsonElement : jsonObject.get("products").getAsJsonArray()) {
                result.getProducts().add(gson.fromJson(jsonElement.toString(), Product.class));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public String getDocJson(Doc doc) {
        return gson.toJson(doc);
    }

    //401 Unauthorized Error («отказ в доступе»)
    public synchronized void httpRequest(String Doc, String signature) {
        //проверка: ограничение на количество запросов
        if (requests < requestLimit) {
            requests++;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(URL))
                        .header("Signature", signature)
                        .POST(HttpRequest.BodyPublishers.ofString(Doc))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println(response.statusCode());
                //после запроса счетчик сбрасывается на 1
                requests--;
            } catch (Exception e) {
                //после запроса счетчик сбрасывается на 1
                requests--;
                e.printStackTrace();
            }
        } else {
            try {
                //ожидание места для новых запросов
                Thread.sleep(timeInterval);
                httpRequest(Doc, signature);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        CrptApi crptApi = new CrptApi(TimeUnit.HOURS, 5);

        String docJson = "{\"description\":{ \"participantInn\": \"string\" },\n" +
                "\"doc_id\": \"string\",\n" +
                "\"doc_status\": \"string\",\n" +
                "\"doc_type\": { \"LP_INTRODUCE_GOODS\": 109 },\n" +
                "\"importRequest\": true,\n" +
                "\"owner_inn\": \"string\",\n" +
                "\"participant_inn\": \"string\",\n" +
                "\"producer_inn\": \"string\",\n" +
                "\"production_date\": \"2020-01-23\",\n" +
                "\"production_type\": \"string\",\n" +
                "\"products\": [ { \"certificate_document\": \"string\",\n" +
                "\t\t\t\t\"certificate_document_date\": \"2020-01-23\",\n" +
                "\t\t\t\t\"certificate_document_number\": \"string\",\n" +
                "\t\t\t\t\"owner_inn\": \"string\",\n" +
                "\t\t\t\t\"producer_inn\": \"string\",\n" +
                "\t\t\t\t\"production_date\": \"2020-01-23\",\n" +
                "\t\t\t\t\"tnved_code\": \"string\",\n" +
                "\t\t\t\t\"uit_code\": \"string\",\n" +
                "\t\t\t\t\"uitu_code\": \"string\" } ],\n" +
                "\"reg_date\": \"2020-01-23\",\n" +
                "\"reg_number\": \"string\"}";

        //проверка на создание документа из json
        Doc doc = crptApi.addDocJson(docJson);
        String signature = "Signature";

        try {
            //проверка на создание json из документа
            String docToJsonTest = crptApi.getDocJson(doc);

            //осмотр документа в консоли
            System.out.println(docToJsonTest);
            //отправка документа на сервер
            crptApi.httpRequest(docToJsonTest, signature);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}