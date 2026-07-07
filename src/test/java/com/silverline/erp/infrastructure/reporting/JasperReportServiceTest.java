package com.silverline.erp.infrastructure.reporting;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import net.sf.jasperreports.engine.JasperCompileManager;

public class JasperReportServiceTest {

    @Test
    public void testCompileReport() throws Exception {
        System.setProperty("net.sf.jasperreports.compiler.xml.validation", "false");
        Thread.currentThread().setContextClassLoader(net.sf.jasperreports.engine.xml.JRXmlLoader.class.getClassLoader());
        InputStream reportStream = getClass().getResourceAsStream("/reports/sales_report.jrxml");
        if (reportStream == null) {
            throw new RuntimeException("Report file not found!");
        }
        try {
            // Let's parse with standard Java SAX parser to find the exact XML parse error
            javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            javax.xml.parsers.SAXParser parser = factory.newSAXParser();
            
            // Re-read stream since getResourceAsStream returns a new stream
            InputStream testStream = getClass().getResourceAsStream("/reports/sales_report.jrxml");
            parser.parse(testStream, new org.xml.sax.helpers.DefaultHandler());
            System.out.println("SAX Parsing check passed successfully!");
        } catch (Exception saxException) {
            System.err.println("SAX PARSER ERROR DETAILS:");
            saxException.printStackTrace();
        }

        try {
            JasperCompileManager.compileReport(reportStream);
            System.out.println("Compilation successful!");
        } catch (Exception e) {
            System.err.println("TOP-LEVEL ERROR: " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) {
                System.err.println("CAUSED BY: " + cause.getClass().getName() + " -> " + cause.getMessage());
                for (StackTraceElement element : cause.getStackTrace()) {
                    System.err.println("    at " + element);
                }
                cause = cause.getCause();
            }
            throw e;
        }
    }
}
