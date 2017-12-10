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


import io.barracks.devicegateway.model.DeviceEvent;
import io.barracks.devicegateway.model.DeviceRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class QueuingServiceClientV2 {

    private final RabbitTemplate rabbitTemplate;

    private String exchangeName;
    private String routingKey;

    private final CounterService counter;

    @Autowired
    public QueuingServiceClientV2(
            @Value("${io.barracks.amqp.exchangename}") String exchangeName,
            @Value("${io.barracks.queuingservice.routingkey.v2}") String routingKey,
            RabbitTemplate rabbitTemplate,
            CounterService counter
    ) {
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.rabbitTemplate = rabbitTemplate;
        this.counter = counter;
    }

    public void postDeviceRequest(DeviceEvent deviceEvent) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, deviceEvent);
            incrementRabbitMQMetric("success");
        } catch (Exception e) {
            log.error("The message cannot be sent to RabbitMQ. It is possible that the broker is not running. Exception : " + e);
            incrementRabbitMQMetric("error");
        }
    }

    private void incrementRabbitMQMetric(String status) {
        counter.increment("message.sent.device." + status);
    }
}
