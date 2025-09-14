package com.example.crptapidoc;

import crptapi.CrptApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class CrptApiDocApplication {

    public static void main(String[] args) {

        // Создаем экземпляр CrptApi с лимитом 10 запросов в секунду
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);

        // Создаем тестовый документ
        CrptApi.Document document = new CrptApi.Document();
        document.setDescription("Test document");
        document.setDoc_id("test_doc_123");
        document.setDoc_status("DRAFT");
        document.setDoc_type("LP_INTRODUCE_GOODS");
        document.setImportRequest(false);
        document.setOwner_inn("1234567890");
        document.setParticipant_inn("0987654321");
        document.setProducer_inn("1122334455");
        document.setProduction_date("2024-01-01");
        document.setProduction_type("PRODUCTION");
        document.setReg_date("2024-01-01");
        document.setReg_number("REG123");

        // Создаем тестовые продукты
        CrptApi.Product product1 = new CrptApi.Product();
        product1.setCertificate_document("CERT123");
        product1.setCertificate_document_date("2024-01-01");
        product1.setCertificate_document_number("CERT123456");
        product1.setOwner_inn("1234567890");
        product1.setProducer_inn("1122334455");
        product1.setProduction_date("2024-01-01");
        product1.setTnved_code("6203420000");
        product1.setUit_code("UIT123456");
        product1.setUitu_code("UITU123456");

        CrptApi.Product[] products = {product1};
        document.setProducts(products);

        String signature = "test_signature_1234567890";

        // Тестируем несколько вызовов для проверки ограничения скорости
        for (int i = 0; i < 15; i++) {
            try {
                System.out.println("Отправка документа " + (i + 1) + "...");
                api.createDocument(document, signature);
                System.out.println("Документ " + (i + 1) + " отправлен успешно");
            } catch (InterruptedException e) {
                System.err.println("Операция прервана: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (CrptApi.ApiException e) {
                System.err.println("Ошибка API при отправке документа " + (i + 1) + ": " + e.getMessage());
            }

            // Небольшая пауза между запросами
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("Тестирование завершено");
    }
}
