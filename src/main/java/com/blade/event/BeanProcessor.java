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

import com.blade.Blade;

/**
 * Bean processor
 * <p>
 * When the Blade program execution at startup time
 *
 * @author biezhi
 * @date 2017/9/18
 * @see com.blade.loader.BladeLoader
 */
@Deprecated
@FunctionalInterface
public interface BeanProcessor {

    /**
     * Initialize the ioc container after execution
     *
     * @param blade Blade instance
     */
    void processor(Blade blade);

    /**
     * Initialize the ioc container before execution
     *
     * @param blade Blade instance
     */
    default void preHandle(Blade blade) {
    }

}