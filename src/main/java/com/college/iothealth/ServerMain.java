package com.college.iothealth;

import com.college.iothealth.config.DBConnection;
import com.college.iothealth.config.DatabaseConfig;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;

/**
 * Embedded Server Runner for zero-configuration, one-command execution.
 * Fulfills the requirement to provide an easy, reliable runner for examination evaluation:
 * Execute simply via: mvn compile exec:java
 */
public class ServerMain {

    public static void main(String[] args) throws Exception {
        int port = DatabaseConfig.getServerPort();

        System.out.println("================================================================================");
        System.out.println("   IoT-Based Health Monitoring System (Java + JDBC + Oracle)            ");
        System.out.println("================================================================================");

        // 1. Test database connection at startup
        System.out.println("[*] Checking Oracle Database connectivity...");
        boolean dbOk = DBConnection.testConnection();
        if (dbOk) {
            System.out.println("[+] Database status: CONNECTED & READY");
        } else {
            System.out.println("[-] Database status: UNREACHABLE / NOT YET CONFIGURED");
            System.out.println("    Note: Update src/main/resources/db.properties or export DB_URL, DB_USER, DB_PASSWORD");
            System.out.println("    The web application will still start so you can access the UI and diagnostics.");
        }

        // 2. Base directory for Tomcat temp work
        File baseDir = new File("target/tomcat");
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(baseDir.getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector(); // Initialize default HTTP connector

        String webappDirLocation = new File("src/main/webapp/").getAbsolutePath();
        StandardContext ctx = (StandardContext) tomcat.addWebapp("", webappDirLocation);
        
        // Critical for embedded Tomcat in custom ClassLoaders
        ctx.setParentClassLoader(ServerMain.class.getClassLoader());
        ctx.setSessionTimeout(60); // 60 minutes session timeout

        System.out.println("[*] Web application context configured from: " + webappDirLocation);

        // Map compiled classes into the WebResourceRoot so Servlets and DAOs are discovered
        File additionWebInfClasses = new File("target/classes");
        if (additionWebInfClasses.exists()) {
            WebResourceRoot resources = new StandardRoot(ctx);
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                    additionWebInfClasses.getAbsolutePath(), "/"));
            ctx.setResources(resources);
        }

        // 3. Start Tomcat Engine
        tomcat.start();

        System.out.println("================================================================================");
        System.out.println("   APPLICATION SERVER RUNNING SUCCESSFULLY!                                     ");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("   Local Web Access : http://localhost:" + port);
        System.out.println("   Health Status    : http://localhost:" + port + "/api/health");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("   DEFAULT DEMO CREDENTIALS:                                                    ");
        System.out.println("   1. Administrator : admin       / Admin@123                                   ");
        System.out.println("   2. Staff Nurse   : nurse_priya / Nurse@123                                   ");
        System.out.println("================================================================================");
        System.out.println("[*] Press Ctrl+C in terminal to stop server.\n");

        tomcat.getServer().await();
    }
}
