package com.blade.event;

import com.blade.Blade;
import org.junit.Test;

/**
 * @author biezhi
 * @date 2017/9/19
 */
public class EventManagerTest {

    @Test
    public void testManager() {
        EventManager eventManager = new EventManager();
        eventManager.addEventListener(EventType.SERVER_STARTED, b -> System.out.println("server started"));
        eventManager.fireEvent(EventType.SERVER_STARTED, new Event().attribute("blade", Blade.of()));
    }

}
