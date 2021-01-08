package com.hdconsulting.springboot.webflux.app.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hdconsulting.springboot.webflux.app.models.dao.ProductoDao;
import com.hdconsulting.springboot.webflux.app.models.documents.Producto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/productos")
public class ProductoRestController {
	
	@Autowired
	private ProductoDao dao;
	
	private static final Logger log = LoggerFactory.getLogger(ProductoRestController.class);
	
	@GetMapping()
	public Flux<Producto> index(){
		
		Flux<Producto> productos = dao.findAll()
				.map(producto -> {
					producto.setNombre(producto.getNombre().toUpperCase());
					return producto;
				}).doOnNext(prod -> log.info(prod.getNombre()));
		return productos;
		
	}
	
	@GetMapping("/{id}")
	public Mono<Producto> show(@PathVariable String id){
		
		Mono<Producto> producto = dao.findById(id);
		
		return producto;
	}
}
