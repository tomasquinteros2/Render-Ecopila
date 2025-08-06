package micro.microservicio_producto.controllers;

import jakarta.annotation.Resource;
import micro.microservicio_producto.entities.NroComprobante;
import micro.microservicio_producto.entities.Producto;
import micro.microservicio_producto.services.ComprobanteService;
import micro.microservicio_producto.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("productos/comprobante")
public class ComprobanteController {

    private ComprobanteService comprobanteService;

    @Autowired
    public ComprobanteController(ComprobanteService cs) {
        this.comprobanteService =  cs;
    }

    @GetMapping("")
    public ResponseEntity<List<NroComprobante>> getComprobante() throws Exception{
        try{
            List<NroComprobante> comprobantes = comprobanteService.findAll();
            return new ResponseEntity<>(comprobantes, HttpStatus.OK);
        }
        catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }
/*
    @GetMapping("/{numero}")
    public ResponseEntity<Resource> obtenerComprobante(@PathVariable String numero) throws Exception {
        return comprobanteService.obtenerComprobante(numero)
                .map(comprobante -> {
                    String contenido = comprobante.getContenidoHtml();
                    ByteArrayResource resource = new ByteArrayResource(contenido.getBytes());

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, "text/html")
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=comprobante_" + numero + ".html")
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }*/
    @PostMapping("")
    public ResponseEntity<Map<String, String>> generarComprobante(@RequestBody String htmlComprobante) {
        try {
            NroComprobante comprobante = comprobanteService.generarComprobanteCompleto(htmlComprobante);

            Map<String, String> response = new HashMap<>();
            response.put("numero", comprobante.getNumeroComprobante());
            response.put("fecha", comprobante.getFechaGeneracion().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/generar")
    public ResponseEntity<NroComprobante> obtenerNumeroComprobante() throws Exception{
        try{
            NroComprobante comprobante = comprobanteService.incrementar();
            return new ResponseEntity<>(comprobante, HttpStatus.OK);
        }
        catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }
}
