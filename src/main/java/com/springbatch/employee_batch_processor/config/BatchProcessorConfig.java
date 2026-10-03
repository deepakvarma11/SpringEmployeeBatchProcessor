package com.springbatch.employee_batch_processor.config;

import com.springbatch.employee_batch_processor.model.Employee;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchProcessorConfig {

    private int attempts = 0;
    @Bean
    public ItemProcessor<Employee, Employee> employeeProcess(){

        return employee -> {

            System.out.println(
                    "Processing employee: " + employee.getId()
            );

            employee.setName(employee.getName().trim());

            employee.setDepartment(employee.getDepartment().trim().toUpperCase());

            if(employee.getSalary()<40000){
                return null;
            }
            double annualSalary = employee.getSalary() * 12;
            employee.setAnnualSalary(annualSalary);

            double monthlySalary = employee.getSalary();
            if(monthlySalary>=40000 && monthlySalary <=59999){
                employee.setSalaryGrade("C");
            } else if (monthlySalary >= 60000 && monthlySalary <= 100000) {
                employee.setSalaryGrade("B");
            } else {
                employee.setSalaryGrade("A");
            }

            String department = employee.getDepartment();
            double bonusPercentage;
            if(department.equalsIgnoreCase("IT")){
                bonusPercentage = 10.0;
            } else if (department.equalsIgnoreCase("Finance")) {
                bonusPercentage = 7.0;
            } else if (department.equalsIgnoreCase("HR")) {
                bonusPercentage = 5.0;
            }else {
                bonusPercentage = 3.0;
            }

            employee.setBonusPercentage(bonusPercentage);

            // 7. Calculate bonus amount
            double bonusAmount =
                    employee.getSalary() * bonusPercentage / 100;

            employee.setBonusAmount(bonusAmount);

            return employee;
        };
    }
}
