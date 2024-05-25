package com.csm.services;

import java.time.LocalDateTime;
import java.util.Base64;

import com.csm.dto.ContractResponse;
import com.csm.enums.Infra;
import com.csm.services.impl.LoginClientImpl;
import com.csm.util.Util;
import org.apache.axis.components.logger.LogFactory;
import org.apache.commons.logging.Log;
import org.springframework.stereotype.Component;


@Component
public class CsmServices {

    protected static Log log =
            LogFactory.getLog(CsmServices.class.getName());

    public ContractResponse token(Infra infra,
                                  String Services,
                                  LocalDateTime gentime,
                                  LocalDateTime exptime) {

        return proccesToken(infra, Services, gentime, exptime);
    }

    private ContractResponse proccesToken(Infra infra,
                                          String service,
                                          LocalDateTime gentime,
                                          LocalDateTime exptime) {

        LoginClientImpl.LoginTicketRequest_xml_string = null;

        String LoginTicketResponse = null;
        String LoginTicketResponseEncode = null;
        String LoginTicketString = null;

        String endpoint = Util.getValueProper(infra, "ws.afip.endpoint");
        //String service = Util.getValueProper(infra, "ws.afip.service");
        String dstDN = Util.getValueProper(infra, "ws.afip.dstdn");
        String p12file = Util.getValueProper(infra, "ws.afip.p12file");
        String signer = Util.getValueProper(infra, "ws.afip.signer");
        String p12pass = Util.getValueProper(infra, "ws.afip.p12pass");

        // Create LoginTicketRequest_xml_cms
        LoginClientImpl.LoginTicketRequest_xml_string = null;

        byte[] LoginTicketRequest_xml_cms = null;
        try {
            LoginTicketRequest_xml_cms = LoginClientImpl.create_cms(p12file, p12pass,
                    signer, dstDN, service, gentime, exptime);
            LoginTicketString = LoginClientImpl.LoginTicketRequest_xml_string;

            System.out.println("LoginTicketString : " + LoginTicketString);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            log.error(e.getMessage());
            return new ContractResponse(500, e.getMessage());
        }


        try {
            LoginTicketResponse = LoginClientImpl.invoke_wsaa(LoginTicketRequest_xml_cms, endpoint);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            log.error(e.getMessage());
            return new ContractResponse(500, e.getMessage());
        }


        byte[] encoded = Base64.getEncoder().encode(LoginTicketResponse.getBytes());
        LoginTicketResponseEncode = new String(encoded);
        System.out.println(LoginTicketResponse);
        System.out.println(LoginTicketResponseEncode);

        return new ContractResponse(200 , "success", LoginTicketResponseEncode);

    }
}
