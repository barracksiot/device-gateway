/*
 * MIT License
 *
 * Copyright (c) 2017 Barracks Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.barracks.devicegateway.integration;

import io.barracks.devicegateway.client.LargeFileForwardingExtractor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {LargeFileTest.TestApp.class, LargeFileTest.LargeFileResource.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class LargeFileTest {

    private static final long FILE_SIZE = (long) 1024 * 1024 * 1024;
    @Value("http://localhost:${local.server.port}")
    private String server;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Test
    public void loadLargeFile() throws IOException {
        final RestTemplate restTemplate = restTemplateBuilder.build();
        final OutputStream os = new FileOutputStream("/dev/null");
        final Long size = restTemplate.execute(
                URI.create(server + "/large"),
                HttpMethod.GET,
                null,
                new LargeFileForwardingExtractor(restTemplate.getMessageConverters(), os)
        );
        assertThat(size).isEqualTo(FILE_SIZE);
    }

    @SpringBootApplication
    @EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
    public static class TestApp {
    }


    @RestController
    @RequestMapping(path = "/large")
    public static class LargeFileResource {

        @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
        public void getLargeFile(HttpServletResponse response) throws Exception {
            OutputStream os = response.getOutputStream();
            byte[] buffer = new byte[4096];
            SecureRandom random = new SecureRandom();
            random.nextBytes(buffer);
            long count = 0;
            while (count < FILE_SIZE) {
                os.write(buffer, 0, buffer.length);
                count += buffer.length;
            }
            response.flushBuffer();
        }
    }

}
