package io.sertaoBit.odontocore.crm;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class Application {

	/**
	 * Fuso horário padrão da aplicação. Configurável via env {@code APP_TIMEZONE};
	 * default America/Sao_Paulo. Em containers (Railway/Docker) a JVM sobe em UTC,
	 * o que faria {@code LocalDateTime.now()}, {@code @CreationTimestamp} e o cron
	 * do RecycleJob produzirem horário UTC — divergindo do frontend, que opera em
	 * horário de Brasília. Fixar o timezone aqui mantém backend e frontend alinhados.
	 */
	private static final String DEFAULT_TIMEZONE = "America/Sao_Paulo";

	public static void main(String[] args) {
		// Definido antes do contexto Spring subir, para que Hibernate e o scheduler
		// já inicializem com o fuso correto.
		String timezone = System.getenv().getOrDefault("APP_TIMEZONE", DEFAULT_TIMEZONE);
		TimeZone.setDefault(TimeZone.getTimeZone(timezone));

		SpringApplication.run(Application.class, args);
	}

}
