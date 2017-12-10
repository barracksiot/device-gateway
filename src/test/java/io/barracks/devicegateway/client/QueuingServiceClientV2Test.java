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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.barracks.devicegateway.model.DeviceEvent;
import io.barracks.devicegateway.model.DeviceRequest;
import io.barracks.devicegateway.utils.DeviceEventUtils;
import io.barracks.devicegateway.utils.DeviceRequestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.metrics.CounterService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class QueuingServiceClientV2Test {

    private QueuingServiceClientV2 queuingServiceClientV2;

    private String exchangeName = "test_exchange";
    private String routingKey = "test_routing_key";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Before
    public void setUp() {
        queuingServiceClientV2 = new QueuingServiceClientV2(exchangeName, routingKey, rabbitTemplate, mock(CounterService.class));
    }

    @Test
    public void sendDataToQueue_whenServiceSucceeds_serverShouldBeCalled() throws JsonProcessingException {
        // Given
        final DeviceEvent deviceEvent = DeviceEventUtils.getDeviceEvent();

        // When
        queuingServiceClientV2.postDeviceRequest(deviceEvent);

        // Then
        verify(rabbitTemplate).convertAndSend(exchangeName, routingKey, deviceEvent);
    }

}
