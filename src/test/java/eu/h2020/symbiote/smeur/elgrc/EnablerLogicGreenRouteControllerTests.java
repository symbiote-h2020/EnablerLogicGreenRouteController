/*
package eu.h2020.symbiote.smeur.elgrc;

import eu.h2020.symbiote.smeur.elgrc.commons.Utils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;


@RunWith(SpringRunner.class)
@SpringBootTest
public class EnablerLogicGreenRouteControllerTests {

    @Value("${httpEndpoint.mobaas}")
    String dataEmbersCityEndpoint;


    @Test
    public void cenas() throws Exception {

        */
/*File newfile = new File("streetSegmentszagreb.json");


        // send file
        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add("file",  new FileSystemResource(Utils.targzCompress(newfile)));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(dataEmbersCityEndpoint,
                HttpMethod.POST, requestEntity, String.class);


        Assert.assertEquals(response.getStatusCode().value(), 200);*//*

    }

}*/
