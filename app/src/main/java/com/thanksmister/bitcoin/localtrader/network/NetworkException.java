/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thanksmister.bitcoin.localtrader.network;

public class NetworkException extends Exception {

    private int code;
    private int status;
    private Throwable cause;

    public int getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public Throwable getCause() {
        return cause;
    }

    public NetworkException(String msg, Throwable cause) {
        super(msg);
        this.cause = cause;
    }

    public NetworkException(String msg, int code, int status, Throwable cause) {
        super(msg);
        this.code = code;
        this.status = status;
        this.cause = cause;
    }

    public NetworkException(String msg, int code, Throwable cause) {
        super(msg);
        this.code = code;
        this.cause = cause;
    }
}
