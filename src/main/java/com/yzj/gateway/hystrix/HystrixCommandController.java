package com.yzj.gateway.hystrix;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HystrixCommandController {

    private final Logger log = LoggerFactory.getLogger(HystrixCommandController.class);

    /**
     * 单个服务短路
     * @return
     */
    @RequestMapping("/fallback")
    public String fallback() {
         return "error";
    }

//    /**
//     * 断路器
//     */
//    @HystrixCommand(commandKey = "fallbackcmd" ,defaultFallback = "/fallback")
//    public void fallbackcmd(){
//        log.info("authHystrixCommand测试");
//        System.out.println("1111111111111111");
//    }
}
