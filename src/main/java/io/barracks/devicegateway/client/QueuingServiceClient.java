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

package io.barracks.devicegateway.client;


import io.barracks.devicegateway.model.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class QueuingServiceClient {

    private final RabbitTemplate rabbitTemplate;

    private String exchangeName;
    private String routingKey;

    @Autowired
    public QueuingServiceClient(
            @Value("${io.barracks.googleanalyticsv1.exchangename}") String exchangeName,
            @Value("${io.barracks.queuingservice.routingkey.v1}") String routingKey,
            RabbitTemplate rabbitTemplate
    ) {
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void postDeviceInfo(DeviceInfo deviceInfo) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, deviceInfo);
        } catch (Exception e) {
            log.error("The message cannot be sent to RabbitMQ. It is possible that the broker is not running. Exception : " + e);
        }
    }

}
