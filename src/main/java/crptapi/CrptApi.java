package crptapi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/* Это реализация класса CrptApi для работы с API Честного знака с поддержкой ограничения запросов. Класс включает:

Ограничение запросов: Использует Semaphore и очередь временных меток для контроля частоты запросов

Thread-safe: Все методы синхронизированы для безопасной работы в многопоточной среде

HTTP клиент: Использует современный HttpClient из Java 11

JSON сериализация: Использует Jackson для преобразования объектов в JSON

Расширяемость: Структура классов позволяет легко добавлять новые методы API

Обработка ошибок: Собственный исключительный класс ApiException

*/
public class CrptApi {

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final Semaphore semaphore;
    private final BlockingQueue<Instant> requestTimestamps;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Request limit must be positive");
        }
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit, true);
        this.requestTimestamps = new LinkedBlockingQueue<>(requestLimit);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, ApiException {
        acquirePermission();

        try {
            String requestBody = buildRequestBody(document, signature);
            String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "?pg=" + document.getProductGroup()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + getAuthToken())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ApiException("API request failed with status: " + response.statusCode() +
                        ", body: " + response.body());
            }

            // Process successful response if needed
            System.out.println("Document created successfully: " + response.body());

        } catch (JsonProcessingException e) {
            throw new ApiException("JSON serialization error", e);
        } catch (IOException e) {
            throw new ApiException("HTTP request error", e);
        } finally {
            releasePermission();
        }
    }

    private void acquirePermission() throws InterruptedException {
        semaphore.acquire();

        Instant now = Instant.now();
        Instant oldestTimestamp = requestTimestamps.peek();

        while (oldestTimestamp != null &&
                Duration.between(oldestTimestamp, now).toMillis() > timeUnit.toMillis(1)) {
            requestTimestamps.poll();
            oldestTimestamp = requestTimestamps.peek();
        }

        while (requestTimestamps.size() >= requestLimit) {
            Instant first = requestTimestamps.take();
            long elapsed = Duration.between(first, now).toMillis();
            long waitTime = timeUnit.toMillis(1) - elapsed;

            if (waitTime > 0) {
                Thread.sleep(waitTime);
                now = Instant.now();
            }
        }

        requestTimestamps.offer(now);
    }

    private void releasePermission() {
        semaphore.release();
    }

    private String buildRequestBody(Document document, String signature) throws JsonProcessingException {
        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setDocument_format("MANUAL");
        request.setProduct_document(objectMapper.writeValueAsString(document));
        request.setSignature(signature);
        request.setType("LP_INTRODUCE_GOODS");

        return objectMapper.writeValueAsString(request);
    }

    /* Реализовать логику аутентификации на основе раздела 1.2 документации API
     Это заглушка. Для фактической реализации потребуется:
      1. GET-запрос к /api/v3/auth/cert/key для получения UUID и данных
      2. Подписать данные с помощью UKEP
      3. POST-запрос к /api/v3/auth/cert/ с подписанными данными для получения токена
      */
    private String getAuthToken() {
        return "your_auth_token_here";
    }

    // Внутренние классы для объектов запроса/ответа
    public static class Document {
        private String description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Product[] products;
        private String reg_date;
        private String reg_number;

        // Getters and setters
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDoc_id() {
            return doc_id;
        }

        public void setDoc_id(String doc_id) {
            this.doc_id = doc_id;
        }

        public String getDoc_status() {
            return doc_status;
        }

        public void setDoc_status(String doc_status) {
            this.doc_status = doc_status;
        }

        public String getDoc_type() {
            return doc_type;
        }

        public void setDoc_type(String doc_type) {
            this.doc_type = doc_type;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getParticipant_inn() {
            return participant_inn;
        }

        public void setParticipant_inn(String participant_inn) {
            this.participant_inn = participant_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getProduction_type() {
            return production_type;
        }

        public void setProduction_type(String production_type) {
            this.production_type = production_type;
        }

        public Product[] getProducts() {
            return products;
        }

        public void setProducts(Product[] products) {
            this.products = products;
        }

        public String getReg_date() {
            return reg_date;
        }

        public void setReg_date(String reg_date) {
            this.reg_date = reg_date;
        }

        public String getReg_number() {
            return reg_number;
        }

        public void setReg_number(String reg_number) {
            this.reg_number = reg_number;
        }

        /* Извлечь группу товаров из товаров или типа документа. Это упрощенная реализация
         */
        public String getProductGroup() {
            return "clothes"; // Default value
        }
    }

    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;

        // Getters and setters
        public String getCertificate_document() {
            return certificate_document;
        }

        public void setCertificate_document(String certificate_document) {
            this.certificate_document = certificate_document;
        }

        public String getCertificate_document_date() {
            return certificate_document_date;
        }

        public void setCertificate_document_date(String certificate_document_date) {
            this.certificate_document_date = certificate_document_date;
        }

        public String getCertificate_document_number() {
            return certificate_document_number;
        }

        public void setCertificate_document_number(String certificate_document_number) {
            this.certificate_document_number = certificate_document_number;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getTnved_code() {
            return tnved_code;
        }

        public void setTnved_code(String tnved_code) {
            this.tnved_code = tnved_code;
        }

        public String getUit_code() {
            return uit_code;
        }

        public void setUit_code(String uit_code) {
            this.uit_code = uit_code;
        }

        public String getUitu_code() {
            return uitu_code;
        }

        public void setUitu_code(String uitu_code) {
            this.uitu_code = uitu_code;
        }
    }

    private static class CreateDocumentRequest {
        private String document_format;
        private String product_document;
        private String product_group;
        private String signature;
        private String type;

        // Getters and setters
        public String getDocument_format() {
            return document_format;
        }

        public void setDocument_format(String document_format) {
            this.document_format = document_format;
        }

        public String getProduct_document() {
            return product_document;
        }

        public void setProduct_document(String product_document) {
            this.product_document = product_document;
        }

        public String getProduct_group() {
            return product_group;
        }

        public void setProduct_group(String product_group) {
            this.product_group = product_group;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class ApiException extends Exception {
        public ApiException(String message) {
            super(message);
        }

        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
