/**
 * Copyright (c) 2018, biezhi 王爵nice (biezhi.me@gmail.com)
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
package com.blade.exception;

/**
 * HTTP 500 internal error exception
 *
 * @author biezhi
 * @date 2017/9/18
 */
public class InternalErrorException extends BladeException {

    public static final int STATUS = 500;
    private static final String NAME = "Internal Error";

    public InternalErrorException() {
        super(STATUS, NAME);
    }

    public InternalErrorException(String message) {
        super(STATUS, NAME, message);
    }

}
