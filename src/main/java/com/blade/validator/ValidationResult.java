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
package com.blade.validator;

import com.blade.exception.ValidatorException;
import com.blade.kit.BladeKit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.var;

@Data
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;
    private String message;
    private String code;

    public static ValidationResult ok() {
        return new ValidationResult(true, null, null);
    }

    public static ValidationResult ok(String code) {
        return new ValidationResult(true, null, code);
    }

    public static ValidationResult fail(String message) {
        return new ValidationResult(false, message, null);
    }

    public static ValidationResult fail(String code, String message) {
        return new ValidationResult(false, message, code);
    }

    public void throwIfInvalid() {
        this.throwMessage(getMessage());
    }

    public void throwIfInvalid(String fieldName) {
        if (!isValid()) throw new ValidatorException(fieldName + " " + getMessage());
    }

    public <T, R> void throwIfInvalid(TypeFunction<T, R> function) {
        var fieldName = BladeKit.getLambdaFieldName(function);
        throwIfInvalid(fieldName);
    }

    public void throwMessage(String msg) {
        if (!isValid()) throw new ValidatorException(msg);
    }

}