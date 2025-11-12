package com.broadcom.demo.firstdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PCFExpertAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(PCFExpertAgentApplication.class, args);
	}

//     @Bean
//     PromptChatMemoryAdvisor promptChatMemoryAdvisor(DataSource dataSource) {
//         var jdbc = JdbcChatMemoryRepository
//                 .builder()
//                 .dataSource(dataSource)
//                 .build();

//         var chatMessageWindow = MessageWindowChatMemory
//                 .builder()
//                 .chatMemoryRepository(jdbc)
//                 .build();

//         return PromptChatMemoryAdvisor
//                 .builder(chatMessageWindow)
//                 .build();
//     }
}
