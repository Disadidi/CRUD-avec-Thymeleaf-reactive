package com.hdconsulting.springboot.webflux.app.controllers;

import java.time.Duration;
import java.util.Date;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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

@SessionAttributes("producto") // on met l'attribute producto dans la session
@Controller
public class ProductoController {

	@Autowired
	private ProductoService service;

	private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

	@GetMapping({ "/listar", "/" })
	public String listar(Model model) {

		Flux<Producto> productos = service.findAllConNombreUpperCase();

		productos.subscribe(prod -> log.info(prod.getNombre()));
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return "listar";
	}

	@GetMapping("/form")
	public Mono<String> crear(Model model) {

		model.addAttribute("producto", new Producto());
		model.addAttribute("titulo", "Formulario de producto");
		model.addAttribute("boton", "Crear");

		return Mono.just("form");
	}

	@GetMapping("/form-v2/{id}")
	public Mono<String> editarV2(@PathVariable String id, Model model) {
		return service.findById(id).doOnNext(p -> {
			// pour afficher le nom du produit
			log.info("Producto: " + p.getNombre());
			/*
			 * effet perver de cette solution, ces paramétres ne se garde pas dans la
			 * session, car ils sont executés dans un autre fils (thread)
			 */
			model.addAttribute("boton", "Editar");
			model.addAttribute("titulo", "Editar Producto");
			model.addAttribute("producto", p);
		}).defaultIfEmpty(new Producto()).flatMap(p -> {
			if (p.getId() == null) {
				return Mono.error(new InterruptedException("No existe el producto"));
			}
			return Mono.just(p);
		}).then(Mono.just("form"))
				.onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));

	}

	@GetMapping("/form/{id}")
	public Mono<String> editar(@PathVariable String id,

			Model model) {
		Mono<Producto> productoMono = service.findById(id).doOnNext(p -> {
			// pour afficher le nom du produit
			log.info("Producto: " + p.getNombre());
		}).defaultIfEmpty(new Producto());

		model.addAttribute("boton", "Editar");
		model.addAttribute("titulo", "Editar Producto");
		model.addAttribute("producto", productoMono);

		return Mono.just("form");

	}

	@PostMapping("/form")
	public Mono<String> guardar(@Valid Producto producto, BindingResult result /* toujours après l'objet a valider */,
			Model model, SessionStatus status /* pour supprimer les attributes de la session */) {
		if (result.hasErrors()) {
			model.addAttribute("boton", "Guardar");
			model.addAttribute("titulo", "Errores en formulario Producto");
			return Mono.just("form");

		} else {
			status.setComplete();
			if (producto.getCreateAt() == null) {
				producto.setCreateAt(new Date());
			}
			return service.save(producto).doOnNext(p -> {
				log.info("Producto guardado: " + p.getNombre() + "Id: " + p.getId());
			}).thenReturn("redirect:/listar?success=Producto+guardado+con+exito");

		}
	}
	
	@GetMapping("/eliminar/{id}")
	public Mono<String> eliminar(@PathVariable String id){
		return service.findById(id)
				.defaultIfEmpty(new Producto()).flatMap(p -> {
					if (p.getId() == null) {
						return Mono.error(new InterruptedException("No existe el product a eliminar!"));
					}
					return Mono.just(p);
				})
				.flatMap(service::delete).then(Mono.just("redirect:/listar?success=producto+eliminado+con+exito"))
				.onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto+a+eliminar"));
	}

	@GetMapping("/listar-datadriver")
	public Mono<String> listarDataDriver(Model model) {

		Flux<Producto> productos = service.findAllConNombreUpperCase().delayElements(Duration.ofSeconds(1));

		productos.subscribe(prod -> log.info(prod.getNombre()));
		// ReactiveDataDriverContextVariable -> pour pas attendre que le flux soit
		// "completed"
		// pour etre consomme
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
