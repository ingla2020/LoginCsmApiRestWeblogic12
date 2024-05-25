package com.csm.services.impl;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.rpc.ParameterMode;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.Base64;
import org.apache.axis.encoding.XMLType;

@SuppressWarnings("All")
public class LoginClientImpl {

    protected static Log log =
            LogFactory.getLog(LoginClientImpl.class.getName());

    public static String LoginTicketRequest_xml_string;

    public static byte [] create_cms(String p12file, String p12pass, String signer, String dstDN, String service,
                                     LocalDateTime gentime,
                                     LocalDateTime exptime) throws Exception {
        PrivateKey pKey = null;
        X509Certificate pCertificate = null;
        byte[] asn1_cms = null;
        CertStore cstore = null;
        String LoginTicketRequest_xml;
        String SignerDN = null;

        //
        // Manage Keys & Certificates
        //
        try {
            ClassLoader loader = LoginClientImpl.class.getClassLoader();
            InputStream inst = loader.getResourceAsStream(p12file);
            //InputStream reso = Thread.currentThread().getContextClassLoader().getResourceAsStream(p12file);

            // Create a keystore using keys from the pkcs#12 p12file
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(inst, p12pass.toCharArray());
            inst.close();

            // Get Certificate & Private key from KeyStore
            pKey = (PrivateKey) ks.getKey(signer, p12pass.toCharArray());

            log.info("create_cms : normarl");
            pCertificate = (X509Certificate) ks.getCertificate(signer);

            SignerDN = pCertificate.getSubjectDN().toString();

            // Create a list of Certificates to include in the final CMS
            ArrayList<X509Certificate> certList = new ArrayList<X509Certificate>();
            certList.add(pCertificate);

            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            cstore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList), "BC");
        } catch (Exception e) {
            log.error("Manage Keys & Certificates create_cms : ");
            log.error(e);
            throw e;
        }

        //
        // Create XML Message
        //
        try {
            LoginTicketRequest_xml = create_LoginTicketRequest(SignerDN, dstDN, service, gentime, exptime);
            LoginTicketRequest_xml_string = LoginTicketRequest_xml;
        }catch (Exception e){
            log.error("Create CMS XML Message : ");
            log.error(e);
            throw e;
        }

        //
        // Create CMS Message
        //
        try {
            // Create a new empty CMS Message
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

            List<X509Certificate> certList = new ArrayList<X509Certificate>();
            certList.add(pCertificate);

            Store certStore = new JcaCertStore(certList);
            ContentSigner sha1Signer = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(pKey);
            gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                            .build(sha1Signer, pCertificate));

            gen.addCertificates(certStore);

            CMSTypedData data2 = new CMSProcessableByteArray(LoginTicketRequest_xml.getBytes("UTF-8"));

            CMSSignedData sigData = gen.generate(data2, true);
            asn1_cms = sigData.getEncoded();
        } catch (Exception e) {
            log.error("Create CMS XML Message : ");
            log.error(e);
            throw e;
        }


        return (asn1_cms);

    }


    public static String invoke_wsaa(byte[] LoginTicketRequest_xml_cms, String endpoint) throws Exception {
        String LoginTicketResponse = null;


        Integer timeout = 250000;
        try {
            // activa el TLSv1.2

            Service  service = new Service();
            Call call = (Call)service.createCall();

            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("loginCms");
            call.addParameter("request", XMLType.XSD_STRING, ParameterMode.IN);
//			call.addParameter(paramName, paramq, parameterMode);
            call.setReturnType(XMLType.XSD_STRING);
            call.setTimeout(timeout);
            LoginTicketResponse = (String) call.invoke(new Object[] { Base64.encode(LoginTicketRequest_xml_cms) });

        } catch (Exception e) {
            log.error("Cinvoke_wsaa servicio : ");
            log.error(e);
            throw e;
        }
        return (LoginTicketResponse);
    }


    public static Document convertStringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try
        {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse( new InputSource( new StringReader( xmlStr ) ) );
            return doc;
        } catch (Exception e) {
            log.error("convertStringToDocument : ");
            log.error(e);
        }
        return null;
    }


    //
    // Create XML Message for AFIP wsaa
    //
    public static String create_LoginTicketRequest(String SignerDN,
                                                   String dstDN,
                                                   String service,
                                                   LocalDateTime gentimeL,
                                                   LocalDateTime exptimeL) throws Exception {

        String LoginTicketRequest_xml;

        //Date GenTime = new Date();
        //GregorianCalendar gentime = new GregorianCalendar();
        //GregorianCalendar exptime = new GregorianCalendar();
        //String UniqueId = String. valueOf(GenTime.getTime() / 1000);
        // exptime.setTime(new Date(GenTime.getTime() + TicketTime));

        int year = gentimeL.getYear();
        int month = gentimeL.getMonthValue()-1;
        int day = gentimeL.getDayOfMonth();
        int hour = gentimeL.getHour();
        int minute = gentimeL.getMinute();
        int second = gentimeL.getSecond();

        int yearexp = exptimeL.getYear();
        int monthexp = exptimeL.getMonthValue()-1;
        int dayexp = exptimeL.getDayOfMonth();
        int hourexp = exptimeL.getHour();
        int minuteexp = exptimeL.getMinute();
        int secondexp = exptimeL.getSecond();

        GregorianCalendar gentime = new GregorianCalendar(year, month, day, hour, minute, second);
        gentime.setTimeZone(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));

        GregorianCalendar exptime = new GregorianCalendar(yearexp, monthexp, dayexp, hourexp, minuteexp, secondexp);
        exptime.setTimeZone(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));

        String UniqueId = String. valueOf(gentime.getTime().getTime() / 1000);

        XMLGregorianCalendar XMLGenTime = null;
        try {
            XMLGenTime = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(gentime);
        } catch (DatatypeConfigurationException e) {
            log.error(e.getMessage());
            throw e;
        } //


        XMLGregorianCalendar XMLExpTime = null;
        try {
            XMLExpTime = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(exptime);
        } catch (DatatypeConfigurationException e) {
            log.error(e.getMessage());
            throw e;
        } //


        LoginTicketRequest_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<loginTicketRequest version=\"1.0\">" + "<header>" + "<source>" + SignerDN + "</source>"
                + "<destination>" + dstDN + "</destination>" + "<uniqueId>" + UniqueId + "</uniqueId>"
                + "<generationTime>" + XMLGenTime + "</generationTime>" + "<expirationTime>" + XMLExpTime
                + "</expirationTime>" + "</header>" + "<service>" + service + "</service>" + "</loginTicketRequest>";

        return (LoginTicketRequest_xml);
    }



}