package com.csm;

import com.csm.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.Properties;

@SpringBootApplication
public class CsmApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(CsmApplication.class, args);
	}

}
