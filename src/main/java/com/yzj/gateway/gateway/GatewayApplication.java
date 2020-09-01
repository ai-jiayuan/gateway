package com.yzj.gateway.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringCloudApplication
@EnableFeignClients(basePackages = "com.uniccc.client")
@ComponentScan(basePackages = "com.uniccc")
public class GatewayApplication  {
	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}


}
