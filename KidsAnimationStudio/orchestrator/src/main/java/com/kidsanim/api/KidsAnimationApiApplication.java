package com.kidsanim.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KidsAnimationApiApplication {

    private static final Logger log = LoggerFactory.getLogger(KidsAnimationApiApplication.class);
    private static final Pattern IP_PATTERN = Pattern.compile("inet\\s+([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)");

    public static void main(String[] args) {
        autoConfigureLocalPostgresHost();
        SpringApplication.run(KidsAnimationApiApplication.class, args);
    }

    private static void autoConfigureLocalPostgresHost() {
        String existingUrl = System.getProperty("DATABASE_URL", System.getenv("DATABASE_URL"));
        if (existingUrl != null && !existingUrl.contains("localhost") && !existingUrl.contains("127.0.0.1")) {
            return;
        }

        // Si localhost:5434 ya responde en loopback, no cambiar nada
        if (isPortOpen("127.0.0.1", 5434, 400)) {
            return;
        }

        // Si localhost:5434 no responde (caso comn en Windows con Podman en WSL2), auto-detectar IP de eth0
        String wslIp = resolveWslPodmanIp();
        if (wslIp != null && isPortOpen(wslIp, 5434, 1000)) {
            String resolvedUrl = "jdbc:postgresql://" + wslIp + ":5434/kidsdb";
            System.setProperty("DATABASE_URL", resolvedUrl);
            System.setProperty("spring.datasource.url", resolvedUrl);
            log.info("[AUTO-DETECT] Base de datos en WSL/Podman detectada automticamente: {}", resolvedUrl);
        }
    }

    private static boolean isPortOpen(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String resolveWslPodmanIp() {
        try {
            Process process = new ProcessBuilder("wsl", "-d", "podman-machine-default", "ip", "-4", "addr", "show", "eth0")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher m = IP_PATTERN.matcher(line);
                    if (m.find()) {
                        return m.group(1);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
