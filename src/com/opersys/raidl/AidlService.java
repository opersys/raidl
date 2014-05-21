/*
 * Copyright (C) 2014 Opersys inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opersys.raidl;

import java.lang.reflect.Method;
import java.util.SortedMap;
import java.util.TreeMap;

public class AidlService {

    private String serviceName;

    private Class<?> serviceClass;

    private SortedMap<Integer, Method> serviceMethods;

    public AidlService(String serviceName, Class<?> serviceClass) {
        this.serviceClass = serviceClass;
        this.serviceName = serviceName;

        this.serviceMethods = new TreeMap<Integer, Method>();
    }

    public void addMethod(int methodNo, Method serviceMethod) {
        this.serviceMethods.put(methodNo, serviceMethod);
    }

    public Integer[] getMethodCodes() {
        return serviceMethods.keySet().toArray(new Integer[serviceMethods.keySet().size()]);
    }

    public Method getMethodByCode(Integer methodNo) {
        return serviceMethods.get(methodNo);
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public Class<?> getServiceClass() {
        return this.serviceClass;
    }
}
