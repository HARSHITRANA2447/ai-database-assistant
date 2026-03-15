package com.aidb.config;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final EntityManager em;

    public DataInitializer(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            createSampleTables();
            log.info("✅ Sample data initialized successfully.");
        } catch (Exception e) {
            log.warn("Sample data may already exist: {}", e.getMessage());
        }
    }

    private void createSampleTables() {
        // Employees
        em.createNativeQuery("""
            CREATE TABLE IF NOT EXISTS employees (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(100),
                email VARCHAR(100),
                department VARCHAR(50),
                salary DECIMAL(10,2),
                joining_date DATE,
                manager_id INT,
                is_active BOOLEAN DEFAULT TRUE
            )""").executeUpdate();

        em.createNativeQuery("""
            MERGE INTO employees (id, name, email, department, salary, joining_date, manager_id)
            KEY(id) VALUES
            (1,'Alice Johnson','alice@company.com','Engineering',95000,'2020-03-15',NULL),
            (2,'Bob Smith','bob@company.com','Engineering',78000,'2021-06-01',1),
            (3,'Carol White','carol@company.com','Marketing',65000,'2019-11-20',NULL),
            (4,'David Brown','david@company.com','Sales',72000,'2022-01-10',NULL),
            (5,'Eva Martinez','eva@company.com','Engineering',88000,'2020-08-05',1),
            (6,'Frank Lee','frank@company.com','HR',60000,'2021-03-22',NULL),
            (7,'Grace Kim','grace@company.com','Marketing',70000,'2023-01-15',3),
            (8,'Henry Wilson','henry@company.com','Sales',75000,'2022-07-30',4),
            (9,'Iris Chen','iris@company.com','Engineering',92000,'2019-05-14',1),
            (10,'James Taylor','james@company.com','HR',58000,'2023-04-01',6)
            """).executeUpdate();

        // Departments
        em.createNativeQuery("""
            CREATE TABLE IF NOT EXISTS departments (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(50),
                budget DECIMAL(12,2),
                location VARCHAR(100),
                head_count INT
            )""").executeUpdate();

        em.createNativeQuery("""
            MERGE INTO departments (id, name, budget, location, head_count)
            KEY(id) VALUES
            (1,'Engineering',500000,'Hyderabad',4),
            (2,'Marketing',200000,'Mumbai',2),
            (3,'Sales',300000,'Delhi',2),
            (4,'HR',150000,'Hyderabad',2)
            """).executeUpdate();

        // Sales / Products
        em.createNativeQuery("""
            CREATE TABLE IF NOT EXISTS products (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(100),
                category VARCHAR(50),
                price DECIMAL(10,2),
                stock INT,
                created_at DATE
            )""").executeUpdate();

        em.createNativeQuery("""
            MERGE INTO products (id, name, category, price, stock, created_at)
            KEY(id) VALUES
            (1,'Laptop Pro','Electronics',85000,'50','2023-01-10'),
            (2,'Wireless Mouse','Electronics',1500,'200','2023-01-15'),
            (3,'Desk Chair','Furniture',12000,'30','2023-02-01'),
            (4,'Notebook Set','Stationery',500,'500','2023-02-10'),
            (5,'Monitor 27"','Electronics',25000,'40','2023-03-01'),
            (6,'Standing Desk','Furniture',35000,'15','2023-03-15'),
            (7,'Keyboard Mech','Electronics',3500,'100','2023-04-01'),
            (8,'Headphones BT','Electronics',5000,'80','2023-04-20')
            """).executeUpdate();

        em.createNativeQuery("""
            CREATE TABLE IF NOT EXISTS sales (
                id INT PRIMARY KEY AUTO_INCREMENT,
                product_id INT,
                employee_id INT,
                quantity INT,
                total_amount DECIMAL(12,2),
                sale_date DATE,
                region VARCHAR(50)
            )""").executeUpdate();

        em.createNativeQuery("""
            MERGE INTO sales (id, product_id, employee_id, quantity, total_amount, sale_date, region)
            KEY(id) VALUES
            (1,1,4,2,170000,'2024-01-05','South'),
            (2,2,8,10,15000,'2024-01-08','North'),
            (3,5,4,3,75000,'2024-01-12','South'),
            (4,3,8,5,60000,'2024-02-01','East'),
            (5,1,4,1,85000,'2024-02-14','South'),
            (6,7,8,20,70000,'2024-02-20','North'),
            (7,4,4,50,25000,'2024-03-01','West'),
            (8,6,8,2,70000,'2024-03-10','East'),
            (9,8,4,15,75000,'2024-03-22','South'),
            (10,1,8,3,255000,'2024-04-05','North'),
            (11,2,4,25,37500,'2024-04-10','South'),
            (12,5,8,2,50000,'2024-04-18','East')
            """).executeUpdate();

        log.info("Sample tables: employees, departments, products, sales — all ready.");
    }
}
