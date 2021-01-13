package com.hdconsulting.springboot.webflux.app.controllers;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;

import com.hdconsulting.springboot.webflux.app.models.documents.Producto;
import com.hdconsulting.springboot.webflux.app.models.services.ProductoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SessionAttributes("producto") //on met l'attribute producto dans la session
@Controller
public class ProductoController {
	
	@Autowired
	private ProductoService service;
	
	private static final Logger log = LoggerFactory.getLogger(ProductoController.class);
	
	@GetMapping({"/listar", "/"})
	public String listar(Model model) {
		
		Flux<Producto> productos = service.findAllConNombreUpperCase();
		
		productos.subscribe(prod -> log.info(prod.getNombre()));
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return "listar";
	}
	
	@GetMapping("/form")
	public Mono<String> crear(Model model){
		
		model.addAttribute("producto", new Producto());
		model.addAttribute("titulo", "Formulario de producto");
		
		return Mono.just("form");
	}
	
	@GetMapping("/form/{id}")
	public Mono<String> editar(@PathVariable String id, Model model){
		Mono<Producto> productoMono = service.findById(id).doOnNext(p -> {
			//pour afficher le nom du produit
			log.info("Producto: " + p.getNombre());
		}).defaultIfEmpty(new Producto());
		
		model.addAttribute("titulo", "Editar Producto");
		model.addAttribute("producto", productoMono);
		
		return Mono.just("form");
		
	}
	
	@PostMapping("/form")
	public Mono<String> guardar(Producto producto, SessionStatus status /*pour supprimer les attributes de la session*/){
		status.setComplete();
		 return service.save(producto).doOnNext(p -> {
			log.info("Producto guardado: " + p.getNombre() + "Id: " + p.getId());
		}).thenReturn("redirect:/listar");
		
	}
	@GetMapping("/listar-datadriver")
	public Mono<String> listarDataDriver(Model model) {
		
		Flux<Producto> productos = service.findAllConNombreUpperCase().delayElements(Duration.ofSeconds(1));
		
		productos.subscribe(prod -> log.info(prod.getNombre()));
		//ReactiveDataDriverContextVariable -> pour pas attendre que le flux soit "completed"
		//pour etre consomme
		model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 2));
		model.addAttribute("titulo", "Listado de productos");
		return Mono.just("listar");
	}
	
	
	@GetMapping("/listar-full")
	public String listarFull(Model model) {
		
		Flux<Producto> productos = service.findAllConNombreUpperCaseRepeat(5000);
		

		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return "listar";
	}
	
	@GetMapping("/listar-chunked")
	public String listarChunked(Model model) {
		
		Flux<Producto> productos = service.findAllConNombreUpperCaseRepeat(5000);
		

		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return "listar-chunked";
	}

}
