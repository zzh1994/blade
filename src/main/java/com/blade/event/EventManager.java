/**
 * Copyright (c) 2017, biezhi 王爵 (biezhi.me@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blade.event;

import com.blade.ioc.bean.OrderComparator;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Event manager
 *
 * @author biezhi
 * @date 2017/9/18
 */
public class EventManager {

    private Map<EventType, List<EventListener>> listenerMap;
    private OrderComparator<EventListener>      comparator = new OrderComparator<>();

    public EventManager() {
        this.listenerMap = Stream.of(EventType.values()).collect(Collectors.toMap(v -> v, v -> new LinkedList<>()));
    }

    public <T> void addEventListener(EventType type, EventListener listener) {
        listenerMap.get(type).add(listener);
    }

    public <T> void fireEvent(EventType type, Event event) {
        listenerMap.get(type).stream()
                .sorted(comparator)
                .forEach(listener -> listener.trigger(event));
    }

}