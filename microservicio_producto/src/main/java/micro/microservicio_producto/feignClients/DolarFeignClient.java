package micro.microservicio_producto.feignClients;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name="microservicio-dolar")
public interface DolarFeignClient {

        @GetMapping("/{id}")
        ResponseEntity<BigDecimal> getValorDolar(@PathVariable Integer id);

}
