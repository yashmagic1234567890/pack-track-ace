package com.freshtrack.config;

import com.freshtrack.entity.*;
import com.freshtrack.repository.InvoiceRepository;
import com.freshtrack.repository.UserRepository;
import com.freshtrack.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Seeds baseline data on first run: a Central Admin, sample warehouses, a couple
 * of Hub Users (mapped to warehouses) and a demo invoice. Idempotent — does
 * nothing if users already exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Data already present, skipping seed.");
            return;
        }
        log.info("Seeding initial FreshTrack data...");

        Warehouse del = warehouseRepository.save(Warehouse.builder()
                .warehouseCode("WH-DEL-01").name("Delhi North Hub").location("Delhi, IN").build());
        Warehouse mum = warehouseRepository.save(Warehouse.builder()
                .warehouseCode("WH-MUM-01").name("Mumbai West Hub").location("Mumbai, IN").build());
        Warehouse blr = warehouseRepository.save(Warehouse.builder()
                .warehouseCode("WH-BLR-01").name("Bengaluru South Hub").location("Bengaluru, IN").build());

        userRepository.save(User.builder()
                .username("admin").email("admin@freshtrack.io")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Central Administrator").role(Role.CENTRAL_ADMIN)
                .enabled(true).build());

        userRepository.save(User.builder()
                .username("hubdel").email("hubdel@freshtrack.io")
                .password(passwordEncoder.encode("hub123"))
                .fullName("Delhi Hub Operator").role(Role.HUB_USER)
                .enabled(true).warehouses(Set.of(del)).build());

        userRepository.save(User.builder()
                .username("hubmum").email("hubmum@freshtrack.io")
                .password(passwordEncoder.encode("hub123"))
                .fullName("Mumbai Hub Operator").role(Role.HUB_USER)
                .enabled(true).warehouses(Set.of(mum, blr)).build());

        // Demo invoice for the Delhi hub
        Invoice inv = Invoice.builder()
                .invoiceBusinessId("INV-1001").vendorName("FreshFarms Pvt Ltd")
                .warehouse(del).status(InvoiceStatus.PENDING).uploadedBy("admin").build();
        inv.addLine(InvoiceLine.builder().itemSku("SKU-APPLE-001").itemName("Royal Gala Apple")
                .expectedQuantity(50).receivedQuantity(0).build());
        inv.addLine(InvoiceLine.builder().itemSku("SKU-BANANA-002").itemName("Cavendish Banana")
                .expectedQuantity(80).receivedQuantity(0).build());
        inv.addLine(InvoiceLine.builder().itemSku("SKU-TOMATO-003").itemName("Roma Tomato")
                .expectedQuantity(40).receivedQuantity(0).build());
        invoiceRepository.save(inv);

        log.info("Seed complete. Login: admin/admin123, hubdel/hub123, hubmum/hub123");
    }
}
