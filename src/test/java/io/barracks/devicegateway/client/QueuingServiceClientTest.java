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
import io.barracks.devicegateway.model.DeviceInfo;
import io.barracks.devicegateway.utils.DeviceInfoUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class QueuingServiceClientTest {

    private QueuingServiceClient queuingServiceClient;

    private String exchangeName = "test_exchange";
    private String routingKey = "test_routing_key";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Before
    public void setUp() {
        queuingServiceClient = new QueuingServiceClient(exchangeName, routingKey, rabbitTemplate);
    }

    @Test
    public void sendDataToQueue_whenServiceSucceeds_serverShouldBeCalled() throws JsonProcessingException {
        // Given
        final DeviceInfo deviceInfo = DeviceInfoUtils.getDeviceInfo();

        // When
        queuingServiceClient.postDeviceInfo(deviceInfo);

        // Then
        verify(rabbitTemplate).convertAndSend(exchangeName, routingKey, deviceInfo);
    }

    @Test
    public void sendDataToQueue_whenServiceFails_shouldLogException() throws JsonProcessingException {
        // Given
        final DeviceInfo deviceInfo = DeviceInfoUtils.getDeviceInfo();
        doThrow(Exception.class).when(rabbitTemplate).convertAndSend(exchangeName, routingKey, deviceInfo);

        // When
        queuingServiceClient.postDeviceInfo(deviceInfo);

        // Then
        verify(rabbitTemplate).convertAndSend(exchangeName, routingKey, deviceInfo);
    }

}
